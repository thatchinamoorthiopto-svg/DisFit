package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.Calibration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("Facial Measurement", appName)
    }

    @Test
    fun `test calibration calculation`() {
        val valMm = Calibration.calculateMmFromPixels(246.8f, 0.124f)
        assertEquals(30.6f, valMm, 0.2f)
        assertTrue(Calibration.isRightMpdNormal(30.6f))
    }
}
