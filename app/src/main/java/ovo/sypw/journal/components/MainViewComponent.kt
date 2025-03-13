package ovo.sypw.journal.components

import SnackbarUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.model.JournalData

/**
 * 主视图组件，显示卡片列表
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView(
    innerPadding: PaddingValues,
    listState: LazyListState,
    cardItems: SnapshotStateList<JournalData>,
    markedSet: MutableSet<Int>
) {
    val transition = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    transition.targetState = true

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
            androidx.compose.animation.AnimatedVisibility(
                visibleState = transition,
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { it * (index + 1) }
                        ),
                exit = androidx.compose.animation.fadeOut() +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { it * (index + 1) }
                        )
            ) {
                SwipeCard(
                    journalData = cardItems[index],
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
                                cardItems[index].isMark = false
                            } else {
                                SnackbarUtils.showSnackbar("mark index $index")
                                markedSet.add(index)
                                cardItems[index].isMark = true
                            }
                        } else {
                            SnackbarUtils.showSnackbar("mark index $index error")
                        }
                    }
                )
            }
        }
    }
}