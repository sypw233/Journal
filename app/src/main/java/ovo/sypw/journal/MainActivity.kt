package ovo.sypw.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovo.sypw.journal.ui.theme.JournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JournalTheme {
                ContentViews()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ContentViews() {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val cardItems = remember { mutableStateListOf<Int>().apply { addAll(1..10) } }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBarView(scrollBehavior, listState)
        },
        floatingActionButton = { AddItemFAB(cardItems) }

    ) { innerPadding ->
        MainView(innerPadding, listState, cardItems)
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView(
    innerPadding: PaddingValues,
    listState: LazyListState,
    cardItems: SnapshotStateList<Int>
) {
//    AnimateItemSample(
//        modifier= Modifier.fillMaxSize(),
//        contentPadding = innerPadding
//    )
    LazyColumn(
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState
    ) {
        items(cardItems.size) { index ->
//            ElevatedCard(index.toString())
            SwipeCard(
                cardItems[index].toString(),
                onDismiss = {
                    println("delete index $index")
                    cardItems.removeAt(index)
                },
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    fadeOutSpec = tween(
                        durationMillis = 300,
                        easing = LinearEasing
                    )

                )
            )
        }
    }

}

@Composable
fun AddItemFAB(cardItems: SnapshotStateList<Int>) {
    Column {
        FloatingActionButton(
            onClick = {
                cardItems.add(cardItems.size)
            },
            shape = CircleShape,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Icon(Icons.Filled.Add, "Floating action button.")
        }
        FloatingActionButton(
            onClick = {
                if (cardItems.isNotEmpty()) {
                    cardItems.removeAt(cardItems.lastIndex)
                }
            },
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Close, "Floating action button.")
        }
    }

}

@Composable
fun ElevatedCard(showText: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp, vertical = 10.dp)
            .height(240.dp)
    ) {
        Text(
            text = "Card$showText",
            modifier = Modifier.padding(15.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(showText: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // 创建滑动状态
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // 当滑动到END位置时触发删除
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // 在这里直接调用onDismiss并返回false，防止状态变为EndToStart
                onDismiss()
                false // 返回false防止状态更新为EndToStart
            } else {
                false // 其他状态也不更新
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.6f }
    )

    // 移除这段代码，因为我们已经在confirmValueChange中处理了删除逻辑
    // if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
    //     onDismiss()
    // }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,  // 允许从左向右滑动
        enableDismissFromEndToStart = true,   // 允许从右向左滑动
        backgroundContent = {
            // 滑动时显示的背景内容（删除图标和红色背景）
//            val color = Color.Red.copy(alpha = 0.8f)
            // 右侧显示删除图标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp, vertical = 10.dp),
//                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "delete",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(30.dp),
                )
            }
        },
        modifier = modifier
    ) {
        // 卡片内容
        ElevatedCard(showText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarView(scrollBehavior: TopAppBarScrollBehavior, listState: LazyListState) {
    val titleFontSize =
        remember(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            val maxFontSize = 30.sp
            val minFontSize = 25.sp
            val scrollThreshold = 150f // 滚动阈值
            val scrollOffset = if (listState.firstVisibleItemIndex > 0) {
                scrollThreshold
            } else {
                listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, scrollThreshold)
            }
            val progress = (scrollOffset / scrollThreshold).coerceIn(0f, 1f)
            androidx.compose.ui.unit.lerp(minFontSize, maxFontSize, progress)
        }
    MediumTopAppBar(
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        title = {
            Text(
                modifier = Modifier.padding(bottom = 5.dp),
                text = "Journal",
                fontSize = titleFontSize,
                maxLines = 1,
            )
        }, actions = {
            IconButton(onClick = {
//                TODO
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.filter_alt_icon_24),
                    contentDescription = "Filter Icon"
                )
            }
            IconButton(onClick = {
//                TODO
            }) {
                Icon(
                    imageVector = Icons.Filled.Menu, contentDescription = "Menu Icon"
                )
            }

        }, scrollBehavior = scrollBehavior
    )
}
