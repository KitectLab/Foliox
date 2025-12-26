package io.github.kitectlab.foliox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import kotlin.random.Random

// Parchment color palette - realistic aged paper tones
internal val Ivory = Color(0xFFF5E6D3)  // Warm ivory base
private val ParchmentBase = Color(0xFFEFDFC8)  // Parchment mid-tone
private val AgedBrown = Color(0xFFD4C4A8)  // Aged edge color
private val SubtleSpot = Color(0x15654321)  // Subtle spots (low opacity)
private val FiberLine = Color(0x28654321)  // Fiber texture line color (increased opacity)

@Composable
fun ParchmentBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().background(Ivory)) {
        drawParchmentBackground()
    }
}

internal fun DrawScope.drawParchmentBackground() {
    // Fixed-seed random generator ensures consistent rendering across different pages
    val random = Random(PARCHMENT_SEED)
    
    // Base radial gradient (center bright, edges dark, simulating natural lighting)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Ivory, ParchmentBase, AgedBrown),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.width.coerceAtLeast(size.height) * 0.7f
        )
    )
    
    // 1. Subtle paper noise texture (creates rough paper feel)
    val noiseStep = 8  // Noise point spacing
    for (x in 0 until size.width.toInt() step noiseStep) {
        for (y in 0 until size.height.toInt() step noiseStep) {
            // Use position as part of seed for stability
            val localSeed = (x * 73 + y * 37) % 256
            val alpha = (localSeed % 5) * 0.0008f  // Reduced: 0.000 ~ 0.0032 (was 0.004)
            if (alpha > 0.0005f) {
                drawCircle(
                    color = Color.Black.copy(alpha = alpha),
                    center = Offset(x.toFloat(), y.toFloat()),
                    radius = 0.8f
                )
            }
        }
    }
    
    // 2. Vertical paper fiber texture (main fibers - simulates handmade paper)
    for (i in 0 until size.width.toInt() step 5) {  // Sparser: step 2 -> 5
        val fiberSeed = (i * 37 + PARCHMENT_SEED.toInt()) % 1000
        val alpha = 0.04f + (fiberSeed % 10) * 0.005f
        
        // Varying fiber length (60% to 100% of height)
        val lengthRatio = 0.6f + (fiberSeed % 40) * 0.01f
        val fiberLength = size.height * lengthRatio
        val startY = (size.height - fiberLength) * ((fiberSeed % 100) / 100f)
        
        // Varying stroke width (0.4f to 0.8f)
        val strokeWidth = 0.4f + (fiberSeed % 5) * 0.08f
        
        // Create slightly wavy path instead of straight line
        val path = Path().apply {
            moveTo(i.toFloat(), startY)
            val segments = 20
            val segmentHeight = fiberLength / segments
            for (j in 1..segments) {
                val y = startY + j * segmentHeight
                // Subtle wave using deterministic sine
                val wave = sin((fiberSeed + j) * 0.5f) * 1.5f
                lineTo(i.toFloat() + wave, y)
            }
        }
        
        drawPath(
            path = path,
            color = FiberLine.copy(alpha = alpha),
            style = Stroke(width = strokeWidth)
        )
    }
    
    // 3. Subtle horizontal fibers (creates weave effect)
    for (i in 0 until size.height.toInt() step 50) {  // Sparser: step 30 -> 50
        val fiberSeed = (i * 73 + PARCHMENT_SEED.toInt()) % 1000
        val alpha = 0.015f + (fiberSeed % 3) * 0.005f
        
        // Varying fiber length (50% to 90% of width)
        val lengthRatio = 0.5f + (fiberSeed % 40) * 0.01f
        val fiberLength = size.width * lengthRatio
        val startX = (size.width - fiberLength) * ((fiberSeed % 100) / 100f)
        
        // Varying stroke width (0.3f to 0.6f)
        val strokeWidth = 0.3f + (fiberSeed % 4) * 0.075f
        
        // Create slightly wavy path
        val path = Path().apply {
            moveTo(startX, i.toFloat())
            val segments = 15
            val segmentWidth = fiberLength / segments
            for (j in 1..segments) {
                val x = startX + j * segmentWidth
                val wave = sin((fiberSeed + j) * 0.4f) * 1.2f
                lineTo(x, i.toFloat() + wave)
            }
        }
        
        drawPath(
            path = path,
            color = FiberLine.copy(alpha = alpha),
            style = Stroke(width = strokeWidth)
        )
    }
    
    // 4. Small paper imperfections and spots
    repeat(50) { idx ->
        val x = (idx * 37 % size.width.toInt()).toFloat()
        val y = (idx * 73 % size.height.toInt()).toFloat()
        val radius = 0.8f + (idx % 4) * 0.4f
        val alpha = 0.3f + (idx % 3) * 0.1f
        drawCircle(
            color = SubtleSpot.copy(alpha = alpha),
            center = Offset(x, y),
            radius = radius
        )
    }
    
    // 5. Soft aged edge effect (gentle fade without jagged edges)
    val edgeFade = 40f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(AgedBrown.copy(alpha = 0.1f), Color.Transparent),
            startY = 0f,
            endY = edgeFade
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, edgeFade)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, AgedBrown.copy(alpha = 0.1f)),
            startY = size.height - edgeFade,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height - edgeFade),
        size = Size(size.width, edgeFade)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(AgedBrown.copy(alpha = 0.1f), Color.Transparent),
            startX = 0f,
            endX = edgeFade
        ),
        topLeft = Offset.Zero,
        size = Size(edgeFade, size.height)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, AgedBrown.copy(alpha = 0.1f)),
            startX = size.width - edgeFade,
            endX = size.width
        ),
        topLeft = Offset(size.width - edgeFade, 0f),
        size = Size(edgeFade, size.height)
    )
}

// Fixed seed ensures stable parchment effect across renders
private const val PARCHMENT_SEED = 20251226L

@Composable
fun PreviewParchment() {
    ParchmentBackground(Modifier.fillMaxSize())
}