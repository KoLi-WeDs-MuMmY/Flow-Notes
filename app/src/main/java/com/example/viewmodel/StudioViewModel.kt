package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FolderEntity
import com.example.data.SketchPageEntity
import com.example.data.SketchRepository
import com.example.data.StudioDatabase
import com.example.model.AppDestination
import com.example.model.CanvasTextNote
import com.example.model.DevicePosture
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.FloatingLayoutPreset
import com.example.model.FolderIconType
import com.example.model.HomeFilterMode
import com.example.model.PaperTemplate
import com.example.model.ScreenCoveragePreset
import com.example.model.ScreenFloatMetrics
import com.example.model.StrokePoint
import com.example.model.StudioCustomization
import com.example.model.StudioPalettes
import com.example.model.SymbolStampType
import com.example.ui.theme.StudioThemeId
import com.example.util.ColorMath
import com.example.util.HsvColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class StudioUiState(
    // Navigation & Nested Folder Explorer State
    val currentDestination: AppDestination = AppDestination.HOME_EXPLORER,
    val currentFolderId: Long? = null,
    val homeFilterMode: HomeFilterMode = HomeFilterMode.EXPLORER,
    val searchQuery: String = "",
    // Theme & Deep Workspace Customization
    val customization: StudioCustomization = StudioCustomization(),
    val isCustomizationModalOpen: Boolean = false,
    // Active Sheet State in Studio Canvas
    val activePageId: Long = 0L,
    val activePageFolderId: Long? = null,
    val activePageIsFavorite: Boolean = false,
    val activePageTitle: String = "Atelier Welcome & Pigment Study",
    val activePageCategory: String = "Drawing & Notes",
    val paperTemplate: PaperTemplate = PaperTemplate.DOT_MATRIX,
    val selectedTool: DrawingTool = DrawingTool.FOUNTAIN_PEN,
    val activeColor: Color = Color(0xFF232625), // Sumi Carbon Ink
    val hsvColor: HsvColor = ColorMath.colorToHsv(Color(0xFF232625)),
    val strokeWidth: Float = 5.2f,
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
    // Independent Normalized Float Positions (0.0f..1.0f) for Modular Floating Widgets
    val toolPodXFraction: Float = 0.04f,
    val toolPodYFraction: Float = 0.24f,
    val colorPodXFraction: Float = 0.50f,
    val colorPodYFraction: Float = 0.92f,
    val symbolPodXFraction: Float = 0.94f,
    val symbolPodYFraction: Float = 0.42f,
    val scratchpadXFraction: Float = 0.72f,
    val scratchpadYFraction: Float = 0.20f,
    // Color Picker & Swatches
    val customSavedSwatches: List<Color> = StudioPalettes.CuratedSwatches,
    val recentColors: List<Color> = listOf(
        Color(0xFF232625),
        Color(0xFF8C5843),
        Color(0xFF526B5C),
        Color(0xFF3D5265),
        Color(0xFFA67C52)
    ),
    // Dialog / Sheet Modals
    val isColorPickerOpen: Boolean = false,
    val isBrushSettingsOpen: Boolean = false,
    val isPaperPickerOpen: Boolean = false,
    val isScreenInspectorOpen: Boolean = false,
    val isSketchbookDrawerOpen: Boolean = false,
    val isAddNoteDialogOpen: Boolean = false,
    val isNewFolderDialogOpen: Boolean = false,
    val isNewSheetDialogOpen: Boolean = false,
    val pageToMove: SketchPageEntity? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudioDatabase.getInstance(application)
    private val repository: SketchRepository = SketchRepository(
        pageDao = db.sketchPageDao(),
        folderDao = db.folderDao()
    )

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val savedPages: StateFlow<List<SketchPageEntity>> = repository.allPages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val firstSeededId = repository.seedStarterWorkspaceIfEmpty()
            val pageToLoad = if (firstSeededId > 0) {
                repository.getPageById(firstSeededId)
            } else {
                repository.getPageById(1L)
            }
            if (pageToLoad != null) {
                applyLoadedPageOffThread(pageToLoad)
            }
        }
    }

    private suspend fun applyLoadedPageOffThread(entity: SketchPageEntity) {
        val (template, loadedStrokes, loadedNotes) = withContext(Dispatchers.Default) {
            val tpl = runCatching {
                PaperTemplate.valueOf(entity.paperTemplateName)
            }.getOrDefault(PaperTemplate.DOT_MATRIX)
            val strokes = SketchRepository.deserializeStrokes(entity.strokesPayload)
            val notes = SketchRepository.deserializeTextNotes(entity.textNotesPayload)
            Triple(tpl, strokes, notes)
        }

        _uiState.update { state ->
            state.copy(
                activePageId = entity.id,
                activePageFolderId = entity.folderId,
                activePageIsFavorite = entity.isFavorite,
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

    // --- Theme & Deep Workspace Customization ---

    fun selectStudioTheme(themeId: StudioThemeId) {
        _uiState.update { state ->
            state.copy(customization = state.customization.copy(themeId = themeId))
        }
    }

    fun updateCornerRadiusDp(radiusDp: Float) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(
                    cornerRadiusDp = radiusDp.coerceIn(6f, 28f)
                )
            )
        }
    }

    fun updateWidgetOpacityFloat(opacity: Float) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(
                    widgetOpacityFloat = opacity.coerceIn(0.70f, 1.00f)
                )
            )
        }
    }

    fun updateGridCustomization(spacingMultiplier: Float, lineOpacity: Float) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(
                    gridSpacingMultiplierFloat = spacingMultiplier.coerceIn(0.55f, 2.20f),
                    gridLineOpacityFloat = lineOpacity.coerceIn(0.15f, 1.00f)
                )
            )
        }
    }

    fun applyFloatingLayoutPreset(preset: FloatingLayoutPreset) {
        _uiState.update { state ->
            when (preset) {
                FloatingLayoutPreset.ATELIER_BALANCED -> state.copy(
                    customization = state.customization.copy(
                        layoutPreset = preset,
                        showToolPod = true,
                        isToolPodCompact = false,
                        showColorPod = true,
                        showZoomCompassPod = true,
                        showSymbolStampPod = true
                    ),
                    toolPodXFraction = 0.04f,
                    toolPodYFraction = 0.24f,
                    colorPodXFraction = 0.50f,
                    colorPodYFraction = 0.92f,
                    symbolPodXFraction = 0.94f,
                    symbolPodYFraction = 0.42f
                )

                FloatingLayoutPreset.LEFT_HANDED_STUDIO -> state.copy(
                    customization = state.customization.copy(
                        layoutPreset = preset,
                        showToolPod = true,
                        isToolPodCompact = false,
                        showColorPod = true,
                        showSymbolStampPod = true
                    ),
                    toolPodXFraction = 0.04f,
                    toolPodYFraction = 0.22f,
                    symbolPodXFraction = 0.04f,
                    symbolPodYFraction = 0.68f,
                    colorPodXFraction = 0.62f,
                    colorPodYFraction = 0.92f
                )

                FloatingLayoutPreset.RIGHT_HANDED_STUDIO -> state.copy(
                    customization = state.customization.copy(
                        layoutPreset = preset,
                        showToolPod = true,
                        isToolPodCompact = false,
                        showColorPod = true,
                        showSymbolStampPod = true
                    ),
                    toolPodXFraction = 0.94f,
                    toolPodYFraction = 0.22f,
                    symbolPodXFraction = 0.94f,
                    symbolPodYFraction = 0.68f,
                    colorPodXFraction = 0.38f,
                    colorPodYFraction = 0.92f
                )

                FloatingLayoutPreset.MINIMAL_CORNERS -> state.copy(
                    customization = state.customization.copy(
                        layoutPreset = preset,
                        showToolPod = true,
                        isToolPodCompact = true,
                        showColorPod = true,
                        showSymbolStampPod = false
                    ),
                    toolPodXFraction = 0.04f,
                    toolPodYFraction = 0.88f,
                    colorPodXFraction = 0.55f,
                    colorPodYFraction = 0.92f
                )
            }
        }
    }

    fun toggleFloatingWidget(
        toolPod: Boolean? = null,
        toolPodCompact: Boolean? = null,
        colorPod: Boolean? = null,
        zoomPod: Boolean? = null,
        symbolPod: Boolean? = null,
        scratchpadPod: Boolean? = null,
        rulerOverlay: Boolean? = null
    ) {
        _uiState.update { state ->
            val c = state.customization
            state.copy(
                customization = c.copy(
                    showToolPod = toolPod ?: c.showToolPod,
                    isToolPodCompact = toolPodCompact ?: c.isToolPodCompact,
                    showColorPod = colorPod ?: c.showColorPod,
                    showZoomCompassPod = zoomPod ?: c.showZoomCompassPod,
                    showSymbolStampPod = symbolPod ?: c.showSymbolStampPod,
                    showScratchpadPod = scratchpadPod ?: c.showScratchpadPod,
                    showRulerGuideOverlay = rulerOverlay ?: c.showRulerGuideOverlay
                )
            )
        }
    }

    fun updateRulerOverlay(angleDegrees: Float? = null, yFraction: Float? = null) {
        _uiState.update { state ->
            val c = state.customization
            state.copy(
                customization = c.copy(
                    rulerAngleDegrees = (angleDegrees ?: c.rulerAngleDegrees).coerceIn(-85f, 85f),
                    rulerYFraction = (yFraction ?: c.rulerYFraction).coerceIn(0.12f, 0.88f)
                )
            )
        }
    }

    fun updateScratchpadText(text: String) {
        _uiState.update { state ->
            state.copy(
                customization = state.customization.copy(scratchpadNoteText = text)
            )
        }
    }

    fun updateToolPodPosition(xFrac: Float, yFrac: Float) {
        _uiState.update {
            it.copy(
                toolPodXFraction = xFrac.coerceIn(0.02f, 0.94f),
                toolPodYFraction = yFrac.coerceIn(0.10f, 0.92f)
            )
        }
    }

    fun updateColorPodPosition(xFrac: Float, yFrac: Float) {
        _uiState.update {
            it.copy(
                colorPodXFraction = xFrac.coerceIn(0.08f, 0.92f),
                colorPodYFraction = yFrac.coerceIn(0.10f, 0.95f)
            )
        }
    }

    fun updateSymbolPodPosition(xFrac: Float, yFrac: Float) {
        _uiState.update {
            it.copy(
                symbolPodXFraction = xFrac.coerceIn(0.02f, 0.96f),
                symbolPodYFraction = yFrac.coerceIn(0.12f, 0.90f)
            )
        }
    }

    fun updateScratchpadPosition(xFrac: Float, yFrac: Float) {
        _uiState.update {
            it.copy(
                scratchpadXFraction = xFrac.coerceIn(0.04f, 0.90f),
                scratchpadYFraction = yFrac.coerceIn(0.12f, 0.88f)
            )
        }
    }

    fun setCustomizationModalOpen(open: Boolean) {
        _uiState.update { it.copy(isCustomizationModalOpen = open) }
    }

    // --- Home Page & Nested Folder Navigation ---

    fun navigateToHomeExplorer() {
        saveActivePageNow()
        _uiState.update {
            it.copy(
                currentDestination = AppDestination.HOME_EXPLORER,
                isSketchbookDrawerOpen = false
            )
        }
    }

    fun openSheetInCanvas(page: SketchPageEntity) {
        if (_uiState.value.currentDestination == AppDestination.STUDIO_CANVAS) {
            saveActivePageNow()
        }
        viewModelScope.launch {
            applyLoadedPageOffThread(page)
            _uiState.update {
                it.copy(
                    currentDestination = AppDestination.STUDIO_CANVAS,
                    isSketchbookDrawerOpen = false
                )
            }
        }
    }

    fun resumeActiveCanvas() {
        _uiState.update { it.copy(currentDestination = AppDestination.STUDIO_CANVAS) }
    }

    fun selectHomeFilterMode(mode: HomeFilterMode) {
        _uiState.update { it.copy(homeFilterMode = mode) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openFolder(folderId: Long?) {
        _uiState.update {
            it.copy(
                currentFolderId = folderId,
                homeFilterMode = HomeFilterMode.EXPLORER
            )
        }
    }

    fun navigateUpFolder(): Boolean {
        val currentId = _uiState.value.currentFolderId ?: return false
        val currentFolder = allFolders.value.firstOrNull { it.id == currentId }
        _uiState.update { it.copy(currentFolderId = currentFolder?.parentFolderId) }
        return true
    }

    fun buildBreadcrumbPath(targetFolderId: Long?, folders: List<FolderEntity>): List<FolderEntity> {
        if (targetFolderId == null) return emptyList()
        val folderMap = folders.associateBy { it.id }
        val chain = ArrayList<FolderEntity>()
        var cursorId: Long? = targetFolderId
        val visited = HashSet<Long>()
        while (cursorId != null && visited.add(cursorId)) {
            val folder = folderMap[cursorId] ?: break
            chain.add(folder)
            cursorId = folder.parentFolderId
        }
        chain.reverse()
        return chain
    }

    fun createSubfolder(name: String, colorArgb: Long, iconType: FolderIconType) {
        val parentId = _uiState.value.currentFolderId
        viewModelScope.launch {
            repository.createFolder(
                name = name,
                parentFolderId = parentId,
                colorArgb = colorArgb,
                iconType = iconType
            )
            _uiState.update { it.copy(isNewFolderDialogOpen = false) }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            val deletingCurrent = _uiState.value.currentFolderId == folderId
            val parentId = allFolders.value.firstOrNull { it.id == folderId }?.parentFolderId
            repository.deleteFolderSafely(folderId)
            if (deletingCurrent) {
                _uiState.update { it.copy(currentFolderId = parentId) }
            }
        }
    }

    fun movePageToFolder(pageId: Long, targetFolderId: Long?) {
        viewModelScope.launch {
            repository.movePageToFolder(pageId, targetFolderId)
            _uiState.update { state ->
                val updatedActiveFolder = if (state.activePageId == pageId) targetFolderId else state.activePageFolderId
                state.copy(
                    activePageFolderId = updatedActiveFolder,
                    pageToMove = null
                )
            }
        }
    }

    fun togglePageFavorite(page: SketchPageEntity) {
        viewModelScope.launch {
            repository.togglePageFavorite(page)
            if (_uiState.value.activePageId == page.id) {
                _uiState.update { it.copy(activePageIsFavorite = !page.isFavorite) }
            }
        }
    }

    fun duplicatePage(page: SketchPageEntity) {
        viewModelScope.launch {
            repository.duplicatePage(page)
        }
    }

    // --- Studio Drawing, Writing & Symbol Stamp Controls ---

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

    fun stampSymbolOnCanvas(symbol: SymbolStampType) {
        val state = _uiState.value
        val jitter = (state.textNotes.size % 6) * 26f
        val targetX = ((-state.panOffsetX + 240f) / state.zoomFloat) + jitter
        val targetY = ((-state.panOffsetY + 300f) / state.zoomFloat) + jitter
        addTextNote(
            text = symbol.glyph,
            canvasX = targetX,
            canvasY = targetY,
            fontSizeSp = symbol.defaultSizeSp
        )
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

    // --- Pinch-to-Zoom and Gesture Transform Handlers ---

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

    // --- Variable-Screen Float & Screen Coverage Controls ---

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

    fun computeScreenFloatMetrics(widthDp: Float, heightDp: Float): ScreenFloatMetrics {
        val safeW = widthDp.coerceAtLeast(1f)
        val safeH = heightDp.coerceAtLeast(1f)
        val aspect = safeW / safeH

        val posture = when {
            safeW >= 840f -> DevicePosture.TABLET_EXPANDED
            safeW >= 600f && aspect in 0.70f..1.55f -> DevicePosture.FOLDABLE_MEDIUM
            aspect > 1.22f -> DevicePosture.MOBILE_LANDSCAPE
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
            DevicePosture.MOBILE_PORTRAIT -> (0.72f + hudBudgetFloat * 0.8f).coerceIn(0.68f, 0.94f)
            DevicePosture.MOBILE_LANDSCAPE -> 0.62f
            DevicePosture.FOLDABLE_MEDIUM -> 0.70f
            DevicePosture.TABLET_EXPANDED -> 0.54f
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

        val homeSidebarWidthFraction = when (posture) {
            DevicePosture.TABLET_EXPANDED -> 0.26f
            DevicePosture.FOLDABLE_MEDIUM -> 0.30f
            DevicePosture.MOBILE_LANDSCAPE -> 0.32f
            DevicePosture.MOBILE_PORTRAIT -> 0.0f
        }

        val homeContentWidthFraction = (1.0f - homeSidebarWidthFraction).coerceIn(0.65f, 1.0f)

        val homeGridColumns = when {
            safeW >= 1080f -> 3
            safeW >= 600f -> 2
            else -> 1
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
            inspectorWidthFraction = inspectorWidthFraction,
            homeSidebarWidthFraction = homeSidebarWidthFraction,
            homeContentWidthFraction = homeContentWidthFraction,
            homeGridColumns = homeGridColumns
        )
    }

    // --- Paper & Sheet Creation ---

    fun selectPaperTemplate(template: PaperTemplate) {
        _uiState.update { state ->
            val nextColor = if (template.isDark && state.activeColor == Color(0xFF232625)) {
                Color(0xFFF6F4EE)
            } else if (!template.isDark && state.activeColor == Color(0xFFF6F4EE)) {
                Color(0xFF232625)
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

    fun createNewSketchPage(
        title: String,
        category: String,
        template: PaperTemplate,
        targetFolderId: Long? = _uiState.value.currentFolderId,
        openImmediately: Boolean = true
    ) {
        if (_uiState.value.currentDestination == AppDestination.STUDIO_CANVAS) {
            saveActivePageNow()
        }
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank { "Untitled Studio Sheet" }
            val defaultColor = if (template.isDark) Color(0xFFA67C52) else Color(0xFF232625)
            val newId = repository.savePage(
                id = 0L,
                folderId = targetFolderId,
                isFavorite = false,
                title = cleanTitle,
                categoryTag = category,
                paperTemplate = template,
                strokes = emptyList(),
                textNotes = emptyList(),
                zoomFloat = 1.0f
            )
            _uiState.update { state ->
                state.copy(
                    currentDestination = if (openImmediately) AppDestination.STUDIO_CANVAS else state.currentDestination,
                    activePageId = newId,
                    activePageFolderId = targetFolderId,
                    activePageIsFavorite = false,
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
                    isSketchbookDrawerOpen = false,
                    isNewSheetDialogOpen = false
                )
            }
        }
    }

    fun deleteSketchPage(pageId: Long) {
        viewModelScope.launch {
            val pages = savedPages.value
            if (pages.size <= 1) {
                clearCanvas()
                return@launch
            }
            repository.deletePage(pageId)
            if (_uiState.value.activePageId == pageId) {
                val remaining = pages.firstOrNull { it.id != pageId }
                if (remaining != null) {
                    applyLoadedPageOffThread(remaining)
                }
            }
        }
    }

    // --- Modal Toggles ---

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

    fun setNewFolderDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isNewFolderDialogOpen = open) }
    }

    fun setNewSheetDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isNewSheetDialogOpen = open) }
    }

    fun setPageToMove(page: SketchPageEntity?) {
        _uiState.update { it.copy(pageToMove = page) }
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(450)
            saveActivePageNow()
        }
    }

    private fun saveActivePageNow() {
        val state = _uiState.value
        if (state.activePageId <= 0L && state.strokes.isEmpty() && state.textNotes.isEmpty()) return
        viewModelScope.launch {
            val savedId = repository.savePage(
                id = state.activePageId,
                folderId = state.activePageFolderId,
                isFavorite = state.activePageIsFavorite,
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
