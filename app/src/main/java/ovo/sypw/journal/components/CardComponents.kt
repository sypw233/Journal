package ovo.sypw.journal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 普通卡片组件
 */
@Composable
fun ElevatedCard(showText: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(240.dp)
    ) {
        Text(
            text = showText,
            modifier = Modifier.padding(15.dp)
        )
    }
}

/**
 * 可滑动卡片组件，支持左右滑动进行标记和删除操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(
    showText: String,
    onDismiss: () -> Unit,
    onMark: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 创建滑动状态
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // 当滑动到END位置时触发删除
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // 在这里直接调用onDismiss并返回false，防止状态变为EndToStart
                onDismiss()
                false // 返回false防止状态更新为EndToStart
            } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                onMark()
                false
            } else {
                false // 其他状态也不更新
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.6f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,  // 允许从左向右滑动
        enableDismissFromEndToStart = true,   // 允许从右向左滑动
        backgroundContent = {
            // 滑动时显示的背景内容（删除图标和红色背景）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "delete",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(30.dp),
                )
            }
        },
        modifier = modifier
    ) {
        // 卡片内容
        ElevatedCard(showText)
    }
}