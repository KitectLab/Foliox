package io.github.kitectlab.foliox.animation

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.kitectlab.foliox.PageType

class CoverPageAnimation internal constructor() : PageAnimation() {
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