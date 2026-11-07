package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.FolderEntity
import com.example.data.SketchPageEntity
import com.example.model.FloatingLayoutPreset
import com.example.model.HomeFilterMode
import com.example.model.PaperTemplate
import com.example.model.ScreenFloatMetrics
import com.example.ui.components.CreateSheetDialog
import com.example.ui.components.CreateSubfolderDialog
import com.example.ui.components.MoveSheetToFolderDialog
import com.example.ui.components.ScreenCoverageInspectorDialog
import com.example.ui.components.StudioCustomizationDialog
import com.example.ui.components.glyphForFolderType
import com.example.ui.components.iconForFolderType
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.StudioThemeId
import com.example.util.ColorMath
import com.example.viewmodel.StudioViewModel

@Composable
fun HomeExplorerScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val savedPages by viewModel.savedPages.collectAsStateWithLifecycle()

    val breadcrumbs = remember(state.currentFolderId, allFolders) {
        viewModel.buildBreadcrumbPath(state.currentFolderId, allFolders)
    }

    val currentFolderName = breadcrumbs.lastOrNull()?.name ?: "Root Workspace"

    val query = state.searchQuery.trim().lowercase()

    val displayedFolders = remember(allFolders, state.currentFolderId, state.homeFilterMode, query) {
        if (query.isNotEmpty()) {
            allFolders.filter { it.name.lowercase().contains(query) }
        } else if (state.homeFilterMode == HomeFilterMode.EXPLORER) {
            allFolders.filter { it.parentFolderId == state.currentFolderId }
        } else {
            emptyList()
        }
    }

    val displayedPages = remember(savedPages, state.currentFolderId, state.homeFilterMode, query) {
        when {
            query.isNotEmpty() -> savedPages.filter {
                it.title.lowercase().contains(query) || it.categoryTag.lowercase().contains(query)
            }
            state.homeFilterMode == HomeFilterMode.FAVORITES -> savedPages.filter { it.isFavorite }
            state.homeFilterMode == HomeFilterMode.RECENTS -> savedPages.sortedByDescending { it.updatedAtMillis }
            else -> savedPages.filter { it.folderId == state.currentFolderId }
        }
    }

    val canStepBack = state.currentFolderId != null ||
        state.searchQuery.isNotBlank() ||
        state.homeFilterMode != HomeFilterMode.EXPLORER ||
        state.isCustomizationModalOpen

    BackHandler(enabled = canStepBack) {
        when {
            state.isCustomizationModalOpen -> viewModel.setCustomizationModalOpen(false)
            state.searchQuery.isNotBlank() -> viewModel.updateSearchQuery("")
            state.homeFilterMode != HomeFilterMode.EXPLORER -> viewModel.selectHomeFilterMode(HomeFilterMode.EXPLORER)
            state.currentFolderId != null -> viewModel.navigateUpFolder()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("home_explorer_screen")
    ) {
        val screenW = this.maxWidth.value
        val screenH = this.maxHeight.value
        val metrics = viewModel.computeScreenFloatMetrics(screenW, screenH)
        val useSplitLayout = metrics.homeSidebarWidthFraction > 0f

        if (useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                HomeSidebarTreePane(
                    metrics = metrics,
                    allFolders = allFolders,
                    allPages = savedPages,
                    currentFolderId = state.currentFolderId,
                    filterMode = state.homeFilterMode,
                    activeThemeId = state.customization.themeId,
                    onSelectFilter = viewModel::selectHomeFilterMode,
                    onSelectFolder = viewModel::openFolder,
                    onSelectTheme = viewModel::selectStudioTheme,
                    onOpenNewFolderDialog = { viewModel.setNewFolderDialogOpen(true) },
                    onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
                    onOpenCustomizer = { viewModel.setCustomizationModalOpen(true) },
                    onResumeCanvas = viewModel::resumeActiveCanvas,
                    modifier = Modifier
                        .fillMaxWidth(metrics.homeSidebarWidthFraction)
                        .fillMaxHeight()
                )

                HomeExplorerMainPane(
                    metrics = metrics,
                    currentFolderId = state.currentFolderId,
                    currentFolderName = currentFolderName,
                    breadcrumbs = breadcrumbs,
                    filterMode = state.homeFilterMode,
                    searchQuery = state.searchQuery,
                    activeThemeId = state.customization.themeId,
                    subfolders = displayedFolders,
                    pages = displayedPages,
                    allFolders = allFolders,
                    allPages = savedPages,
                    activePageId = state.activePageId,
                    activePageTitle = state.activePageTitle,
                    isCompactPhone = false,
                    onSearchChanged = viewModel::updateSearchQuery,
                    onSelectFilter = viewModel::selectHomeFilterMode,
                    onSelectFolder = viewModel::openFolder,
                    onSelectTheme = viewModel::selectStudioTheme,
                    onNavigateUp = { viewModel.navigateUpFolder() },
                    onDeleteFolder = viewModel::deleteFolder,
                    onOpenNewFolderDialog = { viewModel.setNewFolderDialogOpen(true) },
                    onOpenNewSheetDialog = { viewModel.setNewSheetDialogOpen(true) },
                    onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
                    onOpenCustomizer = { viewModel.setCustomizationModalOpen(true) },
                    onOpenSheet = viewModel::openSheetInCanvas,
                    onToggleFavorite = viewModel::togglePageFavorite,
                    onDuplicateSheet = viewModel::duplicatePage,
                    onRequestMoveSheet = viewModel::setPageToMove,
                    onDeleteSheet = viewModel::deleteSketchPage,
                    onResumeCanvas = viewModel::resumeActiveCanvas,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            HomeExplorerMainPane(
                metrics = metrics,
                currentFolderId = state.currentFolderId,
                currentFolderName = currentFolderName,
                breadcrumbs = breadcrumbs,
                filterMode = state.homeFilterMode,
                searchQuery = state.searchQuery,
                activeThemeId = state.customization.themeId,
                subfolders = displayedFolders,
                pages = displayedPages,
                allFolders = allFolders,
                allPages = savedPages,
                activePageId = state.activePageId,
                activePageTitle = state.activePageTitle,
                isCompactPhone = true,
                onSearchChanged = viewModel::updateSearchQuery,
                onSelectFilter = viewModel::selectHomeFilterMode,
                onSelectFolder = viewModel::openFolder,
                onSelectTheme = viewModel::selectStudioTheme,
                onNavigateUp = { viewModel.navigateUpFolder() },
                onDeleteFolder = viewModel::deleteFolder,
                onOpenNewFolderDialog = { viewModel.setNewFolderDialogOpen(true) },
                onOpenNewSheetDialog = { viewModel.setNewSheetDialogOpen(true) },
                onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
                onOpenCustomizer = { viewModel.setCustomizationModalOpen(true) },
                onOpenSheet = viewModel::openSheetInCanvas,
                onToggleFavorite = viewModel::togglePageFavorite,
                onDuplicateSheet = viewModel::duplicatePage,
                onRequestMoveSheet = viewModel::setPageToMove,
                onDeleteSheet = viewModel::deleteSketchPage,
                onResumeCanvas = viewModel::resumeActiveCanvas,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }

        // Modals
        if (state.isNewFolderDialogOpen) {
            CreateSubfolderDialog(
                parentFolderName = currentFolderName,
                onCreateFolder = viewModel::createSubfolder,
                onDismiss = { viewModel.setNewFolderDialogOpen(false) }
            )
        }

        if (state.isNewSheetDialogOpen) {
            CreateSheetDialog(
                currentFolderName = currentFolderName,
                onCreateSheet = { title, category, template ->
                    viewModel.createNewSketchPage(
                        title = title,
                        category = category,
                        template = template,
                        targetFolderId = state.currentFolderId,
                        openImmediately = true
                    )
                },
                onDismiss = { viewModel.setNewSheetDialogOpen(false) }
            )
        }

        state.pageToMove?.let { pageToMove ->
            MoveSheetToFolderDialog(
                page = pageToMove,
                allFolders = allFolders,
                onMoveToFolder = { targetFolderId ->
                    viewModel.movePageToFolder(pageToMove.id, targetFolderId)
                },
                onDismiss = { viewModel.setPageToMove(null) }
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
    }
}

@Composable
private fun HomeSidebarTreePane(
    metrics: ScreenFloatMetrics,
    allFolders: List<FolderEntity>,
    allPages: List<SketchPageEntity>,
    currentFolderId: Long?,
    filterMode: HomeFilterMode,
    activeThemeId: StudioThemeId,
    onSelectFilter: (HomeFilterMode) -> Unit,
    onSelectFolder: (Long?) -> Unit,
    onSelectTheme: (StudioThemeId) -> Unit,
    onOpenNewFolderDialog: () -> Unit,
    onOpenScreenInspector: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onResumeCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val flattenedTree = remember(allFolders) {
        val list = ArrayList<Pair<FolderEntity, Int>>()
        val childrenMap = allFolders.groupBy { it.parentFolderId }
        fun traverse(parentId: Long?, depth: Int) {
            childrenMap[parentId]?.forEach { f ->
                list.add(f to depth)
                traverse(f.id, depth + 1)
            }
        }
        traverse(null, 0)
        list
    }

    Surface(
        color = palette.surface,
        modifier = modifier
            .border(1.dp, palette.border)
            .testTag("home_sidebar_tree_pane")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brand + Variable-Screen Float Badge + Theme Customizer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CanvasFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = String.format(
                            "✦ %s • %.2ff / %.2ff",
                            metrics.posture.badgeCode,
                            metrics.homeSidebarWidthFraction,
                            metrics.homeContentWidthFraction
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onOpenCustomizer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.badgeSurface)
                            .testTag("sidebar_open_customizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme & Pods Studio",
                            tint = palette.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenScreenInspector,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.badgeSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Screen Float Inspector",
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Primary Filter Views
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SidebarNavButton(
                    label = "Root Workspace",
                    badgeText = "${allPages.count { it.folderId == null }}",
                    icon = Icons.Default.Home,
                    selected = filterMode == HomeFilterMode.EXPLORER && currentFolderId == null,
                    accentColor = palette.primary,
                    onClick = { onSelectFolder(null) }
                )
                SidebarNavButton(
                    label = "Starred Favorites",
                    badgeText = "${allPages.count { it.isFavorite }}",
                    icon = Icons.Default.Star,
                    selected = filterMode == HomeFilterMode.FAVORITES,
                    accentColor = palette.secondary,
                    onClick = { onSelectFilter(HomeFilterMode.FAVORITES) }
                )
                SidebarNavButton(
                    label = "Recent Sheets",
                    badgeText = "${allPages.size}",
                    icon = Icons.Default.History,
                    selected = filterMode == HomeFilterMode.RECENTS,
                    accentColor = palette.accent,
                    onClick = { onSelectFilter(HomeFilterMode.RECENTS) }
                )
            }

            // Nested Folder Hierarchy Tree
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NESTED FOLDER TREE (${allFolders.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                IconButton(
                    onClick = onOpenNewFolderDialog,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Folder",
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                flattenedTree.forEach { (folder, depth) ->
                    val isSelected = filterMode == HomeFilterMode.EXPLORER && currentFolderId == folder.id
                    val folderColor = ColorMath.argbLongToColor(folder.colorArgb)
                    val sheetCount = allPages.count { it.folderId == folder.id }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (depth * 14).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.badgeSurface else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) folderColor else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFolder(folder.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = iconForFolderType(folder.iconName),
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${glyphForFolderType(folder.iconName)} ${folder.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (sheetCount > 0) {
                            Text(
                                text = "$sheetCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            // Resume Active Studio Canvas Button
            Button(
                onClick = onResumeCanvas,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sidebar_resume_canvas_button")
            ) {
                Icon(imageVector = Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Active Canvas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SidebarNavButton(
    label: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.badgeSurface else palette.surfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) accentColor else palette.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else palette.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accentColor else palette.textSecondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeExplorerMainPane(
    metrics: ScreenFloatMetrics,
    currentFolderId: Long?,
    currentFolderName: String,
    breadcrumbs: List<FolderEntity>,
    filterMode: HomeFilterMode,
    searchQuery: String,
    activeThemeId: StudioThemeId,
    subfolders: List<FolderEntity>,
    pages: List<SketchPageEntity>,
    allFolders: List<FolderEntity>,
    allPages: List<SketchPageEntity>,
    activePageId: Long,
    activePageTitle: String,
    isCompactPhone: Boolean,
    onSearchChanged: (String) -> Unit,
    onSelectFilter: (HomeFilterMode) -> Unit,
    onSelectFolder: (Long?) -> Unit,
    onSelectTheme: (StudioThemeId) -> Unit,
    onNavigateUp: () -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onOpenNewFolderDialog: () -> Unit,
    onOpenNewSheetDialog: () -> Unit,
    onOpenScreenInspector: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onOpenSheet: (SketchPageEntity) -> Unit,
    onToggleFavorite: (SketchPageEntity) -> Unit,
    onDuplicateSheet: (SketchPageEntity) -> Unit,
    onRequestMoveSheet: (SketchPageEntity) -> Unit,
    onDeleteSheet: (Long) -> Unit,
    onResumeCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val folderLookup = remember(allFolders) { allFolders.associateBy { it.id } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        // 1. Studio Editorial Banner & Active Canvas Quick-Resume Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompactPhone) 132.dp else 120.dp)
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, palette.border, MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_banner_1794045272359),
                    contentDescription = "CanvasFlow Studio Workspace",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF1F1D1B).copy(alpha = 0.62f))
                )
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CanvasFlow Atelier",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Surface(
                                color = Color(0xFF2B2825).copy(alpha = 0.90f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, Color(0xFFC5BEB0), RoundedCornerShape(8.dp))
                                    .clickable { onOpenScreenInspector() }
                                    .testTag("home_posture_float_badge")
                            ) {
                                Text(
                                    text = "⌖ ${metrics.posture.badgeCode} • ${String.format("%.0f%%", metrics.homeContentWidthFraction * 100f)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFEAE5DC),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✒ Active Sheet: $activePageTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE5D2B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allFolders.size} Folders • ${allPages.size} Sheets • ${activeThemeId.title} Theme",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD5CFC4)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onOpenCustomizer,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2B2825).copy(alpha = 0.88f))
                                .border(1.dp, Color(0xFFC5BEB0), RoundedCornerShape(14.dp))
                                .testTag("home_open_customizer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Customize Theme & Pods",
                                tint = Color(0xFFEAE5DC)
                            )
                        }

                        Button(
                            onClick = onResumeCanvas,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF6F4EE),
                                contentColor = Color(0xFF232625)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("resume_active_canvas_button")
                        ) {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Draw Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Quick Non-Neon Theme Switcher Strip
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "◈ THEME:",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                StudioThemeId.entries.forEach { theme ->
                    val isSelected = activeThemeId == theme
                    Surface(
                        color = if (isSelected) palette.primary else palette.surfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectTheme(theme) }
                            .testTag("quick_theme_chip_${theme.name.lowercase()}")
                    ) {
                        Text(
                            text = theme.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) palette.onPrimary else palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3. Search Bar & Action Buttons Row
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = {
                        Text(
                            text = "Search folders, sheets, or tags...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = palette.accent
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = palette.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.border,
                        focusedContainerColor = palette.surfaceElevated,
                        unfocusedContainerColor = palette.surfaceElevated
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_search_input")
                )

                Button(
                    onClick = onOpenNewFolderDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.surfaceElevated,
                        contentColor = palette.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .testTag("home_new_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = palette.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!isCompactPhone) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Folder", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = onOpenNewSheetDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("home_new_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = "New Sheet",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Sheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Mobile Filter Chips (when Sidebar is hidden on Compact Phone)
        if (isCompactPhone) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    HomeFilterMode.entries.forEach { mode ->
                        val selected = filterMode == mode
                        Surface(
                            color = if (selected) palette.primary else palette.surfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (selected) palette.primary else palette.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectFilter(mode) }
                                .testTag("filter_chip_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) palette.onPrimary else palette.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Interactive Breadcrumb Navigation Bar (Unlimited Nested Depth)
        if (filterMode == HomeFilterMode.EXPLORER && searchQuery.isBlank()) {
            item {
                Surface(
                    color = palette.surfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .testTag("breadcrumb_navigation_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (currentFolderId != null) {
                            IconButton(
                                onClick = onNavigateUp,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.badgeSurface)
                                    .testTag("breadcrumb_up_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate Up One Folder",
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Root Crumb
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentFolderId == null) palette.badgeSurface else Color.Transparent)
                                .clickable { onSelectFolder(null) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("breadcrumb_root")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Root Workspace",
                                tint = if (currentFolderId == null) palette.primary else palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Root Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (currentFolderId == null) palette.textPrimary else palette.textSecondary,
                                fontWeight = if (currentFolderId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        breadcrumbs.forEach { crumb ->
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            val isLast = crumb.id == currentFolderId
                            val crumbColor = ColorMath.argbLongToColor(crumb.colorArgb)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLast) palette.badgeSurface else Color.Transparent)
                                    .clickable { onSelectFolder(crumb.id) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(crumbColor)
                                )
                                Text(
                                    text = "${glyphForFolderType(crumb.iconName)} ${crumb.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isLast) palette.textPrimary else palette.textSecondary,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Animated Folder & Sheet Content Section
        item {
            AnimatedContent(
                targetState = Triple(currentFolderId, filterMode, searchQuery),
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 14 })
                        .togetherWith(
                            fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutVertically { -it / 18 }
                        )
                },
                label = "folder_and_sheet_explorer_transition"
            ) { (_, activeFilter, activeSearch) ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Subfolders Grid
                    if (subfolders.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (activeSearch.isNotBlank()) {
                                    "▣ MATCHING FOLDERS (${subfolders.size})"
                                } else {
                                    "▣ SUBFOLDERS IN ${currentFolderName.uppercase()} (${subfolders.size})"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondary
                            )
                            Text(
                                text = "Tap folder to open nested level",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }

                        val folderCardWidthFloat = when {
                            metrics.homeGridColumns >= 3 -> 0.315f
                            isCompactPhone -> 0.48f
                            else -> 0.485f
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            subfolders.forEach { folder ->
                                val childFoldersCount = allFolders.count { it.parentFolderId == folder.id }
                                val childSheetsCount = allPages.count { it.folderId == folder.id }
                                val accentColor = ColorMath.argbLongToColor(folder.colorArgb)

                                Surface(
                                    color = palette.surfaceElevated,
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth(folderCardWidthFloat)
                                        .border(1.dp, accentColor.copy(alpha = 0.48f), MaterialTheme.shapes.medium)
                                        .clickable { onSelectFolder(folder.id) }
                                        .testTag("folder_card_${folder.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentColor.copy(alpha = 0.14f))
                                                    .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(
                                                    imageVector = iconForFolderType(folder.iconName),
                                                    contentDescription = folder.name,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "${glyphForFolderType(folder.iconName)} ${folder.name}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "$childFoldersCount folders • $childSheetsCount sheets",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteFolder(folder.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Folder",
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sheets Section Header
                    val sectionHeader = when {
                        activeSearch.isNotBlank() -> "✒ MATCHING SHEETS (${pages.size})"
                        activeFilter == HomeFilterMode.FAVORITES -> "★ STARRED FAVORITE SHEETS (${pages.size})"
                        activeFilter == HomeFilterMode.RECENTS -> "◷ ALL RECENT SHEETS (${pages.size})"
                        else -> "✒ SHEETS IN ${currentFolderName.uppercase()} (${pages.size})"
                    }
                    Text(
                        text = sectionHeader,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accent
                    )

                    if (pages.isEmpty()) {
                        Surface(
                            color = palette.surfaceElevated,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.border, MaterialTheme.shapes.medium)
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No sheets in this view yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Create a new drawing or handwriting sheet, or add a nested subfolder to organize your studio.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = onOpenNewSheetDialog,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primary,
                                        contentColor = palette.onPrimary
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Sheet Here")
                                }
                            }
                        }
                    } else {
                        val sheetCardWidthFloat = when {
                            metrics.homeGridColumns >= 3 -> 0.315f
                            metrics.homeGridColumns == 2 -> 0.485f
                            else -> 1.0f
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pages.forEach { page ->
                                val isActive = page.id == activePageId
                                val folderName = page.folderId?.let { folderLookup[it]?.name } ?: "Root Workspace"
                                val template = runCatching {
                                    PaperTemplate.valueOf(page.paperTemplateName)
                                }.getOrDefault(PaperTemplate.DOT_MATRIX)

                                SheetExplorerCard(
                                    page = page,
                                    template = template,
                                    folderName = folderName,
                                    isActive = isActive,
                                    onOpen = { onOpenSheet(page) },
                                    onToggleFavorite = { onToggleFavorite(page) },
                                    onMove = { onRequestMoveSheet(page) },
                                    onDuplicate = { onDuplicateSheet(page) },
                                    onDelete = { onDeleteSheet(page.id) },
                                    modifier = Modifier.fillMaxWidth(sheetCardWidthFloat)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetExplorerCard(
    page: SketchPageEntity,
    template: PaperTemplate,
    folderName: String,
    isActive: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceElevated,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        modifier = modifier
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) palette.primary else palette.border,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onOpen)
            .testTag("sheet_card_${page.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Miniature Paper Ruling Preview Canvas
                Canvas(
                    modifier = Modifier
                        .size(width = 64.dp, height = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(template.backgroundColorHex))
                        .border(1.dp, Color(template.lineColorHex), RoundedCornerShape(10.dp))
                ) {
                    val lineCol = Color(template.lineColorHex)
                    for (y in 12..36 step 12) {
                        drawLine(
                            color = lineCol,
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1.2f
                        )
                    }
                    drawCircle(
                        color = palette.secondary.copy(alpha = 0.85f),
                        radius = 6.5f,
                        center = Offset(size.width * 0.65f, size.height * 0.55f)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActive) {
                            Surface(
                                color = palette.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${template.symbolGlyph} ${page.categoryTag} • ${template.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "▣ $folderName • ${page.strokeCount} strokes",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("favorite_sheet_${page.id}")
                ) {
                    Icon(
                        imageVector = if (page.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (page.isFavorite) palette.secondary else palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Sheet Organization Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                            .clickable(onClick = onMove)
                            .testTag("move_sheet_${page.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DriveFileMove,
                                contentDescription = "Move Sheet",
                                tint = palette.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Move",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                            .clickable(onClick = onDuplicate)
                            .testTag("duplicate_sheet_${page.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate Sheet",
                                tint = palette.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Duplicate",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Sheet",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
