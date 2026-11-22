package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.data.db.TimeDeleteDatabase
import com.example.data.prefs.SettingsRepository
import com.example.data.repository.TimeDeleteRepository
import com.example.scheduler.DeletionScheduler
import com.example.scheduler.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeDeleteApp : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TimeDeleteDatabase by lazy { TimeDeleteDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val repository: TimeDeleteRepository by lazy {
        TimeDeleteRepository(this, database.timeDeleteDao(), settingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        appScope.launch {
            DeletionScheduler.syncSchedules(this@TimeDeleteApp)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
