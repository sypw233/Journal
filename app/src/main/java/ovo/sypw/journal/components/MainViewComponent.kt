package ovo.sypw.journal.components

import SnackbarUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 主视图组件，显示卡片列表
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView(
    innerPadding: PaddingValues,
    listState: LazyListState,
    cardItems: SnapshotStateList<Int>,
    markedSet: MutableSet<Int>
) {
    // 确保内容填充正确处理顶部和底部的空间
    val contentPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 8.dp,
        bottom = innerPadding.calculateBottomPadding() + 8.dp,
        start = 8.dp,
        end = 8.dp
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState
    ) {
        items(cardItems.size) { index ->
            SwipeCard(
                showText = "Card ${cardItems[index]}",
                onDismiss = {
                    if (index in cardItems.indices) {
                        SnackbarUtils.showSnackbar("delete index $index")
                        cardItems.removeAt(index)
                    } else {
                        SnackbarUtils.showSnackbar("delete index $index error")
                    }
                },
                onMark = {
                    if (index in cardItems.indices) {
                        if (index in markedSet) {
                            SnackbarUtils.showSnackbar("unmark index $index")
                            markedSet.remove(index)
                        } else {
                            SnackbarUtils.showSnackbar("mark index $index")
                            markedSet.add(index)
                        }
                    } else {
                        SnackbarUtils.showSnackbar("mark index $index error")
                    }
                }
            )
        }
    }
}