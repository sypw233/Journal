package ovo.sypw.journal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.sync.SyncState
import ovo.sypw.journal.viewmodel.SyncViewModel

/**
 * 同步进度组件
 * 显示同步进度和统计信息
 */
@Composable
fun SyncProgressComponent(
    syncViewModel: SyncViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 同步进度状态
    var progress by remember { mutableFloatStateOf(0f) }
    var pendingSyncCount by remember { mutableStateOf(0) }
    var conflictCount by remember { mutableStateOf(0) }

    // 动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "SyncProgress"
    )

    // 获取同步统计信息
    LaunchedEffect(syncState) {
        coroutineScope.launch {
            pendingSyncCount = syncViewModel.getPendingSyncCount()
            conflictCount = syncViewModel.getConflictCount()

            // 如果正在同步，模拟进度
            if (syncState is SyncState.Syncing) {
                progress = 0f
                while (syncState is SyncState.Syncing && progress < 0.95f) {
                    progress += 0.05f
                    delay(300)
                }
                if (syncState !is SyncState.Syncing) {
                    progress = 1f
                }
            } else if (syncState is SyncState.Success) {
                progress = 1f
            } else {
                progress = 0f
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "同步状态",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 同步进度条
            if (syncState is SyncState.Syncing) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "正在同步数据...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (syncState is SyncState.Success) {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "同步完成",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (syncState is SyncState.Error) {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "同步失败: ${(syncState as SyncState.Error).message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 同步统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "待同步项目",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "$pendingSyncCount",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "冲突项目",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conflictCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$conflictCount",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (conflictCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}