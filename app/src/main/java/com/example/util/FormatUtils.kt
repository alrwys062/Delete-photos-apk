package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    private fun isArabicLocale(locale: Locale): Boolean =
        locale.language.equals("ar", ignoreCase = true)

    fun formatFileSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
        val arabic = isArabicLocale(locale)
        if (bytes <= 0L) return if (arabic) "حجم غير معروف" else "Unknown size"
        val units = if (arabic) {
            arrayOf("بايت", "ك.ب", "م.ب", "ج.ب", "ت.ب")
        } else {
            arrayOf("B", "KB", "MB", "GB", "TB")
        }
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes ${units[0]}"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDateTime(millis: Long, locale: Locale = Locale.getDefault()): String {
        if (millis <= 0L) return if (isArabicLocale(locale)) "غير معروف" else "Unknown"
        val pattern = if (isArabicLocale(locale)) "dd MMM yyyy • h:mm a" else "MMM dd, yyyy • h:mm a"
        return SimpleDateFormat(pattern, locale).format(Date(millis))
    }

    fun formatShortDateTime(millis: Long, locale: Locale = Locale.getDefault()): String {
        if (millis <= 0L) return if (isArabicLocale(locale)) "غير معروف" else "Unknown"
        val pattern = if (isArabicLocale(locale)) "dd MMM، h:mm a" else "MMM dd, h:mm a"
        return SimpleDateFormat(pattern, locale).format(Date(millis))
    }

    fun formatMediaTimestamp(
        millis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        locale: Locale = Locale.getDefault()
    ): String {
        if (millis <= 0L) return ""
        val arabic = isArabicLocale(locale)
        val diff = nowMillis - millis
        return when {
            diff < TimeUnit.HOURS.toMillis(24) -> SimpleDateFormat("h:mm a", locale).format(Date(millis))
            diff < TimeUnit.HOURS.toMillis(48) -> if (arabic) "أمس" else "Yesterday"
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                if (days <= 7) {
                    if (arabic) "منذ $days أيام" else "$days days ago"
                } else {
                    formatShortDateTime(millis, locale)
                }
            }
        }
    }

    fun formatVideoDuration(durationMillis: Long): String {
        if (durationMillis <= 0L) return "0:00"
        val totalSeconds = durationMillis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatCountdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "00:00:00"
        val totalSeconds = remainingMillis / 1000L
        val days = totalSeconds / 86400L
        val hours = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (days > 0) {
            String.format(Locale.US, "%dd %02dh %02dm %02ds", days, hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    fun formatRemainingReadable(remainingMillis: Long, locale: Locale = Locale.getDefault()): String {
        val arabic = isArabicLocale(locale)
        if (remainingMillis <= 0L) return if (arabic) "منتهي" else "Expired"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
        val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)
        return if (arabic) {
            when {
                days >= 1 -> "متبقي $days ${if (days == 1L) "يوم" else "أيام"}"
                hours >= 1 -> "متبقي $hours ${if (hours == 1L) "ساعة" else "ساعات"}"
                minutes >= 1 -> "متبقي $minutes ${if (minutes == 1L) "دقيقة" else "دقائق"}"
                else -> "متبقي ${(remainingMillis / 1000L).coerceAtLeast(1L)} ث"
            }
        } else {
            when {
                days >= 1 -> "$days ${if (days == 1L) "day" else "days"} left"
                hours >= 1 -> "$hours ${if (hours == 1L) "hour" else "hours"} left"
                minutes >= 1 -> "$minutes ${if (minutes == 1L) "min" else "mins"} left"
                else -> "${(remainingMillis / 1000L).coerceAtLeast(1L)}s left"
            }
        }
    }
}
