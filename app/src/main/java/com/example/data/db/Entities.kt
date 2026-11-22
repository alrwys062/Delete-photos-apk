package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracked_media",
    indices = [Index(value = ["uriString"], unique = true)]
)
data class TrackedMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val mediaStoreUriString: String?,
    val displayName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedMillis: Long,
    val durationMillis: Long,
    val folderName: String,
    val addedToLibraryAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scheduled_deletions",
    indices = [Index(value = ["uriString"], unique = true)]
)
data class ScheduledDeletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val mediaStoreUriString: String?,
    val displayName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedMillis: Long,
    val durationMillis: Long,
    val folderName: String,
    val scheduledAtMillis: Long,
    val deleteAtMillis: Long,
    val originalDurationMillis: Long,
    val status: ScheduleStatus = ScheduleStatus.SCHEDULED,
    val approachingNotified: Boolean = false,
    val expiredNotified: Boolean = false,
    val lastError: String? = null
)

@Entity(tableName = "deletion_history")
data class DeletionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val displayName: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val folderName: String,
    val scheduledAtMillis: Long,
    val executedAtMillis: Long = System.currentTimeMillis(),
    val outcome: HistoryOutcome,
    val details: String
)

@Entity(
    tableName = "protected_folders",
    indices = [Index(value = ["folderName"], unique = true)]
)
data class ProtectedFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderName: String,
    val treeUriString: String? = null,
    val addedAtMillis: Long = System.currentTimeMillis()
)
