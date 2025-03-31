package ovo.sypw.journal.components.screen

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ovo.sypw.journal.components.BottomSheetContent
import ovo.sypw.journal.components.CustomLazyCardList
import ovo.sypw.journal.components.TopBarView
import ovo.sypw.journal.data.JournalDataSource
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.ui.theme.animiation.VerticalOverscrollWithChange
import ovo.sypw.journal.utils.ImageLoadUtils
import ovo.sypw.journal.utils.SnackBarUtils

private const val TAG = "MainScreen"

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    ImageLoadUtils.init(context)
    val journalPreferences = remember { JournalPreferences(context) }

    // 配置列表状态
    val lazyListPrefetchStrategy = remember { LazyListPrefetchStrategy(10) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        prefetchStrategy = lazyListPrefetchStrategy,
    )
    var markedSet: MutableSet<Int> = mutableSetOf()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackBarUtils.initialize(snackbarHostState, coroutineScope)
    JournalDataSource.initDatabase(LocalContext.current)

    if (journalPreferences.isFirstLaunch()) {
        JournalDataSource.firstLaunchDatabaseInit()
    }


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
//        floatingActionButton = { AddItemFAB() },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                skipHiddenState = true
            )
        )
        val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
//        val bottomSheetHeight by remember {
//            derivedStateOf {
//                if (listState.lastScrolledForward) 30.dp else 100.dp
//            }
//        }
//        val bottomSheetHeightAnimate by animateDpAsState(targetValue = bottomSheetHeight)
        var bottomSheetHeight = remember { derivedStateOf { Animatable(30f) } }
        val dataSource = JournalDataSource.getInstance()
        val isSheetExpanded by remember {
            derivedStateOf { scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded }
        }
        val overscrollEffect = remember(coroutineScope) {
            VerticalOverscrollWithChange(
                scope = coroutineScope,
                onChangeScroll = { scrollOffset ->

                    Log.d(TAG, "MainScreen: ${bottomSheetHeight.value.value}")
                    if (bottomSheetHeight.value.value >= 30f) {
                        scope.launch {
                            bottomSheetHeight.value.animateTo(scrollOffset * 0.8f + bottomSheetHeight.value.value)
                        }
                    }


                },
                onChangeFling = { remaining ->

                    if (bottomSheetHeight.value.value >= 150f) {
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                            bottomSheetHeight.value.animateTo(30f)
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
                        }

                    }
                },

                )
        }


        BottomSheetScaffold(
            modifier = Modifier.animateContentSize(),
            scaffoldState = scaffoldState,
            topBar = { TopBarView(scrollBehavior, listState, markedSet) },

            sheetPeekHeight = bottomSheetHeight.value.value.dp,
            sheetShadowElevation = 10.dp,
            sheetContent = {
                BottomSheetContent(
                    onDismiss = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                            listState.animateScrollToItem(0)
                        }
                    },
                    onSave = { newJournal ->
                        dataSource.addItem(newJournal)
                        SnackBarUtils.showSnackBar("add item success ${newJournal.id}")
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
                markedSet = markedSet,
                isScrolling = isScrolling,
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
                            }
                        }
                )
            }

        }
    }


//    }
}