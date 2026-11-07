package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Muted, Earthy, Non-Neon Tactile Atelier Pigments
val SumiInk = Color(0xFF232625)
val WarmGraphite = Color(0xFF3E4240)
val MutedSienna = Color(0xFF8C5843)
val BotanicalSage = Color(0xFF526B5C)
val PrussianSlate = Color(0xFF3D5265)
val OchreClay = Color(0xFFA67C52)
val BurgundyUmber = Color(0xFF7A3E48)
val OliveMoss = Color(0xFF646E57)

// Backwards-compatible semantic aliases (mapped to refined non-neon studio tones)
val StudioObsidian = Color(0xFF1E1D1B)
val StudioCharcoal = Color(0xFF282623)
val StudioSlate = Color(0xFF35322E)
val StudioBorder = Color(0xFF46423D)

val ArchivalCream = Color(0xFFFBF9F5)
val WarmAlabaster = Color(0xFFF4F1EA)
val DraftingPaperBorder = Color(0xFFDCD6CA)

val VermilionAccent = MutedSienna
val VermilionLight = Color(0xFFA46D57)
val AmberGold = OchreClay
val TealStudio = BotanicalSage
val CobaltInk = PrussianSlate
val IndigoDeep = Color(0xFF344154)
val CrimsonInk = BurgundyUmber
val EmeraldInk = BotanicalSage
val VioletInk = Color(0xFF5D536B)
val RoseInk = Color(0xFF8A5A64)

val TextPrimaryDark = Color(0xFFECE8E1)
val TextSecondaryDark = Color(0xFFA8A298)
val TextPrimaryLight = Color(0xFF222423)
val TextSecondaryLight = Color(0xFF636664)

enum class StudioThemeId(
    val title: String,
    val subtitle: String,
    val isDark: Boolean
) {
    ATELIER_LINEN(
        title = "Atelier Linen",
        subtitle = "Warm archival stone, sumi ink & muted sienna",
        isDark = false
    ),
    NORDIC_STONE(
        title = "Nordic Limestone",
        subtitle = "Architectural cool stone, fjord slate & eucalyptus",
        isDark = false
    ),
    ESPRESSO_ARCHIVE(
        title = "Espresso Archive",
        subtitle = "Warm walnut umber, parchment & aged brass",
        isDark = true
    ),
    MONOCHROME_INK(
        title = "Editorial E-Ink",
        subtitle = "High-contrast distraction-free monochrome studio",
        isDark = false
    )
}

@Immutable
data class StudioColorPalette(
    val themeId: StudioThemeId,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val badgeSurface: Color
)

fun paletteForStudioTheme(themeId: StudioThemeId): StudioColorPalette {
    return when (themeId) {
        StudioThemeId.ATELIER_LINEN -> StudioColorPalette(
            themeId = themeId,
            background = Color(0xFFF3EFE8),
            surface = Color(0xFFFAF8F4),
            surfaceElevated = Color(0xFFEBE5DB),
            border = Color(0xFFD8D0C4),
            primary = Color(0xFF2C302E),      // Deep Sumi Charcoal (replaces harsh orange)
            onPrimary = Color(0xFFFAF8F4),
            secondary = Color(0xFF526B5C),    // Botanical Sage
            accent = Color(0xFF8C5843),       // Muted Terracotta Sienna
            textPrimary = Color(0xFF222423),
            textSecondary = Color(0xFF66635E),
            badgeSurface = Color(0xFFE5DFD4)
        )

        StudioThemeId.NORDIC_STONE -> StudioColorPalette(
            themeId = themeId,
            background = Color(0xFFEEF1F0),
            surface = Color(0xFFF8FAFA),
            surfaceElevated = Color(0xFFE2E7E6),
            border = Color(0xFFCBD2D0),
            primary = Color(0xFF2B3A42),      // Deep Fjord Slate
            onPrimary = Color(0xFFF8FAFA),
            secondary = Color(0xFF4F6D66),    // Muted Eucalyptus
            accent = Color(0xFF7A6352),       // Driftwood Umber
            textPrimary = Color(0xFF1F272B),
            textSecondary = Color(0xFF5C686D),
            badgeSurface = Color(0xFFDCE3E1)
        )

        StudioThemeId.ESPRESSO_ARCHIVE -> StudioColorPalette(
            themeId = themeId,
            background = Color(0xFF1C1A18),
            surface = Color(0xFF262320),
            surfaceElevated = Color(0xFF332F2B),
            border = Color(0xFF443E39),
            primary = Color(0xFF8C6249),      // Warm Roasted Terracotta
            onPrimary = Color(0xFFFAF6F0),
            secondary = Color(0xFF6B7F71),    // Muted Lichen Green
            accent = Color(0xFFB5926B),       // Aged Brass Ochre
            textPrimary = Color(0xFFEDE8E1),
            textSecondary = Color(0xFFA69F95),
            badgeSurface = Color(0xFF2E2A27)
        )

        StudioThemeId.MONOCHROME_INK -> StudioColorPalette(
            themeId = themeId,
            background = Color(0xFFF5F5F3),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFEAEAE6),
            border = Color(0xFFD0D0CA),
            primary = Color(0xFF1C1C1B),      // Pure Carbon Ink
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF4A4A48),    // Graphite Midtone
            accent = Color(0xFF6E6E6A),       // Warm Pewter
            textPrimary = Color(0xFF141413),
            textSecondary = Color(0xFF61615E),
            badgeSurface = Color(0xFFE4E4DF)
        )
    }
}

val LocalStudioPalette = staticCompositionLocalOf {
    paletteForStudioTheme(StudioThemeId.ATELIER_LINEN)
}
