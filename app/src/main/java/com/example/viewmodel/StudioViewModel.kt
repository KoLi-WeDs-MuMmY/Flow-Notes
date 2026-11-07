package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SketchPageEntity
import com.example.data.SketchRepository
import com.example.data.StudioDatabase
import com.example.model.CanvasTextNote
import com.example.model.DevicePosture
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.PaperTemplate
import com.example.model.ScreenCoveragePreset
import com.example.model.ScreenFloatMetrics
import com.example.model.StrokePoint
import com.example.model.StudioPalettes
import com.example.util.ColorMath
import com.example.util.HsvColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class StudioUiState(
    val activePageId: Long = 0L,
    val activePageTitle: String = "Studio Welcome & Color Study",
    val activePageCategory: String = "Drawing & Notes",
    val paperTemplate: PaperTemplate = PaperTemplate.DOT_MATRIX,
    val selectedTool: DrawingTool = DrawingTool.FOUNTAIN_PEN,
    val activeColor: Color = Color(0xFFE85D04),
    val hsvColor: HsvColor = ColorMath.colorToHsv(Color(0xFFE85D04)),
    val strokeWidth: Float = 5.5f,
    val strokeAlpha: Float = 1.0f,
    val strokes: List<DrawnStroke> = emptyList(),
    val redoStrokes: List<DrawnStroke> = emptyList(),
    val textNotes: List<CanvasTextNote> = emptyList(),
    // Pinch-to-Zoom & Pan Gesture State (Float)
    val zoomFloat: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val isPanNavigationMode: Boolean = false,
    val showMinimap: Boolean = true,
    // Multi-Screen Float & Coverage Percentages State
    val coveragePreset: ScreenCoveragePreset = ScreenCoveragePreset.STUDIO_BALANCED,
    val customCoverageFloat: Float = 0.90f,
    // Normalized float position (0.0f..1.0f) for draggable floating studio palette
    val floatingPaletteXFraction: Float = 0.50f,
    val floatingPaletteYFraction: Float = 0.92f,
    // Color Picker & Swatches
    val customSavedSwatches: List<Color> = StudioPalettes.CuratedSwatches,
    val recentColors: List<Color> = listOf(
        Color(0xFFE85D04),
        Color(0xFF1D4ED8),
        Color(0xFF2A9D8F),
        Color(0xFFF4A261),
        Color(0xFF18181B)
    ),
    // Dialog / Sheet Modals
    val isColorPickerOpen: Boolean = false,
    val isBrushSettingsOpen: Boolean = false,
    val isPaperPickerOpen: Boolean = false,
    val isScreenInspectorOpen: Boolean = false,
    val isSketchbookDrawerOpen: Boolean = false,
    val isAddNoteDialogOpen: Boolean = false
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SketchRepository = SketchRepository(
        StudioDatabase.getInstance(application).sketchPageDao()
    )

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val savedPages: StateFlow<List<SketchPageEntity>> = repository.allPages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val firstSeededId = repository.seedStarterPagesIfEmpty()
            val pageToLoad = if (firstSeededId > 0) {
                repository.getPageById(firstSeededId)
            } else {
                repository.getPageById(1L)
            }
            if (pageToLoad != null) {
                applyLoadedPage(pageToLoad)
            }
        }
    }

    private fun applyLoadedPage(entity: SketchPageEntity) {
        val template = runCatching {
            PaperTemplate.valueOf(entity.paperTemplateName)
        }.getOrDefault(PaperTemplate.DOT_MATRIX)
        val loadedStrokes = SketchRepository.deserializeStrokes(entity.strokesPayload)
        val loadedNotes = SketchRepository.deserializeTextNotes(entity.textNotesPayload)

        _uiState.update { state ->
            state.copy(
                activePageId = entity.id,
                activePageTitle = entity.title,
                activePageCategory = entity.categoryTag,
                paperTemplate = template,
                strokes = loadedStrokes,
                redoStrokes = emptyList(),
                textNotes = loadedNotes,
                zoomFloat = entity.lastZoomFloat.coerceIn(0.25f, 8.0f),
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
    }

    fun selectTool(tool: DrawingTool) {
        _uiState.update { state ->
            state.copy(
                selectedTool = tool,
                strokeWidth = tool.defaultWidth,
                strokeAlpha = tool.defaultAlpha,
                isPanNavigationMode = false
            )
        }
    }

    fun updateStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width.coerceIn(1.0f, 64.0f)) }
    }

    fun updateStrokeAlpha(alpha: Float) {
        _uiState.update { state ->
            val clamped = alpha.coerceIn(0.05f, 1.0f)
            state.copy(
                strokeAlpha = clamped,
                hsvColor = state.hsvColor.copy(alpha = clamped)
            )
        }
    }

    fun selectColor(color: Color) {
        val solidColor = color.copy(alpha = 1f)
        val hsv = ColorMath.colorToHsv(solidColor).copy(alpha = _uiState.value.strokeAlpha)
        _uiState.update { state ->
            val updatedRecents = (listOf(solidColor) + state.recentColors.filterNot {
                ColorMath.toHexCode(it) == ColorMath.toHexCode(solidColor)
            }).take(8)
            state.copy(
                activeColor = solidColor,
                hsvColor = hsv,
                recentColors = updatedRecents,
                selectedTool = if (state.selectedTool == DrawingTool.ERASER) {
                    DrawingTool.FOUNTAIN_PEN
                } else {
                    state.selectedTool
                }
            )
        }
    }

    fun updateHsvColor(hue: Float, saturation: Float, value: Float, alpha: Float = _uiState.value.strokeAlpha) {
        val newHsv = HsvColor(
            hue = hue.coerceIn(0f, 360f),
            saturation = saturation.coerceIn(0f, 1f),
            value = value.coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0.05f, 1f)
        )
        val newComposeColor = newHsv.copy(alpha = 1f).toComposeColor()
        _uiState.update { state ->
            state.copy(
                hsvColor = newHsv,
                activeColor = newComposeColor,
                strokeAlpha = newHsv.alpha
            )
        }
    }

    fun saveCurrentColorToSwatches() {
        val current = _uiState.value.activeColor
        _uiState.update { state ->
            val exists = state.customSavedSwatches.any {
                ColorMath.toHexCode(it) == ColorMath.toHexCode(current)
            }
            if (exists) state else state.copy(
                customSavedSwatches = listOf(current) + state.customSavedSwatches
            )
        }
    }

    fun commitStroke(points: List<StrokePoint>) {
        if (points.isEmpty()) return
        val state = _uiState.value
        val strokeColor = if (state.selectedTool == DrawingTool.ERASER) {
            Color(state.paperTemplate.backgroundColorHex)
        } else {
            state.activeColor
        }
        val newStroke = DrawnStroke(
            id = UUID.randomUUID().toString(),
            points = points,
            colorArgb = ColorMath.colorToArgbLong(strokeColor),
            strokeWidth = state.strokeWidth,
            alpha = if (state.selectedTool == DrawingTool.ERASER) 1.0f else state.strokeAlpha,
            tool = state.selectedTool
        )
        _uiState.update {
            it.copy(
                strokes = it.strokes + newStroke,
                redoStrokes = emptyList()
            )
        }
        triggerAutoSave()
    }

    fun addTextNote(text: String, canvasX: Float = 180f, canvasY: Float = 220f, fontSizeSp: Float = 18f) {
        if (text.isBlank()) return
        val state = _uiState.value
        val note = CanvasTextNote(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            x = canvasX,
            y = canvasY,
            colorArgb = ColorMath.colorToArgbLong(state.activeColor),
            fontSizeSp = fontSizeSp
        )
        _uiState.update {
            it.copy(
                textNotes = it.textNotes + note,
                isAddNoteDialogOpen = false
            )
        }
        triggerAutoSave()
    }

    fun removeTextNote(noteId: String) {
        _uiState.update { state ->
            state.copy(textNotes = state.textNotes.filterNot { it.id == noteId })
        }
        triggerAutoSave()
    }

    fun undo() {
        _uiState.update { state ->
            if (state.strokes.isEmpty()) return@update state
            val removed = state.strokes.last()
            state.copy(
                strokes = state.strokes.dropLast(1),
                redoStrokes = state.redoStrokes + removed
            )
        }
        triggerAutoSave()
    }

    fun redo() {
        _uiState.update { state ->
            if (state.redoStrokes.isEmpty()) return@update state
            val restored = state.redoStrokes.last()
            state.copy(
                strokes = state.strokes + restored,
                redoStrokes = state.redoStrokes.dropLast(1)
            )
        }
        triggerAutoSave()
    }

    fun clearCanvas() {
        _uiState.update { state ->
            state.copy(
                strokes = emptyList(),
                redoStrokes = emptyList(),
                textNotes = emptyList()
            )
        }
        triggerAutoSave()
    }

    // Pinch-to-Zoom and Gesture Transform Handlers
    fun onPinchZoomAndPan(
        centroid: Offset,
        panDelta: Offset,
        zoomChange: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float
    ) {
        _uiState.update { state ->
            val oldZoom = state.zoomFloat
            val newZoom = (oldZoom * zoomChange).coerceIn(0.25f, 8.0f)
            val effectiveZoomFactor = newZoom / oldZoom

            // Zoom smoothly around the pinch centroid in screen space
            val newPanX = centroid.x - (centroid.x - state.panOffsetX) * effectiveZoomFactor + panDelta.x
            val newPanY = centroid.y - (centroid.y - state.panOffsetY) * effectiveZoomFactor + panDelta.y

            val maxBoundX = viewportWidthPx * newZoom * 1.5f
            val maxBoundY = viewportHeightPx * newZoom * 1.5f

            state.copy(
                zoomFloat = newZoom,
                panOffsetX = newPanX.coerceIn(-maxBoundX, maxBoundX),
                panOffsetY = newPanY.coerceIn(-maxBoundY, maxBoundY)
            )
        }
    }

    fun stepZoom(multiplier: Float, viewportCenter: Offset = Offset(400f, 400f)) {
        _uiState.update { state ->
            val oldZoom = state.zoomFloat
            val newZoom = (oldZoom * multiplier).coerceIn(0.25f, 8.0f)
            val factor = newZoom / oldZoom
            val newPanX = viewportCenter.x - (viewportCenter.x - state.panOffsetX) * factor
            val newPanY = viewportCenter.y - (viewportCenter.y - state.panOffsetY) * factor
            state.copy(
                zoomFloat = newZoom,
                panOffsetX = newPanX,
                panOffsetY = newPanY
            )
        }
        triggerAutoSave()
    }

    fun setExactZoomFloat(targetZoom: Float) {
        _uiState.update { state ->
            state.copy(zoomFloat = targetZoom.coerceIn(0.25f, 8.0f))
        }
        triggerAutoSave()
    }

    fun resetZoomAndPan() {
        _uiState.update { state ->
            state.copy(
                zoomFloat = 1.0f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
        triggerAutoSave()
    }

    fun togglePanNavigationMode() {
        _uiState.update { it.copy(isPanNavigationMode = !it.isPanNavigationMode) }
    }

    fun toggleMinimap() {
        _uiState.update { it.copy(showMinimap = !it.showMinimap) }
    }

    // Multi-Screen Float & Screen Coverage Controls
    fun selectCoveragePreset(preset: ScreenCoveragePreset) {
        _uiState.update { state ->
            state.copy(
                coveragePreset = preset,
                customCoverageFloat = preset.targetCanvasFloat
            )
        }
    }

    fun updateCustomCoverageFloat(fraction: Float) {
        _uiState.update { state ->
            state.copy(
                coveragePreset = ScreenCoveragePreset.CUSTOM_FLOAT,
                customCoverageFloat = fraction.coerceIn(0.65f, 0.98f)
            )
        }
    }

    fun updateFloatingPaletteFraction(xFraction: Float, yFraction: Float) {
        _uiState.update { state ->
            state.copy(
                floatingPaletteXFraction = xFraction.coerceIn(0.08f, 0.92f),
                floatingPaletteYFraction = yFraction.coerceIn(0.10f, 0.94f)
            )
        }
    }

    fun computeScreenFloatMetrics(widthDp: Float, heightDp: Float): ScreenFloatMetrics {
        val safeW = widthDp.coerceAtLeast(1f)
        val safeH = heightDp.coerceAtLeast(1f)
        val aspect = safeW / safeH

        val posture = when {
            safeW >= 840f -> DevicePosture.TABLET_EXPANDED
            safeW >= 600f && aspect in 0.75f..1.45f -> DevicePosture.FOLDABLE_MEDIUM
            aspect > 1.25f -> DevicePosture.MOBILE_LANDSCAPE
            else -> DevicePosture.MOBILE_PORTRAIT
        }

        val state = _uiState.value
        val targetCanvasFloat = when (state.coveragePreset) {
            ScreenCoveragePreset.CUSTOM_FLOAT -> state.customCoverageFloat
            else -> state.coveragePreset.targetCanvasFloat
        }.coerceIn(0.65f, 0.98f)

        val hudBudgetFloat = (1.0f - targetCanvasFloat).coerceIn(0.02f, 0.35f)

        val topBarFraction = when {
            state.coveragePreset == ScreenCoveragePreset.MAX_IMMERSION -> 0.045f
            posture == DevicePosture.MOBILE_LANDSCAPE -> 0.09f
            posture == DevicePosture.TABLET_EXPANDED -> 0.065f
            else -> 0.072f
        }

        val bottomDockWidthFraction = when (posture) {
            DevicePosture.MOBILE_PORTRAIT -> (0.72f + hudBudgetFloat * 0.8f).coerceIn(0.68f, 0.96f)
            DevicePosture.MOBILE_LANDSCAPE -> 0.64f
            DevicePosture.FOLDABLE_MEDIUM -> 0.76f
            DevicePosture.TABLET_EXPANDED -> 0.58f
        }

        val sideRailWidthFraction = when (posture) {
            DevicePosture.TABLET_EXPANDED -> 0.075f
            DevicePosture.FOLDABLE_MEDIUM -> 0.095f
            DevicePosture.MOBILE_LANDSCAPE -> 0.090f
            DevicePosture.MOBILE_PORTRAIT -> 0.0f
        }

        val inspectorWidthFraction = when {
            state.coveragePreset == ScreenCoveragePreset.DRAFTING_SPLIT && posture == DevicePosture.TABLET_EXPANDED -> 0.24f
            state.coveragePreset == ScreenCoveragePreset.DRAFTING_SPLIT && posture != DevicePosture.MOBILE_PORTRAIT -> 0.28f
            state.coveragePreset == ScreenCoveragePreset.DRAFTING_SPLIT -> 0.92f
            else -> 0.0f
        }

        return ScreenFloatMetrics(
            screenWidthDp = safeW,
            screenHeightDp = safeH,
            aspectRatioFloat = aspect,
            posture = posture,
            effectiveCanvasCoverageFloat = targetCanvasFloat,
            hudCoverageFloat = hudBudgetFloat,
            topBarHeightFraction = topBarFraction,
            bottomDockWidthFraction = bottomDockWidthFraction,
            sideRailWidthFraction = sideRailWidthFraction,
            inspectorWidthFraction = inspectorWidthFraction
        )
    }

    // Paper & Sketchbook Management
    fun selectPaperTemplate(template: PaperTemplate) {
        _uiState.update { state ->
            // If switching to dark slate and current ink is very dark, switch to bright chalk/amber
            val nextColor = if (template.isDark && state.activeColor == Color(0xFF18181B)) {
                Color(0xFFF8FAFC)
            } else if (!template.isDark && state.activeColor == Color(0xFFF8FAFC)) {
                Color(0xFF18181B)
            } else {
                state.activeColor
            }
            state.copy(
                paperTemplate = template,
                activeColor = nextColor,
                isPaperPickerOpen = false
            )
        }
        triggerAutoSave()
    }

    fun loadSketchPage(page: SketchPageEntity) {
        saveActivePageNow()
        applyLoadedPage(page)
        _uiState.update { it.copy(isSketchbookDrawerOpen = false) }
    }

    fun createNewSketchPage(title: String, category: String, template: PaperTemplate) {
        saveActivePageNow()
        viewModelScope.launch {
            val cleanTitle = title.ifBlank { "Untitled Studio Sheet" }
            val defaultColor = if (template.isDark) Color(0xFFF4A261) else Color(0xFF18181B)
            val newId = repository.savePage(
                id = 0L,
                title = cleanTitle,
                categoryTag = category,
                paperTemplate = template,
                strokes = emptyList(),
                textNotes = emptyList(),
                zoomFloat = 1.0f
            )
            _uiState.update { state ->
                state.copy(
                    activePageId = newId,
                    activePageTitle = cleanTitle,
                    activePageCategory = category,
                    paperTemplate = template,
                    activeColor = defaultColor,
                    strokes = emptyList(),
                    redoStrokes = emptyList(),
                    textNotes = emptyList(),
                    zoomFloat = 1.0f,
                    panOffsetX = 0f,
                    panOffsetY = 0f,
                    isSketchbookDrawerOpen = false
                )
            }
        }
    }

    fun deleteSketchPage(pageId: Long) {
        viewModelScope.launch {
            val pages = savedPages.value
            if (pages.size <= 1) {
                // Clear instead of deleting the very last sheet
                clearCanvas()
                return@launch
            }
            repository.deletePage(pageId)
            if (_uiState.value.activePageId == pageId) {
                val remaining = pages.firstOrNull { it.id != pageId }
                if (remaining != null) {
                    applyLoadedPage(remaining)
                }
            }
        }
    }

    // Modal Toggles
    fun setColorPickerOpen(open: Boolean) {
        _uiState.update { it.copy(isColorPickerOpen = open) }
    }

    fun setBrushSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isBrushSettingsOpen = open) }
    }

    fun setPaperPickerOpen(open: Boolean) {
        _uiState.update { it.copy(isPaperPickerOpen = open) }
    }

    fun setScreenInspectorOpen(open: Boolean) {
        _uiState.update { it.copy(isScreenInspectorOpen = open) }
    }

    fun setSketchbookDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(isSketchbookDrawerOpen = open) }
    }

    fun setAddNoteDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddNoteDialogOpen = open) }
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350)
            saveActivePageNow()
        }
    }

    private fun saveActivePageNow() {
        val state = _uiState.value
        viewModelScope.launch {
            val savedId = repository.savePage(
                id = state.activePageId,
                title = state.activePageTitle,
                categoryTag = state.activePageCategory,
                paperTemplate = state.paperTemplate,
                strokes = state.strokes,
                textNotes = state.textNotes,
                zoomFloat = state.zoomFloat
            )
            if (state.activePageId == 0L && savedId > 0L) {
                _uiState.update { it.copy(activePageId = savedId) }
            }
        }
    }
}
