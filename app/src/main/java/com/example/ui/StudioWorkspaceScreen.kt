package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DevicePosture
import com.example.model.DrawingTool
import com.example.model.ScreenCoveragePreset
import com.example.model.ScreenFloatMetrics
import com.example.ui.components.AddTextCalloutDialog
import com.example.ui.components.BrushAndNibSettingsDialog
import com.example.ui.components.InteractiveDrawingCanvas
import com.example.ui.components.PaperTemplateDialog
import com.example.ui.components.PrecisionColorPickerContent
import com.example.ui.components.PrecisionColorPickerDialog
import com.example.ui.components.ScreenCoverageInspectorDialog
import com.example.ui.components.SketchbookPagesDialog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TealStudio
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.VermilionAccent
import com.example.util.ColorMath
import com.example.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@Composable
fun StudioWorkspaceScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedPages by viewModel.savedPages.collectAsStateWithLifecycle()

    // Handle system back if any floating studio modal or split mode is active
    val anyModalOpen = state.isColorPickerOpen ||
        state.isBrushSettingsOpen ||
        state.isPaperPickerOpen ||
        state.isScreenInspectorOpen ||
        state.isSketchbookDrawerOpen ||
        state.isAddNoteDialogOpen

    BackHandler(enabled = anyModalOpen || state.coveragePreset == ScreenCoveragePreset.MAX_IMMERSION) {
        when {
            state.isColorPickerOpen -> viewModel.setColorPickerOpen(false)
            state.isBrushSettingsOpen -> viewModel.setBrushSettingsOpen(false)
            state.isPaperPickerOpen -> viewModel.setPaperPickerOpen(false)
            state.isScreenInspectorOpen -> viewModel.setScreenInspectorOpen(false)
            state.isSketchbookDrawerOpen -> viewModel.setSketchbookDrawerOpen(false)
            state.isAddNoteDialogOpen -> viewModel.setAddNoteDialogOpen(false)
            state.coveragePreset == ScreenCoveragePreset.MAX_IMMERSION ->
                viewModel.selectCoveragePreset(ScreenCoveragePreset.STUDIO_BALANCED)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
    ) {
        val screenMaxWidth = this.maxWidth
        val screenMaxHeight = this.maxHeight
        val widthDp = screenMaxWidth.value
        val heightDp = screenMaxHeight.value
        val metrics = viewModel.computeScreenFloatMetrics(widthDp, heightDp)
        val density = LocalDensity.current
        val isImmersion = state.coveragePreset == ScreenCoveragePreset.MAX_IMMERSION
        val isSplitDockedOnWide = state.coveragePreset == ScreenCoveragePreset.DRAFTING_SPLIT &&
            metrics.posture != DevicePosture.MOBILE_PORTRAIT

        // Main Workspace Row (Supports Side-by-Side Split on Tablet/Landscape using Float Fractions)
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Canvas Region (Uses exact Float weight/fraction)
            Box(
                modifier = Modifier
                    .weight(if (isSplitDockedOnWide) (1f - metrics.inspectorWidthFraction) else 1f)
                    .fillMaxHeight()
            ) {
                // 1. Full-Potential Interactive Drawing & Writing Canvas with Pinch-to-Zoom
                InteractiveDrawingCanvas(
                    strokes = state.strokes,
                    textNotes = state.textNotes,
                    selectedTool = state.selectedTool,
                    activeColor = state.activeColor,
                    strokeWidth = state.strokeWidth,
                    strokeAlpha = state.strokeAlpha,
                    paperTemplate = state.paperTemplate,
                    zoomFloat = state.zoomFloat,
                    panOffsetX = state.panOffsetX,
                    panOffsetY = state.panOffsetY,
                    isPanNavigationMode = state.isPanNavigationMode,
                    showMinimap = state.showMinimap && !isImmersion,
                    onCommitStroke = viewModel::commitStroke,
                    onPinchZoomAndPan = viewModel::onPinchZoomAndPan,
                    onRemoveTextNote = viewModel::removeTextNote,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Top Floating Studio HUD Bar (Uses Float Width Fraction & Status Bar Safe Insets)
                TopFloatingStudioHud(
                    pageTitle = state.activePageTitle,
                    metrics = metrics,
                    zoomFloat = state.zoomFloat,
                    isPanMode = state.isPanNavigationMode,
                    isImmersion = isImmersion,
                    canUndo = state.strokes.isNotEmpty(),
                    canRedo = state.redoStrokes.isNotEmpty(),
                    showMinimap = state.showMinimap,
                    onOpenSketchbook = { viewModel.setSketchbookDrawerOpen(true) },
                    onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
                    onZoomIn = { viewModel.stepZoom(1.25f) },
                    onZoomOut = { viewModel.stepZoom(0.80f) },
                    onZoomReset = viewModel::resetZoomAndPan,
                    onTogglePanMode = viewModel::togglePanNavigationMode,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onOpenPaperPicker = { viewModel.setPaperPickerOpen(true) },
                    onOpenAddNote = { viewModel.setAddNoteDialogOpen(true) },
                    onToggleMinimap = viewModel::toggleMinimap,
                    onClearCanvas = viewModel::clearCanvas,
                    onToggleImmersion = {
                        if (isImmersion) {
                            viewModel.selectCoveragePreset(ScreenCoveragePreset.STUDIO_BALANCED)
                        } else {
                            viewModel.selectCoveragePreset(ScreenCoveragePreset.MAX_IMMERSION)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 6.dp)
                        .fillMaxWidth(0.96f)
                )

                // 3. Adaptive Side Studio Tool Rail on Tablets / Foldables / Landscape
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersion && metrics.sideRailWidthFraction > 0f,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                ) {
                    AdaptiveSideToolRail(
                        selectedTool = state.selectedTool,
                        activeColor = state.activeColor,
                        strokeWidth = state.strokeWidth,
                        onSelectTool = viewModel::selectTool,
                        onOpenColorPicker = { viewModel.setColorPickerOpen(true) },
                        onOpenBrushSettings = { viewModel.setBrushSettingsOpen(true) }
                    )
                }

                // 4. Draggable / Percentage-Scaled Bottom Floating Studio Dock (Tools + Color Bar + Color Picker Trigger)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersion,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 10.dp)
                ) {
                    val containerWidthPx = with(density) { screenMaxWidth.toPx().coerceAtLeast(1f) }
                    val containerHeightPx = with(density) { screenMaxHeight.toPx().coerceAtLeast(1f) }
                    val currentXFrac by rememberUpdatedState(state.floatingPaletteXFraction)
                    val currentYFrac by rememberUpdatedState(state.floatingPaletteYFraction)

                    val dragOffsetX = ((state.floatingPaletteXFraction - 0.50f) * containerWidthPx * 0.45f).roundToInt()
                    val dragOffsetY = ((state.floatingPaletteYFraction - 0.92f) * containerHeightPx * 0.45f).roundToInt()

                    FloatingBottomStudioDock(
                        selectedTool = state.selectedTool,
                        activeColor = state.activeColor,
                        strokeWidth = state.strokeWidth,
                        strokeAlpha = state.strokeAlpha,
                        swatches = state.customSavedSwatches,
                        showToolsInBottomDock = metrics.sideRailWidthFraction == 0f,
                        dockWidthFraction = metrics.bottomDockWidthFraction,
                        onSelectTool = viewModel::selectTool,
                        onSelectColor = viewModel::selectColor,
                        onOpenColorPicker = { viewModel.setColorPickerOpen(true) },
                        onOpenBrushSettings = { viewModel.setBrushSettingsOpen(true) },
                        onUpdateStrokeWidth = viewModel::updateStrokeWidth,
                        onDragDelta = { dx, dy ->
                            val nextX = currentXFrac + (dx / containerWidthPx)
                            val nextY = currentYFrac + (dy / containerHeightPx)
                            viewModel.updateFloatingPaletteFraction(nextX, nextY)
                        },
                        modifier = Modifier.offset { IntOffset(dragOffsetX, dragOffsetY) }
                    )
                }
            }

            // Right Docked Color & Layer Studio Panel when in Split Mode on Tablet / Landscape
            if (isSplitDockedOnWide) {
                Surface(
                    color = StudioCharcoal,
                    modifier = Modifier
                        .fillMaxWidth(metrics.inspectorWidthFraction / (1f - metrics.inspectorWidthFraction + metrics.inspectorWidthFraction))
                        .fillMaxHeight()
                        .border(1.dp, StudioBorder)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    PrecisionColorPickerContent(
                        activeColor = state.activeColor,
                        hsvColor = state.hsvColor,
                        strokeAlpha = state.strokeAlpha,
                        customSwatches = state.customSavedSwatches,
                        recentColors = state.recentColors,
                        onHsvChanged = viewModel::updateHsvColor,
                        onColorSelected = viewModel::selectColor,
                        onSaveSwatch = viewModel::saveCurrentColorToSwatches,
                        onDismiss = {
                            viewModel.selectCoveragePreset(ScreenCoveragePreset.STUDIO_BALANCED)
                        },
                        isDockedPanel = true
                    )
                }
            }
        }

        // Modals & Studio Sheets
        if (state.isColorPickerOpen) {
            PrecisionColorPickerDialog(
                activeColor = state.activeColor,
                hsvColor = state.hsvColor,
                strokeAlpha = state.strokeAlpha,
                customSwatches = state.customSavedSwatches,
                recentColors = state.recentColors,
                onHsvChanged = viewModel::updateHsvColor,
                onColorSelected = viewModel::selectColor,
                onSaveSwatch = viewModel::saveCurrentColorToSwatches,
                onDismiss = { viewModel.setColorPickerOpen(false) }
            )
        }

        if (state.isScreenInspectorOpen) {
            ScreenCoverageInspectorDialog(
                metrics = metrics,
                currentPreset = state.coveragePreset,
                customCoverageFloat = state.customCoverageFloat,
                zoomFloat = state.zoomFloat,
                floatingXFraction = state.floatingPaletteXFraction,
                floatingYFraction = state.floatingPaletteYFraction,
                onSelectPreset = viewModel::selectCoveragePreset,
                onUpdateCustomFloat = viewModel::updateCustomCoverageFloat,
                onResetDockPosition = { viewModel.updateFloatingPaletteFraction(0.50f, 0.92f) },
                onDismiss = { viewModel.setScreenInspectorOpen(false) }
            )
        }

        if (state.isSketchbookDrawerOpen) {
            SketchbookPagesDialog(
                pages = savedPages,
                activePageId = state.activePageId,
                onSelectPage = viewModel::loadSketchPage,
                onCreatePage = viewModel::createNewSketchPage,
                onDeletePage = viewModel::deleteSketchPage,
                onDismiss = { viewModel.setSketchbookDrawerOpen(false) }
            )
        }

        if (state.isPaperPickerOpen) {
            PaperTemplateDialog(
                currentTemplate = state.paperTemplate,
                onSelectTemplate = viewModel::selectPaperTemplate,
                onDismiss = { viewModel.setPaperPickerOpen(false) }
            )
        }

        if (state.isBrushSettingsOpen) {
            BrushAndNibSettingsDialog(
                selectedTool = state.selectedTool,
                strokeWidth = state.strokeWidth,
                strokeAlpha = state.strokeAlpha,
                activeColor = state.activeColor,
                onUpdateWidth = viewModel::updateStrokeWidth,
                onUpdateAlpha = viewModel::updateStrokeAlpha,
                onDismiss = { viewModel.setBrushSettingsOpen(false) }
            )
        }

        if (state.isAddNoteDialogOpen) {
            AddTextCalloutDialog(
                onAddNote = { text, fontSize ->
                    viewModel.addTextNote(text = text, fontSizeSp = fontSize)
                },
                onDismiss = { viewModel.setAddNoteDialogOpen(false) }
            )
        }
    }
}

