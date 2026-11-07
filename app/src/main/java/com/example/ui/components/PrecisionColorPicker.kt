package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalStudioPalette
import com.example.util.ColorMath
import com.example.util.HsvColor
import kotlin.math.roundToInt

@Composable
fun PrecisionColorPickerDialog(
    activeColor: Color,
    hsvColor: HsvColor,
    strokeAlpha: Float,
    customSwatches: List<Color>,
    recentColors: List<Color>,
    onHsvChanged: (hue: Float, saturation: Float, value: Float, alpha: Float) -> Unit,
    onColorSelected: (Color) -> Unit,
    onSaveSwatch: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = palette.surfaceElevated,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, palette.border, MaterialTheme.shapes.large)
                .testTag("color_picker_dialog")
        ) {
            PrecisionColorPickerContent(
                activeColor = activeColor,
                hsvColor = hsvColor,
                strokeAlpha = strokeAlpha,
                customSwatches = customSwatches,
                recentColors = recentColors,
                onHsvChanged = onHsvChanged,
                onColorSelected = onColorSelected,
                onSaveSwatch = onSaveSwatch,
                onDismiss = onDismiss,
                isDockedPanel = false
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrecisionColorPickerContent(
    activeColor: Color,
    hsvColor: HsvColor,
    strokeAlpha: Float,
    customSwatches: List<Color>,
    recentColors: List<Color>,
    onHsvChanged: (hue: Float, saturation: Float, value: Float, alpha: Float) -> Unit,
    onColorSelected: (Color) -> Unit,
    onSaveSwatch: () -> Unit,
    onDismiss: () -> Unit,
    isDockedPanel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var hexInput by remember(activeColor) {
        mutableStateOf(ColorMath.toHexCode(activeColor))
    }

    LaunchedEffect(activeColor) {
        hexInput = ColorMath.toHexCode(activeColor)
    }

    val currentHsv by rememberUpdatedState(hsvColor)
    val currentAlpha by rememberUpdatedState(strokeAlpha)
    val currentCallback by rememberUpdatedState(onHsvChanged)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Pigment Studio",
                    tint = palette.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = if (isDockedPanel) "Studio Pigment Deck" else "Archival Pigment & HSV Mixer",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = String.format(
                            "◈ H: %.0f° • S: %.2ff • V: %.2ff • α: %.2ff",
                            hsvColor.hue,
                            hsvColor.saturation,
                            hsvColor.value,
                            strokeAlpha
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
            if (!isDockedPanel) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_color_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Color Picker",
                        tint = palette.textPrimary
                    )
                }
            }
        }

        // 1. 2D Saturation-Value (SV) Gradient Box (Float 0.0f..1.0f coordinates)
        val pureHueColor = remember(hsvColor.hue) {
            HsvColor(hsvColor.hue, 1f, 1f, 1f).toComposeColor()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isDockedPanel) 150.dp else 185.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                .testTag("sv_color_matrix_canvas")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width).coerceIn(0f, 1f)
                        val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        currentCallback(currentHsv.hue, s, v, currentAlpha)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val s = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        currentCallback(currentHsv.hue, s, v, currentAlpha)
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, pureHueColor)
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                val cx = (hsvColor.saturation * size.width).coerceIn(0f, size.width)
                val cy = ((1f - hsvColor.value) * size.height).coerceIn(0f, size.height)

                drawCircle(
                    color = Color.Black.copy(alpha = 0.65f),
                    radius = 12.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawCircle(
                    color = activeColor,
                    radius = 7.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // 2. 360° Hue Spectrum Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HUE SPECTRUM (0° - 360°)",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Text(
                    text = "${hsvColor.hue.roundToInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            val hueSpectrumColors = remember {
                listOf(
                    Color(0xFFC94A4A),
                    Color(0xFFD6A848),
                    Color(0xFF569E62),
                    Color(0xFF4A909E),
                    Color(0xFF4B62A8),
                    Color(0xFF96529A),
                    Color(0xFFC94A4A)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .testTag("hue_spectrum_bar")
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newHue = ((offset.x / size.width) * 360f).coerceIn(0f, 360f)
                            currentCallback(newHue, currentHsv.saturation, currentHsv.value, currentAlpha)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newHue = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                            currentCallback(newHue, currentHsv.saturation, currentHsv.value, currentAlpha)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(brush = Brush.horizontalGradient(hueSpectrumColors))
                    val thumbX = ((hsvColor.hue / 360f) * size.width).coerceIn(8f, size.width - 8f)
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = Offset(thumbX, size.height / 2f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // 3. Opacity / Alpha Float Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "INK OPACITY FLOAT (0.05f - 1.00f)",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Text(
                    text = String.format("%.2ff (%d%%)", strokeAlpha, (strokeAlpha * 100).roundToInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = strokeAlpha,
                onValueChange = { newAlpha ->
                    onHsvChanged(hsvColor.hue, hsvColor.saturation, hsvColor.value, newAlpha)
                },
                valueRange = 0.05f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = palette.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("alpha_opacity_slider")
            )
        }

        // 4. Active Color Preview + Hex Code Input + Save Swatch Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeColor.copy(alpha = strokeAlpha))
                    .border(2.dp, palette.border, RoundedCornerShape(12.dp))
            )

            OutlinedTextField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = input.uppercase()
                    ColorMath.parseHexColor(input)?.let { parsed ->
                        onColorSelected(parsed)
                    }
                },
                label = { Text("HEX Pigment", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.labelLarge.copy(color = palette.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border,
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("hex_color_input")
            )

            Button(
                onClick = onSaveSwatch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.badgeSurface,
                    contentColor = palette.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("save_swatch_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Save Swatch",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", style = MaterialTheme.typography.labelMedium)
            }
        }

        // 5. Automatic Color Harmonies (Complement, Analogous, Triadic)
        val harmonies = remember(activeColor) { ColorMath.generateHarmonies(activeColor) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PIGMENT HARMONIES (1-TAP MATCH)",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                harmonies.forEach { (label, harmonyColor) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                            .clickable { onColorSelected(harmonyColor) }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(harmonyColor)
                                .border(1.dp, palette.border, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label.take(6),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 6. Saved Studio Swatches Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CURATED ARCHIVAL PALETTE (${customSwatches.size} SWATCHES)",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                customSwatches.forEachIndexed { idx, swatch ->
                    val isSelected = ColorMath.toHexCode(swatch) == ColorMath.toHexCode(activeColor)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(swatch) }
                            .testTag("palette_swatch_$idx")
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected Swatch",
                                tint = if (ColorMath.colorToHsv(swatch).value > 0.65f) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isDockedPanel) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_color_button")
            ) {
                Text(
                    text = "Apply Pigment (${ColorMath.toHexCode(activeColor)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
