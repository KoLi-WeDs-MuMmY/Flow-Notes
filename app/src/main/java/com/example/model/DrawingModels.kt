package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.ui.theme.StudioThemeId
import kotlin.math.max

enum class AppDestination {
    HOME_EXPLORER,
    STUDIO_CANVAS
}

enum class HomeFilterMode(val label: String) {
    EXPLORER("Folders & Sheets"),
    FAVORITES("Starred"),
    RECENTS("Recent Activity")
}

enum class FolderIconType(val label: String, val symbolGlyph: String) {
    FOLDER("Atelier Folder", "▣"),
    ARCHITECTURE("Drafting & CAD", "⌖"),
    NOTES("Manuscript Notes", "¶"),
    PALETTE("Color & Wash", "◈"),
    GEOMETRY("Axonometric", "△"),
    BOOKMARK("Curated Folio", "§"),
    SCIENCE("Lab & Equations", "∑"),
    JOURNAL("Daily Sketchbook", "✦"),
    ARCHIVE("Vault Archive", "❖"),
    LIGHTBULB("Concept Studies", "☼")
}

enum class SymbolStampType(
    val label: String,
    val glyph: String,
    val defaultSizeSp: Float = 24f
) {
    CHECK_SEAL("Verified", "✓ Check"),
    STAR_BADGE("Key Point", "★ Key"),
    PIN_MARKER("Location Pin", "⌖ Pin"),
    ARROW_NORTH("North Axis", "↑ N 0°"),
    IDEA_SPARK("Insight", "✦ Insight"),
    WARNING_TAG("Attention", "⚠ Review"),
    MATH_SIGMA("Summation", "∑ f(x)"),
    ANGLE_ARC("90° Right Angle", "∟ 90°"),
    QUOTE_MARK("Editorial Note", "❝ Note ❞"),
    FOCUS_BRACKET("Section Callout", "⟦ Section A ⟧"),
    ORIGIN_AXIS("XY Origin", "┼ (0,0)"),
    DIAMOND_NODE("Milestone", "◆ Node")
}

enum class FloatingLayoutPreset(
    val title: String,
    val subtitle: String
) {
    ATELIER_BALANCED("Atelier Pods", "Independent floating tool, color & symbol pods"),
    LEFT_HANDED_STUDIO("Left-Handed Deck", "Tools & symbols docked left, colors bottom-right"),
    RIGHT_HANDED_STUDIO("Right-Handed Deck", "Tools & symbols docked right, colors bottom-left"),
    MINIMAL_CORNERS("Minimalist Corners", "Compact corner pods with 96% canvas visibility")
}

enum class DrawingTool(
    val label: String,
    val shortCode: String,
    val symbolGlyph: String,
    val defaultWidth: Float,
    val defaultAlpha: Float,
    val isShape: Boolean = false
) {
    FOUNTAIN_PEN("Fountain Nib", "NIB", "✒", 5.2f, 1.0f),
    FINELINER("Technical Pen", "0.3", "∣", 2.6f, 1.0f),
    CHISEL_MARKER("Wash Marker", "MRK", "▱", 20.0f, 0.36f),
    ART_BRUSH("Sable Brush", "BRS", "∿", 14.0f, 0.88f),
    CHARCOAL("Willow Charcoal", "CHR", "░", 9.5f, 0.72f),
    RULER_LINE("Drafting Line", "LIN", "╱", 3.5f, 1.0f, isShape = true),
    RECTANGLE_SHAPE("Frame Box", "RCT", "▭", 3.5f, 1.0f, isShape = true),
    CIRCLE_SHAPE("Compass Circle", "CIR", "◯", 3.5f, 1.0f, isShape = true),
    ERASER("Kneaded Eraser", "ERS", "⌫", 26.0f, 1.0f)
}

