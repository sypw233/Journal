package ovo.sypw.journal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.database.ConflictResolutionStrategy
import ovo.sypw.journal.data.database.SyncConflict
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.sync.SyncState
import ovo.sypw.journal.viewmodel.SyncViewModel

/**
 * 同步按钮组件
 * 显示在用户详情页面，用于触发数据同步
 */
@Composable
fun SyncButton(
    syncViewModel: SyncViewModel = viewModel(),
    onSyncClick: () -> Unit
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val conflicts by syncViewModel.conflicts.collectAsState()

    Button(
        onClick = onSyncClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (syncState is SyncState.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "同步",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = when (syncState) {
                    is SyncState.Syncing -> "正在同步..."
                    is SyncState.Success -> "同步成功"
                    is SyncState.Error -> "同步失败"
                    else -> if (conflicts.isNotEmpty()) "同步 (${conflicts.size}个冲突)" else "同步数据"
                }
            )

            if (conflicts.isNotEmpty() && syncState !is SyncState.Syncing) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "冲突",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 同步状态对话框
 * 显示当前同步状态和冲突列表
 */
@Composable
fun SyncStatusDialog(
    syncViewModel: SyncViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val conflicts by syncViewModel.conflicts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "数据同步",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 同步状态
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "状态：",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when (syncState) {
                            is SyncState.Syncing -> "正在同步..."
                            is SyncState.Success -> "同步成功"
                            is SyncState.Error -> "同步失败: ${(syncState as SyncState.Error).message}"
                            else -> "准备同步"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (syncState) {
                            is SyncState.Success -> MaterialTheme.colorScheme.primary
                            is SyncState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (syncState is SyncState.Syncing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // 冲突列表
                if (conflicts.isNotEmpty()) {
                    Text(
                        text = "检测到 ${conflicts.size} 个数据冲突",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(conflicts) { conflict ->
                            ConflictItem(
                                conflict = conflict,
                                onResolve = { strategy ->
                                    coroutineScope.launch {
                                        syncViewModel.resolveConflict(conflict.entityId, strategy)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "没有检测到数据冲突",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }

                // 同步按钮
                Button(
                    onClick = {
                        coroutineScope.launch {
                            syncViewModel.synchronize()
                        }
                    },
                    enabled = syncState !is SyncState.Syncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("开始同步")
                }

                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

/**
 * 冲突项组件
 * 显示单个冲突的详情和解决选项
 */
@Composable
fun ConflictItem(
    conflict: SyncConflict,
    onResolve: (ConflictResolutionStrategy) -> Unit
) {
    var selectedStrategy by remember { mutableStateOf<ConflictResolutionStrategy?>(null) }
    val localData = conflict.localEntity as? JournalData
    val remoteData = conflict.remoteEntity as? JournalData

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "日记 ID: ${localData?.id ?: "未知"}",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "本地版本: ${localData?.text?.take(20) ?: "无内容"}${if ((localData?.text?.length ?: 0) > 20) "..." else ""}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "远程版本: ${remoteData?.text?.take(20) ?: "无内容"}${if ((remoteData?.text?.length ?: 0) > 20) "..." else ""}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 冲突解决选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedStrategy == ConflictResolutionStrategy.LOCAL_WINS,
                    onClick = { selectedStrategy = ConflictResolutionStrategy.LOCAL_WINS }
                )
                Text("保留本地版本", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.width(8.dp))

                RadioButton(
                    selected = selectedStrategy == ConflictResolutionStrategy.REMOTE_WINS,
                    onClick = { selectedStrategy = ConflictResolutionStrategy.REMOTE_WINS }
                )
                Text("使用远程版本", style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    selectedStrategy?.let { onResolve(it) }
                },
                enabled = selectedStrategy != null,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            ) {
                Text("解决冲突")
            }
        }
    }
}