package com.example.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.TimeDeleteDatabase
import com.example.data.prefs.SettingsRepository
import com.example.data.repository.TimeDeleteRepository

class DeletionCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = TimeDeleteDatabase.getInstance(applicationContext)
        val settingsRepo = SettingsRepository(applicationContext)
        val repo = TimeDeleteRepository(applicationContext, db.timeDeleteDao(), settingsRepo)
        repo.evaluateScheduledCountdowns()
        DeletionScheduler.syncSchedules(applicationContext)
        return Result.success()
    }
}
