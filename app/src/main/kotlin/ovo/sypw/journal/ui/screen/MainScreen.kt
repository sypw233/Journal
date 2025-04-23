package ovo.sypw.journal.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovo.sypw.journal.ui.components.BottomSheetContent
import ovo.sypw.journal.ui.components.CustomLazyCardList
import ovo.sypw.journal.ui.components.TopBarView
import ovo.sypw.journal.ui.main.MainScreenState
import ovo.sypw.journal.ui.theme.animiation.VerticalOverscrollWithChange
import ovo.sypw.journal.utils.SnackBarUtils
import ovo.sypw.journal.utils.TopSnackbarHost
import ovo.sypw.journal.viewmodel.AuthViewModel
import ovo.sypw.journal.viewmodel.MainViewModel

private const val TAG = "MainScreen"

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel, authViewModel: AuthViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackBarUtils.initialize(snackbarHostState, coroutineScope)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    // 从ViewModel获取UI状态
    val uiState by viewModel.uiState.collectAsState()

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
                scrollBehavior = scrollBehavior,
                listState = listState,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
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
    scrollBehavior: TopAppBarScrollBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    // 获取JournalListViewModel的状态
    val journalListState by viewModel.journalListViewModel.uiState.collectAsState()
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBarView(
                scrollBehavior,
                listState,
                journalListState.markedItems,
                authViewModel
            )
        },
        snackbarHost = {
            TopSnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                skipHiddenState = true
            )
        )
        // 使用ViewModel中的状态
        var bottomSheetHeight = remember { derivedStateOf { Animatable(state.bottomSheetHeight) } }
        val isSheetExpanded = state.isBottomSheetExpanded

        // 当滚动状态改变时通知两个ViewModel
        val isScrollingState by remember { derivedStateOf { listState.isScrollInProgress } }
        if (isScrollingState != state.isScrolling) {
            viewModel.setScrolling(isScrollingState)
            viewModel.journalListViewModel.setScrolling(isScrollingState)
        }

        val overscrollEffect = remember(coroutineScope) {
            VerticalOverscrollWithChange(
                scope = coroutineScope,
                onChangeScroll = { scrollOffset ->
                    Log.d(TAG, "MainScreen: ${bottomSheetHeight.value.value}")
                    if (scrollOffset * 0.8f + bottomSheetHeight.value.value >= 30) {
                        scope.launch {
                            val newHeight = scrollOffset * 0.8f + bottomSheetHeight.value.value
                            bottomSheetHeight.value.animateTo(newHeight)
                            viewModel.setBottomSheetHeight(newHeight)
                        }
                    }
                },
                onChangeFling = { remaining ->
                    if (bottomSheetHeight.value.value >= 150f) {
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                            bottomSheetHeight.value.animateTo(30f)
                            viewModel.setBottomSheetHeight(30f)
                            viewModel.setBottomSheetExpanded(true)
                        }
                    } else {
                        scope.launch {
                            bottomSheetHeight.value.animateTo(
                                targetValue = 30f,  // 目标值归零
                                initialVelocity = remaining.y,  // 初始速度为剩余速度
                                animationSpec = tween(
                                    durationMillis = 500,  // 动画时长500ms
                                    easing = EaseOutQuad   // 使用缓出曲线
                                )
                            )
                            viewModel.setBottomSheetHeight(30f)
                        }
                    }
                },
            )
        }


        BottomSheetScaffold(
            modifier = Modifier.animateContentSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = bottomSheetHeight.value.value.dp,
            sheetShadowElevation = 10.dp,
            sheetContent = {
                BottomSheetContent(
                    onDismiss = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                            listState.animateScrollToItem(0)
                            viewModel.setBottomSheetExpanded(false)
                        }
                    },
                    onSave = { newJournal ->
                        // 使用ViewModel添加日记
                        viewModel.addJournal(newJournal)
                    }
                )
            }) {
            CustomLazyCardList(
                modifier = Modifier
                    .fillMaxSize()
                    .overscroll(overscrollEffect)
                    .scrollable(
                        orientation = Orientation.Vertical,
                        reverseDirection = true,
                        state = listState,
                        overscrollEffect = overscrollEffect
                    ),
                contentPadding = innerPadding,
                listState = listState,
                viewModel = viewModel.journalListViewModel
            )
            if (isSheetExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize()
                        .pointerInput(Unit) {
                            // 拦截所有点击事件
                            scope.launch {
                                scaffoldState.bottomSheetState.partialExpand()
                                viewModel.setBottomSheetExpanded(false)
                            }
                        }
                )
            }
        }
    }
}