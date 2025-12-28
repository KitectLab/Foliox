package io.github.kitectlab.foliox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * PageBackground is represented as a Composable.
 * If you need to draw, use Canvas inside the Composable.
 */
typealias PageBackground = @Composable () -> Unit

object PageBackgrounds {
    /** Parchment-style background implemented via Canvas. */
    fun parchment(): PageBackground = {
        Canvas(modifier = Modifier.fillMaxSize().background(Ivory)) {
            drawParchmentBackground()
        }
    }

    context(context: DrawScope)
    fun drawParchment() {
        with(context) {
            drawParchmentBackground()
        }
    }

    val None: PageBackground = {}
}

/**
 * Wrap content with a page background Composable drawn beneath.
 */
@Composable
fun PageBackground(
    style: PageBackground,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.matchParentSize()) { style() }
        content()
    }
}
