package ovo.sypw.journal.presentation.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.theme.VerticalOverscrollWithChange
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.common.utils.TopSnackbarHost
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.presentation.components.BottomSheetContent
import ovo.sypw.journal.presentation.components.CustomLazyCardList
import ovo.sypw.journal.presentation.components.JournalBottomSheet
import ovo.sypw.journal.presentation.components.LoginDialog
import ovo.sypw.journal.presentation.components.TopBarView
import ovo.sypw.journal.presentation.viewmodels.AuthViewModel
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
import ovo.sypw.journal.presentation.viewmodels.MainViewModel

private const val TAG = "MainScreen"

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel, 
    authViewModel: AuthViewModel = viewModel(),
    databaseManagementViewModel: DatabaseManagementViewModel = viewModel(),
    autoSyncManager: AutoSyncManager? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackBarUtils.initialize(snackbarHostState, coroutineScope)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 从ViewModel获取UI状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听认证状态
    val authState by authViewModel.authState.collectAsState()
    
    // 登录对话框状态
    var showLoginDialog by remember { mutableStateOf(false) }
    
    // 当认证状态变为Unauthenticated且有错误消息时，显示登录对话框
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            val unauthState = authState as AuthState.Unauthenticated
            val message = unauthState.message
            if (message != null && message.contains("登录已过期")) {
                SnackBarUtils.showSnackBar(message)
                showLoginDialog = true
            }
        }
    }
    
    // 显示登录对话框
    if (showLoginDialog) {
        LoginDialog(
            authViewModel = authViewModel,
            onDismiss = { showLoginDialog = false }
        )
    }

    // 配置列表状态
    val lazyListPrefetchStrategy = remember { LazyListPrefetchStrategy(10) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        prefetchStrategy = lazyListPrefetchStrategy,
    )

    // 根据UI状态渲染不同的界面
    when (val state = uiState) {
        is MainScreenState.Initial, is MainScreenState.Loading -> {
            // 可以显示加载中的UI
        }

        is MainScreenState.Error -> {
            // 显示错误信息
            SnackBarUtils.showSnackBar(state.error)
        }

        is MainScreenState.Success -> {
            // 显示主界面
            MainScreenContent(
                state = state,
                viewModel = viewModel,
                authViewModel = authViewModel,
                databaseManagementViewModel = databaseManagementViewModel,
                autoSyncManager = autoSyncManager,
                scrollBehavior = scrollBehavior,
                listState = listState,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                onShowLoginDialog = { showLoginDialog = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(
    state: MainScreenState.Success,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    databaseManagementViewModel: DatabaseManagementViewModel,
    autoSyncManager: AutoSyncManager?,
    scrollBehavior: TopAppBarScrollBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onShowLoginDialog: () -> Unit
) {
    // 获取JournalListViewModel的状态
    val journalListViewModel = viewModel.journalListViewModel
    val journalListState by journalListViewModel.uiState.collectAsState()
    
    // 获取标记的日记集合
    val markedItems = journalListState.markedItems
    
    // 底部表单展开标志
    val isBottomSheetExpanded = state.isBottomSheetExpanded
    
    // 显示添加日记对话框
    JournalBottomSheet(
        isVisible = isBottomSheetExpanded,
        initialJournalData = null, // 添加模式，初始数据为空
        onSave = { journalData ->
            // 创建新日记
            journalListViewModel.addJournal(journalData)
            // 关闭底部表单
            viewModel.setBottomSheetExpanded(false)
        },
        onDismiss = {
            viewModel.setBottomSheetExpanded(false)
        }
    )

    Scaffold(
        topBar = {
            TopBarView(
                scope = coroutineScope,
                scrollBehavior = scrollBehavior,
                markedSet = markedItems,
                authViewModel = authViewModel,
                journalListViewModel = journalListViewModel,
                databaseManagementViewModel = databaseManagementViewModel,
                autoSyncManager = autoSyncManager,
                onShowLoginDialog = onShowLoginDialog,
                onAddJournalClick = {
                    viewModel.setBottomSheetExpanded(true)
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { TopSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 使用现有的CustomLazyCardList的API
            CustomLazyCardList(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                journalListViewModel = journalListViewModel,
                onItemClick = { item -> 
                    SnackBarUtils.showSnackBar("查看日记: ${item.id}")
                },
                onRefresh = {
                    // 刷新列表
                    journalListViewModel.resetList()
                },
                // 监听滚动状态变化
                onScrollChange = { isScrolling ->
                    viewModel.setScrolling(isScrolling)
                }
            )
        }
    }
}