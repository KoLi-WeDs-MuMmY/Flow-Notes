package com.example.data

import com.example.model.CanvasTextNote
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.PaperTemplate
import com.example.model.StrokePoint
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class SketchRepository(private val dao: SketchPageDao) {

    val allPages: Flow<List<SketchPageEntity>> = dao.observeAllPages()

    suspend fun getPageById(id: Long): SketchPageEntity? = dao.getPageById(id)

    suspend fun getPageCount(): Int = dao.getPageCount()

    suspend fun savePage(
        id: Long,
        title: String,
        categoryTag: String,
        paperTemplate: PaperTemplate,
        strokes: List<DrawnStroke>,
        textNotes: List<CanvasTextNote>,
        zoomFloat: Float
    ): Long {
        val entity = SketchPageEntity(
            id = id,
            title = title,
            categoryTag = categoryTag,
            paperTemplateName = paperTemplate.name,
            strokesPayload = serializeStrokes(strokes),
            textNotesPayload = serializeTextNotes(textNotes),
            strokeCount = strokes.size,
            lastZoomFloat = zoomFloat,
            updatedAtMillis = System.currentTimeMillis()
        )
        return dao.upsertPage(entity)
    }

    suspend fun deletePage(id: Long) {
        dao.deletePageById(id)
    }

    suspend fun seedStarterPagesIfEmpty(): Long {
        if (dao.getPageCount() > 0) return -1L

        // Starter Sheet 1: Architectural & Color Study (rich visual starter strokes + handwriting)
        val starterStrokes = buildStarterStudioArtwork()
        val starterNotes = listOf(
            CanvasTextNote(
                id = UUID.randomUUID().toString(),
                text = "CanvasFlow Studio • Pinch to Zoom (0.25x - 8.0x)",
                x = 120f,
                y = 95f,
                colorArgb = 0xFF18181BL,
                fontSizeSp = 19f
            ),
            CanvasTextNote(
                id = UUID.randomUUID().toString(),
                text = "Multi-Screen Float Coverage • Draw & Write Freely",
                x = 120f,
                y = 132f,
                colorArgb = 0xFFE85D04L,
                fontSizeSp = 15f
            )
        )
        val firstId = savePage(
            id = 0L,
            title = "Studio Welcome & Color Study",
            categoryTag = "Drawing & Notes",
            paperTemplate = PaperTemplate.DOT_MATRIX,
            strokes = starterStrokes,
            textNotes = starterNotes,
            zoomFloat = 1.0f
        )

        // Starter Sheet 2: Ruled Handwriting & Lecture Notes
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
                text = "1. Use Fountain Nib for pressure-sensitive calligraphy\n2. Pinch with two fingers to zoom up to 800% for micro-notes",
                x = 150f,
                y = 148f,
                colorArgb = 0xFF18181BL,
                fontSizeSp = 14f
            )
        )
        savePage(
            id = 0L,
            title = "Calligraphy & Ruled Notes",
            categoryTag = "Handwriting",
            paperTemplate = PaperTemplate.RULED_NOTEBOOK,
            strokes = ruledStrokes,
            textNotes = ruledNotes,
            zoomFloat = 1.0f
        )

        // Starter Sheet 3: Dark Blueprint Concept
        val blueprintStrokes = buildStarterBlueprintSheet()
        savePage(
            id = 0L,
            title = "Tablet UI Wireframe & Geometry",
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

        return firstId
    }

    companion object {
        // Format: id|tool|colorArgb|width|alpha|x1,y1,p1;x2,y2,p2...
        fun serializeStrokes(strokes: List<DrawnStroke>): String {
            if (strokes.isEmpty()) return ""
            return strokes.joinToString(separator = "\n") { s ->
                val pts = s.points.joinToString(separator = ";") { pt ->
                    "${pt.x},${pt.y},${pt.pressure}"
                }
                "${s.id}|${s.tool.name}|${s.colorArgb}|${s.strokeWidth}|${s.alpha}|$pts"
            }
        }

        fun deserializeStrokes(payload: String): List<DrawnStroke> {
            if (payload.isBlank()) return emptyList()
            return payload.lines().mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 6) return@mapNotNull null
                try {
                    val id = parts[0]
                    val tool = runCatching { DrawingTool.valueOf(parts[1]) }.getOrDefault(DrawingTool.FOUNTAIN_PEN)
                    val colorArgb = parts[2].toLong()
                    val strokeWidth = parts[3].toFloat()
                    val alpha = parts[4].toFloat()
                    val points = parts[5].split(";").mapNotNull { ptStr ->
                        val coords = ptStr.split(",")
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
            }
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

            // Expressive Vermilion Color Arc
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

            // Amber Gold Secondary Ribbon
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

            // Teal Studio Inner Wave
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

            // Architectural Perspective Frame Box
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

            // Handwritten Flourish Signature Wave at bottom of frame
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
            // Yellow Highlighter underline under title
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
            // Calligraphic Practice Loops
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
