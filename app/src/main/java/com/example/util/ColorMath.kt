package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class HsvColor(
    val hue: Float,        // 0f..360f
    val saturation: Float, // 0f..1f
    val value: Float,      // 0f..1f
    val alpha: Float = 1f  // 0f..1f
) {
    fun toComposeColor(): Color {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
        val m = v - c

        val (rPrime, gPrime, bPrime) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }
}

object ColorMath {
    fun colorToHsv(color: Color): HsvColor {
        val r = color.red
        val g = color.green
        val b = color.blue
        val maxComp = max(r, max(g, b))
        val minComp = min(r, min(g, b))
        val delta = maxComp - minComp

        val hue = when {
            delta == 0f -> 0f
            maxComp == r -> 60f * (((g - b) / delta) % 6f)
            maxComp == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val saturation = if (maxComp == 0f) 0f else (delta / maxComp)
        val value = maxComp

        return HsvColor(
            hue = hue.coerceIn(0f, 360f),
            saturation = saturation.coerceIn(0f, 1f),
            value = value.coerceIn(0f, 1f),
            alpha = color.alpha.coerceIn(0f, 1f)
        )
    }

    fun toHexCode(color: Color, includeAlpha: Boolean = false): String {
        val r = (color.red * 255).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
        return if (includeAlpha) {
            val a = (color.alpha * 255).roundToInt().coerceIn(0, 255)
            String.format("#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }

    fun parseHexColor(hexInput: String): Color? {
        val cleaned = hexInput.trim().removePrefix("#")
        if (cleaned.length != 6 && cleaned.length != 8) return null
        return try {
            val parsedLong = cleaned.toLong(16)
            if (cleaned.length == 6) {
                val r = ((parsedLong shr 16) and 0xFF).toInt()
                val g = ((parsedLong shr 8) and 0xFF).toInt()
                val b = (parsedLong and 0xFF).toInt()
                Color(r, g, b)
            } else {
                val a = ((parsedLong shr 24) and 0xFF).toInt()
                val r = ((parsedLong shr 16) and 0xFF).toInt()
                val g = ((parsedLong shr 8) and 0xFF).toInt()
                val b = (parsedLong and 0xFF).toInt()
                Color(r, g, b, a)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun generateHarmonies(baseColor: Color): List<Pair<String, Color>> {
        val hsv = colorToHsv(baseColor)
        val complement = hsv.copy(hue = (hsv.hue + 180f) % 360f).toComposeColor()
        val analogous1 = hsv.copy(hue = (hsv.hue + 32f) % 360f).toComposeColor()
        val analogous2 = hsv.copy(hue = (hsv.hue + 328f) % 360f).toComposeColor()
        val triadic1 = hsv.copy(hue = (hsv.hue + 120f) % 360f).toComposeColor()
        val triadic2 = hsv.copy(hue = (hsv.hue + 240f) % 360f).toComposeColor()
        return listOf(
            "Complement" to complement,
            "Analog +32°" to analogous1,
            "Analog -32°" to analogous2,
            "Triad +120°" to triadic1,
            "Triad +240°" to triadic2
        )
    }

    fun colorToArgbLong(color: Color): Long {
        return color.toArgb().toLong() and 0xFFFFFFFFL
    }

    fun argbLongToColor(argb: Long): Color {
        return Color(argb.toInt())
    }
}
