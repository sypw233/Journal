package ovo.sypw.journal.presentation.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel
import ovo.sypw.journal.presentation.viewmodels.SentimentViewModel

private const val TAG = "CustomLazyList"

/**
 * 自定义懒加载列表组件，使用ViewModel实现分页加载
 * 支持滑动删除功能
 */
@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomLazyCardList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    journalListViewModel: JournalListViewModel,
    sentimentViewModel: SentimentViewModel? = null,
    onItemClick: (JournalData) -> Unit = {},
    onRefresh: () -> Unit = {},
    onScrollChange: (Boolean) -> Unit = {},
    onViewSentimentReport: () -> Unit = {}
) {
    // 从ViewModel获取UI状态
    val uiState by journalListViewModel.uiState.collectAsState()
    
    // 编辑日记状态
    var showEditScreen by remember { mutableStateOf(false) }
    var editingJournal by remember { mutableStateOf<JournalData?>(null) }
    
    // 情感分析对话框状态
    var showSentimentDialog by remember { mutableStateOf(false) }
    var selectedJournalForSentiment by remember { mutableStateOf<JournalData?>(null) }

    // 根据是否处于搜索模式选择要显示的日记列表
    val journalList = if (uiState.isSearchMode) uiState.searchResults else uiState.journals

    // 添加防抖动延迟，确保滚动完全停止后再加载数据
    val shouldLoadMore by remember {
        derivedStateOf {
            if (uiState.isScrolling || uiState.isSearchMode) {
                false // 滚动时或搜索模式下不加载
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
    
    // 处理删除确认对话框
    if (journalListViewModel.showDeleteConfirmDialog.value) {
        val journalId = journalListViewModel.journalToDelete.value
        val journalToDelete = journalList.find { it.id == journalId }
        
        AlertDialog(
            onDismissRequest = { journalListViewModel.cancelDelete() },
            title = { Text("删除确认") },
            text = { 
                Text(
                    "确定要删除「${journalToDelete?.text?.take(20) ?: ""}${if ((journalToDelete?.text?.length ?: 0) > 20) "..." else ""}」吗？此操作无法撤销。",
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            confirmButton = {
                Button(
                    onClick = { journalListViewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { journalListViewModel.cancelDelete() }) {
                    Text("取消")
                }
            }
        )
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

    // 显示情感分析对话框
    if (showSentimentDialog && selectedJournalForSentiment != null && sentimentViewModel != null) {
        SentimentAnalysisDialog(
            journal = selectedJournalForSentiment!!,
            viewModel = sentimentViewModel,
            onDismiss = {
                showSentimentDialog = false
                selectedJournalForSentiment = null
            },
            onViewReport = {
                showSentimentDialog = false
                selectedJournalForSentiment = null
                onViewSentimentReport()
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
            items(
                count = journalList.size,
                key = { index -> journalList[index].id } // 使用日记ID作为key
            ) { index ->
                if (index < journalList.size) {
                    val journalData = journalList[index]
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
                            Log.d(TAG, "CustomLazyCardList:  尝试删除$id")
                        },
                        onEdit = {
                            // 处理滑动编辑，显示编辑界面
                            editingJournal = journalData
                            showEditScreen = true
                        },
                        onClick = {
                            // 处理点击事件
                            onItemClick(journalData)
                        },
                        onLongClick = {
                            // 处理长按事件，显示情感分析对话框
                            if (sentimentViewModel != null) {
                                selectedJournalForSentiment = journalData
                                showSentimentDialog = true
                            }
                        }
                    )
                }
            }
            item {
                Box(Modifier.height(100.dp))
            }

            // 如果还有更多数据且正在加载，显示加载指示器
            if (uiState.hasMoreData && uiState.isLoading && !uiState.isSearchMode) {
                item {
                    LoadingPlaceholder()
                }
            }
            
            // 如果正在搜索，显示搜索加载指示器
            if (uiState.isSearching) {
                item {
                    LoadingPlaceholder()
                }
            }
            
            // 如果是搜索模式但没有结果，显示无结果提示
            if (uiState.isSearchMode && !uiState.isSearching && uiState.searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "没有找到匹配的日记",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

