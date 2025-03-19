package ovo.sypw.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import ovo.sypw.journal.utils.SnackbarUtils

/**
 * 自定义懒加载列表组件，使用CustomJournalDataSource实现分页加载
 * 支持滑动删除功能
 */
@Composable
fun CustomLazyCardList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    markedSet: MutableSet<Int>,
    onLoadMore: () -> Unit = {}
) {
    // 获取数据源实例
    val dataSource = remember { JournalDataSource.getInstance() }

    // 监听滚动状态
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    // 监听滚动到底部事件，触发加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
                    lastVisibleItem.index >= dataSource.loadedItems.size - 3 &&
                    dataSource.hasMoreData() &&
                    !isScrolling // 只有在不滚动时才加载更多
        }
    }

    // 当需要加载更多时，调用loadNextPage
    LaunchedEffect(shouldLoadMore) {
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

    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState
    ) {
        items(
            count = dataSource.loadedItems.size,
            key = { index -> dataSource.loadedItems[index].id }
        ) { index ->
            val journalData = dataSource.loadedItems[index]
            SwipeCard(
                modifier = Modifier
                    .animateItem(

                    )
                    .fillMaxSize(),
                journalData = journalData,
                onDismiss = {
                    // 处理滑动删除
                    val id = journalData.id
                    val removed = dataSource.removeItem(id)
                    if (removed) {
                        SnackbarUtils.showSnackbar("删除了条目 #${id}")
                    } else {
                        SnackbarUtils.showSnackbar("删除条目 #${id} 失败")
                    }
                },
                onMark = {
                    // 处理标记
                    val id = journalData.id
                    if (id in markedSet) {
                        SnackbarUtils.showSnackbar("取消标记条目 #${id}")
                        markedSet.remove(id)
                        dataSource.updateItem(id) { item ->
                            item.copy(isMark = false)
                        }
                    } else {
                        SnackbarUtils.showSnackbar("标记条目 #${id}")
                        markedSet.add(id)
                        dataSource.updateItem(id) { item ->
                            item.copy(isMark = true)
                        }
                    }
                }
            )
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
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator()
    }
}