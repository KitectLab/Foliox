package io.github.kitectlab.foliox.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.kitectlab.foliox.PageType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

class CurlPageAnimation internal constructor() : PageAnimation() {
    override fun calculateDirection(state: PageAnimationState): Direction {
        if (!state.isRunning) return Direction.NONE
        val width = state.viewportSize.width.toFloat()
        if (width <= 0f) return Direction.NONE
        val startX = state.startFirstPoint.x
        if (startX < width * EDGE_ZONE_FRACTION) return Direction.PREVIOUS
        if (startX > width * (1 - EDGE_ZONE_FRACTION)) return Direction.NEXT
        return super.calculateDirection(state)
    }

    override fun DrawScope.drawToCanvas(
        state: PageAnimationState,
        currentGraphicLayer: GraphicsLayer,
        targetGraphicLayer: GraphicsLayer,
        background: DrawScope.(pageType: PageType) -> Unit,
    ) {
        val width = state.viewportSize.width.toFloat()
        val height = state.viewportSize.height.toFloat()
        if (width <= 0f || height <= 0f) return

        val direction = state.direction
        if (direction == Direction.NONE) {
            background(PageType.CURRENT)
            drawLayer(currentGraphicLayer)
            return
        }

        val isLeft = direction == Direction.PREVIOUS
        val isTop = state.startFirstPoint.y <= height / 2f
        val f = Offset(if (isLeft) 0f else width, if (isTop) 0f else height)
        val points = computePoints(
            rawTouch = state.finalOffset.value,
            f = f,
            width = width,
            height = height,
            isLeft = isLeft
        )
        if (!points.isValid()) {
            background(PageType.CURRENT)
            drawLayer(currentGraphicLayer)
            return
        }

        val pathA = buildPathA(points, width, height, isTop, isLeft)
        val pathC = buildPathC(points)
        val targetType = if (isLeft) PageType.PREVIOUS else PageType.NEXT

        background(targetType)
        drawLayer(targetGraphicLayer)

        val foldShadowWidth = (distance(points.a, points.f) / 4f).coerceIn(6f, width)
        drawFoldShadow(
            pathA = pathA,
            pathC = pathC,
            c = points.c,
            j = points.j,
            referencePoint = points.f,
            width = width,
            height = height,
            shadowWidth = foldShadowWidth,
            colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent),
            clipToC = false
        )

        clipPath(pathC) {
            clipPath(pathA, ClipOp.Difference) {
                drawBackContent(points, background, currentGraphicLayer)
            }
        }

        val backShadowWidth = min(abs(points.c.x - points.e.x), abs(points.j.y - points.h.y)) * 0.5f
        drawFoldShadow(
            pathA = pathA,
            pathC = pathC,
            c = points.c,
            j = points.j,
            referencePoint = points.a,
            width = width,
            height = height,
            shadowWidth = backShadowWidth,
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            clipToC = true,
        )

        clipPath(pathA) {
            background(PageType.CURRENT)
            drawLayer(currentGraphicLayer)
        }

        drawFrontShadow(
            pathA = pathA,
            c = points.c,
            j = points.j,
            referencePoint = points.a,
            width = width,
            height = height,
            shadowWidth = foldShadowWidth * 0.6f
        )
    }

    override fun DrawScope.transformCanvas(
        state: PageAnimationState,
        pageType: PageType,
        drawBlock: DrawScope.() -> Unit
    ) {
        drawBlock()
    }
}

private const val MIN_TOUCH_DELTA = 0.1f
private const val BACK_TEXT_WASH_ALPHA = 0.5f

private data class CurlPoints(
    val a: Offset,
    val f: Offset,
    val g: Offset,
    val e: Offset,
    val h: Offset,
    val c: Offset,
    val j: Offset,
    val b: Offset,
    val k: Offset,
    val d: Offset,
    val i: Offset
)