@Composable
private fun TopFloatingStudioHud(
    pageTitle: String,
    metrics: ScreenFloatMetrics,
    zoomFloat: Float,
    isPanMode: Boolean,
    isImmersion: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    showMinimap: Boolean,
    onOpenSketchbook: () -> Unit,
    onOpenScreenInspector: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onTogglePanMode: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenPaperPicker: () -> Unit,
    onOpenAddNote: () -> Unit,
    onToggleMinimap: () -> Unit,
    onClearCanvas: () -> Unit,
    onToggleImmersion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCharcoal.copy(alpha = if (isImmersion) 0.78f else 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            .testTag("top_floating_studio_hud")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Sketchbook Sheet Selector Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioObsidian)
                    .clickable { onOpenSketchbook() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("sketchbook_selector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Open Sketchbook",
                    tint = VermilionAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(if (metrics.posture == DevicePosture.MOBILE_PORTRAIT) 105.dp else 150.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Multi-Screen Float & Coverage Percentage Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioObsidian)
                    .border(1.dp, TealStudio.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable { onOpenScreenInspector() }
                    .padding(horizontal = 9.dp, vertical = 7.dp)
                    .testTag("screen_coverage_badge")
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Screen Coverage Float Inspector",
                    tint = TealStudio,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = String.format(
                        "%.1f%% (%.2ff) • %s",
                        metrics.canvasCoveragePercent,
                        metrics.effectiveCanvasCoverageFloat,
                        metrics.posture.badgeCode
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = TealStudio,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Pinch-to-Zoom & Pan Gesture Control Cluster
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioObsidian)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSlate)
                        .clickable { onZoomReset() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("zoom_percentage_reset_badge")
                ) {
                    Text(
                        text = "${(zoomFloat * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePanMode,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPanMode) VermilionAccent else Color.Transparent)
                        .testTag("pan_gesture_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Pan & Pinch Navigation Mode",
                        tint = if (isPanMode) Color.White else TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Undo / Redo / Paper / Stamp / Immersion Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Stroke",
                        tint = if (canUndo) TextPrimaryDark else TextSecondaryDark.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo Stroke",
                        tint = if (canRedo) TextPrimaryDark else TextSecondaryDark.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenPaperPicker,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("paper_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Change Paper Template",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenAddNote,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("add_text_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Stamp Handwriting/Text Note",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleMinimap,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_minimap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Toggle Minimap",
                        tint = if (showMinimap) TealStudio else TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClearCanvas,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("clear_canvas_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Canvas",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleImmersion,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_immersion_button")
                ) {
                    Icon(
                        imageVector = if (isImmersion) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Full Immersion",
                        tint = if (isImmersion) VermilionAccent else TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveSideToolRail(
    selectedTool: DrawingTool,
    activeColor: Color,
    strokeWidth: Float,
    onSelectTool: (DrawingTool) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenBrushSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCharcoal.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 10.dp,
        modifier = modifier
            .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
            .testTag("side_tool_rail")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            // Color Picker Orb
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(activeColor)
                    .border(2.5.dp, Color.White, CircleShape)
                    .clickable { onOpenColorPicker() }
                    .testTag("rail_color_picker_orb")
            )

            // Nib Width Pill
            Surface(
                color = StudioObsidian,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onOpenBrushSettings() }
            ) {
                Text(
                    text = "${strokeWidth.roundToInt()}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberGold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            DrawingTool.entries.forEach { tool ->
                val isSelected = tool == selectedTool
                IconButton(
                    onClick = { onSelectTool(tool) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) VermilionAccent else StudioObsidian)
                        .testTag("rail_tool_${tool.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = iconForTool(tool),
                        contentDescription = tool.label,
                        tint = if (isSelected) Color.White else TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomStudioDock(
    selectedTool: DrawingTool,
    activeColor: Color,
    strokeWidth: Float,
    strokeAlpha: Float,
    swatches: List<Color>,
    showToolsInBottomDock: Boolean,
    dockWidthFraction: Float,
    onSelectTool: (DrawingTool) -> Unit,
    onSelectColor: (Color) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenBrushSettings: () -> Unit,
    onUpdateStrokeWidth: (Float) -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCharcoal.copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth(dockWidthFraction.coerceIn(0.55f, 0.96f))
            .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
            .testTag("bottom_floating_studio_dock")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Drag Handle + Drawing & Writing Tools (if on Mobile Portrait) + Nib Size Slider
            if (showToolsInBottomDock) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Float Drag Handle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioObsidian)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.x, dragAmount.y)
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag Floating Studio Dock",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DrawingTool.entries.forEach { tool ->
                        val isSelected = tool == selectedTool
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) VermilionAccent else StudioObsidian)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.4f) else StudioBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectTool(tool) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("dock_tool_${tool.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = iconForTool(tool),
                                contentDescription = tool.label,
                                tint = if (isSelected) Color.White else TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            if (isSelected) {
                                Text(
                                    text = tool.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: Color Picker Studio Button + Quick Color Swatches + Nib Calibration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Full Color Picker Trigger Button (Rainbow Ring + Active Color Orb + HEX)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioObsidian)
                        .border(1.5.dp, VermilionAccent, RoundedCornerShape(14.dp))
                        .clickable { onOpenColorPicker() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("open_color_picker_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = strokeAlpha))
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Color Picker",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ColorMath.toHexCode(activeColor),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold
                        )
                    }
                }

                // Scrollable Quick Color Swatches
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    swatches.forEachIndexed { idx, color ->
                        val isCurrent = ColorMath.toHexCode(color) == ColorMath.toHexCode(activeColor) &&
                            selectedTool != DrawingTool.ERASER
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) VermilionAccent else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(color) }
                                .testTag("quick_swatch_$idx")
                        )
                    }
                }

                // Nib Width & Opacity Calibrator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioObsidian)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenBrushSettings() }
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .testTag("open_brush_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Brush & Nib Calibration",
                        tint = TealStudio,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format("%.1fpx", strokeWidth),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryDark
                    )
                }
            }
        }
    }
}

private fun iconForTool(tool: DrawingTool): ImageVector {
    return when (tool) {
        DrawingTool.FOUNTAIN_PEN -> Icons.Default.Create
        DrawingTool.FINELINER -> Icons.Default.Edit
        DrawingTool.CHISEL_MARKER -> Icons.Default.FormatColorFill
        DrawingTool.ART_BRUSH -> Icons.Default.Brush
        DrawingTool.CHARCOAL -> Icons.Default.ColorLens
        DrawingTool.RULER_LINE -> Icons.Default.Straighten
        DrawingTool.RECTANGLE_SHAPE -> Icons.Default.CropSquare
        DrawingTool.CIRCLE_SHAPE -> Icons.Default.PanoramaFishEye
        DrawingTool.ERASER -> Icons.Default.AutoFixNormal
    }
}
