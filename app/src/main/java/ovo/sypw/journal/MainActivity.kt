package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import ovo.sypw.journal.components.AddItemFAB
import ovo.sypw.journal.components.MainView
import ovo.sypw.journal.components.TopBarView
import ovo.sypw.journal.data.SampleDataProvider
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ContentViews() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    ImageLoadUtils.init(context)

    // 使用SampleDataProvider获取示例数据
    val cardItems = SampleDataProvider.generateSampleData(context)

    // 获取最后一个示例数据的图片列表，用于FAB

    val listState = rememberLazyListState()
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
        floatingActionButton = { AddItemFAB(cardItems) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) { innerPadding ->
        MainView(innerPadding, listState, cardItems, markedSet)
    }
}
