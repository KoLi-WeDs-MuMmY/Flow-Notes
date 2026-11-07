package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MyApplicationTheme(
    themeId: StudioThemeId = StudioThemeId.ATELIER_LINEN,
    cornerRadiusDp: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val rawPalette = paletteForStudioTheme(themeId)

    val animSpec = spring<Color>(stiffness = Spring.StiffnessMediumLow)
    val animatedBg by animateColorAsState(rawPalette.background, animSpec, label = "bg")
    val animatedSurface by animateColorAsState(rawPalette.surface, animSpec, label = "surface")
    val animatedElevated by animateColorAsState(rawPalette.surfaceElevated, animSpec, label = "elevated")
    val animatedBorder by animateColorAsState(rawPalette.border, animSpec, label = "border")
    val animatedPrimary by animateColorAsState(rawPalette.primary, animSpec, label = "primary")
    val animatedSecondary by animateColorAsState(rawPalette.secondary, animSpec, label = "secondary")
    val animatedAccent by animateColorAsState(rawPalette.accent, animSpec, label = "accent")
    val animatedTextPrimary by animateColorAsState(rawPalette.textPrimary, animSpec, label = "textPrimary")
    val animatedTextSecondary by animateColorAsState(rawPalette.textSecondary, animSpec, label = "textSecondary")

    val animatedPalette = rawPalette.copy(
        background = animatedBg,
        surface = animatedSurface,
        surfaceElevated = animatedElevated,
        border = animatedBorder,
        primary = animatedPrimary,
        secondary = animatedSecondary,
        accent = animatedAccent,
        textPrimary = animatedTextPrimary,
        textSecondary = animatedTextSecondary
    )

    val colorScheme = if (themeId.isDark) {
        darkColorScheme(
            primary = animatedPalette.primary,
            onPrimary = animatedPalette.onPrimary,
            secondary = animatedPalette.secondary,
            tertiary = animatedPalette.accent,
            background = animatedPalette.background,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.surface,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.surfaceElevated,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.border
        )
    } else {
        lightColorScheme(
            primary = animatedPalette.primary,
            onPrimary = animatedPalette.onPrimary,
            secondary = animatedPalette.secondary,
            tertiary = animatedPalette.accent,
            background = animatedPalette.background,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.surface,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.surfaceElevated,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.border
        )
    }

    val shapes = Shapes(
        extraSmall = RoundedCornerShape((cornerRadiusDp * 0.4f).coerceAtLeast(4.dp)),
        small = RoundedCornerShape((cornerRadiusDp * 0.65f).coerceAtLeast(6.dp)),
        medium = RoundedCornerShape(cornerRadiusDp),
        large = RoundedCornerShape(cornerRadiusDp * 1.25f),
        extraLarge = RoundedCornerShape(cornerRadiusDp * 1.6f)
    )

    CompositionLocalProvider(LocalStudioPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
