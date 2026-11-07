package com.example

import androidx.compose.ui.graphics.Color
import com.example.model.DrawingTool
import com.example.model.PaperTemplate
import com.example.model.ScreenCoveragePreset
import com.example.util.ColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `verify studio presets and color harmonies`() {
        val vermilion = Color(0xFFE85D04)
        val hex = ColorMath.toHexCode(vermilion)
        assertEquals("#E85D04", hex)

        val harmonies = ColorMath.generateHarmonies(vermilion)
        assertEquals(5, harmonies.size)

        assertTrue(DrawingTool.entries.isNotEmpty())
        assertTrue(PaperTemplate.entries.size >= 6)
        assertEquals(0.965f, ScreenCoveragePreset.MAX_IMMERSION.targetCanvasFloat, 0.001f)
    }
}
