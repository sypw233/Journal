package ovo.sypw.journal.presentation.components

import android.util.Log
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel


private const val TAG = "CustomLazyList"

/**
 * 自定义懒加载列表组件，使用ViewModel实现分页加载
 * 支持滑动删除功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomLazyCardList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    journalListViewModel: JournalListViewModel,
    onItemClick: (JournalData) -> Unit = {},
    onRefresh: () -> Unit = {},
    onScrollChange: (Boolean) -> Unit = {}
) {
    // 从ViewModel获取UI状态
    val uiState by journalListViewModel.uiState.collectAsState()
    
    // 编辑日记状态
    var showEditScreen by remember { mutableStateOf(false) }
    var editingJournal by remember { mutableStateOf<JournalData?>(null) }

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
            journalListViewModel.loadNextPage()
        }
    }

    // 监听列表滚动状态并通知ViewModel
    val isListScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }

    // 当滚动状态改变时通知ViewModel和外部
    LaunchedEffect(isListScrolling) {
        journalListViewModel.setScrolling(isListScrolling)
        onScrollChange(isListScrolling)
    }

    // 监听scrollToPosition状态变化，当需要滚动到特定位置时执行滚动
    LaunchedEffect(uiState.scrollToPosition) {
        uiState.scrollToPosition?.let { position ->
            // 使用动画滚动到指定位置
            listState.animateScrollToItem(position)

            // 滚动完成后重置scrollToPosition状态为null
            journalListViewModel.resetScrollPosition()
        }
    }

    // 显示编辑界面
    if (showEditScreen && editingJournal != null) {
        JournalBottomSheet(
            isVisible = showEditScreen,
            initialJournalData = editingJournal,
            onSave = { updatedJournal ->
                // 调用ViewModel的更新日记方法
                journalListViewModel.updateJournal(updatedJournal)
                showEditScreen = false
                editingJournal = null
            },
            onDismiss = {
                showEditScreen = false
                editingJournal = null
            }
        )
    }

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(uiState.journals.size) { index ->
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
                            journalListViewModel.deleteJournal(id)
                            // 使用带有撤销按钮的Snackbar
                            SnackBarUtils.showActionSnackBar(
                                message = "已删除 #${id}",
                                actionLabel = "撤销",
                                onActionPerformed = { journalListViewModel.undoDelete() },
                                onDismissed = { }
                            )
                        },
                        onEdit = {
                            // 处理右滑编辑，显示编辑界面
                            editingJournal = journalData
                            showEditScreen = true
                        },
                        onClick = { onItemClick(journalData) }
                    )
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
}

// 加载指示器组件
@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

