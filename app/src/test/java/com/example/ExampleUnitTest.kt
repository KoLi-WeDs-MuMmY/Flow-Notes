package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.SketchRepository
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.StrokePoint
import com.example.util.ColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun colorMath_hsvAndHexRoundTrip() {
        val vermilion = Color(0xFFE85D04)
        val hex = ColorMath.toHexCode(vermilion)
        assertEquals("#E85D04", hex)

        val parsed = ColorMath.parseHexColor("#E85D04")
        assertNotNull(parsed)
        assertEquals("#E85D04", ColorMath.toHexCode(parsed!!))

        val harmonies = ColorMath.generateHarmonies(vermilion)
        assertEquals(5, harmonies.size)
    }

    @Test
    fun strokeSerialization_roundTripPreservesFloats() {
        val original = listOf(
            DrawnStroke(
                id = "stroke-1",
                points = listOf(StrokePoint(12.5f, 44.25f, 0.9f), StrokePoint(80.0f, 120.75f, 1.1f)),
                colorArgb = 0xFFE85D04L,
                strokeWidth = 5.5f,
                alpha = 0.85f,
                tool = DrawingTool.FOUNTAIN_PEN
            )
        )
        val payload = SketchRepository.serializeStrokes(original)
        val restored = SketchRepository.deserializeStrokes(payload)
        assertEquals(1, restored.size)
        assertEquals(DrawingTool.FOUNTAIN_PEN, restored.first().tool)
        assertEquals(5.5f, restored.first().strokeWidth, 0.001f)
        assertTrue(restored.first().points.size == 2)
    }
}
