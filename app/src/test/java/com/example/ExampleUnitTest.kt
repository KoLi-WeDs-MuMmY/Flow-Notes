package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.SketchRepository
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.FloatingLayoutPreset
import com.example.model.FolderIconType
import com.example.model.StrokePoint
import com.example.model.StudioCustomization
import com.example.model.SymbolStampType
import com.example.ui.theme.StudioThemeId
import com.example.ui.theme.paletteForStudioTheme
import com.example.util.ColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun colorMath_hsvAndHexRoundTrip() {
        val sienna = Color(0xFF8C5843)
        val hex = ColorMath.toHexCode(sienna)
        assertEquals("#8C5843", hex)

        val parsed = ColorMath.parseHexColor("#8C5843")
        assertNotNull(parsed)
        assertEquals("#8C5843", ColorMath.toHexCode(parsed!!))

        val harmonies = ColorMath.generateHarmonies(sienna)
        assertEquals(5, harmonies.size)
    }

    @Test
    fun strokeSerialization_roundTripAndCachedBounds() {
        val original = listOf(
            DrawnStroke(
                id = "stroke-1",
                points = listOf(StrokePoint(12.5f, 44.25f, 0.9f), StrokePoint(80.0f, 120.75f, 1.1f)),
                colorArgb = 0xFF232625L,
                strokeWidth = 5.2f,
                alpha = 0.85f,
                tool = DrawingTool.FOUNTAIN_PEN
            )
        )
        assertFalse(original.first().bounds.isEmpty)
        val payload = SketchRepository.serializeStrokes(original)
        val restored = SketchRepository.deserializeStrokes(payload)
        assertEquals(1, restored.size)
        assertEquals(DrawingTool.FOUNTAIN_PEN, restored.first().tool)
        assertEquals(5.2f, restored.first().strokeWidth, 0.001f)
        assertTrue(restored.first().points.size == 2)
    }

    @Test
    fun studioThemes_symbolsAndFloatingPods() {
        assertEquals(4, StudioThemeId.entries.size)
        StudioThemeId.entries.forEach { themeId ->
            val pal = paletteForStudioTheme(themeId)
            assertNotNull(pal.primary)
            assertNotNull(pal.background)
        }
        assertTrue(FolderIconType.entries.size >= 10)
        assertTrue(SymbolStampType.entries.size >= 12)
        assertEquals(4, FloatingLayoutPreset.entries.size)
        val custom = StudioCustomization()
        assertEquals(StudioThemeId.ATELIER_LINEN, custom.themeId)
        assertTrue(custom.showToolPod && custom.showColorPod && custom.showSymbolStampPod)
    }
}
