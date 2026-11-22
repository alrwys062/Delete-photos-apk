package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class TimeDeleteConverters {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType =
        MediaType.entries.firstOrNull { it.name == value } ?: MediaType.PHOTO

    @TypeConverter
    fun fromScheduleStatus(value: ScheduleStatus): String = value.name

    @TypeConverter
    fun toScheduleStatus(value: String): ScheduleStatus =
        ScheduleStatus.entries.firstOrNull { it.name == value } ?: ScheduleStatus.SCHEDULED

    @TypeConverter
    fun fromHistoryOutcome(value: HistoryOutcome): String = value.name

    @TypeConverter
    fun toHistoryOutcome(value: String): HistoryOutcome =
        HistoryOutcome.entries.firstOrNull { it.name == value } ?: HistoryOutcome.FAILED
}

@Database(
    entities = [
        TrackedMediaEntity::class,
        ScheduledDeletionEntity::class,
        DeletionHistoryEntity::class,
        ProtectedFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TimeDeleteConverters::class)
abstract class TimeDeleteDatabase : RoomDatabase() {
    abstract fun timeDeleteDao(): TimeDeleteDao

    companion object {
        @Volatile
        private var INSTANCE: TimeDeleteDatabase? = null

        fun getInstance(context: Context): TimeDeleteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TimeDeleteDatabase::class.java,
                    "timedelete.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
