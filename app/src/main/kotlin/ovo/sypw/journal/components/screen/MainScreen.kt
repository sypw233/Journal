package ovo.sypw.journal.components.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ovo.sypw.journal.components.AddItemFAB
import ovo.sypw.journal.components.BottomSheetContent
import ovo.sypw.journal.components.CustomLazyCardList
import ovo.sypw.journal.components.TopBarView
import ovo.sypw.journal.data.JournalDataSource
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.utils.ImageLoadUtils
import ovo.sypw.journal.utils.SnackBarUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
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
        topBar = {
            TopBarView(scrollBehavior, listState, markedSet)
        },
        floatingActionButton = { AddItemFAB() },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) { innerPadding ->
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                skipHiddenState = false
            )
        )
        val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
        val bottomSheetHeight by remember {
            derivedStateOf {
                if (listState.lastScrolledForward) 30.dp else 100.dp
            }
        }
        val dataSource = JournalDataSource.getInstance()
        val bottomSheetHeightAnimate by animateDpAsState(targetValue = bottomSheetHeight)
        BottomSheetScaffold(
            modifier = Modifier.animateContentSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = bottomSheetHeightAnimate,
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
                contentPadding = innerPadding,
                listState = listState,
                markedSet = markedSet,
                isScrolling = isScrolling
            )
        }
    }
}