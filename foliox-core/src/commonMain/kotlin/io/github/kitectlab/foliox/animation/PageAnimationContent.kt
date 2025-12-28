package io.github.kitectlab.foliox.animation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import io.github.kitectlab.foliox.PageType
import io.github.kitectlab.foliox.animation.PageAnimation.Direction
import kotlinx.coroutines.launch

@Composable
fun PageAnimationContent(
    modifier: Modifier = Modifier,
    state: PageAnimationState = rememberPageAnimationState(),
    animation: PageAnimation = PageAnimation.cover(),
    onCurrentChange: (pageType: PageType) -> Unit = {},
    background: DrawScope.(pageType: PageType) -> Unit = {  },
    content: @Composable (pageType: PageType) -> Unit
) {
    val targetGraphicLayer = rememberGraphicsLayer()
    val currentGraphicLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    Layout(
        contents = listOf(
            { content(PageType.PREVIOUS) },
            { content(PageType.CURRENT) },
            { content(PageType.NEXT) }
        ),
        modifier = modifier
            .clip(RectangleShape)
            .onSizeChanged {
                state.viewportSize = it
            }.drawWithContent {
                val isRunningAnimation = state.canRunAnimation && state.isRunning
                if (!isRunningAnimation) {
                    background(PageType.CURRENT)
                    drawContent()
                } else {
                    withTransform(
                        transformBlock = {
                            clipRect(0f, 0f, 0f, 0f)
                        },
                        drawBlock = {
                           this@drawWithContent.drawContent()
                        }
                    )
                    with(animation) {
                        drawToCanvas(state, currentGraphicLayer, targetGraphicLayer, background)
                    }
                }
        }
            .pointerInput(state) {
                detectTapGestures(
                    onTap = {
                        scope.launch {
                            val direction = state.tap(it)
                            if (direction == Direction.NEXT) {
                                onCurrentChange(PageType.NEXT)
                            } else if (direction == Direction.PREVIOUS) {
                                onCurrentChange(PageType.PREVIOUS)
                            }
                            state.resetAnimation()
                        }
                    }
                )
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = {
                        scope.launch {
                            state.startDrag(it)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val direction = state.dragEnd()
                            if (direction == Direction.NEXT) {
                                onCurrentChange(PageType.NEXT)
                            } else if (direction == Direction.PREVIOUS) {
                                onCurrentChange(PageType.PREVIOUS)
                            }
                            state.resetAnimation()
                        }
                    },
                    onDragCancel = {
                        state.dragging = false
                        scope.launch {
                            state.resetAnimation()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        scope.launch {
                            state.dragging(change, dragAmount)
                        }
                        change.consume()
                    }
                )
            }
    ) { (previous, current, next), constraints ->
        state.hasPrevious = previous.isNotEmpty()
        state.hasNext = next.isNotEmpty()
        if (current.isEmpty()) {
            return@Layout layout(0, 0) {  }
        }
        val current = current.first().measure(constraints)
        if (!state.isRunning) {
            return@Layout layout(current.width, current.height) {
                current.placeWithLayer(0, 0)
            }
        }
        state.direction = animation.calculateDirection(state)
        return@Layout when (state.direction) {
            Direction.PREVIOUS if state.hasPrevious -> {
                val previous = previous.first().measure(constraints)
                layout(current.width, current.height) {
                    current.placeWithLayer(0, 0, currentGraphicLayer)
                    previous.placeWithLayer(0, 0, targetGraphicLayer)
                }
            }
            Direction.NEXT if state.hasNext -> {
                val next = next.first().measure(constraints)
                layout(current.width, current.height) {
                    next.placeWithLayer(0, 0,  targetGraphicLayer)
                    current.placeWithLayer(0, 0, currentGraphicLayer)
                }
            }
            else -> {
                layout(current.width, current.height) {
                    current.placeWithLayer(0, 0, currentGraphicLayer)
                }
            }
        }
    }

}
