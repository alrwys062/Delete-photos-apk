package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TimeDelete", appName)
  }

  @Test
  fun `format countdown and file size accurately`() {
    assertEquals("01:02:03", com.example.util.FormatUtils.formatCountdown(3_723_000L))
    assertEquals("1d 02h 00m 00s", com.example.util.FormatUtils.formatCountdown(93_600_000L))
    assertEquals("2.0 MB", com.example.util.FormatUtils.formatFileSize(2L * 1024L * 1024L))
  }
}
