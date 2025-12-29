import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.setOutline
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import foliox.sampleapp.sharedapp.generated.resources.Res
import io.github.kitectlab.foliox.PageType
import io.github.kitectlab.foliox.animation.PageAnimation
import io.github.kitectlab.foliox.animation.PageAnimationContent
import io.github.kitectlab.foliox.animation.PageAnimationState
import io.github.kitectlab.foliox.animation.rememberPageAnimationState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.kitectlab.foliox.PageBackground
import io.github.kitectlab.foliox.PageBackgrounds
import io.github.kitectlab.foliox.animation.CoverPageAnimation
import io.github.kitectlab.foliox.animation.CurlPageAnimation
import io.github.kitectlab.foliox.animation.SlidePageAnimation

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        val animationState = rememberPageAnimationState()
        Column {
            Text("page: hasNext: ${animationState.hasNext}, hasPrevious: ${animationState.hasPrevious}")
            Text("page running:  ${animationState.isRunning}, dragging: ${animationState.dragging}, canRunAnimation: ${animationState.canRunAnimation}")
            Text("page direction: ${animationState.direction}")
            Text("page last offset: ${animationState.lastOffset}")
            Text("page target offset: ${animationState.finalOffset.value}")
            val animations = remember {
                listOf(
                    PageAnimation.cover(),
                    PageAnimation.slide(),
                    PageAnimation.curl(),
                )
            }
            val currentAnimation = remember(animations) { mutableStateOf(animations.last()) }
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                animations.forEachIndexed { index, animation ->
                    ToggleButton(
                        checked = currentAnimation.value == animation,
                        onCheckedChange = {
                            currentAnimation.value = animation
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            animations.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text(
                            text = when(animation) {
                                is CoverPageAnimation -> "Cover"
                                is SlidePageAnimation -> "Slide"
                                is CurlPageAnimation -> "Curl"
                                else -> animation.toString()
                            },
                        )
                    }
                }
            }
            PageReader(animationState, currentAnimation.value, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PageReader(animationState: PageAnimationState,animation: PageAnimation, modifier: Modifier = Modifier) {
    val resolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val style = LocalTextStyle.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val source = remember(constraints) {
            TestPagingSource(
                defaultFontFamilyResolver = resolver,
                defaultDensity = density,
                defaultLayoutDirection = direction,
                constraints = constraints,
                style = style,
            )
        }
        val pager = remember(source) {
            Pager(
                PagingConfig(20),
                pagingSourceFactory = { source }
            )
        }
        val flow = pager.flow.collectAsLazyPagingItems()

        var currentIndex by remember(flow) { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()
        PageAnimationContent(
            animation = animation,
            state = animationState,
            onCurrentChange = {
                when (it) {
                    PageType.PREVIOUS -> {
                        currentIndex = (currentIndex - 1).coerceAtLeast(0)
                    }

                    PageType.NEXT -> {
                        currentIndex = (currentIndex + 1).coerceAtMost(flow.itemCount - 1)
                    }

                    else -> {}
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            PageBackground(
                style = PageBackgrounds.parchment(),
            ) {
                when (it) {
                    PageType.CURRENT -> {
                        if (flow.itemCount <= 0) {
                            Text("No item current")
                        } else {
                            flow[currentIndex]?.let {
                                Text(it)
                            } ?: Text("loading next")
                        }
                    }

                    PageType.NEXT -> {
                        if (currentIndex >= flow.itemCount - 1) {
                            Text("No item next")
                        } else {
                            flow[currentIndex + 1]?.let {
                                Text(it)
                            } ?: Text("loading next")
                        }
                    }

                    PageType.PREVIOUS -> {
                        if (currentIndex <= 0) {
                            Text("No item previous")
                        } else {

                            flow[currentIndex - 1]?.let {
                                Text(it)
                            } ?: Text("loading previous")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleReader() {
    val resolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val style = LocalTextStyle.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val source = remember(constraints) {
            TestPagingSource(
                defaultFontFamilyResolver = resolver,
                defaultDensity = density,
                defaultLayoutDirection = direction,
                constraints = constraints,
                style = style,
            )
        }
        val pager = remember(source) {
            Pager(
                PagingConfig(20),
                pagingSourceFactory = { source }
            )
        }
        val flow = pager.flow.collectAsLazyPagingItems()

        var currentIndex by remember { mutableIntStateOf(0) }
        var nextIndex by remember { mutableIntStateOf(-1) }
        val previousLayer = rememberGraphicsLayer()
        val nextLayer = rememberGraphicsLayer()
        val layer = rememberGraphicsLayer()
        var dragOffset = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures {
                    if (it.x > constraints.maxWidth / 2) {
                        currentIndex += 1
                    } else {
                        currentIndex -= 1
                    }
                }
            }.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        scope.launch { dragOffset.snapTo(offset.x) }
                    },
                    onDragEnd = {
                        // 根据 dragOffset.x 的值判断是否翻页
                        val nextIndex =
                            if (dragOffset.value < -constraints.maxWidth / 2) { // 向左滑动超过1/4宽度，翻到下一页
                                currentIndex += 1
                            } else if (dragOffset.value > constraints.maxWidth / 2) { // 向右滑动超过1/4宽度，翻到上一页
                                currentIndex -= 1
                            } else {
                                currentIndex
                            }
                        when (nextIndex) {
                            currentIndex + 1 -> {
                                previousLayer.alpha = 0f
                                previousLayer.translationX = -constraints.maxWidth.toFloat()
                                previousLayer.translationY = 0f
                                previousLayer.scaleX = 1f
                                previousLayer.scaleY = 1f
                                previousLayer.apply {}
                            }
                        }
                        if (nextIndex != currentIndex) {
                            scope.launch {
//                                dragOffset.snapTo()
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        scope.launch {
                            dragOffset.snapTo(dragAmount.x + dragOffset.value)
                        }
                    }
                )
            }
                .drawWithContent {
                    drawContent()
                    drawLayer(layer)
                    nextLayer.apply {
                        nextLayer.clip = true
                        nextLayer.setOutline(
                            CircleShape.createOutline(
                                this@drawWithContent.size,
                                layoutDirection,
                                density
                            )
                        )
                        drawLayer(nextLayer)
                    }

                }
        ) {
            if (currentIndex >= 1 && currentIndex < flow.itemCount - 1) {
                Box(
                    modifier = Modifier.drawWithContent {
                        previousLayer.record {
                            this@drawWithContent.drawContent()
                        }
                    }
                ) {
                    PageContent(flow[currentIndex - 1] ?: AnnotatedString("No text"))
                }
            }
            if (currentIndex >= 0 && currentIndex < flow.itemCount) {
                SelectionContainer {
                    Box(
                        modifier = Modifier.drawWithContent {
                            layer.record {
                                this@drawWithContent.drawContent()
                            }
                        }
                    ) {
                        PageContent(flow[currentIndex] ?: AnnotatedString("No text"))
                    }
                }
            }

            if (dragOffset.value != 0f && currentIndex > -1 && currentIndex < flow.itemCount - 1) {
                Box(
                    modifier = Modifier.drawWithContent {
                        nextLayer.record { this@drawWithContent.drawContent() }
                    }
                ) {
                    PageContent(
                        flow[currentIndex + 1] ?: AnnotatedString("No text"),
                        modifier = Modifier.background(Color.Red)
                    )
                }
            }
        }
//                LazyColumn(
//                    modifier = Modifier.fillMaxSize(),
//                ) {
//                    itemsIndexed(flow) { index, it ->
//                        Column {
//                            Text("PAGE: $index")
//                            PageContent(it ?: AnnotatedString("No text"))
//                        }
//
//                    }
//                }
    }
}

@Composable
fun PageContent(
    content: AnnotatedString,
    modifier: Modifier = Modifier
) {
    Text(content, modifier = modifier)
}

@Stable
class TestPagingSource(
    defaultFontFamilyResolver: FontFamily.Resolver,
    defaultDensity: Density,
    defaultLayoutDirection: LayoutDirection,
    cacheSize: Int = 8,
    private val constraints: Constraints,
    private val style: TextStyle = TextStyle.Default,
) : PagingSource<Int, AnnotatedString>() {

    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = defaultFontFamilyResolver,
        defaultDensity = defaultDensity,
        defaultLayoutDirection = defaultLayoutDirection,
        cacheSize = cacheSize
    )

    override fun getRefreshKey(state: PagingState<Int, AnnotatedString>): Int? {
        return null
    }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AnnotatedString> {
        val content = Res.readBytes("files/test.txt").decodeToString()
        val result = measurer.measure(
            text = AnnotatedString(content),
            style = style,
            overflow = TextOverflow.Visible,
            constraints = constraints
        )
        val data = buildList {
            var offset = 0f
            var isDone: Boolean
            var rangeStart = 0
            for (i in 0 until result.multiParagraph.lineCount) {
                val bottom = result.multiParagraph.getLineBottom(i)
                if ((bottom > offset + constraints.maxHeight && i > 0) || i == result.multiParagraph.lineCount - 1) {
                    add(
                        result.layoutInput.text.subSequence(
                            TextRange(
                                rangeStart,
                                result.multiParagraph.getLineEnd(i - 1)
                            )
                        )
                    )
                    rangeStart = result.multiParagraph.getLineStart(i)
                    offset = bottom
                }
            }
            println("isDone")
        }

        return LoadResult.Page(
            data = data,
            nextKey = null,
            prevKey = null
        )
    }
}