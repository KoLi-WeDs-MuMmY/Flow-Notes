package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.FloatingLayoutPreset
import com.example.model.ScreenCoveragePreset
import com.example.model.ScreenFloatMetrics
import com.example.model.SymbolStampType
import com.example.ui.components.AddTextCalloutDialog
import com.example.ui.components.BrushAndNibSettingsDialog
import com.example.ui.components.InteractiveDrawingCanvas
import com.example.ui.components.PaperTemplateDialog
import com.example.ui.components.PrecisionColorPickerContent
import com.example.ui.components.PrecisionColorPickerDialog
import com.example.ui.components.ScreenCoverageInspectorDialog
import com.example.ui.components.SketchbookPagesDialog
import com.example.ui.components.StudioCustomizationDialog
import com.example.ui.theme.LocalStudioPalette
import com.example.util.ColorMath
import com.example.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@Composable
fun StudioWorkspaceScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedPages by viewModel.savedPages.collectAsStateWithLifecycle()
    val custom = state.customization

    BackHandler {
        when {
            state.isCustomizationModalOpen -> viewModel.setCustomizationModalOpen(false)
            state.isColorPickerOpen -> viewModel.setColorPickerOpen(false)
            state.isBrushSettingsOpen -> viewModel.setBrushSettingsOpen(false)
            state.isPaperPickerOpen -> viewModel.setPaperPickerOpen(false)
            state.isScreenInspectorOpen -> viewModel.setScreenInspectorOpen(false)
            state.isSketchbookDrawerOpen -> viewModel.setSketchbookDrawerOpen(false)
            state.isAddNoteDialogOpen -> viewModel.setAddNoteDialogOpen(false)
            state.coveragePreset == ScreenCoveragePreset.MAX_IMMERSION ->
                viewModel.selectCoveragePreset(ScreenCoveragePreset.STUDIO_BALANCED)
            else -> viewModel.navigateToHomeExplorer()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
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

        val containerWidthPx = with(density) { screenMaxWidth.toPx().coerceAtLeast(1f) }
        val containerHeightPx = with(density) { screenMaxHeight.toPx().coerceAtLeast(1f) }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left / Primary Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(if (isSplitDockedOnWide) (1f - metrics.inspectorWidthFraction) else 1f)
                    .fillMaxHeight()
            ) {
                // 1. High-FPS Path-Cached Interactive Drawing & Writing Canvas with Pinch-to-Zoom & Straightedge Ruler
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
                    gridSpacingMultiplierFloat = custom.gridSpacingMultiplierFloat,
                    gridLineOpacityFloat = custom.gridLineOpacityFloat,
                    showRulerOverlay = custom.showRulerGuideOverlay,
                    rulerAngleDegrees = custom.rulerAngleDegrees,
                    rulerYFraction = custom.rulerYFraction,
                    onUpdateRuler = viewModel::updateRulerOverlay,
                    onCloseRuler = {
                        viewModel.toggleFloatingWidget(rulerOverlay = false)
                    },
                    onCommitStroke = viewModel::commitStroke,
                    onPinchZoomAndPan = viewModel::onPinchZoomAndPan,
                    onRemoveTextNote = viewModel::removeTextNote,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Top Floating Atelier HUD Bar
                TopFloatingStudioHud(
                    pageTitle = state.activePageTitle,
                    metrics = metrics,
                    zoomFloat = state.zoomFloat,
                    isPanMode = state.isPanNavigationMode,
                    isImmersion = isImmersion,
                    canUndo = state.strokes.isNotEmpty(),
                    canRedo = state.redoStrokes.isNotEmpty(),
                    showMinimap = state.showMinimap,
                    showRuler = custom.showRulerGuideOverlay,
                    showScratchpad = custom.showScratchpadPod,
                    podOpacity = custom.widgetOpacityFloat,
                    onNavigateHome = viewModel::navigateToHomeExplorer,
                    onOpenSketchbook = { viewModel.setSketchbookDrawerOpen(true) },
                    onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
                    onOpenCustomizer = { viewModel.setCustomizationModalOpen(true) },
                    onZoomIn = { viewModel.stepZoom(1.25f) },
                    onZoomOut = { viewModel.stepZoom(0.80f) },
                    onZoomReset = viewModel::resetZoomAndPan,
                    onTogglePanMode = viewModel::togglePanNavigationMode,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onOpenPaperPicker = { viewModel.setPaperPickerOpen(true) },
                    onOpenAddNote = { viewModel.setAddNoteDialogOpen(true) },
                    onToggleRuler = {
                        viewModel.toggleFloatingWidget(rulerOverlay = !custom.showRulerGuideOverlay)
                    },
                    onToggleScratchpad = {
                        viewModel.toggleFloatingWidget(scratchpadPod = !custom.showScratchpadPod)
                    },
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

                // 3. Independently Draggable Floating Tool & Nib Pod
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersion && custom.showToolPod,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.92f),
                    exit = fadeOut() + scaleOut(targetScale = 0.92f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    val currentX by rememberUpdatedState(state.toolPodXFraction)
                    val currentY by rememberUpdatedState(state.toolPodYFraction)
                    val offsetX = (state.toolPodXFraction * containerWidthPx * 0.78f).roundToInt()
                    val offsetY = (state.toolPodYFraction * containerHeightPx * 0.72f).roundToInt()

                    FloatingToolPod(
                        selectedTool = state.selectedTool,
                        activeColor = state.activeColor,
                        strokeWidth = state.strokeWidth,
                        isCompact = custom.isToolPodCompact,
                        podOpacity = custom.widgetOpacityFloat,
                        onSelectTool = viewModel::selectTool,
                        onToggleCompact = {
                            viewModel.toggleFloatingWidget(toolPodCompact = !custom.isToolPodCompact)
                        },
                        onOpenBrushSettings = { viewModel.setBrushSettingsOpen(true) },
                        onDragDelta = { dx, dy ->
                            viewModel.updateToolPodPosition(
                                currentX + (dx / containerWidthPx),
                                currentY + (dy / containerHeightPx)
                            )
                        },
                        modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
                    )
                }

                // 4. Independently Draggable Floating Symbol Stamp & Drafting Pod
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersion && custom.showSymbolStampPod,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.92f),
                    exit = fadeOut() + scaleOut(targetScale = 0.92f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    val currentSymX by rememberUpdatedState(state.symbolPodXFraction)
                    val currentSymY by rememberUpdatedState(state.symbolPodYFraction)
                    val offsetX = ((state.symbolPodXFraction - 0.94f) * containerWidthPx * 0.65f).roundToInt()
                    val offsetY = (state.symbolPodYFraction * containerHeightPx * 0.65f).roundToInt()

                    FloatingSymbolStampPod(
                        showRuler = custom.showRulerGuideOverlay,
                        podOpacity = custom.widgetOpacityFloat,
                        onStampSymbol = viewModel::stampSymbolOnCanvas,
                        onToggleRuler = {
                            viewModel.toggleFloatingWidget(rulerOverlay = !custom.showRulerGuideOverlay)
                        },
                        onDragDelta = { dx, dy ->
                            viewModel.updateSymbolPodPosition(
                                currentSymX + (dx / containerWidthPx),
                                currentSymY + (dy / containerHeightPx)
                            )
                        },
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .offset { IntOffset(offsetX, offsetY) }
                    )
                }

                // 5. Independently Draggable Floating Reference Scratchpad Pod
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersion && custom.showScratchpadPod,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.92f),
                    exit = fadeOut() + scaleOut(targetScale = 0.92f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    val currentNoteX by rememberUpdatedState(state.scratchpadXFraction)
                    val currentNoteY by rememberUpdatedState(state.scratchpadYFraction)
                    val offsetX = (state.scratchpadXFraction * containerWidthPx * 0.62f).roundToInt()
                    val offsetY = (state.scratchpadYFraction * containerHeightPx * 0.68f).roundToInt()

                    FloatingScratchpadPod(
                        noteText = custom.scratchpadNoteText,
                        podOpacity = custom.widgetOpacityFloat,
                        onUpdateText = viewModel::updateScratchpadText,
                        onStampToCanvas = {
                            viewModel.addTextNote(
                                text = custom.scratchpadNoteText.lines().firstOrNull()?.removePrefix("• ")?.trim()
                                    ?: "Studio Note",
                                fontSizeSp = 18f
                            )
                        },
                        onClose = { viewModel.toggleFloatingWidget(scratchpadPod = false) },
                        onDragDelta = { dx, dy ->
                            viewModel.updateScratchpadPosition(
                                currentNoteX + (dx / containerWidthPx),
                                currentNoteY + (dy / containerHeightPx)
                            )
                        },
                        modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
                    )
                }

                // 6. Independently Draggable Bottom Floating Pigment & Swatch Pod
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersion && custom.showColorPod,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.95f),
                    exit = fadeOut() + scaleOut(targetScale = 0.95f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 10.dp)
                ) {
                    val currentXFrac by rememberUpdatedState(state.colorPodXFraction)
                    val currentYFrac by rememberUpdatedState(state.colorPodYFraction)

                    val dragOffsetX = ((state.colorPodXFraction - 0.50f) * containerWidthPx * 0.48f).roundToInt()
                    val dragOffsetY = ((state.colorPodYFraction - 0.92f) * containerHeightPx * 0.48f).roundToInt()

                    FloatingPigmentPod(
                        selectedTool = state.selectedTool,
                        activeColor = state.activeColor,
                        strokeWidth = state.strokeWidth,
                        strokeAlpha = state.strokeAlpha,
                        swatches = state.customSavedSwatches,
                        dockWidthFraction = metrics.bottomDockWidthFraction,
                        podOpacity = custom.widgetOpacityFloat,
                        onSelectTool = viewModel::selectTool,
                        onSelectColor = viewModel::selectColor,
                        onOpenColorPicker = { viewModel.setColorPickerOpen(true) },
                        onOpenBrushSettings = { viewModel.setBrushSettingsOpen(true) },
                        onOpenCustomizer = { viewModel.setCustomizationModalOpen(true) },
                        onDragDelta = { dx, dy ->
                            viewModel.updateColorPodPosition(
                                currentXFrac + (dx / containerWidthPx),
                                currentYFrac + (dy / containerHeightPx)
                            )
                        },
                        modifier = Modifier.offset { IntOffset(dragOffsetX, dragOffsetY) }
                    )
                }
            }

            // Right Docked Pigment & Calibration Deck when in Split Mode on Tablet / Landscape
            if (isSplitDockedOnWide) {
                Surface(
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth(metrics.inspectorWidthFraction / (1f - metrics.inspectorWidthFraction + metrics.inspectorWidthFraction))
                        .fillMaxHeight()
                        .border(1.dp, palette.border)
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
                floatingXFraction = state.colorPodXFraction,
                floatingYFraction = state.colorPodYFraction,
                onSelectPreset = viewModel::selectCoveragePreset,
                onUpdateCustomFloat = viewModel::updateCustomCoverageFloat,
                onResetDockPosition = { viewModel.applyFloatingLayoutPreset(FloatingLayoutPreset.ATELIER_BALANCED) },
                onDismiss = { viewModel.setScreenInspectorOpen(false) }
            )
        }

        if (state.isCustomizationModalOpen) {
            StudioCustomizationDialog(
                customization = state.customization,
                onSelectTheme = viewModel::selectStudioTheme,
                onUpdateCornerRadius = viewModel::updateCornerRadiusDp,
                onUpdateWidgetOpacity = viewModel::updateWidgetOpacityFloat,
                onUpdateGridCustomization = viewModel::updateGridCustomization,
                onSelectLayoutPreset = viewModel::applyFloatingLayoutPreset,
                onToggleWidget = viewModel::toggleFloatingWidget,
                onDismiss = { viewModel.setCustomizationModalOpen(false) }
            )
        }

        if (state.isSketchbookDrawerOpen) {
            SketchbookPagesDialog(
                pages = savedPages,
                activePageId = state.activePageId,
                onSelectPage = viewModel::openSheetInCanvas,
                onCreatePage = { title, category, template ->
                    viewModel.createNewSketchPage(
                        title = title,
                        category = category,
                        template = template,
                        targetFolderId = state.activePageFolderId,
                        openImmediately = true
                    )
                },
                onDeletePage = viewModel::deleteSketchPage,
                onOpenHomeExplorer = viewModel::navigateToHomeExplorer,
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
    showRuler: Boolean,
    showScratchpad: Boolean,
    podOpacity: Float,
    onNavigateHome: () -> Unit,
    onOpenSketchbook: () -> Unit,
    onOpenScreenInspector: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onTogglePanMode: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenPaperPicker: () -> Unit,
    onOpenAddNote: () -> Unit,
    onToggleRuler: () -> Unit,
    onToggleScratchpad: () -> Unit,
    onToggleMinimap: () -> Unit,
    onClearCanvas: () -> Unit,
    onToggleImmersion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceElevated.copy(alpha = if (isImmersion) 0.80f else podOpacity),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp,
        modifier = modifier
            .border(1.dp, palette.border, MaterialTheme.shapes.medium)
            .testTag("top_floating_studio_hud")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Home Page & Nested Folder Explorer Button (Refined Sumi Ink / Slate Pill)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary)
                    .clickable { onNavigateHome() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("home_explorer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Return to Home & Folders",
                    tint = palette.onPrimary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sketchbook Sheet Selector Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .clickable { onOpenSketchbook() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("sketchbook_selector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Open Sketchbook",
                    tint = palette.secondary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(if (metrics.posture == DevicePosture.MOBILE_PORTRAIT) 90.dp else 135.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Multi-Screen Float & Coverage Percentage Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable { onOpenScreenInspector() }
                    .padding(horizontal = 9.dp, vertical = 7.dp)
                    .testTag("screen_coverage_badge")
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Screen Coverage Float Inspector",
                    tint = palette.accent,
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
                    color = palette.accent,
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
                    .background(palette.surface)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
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
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.badgeSurface)
                        .clickable { onZoomReset() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("zoom_percentage_reset_badge")
                ) {
                    Text(
                        text = "${(zoomFloat * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
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
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePanMode,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPanMode) palette.primary else Color.Transparent)
                        .testTag("pan_gesture_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Pan & Pinch Navigation Mode",
                        tint = if (isPanMode) palette.onPrimary else palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Undo / Redo / Ruler / Scratchpad / Paper / Customizer Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Stroke",
                        tint = if (canUndo) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo Stroke",
                        tint = if (canRedo) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleRuler,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showRuler) palette.badgeSurface else Color.Transparent)
                        .testTag("toggle_ruler_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Toggle Straightedge Ruler",
                        tint = if (showRuler) palette.accent else palette.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleScratchpad,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showScratchpad) palette.badgeSurface else Color.Transparent)
                        .testTag("toggle_scratchpad_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = "Toggle Reference Scratchpad Pod",
                        tint = if (showScratchpad) palette.secondary else palette.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenPaperPicker,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("paper_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Change Paper Template",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenAddNote,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("add_text_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Stamp Text Callout",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleMinimap,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_minimap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Toggle Minimap",
                        tint = if (showMinimap) palette.accent else palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenCustomizer,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.badgeSurface)
                        .testTag("open_studio_customizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Open Theme & Pods Customizer",
                        tint = palette.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClearCanvas,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_canvas_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Canvas",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleImmersion,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_immersion_button")
                ) {
                    Icon(
                        imageVector = if (isImmersion) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Full Immersion",
                        tint = if (isImmersion) palette.secondary else palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingToolPod(
    selectedTool: DrawingTool,
    activeColor: Color,
    strokeWidth: Float,
    isCompact: Boolean,
    podOpacity: Float,
    onSelectTool: (DrawingTool) -> Unit,
    onToggleCompact: () -> Unit,
    onOpenBrushSettings: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceElevated.copy(alpha = podOpacity),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 8.dp,
        modifier = modifier
            .padding(start = 10.dp)
            .border(1.dp, palette.border, MaterialTheme.shapes.medium)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .testTag("floating_tool_pod")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 7.dp)
        ) {
            // Drag Handle + Compact/Expand Toggle Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.badgeSurface)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.x, dragAmount.y)
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Tool Pod",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.badgeSurface)
                        .clickable { onToggleCompact() }
                        .testTag("toggle_tool_pod_compact_button")
                ) {
                    Icon(
                        imageVector = if (isCompact) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                        contentDescription = "Collapse or Expand Tool Pod",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Active Nib Calibration Badge
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                    .clickable { onOpenBrushSettings() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                    Text(
                        text = "${selectedTool.symbolGlyph} ${strokeWidth.roundToInt()}px",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isCompact) {
                // Compact Mode: Only show active tool + Eraser quick toggle
                listOf(selectedTool, DrawingTool.ERASER).distinct().forEach { tool ->
                    val isSelected = tool == selectedTool
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) palette.primary else palette.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { onSelectTool(tool) }
                    ) {
                        Icon(
                            imageVector = iconForTool(tool),
                            contentDescription = tool.label,
                            tint = if (isSelected) palette.onPrimary else palette.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            } else {
                // Full Tactile Tool List with Icon + Symbol Glyph
                DrawingTool.entries.forEach { tool ->
                    val isSelected = tool == selectedTool
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) palette.primary else palette.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectTool(tool) }
                            .testTag("rail_tool_${tool.name.lowercase()}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = iconForTool(tool),
                                contentDescription = tool.label,
                                tint = if (isSelected) palette.onPrimary else palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "${tool.symbolGlyph}${tool.shortCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) palette.onPrimary else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSymbolStampPod(
    showRuler: Boolean,
    podOpacity: Float,
    onStampSymbol: (SymbolStampType) -> Unit,
    onToggleRuler: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceElevated.copy(alpha = podOpacity),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 8.dp,
        modifier = modifier
            .border(1.dp, palette.border, MaterialTheme.shapes.medium)
            .testTag("floating_symbol_stamp_pod")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 7.dp)
        ) {
            // Drag Handle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 36.dp, height = 24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.badgeSurface)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Drag Symbol Pod",
                        tint = palette.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "SYM",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            // 1-Tap Straightedge Ruler Toggle Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showRuler) palette.accent else palette.surface)
                    .border(1.dp, if (showRuler) palette.accent else palette.border, RoundedCornerShape(10.dp))
                    .clickable { onToggleRuler() }
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = "Toggle Straightedge Ruler",
                    tint = if (showRuler) Color.White else palette.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 1-Tap Architectural, Math & Editorial Symbol Stamps
            SymbolStampType.entries.take(8).forEach { stamp ->
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, palette.border, RoundedCornerShape(9.dp))
                        .clickable { onStampSymbol(stamp) }
                        .testTag("stamp_symbol_${stamp.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stamp.glyph.take(2).trim(),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingScratchpadPod(
    noteText: String,
    podOpacity: Float,
    onUpdateText: (String) -> Unit,
    onStampToCanvas: () -> Unit,
    onClose: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceElevated.copy(alpha = podOpacity),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 10.dp,
        modifier = modifier
            .width(235.dp)
            .border(1.dp, palette.border, MaterialTheme.shapes.medium)
            .testTag("floating_scratchpad_pod")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Scratchpad",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "¶ REFERENCE SCRATCHPAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = palette.badgeSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onStampToCanvas() }
                    ) {
                        Text(
                            text = "Stamp",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(palette.badgeSurface)
                            .clickable { onClose() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Scratchpad",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = onUpdateText,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border,
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            )
        }
    }
}

@Composable
private fun FloatingPigmentPod(
    selectedTool: DrawingTool,
    activeColor: Color,
    strokeWidth: Float,
    strokeAlpha: Float,
    swatches: List<Color>,
    dockWidthFraction: Float,
    podOpacity: Float,
    onSelectTool: (DrawingTool) -> Unit,
    onSelectColor: (Color) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenBrushSettings: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceElevated.copy(alpha = podOpacity),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth(dockWidthFraction.coerceIn(0.55f, 0.96f))
            .border(1.dp, palette.border, MaterialTheme.shapes.large)
            .testTag("bottom_floating_studio_dock")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Tactile Non-Neon Tool Strip + Drag Handle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.badgeSurface)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.x, dragAmount.y)
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Floating Pigment Pod",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DrawingTool.entries.forEach { tool ->
                    val isSelected = tool == selectedTool
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) palette.primary else palette.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { onSelectTool(tool) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("dock_tool_${tool.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = iconForTool(tool),
                            contentDescription = tool.label,
                            tint = if (isSelected) palette.onPrimary else palette.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (isSelected) "${tool.symbolGlyph} ${tool.label}" else tool.symbolGlyph,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) palette.onPrimary else palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Row: Pigment Mixer Button + Archival Swatches + Nib Calibration + Theme Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                        .border(1.5.dp, palette.primary, RoundedCornerShape(12.dp))
                        .clickable { onOpenColorPicker() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("open_color_picker_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = strokeAlpha))
                            .border(1.5.dp, palette.border, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Pigment",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ColorMath.toHexCode(activeColor),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary
                        )
                    }
                }

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
                                .size(if (isCurrent) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) palette.primary else palette.border,
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(color) }
                                .testTag("quick_swatch_$idx")
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(11.dp))
                        .clickable { onOpenBrushSettings() }
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .testTag("open_brush_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Brush & Nib Calibration",
                        tint = palette.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = String.format("%.1fpx", strokeWidth),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
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
