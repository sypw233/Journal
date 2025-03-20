package ovo.sypw.journal.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import ovo.sypw.journal.utils.SnackBarUtils

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
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = true

    // 确保内容填充正确处理顶部和底部的空间
    val contentPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 8.dp,
        bottom = innerPadding.calculateBottomPadding() + 8.dp,
        start = 8.dp,
        end = 8.dp
    )
//    LazyPagingCardList(
//        modifier = Modifier.fillMaxSize(),
//        contentPadding = contentPadding,
//        pager = cardItems
//    )
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState
    ) {

        items(
            count = cardItems.size,
//            key = { index -> cardItems[index].id }
        ) { index ->
            AnimatedVisibility(
                visibleState = transition,
                enter = fadeIn() +
                        slideInHorizontally(
                            initialOffsetX = { it * (index + 1) }
                        ),
                exit = fadeOut() +
                        slideOutHorizontally(
                            targetOffsetX = { it * (index + 1) }
                        )
            ) {
//                JournalCard(
//                    modifier = Modifier
//                        .animateItem()
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp, vertical = 12.dp),
//                    journalData = cardItems[index],
//                )
                SwipeCard(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxSize(),
//                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    journalData = cardItems[index],
                    onDismiss = {
                        if (index in cardItems.indices) {
                            SnackBarUtils.showSnackBar("delete index $index")
                            cardItems.removeAt(index)
                        } else {
                            SnackBarUtils.showSnackBar("delete index $index error")
                        }
                    },
                    onMark = {
                        if (index in cardItems.indices) {
                            if (index in markedSet) {
                                SnackBarUtils.showSnackBar("unmark index $index")
                                markedSet.remove(index)
                                cardItems[index].isMark = false
                            } else {
                                SnackBarUtils.showSnackBar("mark index $index")
                                markedSet.add(index)
                                cardItems[index].isMark = true
                            }
                        } else {
                            SnackBarUtils.showSnackBar("mark index $index error")
                        }
                    }
                )
            }
        }
    }
}