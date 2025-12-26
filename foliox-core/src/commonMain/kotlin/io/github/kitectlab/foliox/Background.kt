import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 颜色定义
private val Ivory = Color(0xFFF8F0E3)
private val LightBrown = Color(0xFFE0D0B8)
private val DarkSpot = Color(0xFF5D4037)
private val WaterStain = Color(0x66FFF9C4) // 带透明度的淡黄水渍
private val EdgeShadow = Color(0x88422B1E)

@Composable
fun ParchmentBackground(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize().background(Ivory)) {
        drawParchmentBackground()
    }
}

internal fun DrawScope.drawParchmentBackground() {
    // 计算无缝平铺单元尺寸 (2的幂次确保高质量渲染)
    val tileSize = 512.dp.toPx().toInt()

    // 创建纹理缓存（实际应用中应使用remember缓存）
    val textureTile = buildParchmentTextureTile(tileSize)

    // 平铺纹理
    val tileWidth = textureTile.width.toFloat()
    val tileHeight = textureTile.height.toFloat()

    for (x in 0..size.width.toInt() step tileSize) {
        for (y in 0..size.height.toInt() step tileSize) {
            drawImage(
                image = textureTile,
                dstOffset = IntOffset(x, y)
            )
        }
    }

    // 绘制边缘磨损效果（只在外围）
    drawEdgeDistress(size)
}

private fun DrawScope.buildParchmentTextureTile(tileSize: Int): ImageBitmap {
    return ImageBitmap(tileSize, tileSize).apply {
        // 创建原生Canvas进行绘制
        val canvas = Canvas(this)
        with(canvas) {
            // 基础底色（渐变增加深度）
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(LightBrown, Ivory),
                    center = center,
                    radius = tileSize * 0.8f
                ),
                size = Size(tileSize.toFloat(), tileSize.toFloat())
            )

            // 生成无缝纹理（确保边缘连续）
            drawParchmentFeatures(tileSize)
        }
    }
}

