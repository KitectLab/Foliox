package io.github.kitectlab.foliox.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.kitectlab.foliox.PageType

class CoverPageAnimation internal constructor() : PageAnimation() {
    override fun DrawScope.drawToCanvas(
        state: PageAnimationState,
        currentGraphicLayer: GraphicsLayer,
        targetGraphicLayer: GraphicsLayer,
        background: DrawScope.(pageType: PageType) -> Unit,
    ) {
        val width = state.viewportSize.width.toFloat()
        val height = state.viewportSize.height.toFloat()
        val shadowWidth = (width * 0.06f).coerceIn(8f, 32f)
        when (state.direction) {
            Direction.NEXT -> {
                background(PageType.NEXT)
                drawLayer(targetGraphicLayer)
                val delta = (state.finalOffset.value.x - state.startFirstPoint.x).coerceIn(-width..0f)
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
                val delta = (state.finalOffset.value.x - state.startFirstPoint.x).coerceIn(0f, width)
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
        when (pageType) {
            PageType.NEXT -> {
                drawBlock()
            }

            else -> {
                withTransform(
                    {
                        if (state.direction == Direction.NEXT) {
                            translate(
                                left = ((state.finalOffset.value.x - state.startFirstPoint.x)).coerceIn(
                                    -state.viewportSize.width.toFloat()..0f
                                ),
                            )
                        } else if (state.direction == Direction.PREVIOUS && pageType == PageType.PREVIOUS) {
                            translate(
                                left = -state.viewportSize.width + (state.finalOffset.value.x - state.startFirstPoint.x).coerceIn(0f, state.viewportSize.width.toFloat()),
                            )
                        }
                    }
                ) {
                    drawBlock()
                }
            }
        }
    }
}

private fun DrawScope.drawOuterEdgeShadow(
    edgeX: Float,
    width: Float,
    height: Float,
    shadowWidth: Float
) {
    if (shadowWidth <= 0f || height <= 0f) return
    val left = edgeX.coerceIn(0f, width)
    val right = (edgeX + shadowWidth).coerceIn(0f, width)
    if (right <= left) return
    val brush = Brush.horizontalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent),
        startX = left,
        endX = right
    )
    drawRect(brush = brush, topLeft = Offset(left, 0f), size = Size(right - left, height))
}
