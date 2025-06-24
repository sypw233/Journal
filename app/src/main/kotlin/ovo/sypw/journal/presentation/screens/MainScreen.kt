package ovo.sypw.journal.presentation.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.common.utils.TopSnackbarHost
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.presentation.components.CustomLazyCardList
import ovo.sypw.journal.presentation.components.JournalBottomSheet
import ovo.sypw.journal.presentation.components.LoginDialog
import ovo.sypw.journal.presentation.components.TopBarView
import ovo.sypw.journal.presentation.components.search.SearchBar
import ovo.sypw.journal.presentation.viewmodels.AuthViewModel
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel
import ovo.sypw.journal.presentation.viewmodels.MainViewModel

private const val TAG = "MainScreen"

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    journalListViewModel: JournalListViewModel = hiltViewModel(),
    databaseManagementViewModel: DatabaseManagementViewModel = viewModel(),
    autoSyncManager: AutoSyncManager? = null,
    navController: androidx.navigation.NavController? = null
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
                journalListViewModel = journalListViewModel,
                authViewModel = authViewModel,
                databaseManagementViewModel = databaseManagementViewModel,
                autoSyncManager = autoSyncManager,
                scrollBehavior = scrollBehavior,
                listState = listState,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                onShowLoginDialog = { showLoginDialog = true },
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(
    state: MainScreenState.Success,
    viewModel: MainViewModel,
    journalListViewModel: JournalListViewModel,
    authViewModel: AuthViewModel,
    databaseManagementViewModel: DatabaseManagementViewModel,
    autoSyncManager: AutoSyncManager?,
    scrollBehavior: TopAppBarScrollBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onShowLoginDialog: () -> Unit,
    navController: androidx.navigation.NavController? = null
) {
    // 获取JournalListViewModel的状态
    val journalListState by journalListViewModel.uiState.collectAsState()

    // 获取标记的日记集合
    journalListState.markedItems

    // 底部表单展开标志
    val isBottomSheetExpanded = state.isBottomSheetExpanded

    // 搜索模式标志
    var showSearchBar by remember { mutableStateOf(false) }

    // 用于记录搜索按钮位置的引用
    val searchButtonPositionRef =
        remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val searchBarIconPositionRef =
        remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // 用于控制搜索图标的动画
    remember { Animatable(0f) }
    val alphaAnimation = remember { Animatable(1f) }

    // 当搜索栏状态变化时，触发动画
    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            // 进入搜索模式：图标淡出
            alphaAnimation.animateTo(0f, animationSpec = tween(300))
        } else {
            // 退出搜索模式：图标淡入
            alphaAnimation.animateTo(1f, animationSpec = tween(300))
        }
    }

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
            // 始终显示顶部栏
            TopBarView(
                scrollBehavior = scrollBehavior,
                authViewModel = authViewModel,
                autoSyncManager = autoSyncManager,
                onShowLoginDialog = onShowLoginDialog,
                onSearchClick = {
                    showSearchBar = !showSearchBar
                    // 如果关闭搜索栏，同时清除搜索结果
                    if (!showSearchBar) {
                        journalListViewModel.resetSearchMode()
                    }
                },
                onOpenSettings = {
                    // 导航到设置界面
                    navController?.navigate("settings")
                },
                onOpenDatabaseManagement = {
                    navController?.navigate("database_management")
                },
                onOpenSentimentAnalysis = {
                    navController?.navigate("sentiment_report")
                },
                onOpenAIChat = {
                    navController?.navigate("ai_chat")
                },
                searchButtonAlpha = alphaAnimation.value,
                onSearchButtonPosition = { coordinates ->
                    searchButtonPositionRef.value = coordinates
                }
            )
        },
        floatingActionButton = {
            // 搜索模式下不显示悬浮按钮
            if (!showSearchBar && !journalListState.isSearchMode) {
                FloatingActionButton(
                    onClick = {
                        viewModel.setBottomSheetExpanded(true)
                    },
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加日记"
                    )
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { TopSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // 用于检测点击搜索区域外的部分
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                // 点击空白区域退出搜索模式
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = showSearchBar || journalListState.isSearchMode
                ) {
                    if (showSearchBar || journalListState.isSearchMode) {
                        showSearchBar = false
                        journalListViewModel.resetSearchMode()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 搜索栏显示在顶部栏下方
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSearchBar || journalListState.isSearchMode,
                    enter = androidx.compose.animation.expandVertically() +
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() +
                            androidx.compose.animation.fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // 阻止点击事件冒泡，确保点击搜索区域不会关闭搜索框
                            }
                    ) {
                        SearchBar(
                            journalListViewModel = journalListViewModel,
                            showBackButton = false,
                            onSearchIconPosition = { coordinates ->
                                searchBarIconPositionRef.value = coordinates
                            }
                        )
                    }
                }

                // 日记列表
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // 使用现有的CustomLazyCardList的API
                    CustomLazyCardList(
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        journalListViewModel = journalListViewModel,
                        sentimentViewModel = hiltViewModel(),
                        onItemClick = { item ->
                        },
                        onRefresh = {
                            // 刷新列表
                            journalListViewModel.resetList()
                        },
                        // 监听滚动状态变化
                        onScrollChange = { isScrolling ->
                            viewModel.setScrolling(isScrolling)
                        },
                        onViewSentimentReport = {
                            // 导航到情感报告界面
                            navController?.navigate("sentiment_report")
                        }
                    )
                }
            }
        }
    }
}