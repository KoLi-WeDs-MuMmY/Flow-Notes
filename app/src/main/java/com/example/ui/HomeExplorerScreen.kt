package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.model.HomeFilterMode
import com.example.model.PaperTemplate
import com.example.model.ScreenFloatMetrics
import com.example.ui.components.CreateSheetDialog
import com.example.ui.components.CreateSubfolderDialog
import com.example.ui.components.MoveSheetToFolderDialog
import com.example.ui.components.ScreenCoverageInspectorDialog
import com.example.ui.components.iconForFolderType
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

@Composable
fun HomeExplorerScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val savedPages by viewModel.savedPages.collectAsStateWithLifecycle()

    val breadcrumbs = remember(state.currentFolderId, allFolders) {
        viewModel.buildBreadcrumbPath(state.currentFolderId, allFolders)
    }

    val currentFolderName = breadcrumbs.lastOrNull()?.name ?: "Root Workspace"

    // Filter subfolders and sheets based on searchQuery, homeFilterMode, and currentFolderId
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

    // Back handler: step up nested folder tree, clear search, or switch filter back to Explorer
    val canStepBack = state.currentFolderId != null ||
        state.searchQuery.isNotBlank() ||
        state.homeFilterMode != HomeFilterMode.EXPLORER

    BackHandler(enabled = canStepBack) {
        when {
            state.searchQuery.isNotBlank() -> viewModel.updateSearchQuery("")
            state.homeFilterMode != HomeFilterMode.EXPLORER -> viewModel.selectHomeFilterMode(HomeFilterMode.EXPLORER)
            state.currentFolderId != null -> viewModel.navigateUpFolder()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .testTag("home_explorer_screen")
    ) {
        val screenW = this.maxWidth.value
        val screenH = this.maxHeight.value
        val metrics = viewModel.computeScreenFloatMetrics(screenW, screenH)
        val useSplitLayout = metrics.homeSidebarWidthFraction > 0f

        if (useSplitLayout) {
            // Adaptive Tablet / Foldable / Landscape Split Layout using Explicit Float Percentages
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Left Pane: Nested Folder Tree & Filter Sidebar (Float Width Fraction: 0.26f - 0.32f)
                HomeSidebarTreePane(
                    metrics = metrics,
                    allFolders = allFolders,
                    allPages = savedPages,
                    currentFolderId = state.currentFolderId,
                    filterMode = state.homeFilterMode,
                    activePageTitle = state.activePageTitle,
                    onSelectFilter = viewModel::selectHomeFilterMode,
                    onSelectFolder = viewModel::openFolder,
                    onOpenNewFolderDialog = { viewModel.setNewFolderDialogOpen(true) },
                    onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
                    onResumeCanvas = viewModel::resumeActiveCanvas,
                    modifier = Modifier
                        .fillMaxWidth(metrics.homeSidebarWidthFraction)
                        .fillMaxHeight()
                )

                // Right Pane: Main Content Explorer (Float Width Fraction: 0.68f - 0.74f)
                HomeExplorerMainPane(
                    metrics = metrics,
                    currentFolderId = state.currentFolderId,
                    currentFolderName = currentFolderName,
                    breadcrumbs = breadcrumbs,
                    filterMode = state.homeFilterMode,
                    searchQuery = state.searchQuery,
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
                    onNavigateUp = { viewModel.navigateUpFolder() },
                    onDeleteFolder = viewModel::deleteFolder,
                    onOpenNewFolderDialog = { viewModel.setNewFolderDialogOpen(true) },
                    onOpenNewSheetDialog = { viewModel.setNewSheetDialogOpen(true) },
                    onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
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
            // Compact Mobile Portrait Breadcrumb & Grid Layout (1.0f Full Width Allocation)
            HomeExplorerMainPane(
                metrics = metrics,
                currentFolderId = state.currentFolderId,
                currentFolderName = currentFolderName,
                breadcrumbs = breadcrumbs,
                filterMode = state.homeFilterMode,
                searchQuery = state.searchQuery,
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
                onNavigateUp = { viewModel.navigateUpFolder() },
                onDeleteFolder = viewModel::deleteFolder,
                onOpenNewFolderDialog = { viewModel.setNewFolderDialogOpen(true) },
                onOpenNewSheetDialog = { viewModel.setNewSheetDialogOpen(true) },
                onOpenScreenInspector = { viewModel.setScreenInspectorOpen(true) },
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

        // Modals for Nested Folder Creation, Sheet Creation, Moving Sheets & Screen Float Inspector
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
                floatingXFraction = state.floatingPaletteXFraction,
                floatingYFraction = state.floatingPaletteYFraction,
                onSelectPreset = viewModel::selectCoveragePreset,
                onUpdateCustomFloat = viewModel::updateCustomCoverageFloat,
                onResetDockPosition = { viewModel.updateFloatingPaletteFraction(0.50f, 0.92f) },
                onDismiss = { viewModel.setScreenInspectorOpen(false) }
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
    activePageTitle: String,
    onSelectFilter: (HomeFilterMode) -> Unit,
    onSelectFolder: (Long?) -> Unit,
    onOpenNewFolderDialog: () -> Unit,
    onOpenScreenInspector: () -> Unit,
    onResumeCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        color = StudioCharcoal,
        modifier = modifier
            .border(1.dp, StudioBorder)
            .testTag("home_sidebar_tree_pane")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brand + Variable-Screen Float Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CanvasFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = String.format(
                            "%s • Split %.2ff / %.2ff",
                            metrics.posture.badgeCode,
                            metrics.homeSidebarWidthFraction,
                            metrics.homeContentWidthFraction
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealStudio
                    )
                }
                IconButton(
                    onClick = onOpenScreenInspector,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioObsidian)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Screen Float Inspector",
                        tint = TealStudio,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Primary Filter Views
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SidebarNavButton(
                    label = "Root Workspace",
                    badgeText = "${allPages.count { it.folderId == null }}",
                    icon = Icons.Default.Home,
                    selected = filterMode == HomeFilterMode.EXPLORER && currentFolderId == null,
                    accentColor = VermilionAccent,
                    onClick = { onSelectFolder(null) }
                )
                SidebarNavButton(
                    label = "Starred Favorites",
                    badgeText = "${allPages.count { it.isFavorite }}",
                    icon = Icons.Default.Star,
                    selected = filterMode == HomeFilterMode.FAVORITES,
                    accentColor = AmberGold,
                    onClick = { onSelectFilter(HomeFilterMode.FAVORITES) }
                )
                SidebarNavButton(
                    label = "Recent Sheets",
                    badgeText = "${allPages.size}",
                    icon = Icons.Default.History,
                    selected = filterMode == HomeFilterMode.RECENTS,
                    accentColor = TealStudio,
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
                    color = TextSecondaryDark
                )
                IconButton(
                    onClick = onOpenNewFolderDialog,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Folder",
                        tint = VermilionAccent,
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
                            .background(if (isSelected) StudioSlate else Color.Transparent)
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
                            text = folder.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else TextPrimaryDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (sheetCount > 0) {
                            Text(
                                text = "$sheetCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Resume Active Studio Canvas Card
            Button(
                onClick = onResumeCanvas,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VermilionAccent,
                    contentColor = Color.White
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) StudioSlate else StudioObsidian)
            .border(
                width = 1.dp,
                color = if (selected) accentColor else StudioBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else TextSecondaryDark,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimaryDark,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accentColor else TextSecondaryDark
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
    onNavigateUp: () -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onOpenNewFolderDialog: () -> Unit,
    onOpenNewSheetDialog: () -> Unit,
    onOpenScreenInspector: () -> Unit,
    onOpenSheet: (SketchPageEntity) -> Unit,
    onToggleFavorite: (SketchPageEntity) -> Unit,
    onDuplicateSheet: (SketchPageEntity) -> Unit,
    onRequestMoveSheet: (SketchPageEntity) -> Unit,
    onDeleteSheet: (Long) -> Unit,
    onResumeCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folderLookup = remember(allFolders) { allFolders.associateBy { it.id } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        // 1. Studio Hero Banner & Active Canvas Quick-Resume Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompactPhone) 128.dp else 118.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
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
                        .background(StudioObsidian.copy(alpha = 0.68f))
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
                                text = "CanvasFlow Studio",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Surface(
                                color = StudioCharcoal.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, TealStudio, RoundedCornerShape(8.dp))
                                    .clickable { onOpenScreenInspector() }
                                    .testTag("home_posture_float_badge")
                            ) {
                                Text(
                                    text = "${metrics.posture.badgeCode} • ${String.format("%.0f%%", metrics.homeContentWidthFraction * 100f)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealStudio,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Active Sheet: $activePageTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allFolders.size} Nested Folders • ${allPages.size} Sheets • 60fps Path-Cached Canvas",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onResumeCanvas,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermilionAccent,
                            contentColor = Color.White
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

        // 2. Search Bar & Action Buttons Row
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
                            color = TextSecondaryDark
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TealStudio
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = TextSecondaryDark
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealStudio,
                        unfocusedBorderColor = StudioBorder,
                        focusedContainerColor = StudioCharcoal,
                        unfocusedContainerColor = StudioCharcoal
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_search_input")
                )

                Button(
                    onClick = onOpenNewFolderDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCharcoal,
                        contentColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                        .testTag("home_new_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = AmberGold,
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
                        containerColor = TealStudio,
                        contentColor = Color.White
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

        // 3. Mobile Filter Chips (when Sidebar is hidden on Compact Phone)
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
                            color = if (selected) VermilionAccent else StudioCharcoal,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (selected) VermilionAccent else StudioBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectFilter(mode) }
                                .testTag("filter_chip_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Interactive Breadcrumb Navigation Bar (Unlimited Nested Depth)
        if (filterMode == HomeFilterMode.EXPLORER && searchQuery.isBlank()) {
            item {
                Surface(
                    color = StudioCharcoal,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
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
                                    .background(StudioObsidian)
                                    .testTag("breadcrumb_up_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate Up One Folder",
                                    tint = VermilionAccent,
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
                                .background(if (currentFolderId == null) StudioSlate else Color.Transparent)
                                .clickable { onSelectFolder(null) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("breadcrumb_root")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Root Workspace",
                                tint = if (currentFolderId == null) VermilionAccent else TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Root Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (currentFolderId == null) Color.White else TextSecondaryDark,
                                fontWeight = if (currentFolderId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Nested Ancestor & Current Folder Crumbs
                        breadcrumbs.forEach { crumb ->
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            val isLast = crumb.id == currentFolderId
                            val crumbColor = ColorMath.argbLongToColor(crumb.colorArgb)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLast) StudioSlate else Color.Transparent)
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
                                    text = crumb.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isLast) Color.White else TextSecondaryDark,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Subfolders Grid Section
        if (subfolders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "MATCHING FOLDERS (${subfolders.size})"
                        } else {
                            "SUBFOLDERS IN ${currentFolderName.uppercase()} (${subfolders.size})"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGold
                    )
                    Text(
                        text = "Tap folder to open nested level",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }

            item {
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
                            color = StudioCharcoal,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth(folderCardWidthFloat)
                                .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
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
                                            .background(accentColor.copy(alpha = 0.18f))
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
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$childFoldersCount folders • $childSheetsCount sheets",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark
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
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Sheets Grid Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sectionHeader = when {
                    searchQuery.isNotBlank() -> "MATCHING SHEETS (${pages.size})"
                    filterMode == HomeFilterMode.FAVORITES -> "STARRED FAVORITE SHEETS (${pages.size})"
                    filterMode == HomeFilterMode.RECENTS -> "ALL RECENT SHEETS (${pages.size})"
                    else -> "SHEETS IN ${currentFolderName.uppercase()} (${pages.size})"
                }
                Text(
                    text = sectionHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = TealStudio
                )
            }
        }

        if (pages.isEmpty()) {
            item {
                Surface(
                    color = StudioCharcoal,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No sheets in this view yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Create a new drawing or handwriting sheet, or add a nested subfolder to organize your studio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onOpenNewSheetDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = VermilionAccent)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Sheet Here")
                        }
                    }
                }
            }
        } else {
            item {
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
    Surface(
        color = StudioCharcoal,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) VermilionAccent else StudioBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onOpen)
            .testTag("sheet_card_${page.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Preview Swatch & Favorite Star
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
                        color = VermilionAccent.copy(alpha = 0.85f),
                        radius = 7f,
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
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActive) {
                            Surface(
                                color = VermilionAccent,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${page.categoryTag} • ${template.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        maxLines = 1
                    )
                    Text(
                        text = "Folder: $folderName • ${page.strokeCount} strokes",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealStudio,
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
                        tint = if (page.isFavorite) AmberGold else TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Sheet Organization Actions (Move across folders, Duplicate, Open, Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = StudioObsidian,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
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
                                tint = AmberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Move",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Surface(
                        color = StudioObsidian,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
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
                                tint = TealStudio,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Duplicate",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimaryDark
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
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
