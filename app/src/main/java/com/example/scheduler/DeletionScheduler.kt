package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.db.ScheduleStatus
import com.example.data.db.TimeDeleteDatabase
import com.example.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object DeletionScheduler {
    private const val UNIQUE_NEXT_CHECK_WORK = "timedelete_next_check"
    private const val UNIQUE_PERIODIC_SWEEP = "timedelete_periodic_sweep"
    private const val ALARM_REQUEST_CODE = 7710

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    suspend fun syncSchedules(context: Context) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val settings = SettingsRepository(appContext).settingsFlow.first()
        val dao = TimeDeleteDatabase.getInstance(appContext).timeDeleteDao()
        val activeItems = dao.getScheduledDeletionsOnce()
            .filter { it.status == ScheduleStatus.SCHEDULED }

        if (settings.isPausedAll || !settings.backgroundMonitorEnabled || activeItems.isEmpty()) {
            workManager.cancelUniqueWork(UNIQUE_NEXT_CHECK_WORK)
            cancelAlarm(appContext, alarmManager)
            if (activeItems.isEmpty() || !settings.backgroundMonitorEnabled) {
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_SWEEP)
            }
            return
        }

        // Ensure periodic 15-minute sweep runs as battery-friendly safeguard against Doze/OEM kills
        val periodic = PeriodicWorkRequestBuilder<DeletionCheckWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SWEEP,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )

        val now = System.currentTimeMillis()
        val leadMillis = settings.approachingLeadMinutes * 60_000L

        // Find the earliest wakeup needed (either approaching reminder or actual expiry)
        var earliestTrigger = Long.MAX_VALUE
        for (item in activeItems) {
            if (!item.approachingNotified && settings.notifyApproaching) {
                val approachingAt = item.deleteAtMillis - leadMillis
                if (approachingAt > now && approachingAt < earliestTrigger) {
                    earliestTrigger = approachingAt
                }
            }
            if (item.deleteAtMillis < earliestTrigger) {
                earliestTrigger = item.deleteAtMillis
            }
        }

        if (earliestTrigger == Long.MAX_VALUE) return

        val delayMillis = (earliestTrigger - now).coerceAtLeast(0L)
        val oneTime = OneTimeWorkRequestBuilder<DeletionCheckWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_NEXT_CHECK_WORK,
            ExistingWorkPolicy.REPLACE,
            oneTime
        )

        scheduleAlarm(appContext, alarmManager, earliestTrigger.coerceAtLeast(now + 1_000L))
    }

    fun triggerImmediateCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<DeletionCheckWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "timedelete_immediate_check",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, triggerAtMillis: Long) {
        val pendingIntent = buildAlarmPendingIntent(context)
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager) {
        alarmManager.cancel(buildAlarmPendingIntent(context))
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DeletionAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
