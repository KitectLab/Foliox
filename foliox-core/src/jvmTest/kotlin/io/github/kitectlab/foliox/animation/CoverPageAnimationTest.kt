package io.github.kitectlab.foliox.animation

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.kitectlab.foliox.PageType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverPageAnimationTest {
    @Test
    fun previousDragAlignsTheIncomingPageBeforeCommit() = withAnimationClock {
        for (density in listOf(1f, 2.75f)) {
            for (dragFraction in listOf(0.6f, 1.2f)) {
                val state = newState(density)
                val width = state.viewportSize.width.toFloat()
                val start = Offset(width * 0.1f, 200f)
                state.startDrag(start)
                state.snapTo(start + Offset(width * dragFraction, 0f))
                state.direction = PageAnimation.Direction.PREVIOUS
                var commits = 0

                CoverPageAnimation.handleDragEnd(state) { page ->
                    assertEquals(PageType.PREVIOUS, page)
                    assertEquals(width, state.finalOffset.value.x - start.x, 0.001f,
                        "The incoming page must reach the viewport origin before becoming current")
                    commits++
                }

                assertEquals(1, commits)
                assertEquals(Offset.Zero, state.finalOffset.value)
            }
        }
    }

    @Test
    fun previousTapUsesTheSameAlignedEndpoint() = withAnimationClock {
        val state = newState(2.75f)
        val width = state.viewportSize.width.toFloat()
        val start = Offset(width * 0.1f, 200f)
        var commits = 0

        CoverPageAnimation.handleTap(state, start) { page ->
            assertEquals(PageType.PREVIOUS, page)
            assertEquals(width, state.finalOffset.value.x - start.x, 0.001f)
            commits++
        }

        assertEquals(1, commits)
    }

    @Test
    fun nextDragStillClearsTheOutgoingShadowBeforeCommit() = withAnimationClock {
        val state = newState(2.75f)
        val width = state.viewportSize.width.toFloat()
        val start = Offset(width * 0.9f, 200f)
        state.startDrag(start)
        state.snapTo(start - Offset(width * 0.6f, 0f))
        state.direction = PageAnimation.Direction.NEXT
        var commits = 0

        CoverPageAnimation.handleDragEnd(state) { page ->
            assertEquals(PageType.NEXT, page)
            val rightEdge = width + state.finalOffset.value.x - start.x
            assertTrue(rightEdge + 8f * state.density <= 0.001f,
                "The outgoing shadow must leave the viewport before the page changes")
            commits++
        }

        assertEquals(1, commits)
    }

    @Test
    fun shortPreviousDragReturnsWithoutChangingPages() = withAnimationClock {
        val state = newState(2.75f)
        val width = state.viewportSize.width.toFloat()
        val start = Offset(width * 0.1f, 200f)
        state.startDrag(start)
        state.snapTo(start + Offset(width * 0.1f, 0f))
        state.direction = PageAnimation.Direction.PREVIOUS

        CoverPageAnimation.handleDragEnd(state) {
            error("A short drag must return to the current page")
        }

        assertEquals(Offset.Zero, state.finalOffset.value)
        assertTrue(!state.isRunning)
    }

    private fun newState(density: Float) = PageAnimationState().apply {
        viewportSize = IntSize((360f * density).toInt(), (600f * density).toInt())
        this.density = density
        hasPrevious = true
        hasNext = true
    }

    private fun withAnimationClock(block: suspend () -> Unit) = runBlocking(
        object : MonotonicFrameClock {
            private var timeNanos = 0L

            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                timeNanos += 16_000_000L
                return onFrame(timeNanos)
            }
        }
    ) { block() }
}
