package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeDeleteDao {

    // Tracked Media
    @Query("SELECT * FROM tracked_media ORDER BY dateAddedMillis DESC, addedToLibraryAtMillis DESC")
    fun observeAllTrackedMedia(): Flow<List<TrackedMediaEntity>>

    @Query("SELECT * FROM tracked_media WHERE mediaType = :mediaType ORDER BY dateAddedMillis DESC, addedToLibraryAtMillis DESC")
    fun observeTrackedMediaByType(mediaType: MediaType): Flow<List<TrackedMediaEntity>>

    @Query("SELECT * FROM tracked_media")
    suspend fun getAllTrackedMediaOnce(): List<TrackedMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackedMedia(items: List<TrackedMediaEntity>)

    @Query("DELETE FROM tracked_media WHERE uriString IN (:uris)")
    suspend fun deleteTrackedMediaByUris(uris: List<String>)

    // Scheduled Deletions
    @Query("SELECT * FROM scheduled_deletions ORDER BY deleteAtMillis ASC")
    fun observeScheduledDeletions(): Flow<List<ScheduledDeletionEntity>>

    @Query("SELECT * FROM scheduled_deletions ORDER BY deleteAtMillis ASC")
    suspend fun getScheduledDeletionsOnce(): List<ScheduledDeletionEntity>

    @Query("SELECT * FROM scheduled_deletions WHERE uriString = :uriString LIMIT 1")
    suspend fun findScheduledByUri(uriString: String): ScheduledDeletionEntity?

    @Query("SELECT * FROM scheduled_deletions WHERE id = :id LIMIT 1")
    suspend fun findScheduledById(id: Long): ScheduledDeletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScheduledDeletion(item: ScheduledDeletionEntity): Long

    @Update
    suspend fun updateScheduledDeletion(item: ScheduledDeletionEntity)

    @Query("DELETE FROM scheduled_deletions WHERE id = :id")
    suspend fun deleteScheduledById(id: Long)

    @Query("DELETE FROM scheduled_deletions WHERE id IN (:ids)")
    suspend fun deleteScheduledByIds(ids: List<Long>)

    // Deletion History
    @Query("SELECT * FROM deletion_history ORDER BY executedAtMillis DESC")
    fun observeDeletionHistory(): Flow<List<DeletionHistoryEntity>>

    @Insert
    suspend fun insertHistoryRecord(record: DeletionHistoryEntity)

    @Insert
    suspend fun insertHistoryRecords(records: List<DeletionHistoryEntity>)

    @Query("DELETE FROM deletion_history")
    suspend fun clearDeletionHistory()

    // Protected Folders
    @Query("SELECT * FROM protected_folders ORDER BY folderName ASC")
    fun observeProtectedFolders(): Flow<List<ProtectedFolderEntity>>

    @Query("SELECT * FROM protected_folders")
    suspend fun getProtectedFoldersOnce(): List<ProtectedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProtectedFolder(folder: ProtectedFolderEntity): Long

    @Query("DELETE FROM protected_folders WHERE id = :id")
    suspend fun deleteProtectedFolderById(id: Long)
}