private fun DrawScope.drawParchmentFeatures(tileSize: Int) {
    val tileArea = Size(tileSize.toFloat(), tileSize.toFloat())
    val center = Offset(tileSize / 2f, tileSize / 2f)

    // 不规则深褐色斑点 (使用柏林噪声分布)
    repeat(120) {
        val noise = PerlinNoise(0.05f, 3)
        val x = noise.getValue(it * 5f, 0f) * tileSize
        val y = noise.getValue(0f, it * 5f) * tileSize
        val radius = 2f + noise.getValue(x, y) * 15f

        // 跨越边界的斑点（确保无缝）
        drawCircle(
            color = DarkSpot.copy(alpha = 0.4f),
            center = Offset(x, y),
            radius = radius,
            blendMode = BlendMode.Multiply
        )
    }

    // 淡黄色水渍（不规则形状）
    repeat(15) {
        val width = 40f + Random.nextFloat() * 80f
        val height = 30f + Random.nextFloat() * 60f
        val x = Random.nextFloat() * tileSize
        val y = Random.nextFloat() * tileSize

        drawOval(
            color = WaterStain,
            topLeft = Offset(x, y),
            size = Size(width, height),
            blendMode = BlendMode.Screen
        )
    }

    // 皮革颗粒纹理（高频噪声）
    for (x in 0 until tileSize step 4) {
        for (y in 0 until tileSize step 4) {
            val intensity = 0.02f * PerlinNoise(0.2f, 2).getValue(x.toFloat(), y.toFloat())
            drawCircle(
                color = DarkSpot.copy(alpha = intensity),
                center = Offset(x.toFloat(), y.toFloat()),
                radius = 0.8f
            )
        }
    }

    // 自然褶皱（使用正弦曲线）
    repeat(8) { foldId ->
        val amplitude = 3f + Random.nextFloat() * 8f
        val frequency = 0.01f + Random.nextFloat() * 0.03f
        val phase = Random.nextFloat() * PI.toFloat()
        val path = Path().apply {
            moveTo(0f, center.y)
            for (x in 0..tileSize) {
                val y = center.y + amplitude * sin(phase + frequency * x)
                lineTo(x.toFloat(), y)
            }
        }

        drawPath(
            path = path,
            color = DarkSpot.copy(alpha = 0.08f),
            style = Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawEdgeDistress(canvasSize: Size) {
    // 边缘磨损厚度（占宽度百分比）
    val edgeThickness = minOf(canvasSize.width, canvasSize.height) * 0.08f

    // 四边磨损效果
    listOf(
        Rect(0f, 0f, canvasSize.width, edgeThickness), // 上
        Rect(0f, canvasSize.height - edgeThickness, canvasSize.width, edgeThickness), // 下
        Rect(0f, 0f, edgeThickness, canvasSize.height), // 左
        Rect(canvasSize.width - edgeThickness, 0f, edgeThickness, canvasSize.height) // 右
    ).forEach { rect ->
        // 不规则撕裂效果
        drawDistressedEdge(rect)
    }

    // 四角强化磨损
    listOf(
        Offset(0f, 0f), // 左上
        Offset(canvasSize.width, 0f), // 右上
        Offset(0f, canvasSize.height), // 左下
        Offset(canvasSize.width, canvasSize.height) // 右下
    ).forEach { corner ->
        val size = Size(edgeThickness * 1.5f, edgeThickness * 1.5f)
        drawDistressedEdge(Rect(corner, size))
    }
}

private fun DrawScope.drawDistressedEdge(area: Rect) {
    val path = Path().apply {
        // 生成不规则锯齿边缘
        val segments = 15
        val segmentWidth = area.width / segments
        var currentY = area.top

        moveTo(area.left, area.top)
        for (i in 1..segments) {
            val x = area.left + i * segmentWidth
            val depth = Random.nextFloat() * area.height * 0.7f
            currentY = if (i % 2 == 0) area.top + depth else area.top
            lineTo(x, currentY)
        }
        lineTo(area.right, area.top)
        lineTo(area.right, area.bottom)
        lineTo(area.left, area.bottom)
        close()
    }

    // 多层渲染增强立体感
    drawPath(
        path = path,
        color = EdgeShadow,
        blendMode = BlendMode.Multiply
    )
    drawPath(
        path = path,
        color = DarkSpot.copy(alpha = 0.2f),
        style = Stroke(width = 1.5f)
    )
}

// 简化版柏林噪声生成器（用于自然纹理分布）
private class PerlinNoise(frequency: Float, octaves: Int) {
    private val rand = Random(seed)
    private val gradients = mutableListOf<Offset>()
    private val perm = (0..255).shuffled(rand).toIntArray()

    init {
        repeat(256) {
            val angle = rand.nextFloat() * 2f * PI.toFloat()
            gradients.add(Offset(cos(angle), sin(angle)))
        }
    }

    fun getValue(x: Float, y: Float): Float {
        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = x0 + 1
        val y1 = y0 + 1

        val dx = x - x0
        val dy = y - y0

        val g00 = grad(x0, y0, dx, dy)
        val g01 = grad(x0, y1, dx, dy - 1)
        val g10 = grad(x1, y0, dx - 1, dy)
        val g11 = grad(x1, y1, dx - 1, dy - 1)

        val u = fade(dx)
        val v = fade(dy)

        return lerp(
            lerp(g00, g10, u),
            lerp(g01, g11, u),
            v
        )
    }

    private fun grad(ix: Int, iy: Int, dx: Float, dy: Float): Float {
        val idx = perm[(perm[ix and 255] + iy) and 255]
        return gradients[idx].x * dx + gradients[idx].y * dy
    }

    private fun fade(t: Float) = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)

    companion object {
        private const val seed = 42 // 固定种子确保纹理稳定
    }
}

@Composable
fun PreviewParchment() {
    ParchmentBackground(Modifier.fillMaxSize())
}