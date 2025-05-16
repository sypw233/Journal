package ovo.sypw.journal.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
) {
    val syncState by autoSyncManager.syncState.collectAsState()
    val isSyncing by autoSyncManager.isSyncing.collectAsState()
    val lastSyncTime by autoSyncManager.lastSyncTime.collectAsState()
    val autoSyncEnabled by autoSyncManager.autoSyncEnabled.collectAsState()

    // 根据同步状态确定按钮颜色
    when (syncState) {
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
    getSyncStatusText(syncState, lastSyncTime)

    // tooltip显示的错误信息
//    val syncShowText = when {
//        syncState == SyncState.FAILED && !syncError.isNullOrEmpty() -> syncError
//        syncState == SyncState.SUCCESS -> "上次同步时间: ${formatTime(lastSyncTime)}"
//        syncState == SyncState.PAUSED -> "自动同步已暂停，点击启用"
//        else -> "点击开始同步"
//    }


    IconButton(
        onClick = {
            // 如果自动同步已禁用，先启用它
            if (!autoSyncEnabled) {
                autoSyncManager.setAutoSyncEnabled(true)
            } else {
                // 否则触发立即同步
                autoSyncManager.scheduleSyncNow()
            }

        },

    ) {

        Icon(
            imageVector = icon,
            contentDescription = "同步状态",
            modifier = Modifier
                .rotate(if (isSyncing) rotation else 0f)
        )

    }
}

/**
 * 根据同步状态获取图标
 */
private fun getSyncIcon(syncState: SyncState): ImageVector {
    return when (syncState) {
        SyncState.SYNCING -> Icons.Rounded.Sync
        SyncState.SUCCESS -> Icons.Rounded.CloudSync
        SyncState.FAILED -> Icons.Rounded.SyncProblem
        SyncState.PAUSED -> Icons.Rounded.SyncDisabled
        SyncState.IDLE -> Icons.Rounded.Sync
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