package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.db.DeletionHistoryEntity
import com.example.data.db.HistoryOutcome
import com.example.data.db.MediaType
import com.example.data.db.ProtectedFolderEntity
import com.example.data.db.ScheduleStatus
import com.example.data.db.ScheduledDeletionEntity
import com.example.data.db.TimeDeleteDao
import com.example.data.db.TrackedMediaEntity
import com.example.data.media.FileVerificationResult
import com.example.data.media.MediaPlatformHelper
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.AppSettings
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.ThemeMode
import com.example.scheduler.DeletionScheduler
import com.example.scheduler.NotificationHelper
import com.example.util.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class ScheduleBatchOutcome(
    val scheduledCount: Int,
    val skippedDuplicates: Int,
    val blockedProtected: Int,
    val blockedFolderNames: List<String>
)

class TimeDeleteRepository(
    private val context: Context,
    private val dao: TimeDeleteDao,
    private val settingsRepository: SettingsRepository
) {
    val trackedPhotos: Flow<List<TrackedMediaEntity>> = dao.observeTrackedMediaByType(MediaType.PHOTO)
    val trackedVideos: Flow<List<TrackedMediaEntity>> = dao.observeTrackedMediaByType(MediaType.VIDEO)
    val allTrackedMedia: Flow<List<TrackedMediaEntity>> = dao.observeAllTrackedMedia()
    val scheduledDeletions: Flow<List<ScheduledDeletionEntity>> = dao.observeScheduledDeletions()
    val deletionHistory: Flow<List<DeletionHistoryEntity>> = dao.observeDeletionHistory()
    val protectedFolders: Flow<List<ProtectedFolderEntity>> = dao.observeProtectedFolders()
    val settings: Flow<AppSettings> = settingsRepository.settingsFlow

    private suspend fun localizedContext(): Context {
        val lang = settingsRepository.settingsFlow.first().appLanguage
        return LocaleHelper.localizedContext(context, lang)
    }

    suspend fun importMediaUris(
        uris: List<Uri>,
        fallbackType: MediaType? = null
    ): List<TrackedMediaEntity> {
        if (uris.isEmpty()) return emptyList()
        val parsed = uris.mapNotNull { uri ->
            MediaPlatformHelper.extractMediaMetadata(context, uri, fallbackType)
        }
        if (parsed.isNotEmpty()) {
            dao.upsertTrackedMedia(parsed)
        }
        return parsed
    }

    suspend fun refreshTrackedLibraryIntegrity() {
        val current = dao.getAllTrackedMediaOnce()
        val staleUris = mutableListOf<String>()
        for (item in current) {
            val present = MediaPlatformHelper.isUriStillPresent(
                context = context,
                uriString = item.uriString,
                mediaStoreUriString = item.mediaStoreUriString
            )
            if (!present) {
                staleUris += item.uriString
            }
        }
        if (staleUris.isNotEmpty()) {
            dao.deleteTrackedMediaByUris(staleUris)
        }
    }

    suspend fun removeFromTrackedLibrary(uris: List<String>) {
        if (uris.isNotEmpty()) {
            dao.deleteTrackedMediaByUris(uris)
        }
    }

    suspend fun scheduleMediaDeletion(
        items: List<TrackedMediaEntity>,
        deleteAtMillis: Long,
        timerLabel: String
    ): ScheduleBatchOutcome {
        val now = System.currentTimeMillis()
        val safeDeleteAt = deleteAtMillis.coerceAtLeast(now + 10_000L)
        val duration = safeDeleteAt - now

        val protectedList = dao.getProtectedFoldersOnce()
        val protectedNames = protectedList.map { it.folderName.lowercase().trim() }.toSet()

        var scheduledCount = 0
        var skippedDuplicates = 0
        var blockedProtected = 0
        val blockedFolders = mutableSetOf<String>()

        for (media in items) {
            val folderKey = media.folderName.lowercase().trim()
            val isProtected = protectedNames.any { p ->
                p.isNotEmpty() && (folderKey == p || folderKey.contains(p) || p.contains(folderKey))
            }
            if (isProtected) {
                blockedProtected++
                blockedFolders += media.folderName
                continue
            }

            val existing = dao.findScheduledByUri(media.uriString)
            if (existing != null) {
                skippedDuplicates++
                continue
            }

            val entity = ScheduledDeletionEntity(
                uriString = media.uriString,
                mediaStoreUriString = media.mediaStoreUriString,
                displayName = media.displayName,
                mediaType = media.mediaType,
                mimeType = media.mimeType,
                sizeBytes = media.sizeBytes,
                dateAddedMillis = media.dateAddedMillis,
                durationMillis = media.durationMillis,
                folderName = media.folderName,
                scheduledAtMillis = now,
                deleteAtMillis = safeDeleteAt,
                originalDurationMillis = duration,
                status = ScheduleStatus.SCHEDULED
            )
            val insertedId = dao.insertScheduledDeletion(entity)
            if (insertedId > 0L) {
                scheduledCount++
            } else {
                skippedDuplicates++
            }
        }

        if (scheduledCount > 0) {
            val currentSettings = settingsRepository.settingsFlow.first()
            NotificationHelper.notifyScheduledSuccess(context, currentSettings, scheduledCount, timerLabel)
            DeletionScheduler.syncSchedules(context)
        }

        return ScheduleBatchOutcome(
            scheduledCount = scheduledCount,
            skippedDuplicates = skippedDuplicates,
            blockedProtected = blockedProtected,
            blockedFolderNames = blockedFolders.toList()
        )
    }

    suspend fun updateScheduledTimer(itemId: Long, newDeleteAtMillis: Long) {
        val existing = dao.findScheduledById(itemId) ?: return
        val now = System.currentTimeMillis()
        val safeTarget = newDeleteAtMillis.coerceAtLeast(now + 10_000L)
        dao.updateScheduledDeletion(
            existing.copy(
                scheduledAtMillis = now,
                deleteAtMillis = safeTarget,
                originalDurationMillis = safeTarget - now,
                status = ScheduleStatus.SCHEDULED,
                approachingNotified = false,
                expiredNotified = false,
                lastError = null
            )
        )
        DeletionScheduler.syncSchedules(context)
    }

    suspend fun cancelScheduledDeletion(item: ScheduledDeletionEntity, logInHistory: Boolean = true) {
        dao.deleteScheduledById(item.id)
        if (logInHistory) {
            val lCtx = localizedContext()
            dao.insertHistoryRecord(
                DeletionHistoryEntity(
                    uriString = item.uriString,
                    displayName = item.displayName,
                    mediaType = item.mediaType,
                    sizeBytes = item.sizeBytes,
                    folderName = item.folderName,
                    scheduledAtMillis = item.scheduledAtMillis,
                    executedAtMillis = System.currentTimeMillis(),
                    outcome = HistoryOutcome.CANCELLED,
                    details = lCtx.getString(R.string.msg_cancelled_by_user)
                )
            )
        }
        DeletionScheduler.syncSchedules(context)
    }

    suspend fun evaluateScheduledCountdowns() {
        val currentSettings = settingsRepository.settingsFlow.first()
        if (currentSettings.isPausedAll) return

        val lCtx = LocaleHelper.localizedContext(context, currentSettings.appLanguage)
        val now = System.currentTimeMillis()
        val leadMillis = currentSettings.approachingLeadMinutes * 60_000L
        val items = dao.getScheduledDeletionsOnce()
        val protectedNames = dao.getProtectedFoldersOnce().map { it.folderName.lowercase().trim() }.toSet()

        val newlyAwaiting = mutableListOf<ScheduledDeletionEntity>()

        for (item in items) {
            if (item.status == ScheduleStatus.FAILED) continue

            val folderKey = item.folderName.lowercase().trim()
            if (protectedNames.any { p -> p.isNotEmpty() && (folderKey == p || folderKey.contains(p)) }) {
                dao.deleteScheduledById(item.id)
                dao.insertHistoryRecord(
                    DeletionHistoryEntity(
                        uriString = item.uriString,
                        displayName = item.displayName,
                        mediaType = item.mediaType,
                        sizeBytes = item.sizeBytes,
                        folderName = item.folderName,
                        scheduledAtMillis = item.scheduledAtMillis,
                        executedAtMillis = now,
                        outcome = HistoryOutcome.CANCELLED,
                        details = lCtx.getString(R.string.msg_protected_blocked, item.folderName)
                    )
                )
                continue
            }

            if (item.status == ScheduleStatus.SCHEDULED) {
                val remaining = item.deleteAtMillis - now
                if (remaining <= 0L) {
                    when (val check = MediaPlatformHelper.verifyFileIntegrity(context, item)) {
                        is FileVerificationResult.Valid -> {
                            val updated = item.copy(
                                status = ScheduleStatus.AWAITING_CONFIRMATION,
                                expiredNotified = true,
                                lastError = null
                            )
                            dao.updateScheduledDeletion(updated)
                            newlyAwaiting += updated
                            if (!item.expiredNotified) {
                                NotificationHelper.notifyCountdownExpired(context, currentSettings, item.displayName)
                            }
                        }
                        is FileVerificationResult.Missing -> {
                            val reason = lCtx.getString(R.string.msg_file_missing)
                            dao.deleteScheduledById(item.id)
                            dao.deleteTrackedMediaByUris(listOf(item.uriString))
                            dao.insertHistoryRecord(
                                DeletionHistoryEntity(
                                    uriString = item.uriString,
                                    displayName = item.displayName,
                                    mediaType = item.mediaType,
                                    sizeBytes = item.sizeBytes,
                                    folderName = item.folderName,
                                    scheduledAtMillis = item.scheduledAtMillis,
                                    executedAtMillis = now,
                                    outcome = HistoryOutcome.FAILED,
                                    details = reason
                                )
                            )
                            NotificationHelper.notifyDeletionFailed(context, currentSettings, item.displayName, reason)
                        }
                        is FileVerificationResult.Mismatch -> {
                            dao.updateScheduledDeletion(
                                item.copy(
                                    status = ScheduleStatus.FAILED,
                                    lastError = check.reason
                                )
                            )
                            dao.insertHistoryRecord(
                                DeletionHistoryEntity(
                                    uriString = item.uriString,
                                    displayName = item.displayName,
                                    mediaType = item.mediaType,
                                    sizeBytes = item.sizeBytes,
                                    folderName = item.folderName,
                                    scheduledAtMillis = item.scheduledAtMillis,
                                    executedAtMillis = now,
                                    outcome = HistoryOutcome.FAILED,
                                    details = check.reason
                                )
                            )
                            NotificationHelper.notifyDeletionFailed(context, currentSettings, item.displayName, check.reason)
                        }
                    }
                } else if (remaining <= leadMillis && !item.approachingNotified) {
                    dao.updateScheduledDeletion(item.copy(approachingNotified = true))
                    val minutesLeft = (remaining / 60_000L).coerceAtLeast(1L)
                    NotificationHelper.notifyApproaching(context, currentSettings, item.displayName, minutesLeft)
                }
            }
        }

        if (newlyAwaiting.isNotEmpty()) {
            NotificationHelper.notifyAwaitingConfirmation(
                context = context,
                settings = currentSettings,
                count = newlyAwaiting.size,
                firstName = newlyAwaiting.first().displayName
            )
        }
    }

    suspend fun finalizeVerifiedDeletions(items: List<ScheduledDeletionEntity>): Pair<Int, Int> {
        val currentSettings = settingsRepository.settingsFlow.first()
        val lCtx = LocaleHelper.localizedContext(context, currentSettings.appLanguage)
        val now = System.currentTimeMillis()
        val confirmedDeleted = mutableListOf<ScheduledDeletionEntity>()
        val notDeleted = mutableListOf<ScheduledDeletionEntity>()

        for (item in items) {
            val stillExists = MediaPlatformHelper.isUriStillPresent(
                context = context,
                uriString = item.uriString,
                mediaStoreUriString = item.mediaStoreUriString
            )
            if (!stillExists) {
                confirmedDeleted += item
            } else {
                notDeleted += item
            }
        }

        if (confirmedDeleted.isNotEmpty()) {
            dao.deleteScheduledByIds(confirmedDeleted.map { it.id })
            dao.deleteTrackedMediaByUris(confirmedDeleted.map { it.uriString })
            dao.insertHistoryRecords(
                confirmedDeleted.map { item ->
                    DeletionHistoryEntity(
                        uriString = item.uriString,
                        displayName = item.displayName,
                        mediaType = item.mediaType,
                        sizeBytes = item.sizeBytes,
                        folderName = item.folderName,
                        scheduledAtMillis = item.scheduledAtMillis,
                        executedAtMillis = now,
                        outcome = HistoryOutcome.DELETED,
                        details = lCtx.getString(R.string.msg_deleted_verified)
                    )
                }
            )
            NotificationHelper.notifyDeletionCompleted(
                context = context,
                settings = currentSettings,
                count = confirmedDeleted.size,
                firstName = confirmedDeleted.first().displayName
            )
        }

        for (item in notDeleted) {
            val reason = lCtx.getString(R.string.msg_deleted_verify_failed)
            dao.updateScheduledDeletion(item.copy(status = ScheduleStatus.FAILED, lastError = reason))
            dao.insertHistoryRecord(
                DeletionHistoryEntity(
                    uriString = item.uriString,
                    displayName = item.displayName,
                    mediaType = item.mediaType,
                    sizeBytes = item.sizeBytes,
                    folderName = item.folderName,
                    scheduledAtMillis = item.scheduledAtMillis,
                    executedAtMillis = now,
                    outcome = HistoryOutcome.FAILED,
                    details = reason
                )
            )
            NotificationHelper.notifyDeletionFailed(context, currentSettings, item.displayName, reason)
        }

        DeletionScheduler.syncSchedules(context)
        return confirmedDeleted.size to notDeleted.size
    }

    suspend fun recordDeletionFailure(items: List<ScheduledDeletionEntity>, reason: String) {
        val currentSettings = settingsRepository.settingsFlow.first()
        val now = System.currentTimeMillis()
        for (item in items) {
            dao.updateScheduledDeletion(
                item.copy(
                    status = ScheduleStatus.AWAITING_CONFIRMATION,
                    lastError = reason
                )
            )
            dao.insertHistoryRecord(
                DeletionHistoryEntity(
                    uriString = item.uriString,
                    displayName = item.displayName,
                    mediaType = item.mediaType,
                    sizeBytes = item.sizeBytes,
                    folderName = item.folderName,
                    scheduledAtMillis = item.scheduledAtMillis,
                    executedAtMillis = now,
                    outcome = HistoryOutcome.FAILED,
                    details = reason
                )
            )
            NotificationHelper.notifyDeletionFailed(context, currentSettings, item.displayName, reason)
        }
    }

    suspend fun addProtectedFolder(folderName: String, treeUri: String? = null): Boolean {
        val clean = folderName.trim()
        if (clean.isBlank()) return false
        return dao.insertProtectedFolder(
            ProtectedFolderEntity(folderName = clean, treeUriString = treeUri)
        ) > 0L
    }

    suspend fun removeProtectedFolder(id: Long) {
        dao.deleteProtectedFolderById(id)
    }

    suspend fun clearHistory() {
        dao.clearDeletionHistory()
    }

    suspend fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    suspend fun setAppLanguage(language: AppLanguage) {
        settingsRepository.setAppLanguage(language)
        NotificationHelper.createChannels(context, language)
    }

    suspend fun setPausedAll(paused: Boolean) {
        settingsRepository.setPausedAll(paused)
        DeletionScheduler.syncSchedules(context)
    }

    suspend fun setBackgroundMonitorEnabled(enabled: Boolean) {
        settingsRepository.setBackgroundMonitorEnabled(enabled)
        DeletionScheduler.syncSchedules(context)
    }

    suspend fun setNotifyScheduled(enabled: Boolean) = settingsRepository.setNotifyScheduled(enabled)
    suspend fun setNotifyApproaching(enabled: Boolean) = settingsRepository.setNotifyApproaching(enabled)
    suspend fun setNotifyExpired(enabled: Boolean) = settingsRepository.setNotifyExpired(enabled)
    suspend fun setNotifyAwaitingConfirmation(enabled: Boolean) = settingsRepository.setNotifyAwaitingConfirmation(enabled)
    suspend fun setNotifyCompleted(enabled: Boolean) = settingsRepository.setNotifyCompleted(enabled)
    suspend fun setNotifyFailed(enabled: Boolean) = settingsRepository.setNotifyFailed(enabled)
    suspend fun setApproachingLeadMinutes(minutes: Int) = settingsRepository.setApproachingLeadMinutes(minutes)
}
