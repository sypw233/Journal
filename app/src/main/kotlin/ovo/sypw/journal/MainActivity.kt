package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import ovo.sypw.journal.components.AddItemFAB
import ovo.sypw.journal.components.CustomLazyCardList
import ovo.sypw.journal.components.TopBarView
import ovo.sypw.journal.data.JournalDataSource
import ovo.sypw.journal.ui.theme.JournalTheme
import ovo.sypw.journal.utils.ImageLoadUtils
import ovo.sypw.journal.utils.SnackbarUtils

@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JournalTheme {
                ContentViews()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContentViews() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    ImageLoadUtils.init(context)

    // 使用自定义数据源
    val dataSource = remember { JournalDataSource.getInstance() }

    // 配置列表状态
    val lazyListPrefetchStrategy = remember { LazyListPrefetchStrategy(10) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        prefetchStrategy = lazyListPrefetchStrategy,
    )
    var markedSet: MutableSet<Int> = mutableSetOf()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackbarUtils.initialize(snackbarHostState, coroutineScope)

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
        // 使用自定义懒加载列表组件
        CustomLazyCardList(
            contentPadding = innerPadding,
            listState = listState,
            markedSet = markedSet
        )
    }
}
