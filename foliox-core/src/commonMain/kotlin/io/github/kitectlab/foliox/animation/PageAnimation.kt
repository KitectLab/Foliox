package io.github.kitectlab.foliox.animation


import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.kitectlab.foliox.PageType

abstract class PageAnimation {

    abstract val name: String

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

    open suspend fun handleTap(
        state: PageAnimationState,
        offset: Offset,
        onCurrentPageChange: (pageType: PageType) -> Unit
    ) {
        state.startTap(offset)
        val direction = resolveTapDirection(state, offset)
        animateToDirection(state, direction)
        if (direction == Direction.NEXT) {
            onCurrentPageChange(PageType.NEXT)
        } else if (direction == Direction.PREVIOUS) {
            onCurrentPageChange(PageType.PREVIOUS)
        }
        state.resetAnimation()
    }

    open suspend fun handleDragEnd(
        state: PageAnimationState,
        onCurrentPageChange: (pageType: PageType) -> Unit
    ) {
        val direction = resolveReleaseDirection(state)
        state.dragging = false
        animateToDirection(state, direction)
        if (direction == Direction.NEXT) {
            onCurrentPageChange(PageType.NEXT)
        } else if (direction == Direction.PREVIOUS) {
            onCurrentPageChange(PageType.PREVIOUS)
        }
        state.resetAnimation()
    }

    protected open fun resolveTapDirection(state: PageAnimationState, tapOffset: Offset): Direction {
        val width = state.viewportSize.width.toFloat()
        if (width <= 0f) return Direction.NONE
        val startX = tapOffset.x
        val direction = when {
            startX < width * EDGE_ZONE_FRACTION -> Direction.PREVIOUS
            startX > width * (1 - EDGE_ZONE_FRACTION) -> Direction.NEXT
            else -> Direction.NONE
        }
        return clampDirectionByAvailability(direction, state)
    }

    protected open fun resolveReleaseDirection(state: PageAnimationState): Direction {
        val width = state.viewportSize.width.toFloat()
        if (width <= 0f) return Direction.NONE
        val startX = state.startFirstPoint.x
        val currentX = state.finalOffset.value.x
        val deltaX = currentX - startX
        val direction = when (state.direction) {
            Direction.NEXT -> if (-deltaX > width * SWIPE_THRESHOLD) Direction.NEXT else Direction.NONE
            Direction.PREVIOUS -> if (deltaX > width * SWIPE_THRESHOLD) Direction.PREVIOUS else Direction.NONE
            Direction.NONE -> when {
                deltaX > width * SWIPE_THRESHOLD -> Direction.PREVIOUS
                -deltaX > width * SWIPE_THRESHOLD -> Direction.NEXT
                else -> Direction.NONE
            }
        }
        return clampDirectionByAvailability(direction, state)
    }

    protected open suspend fun animateToDirection(state: PageAnimationState, direction: Direction) {
        val width = state.viewportSize.width.toFloat()
        val targetOffset = when (direction) {
            Direction.NEXT -> Offset(
                x = state.startFirstPoint.x - width,
                y = state.startFirstPoint.y
            )
            Direction.PREVIOUS -> Offset(
                x = state.startFirstPoint.x + width,
                y = state.startFirstPoint.y
            )
            Direction.NONE -> state.startFirstPoint
        }
        state.animateTo(targetOffset)
    }

    protected fun clampDirectionByAvailability(direction: Direction, state: PageAnimationState): Direction {
        return when (direction) {
            Direction.NEXT if !state.hasNext -> Direction.NONE
            Direction.PREVIOUS if !state.hasPrevious -> Direction.NONE
            else -> direction
        }
    }

    companion object {
        internal const val SWIPE_THRESHOLD = 0.2f  // 滑动超过屏幕宽度的20%
        internal const val EDGE_ZONE_FRACTION = 0.3f  // 屏幕边缘10%区域作为触发区

        @Stable
        fun cover(): CoverPageAnimation  = CoverPageAnimation

        @Stable
        fun slide(): SlidePageAnimation = SlidePageAnimation

        @Stable
        fun curl(): CurlPageAnimation = CurlPageAnimation

    }

    enum class Direction {
        NEXT,
        PREVIOUS,
        NONE
    }
}
