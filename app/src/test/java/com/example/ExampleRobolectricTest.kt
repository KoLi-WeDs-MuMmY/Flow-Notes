package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.util.ColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify color math`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CanvasFlow", appName)

        val vermilion = Color(0xFFE85D04)
        val hex = ColorMath.toHexCode(vermilion)
        assertEquals("#E85D04", hex)

        val harmonies = ColorMath.generateHarmonies(vermilion)
        assertTrue(harmonies.size == 5)
    }
}
