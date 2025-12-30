package io.github.kitectlab.foliox.animation

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.kitectlab.foliox.PageType
import androidx.compose.ui.unit.dp

@Stable
data object CoverPageAnimation : PageAnimation() {

    override val name: String = "CoverPageAnimation"

    override fun DrawScope.drawToCanvas(
        state: PageAnimationState,
        currentGraphicLayer: GraphicsLayer,
        targetGraphicLayer: GraphicsLayer,
        background: DrawScope.(pageType: PageType) -> Unit,
    ) {
        val width = state.viewportSize.width.toFloat()
        val height = state.viewportSize.height.toFloat()
        val shadowWidth = shadowWidth(state)
        val maxTravel = width + shadowWidth
        when (state.direction) {
            Direction.NEXT -> {
                background(PageType.NEXT)
                drawLayer(targetGraphicLayer)
                val delta = (state.finalOffset.value.x - state.startFirstPoint.x).coerceIn(-maxTravel..0f)
                drawOuterEdgeShadow(
                    edgeX = width + delta,
                    width = width,
                    height = height,
                    shadowWidth = shadowWidth
                )
                transformCanvas(state, PageType.CURRENT) {
                    background(PageType.CURRENT)
                    this@drawToCanvas.drawLayer(currentGraphicLayer)
                }
            }

            Direction.PREVIOUS -> {
                background(PageType.CURRENT)
                drawLayer(currentGraphicLayer)
                val delta = (state.finalOffset.value.x - state.startFirstPoint.x).coerceIn(0f, maxTravel)
                drawOuterEdgeShadow(
                    edgeX = delta,
                    width = width,
                    height = height,
                    shadowWidth = shadowWidth
                )
                transformCanvas(state, PageType.PREVIOUS) {
                    background(PageType.PREVIOUS)
                    this@drawToCanvas.drawLayer(targetGraphicLayer)
                }
            }

            Direction.NONE -> {
                transformCanvas(state, PageType.CURRENT) {
                    background(PageType.CURRENT)
                    this@drawToCanvas.drawLayer(currentGraphicLayer)
                }
            }
        }
    }

    override fun DrawScope.transformCanvas(
        state: PageAnimationState,
        pageType: PageType,
        drawBlock: DrawScope.() -> Unit
    ) {
        val width = state.viewportSize.width.toFloat()
        val shadowWidth = shadowWidth(state)
        val maxTravel = width + shadowWidth
        when (pageType) {
            PageType.NEXT -> {
                drawBlock()
            }

            else -> {
                withTransform(
                    {
                        if (state.direction == Direction.NEXT) {
                            translate(
                                left = (state.finalOffset.value.x - state.startFirstPoint.x)
                                    .coerceIn(-maxTravel..0f),
                            )
                        } else if (state.direction == Direction.PREVIOUS && pageType == PageType.PREVIOUS) {
                            translate(
                                left = -width + (state.finalOffset.value.x - state.startFirstPoint.x)
                                    .coerceIn(0f, maxTravel),
                            )
                        }
                    }
                ) {
                    drawBlock()
                }
            }
        }
    }

    override suspend fun animateToDirection(state: PageAnimationState, direction: Direction) {
        val width = state.viewportSize.width.toFloat()
        val travel = width + shadowWidth(state)
        val targetOffset = when (direction) {
            Direction.NEXT -> Offset(
                x = state.startFirstPoint.x - travel,
                y = state.startFirstPoint.y
            )
            Direction.PREVIOUS -> Offset(
                x = state.startFirstPoint.x + travel,
                y = state.startFirstPoint.y
            )
            Direction.NONE -> state.startFirstPoint
        }
        state.animateTo(targetOffset)
    }
}

private fun shadowWidth(state: PageAnimationState): Float {
    return 8.dp.value * state.density
}

private fun DrawScope.drawOuterEdgeShadow(
    edgeX: Float,
    width: Float,
    height: Float,
    shadowWidth: Float
) {
    if (shadowWidth <= 0f || height <= 0f) return
    if (edgeX >= width || edgeX + shadowWidth <= 0f) return
    val brush = Brush.horizontalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent),
        startX = edgeX,
        endX = edgeX + shadowWidth
    )
    drawRect(brush = brush, topLeft = Offset(0f, 0f), size = Size(width, height))
}