private fun computePoints(
    rawTouch: Offset,
    f: Offset,
    width: Float,
    height: Float,
    isLeft: Boolean
): CurlPoints {
    var a = clampToBounds(rawTouch, width, height)
    a = avoidCorner(a, f)
    a = clampToBounds(a, width, height)
    a = adjustForBinding(a, f, width, height, isLeft)
    return calcPoints(a, f)
}

private fun adjustForBinding(
    a: Offset,
    f: Offset,
    width: Float,
    height: Float,
    isLeft: Boolean
): Offset {
    return if (!isLeft) {
        if (calcPointCX(a, f) < 0f) {
            adjustPointRight(a, f, width, height)
        } else {
            a
        }
    } else {
        val mirroredA = Offset(width - a.x, a.y)
        val mirroredF = Offset(width, f.y)
        val adjusted = if (calcPointCX(mirroredA, mirroredF) < 0f) {
            adjustPointRight(mirroredA, mirroredF, width, height)
        } else {
            mirroredA
        }
        Offset(width - adjusted.x, adjusted.y)
    }
}

private fun adjustPointRight(a: Offset, f: Offset, width: Float, height: Float): Offset {
    val cX = calcPointCX(a, f)
    val w0 = width - cX
    val w1 = abs(f.x - a.x).coerceAtLeast(MIN_TOUCH_DELTA)
    val w2 = width * w1 / w0
    val ax = abs(f.x - w2)
    val h1 = abs(f.y - a.y)
    val h2 = w2 * h1 / w1
    val ay = abs(f.y - h2)
    return clampToBounds(Offset(ax, ay), width, height)
}

private fun calcPointCX(a: Offset, f: Offset): Float {
    val g = Offset((a.x + f.x) / 2f, (a.y + f.y) / 2f)
    val denom = safeDenominator(f.x - g.x)
    val eX = g.x - (f.y - g.y) * (f.y - g.y) / denom
    return eX - (f.x - eX) / 2f
}

private fun calcPoints(a: Offset, f: Offset): CurlPoints {
    val g = Offset((a.x + f.x) / 2f, (a.y + f.y) / 2f)
    val denomX = safeDenominator(f.x - g.x)
    val denomY = safeDenominator(f.y - g.y)
    val e = Offset(
        x = g.x - (f.y - g.y) * (f.y - g.y) / denomX,
        y = f.y
    )
    val h = Offset(
        x = f.x,
        y = g.y - (f.x - g.x) * (f.x - g.x) / denomY
    )
    val c = Offset(
        x = e.x - (f.x - e.x) / 2f,
        y = f.y
    )
    val j = Offset(
        x = f.x,
        y = h.y - (f.y - h.y) / 2f
    )
    val b = intersection(a, e, c, j)
    val k = intersection(a, h, c, j)
    val d = Offset(
        x = (c.x + 2f * e.x + b.x) / 4f,
        y = (2f * e.y + c.y + b.y) / 4f
    )
    val i = Offset(
        x = (j.x + 2f * h.x + k.x) / 4f,
        y = (2f * h.y + j.y + k.y) / 4f
    )
    return CurlPoints(a = a, f = f, g = g, e = e, h = h, c = c, j = j, b = b, k = k, d = d, i = i)
}

