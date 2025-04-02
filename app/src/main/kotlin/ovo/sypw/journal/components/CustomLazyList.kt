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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.utils.SnackBarUtils
import ovo.sypw.journal.viewmodel.JournalListViewModel


const val TAG = "CustomLazyList"

/**
 * 自定义懒加载列表组件，使用ViewModel实现分页加载
 * 支持滑动删除功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomLazyCardList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    viewModel: JournalListViewModel
) {
    // 从ViewModel获取UI状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 添加防抖动延迟，确保滚动完全停止后再加载数据
    val shouldLoadMore by remember {
        derivedStateOf {
            if (uiState.isScrolling) {
                false // 滚动时不加载
            } else {
                // 最后一个ITEM可见
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null &&
                        lastVisibleItem.index >= uiState.journals.size - 4 &&
                        uiState.hasMoreData
            }
        }
    }

    // 当需要加载更多时，调用ViewModel的loadNextPage方法
    LaunchedEffect(shouldLoadMore) {
        Log.i(TAG, "CustomLazyCardList: LaunchedEffect")
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    // 监听列表滚动状态并通知ViewModel
    val isListScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }

    // 当滚动状态改变时通知ViewModel
    LaunchedEffect(isListScrolling) {
        viewModel.setScrolling(isListScrolling)
    }

    // 监听scrollToPosition状态变化，当需要滚动到特定位置时执行滚动
    LaunchedEffect(uiState.scrollToPosition) {
        uiState.scrollToPosition?.let { position ->
            // 使用动画滚动到指定位置
            listState.animateScrollToItem(position)

            // 滚动完成后重置scrollToPosition状态为null
            viewModel.resetScrollPosition()
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
            count = uiState.journals.size,
            // 使用稳定的唯一ID作为key，而不是依赖于索引位置
            key = { index ->
                // 确保即使在快速滑动时也能保持唯一性
                val item = uiState.journals[index]
                "journal_item_${item.id}"
            }) { index ->
            // 添加安全检查，确保索引有效
            if (index < uiState.journals.size) {
                val journalData = uiState.journals[index]
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
                        // 处理滑动删除，调用ViewModel的方法
                        val id = journalData.id
                        viewModel.deleteJournal(id)
                        // 使用带有撤销按钮的Snackbar
                        SnackBarUtils.showActionSnackBar(
                            message = "已删除 #${id}",
                            actionLabel = "撤销",
                            onActionPerformed = { viewModel.undoDelete() },
                            onDismissed = {}
                        )
                    }, onMark = {
                        // 处理标记，调用ViewModel的方法
                        val id = journalData.id
                        viewModel.toggleMarkJournal(id)
                        if (id in uiState.markedItems) {
                            SnackBarUtils.showSnackBar("Cancel mark #${id}")
                        } else {
                            SnackBarUtils.showSnackBar("Mark #${id}")
                        }
                    })
            }
        }
        item {
            Box(Modifier.height(100.dp))
        }

        // 如果还有更多数据且正在加载，显示加载指示器
        if (uiState.hasMoreData && uiState.isLoading) {
            item {
                LoadingPlaceholder()
            }
        }
    }
}

// 加载指示器组件
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

