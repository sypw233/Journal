package ovo.sypw.journal.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.R
import ovo.sypw.journal.data.model.JournalData

/** 可滑动卡片组件，支持左右滑动进行编辑和删除操作 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(
    journalData: JournalData,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enableScroll: Boolean = true
) {
    // 记住当前日记ID，确保操作的是正确的项
    val currentJournalId = remember { mutableStateOf(journalData.id) }
    
    // 更新当前ID，确保在重组时ID始终是最新的
    LaunchedEffect(journalData.id) {
        currentJournalId.value = journalData.id
    }

    var currentProgress = remember { mutableFloatStateOf(0f) }
    // 创建滑动状态
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                // 当滑动到END位置时触发删除
                if (value == SwipeToDismissBoxValue.EndToStart
                    && currentProgress.floatValue >= 0.4f && currentProgress.floatValue <= 1f
                ) {
                    // 确认当前ID与传入的日记ID一致后再调用onDismiss
                    if (currentJournalId.value == journalData.id) {
                        onDismiss()
                    }
                    false // 返回false防止状态更新为EndToStart
                    // 在这里直接调用onDismiss并返回false，防止状态变为EndToStart

                } else if (value == SwipeToDismissBoxValue.StartToEnd
                    && currentProgress.floatValue >= 0.4f && currentProgress.floatValue <= 1f
                ) {
                    // 确认当前ID与传入的日记ID一致后再调用onEdit
                    if (currentJournalId.value == journalData.id) {
                        onEdit()
                    }
                    false
                }
                false
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.3f }
        )
    ForUpdateData {
        currentProgress.floatValue = dismissState.progress
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enableScroll, // 允许从左向右滑动
        enableDismissFromEndToStart = enableScroll, // 允许从右向左滑动
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 25.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier
                        .offset((-150).dp)
                        .padding(start = 16.dp)
                        .size(30.dp),
                )
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier
                        .offset(150.dp)
                        .padding(end = 16.dp)
                        .size(30.dp),
                )
            }
        },
        modifier = modifier
            .animateContentSize()
            .fillMaxSize()

    ) {
        // 卡片内容
        JournalCard(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .clickable { onClick() },
            journalData = journalData
        )
    }
}

@Composable
private fun ForUpdateData(onUpdate: () -> Unit) {
    onUpdate()
}