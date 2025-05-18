package ovo.sypw.journal.presentation.screens


import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.presentation.viewmodels.DatabaseCompareResult
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据库管理界面
 * 提供数据库的导出、上传、下载等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseManagementScreen(
    onBackClick: () -> Unit,
    viewModel: DatabaseManagementViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current



    // 从ViewModel获取数据
    val localFiles by viewModel.localDbFiles.collectAsState()
    val remoteFiles by viewModel.remoteDbFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val compareResult by viewModel.databaseCompareResult.collectAsState()
    val showRestartDialog by viewModel.showRestartDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据库管理") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.exportDatabase() },
                    enabled = !isLoading
                ) {
                    Text("导出数据库")
                }

                Button(
                    onClick = { viewModel.uploadDatabase() },
                    enabled = !isLoading
                ) {
                    Text("上传数据库")
                }
                
                Button(
                    onClick = { viewModel.syncDatabase() },
                    enabled = !isLoading
                ) {
                    Text("同步数据库")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Refresh, contentDescription = "同步")
                }

                Button(
                    onClick = { viewModel.refreshRemoteFiles() },
                    enabled = !isLoading
                ) {
                    Text("刷新列表")
                }
            }

            // 添加诊断按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { viewModel.forceExportDatabase() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("强制导出数据库")
                }
                
                Button(
                    onClick = { viewModel.diagnoseDatabaseExport() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("诊断导出问题")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.BugReport, contentDescription = "诊断")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 本地数据库文件
            Text(
                text = "本地数据库文件",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (localFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有本地数据库文件")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(localFiles) { file ->
                        DatabaseFileItem(
                            file = file,
                            onUpload = { viewModel.uploadDatabase(file) },
                            onDelete = { viewModel.deleteLocalFile(file) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // 远程数据库文件
            Text(
                text = "远程数据库文件",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (remoteFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有远程数据库文件")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(remoteFiles) { fileItem ->
                        RemoteFileItem(
                            name = fileItem.name,
                            path = fileItem.url ?: "",
                            size = fileItem.size,
                            modified = fileItem.modified,
                            onDownload = { viewModel.downloadDatabase(fileItem.url ?: "") },
                            onDelete = { viewModel.deleteRemoteFile(fileItem.url ?: "") }
                        )
                    }
                }
            }
        }
        
        // 显示数据库比较对话框
        if (compareResult != null) {
            DatabaseCompareDialog(
                compareResult = compareResult!!,
                onDismiss = { viewModel.clearCompareResult() },
                onUseLocal = { viewModel.useLocalDatabase() },
                onUseRemote = { viewModel.useRemoteDatabase() }
            )
        }
        
        // 显示重启应用提示对话框
        if (showRestartDialog) {
            RestartAppDialog(
                onDismiss = { viewModel.dismissRestartDialog() },
                onRestart = {
                    // 重启应用
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    // 结束当前进程
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            )
        }
    }
}

/**
 * 数据库比较对话框
 */
@Composable
fun DatabaseCompareDialog(
    compareResult: DatabaseCompareResult,
    onDismiss: () -> Unit,
    onUseLocal: () -> Unit,
    onUseRemote: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据库同步") },
        text = {
            Column {
                Text(
                    "请选择要保留的数据库版本：",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 本地数据库信息
                Text(
                    "本地数据库:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("条目数量: ${compareResult.localEntryCount}")
                Text("最后修改: ${formatLocalDate(compareResult.localLastModified)}")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 远程数据库信息
                Text(
                    "远程数据库:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("条目数量: ${compareResult.remoteEntryCount}")
                Text("最后修改: ${formatDate(compareResult.remoteLastModified / 1000)}")  // 转换为秒
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 推荐选择
                val isRemoteNewer = compareResult.remoteLastModified > compareResult.localLastModified
                val hasMoreEntries = compareResult.remoteEntryCount > compareResult.localEntryCount
                
                if (isRemoteNewer && hasMoreEntries) {
                    Text(
                        "推荐: 使用远程数据库（更新且条目更多）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isRemoteNewer) {
                    Text(
                        "推荐: 使用远程数据库（更新）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (hasMoreEntries) {
                    Text(
                        "推荐: 使用远程数据库（条目更多）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "推荐: 使用本地数据库（更新且/或条目更多）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUseRemote()
                    onDismiss()
                }
            ) {
                Text("使用远程数据库")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onUseLocal()
                    onDismiss()
                }
            ) {
                Text("使用本地数据库")
            }
        }
    )
}

/**
 * 本地数据库文件项
 */
@Composable
fun DatabaseFileItem(
    file: File,
    onUpload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "大小: ${formatFileSize(file.length())}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "修改时间: ${formatLocalDate(file.lastModified())}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onUpload) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上传")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

/**
 * 远程数据库文件项
 */
@Composable
fun RemoteFileItem(
    name: String,
    path: String,
    size: Long,
    modified: Long,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "路径: $path",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "大小: ${formatFileSize(size)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "修改时间: ${formatDate(modified)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下载")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${(size / 1024f).toInt()} KB"
        else -> "${(size / (1024f * 1024f)).toInt()} MB"
    }
}

/**
 * 格式化日期（适用于远程文件时间戳，单位为秒）
 */
private fun formatDate(timestamp: Long): String {
    // 时间戳单位是秒，转换为毫秒
    return SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    ).format(Date(timestamp * 1000))
}

/**
 * 格式化本地日期（适用于本地文件时间戳，单位为毫秒）
 */
private fun formatLocalDate(timestamp: Long): String {
    // 本地文件的时间戳单位已经是毫秒，不需要转换
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * 重启应用对话框
 */
@Composable
fun RestartAppDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据库已恢复") },
        text = {
            Text("数据库已成功恢复，需要重启应用才能生效。现在重启应用吗？")
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text("重启应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后重启")
            }
        }
    )
} 