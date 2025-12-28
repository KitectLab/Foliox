package io.github.kitectlab.foliox.animation


import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.kitectlab.foliox.PageType
import kotlin.math.abs

abstract class PageAnimation {

    open fun calculateDirection(state: PageAnimationState): Direction {
        if (!state.isRunning) return Direction.NONE
        val startX = state.startFirstPoint.x
        val deltaX = state.finalOffset.value.x - state.startFirstPoint.x
        if (deltaX > 0f) {
            return Direction.PREVIOUS
        }
        if (deltaX < 0f) {
            return Direction.NEXT
        }
        if (startX < state.viewportSize.width * EDGE_ZONE_FRACTION) {
            return Direction.PREVIOUS
        }
        if (startX > state.viewportSize.width * (1 - EDGE_ZONE_FRACTION)) {
            return Direction.NEXT
        }
        return Direction.NONE
    }

    open fun DrawScope.drawToCanvas(
        state: PageAnimationState,
        currentGraphicLayer: GraphicsLayer,
        targetGraphicLayer: GraphicsLayer,
        background: DrawScope.(pageType: PageType) -> Unit,
    ) {
        when(state.direction) {
            Direction.NEXT -> {
                transformCanvas(state, PageType.NEXT) {
                    background(PageType.NEXT)
                    this@drawToCanvas.drawLayer(targetGraphicLayer)
                }
                transformCanvas(state, PageType.CURRENT) {
                    background(PageType.CURRENT)
                    this@drawToCanvas.drawLayer(currentGraphicLayer)
                }
            }
            Direction.PREVIOUS -> {
                transformCanvas(state, PageType.CURRENT) {
                    background(PageType.CURRENT)
                    this@drawToCanvas.drawLayer(currentGraphicLayer)
                }
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

    abstract fun DrawScope.transformCanvas(state: PageAnimationState, pageType: PageType, drawBlock: DrawScope.() -> Unit)

    companion object {
        internal const val SWIPE_THRESHOLD = 0.2f  // 滑动超过屏幕宽度的20%
        internal const val EDGE_ZONE_FRACTION = 0.3f  // 屏幕边缘10%区域作为触发区

        fun cover(): CoverPageAnimation  = CoverPageAnimation()

        fun slide(): SlidePageAnimation = SlidePageAnimation()

        fun curl(): CurlPageAnimation = CurlPageAnimation()

    }

    enum class Direction {
        NEXT,
        PREVIOUS,
        NONE
    }
}
