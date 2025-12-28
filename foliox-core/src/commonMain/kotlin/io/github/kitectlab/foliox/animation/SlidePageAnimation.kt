package io.github.kitectlab.foliox.animation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.kitectlab.foliox.PageType

@Stable
data object SlidePageAnimation : PageAnimation() {

    override val name: String = "SlidePageAnimation"

    override fun DrawScope.transformCanvas(
        state: PageAnimationState,
        pageType: PageType,
        drawBlock: DrawScope.() -> Unit
    ) {
        when (state.direction) {
            Direction.NEXT -> {
                withTransform(
                    transformBlock = {
                        val offset = (state.finalOffset.value.x - state.startFirstPoint.x).coerceAtLeast(-state.viewportSize.width.toFloat())
                        if (pageType == PageType.NEXT) {
                            translate(left = state.viewportSize.width + offset)
                        } else if (pageType == PageType.CURRENT) {
                            translate(left = offset)
                        }
                    },
                    drawBlock = drawBlock
                )
            }
            Direction.PREVIOUS -> {
                withTransform(
                    transformBlock = {
                        val offset = (state.finalOffset.value.x - state.startFirstPoint.x).coerceAtMost(state.viewportSize.width.toFloat())
                        if (pageType == PageType.PREVIOUS) {
                            translate(left = -state.viewportSize.width + offset)
                        } else if (pageType == PageType.CURRENT) {
                            translate(left = offset)
                        }
                    },
                    drawBlock = drawBlock
                )
            }
            else -> {
                drawBlock()
            }
        }
    }
}
