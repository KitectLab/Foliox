package io.github.kitectlab.foliox.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntSize

@Composable
fun rememberPageAnimationState(): PageAnimationState {

    return remember {
        PageAnimationState()
    }
}

// Read necessary page gesture state
class PageAnimationState {
    var viewportSize by mutableStateOf(IntSize.Zero)
        internal set
    var density by mutableStateOf(1f)
        internal set

    private val mutex = MutatorMutex()

    var startFirstPoint by mutableStateOf(Offset.Zero)
        private set

    var lastOffset: Offset by mutableStateOf(Offset.Zero)
        private set

    val finalOffset = Animatable(Offset.Zero, Offset.VectorConverter)

    suspend fun startDrag(offset: Offset) {
        mutex.mutate {
            startFirstPoint = offset
            lastOffset = finalOffset.value
            finalOffset.snapTo(offset)
            dragging = true
        }
    }

    suspend fun dragging(change: PointerInputChange, dragAmount: Offset) {
        mutex.mutate {
            lastOffset = finalOffset.value
            finalOffset.snapTo(lastOffset + dragAmount)
        }
    }

    suspend fun startTap(offset: Offset) {
        mutex.mutate {
            startFirstPoint = offset
            lastOffset = finalOffset.value
            finalOffset.snapTo(offset)
            dragging = false
        }
    }

    suspend fun snapTo(offset: Offset) {
        mutex.mutate {
            lastOffset = finalOffset.value
            finalOffset.snapTo(offset)
        }
    }

    suspend fun animateTo(offset: Offset) {
        mutex.mutate {
            lastOffset = finalOffset.value
            finalOffset.animateTo(offset)
        }
    }

    suspend fun resetAnimation() {
        lastOffset = Offset.Zero
        startFirstPoint = Offset.Zero
        finalOffset.snapTo(Offset.Zero)
    }

    var dragging by mutableStateOf(false)
        internal set

    val isRunning by derivedStateOf { finalOffset.isRunning || dragging }

    var hasNext by mutableStateOf(false)
        internal set
    var hasPrevious by mutableStateOf(false)
        internal set

    var direction by mutableStateOf(PageAnimation.Direction.NONE)
        internal set

    val canRunAnimation by derivedStateOf { (direction == PageAnimation.Direction.NEXT && hasNext) || (direction == PageAnimation.Direction.PREVIOUS && hasPrevious)  }
}
