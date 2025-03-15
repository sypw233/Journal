package ovo.sypw.journal.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    cardItems: SnapshotStateList<JournalData>,
    markedSet: MutableSet<Int>
) {
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = true
    val scrollState = rememberScrollState()
    // 确保内容填充正确处理顶部和底部的空间
    val contentPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 8.dp,
        bottom = innerPadding.calculateBottomPadding() + 8.dp,
        start = 8.dp,
        end = 8.dp
    )
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                start = 8.dp,
                end = 8.dp
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // 遍历所有卡片项目
        for (index in cardItems.indices) {
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
                JournalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    journalData = cardItems[index],
                )
//                SwipeCard(
//                    journalData = cardItems[index],
//                    onDismiss = {
//                        if (index in cardItems.indices) {
//                            SnackbarUtils.showSnackbar("delete index $index")
//                            cardItems.removeAt(index)
//                        } else {
//                            SnackbarUtils.showSnackbar("delete index $index error")
//                        }
//                    },
//                    onMark = {
//                        if (index in cardItems.indices) {
//                            if (index in markedSet) {
//                                SnackbarUtils.showSnackbar("unmark index $index")
//                                markedSet.remove(index)
//                                cardItems[index].isMark = false
//                            } else {
//                                SnackbarUtils.showSnackbar("mark index $index")
//                                markedSet.add(index)
//                                cardItems[index].isMark = true
//                            }
//                        } else {
//                            SnackbarUtils.showSnackbar("mark index $index error")
//                        }
//                    }
//                )
            }
        }
    }
}