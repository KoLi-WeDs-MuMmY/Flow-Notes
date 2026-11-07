package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.SketchPageEntity
import com.example.model.DrawingTool
import com.example.model.PaperTemplate
import com.example.model.ScreenCoveragePreset
import com.example.model.ScreenFloatMetrics
import com.example.ui.theme.AmberGold
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TealStudio
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.VermilionAccent
import kotlin.math.roundToInt

@Composable
fun ScreenCoverageInspectorDialog(
    metrics: ScreenFloatMetrics,
    currentPreset: ScreenCoveragePreset,
    customCoverageFloat: Float,
    zoomFloat: Float,
    floatingXFraction: Float,
    floatingYFraction: Float,
    onSelectPreset: (ScreenCoveragePreset) -> Unit,
    onUpdateCustomFloat: (Float) -> Unit,
    onResetDockPosition: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioCharcoal,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
                .testTag("screen_coverage_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Screen Coverage Engine",
                            tint = TealStudio
                        )
                        Column {
                            Text(
                                text = "Multi-Screen Float & Coverage Engine",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${metrics.posture.displayName} (${metrics.posture.badgeCode}) • Aspect ${String.format("%.2ff", metrics.aspectRatioFloat)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Inspector",
                            tint = TextPrimaryDark
                        )
                    }
                }

                // Visual Float Bar Breakdown
                Surface(
                    color = StudioObsidian,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(
                                    "CANVAS: %.3ff (%.1f%%)",
                                    metrics.effectiveCanvasCoverageFloat,
                                    metrics.canvasCoveragePercent
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = TealStudio
                            )
                            Text(
                                text = String.format(
                                    "HUD FLOAT: %.3ff (%.1f%%)",
                                    metrics.hudCoverageFloat,
                                    metrics.hudCoveragePercent
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = VermilionAccent
                            )
                        }

                        LinearProgressIndicator(
                            progress = { metrics.effectiveCanvasCoverageFloat },
                            color = TealStudio,
                            trackColor = VermilionAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(
                                    "Viewport: %.0fdp × %.0fdp",
                                    metrics.screenWidthDp,
                                    metrics.screenHeightDp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = String.format(
                                    "Dock Float: (%.2ff, %.2ff) • Zoom: %.2fx",
                                    floatingXFraction,
                                    floatingYFraction,
                                    zoomFloat
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                // Custom Screen Cover Percentage Float Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ADJUST SCREEN COVER FLOAT (0.65f - 0.98f)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = String.format("%.2ff (%d%%)", customCoverageFloat, (customCoverageFloat * 100).roundToInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = TealStudio
                        )
                    }
                    Slider(
                        value = customCoverageFloat,
                        onValueChange = onUpdateCustomFloat,
                        valueRange = 0.65f..0.98f,
                        colors = SliderDefaults.colors(
                            thumbColor = TealStudio,
                            activeTrackColor = TealStudio
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("screen_coverage_float_slider")
                    )
                }

                // Preset Cards
                Text(
                    text = "MULTI-SCREEN COVERAGE PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScreenCoveragePreset.entries.filter { it != ScreenCoveragePreset.CUSTOM_FLOAT }.forEach { preset ->
                        val selected = currentPreset == preset
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) StudioSlate else StudioObsidian)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) TealStudio else StudioBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active Preset",
                                    tint = TealStudio
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onResetDockPosition,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSlate,
                            contentColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Center Float Dock (0.50f)", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermilionAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SketchbookPagesDialog(
    pages: List<SketchPageEntity>,
    activePageId: Long,
    onSelectPage: (SketchPageEntity) -> Unit,
    onCreatePage: (title: String, category: String, template: PaperTemplate) -> Unit,
    onDeletePage: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newSheetTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Drawing & Notes") }
    var selectedTemplate by remember { mutableStateOf(PaperTemplate.DOT_MATRIX) }
    var showCreateForm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioCharcoal,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.84f)
                .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
                .testTag("sketchbook_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Studio Hero Banner Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner_1794045272359),
                        contentDescription = "CanvasFlow Studio Sketchbook",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(StudioObsidian.copy(alpha = 0.56f))
                    )
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Studio Sketchbook & Notes",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "${pages.size} Multi-Screen Sheets • Auto-Saved Locally",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(StudioObsidian.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sketchbook",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (showCreateForm) {
                    Surface(
                        color = StudioObsidian,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VermilionAccent, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "CREATE NEW DRAWING / WRITING SHEET",
                                style = MaterialTheme.typography.labelMedium,
                                color = VermilionAccent
                            )
                            OutlinedTextField(
                                value = newSheetTitle,
                                onValueChange = { newSheetTitle = it },
                                label = { Text("Sheet Title (e.g. Tablet Concept, Lecture Notes)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VermilionAccent,
                                    unfocusedBorderColor = StudioBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_sheet_title_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Drawing", "Handwriting", "Blueprint").forEach { cat ->
                                    val isCat = selectedCategory == cat
                                    Surface(
                                        color = if (isCat) VermilionAccent else StudioSlate,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { selectedCategory = cat }
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showCreateForm = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioSlate)
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onCreatePage(
                                            newSheetTitle.ifBlank { "Studio Sheet #${pages.size + 1}" },
                                            selectedCategory,
                                            selectedTemplate
                                        )
                                        showCreateForm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VermilionAccent),
                                    modifier = Modifier.testTag("confirm_create_sheet_button")
                                ) {
                                    Text("Create Sheet")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Button(
                        onClick = { showCreateForm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermilionAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_new_sheet_form_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Drawing or Writing Sheet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pages, key = { it.id }) { page ->
                        val isCurrent = page.id == activePageId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isCurrent) StudioSlate else StudioObsidian)
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) VermilionAccent else StudioBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectPage(page) }
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) VermilionAccent else StudioSlate)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "${page.categoryTag} • ${page.strokeCount} strokes • Zoom ${String.format("%.1fx", page.lastZoomFloat)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                            if (pages.size > 1) {
                                IconButton(
                                    onClick = { onDeletePage(page.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Sheet",
                                        tint = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaperTemplateDialog(
    currentTemplate: PaperTemplate,
    onSelectTemplate: (PaperTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioCharcoal,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
                .testTag("paper_template_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = VermilionAccent
                        )
                        Text(
                            text = "Paper & Ruling Template",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimaryDark
                        )
                    }
                }

                PaperTemplate.entries.forEach { template ->
                    val selected = template == currentTemplate
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) StudioSlate else StudioObsidian)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) VermilionAccent else StudioBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTemplate(template) }
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(template.backgroundColorHex))
                                .border(2.dp, Color(template.lineColorHex), RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = template.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = VermilionAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrushAndNibSettingsDialog(
    selectedTool: DrawingTool,
    strokeWidth: Float,
    strokeAlpha: Float,
    activeColor: Color,
    onUpdateWidth: (Float) -> Unit,
    onUpdateAlpha: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioCharcoal,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
                .testTag("brush_settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = VermilionAccent
                        )
                        Text(
                            text = "${selectedTool.label} Calibration",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimaryDark
                        )
                    }
                }

                // Stroke Width Float Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NIB / STROKE WIDTH FLOAT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = String.format("%.1f px", strokeWidth),
                            style = MaterialTheme.typography.labelMedium,
                            color = VermilionAccent
                        )
                    }
                    Slider(
                        value = strokeWidth,
                        onValueChange = onUpdateWidth,
                        valueRange = 1.0f..60.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = VermilionAccent,
                            activeTrackColor = VermilionAccent
                        ),
                        modifier = Modifier.testTag("stroke_width_slider")
                    )
                }

                // Opacity Float Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "INK FLOW OPACITY FLOAT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = String.format("%.2ff (%d%%)", strokeAlpha, (strokeAlpha * 100).roundToInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = TealStudio
                        )
                    }
                    Slider(
                        value = strokeAlpha,
                        onValueChange = onUpdateAlpha,
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = TealStudio,
                            activeTrackColor = TealStudio
                        ),
                        modifier = Modifier.testTag("stroke_alpha_slider")
                    )
                }

                // Quick Nib Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2.0f to "Fine 2px", 5.5f to "Nib 5.5px", 14.0f to "Broad 14px", 28.0f to "Wash 28px").forEach { (w, label) ->
                        Surface(
                            color = if (kotlin.math.abs(strokeWidth - w) < 0.8f) VermilionAccent else StudioObsidian,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateWidth(w) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VermilionAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Nib Settings")
                }
            }
        }
    }
}

@Composable
fun AddTextCalloutDialog(
    onAddNote: (text: String, fontSizeSp: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(18f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioCharcoal,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                .testTag("add_text_note_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stamp Typography / Note Label",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Enter heading, annotation, or study note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_text_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TEXT SIZE FLOAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "${fontSize.roundToInt()} sp",
                        style = MaterialTheme.typography.labelSmall,
                        color = VermilionAccent
                    )
                }
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..36f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSlate)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAddNote(noteText, fontSize) },
                        colors = ButtonDefaults.buttonColors(containerColor = VermilionAccent),
                        modifier = Modifier.testTag("confirm_add_note_button")
                    ) {
                        Text("Place on Canvas")
                    }
                }
            }
        }
    }
}
