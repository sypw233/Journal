package ovo.sypw.journal.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.viewmodel.JournalListViewModel

/**
 * 同步按钮组件
 * 显示在顶部应用栏中，用于触发日记条目的同步功能
 * 使用JournalListViewModel管理同步状态和操作
 */
@Composable
fun SyncButton(
    journalListViewModel: JournalListViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by journalListViewModel.uiState.collectAsState()
    
    // 旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500)
        ),
        label = "sync_rotation"
    )
    
    Box(modifier = modifier) {
        if (uiState.isSyncing) {
            // 显示同步进度
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (uiState.syncTotal > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.syncProgress}/${uiState.syncTotal}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // 显示同步按钮
            IconButton(
                onClick = {
                    journalListViewModel.syncWithServer()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "同步日记",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}