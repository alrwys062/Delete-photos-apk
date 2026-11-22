package com.example.scheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.AppSettings
import com.example.util.LocaleHelper

object NotificationHelper {
    const val CHANNEL_ALERTS = "timedelete_alerts"
    const val CHANNEL_STATUS = "timedelete_status"
    const val EXTRA_NAV_DESTINATION = "extra_nav_destination"
    const val DEST_SCHEDULED = "scheduled"
    const val DEST_HISTORY = "history"

    fun createChannels(context: Context, language: AppLanguage = AppLanguage.SYSTEM) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val lCtx = LocaleHelper.localizedContext(context, language)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                lCtx.getString(R.string.notification_channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = lCtx.getString(R.string.notification_channel_alerts_desc)
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                lCtx.getString(R.string.notification_channel_status_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = lCtx.getString(R.string.notification_channel_status_desc)
            }

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(statusChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyScheduledSuccess(context: Context, settings: AppSettings, count: Int, timerLabel: String) {
        if (!settings.notifyScheduled) return
        val lCtx = LocaleHelper.localizedContext(context, settings.appLanguage)
        val title = if (count == 1) {
            lCtx.getString(R.string.notif_scheduled_title_single)
        } else {
            lCtx.getString(R.string.notif_scheduled_title_multi, count)
        }
        val body = lCtx.getString(R.string.notif_scheduled_body, timerLabel)
        post(context, settings.appLanguage, CHANNEL_STATUS, 1001, title, body, DEST_SCHEDULED, highPriority = false)
    }

    fun notifyApproaching(context: Context, settings: AppSettings, fileName: String, minutesLeft: Long) {
        if (!settings.notifyApproaching) return
        val lCtx = LocaleHelper.localizedContext(context, settings.appLanguage)
        val title = lCtx.getString(R.string.notif_approaching_title)
        val body = lCtx.getString(R.string.notif_approaching_body, fileName, minutesLeft.coerceAtLeast(1))
        post(context, settings.appLanguage, CHANNEL_ALERTS, 2000 + fileName.hashCode(), title, body, DEST_SCHEDULED, highPriority = true)
    }

    fun notifyCountdownExpired(context: Context, settings: AppSettings, fileName: String) {
        if (!settings.notifyExpired) return
        val lCtx = LocaleHelper.localizedContext(context, settings.appLanguage)
        val title = lCtx.getString(R.string.notif_expired_title)
        val body = lCtx.getString(R.string.notif_expired_body, fileName)
        post(context, settings.appLanguage, CHANNEL_ALERTS, 3000 + fileName.hashCode(), title, body, DEST_SCHEDULED, highPriority = true)
    }

    fun notifyAwaitingConfirmation(context: Context, settings: AppSettings, count: Int, firstName: String) {
        if (!settings.notifyAwaitingConfirmation) return
        val lCtx = LocaleHelper.localizedContext(context, settings.appLanguage)
        val title = if (count == 1) {
            lCtx.getString(R.string.notif_awaiting_title_single)
        } else {
            lCtx.getString(R.string.notif_awaiting_title_multi, count)
        }
        val body = if (count == 1) {
            lCtx.getString(R.string.notif_awaiting_body_single, firstName)
        } else {
            lCtx.getString(R.string.notif_awaiting_body_multi, firstName, count - 1)
        }
        post(context, settings.appLanguage, CHANNEL_ALERTS, 4001, title, body, DEST_SCHEDULED, highPriority = true)
    }

    fun notifyDeletionCompleted(context: Context, settings: AppSettings, count: Int, firstName: String) {
        if (!settings.notifyCompleted) return
        val lCtx = LocaleHelper.localizedContext(context, settings.appLanguage)
        val title = lCtx.getString(R.string.notif_completed_title)
        val body = if (count == 1) {
            lCtx.getString(R.string.notif_completed_body_single, firstName)
        } else {
            lCtx.getString(R.string.notif_completed_body_multi, count)
        }
        post(context, settings.appLanguage, CHANNEL_STATUS, 5001, title, body, DEST_HISTORY, highPriority = false)
    }

    fun notifyDeletionFailed(context: Context, settings: AppSettings, fileName: String, reason: String) {
        if (!settings.notifyFailed) return
        val lCtx = LocaleHelper.localizedContext(context, settings.appLanguage)
        val title = lCtx.getString(R.string.notif_failed_title)
        val body = "$fileName: $reason"
        post(context, settings.appLanguage, CHANNEL_ALERTS, 6000 + fileName.hashCode(), title, body, DEST_HISTORY, highPriority = true)
    }

    fun sendTestNotification(context: Context, language: AppLanguage = AppLanguage.SYSTEM) {
        val lCtx = LocaleHelper.localizedContext(context, language)
        post(
            context = context,
            language = language,
            channelId = CHANNEL_ALERTS,
            notificationId = 9001,
            title = lCtx.getString(R.string.notif_test_title),
            body = lCtx.getString(R.string.notif_test_body),
            destination = DEST_SCHEDULED,
            highPriority = true
        )
    }

    private fun post(
        context: Context,
        language: AppLanguage,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        destination: String,
        highPriority: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        createChannels(context, language)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_DESTINATION, destination)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }
}
