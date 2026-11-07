package com.example.model

import androidx.compose.ui.graphics.Color

enum class DrawingTool(
    val label: String,
    val shortCode: String,
    val defaultWidth: Float,
    val defaultAlpha: Float,
    val isShape: Boolean = false
) {
    FOUNTAIN_PEN("Fountain Nib", "NIB", 5.5f, 1.0f),
    FINELINER("Fine Liner", "INK", 2.8f, 1.0f),
    CHISEL_MARKER("Highlighter", "MRK", 22.0f, 0.38f),
    ART_BRUSH("Studio Brush", "BRS", 14.0f, 0.88f),
    CHARCOAL("Charcoal", "CHR", 9.5f, 0.72f),
    RULER_LINE("Ruler Line", "LIN", 4.0f, 1.0f, isShape = true),
    RECTANGLE_SHAPE("Rectangle", "RCT", 4.0f, 1.0f, isShape = true),
    CIRCLE_SHAPE("Circle", "CIR", 4.0f, 1.0f, isShape = true),
    ERASER("Eraser", "ERS", 26.0f, 1.0f)
}

enum class PaperTemplate(
    val title: String,
    val subtitle: String,
    val backgroundColorHex: Long,
    val lineColorHex: Long,
    val isDark: Boolean = false
) {
    WARM_ARCHIVAL(
        title = "Archival Cream",
        subtitle = "Smooth heavyweight sketch paper",
        backgroundColorHex = 0xFFFBF9F5,
        lineColorHex = 0xFFE5E0D5
    ),
    RULED_NOTEBOOK(
        title = "Ruled Writing",
        subtitle = "College-ruled lines with margin for handwriting",
        backgroundColorHex = 0xFFFCFBF7,
        lineColorHex = 0xFFBFD4E8
    ),
    DOT_MATRIX(
        title = "5mm Dot Grid",
        subtitle = "Precision bullet journal dot matrix",
        backgroundColorHex = 0xFFF9F8F3,
        lineColorHex = 0xFFC8C2B4
    ),
    GRAPH_ENGINEERING(
        title = "Quad Graph",
        subtitle = "Engineering major & minor square grid",
        backgroundColorHex = 0xFFF6F9FC,
        lineColorHex = 0xFFD0DFEE
    ),
    ISOMETRIC_DRAFT(
        title = "Isometric 3D",
        subtitle = "30° architectural axonometric grid",
        backgroundColorHex = 0xFFF7F8FA,
        lineColorHex = 0xFFD4DAE4
    ),
    DARK_SLATE(
        title = "Midnight Slate",
        subtitle = "High-contrast dark studio canvas",
        backgroundColorHex = 0xFF14161B,
        lineColorHex = 0xFF282C37,
        isDark = true
    )
}

enum class ScreenCoveragePreset(
    val label: String,
    val targetCanvasFloat: Float,
    val description: String
) {
    MAX_IMMERSION(
        label = "Max Immersion (0.96f)",
        targetCanvasFloat = 0.965f,
        description = "96.5% screen cover • Compact floating edge pills"
    ),
    STUDIO_BALANCED(
        label = "Studio Balanced (0.90f)",
        targetCanvasFloat = 0.90f,
        description = "90.0% screen cover • Adaptive floating toolbars"
    ),
    DRAFTING_SPLIT(
        label = "Inspector Split (0.78f)",
        targetCanvasFloat = 0.78f,
        description = "78.0% screen cover • Docked color & telemetry panel"
    ),
    CUSTOM_FLOAT(
        label = "Custom Float",
        targetCanvasFloat = 0.88f,
        description = "User-adjusted float fraction allocation"
    )
}

enum class DevicePosture(
    val displayName: String,
    val badgeCode: String
) {
    MOBILE_PORTRAIT("Mobile Portrait", "MOB-P"),
    MOBILE_LANDSCAPE("Mobile Landscape", "MOB-L"),
    FOLDABLE_MEDIUM("Foldable / Dual-Pane", "FOLD"),
    TABLET_EXPANDED("Tablet Studio Deck", "TAB-XL")
}

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

data class DrawnStroke(
    val id: String,
    val points: List<StrokePoint>,
    val colorArgb: Long,
    val strokeWidth: Float,
    val alpha: Float,
    val tool: DrawingTool
)

data class CanvasTextNote(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val colorArgb: Long,
    val fontSizeSp: Float = 20f
)

data class ScreenFloatMetrics(
    val screenWidthDp: Float,
    val screenHeightDp: Float,
    val aspectRatioFloat: Float,
    val posture: DevicePosture,
    val effectiveCanvasCoverageFloat: Float,
    val hudCoverageFloat: Float,
    val topBarHeightFraction: Float,
    val bottomDockWidthFraction: Float,
    val sideRailWidthFraction: Float,
    val inspectorWidthFraction: Float
) {
    val canvasCoveragePercent: Float
        get() = (effectiveCanvasCoverageFloat * 100f).coerceIn(50f, 99.5f)

    val hudCoveragePercent: Float
        get() = (100f - canvasCoveragePercent).coerceAtLeast(0.5f)
}

object StudioPalettes {
    val CuratedSwatches: List<Color> = listOf(
        Color(0xFF18181B), // Obsidian Ink
        Color(0xFFE85D04), // Vermilion Studio
        Color(0xFF1D4ED8), // Cobalt Fountain Ink
        Color(0xFF059669), // Emerald Drafting
        Color(0xFFD97706), // Ochre Gold
        Color(0xFFDC2626), // Crimson Red
        Color(0xFF7C3AED), // Ultramarine Violet
        Color(0xFF0D9488), // Teal Cyan
        Color(0xFFDB2777), // Magenta Rose
        Color(0xFF4F46E5), // Indigo Night
        Color(0xFF57534E), // Warm Graphite
        Color(0xFFF8FAFC)  // Titanium White
    )
}
