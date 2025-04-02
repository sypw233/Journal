package ovo.sypw.journal.components.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.ui.theme.animiation.VerticalOverscroll
import kotlin.math.roundToInt

@Composable
fun TestScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(0.dp),
    contentAlignment: Alignment = Alignment.Center
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
//        DraggableText()
//        DraggableTextLowLevel()
//        TestList()
//        TestLazyList()
        AIChatScreen()
    }
}

@Composable
private fun DraggableTextLowLevel() {
    Box(modifier = Modifier.fillMaxSize()) {
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .background(Color.Blue)
                .size(50.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        )
    }
}

@Composable
fun DraggableText() {
    var offsetX = remember { mutableFloatStateOf(0f) }
    Text(
        modifier = Modifier
            .offset { IntOffset(offsetX.floatValue.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX.floatValue += delta
                }
            ),
        text = "Drag me!"
    )
}


@Composable
fun TestList() {
    // 1. 使用自定义的弹性滚动状态
    val scrollState = rememberScrollState()
    val overscrollEffect = rememberOverscrollEffect()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // 使用自定义滚动状态
            .overscroll(overscrollEffect)
    ) {
        // 2. 添加一个可弹性拉伸的空格
        Spacer(modifier = Modifier.height(500.dp)) // 这个高度可以调整

        // 3. 原有内容
        for (i in 0..100) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "test$i",
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // 4. 底部也添加可弹性拉伸的空格
        Spacer(modifier = Modifier.height(500.dp))
    }
}


@Composable
fun TestLazyList() {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val overscrollEffect = remember(coroutineScope) { VerticalOverscroll(coroutineScope) }
    val numRange = (1..20).toList()
    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .overscroll(overscrollEffect)
            .scrollable(
                orientation = Orientation.Vertical,
                reverseDirection = true,
                state = listState,
                overscrollEffect = overscrollEffect
            )

    ) {

        items(
            count = numRange.size,
            key = { index ->
                index
            }
        )
        { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)

            ) {
                Text(
                    text = "test$index",
                    modifier = Modifier.padding(10.dp)
                )
            }
        }


    }
}