package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CueMaster", appName)
    }

    @Test
    fun `test time formatting`() {
        assertEquals("00:00.00", TimeFormatter.formatMs(0L))
        assertEquals("01:23.45", TimeFormatter.formatMs(83450L))
        assertEquals("01:23", TimeFormatter.formatDuration(83450L))
    }
}