private fun intersection(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Offset {
    val x1 = p1.x
    val y1 = p1.y
    val x2 = p2.x
    val y2 = p2.y
    val x3 = p3.x
    val y3 = p3.y
    val x4 = p4.x
    val y4 = p4.y
    val denom = safeDenominator((x3 - x4) * (y1 - y2) - (x1 - x2) * (y3 - y4))
    val px = ((x1 - x2) * (x3 * y4 - x4 * y3) - (x3 - x4) * (x1 * y2 - x2 * y1)) / denom
    val py = ((y1 - y2) * (x3 * y4 - x4 * y3) - (x1 * y2 - x2 * y1) * (y3 - y4)) / denom
    return Offset(px, py)
}

private fun buildPathA(
    points: CurlPoints,
    width: Float,
    height: Float,
    isTop: Boolean,
    isLeft: Boolean
): Path {
    val path = Path()
    if (!isLeft) {
        if (isTop) {
            path.moveTo(0f, 0f)
            path.lineTo(points.c.x, points.c.y)
            path.quadraticTo(points.e.x, points.e.y, points.b.x, points.b.y)
            path.lineTo(points.a.x, points.a.y)
            path.lineTo(points.k.x, points.k.y)
            path.quadraticTo(points.h.x, points.h.y, points.j.x, points.j.y)
            path.lineTo(width, height)
            path.lineTo(0f, height)
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(0f, height)
            path.lineTo(points.c.x, points.c.y)
            path.quadraticTo(points.e.x, points.e.y, points.b.x, points.b.y)
            path.lineTo(points.a.x, points.a.y)
            path.lineTo(points.k.x, points.k.y)
            path.quadraticTo(points.h.x, points.h.y, points.j.x, points.j.y)
            path.lineTo(width, 0f)
        }
    } else {
        if (isTop) {
            path.moveTo(width, 0f)
            path.lineTo(points.c.x, points.c.y)
            path.quadraticTo(points.e.x, points.e.y, points.b.x, points.b.y)
            path.lineTo(points.a.x, points.a.y)
            path.lineTo(points.k.x, points.k.y)
            path.quadraticTo(points.h.x, points.h.y, points.j.x, points.j.y)
            path.lineTo(0f, height)
            path.lineTo(width, height)
        } else {
            path.moveTo(width, 0f)
            path.lineTo(width, height)
            path.lineTo(points.c.x, points.c.y)
            path.quadraticTo(points.e.x, points.e.y, points.b.x, points.b.y)
            path.lineTo(points.a.x, points.a.y)
            path.lineTo(points.k.x, points.k.y)
            path.quadraticBezierTo(points.h.x, points.h.y, points.j.x, points.j.y)
            path.lineTo(0f, 0f)
        }
    }
    path.close()
    return path
}

private fun buildPathC(points: CurlPoints): Path {
    return Path().apply {
        moveTo(points.i.x, points.i.y)
        lineTo(points.d.x, points.d.y)
        lineTo(points.b.x, points.b.y)
        lineTo(points.a.x, points.a.y)
        lineTo(points.k.x, points.k.y)
        close()
    }
}

private fun DrawScope.drawBackContent(
    points: CurlPoints,
    background: DrawScope.(pageType: PageType) -> Unit,
    currentGraphicLayer: GraphicsLayer
) {
    val e = points.e
    val f = points.f
    val h = points.h
    val eh = hypot(f.x - e.x, h.y - f.y)
    if (eh <= MIN_TOUCH_DELTA) return
    val sin0 = (f.x - e.x) / eh
    val cos0 = (h.y - f.y) / eh
    val degrees = atan2(sin0, cos0).toDouble().toDegrees().toFloat()
    withTransform({
        rotate(-degrees, pivot = e)
        scale(scaleX = -1f, scaleY = 1f, pivot = e)
        rotate(degrees, pivot = e)
    }) {
        background(PageType.CURRENT)
        drawLayer(currentGraphicLayer)
        drawRect(
            color = Color.White.copy(alpha = BACK_TEXT_WASH_ALPHA),
            blendMode = BlendMode.Screen
        )
    }
}

private fun DrawScope.drawFoldShadow(
    pathA: Path,
    pathC: Path,
    c: Offset,
    j: Offset,
    referencePoint: Offset,
    width: Float,
    height: Float,
    shadowWidth: Float,
    colors: List<Color>,
    clipToC: Boolean,
) {
    if (shadowWidth <= 0f) return
    val normal = foldNormalToward(c, j, referencePoint)
    val unit = normalize(normal)
    if (unit == Offset.Zero) return
    val start = c
    val end = Offset(c.x + unit.x * shadowWidth, c.y + unit.y * shadowWidth)
    val brush = Brush.linearGradient(colors = colors, start = start, end = end)
    val drawShadow: DrawScope.() -> Unit = {
        drawRect(brush = brush, topLeft = Offset.Zero, size = Size(width, height))
    }
    if (clipToC) {
        clipPath(pathC) {
            clipPath(pathA, ClipOp.Difference) {
                drawShadow()
            }
        }
    } else {
        clipPath(pathA, ClipOp.Difference) {
            clipPath(pathC, ClipOp.Difference) {
                drawShadow()
            }
        }
    }
}

private fun DrawScope.drawFrontShadow(
    pathA: Path,
    c: Offset,
    j: Offset,
    referencePoint: Offset,
    width: Float,
    height: Float,
    shadowWidth: Float
) {
    if (shadowWidth <= 0f) return
    val normal = foldNormalToward(c, j, referencePoint)
    val unit = normalize(normal)
    if (unit == Offset.Zero) return
    val start = c
    val end = Offset(c.x + unit.x * shadowWidth, c.y + unit.y * shadowWidth)
    val brush = Brush.linearGradient(
        colors = listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
        start = start,
        end = end
    )
    clipPath(pathA) {
        drawRect(brush = brush, topLeft = Offset.Zero, size = Size(width, height))
    }
}

private fun clampToBounds(point: Offset, width: Float, height: Float): Offset {
    val minX = MIN_TOUCH_DELTA
    val minY = MIN_TOUCH_DELTA
    val maxX = (width - MIN_TOUCH_DELTA).coerceAtLeast(minX)
    val maxY = (height - MIN_TOUCH_DELTA).coerceAtLeast(minY)
    return Offset(
        x = point.x.coerceIn(minX, maxX),
        y = point.y.coerceIn(minY, maxY)
    )
}

private fun avoidCorner(point: Offset, f: Offset): Offset {
    var x = point.x
    var y = point.y
    if (abs(f.x - x) < MIN_TOUCH_DELTA) {
        x += if (x < f.x) -MIN_TOUCH_DELTA else MIN_TOUCH_DELTA
    }
    if (abs(f.y - y) < MIN_TOUCH_DELTA) {
        y += if (y < f.y) -MIN_TOUCH_DELTA else MIN_TOUCH_DELTA
    }
    return Offset(x, y)
}

private fun safeDenominator(value: Float): Float {
    return when {
        abs(value) < MIN_TOUCH_DELTA && value >= 0f -> MIN_TOUCH_DELTA
        abs(value) < MIN_TOUCH_DELTA -> -MIN_TOUCH_DELTA
        else -> value
    }
}

private fun normalize(v: Offset): Offset {
    val length = hypot(v.x, v.y)
    return if (length > MIN_TOUCH_DELTA) {
        Offset(v.x / length, v.y / length)
    } else {
        Offset.Zero
    }
}

private fun foldNormalToward(c: Offset, j: Offset, toward: Offset): Offset {
    val vx = j.x - c.x
    val vy = j.y - c.y
    val n1 = Offset(vy, -vx)
    val n2 = Offset(-vy, vx)
    val toTarget = Offset(toward.x - c.x, toward.y - c.y)
    return if (n1.x * toTarget.x + n1.y * toTarget.y > 0f) n1 else n2
}

private fun distance(a: Offset, b: Offset): Float {
    return hypot(a.x - b.x, a.y - b.y)
}

private fun CurlPoints.isValid(): Boolean {
    return listOf(a, f, g, e, h, c, j, b, k, d, i).all { it.isFinite() }
}

private fun Offset.isFinite(): Boolean = x.isFinite() && y.isFinite()

private fun Double.toDegrees(): Float = (this * 180.0 / PI).toFloat()
