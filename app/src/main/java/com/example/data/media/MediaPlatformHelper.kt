package com.example.data.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.IntentSenderRequest
import com.example.data.db.MediaType
import com.example.data.db.ScheduledDeletionEntity
import com.example.data.db.TrackedMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class FileVerificationResult {
    data object Valid : FileVerificationResult()
    data object Missing : FileVerificationResult()
    data class Mismatch(val reason: String) : FileVerificationResult()
}

sealed class DeletePreparationResult {
    data class RequiresSystemConfirmation(
        val intentSenderRequest: IntentSenderRequest,
        val targetItems: List<ScheduledDeletionEntity>
    ) : DeletePreparationResult()

    data class DirectAttemptCompleted(
        val deletedIds: List<Long>,
        val failedItems: List<Pair<ScheduledDeletionEntity, String>>
    ) : DeletePreparationResult()

    data class Failed(val reason: String) : DeletePreparationResult()
}

object MediaPlatformHelper {

    fun takePersistablePermissionSafe(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Photo picker on some API levels provides temporary or non-persistable grants; ignore safely.
        } catch (_: IllegalArgumentException) {
            // Not a persistable document URI.
        }
    }

    suspend fun extractMediaMetadata(
        context: Context,
        uri: Uri,
        fallbackType: MediaType? = null
    ): TrackedMediaEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        takePersistablePermissionSafe(context, uri)

        val rawMime = resolver.getType(uri)
        var displayName: String? = null
        var sizeBytes = 0L
        var dateAddedMillis = 0L
        var durationMillis = 0L
        var folderName = ""
        var resolvedMime = rawMime

        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx)

                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }

                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    if (mimeIdx >= 0 && !cursor.isNull(mimeIdx)) {
                        resolvedMime = cursor.getString(mimeIdx)
                    }

                    val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) {
                        dateAddedMillis = cursor.getLong(dateTakenIdx)
                    }

                    if (dateAddedMillis <= 0L) {
                        val dateAddedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                        if (dateAddedIdx >= 0 && !cursor.isNull(dateAddedIdx)) {
                            val sec = cursor.getLong(dateAddedIdx)
                            dateAddedMillis = if (sec in 1..9_999_999_999L) sec * 1000L else sec
                        }
                    }

                    if (dateAddedMillis <= 0L) {
                        val lastModIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        if (lastModIdx >= 0 && !cursor.isNull(lastModIdx)) {
                            dateAddedMillis = cursor.getLong(lastModIdx)
                        }
                    }

                    val bucketIdx = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    if (bucketIdx >= 0 && !cursor.isNull(bucketIdx)) {
                        folderName = cursor.getString(bucketIdx).orEmpty()
                    }

                    if (folderName.isBlank()) {
                        val relPathIdx = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        if (relPathIdx >= 0 && !cursor.isNull(relPathIdx)) {
                            val rel = cursor.getString(relPathIdx).orEmpty().trimEnd('/')
                            folderName = rel.substringAfterLast('/', rel)
                        }
                    }

                    val durIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                    if (durIdx >= 0 && !cursor.isNull(durIdx)) {
                        durationMillis = cursor.getLong(durIdx)
                    }
                }
            }
        } catch (_: Exception) {
            return@withContext null
        }

        val finalMime = resolvedMime ?: "application/octet-stream"
        val mediaType = when {
            finalMime.startsWith("video/") -> MediaType.VIDEO
            finalMime.startsWith("image/") -> MediaType.PHOTO
            fallbackType != null -> fallbackType
            uri.toString().contains("video", ignoreCase = true) -> MediaType.VIDEO
            else -> MediaType.PHOTO
        }

        if (sizeBytes <= 0L) {
            try {
                resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0L) sizeBytes = afd.length
                }
            } catch (_: Exception) {
            }
        }

        if (mediaType == MediaType.VIDEO && durationMillis <= 0L) {
            durationMillis = extractVideoDuration(context, uri)
        }

        if (folderName.isBlank()) {
            folderName = inferFolderNameFromUri(uri)
        }

        val finalName = displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: if (mediaType == MediaType.VIDEO) "Video_${System.currentTimeMillis()}.mp4" else "Photo_${System.currentTimeMillis()}.jpg"

        val mediaStoreUri = resolveMediaStoreUri(uri, mediaType)

        TrackedMediaEntity(
            uriString = uri.toString(),
            mediaStoreUriString = mediaStoreUri?.toString(),
            displayName = finalName,
            mediaType = mediaType,
            mimeType = finalMime,
            sizeBytes = sizeBytes.coerceAtLeast(0L),
            dateAddedMillis = if (dateAddedMillis > 0L) dateAddedMillis else System.currentTimeMillis(),
            durationMillis = durationMillis.coerceAtLeast(0L),
            folderName = folderName.ifBlank { "Device Gallery" }
        )
    }

    private fun extractVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun inferFolderNameFromUri(uri: Uri): String {
        val path = uri.path.orEmpty()
        return when {
            path.contains("Screenshots", ignoreCase = true) -> "Screenshots"
            path.contains("Camera", ignoreCase = true) -> "Camera"
            path.contains("Download", ignoreCase = true) -> "Downloads"
            path.contains("WhatsApp", ignoreCase = true) -> "WhatsApp"
            else -> {
                val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                if (!docId.isNullOrBlank() && docId.contains('/')) {
                    docId.substringBeforeLast('/').substringAfterLast('/').ifBlank { "Device Gallery" }
                } else {
                    "Device Gallery"
                }
            }
        }
    }

    fun resolveMediaStoreUri(uri: Uri, mediaType: MediaType): Uri? {
        if (uri.authority == MediaStore.AUTHORITY) {
            val path = uri.path.orEmpty()
            if (path.contains("/external/") || path.contains("/external_primary/")) {
                return uri
            }
            val numericId = uri.lastPathSegment?.toLongOrNull()
            if (numericId != null) {
                val base = if (mediaType == MediaType.VIDEO) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                return ContentUris.withAppendedId(base, numericId)
            }
        }
        return null
    }

    suspend fun verifyFileIntegrity(
        context: Context,
        scheduled: ScheduledDeletionEntity
    ): FileVerificationResult = withContext(Dispatchers.IO) {
        val uri = Uri.parse(scheduled.uriString)
        val resolver = context.contentResolver

        var exists = false
        var currentName: String? = null
        var currentSize = -1L

        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    exists = true
                    val nIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nIdx >= 0 && !c.isNull(nIdx)) currentName = c.getString(nIdx)
                    val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (sIdx >= 0 && !c.isNull(sIdx)) currentSize = c.getLong(sIdx)
                }
            }
        } catch (_: Exception) {
        }

        if (!exists) {
            // Try checking MediaStore URI fallback if available
            val msUriStr = scheduled.mediaStoreUriString
            if (!msUriStr.isNullOrBlank()) {
                try {
                    resolver.openAssetFileDescriptor(Uri.parse(msUriStr), "r")?.use {
                        exists = true
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (!exists) {
            try {
                resolver.openAssetFileDescriptor(uri, "r")?.use {
                    exists = true
                }
            } catch (_: Exception) {
            }
        }

        if (!exists) {
            return@withContext FileVerificationResult.Missing
        }

        if (currentName != null && currentName != scheduled.displayName) {
            return@withContext FileVerificationResult.Mismatch(
                "File name changed from ${scheduled.displayName} to $currentName"
            )
        }
        if (scheduled.sizeBytes > 0L && currentSize > 0L && currentSize != scheduled.sizeBytes) {
            return@withContext FileVerificationResult.Mismatch(
                "File size changed (${scheduled.sizeBytes}B -> ${currentSize}B)"
            )
        }

        FileVerificationResult.Valid
    }

    suspend fun isUriStillPresent(context: Context, uriString: String, mediaStoreUriString: String?): Boolean =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val candidates = listOfNotNull(mediaStoreUriString, uriString).distinct()
            for (candidate in candidates) {
                val uri = Uri.parse(candidate)
                val foundViaQuery = try {
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        it.moveToFirst()
                    } ?: false
                } catch (_: Exception) {
                    false
                }
                if (foundViaQuery) return@withContext true

                val foundViaStream = try {
                    resolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                } catch (_: Exception) {
                    false
                }
                if (foundViaStream) return@withContext true
            }
            false
        }

    suspend fun prepareDeletionRequest(
        context: Context,
        items: List<ScheduledDeletionEntity>
    ): DeletePreparationResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext DeletePreparationResult.Failed("No items selected for deletion.")
        val resolver = context.contentResolver

        val mediaStoreTargets = mutableListOf<Pair<ScheduledDeletionEntity, Uri>>()
        val safTargets = mutableListOf<Pair<ScheduledDeletionEntity, Uri>>()

        for (item in items) {
            val rawUri = Uri.parse(item.uriString)
            val resolvedMsUri = item.mediaStoreUriString?.let { Uri.parse(it) }
                ?: resolveMediaStoreUri(rawUri, item.mediaType)
            if (resolvedMsUri != null) {
                mediaStoreTargets += item to resolvedMsUri
            } else if (DocumentsContract.isDocumentUri(context, rawUri)) {
                safTargets += item to rawUri
            } else if (rawUri.authority == MediaStore.AUTHORITY) {
                mediaStoreTargets += item to rawUri
            } else {
                safTargets += item to rawUri
            }
        }

        if (mediaStoreTargets.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uris = mediaStoreTargets.map { it.second }
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                    return@withContext DeletePreparationResult.RequiresSystemConfirmation(
                        intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                        targetItems = mediaStoreTargets.map { it.first }
                    )
                } catch (e: Exception) {
                    // Retry with raw URIs if MediaStore URI conversion wasn't accepted on this provider
                    try {
                        val rawUris = mediaStoreTargets.map { Uri.parse(it.first.uriString) }
                        val pendingIntent = MediaStore.createDeleteRequest(resolver, rawUris)
                        return@withContext DeletePreparationResult.RequiresSystemConfirmation(
                            intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                            targetItems = mediaStoreTargets.map { it.first }
                        )
                    } catch (inner: Exception) {
                        return@withContext DeletePreparationResult.Failed(
                            inner.localizedMessage ?: e.localizedMessage ?: "MediaStore could not authorize deletion for this item."
                        )
                    }
                }
            } else {
                // Android 10 (API 29) and below
                val deletedIds = mutableListOf<Long>()
                val failedItems = mutableListOf<Pair<ScheduledDeletionEntity, String>>()
                for ((item, uri) in mediaStoreTargets) {
                    try {
                        val rows = resolver.delete(uri, null, null)
                        if (rows > 0) {
                            deletedIds += item.id
                        } else {
                            failedItems += item to "System did not delete file (0 rows removed)."
                        }
                    } catch (rse: SecurityException) {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && rse is RecoverableSecurityException) {
                            return@withContext DeletePreparationResult.RequiresSystemConfirmation(
                                intentSenderRequest = IntentSenderRequest.Builder(rse.userAction.actionIntent.intentSender).build(),
                                targetItems = listOf(item)
                            )
                        } else {
                            failedItems += item to (rse.localizedMessage ?: "Permission denied by Android.")
                        }
                    } catch (e: Exception) {
                        failedItems += item to (e.localizedMessage ?: "Deletion failed.")
                    }
                }
                return@withContext DeletePreparationResult.DirectAttemptCompleted(deletedIds, failedItems)
            }
        }

        // SAF Document URIs
        val deletedIds = mutableListOf<Long>()
        val failedItems = mutableListOf<Pair<ScheduledDeletionEntity, String>>()
        for ((item, uri) in safTargets) {
            try {
                val deleted = if (DocumentsContract.isDocumentUri(context, uri)) {
                    DocumentsContract.deleteDocument(resolver, uri)
                } else {
                    resolver.delete(uri, null, null) > 0
                }
                if (deleted) {
                    deletedIds += item.id
                } else {
                    failedItems += item to "Provider declined deletion or URI is read-only."
                }
            } catch (e: Exception) {
                failedItems += item to (e.localizedMessage ?: "Unable to delete document URI without write access.")
            }
        }
        DeletePreparationResult.DirectAttemptCompleted(deletedIds, failedItems)
    }
}
