package ovo.sypw.journal.presentation.components

import android.R.attr.repeatMode
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 同步状态按钮组件
 * 显示当前同步状态并提供点击同步功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusButton(
    autoSyncManager: AutoSyncManager,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val syncState by autoSyncManager.syncState.collectAsState()
    val isSyncing by autoSyncManager.isSyncing.collectAsState()
    val lastSyncTime by autoSyncManager.lastSyncTime.collectAsState()
    val syncError by autoSyncManager.syncError.collectAsState()
    val autoSyncEnabled by autoSyncManager.autoSyncEnabled.collectAsState()
    
    // 根据同步状态确定按钮颜色
    val buttonColor = when (syncState) {
        SyncState.SYNCING -> MaterialTheme.colorScheme.primary
        SyncState.SUCCESS -> MaterialTheme.colorScheme.tertiary
        SyncState.FAILED -> MaterialTheme.colorScheme.error
        SyncState.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
        SyncState.IDLE -> MaterialTheme.colorScheme.secondary
    }
    
    // 同步图标旋转动画
    val rotation by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        label = "SyncRotation"
    )
    
    // 获取状态图标
    val icon = getSyncIcon(syncState)
    
    // 获取状态文本
    val statusText = getSyncStatusText(syncState, lastSyncTime)
    
    // tooltip显示的错误信息
    val tooltipText = when {
        syncState == SyncState.FAILED && !syncError.isNullOrEmpty() -> syncError
        syncState == SyncState.SUCCESS -> "上次同步时间: ${formatTime(lastSyncTime)}"
        syncState == SyncState.PAUSED -> "自动同步已暂停，点击启用"
        else -> "点击开始同步"
    }
    
    // 获取tooltip状态
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(
                        text = tooltipText.toString(),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            },
            state = tooltipState
        ) {
            Button(
                onClick = {
                    // 如果自动同步已禁用，先启用它
                    if (!autoSyncEnabled) {
                        autoSyncManager.setAutoSyncEnabled(true)
                    } else {
                        // 否则触发立即同步
                        autoSyncManager.scheduleSyncNow()
                    }
                    // 显示/隐藏tooltip
                    scope.launch {
                        if (tooltipState.isVisible) {
                            tooltipState.dismiss()
                        } else {
                            tooltipState.show()
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "同步状态",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isSyncing) rotation else 0f)
                    )
                    
                    if (showText) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 根据同步状态获取图标
 */
private fun getSyncIcon(syncState: SyncState): ImageVector {
    return when (syncState) {
        SyncState.SYNCING -> Icons.Filled.Refresh
        SyncState.SUCCESS -> Icons.Filled.Check
        SyncState.FAILED -> Icons.Filled.Error
        SyncState.PAUSED -> Icons.Filled.SyncProblem
        SyncState.IDLE -> Icons.Filled.Refresh
    }
}

/**
 * 根据同步状态获取文本描述
 */
private fun getSyncStatusText(syncState: SyncState, lastSyncTime: Long): String {
    return when (syncState) {
        SyncState.SYNCING -> "同步中..."
        SyncState.SUCCESS -> "同步成功"
        SyncState.FAILED -> "同步失败"
        SyncState.PAUSED -> "同步已暂停"
        SyncState.IDLE -> if (lastSyncTime > 0) {
            "上次同步: ${formatTime(lastSyncTime)}"
        } else {
            "未同步"
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return "从未"
    
    // 计算时间差
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
} 