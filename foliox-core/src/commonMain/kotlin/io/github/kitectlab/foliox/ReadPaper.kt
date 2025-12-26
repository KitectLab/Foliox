package io.github.kitectlab.foliox

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily

@Composable
fun ReadPaper(
    loader: PaperLoader,
    modifier: Modifier = Modifier,
    scope: PaperDrawScope = PaperDrawScope.DEFAULT
) { // 监测手指滑动，用来实现翻页动画和点击翻页，以及点击中间打开菜单

    val touchX = remember { mutableFloatStateOf(0f) }
    val touchY = remember { mutableFloatStateOf(0f) }
    val cornerX = remember { mutableFloatStateOf(0f) }
    val cornerY = remember { mutableFloatStateOf(0f) }

    // 记录两个页面的 GraphicsLayer
    val currentLayer = rememberGraphicsLayer()
    val nextLayer = rememberGraphicsLayer()
    Box(
        modifier = modifier.fillMaxSize().pointerInput(Unit) {

            detectDragGestures(
                onDragStart = { offset ->
                    // 开始滑动
                    // offset 包含手指按下的位置信息 (offset.x, offset.y)
                },
                onDrag = { change, dragAmount ->
                    // 滑动中
                    // change.position 包含当前手指位置信息
                    change.position
                    change.consume()
                },
                onDragEnd = {
                    // 滑动结束
                }
            )
        }
    ) {

        // 下一页
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    nextLayer.record {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {

        }
        // 当前页
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    currentLayer.record {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {

        }
    }
}

@Composable
fun Background(modifier: Modifier = Modifier) {

}

@Composable
private fun PaperDrawScope.PaperContent(
    page: Page,
    fontFamily: FontFamily
) {
    Column {
        page.spans.forEach {
            drawText(it, fontFamily)
        }
    }
}

interface PaperDrawScope {
    @Composable
    fun drawText(content: AnnotatedString, fontFamily: FontFamily)

    @Composable
    fun drawCustom() = {}

    companion object {
        val DEFAULT = object : PaperDrawScope {

            @Composable
            override fun drawText(content: AnnotatedString, fontFamily: FontFamily) {

            }
        }
    }
}

class Page(
    val spans: List<AnnotatedString>,
    val position: LongRange
)