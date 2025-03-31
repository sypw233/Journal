package ovo.sypw.journal.components

import android.util.Log
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.data.JournalDataSource
import ovo.sypw.journal.utils.SnackBarUtils


const val TAG = "JournalDataSource"
var itemKeyCount = 0

/**
 * 自定义懒加载列表组件，使用CustomJournalDataSource实现分页加载
 * 支持滑动删除功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomLazyCardList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
//    overscrollEffect: VerticalOverscroll,
    markedSet: MutableSet<Int>,
    onLoadMore: () -> Unit = {},
    isScrolling: Boolean
) {
    // 获取数据源实例
    val dataSource = remember { JournalDataSource.getInstance() }
//    dataSource.initialize()
    // 监听滚动状态
//    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    // 添加防抖动延迟，确保滚动完全停止后再加载数据
    val shouldLoadMore by remember {
        derivedStateOf {
            if (isScrolling) {
                false // 滚动时不加载
            } else {
//                最后一个ITEM可见
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null &&
                        lastVisibleItem.index >= dataSource.loadedItems.size - 4 &&
                        dataSource.hasMoreData()
            }
        }
    }

    // 当需要加载更多时，调用loadNextPage
    LaunchedEffect(shouldLoadMore) {
        Log.i(TAG, "CustomLazyCardList: LaunchedEffect")
        if (shouldLoadMore && !dataSource.isLoading()) {
            val success = dataSource.loadNextPage()

            if (success) {
                onLoadMore()
            }
        }
    }

    // 初始化数据源
    LaunchedEffect(Unit) {
        if (dataSource.loadedItems.isEmpty()) {
            dataSource.initialize()
        }
    }

    val isListScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }
    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
        userScrollEnabled = false
    ) {

        items(

            count = dataSource.loadedItems.size,
            // 使用稳定的唯一ID作为key，而不是依赖于索引位置
            key = { index ->
                // 确保即使在快速滑动时也能保持唯一性
                val item = dataSource.loadedItems[index]
                "journal_item_${item.id}"
            }) { index ->
            // 添加安全检查，确保索引有效
            if (index < dataSource.loadedItems.size) {
                val journalData = dataSource.loadedItems[index]
//                Log.d(
//                    "JOURNAL_DEBUG",
//                    "CustomLazyCardList: current item index: $index id: ${journalData.id}"
//                )
                SwipeCard(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = spring(
                                dampingRatio = 0.5f, stiffness = 100f
                            ), fadeOutSpec = spring(
                                dampingRatio = 0.5f,
                            )
                        )
                        .fillMaxSize(),
                    enableScroll = !isListScrolling,
                    journalData = journalData,
                    onDismiss = {
                        // 处理滑动删除
                        val id = journalData.id
                        val waitToDeleteData = journalData
                        val removed = dataSource.removeItem(id)
                        if (removed) {
                            SnackBarUtils.showActionSnackBar(
                                message = "删除条目 #${id}",
                                actionLabel = "撤销",
                                onActionPerformed = {
                                    // 撤销删除
                                    dataSource.addItem(waitToDeleteData, index)
                                },
                                onDismissed = {
                                    dataSource.removeItem(id)
                                })
                        } else {
                            SnackBarUtils.showSnackBar("删除条目 #${id} 失败")
                        }
                    }, onMark = {
                        // 处理标记
                        val id = journalData.id
                        if (id in markedSet) {
                            SnackBarUtils.showSnackBar("取消标记条目 #${id}")
                            markedSet.remove(id)
                            dataSource.updateItem(id) { item ->
                                item.copy(isMark = false)
                            }
                        } else {
                            SnackBarUtils.showSnackBar("标记条目 #${id}")
                            markedSet.add(id)
                            dataSource.updateItem(id) { item ->
                                item.copy(isMark = true)
                            }
                        }
                    })
            }
        }
        item {
            Box(Modifier.height(100.dp))
        }

        // 如果还有更多数据，显示加载指示器
        // 在滚动时或加载中时都显示加载指示器
        if (dataSource.hasMoreData() && (isScrolling || dataSource.isLoading())) {
            item {
                LoadingPlaceholder()
            }
        }

    }
}

// 如果LoadingPlaceholder组件不存在，添加实现
@Composable
fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator()
    }
}

