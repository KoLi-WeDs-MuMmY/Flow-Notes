package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasTextNote
import com.example.model.DrawingTool
import com.example.model.DrawnStroke
import com.example.model.PaperTemplate
import com.example.model.StrokePoint
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.VermilionAccent
import com.example.util.ColorMath
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun InteractiveDrawingCanvas(
    strokes: List<DrawnStroke>,
    textNotes: List<CanvasTextNote>,
    selectedTool: DrawingTool,
    activeColor: Color,
    strokeWidth: Float,
    strokeAlpha: Float,
    paperTemplate: PaperTemplate,
    zoomFloat: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    isPanNavigationMode: Boolean,
    showMinimap: Boolean,
    onCommitStroke: (List<StrokePoint>) -> Unit,
    onPinchZoomAndPan: (centroid: Offset, panDelta: Offset, zoomChange: Float, viewportW: Float, viewportH: Float) -> Unit,
    onRemoveTextNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePoints = remember { mutableStateListOf<StrokePoint>() }
    var lastTouchCanvasPoint by remember { mutableStateOf<Offset?>(null) }
    var canvasSizePx by remember { mutableStateOf(IntSize(1080, 1920)) }

    val currentZoom by rememberUpdatedState(zoomFloat)
    val currentPanX by rememberUpdatedState(panOffsetX)
    val currentPanY by rememberUpdatedState(panOffsetY)
    val currentPanMode by rememberUpdatedState(isPanNavigationMode)
    val currentTool by rememberUpdatedState(selectedTool)
    val currentCommitCallback by rememberUpdatedState(onCommitStroke)
    val currentPinchCallback by rememberUpdatedState(onPinchZoomAndPan)

    val backgroundColor = Color(paperTemplate.backgroundColorHex)
    val gridColor = Color(paperTemplate.lineColorHex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onSizeChanged { canvasSizePx = it }
            .testTag("interactive_drawing_canvas")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    var wasMultiTouch = false
                    activePoints.clear()

                    fun screenToCanvas(screenOffset: Offset): StrokePoint {
                        val cx = (screenOffset.x - currentPanX) / currentZoom
                        val cy = (screenOffset.y - currentPanY) / currentZoom
                        return StrokePoint(cx, cy, pressure = 1.0f)
                    }

                    if (!currentPanMode) {
                        val startPt = screenToCanvas(firstDown.position)
                        activePoints.add(startPt)
                        lastTouchCanvasPoint = Offset(startPt.x, startPt.y)
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.size >= 2 || currentPanMode) {
                            if (pressed.size >= 2 && !wasMultiTouch) {
                                // Discard accidental single-finger start dot when 2nd finger lands for pinch-to-zoom
                                wasMultiTouch = true
                                activePoints.clear()
                                lastTouchCanvasPoint = null
                            }
                            val zoomChange = if (pressed.size >= 2) event.calculateZoom() else 1f
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val safeCentroid = if (centroid == Offset.Unspecified) {
                                Offset(size.width / 2f, size.height / 2f)
                            } else {
                                centroid
                            }

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                currentPinchCallback(
                                    safeCentroid,
                                    panChange,
                                    zoomChange,
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                            }
                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        } else if (pressed.size == 1 && !wasMultiTouch) {
                            val change = pressed.first()
                            val pt = screenToCanvas(change.position)
                            lastTouchCanvasPoint = Offset(pt.x, pt.y)

                            if (currentTool.isShape) {
                                if (activePoints.isEmpty()) {
                                    activePoints.add(pt)
                                } else if (activePoints.size == 1) {
                                    activePoints.add(pt)
                                } else {
                                    activePoints[1] = pt
                                }
                            } else {
                                val prev = activePoints.lastOrNull()
                                if (prev == null || hypot(pt.x - prev.x, pt.y - prev.y) >= (1.2f / currentZoom)) {
                                    // Velocity-based synthetic pressure for Fountain Nib calligraphy feel
                                    val dist = if (prev != null) hypot(pt.x - prev.x, pt.y - prev.y) else 0f
                                    val pressure = (1.25f - (dist * 0.015f)).coerceIn(0.45f, 1.35f)
                                    activePoints.add(pt.copy(pressure = pressure))
                                }
                            }
                            if (change.positionChanged()) {
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!wasMultiTouch && !currentPanMode && activePoints.isNotEmpty()) {
                        val finalPoints = if (activePoints.size == 1) {
                            // Single tap dot
                            val p = activePoints.first()
                            listOf(p, p.copy(x = p.x + 0.5f, y = p.y + 0.5f))
                        } else {
                            activePoints.toList()
                        }
                        currentCommitCallback(finalPoints)
                    }
                    activePoints.clear()
                    lastTouchCanvasPoint = null
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({
                translate(left = panOffsetX, top = panOffsetY)
                scale(scaleX = zoomFloat, scaleY = zoomFloat, pivot = Offset.Zero)
            }) {
                // 1. Draw Paper Template Grid / Ruling in Canvas Space
                drawPaperTemplateGrid(
                    template = paperTemplate,
                    gridColor = gridColor,
                    zoomFloat = zoomFloat,
                    panOffsetX = panOffsetX,
                    panOffsetY = panOffsetY
                )

                // 2. Draw Committed Strokes
                strokes.forEach { stroke ->
                    drawStudioStroke(
                        points = stroke.points,
                        color = ColorMath.argbLongToColor(stroke.colorArgb),
                        strokeWidth = stroke.strokeWidth,
                        alpha = stroke.alpha,
                        tool = stroke.tool,
                        paperBgColor = backgroundColor
                    )
                }

                // 3. Draw Active Live Stroke
                if (activePoints.isNotEmpty()) {
                    val liveColor = if (selectedTool == DrawingTool.ERASER) {
                        backgroundColor
                    } else {
                        activeColor
                    }
                    drawStudioStroke(
                        points = activePoints,
                        color = liveColor,
                        strokeWidth = strokeWidth,
                        alpha = if (selectedTool == DrawingTool.ERASER) 1f else strokeAlpha,
                        tool = selectedTool,
                        paperBgColor = backgroundColor
                    )
                }

                // 4. Draw Eraser Ring Indicator if erasing
                if (selectedTool == DrawingTool.ERASER && lastTouchCanvasPoint != null) {
                    drawCircle(
                        color = VermilionAccent.copy(alpha = 0.85f),
                        radius = strokeWidth / 2f,
                        center = lastTouchCanvasPoint!!,
                        style = Stroke(width = 1.8f / zoomFloat)
                    )
                }
            }
        }

        // 5. Render Movable / Deletable Typography & Handwriting Callout Notes in Transformed Coordinates
        textNotes.forEach { note ->
            val screenX = (note.x * zoomFloat + panOffsetX).roundToInt()
            val screenY = (note.y * zoomFloat + panOffsetY).roundToInt()
            val scaledFontSp = (note.fontSizeSp * zoomFloat.coerceIn(0.55f, 2.4f)).sp
            val noteColor = ColorMath.argbLongToColor(note.colorArgb)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset { IntOffset(screenX, screenY) }
                    .background(
                        color = backgroundColor.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = noteColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = note.text,
                    color = noteColor,
                    fontSize = scaledFontSp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(StudioCharcoal.copy(alpha = 0.15f))
                        .clickable { onRemoveTextNote(note.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove text note",
                        tint = noteColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // 6. Floating Minimap / Viewport Radar Overlay (Bottom-Left or Top-Right safe spot)
        if (showMinimap) {
            CanvasMinimapOverlay(
                strokes = strokes,
                zoomFloat = zoomFloat,
                panOffsetX = panOffsetX,
                panOffsetY = panOffsetY,
                viewportWidthPx = canvasSizePx.width.toFloat().coerceAtLeast(1f),
                viewportHeightPx = canvasSizePx.height.toFloat().coerceAtLeast(1f),
                paperTemplate = paperTemplate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 74.dp, end = 14.dp)
            )
        }
    }
}

private fun DrawScope.drawPaperTemplateGrid(
    template: PaperTemplate,
    gridColor: Color,
    zoomFloat: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    // Visible region in canvas coordinates
    val startX = (-panOffsetX / zoomFloat) - 200f
    val startY = (-panOffsetY / zoomFloat) - 200f
    val endX = ((size.width - panOffsetX) / zoomFloat) + 200f
    val endY = ((size.height - panOffsetY) / zoomFloat) + 200f

    when (template) {
        PaperTemplate.WARM_ARCHIVAL, PaperTemplate.DARK_SLATE -> {
            // Subtle architectural crosshairs at 160px intervals
            val step = 160f
            val firstX = (startX / step).toInt() * step
            val firstY = (startY / step).toInt() * step
            var x = firstX
            while (x <= endX) {
                var y = firstY
                while (y <= endY) {
                    drawLine(
                        color = gridColor.copy(alpha = 0.55f),
                        start = Offset(x - 5f, y),
                        end = Offset(x + 5f, y),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = gridColor.copy(alpha = 0.55f),
                        start = Offset(x, y - 5f),
                        end = Offset(x, y + 5f),
                        strokeWidth = 1.2f
                    )
                    y += step
                }
                x += step
            }
        }

        PaperTemplate.RULED_NOTEBOOK -> {
            // College-ruled horizontal writing lines + vertical crimson margin line
            val lineSpacing = 42f
            val firstY = (startY / lineSpacing).toInt() * lineSpacing
            var y = firstY
            while (y <= endY) {
                drawLine(
                    color = gridColor.copy(alpha = 0.78f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 1.3f
                )
                y += lineSpacing
            }
            // Vertical Red Margin Line at X = 115f
            if (115f in startX..endX) {
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = 0.55f),
                    start = Offset(115f, startY),
                    end = Offset(115f, endY),
                    strokeWidth = 2.0f
                )
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = 0.28f),
                    start = Offset(121f, startY),
                    end = Offset(121f, endY),
                    strokeWidth = 1.0f
                )
            }
        }

        PaperTemplate.DOT_MATRIX -> {
            val step = 36f
            val firstX = (startX / step).toInt() * step
            val firstY = (startY / step).toInt() * step
            var x = firstX
            while (x <= endX) {
                var y = firstY
                while (y <= endY) {
                    drawCircle(
                        color = gridColor.copy(alpha = 0.85f),
                        radius = 1.8f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }

        PaperTemplate.GRAPH_ENGINEERING -> {
            val minorStep = 28f
            val firstX = (startX / minorStep).toInt() * minorStep
            val firstY = (startY / minorStep).toInt() * minorStep
            var x = firstX
            while (x <= endX) {
                val index = (x / minorStep).roundToInt()
                val isMajor = index % 5 == 0
                drawLine(
                    color = if (isMajor) gridColor.copy(alpha = 0.9f) else gridColor.copy(alpha = 0.42f),
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = if (isMajor) 1.6f else 0.8f
                )
                x += minorStep
            }
            var y = firstY
            while (y <= endY) {
                val index = (y / minorStep).roundToInt()
                val isMajor = index % 5 == 0
                drawLine(
                    color = if (isMajor) gridColor.copy(alpha = 0.9f) else gridColor.copy(alpha = 0.42f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = if (isMajor) 1.6f else 0.8f
                )
                y += minorStep
            }
        }

        PaperTemplate.ISOMETRIC_DRAFT -> {
            val step = 48f
            val firstY = (startY / step).toInt() * step
            var y = firstY
            while (y <= endY) {
                drawLine(
                    color = gridColor.copy(alpha = 0.55f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 1.0f
                )
                y += step
            }
            val firstX = (startX / step).toInt() * step
            var x = firstX
            while (x <= endX) {
                drawLine(
                    color = gridColor.copy(alpha = 0.65f),
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = 1.0f
                )
                x += step
            }
        }
    }
}

private fun DrawScope.drawStudioStroke(
    points: List<StrokePoint>,
    color: Color,
    strokeWidth: Float,
    alpha: Float,
    tool: DrawingTool,
    paperBgColor: Color
) {
    if (points.isEmpty()) return
    val paintColor = if (tool == DrawingTool.ERASER) {
        paperBgColor
    } else {
        color.copy(alpha = alpha.coerceIn(0.05f, 1.0f))
    }

    when (tool) {
        DrawingTool.RULER_LINE -> {
            val start = points.first()
            val end = points.last()
            drawLine(
                color = paintColor,
                start = Offset(start.x, start.y),
                end = Offset(end.x, end.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        DrawingTool.RECTANGLE_SHAPE -> {
            val start = points.first()
            val end = points.last()
            val left = min(start.x, end.x)
            val top = min(start.y, end.y)
            val w = abs(end.x - start.x).coerceAtLeast(2f)
            val h = abs(end.y - start.y).coerceAtLeast(2f)
            drawRect(
                color = paintColor,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
            )
        }

        DrawingTool.CIRCLE_SHAPE -> {
            val start = points.first()
            val end = points.last()
            val left = min(start.x, end.x)
            val top = min(start.y, end.y)
            val w = abs(end.x - start.x).coerceAtLeast(2f)
            val h = abs(end.y - start.y).coerceAtLeast(2f)
            drawOval(
                color = paintColor,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = strokeWidth)
            )
        }

        else -> {
            val path = buildSmoothPath(points)
            when (tool) {
                DrawingTool.CHISEL_MARKER -> {
                    drawPath(
                        path = path,
                        color = paintColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Square,
                            join = StrokeJoin.Bevel
                        )
                    )
                }

                DrawingTool.ART_BRUSH -> {
                    // Outer soft glaze + inner saturated core
                    drawPath(
                        path = path,
                        color = paintColor.copy(alpha = (paintColor.alpha * 0.35f).coerceAtLeast(0.05f)),
                        style = Stroke(
                            width = strokeWidth * 1.38f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    drawPath(
                        path = path,
                        color = paintColor,
                        style = Stroke(
                            width = strokeWidth * 0.85f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                DrawingTool.CHARCOAL -> {
                    drawPath(
                        path = path,
                        color = paintColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 1.4f, strokeWidth * 0.3f), 0f)
                        )
                    )
                    drawPath(
                        path = path,
                        color = paintColor.copy(alpha = paintColor.alpha * 0.65f),
                        style = Stroke(
                            width = strokeWidth * 0.7f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                DrawingTool.FOUNTAIN_PEN -> {
                    // Main calligraphy body + crisp nib edge shadow for authentic fountain ink feel
                    drawPath(
                        path = path,
                        color = paintColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                else -> {
                    drawPath(
                        path = path,
                        color = paintColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

private fun buildSmoothPath(points: List<StrokePoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    val first = points.first()
    path.moveTo(first.x, first.y)

    if (points.size == 1) {
        path.lineTo(first.x + 0.5f, first.y + 0.5f)
        return path
    }

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

@Composable
private fun CanvasMinimapOverlay(
    strokes: List<DrawnStroke>,
    zoomFloat: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    paperTemplate: PaperTemplate,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCharcoal.copy(alpha = 0.86f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 74.dp, height = 52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(paperTemplate.backgroundColorHex))
            ) {
                val worldW = max(viewportWidthPx * 2.2f, 1600f)
                val worldH = max(viewportHeightPx * 2.2f, 1600f)
                val scaleX = size.width / worldW
                val scaleY = size.height / worldH

                // Draw miniature strokes
                strokes.forEach { s ->
                    if (s.points.size >= 2) {
                        val p0 = s.points.first()
                        val p1 = s.points.last()
                        drawLine(
                            color = ColorMath.argbLongToColor(s.colorArgb),
                            start = Offset(
                                (p0.x + worldW * 0.2f) * scaleX,
                                (p0.y + worldH * 0.2f) * scaleY
                            ),
                            end = Offset(
                                (p1.x + worldW * 0.2f) * scaleX,
                                (p1.y + worldH * 0.2f) * scaleY
                            ),
                            strokeWidth = 1.6f
                        )
                    }
                }

                // Draw current viewport frame in minimap
                val viewLeft = ((-panOffsetX / zoomFloat) + worldW * 0.2f) * scaleX
                val viewTop = ((-panOffsetY / zoomFloat) + worldH * 0.2f) * scaleY
                val viewW = (viewportWidthPx / zoomFloat) * scaleX
                val viewH = (viewportHeightPx / zoomFloat) * scaleY

                drawRect(
                    color = VermilionAccent,
                    topLeft = Offset(
                        viewLeft.coerceIn(0f, size.width - 6f),
                        viewTop.coerceIn(0f, size.height - 6f)
                    ),
                    size = Size(
                        viewW.coerceIn(6f, size.width),
                        viewH.coerceIn(6f, size.height)
                    ),
                    style = Stroke(width = 1.5f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp)
            ) {
                Text(
                    text = String.format("%.2fx", zoomFloat),
                    style = MaterialTheme.typography.labelSmall,
                    color = VermilionAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(zoomFloat * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
