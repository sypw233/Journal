package ovo.sypw.journal.presentation.screens


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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovo.sypw.journal.common.utils.SnackBarUtils
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
    onNavigateBack: () -> Unit,
    viewModel: DatabaseManagementViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 初始化SnackBarUtils
    LaunchedEffect(Unit) {
        SnackBarUtils.initialize(snackbarHostState, coroutineScope)
    }

    // 从ViewModel获取数据
    val localFiles by viewModel.localDbFiles.collectAsState()
    val remoteFiles by viewModel.remoteDbFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据库管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    onClick = { viewModel.refreshRemoteFiles() },
                    enabled = !isLoading
                ) {
                    Text("刷新列表")
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
    }
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