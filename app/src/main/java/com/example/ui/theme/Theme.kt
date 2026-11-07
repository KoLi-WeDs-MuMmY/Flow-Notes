package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = VermilionAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF43200A),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = TealStudio,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF113D38),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = AmberGold,
    onTertiary = StudioObsidian,
    background = StudioObsidian,
    onBackground = TextPrimaryDark,
    surface = StudioCharcoal,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSlate,
    onSurfaceVariant = TextSecondaryDark,
    outline = StudioBorder
)

private val StudioLightColorScheme = lightColorScheme(
    primary = VermilionAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDE5),
    onPrimaryContainer = Color(0xFF5C1F00),
    secondary = TealStudio,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = AmberGold,
    onTertiary = StudioObsidian,
    background = WarmAlabaster,
    onBackground = TextPrimaryLight,
    surface = ArchivalCream,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE7E2D8),
    onSurfaceVariant = TextSecondaryLight,
    outline = DraftingPaperBorder
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
