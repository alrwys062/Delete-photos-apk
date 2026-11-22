package com.example

import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TimeDeleteLogicUnitTest {

    @Test
    fun `formatCountdown formats hours and days accurately`() {
        assertEquals("00:00:00", FormatUtils.formatCountdown(0L))
        assertEquals("00:00:00", FormatUtils.formatCountdown(-5000L))
        assertEquals("01:02:03", FormatUtils.formatCountdown(3_723_000L))
        assertEquals("1d 02h 00m 00s", FormatUtils.formatCountdown(93_600_000L))
    }

    @Test
    fun `formatFileSize formats bytes, KB, and MB in English and Arabic`() {
        assertEquals("Unknown size", FormatUtils.formatFileSize(0L, Locale.US))
        assertEquals("512 B", FormatUtils.formatFileSize(512L, Locale.US))
        assertEquals("1.0 KB", FormatUtils.formatFileSize(1024L, Locale.US))
        assertEquals("2.0 MB", FormatUtils.formatFileSize(2L * 1024L * 1024L, Locale.US))

        val arabic = Locale("ar")
        assertEquals("حجم غير معروف", FormatUtils.formatFileSize(0L, arabic))
        assertEquals("512 بايت", FormatUtils.formatFileSize(512L, arabic))
        assertEquals("1.0 ك.ب", FormatUtils.formatFileSize(1024L, arabic))
        assertEquals("2.0 م.ب", FormatUtils.formatFileSize(2L * 1024L * 1024L, arabic))
    }

    @Test
    fun `formatVideoDuration formats mm_ss and hh_mm_ss`() {
        assertEquals("0:00", FormatUtils.formatVideoDuration(0L))
        assertEquals("0:45", FormatUtils.formatVideoDuration(45_000L))
        assertEquals("2:05", FormatUtils.formatVideoDuration(125_000L))
        assertEquals("1:01:05", FormatUtils.formatVideoDuration(3_665_000L))
    }

    @Test
    fun `formatRemainingReadable handles seconds, minutes, hours, and days in English and Arabic`() {
        assertEquals("Expired", FormatUtils.formatRemainingReadable(0L, Locale.US))
        assertEquals("30s left", FormatUtils.formatRemainingReadable(30_000L, Locale.US))
        assertEquals("15 mins left", FormatUtils.formatRemainingReadable(15 * 60_000L, Locale.US))
        assertEquals("1 hour left", FormatUtils.formatRemainingReadable(60 * 60_000L, Locale.US))
        assertEquals("3 days left", FormatUtils.formatRemainingReadable(3 * 24 * 60 * 60_000L, Locale.US))

        val arabic = Locale("ar")
        assertEquals("منتهي", FormatUtils.formatRemainingReadable(0L, arabic))
        assertEquals("متبقي 30 ث", FormatUtils.formatRemainingReadable(30_000L, arabic))
        assertEquals("متبقي 15 دقائق", FormatUtils.formatRemainingReadable(15 * 60_000L, arabic))
        assertEquals("متبقي 1 ساعة", FormatUtils.formatRemainingReadable(60 * 60_000L, arabic))
        assertEquals("متبقي 3 أيام", FormatUtils.formatRemainingReadable(3 * 24 * 60 * 60_000L, arabic))
    }

    @Test
    fun `formatMediaTimestamp formats relative timestamps in English and Arabic`() {
        val now = 1_700_000_000_000L
        val yesterday = now - 30L * 60L * 60L * 1000L
        val threeDaysAgo = now - 3L * 24L * 60L * 60L * 1000L

        assertEquals("Yesterday", FormatUtils.formatMediaTimestamp(yesterday, now, Locale.US))
        assertEquals("3 days ago", FormatUtils.formatMediaTimestamp(threeDaysAgo, now, Locale.US))

        val arabic = Locale("ar")
        assertEquals("أمس", FormatUtils.formatMediaTimestamp(yesterday, now, arabic))
        assertEquals("منذ 3 أيام", FormatUtils.formatMediaTimestamp(threeDaysAgo, now, arabic))
    }
}
