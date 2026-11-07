package com.example.data

import com.example.model.CanvasTextNote
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.FolderIconType
import com.example.model.PaperTemplate
import com.example.model.StrokePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class SketchRepository(
    private val pageDao: SketchPageDao,
    private val folderDao: FolderDao
) {

    val allPages: Flow<List<SketchPageEntity>> = pageDao.observeAllPages()
    val allFolders: Flow<List<FolderEntity>> = folderDao.observeAllFolders()

    suspend fun getPageById(id: Long): SketchPageEntity? = withContext(Dispatchers.IO) {
        pageDao.getPageById(id)
    }

    suspend fun getPageCount(): Int = withContext(Dispatchers.IO) {
        pageDao.getPageCount()
    }

    suspend fun createFolder(
        name: String,
        parentFolderId: Long?,
        colorArgb: Long,
        iconType: FolderIconType
    ): Long = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifBlank { "New Studio Folder" }
        folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = parentFolderId,
                name = cleanName,
                colorArgb = colorArgb,
                iconName = iconType.name,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateFolder(
        folder: FolderEntity,
        newName: String,
        newColorArgb: Long,
        newIconType: FolderIconType
    ) = withContext(Dispatchers.IO) {
        folderDao.upsertFolder(
            folder.copy(
                name = newName.trim().ifBlank { folder.name },
                colorArgb = newColorArgb,
                iconName = newIconType.name,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    /**
     * Deletes a folder and safely re-parents its child subfolders and sheets
     * to the deleted folder's parent (or Root if null) so no user work is lost.
     */
    suspend fun deleteFolderSafely(folderId: Long) = withContext(Dispatchers.IO) {
        val folder = folderDao.getFolderById(folderId) ?: return@withContext
        val parentId = folder.parentFolderId
        folderDao.reparentChildFolders(oldParentId = folderId, newParentId = parentId)
        pageDao.movePagesFromFolder(oldFolderId = folderId, newFolderId = parentId)
        folderDao.deleteFolderById(folderId)
    }

    suspend fun savePage(
        id: Long,
        folderId: Long?,
        isFavorite: Boolean,
        title: String,
        categoryTag: String,
        paperTemplate: PaperTemplate,
        strokes: List<DrawnStroke>,
        textNotes: List<CanvasTextNote>,
        zoomFloat: Float
    ): Long = withContext(Dispatchers.Default) {
        val serializedStrokes = serializeStrokes(strokes)
        val serializedNotes = serializeTextNotes(textNotes)
        val entity = SketchPageEntity(
            id = id,
            folderId = folderId,
            isFavorite = isFavorite,
            title = title,
            categoryTag = categoryTag,
            paperTemplateName = paperTemplate.name,
            strokesPayload = serializedStrokes,
            textNotesPayload = serializedNotes,
            strokeCount = strokes.size,
            lastZoomFloat = zoomFloat,
            updatedAtMillis = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            pageDao.upsertPage(entity)
        }
    }

    suspend fun movePageToFolder(pageId: Long, targetFolderId: Long?) = withContext(Dispatchers.IO) {
        pageDao.movePageToFolder(pageId, targetFolderId)
    }

    suspend fun togglePageFavorite(page: SketchPageEntity) = withContext(Dispatchers.IO) {
        pageDao.setPageFavorite(page.id, !page.isFavorite)
    }

    suspend fun duplicatePage(page: SketchPageEntity): Long = withContext(Dispatchers.IO) {
        val copyEntity = page.copy(
            id = 0L,
            title = "${page.title} (Copy)",
            updatedAtMillis = System.currentTimeMillis()
        )
        pageDao.upsertPage(copyEntity)
    }

    suspend fun deletePage(id: Long) = withContext(Dispatchers.IO) {
        pageDao.deletePageById(id)
    }

    suspend fun seedStarterWorkspaceIfEmpty(): Long = withContext(Dispatchers.IO) {
        if (pageDao.getPageCount() > 0) return@withContext -1L

        // 1. Create Top-Level Folders
        val designFolderId = folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = null,
                name = "Architecture & Design",
                colorArgb = 0xFFE85D04L,
                iconName = FolderIconType.ARCHITECTURE.name
            )
        )
        val notesFolderId = folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = null,
                name = "Handwritten Study Notes",
                colorArgb = 0xFF2A9D8FL,
                iconName = FolderIconType.NOTES.name
            )
        )
        val artFolderId = folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = null,
                name = "Color & Brush Studies",
                colorArgb = 0xFFF4A261L,
                iconName = FolderIconType.PALETTE.name
            )
        )

        // 2. Create Nested Subfolders (Level 2 & Level 3) to showcase unlimited nesting
        val blueprintSubfolderId = folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = designFolderId,
                name = "Tablet UI Wireframes",
                colorArgb = 0xFF3B82F6L,
                iconName = FolderIconType.GEOMETRY.name
            )
        )
        val floorPlanNestedId = folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = designFolderId,
                name = "Studio Floor Plans",
                colorArgb = 0xFF8B5CF6L,
                iconName = FolderIconType.ARCHITECTURE.name
            )
        )
        val calligraphySubfolderId = folderDao.upsertFolder(
            FolderEntity(
                parentFolderId = notesFolderId,
                name = "Fountain Nib Drills",
                colorArgb = 0xFF10B981L,
                iconName = FolderIconType.BOOKMARK.name
            )
        )

        // 3. Starter Sheet 1 (Root Workspace, Starred): Studio Welcome & Color Study
        val starterStrokes = buildStarterStudioArtwork()
        val starterNotes = listOf(
            CanvasTextNote(
                id = UUID.randomUUID().toString(),
                text = "CanvasFlow 2.0 • 60fps Path-Cached Engine",
                x = 120f,
                y = 95f,
                colorArgb = 0xFF18181BL,
                fontSizeSp = 19f
            ),
            CanvasTextNote(
                id = UUID.randomUUID().toString(),
                text = "Multi-Screen Float Coverage • Pinch to Zoom (0.25x - 8.0x)",
                x = 120f,
                y = 132f,
                colorArgb = 0xFFE85D04L,
                fontSizeSp = 15f
            )
        )
        val firstId = savePage(
            id = 0L,
            folderId = null, // Root Workspace for immediate visibility + also accessible in Recents/Favorites
            isFavorite = true,
            title = "Studio Welcome & Color Study",
            categoryTag = "Drawing & Notes",
            paperTemplate = PaperTemplate.DOT_MATRIX,
            strokes = starterStrokes,
            textNotes = starterNotes,
            zoomFloat = 1.0f
        )

        // 4. Starter Sheet 2 (Inside Handwritten Study Notes): Calligraphy & Ruled Notes
        val ruledStrokes = buildStarterHandwritingSheet()
        val ruledNotes = listOf(
            CanvasTextNote(
                id = UUID.randomUUID().toString(),
                text = "Handwriting & Fountain Nib Practice",
                x = 150f,
                y = 104f,
                colorArgb = 0xFF1D4ED8L,
                fontSizeSp = 20f
            ),
            CanvasTextNote(
                id = UUID.randomUUID().toString(),
                text = "1. Fountain Nib responds smoothly with zero frame lag\n2. Organize sheets into unlimited nested folders",
                x = 150f,
                y = 148f,
                colorArgb = 0xFF18181BL,
                fontSizeSp = 14f
            )
        )
        savePage(
            id = 0L,
            folderId = notesFolderId,
            isFavorite = true,
            title = "Calligraphy & Ruled Notes",
            categoryTag = "Handwriting",
            paperTemplate = PaperTemplate.RULED_NOTEBOOK,
            strokes = ruledStrokes,
            textNotes = ruledNotes,
            zoomFloat = 1.0f
        )

        // 5. Starter Sheet 3 (Inside Architecture & Design -> Tablet UI Wireframes)
        val blueprintStrokes = buildStarterBlueprintSheet()
        savePage(
            id = 0L,
            folderId = blueprintSubfolderId,
            isFavorite = false,
            title = "Adaptive Multi-Screen Spec",
            categoryTag = "Blueprint",
            paperTemplate = PaperTemplate.DARK_SLATE,
            strokes = blueprintStrokes,
            textNotes = listOf(
                CanvasTextNote(
                    id = UUID.randomUUID().toString(),
                    text = "FLOAT COVERAGE SPEC: 0.92f CANVAS / 0.08f HUD",
                    x = 130f,
                    y = 110f,
                    colorArgb = 0xFFF4A261L,
                    fontSizeSp = 16f
                )
            ),
            zoomFloat = 1.0f
        )

        // 6. Starter Sheet 4 (Inside Architecture & Design)
        savePage(
            id = 0L,
            folderId = designFolderId,
            isFavorite = false,
            title = "Isometric Pavilion Sketch",
            categoryTag = "Architecture",
            paperTemplate = PaperTemplate.ISOMETRIC_DRAFT,
            strokes = blueprintStrokes,
            textNotes = listOf(
                CanvasTextNote(
                    id = UUID.randomUUID().toString(),
                    text = "30° Axonometric Structural Grid",
                    x = 140f,
                    y = 120f,
                    colorArgb = 0xFF18181BL,
                    fontSizeSp = 17f
                )
            ),
            zoomFloat = 1.0f
        )

        // 7. Starter Sheet 5 (Inside Color & Brush Studies)
        savePage(
            id = 0L,
            folderId = artFolderId,
            isFavorite = false,
            title = "Vermilion & Teal Pigment Harmonies",
            categoryTag = "Color Study",
            paperTemplate = PaperTemplate.WARM_ARCHIVAL,
            strokes = starterStrokes,
            textNotes = emptyList(),
            zoomFloat = 1.0f
        )

        // Suppress unused warning for nested empty demo folders
        if (floorPlanNestedId < 0 || calligraphySubfolderId < 0) Unit

        firstId
    }

    companion object {
        // Format: id|tool|colorArgb|width|alpha|x1,y1,p1;x2,y2,p2...
        fun serializeStrokes(strokes: List<DrawnStroke>): String {
            if (strokes.isEmpty()) return ""
            val sb = StringBuilder(strokes.size * 64)
            for (i in strokes.indices) {
                if (i > 0) sb.append('\n')
                val s = strokes[i]
                sb.append(s.id).append('|')
                    .append(s.tool.name).append('|')
                    .append(s.colorArgb).append('|')
                    .append(s.strokeWidth).append('|')
                    .append(s.alpha).append('|')
                val pts = s.points
                for (j in pts.indices) {
                    if (j > 0) sb.append(';')
                    val pt = pts[j]
                    sb.append(pt.x).append(',').append(pt.y).append(',').append(pt.pressure)
                }
            }
            return sb.toString()
        }

        fun deserializeStrokes(payload: String): List<DrawnStroke> {
            if (payload.isBlank()) return emptyList()
            return payload.lineSequence().mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size < 6) return@mapNotNull null
                try {
                    val id = parts[0]
                    val tool = runCatching { DrawingTool.valueOf(parts[1]) }.getOrDefault(DrawingTool.FOUNTAIN_PEN)
                    val colorArgb = parts[2].toLong()
                    val strokeWidth = parts[3].toFloat()
                    val alpha = parts[4].toFloat()
                    val points = parts[5].split(';').mapNotNull { ptStr ->
                        val coords = ptStr.split(',')
                        if (coords.size >= 2) {
                            StrokePoint(
                                x = coords[0].toFloat(),
                                y = coords[1].toFloat(),
                                pressure = coords.getOrNull(2)?.toFloatOrNull() ?: 1f
                            )
                        } else null
                    }
                    if (points.isEmpty()) null else DrawnStroke(
                        id = id,
                        points = points,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth,
                        alpha = alpha,
                        tool = tool
                    )
                } catch (_: Exception) {
                    null
                }
            }.toList()
        }

        // Format: id|x|y|colorArgb|fontSizeSp|escapedText
        fun serializeTextNotes(notes: List<CanvasTextNote>): String {
            if (notes.isEmpty()) return ""
            return notes.joinToString(separator = "\n") { note ->
                val safeText = note.text.replace("\n", "\\n").replace("|", " ")
                "${note.id}|${note.x}|${note.y}|${note.colorArgb}|${note.fontSizeSp}|$safeText"
            }
        }

        fun deserializeTextNotes(payload: String): List<CanvasTextNote> {
            if (payload.isBlank()) return emptyList()
            return payload.lines().mapNotNull { line ->
                val parts = line.split("|", limit = 6)
                if (parts.size < 6) return@mapNotNull null
                try {
                    CanvasTextNote(
                        id = parts[0],
                        x = parts[1].toFloat(),
                        y = parts[2].toFloat(),
                        colorArgb = parts[3].toLong(),
                        fontSizeSp = parts[4].toFloat(),
                        text = parts[5].replace("\\n", "\n")
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }

        private fun buildStarterStudioArtwork(): List<DrawnStroke> {
            val strokes = mutableListOf<DrawnStroke>()

            val arc1 = (0..36).map { i ->
                val angle = Math.toRadians(195.0 + i * 4.2)
                StrokePoint(
                    x = 360f + (210f * cos(angle)).toFloat(),
                    y = 430f + (180f * sin(angle)).toFloat(),
                    pressure = 0.8f + 0.3f * sin(i / 5.0).toFloat()
                )
            }
            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = arc1,
                    colorArgb = 0xFFE85D04L,
                    strokeWidth = 16f,
                    alpha = 0.85f,
                    tool = DrawingTool.ART_BRUSH
                )
            )

            val arc2 = (0..36).map { i ->
                val angle = Math.toRadians(198.0 + i * 4.0)
                StrokePoint(
                    x = 360f + (175f * cos(angle)).toFloat(),
                    y = 430f + (148f * sin(angle)).toFloat(),
                    pressure = 1.0f
                )
            }
            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = arc2,
                    colorArgb = 0xFFF4A261L,
                    strokeWidth = 14f,
                    alpha = 0.88f,
                    tool = DrawingTool.CHISEL_MARKER
                )
            )

            val arc3 = (0..36).map { i ->
                val angle = Math.toRadians(202.0 + i * 3.8)
                StrokePoint(
                    x = 360f + (140f * cos(angle)).toFloat(),
                    y = 430f + (116f * sin(angle)).toFloat(),
                    pressure = 1.0f
                )
            }
            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = arc3,
                    colorArgb = 0xFF2A9D8FL,
                    strokeWidth = 11f,
                    alpha = 0.92f,
                    tool = DrawingTool.FOUNTAIN_PEN
                )
            )

            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = listOf(
                        StrokePoint(130f, 220f),
                        StrokePoint(590f, 520f)
                    ),
                    colorArgb = 0xFF18181BL,
                    strokeWidth = 3.2f,
                    alpha = 0.75f,
                    tool = DrawingTool.RECTANGLE_SHAPE
                )
            )

            val flourish = (0..45).map { i ->
                val t = i / 45f
                StrokePoint(
                    x = 160f + t * 390f,
                    y = 475f + sin(t * 18.0).toFloat() * (18f * (1f - t * 0.5f)),
                    pressure = 0.6f + 0.6f * sin(t * 9.0).toFloat()
                )
            }
            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = flourish,
                    colorArgb = 0xFF1D4ED8L,
                    strokeWidth = 5.5f,
                    alpha = 1.0f,
                    tool = DrawingTool.FOUNTAIN_PEN
                )
            )

            return strokes
        }

        private fun buildStarterHandwritingSheet(): List<DrawnStroke> {
            val strokes = mutableListOf<DrawnStroke>()
            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = listOf(
                        StrokePoint(148f, 128f),
                        StrokePoint(530f, 128f)
                    ),
                    colorArgb = 0xFFF4A261L,
                    strokeWidth = 20f,
                    alpha = 0.40f,
                    tool = DrawingTool.CHISEL_MARKER
                )
            )
            val loops = (0..60).map { i ->
                val t = i / 60f
                val angle = i * 0.48
                StrokePoint(
                    x = 160f + t * 420f + (cos(angle) * 14f).toFloat(),
                    y = 290f + (sin(angle) * 26f).toFloat(),
                    pressure = 0.7f + 0.4f * sin(angle).toFloat()
                )
            }
            strokes.add(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = loops,
                    colorArgb = 0xFF1D4ED8L,
                    strokeWidth = 5.0f,
                    alpha = 1.0f,
                    tool = DrawingTool.FOUNTAIN_PEN
                )
            )
            return strokes
        }

        private fun buildStarterBlueprintSheet(): List<DrawnStroke> {
            return listOf(
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = listOf(StrokePoint(130f, 180f), StrokePoint(620f, 480f)),
                    colorArgb = 0xFF2A9D8FL,
                    strokeWidth = 3.5f,
                    alpha = 0.9f,
                    tool = DrawingTool.RECTANGLE_SHAPE
                ),
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = listOf(StrokePoint(260f, 240f), StrokePoint(490f, 420f)),
                    colorArgb = 0xFFE85D04L,
                    strokeWidth = 4.0f,
                    alpha = 0.95f,
                    tool = DrawingTool.CIRCLE_SHAPE
                ),
                DrawnStroke(
                    id = UUID.randomUUID().toString(),
                    points = listOf(StrokePoint(130f, 180f), StrokePoint(620f, 480f)),
                    colorArgb = 0xFFF4A261L,
                    strokeWidth = 2.5f,
                    alpha = 0.75f,
                    tool = DrawingTool.RULER_LINE
                )
            )
        }
    }
}