enum class PaperTemplate(
    val title: String,
    val subtitle: String,
    val symbolGlyph: String,
    val backgroundColorHex: Long,
    val lineColorHex: Long,
    val isDark: Boolean = false
) {
    WARM_ARCHIVAL(
        title = "Fabriano Cream",
        subtitle = "Heavyweight cotton rag sketch paper",
        symbolGlyph = "□",
        backgroundColorHex = 0xFFFBF9F4,
        lineColorHex = 0xFFE3DDD1
    ),
    RULED_NOTEBOOK(
        title = "Manuscript Ruled",
        subtitle = "Archival ruled lines with sienna margin",
        symbolGlyph = "≡",
        backgroundColorHex = 0xFFFAF8F2,
        lineColorHex = 0xFFD2DCE4
    ),
    DOT_MATRIX(
        title = "5mm Architectural Dot",
        subtitle = "Precision dot matrix for lettering & CAD",
        symbolGlyph = "∷",
        backgroundColorHex = 0xFFF8F6F0,
        lineColorHex = 0xFFC5BEB0
    ),
    GRAPH_ENGINEERING(
        title = "Engineering Quad",
        subtitle = "Major & minor millimeter coordinate grid",
        symbolGlyph = "⊞",
        backgroundColorHex = 0xFFF5F7F6,
        lineColorHex = 0xFFD1DBD7
    ),
    ISOMETRIC_DRAFT(
        title = "Isometric 30° Grid",
        subtitle = "Axonometric structural drafting matrix",
        symbolGlyph = "⬡",
        backgroundColorHex = 0xFFF6F6F4,
        lineColorHex = 0xFFD4D6D0
    ),
    DARK_SLATE(
        title = "Espresso Blueprint",
        subtitle = "Warm dark umber slate for light chalk & ochre",
        symbolGlyph = "■",
        backgroundColorHex = 0xFF1F1D1B,
        lineColorHex = 0xFF33302C,
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
        description = "90.0% screen cover • Modular floating studio pods"
    ),
    DRAFTING_SPLIT(
        label = "Inspector Split (0.78f)",
        targetCanvasFloat = 0.78f,
        description = "78.0% screen cover • Docked color & calibration deck"
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

@Immutable
data class StudioCustomization(
    val themeId: StudioThemeId = StudioThemeId.ATELIER_LINEN,
    val cornerRadiusDp: Float = 16f,
    val widgetOpacityFloat: Float = 0.95f,
    val gridSpacingMultiplierFloat: Float = 1.0f,
    val gridLineOpacityFloat: Float = 0.80f,
    val layoutPreset: FloatingLayoutPreset = FloatingLayoutPreset.ATELIER_BALANCED,
    // Independent Floating Widget Visibility Toggles
    val showToolPod: Boolean = true,
    val isToolPodCompact: Boolean = false,
    val showColorPod: Boolean = true,
    val showZoomCompassPod: Boolean = true,
    val showSymbolStampPod: Boolean = true,
    val showScratchpadPod: Boolean = false,
    val showRulerGuideOverlay: Boolean = false,
    val rulerAngleDegrees: Float = 0f,
    val rulerYFraction: Float = 0.50f,
    val scratchpadNoteText: String = "• Sketch elevation proportions\n• Check nib weight (5.2px)\n• Stamp North axis & scale"
)

@Immutable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

@Immutable
data class DrawnStroke(
    val id: String,
    val points: List<StrokePoint>,
    val colorArgb: Long,
    val strokeWidth: Float,
    val alpha: Float,
    val tool: DrawingTool
) {
    val cachedPath: Path by lazy(LazyThreadSafetyMode.NONE) {
        buildCachedPath(points, tool)
    }
    val bounds: Rect = computeStrokeBounds(points, strokeWidth)

    companion object {
        fun buildCachedPath(points: List<StrokePoint>, tool: DrawingTool): Path {
            val path = Path()
            if (points.isEmpty() || tool.isShape) return path
            val first = points.first()
            path.moveTo(first.x, first.y)

            if (points.size == 1) {
                path.lineTo(first.x + 0.5f, first.y + 0.5f)
                return path
            }

            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) * 0.5f
                val midY = (prev.y + curr.y) * 0.5f
                path.quadraticTo(prev.x, prev.y, midX, midY)
            }
            val last = points.last()
            path.lineTo(last.x, last.y)
            return path
        }

        fun computeStrokeBounds(points: List<StrokePoint>, strokeWidth: Float): Rect {
            if (points.isEmpty()) return Rect.Zero
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            for (i in points.indices) {
                val p = points[i]
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
            val pad = max(strokeWidth * 1.6f, 12f)
            return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
        }
    }
}

@Immutable
data class CanvasTextNote(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val colorArgb: Long,
    val fontSizeSp: Float = 20f
)

@Immutable
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
    val inspectorWidthFraction: Float,
    val homeSidebarWidthFraction: Float,
    val homeContentWidthFraction: Float,
    val homeGridColumns: Int
) {
    val canvasCoveragePercent: Float
        get() = (effectiveCanvasCoverageFloat * 100f).coerceIn(50f, 99.5f)

    val hudCoveragePercent: Float
        get() = (100f - canvasCoveragePercent).coerceAtLeast(0.5f)
}

object StudioPalettes {
    // Non-Neon Archival Artist & Drafting Pigments
    val CuratedSwatches: List<Color> = listOf(
        Color(0xFF232625), // Sumi Carbon Ink
        Color(0xFF8C5843), // Burnt Terracotta Sienna
        Color(0xFF3D5265), // Prussian Drafting Blue
        Color(0xFF526B5C), // Botanical Sage Green
        Color(0xFFA67C52), // Raw Roman Ochre
        Color(0xFF7A3E48), // Venetian Burgundy
        Color(0xFF5D536B), // Muted Mineral Violet
        Color(0xFF4F6D66), // Deep Eucalyptus
        Color(0xFF646E57), // Olive Umber
        Color(0xFF5A524C), // Warm Sepia Walnut
        Color(0xFF787D82), // Architectural Pewter
        Color(0xFFF6F4EE)  // Unbleached Titanium Chalk
    )

    val FolderAccentSwatches: List<Long> = listOf(
        0xFF8C5843L, // Muted Sienna
        0xFF526B5CL, // Botanical Sage
        0xFF3D5265L, // Prussian Slate
        0xFFA67C52L, // Roman Ochre
        0xFF7A3E48L, // Venetian Umber
        0xFF5D536BL, // Mineral Violet
        0xFF646E57L, // Olive Moss
        0xFF4A4E4DL  // Sumi Graphite
    )
}
