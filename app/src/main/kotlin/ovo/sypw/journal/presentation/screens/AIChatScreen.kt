package ovo.sypw.journal.presentation.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.ImageUriUtils
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.presentation.viewmodels.AIChatViewModel

/**
 * AI聊天消息数据类
 */
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val images: List<Uri> = emptyList(),
    val isUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val thinking: String? = null
)

/**
 * AI聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    viewModel: AIChatViewModel
) {
    LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 从ViewModel获取状态
    val uiState by viewModel.uiState.collectAsState()

    // 本地UI状态
    var inputText by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val listState = rememberLazyListState()
    var showModelDropdown by remember { mutableStateOf(false) }

    // 图片选择器
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val processedUris = uris.mapNotNull { uri ->
                try {
                    // 使用工具类处理URI权限
                    ImageUriUtils.takePersistablePermission(context, uri)
                    uri
                } catch (e: Exception) {
                    Log.e("AIChatScreen", "处理图片URI错误: ${e.message}")
                    null
                }
            }

            selectedImages = processedUris
            if (processedUris.isNotEmpty()) {
                SnackBarUtils.showSnackBar("已选择${processedUris.size}张图片")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    // 上下文切换按钮
                    val tooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = if (uiState.contextEnabled) "上下文已启用" else "上下文已禁用",
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        },
                        state = tooltipState
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.contextEnabled)
                                    Icons.Default.History
                                else
                                    Icons.Default.HistoryToggleOff,
                                contentDescription = "上下文状态",
                                tint = if (uiState.contextEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Switch(
                                checked = uiState.contextEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleContext(enabled)
                                    SnackBarUtils.showSnackBar(
                                        if (enabled) "上下文已启用，AI将记住对话历史"
                                        else "上下文已禁用，每次对话独立"
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // 模型选择下拉菜单
                    Box {
                        TextButton(
                            onClick = { showModelDropdown = true }
                        ) {
                            Text("模型: ${uiState.selectedModel}")
                        }

                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false }
                        ) {
                            uiState.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.updateSelectedModel(model)
                                        showModelDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 重置对话按钮
                    IconButton(onClick = {
                        viewModel.resetChat()
                        SnackBarUtils.showSnackBar("对话已重置")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重置对话")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                // 显示已选择的图片
                if (selectedImages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedImages) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // 删除图片按钮
                                IconButton(
                                    onClick = {
                                        selectedImages = selectedImages.filter { it != uri }
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 输入区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 选择图片按钮
                    IconButton(onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Default.Face, contentDescription = "选择图片")
                    }

                    // 文本输入框
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("输入消息...") },
                        singleLine = false,
                        maxLines = 3,
//                        colors = TextFieldDefaults.textFieldColors(
//                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
//                            focusedIndicatorColor = Color.Transparent,
//                            unfocusedIndicatorColor = Color.Transparent
//                        )
                    )

                    // 发送按钮
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                // 调用ViewModel发送消息
                                viewModel.sendMessage(inputText, selectedImages)

                                // 清空输入和已选图片
                                inputText = ""
                                selectedImages = emptyList()

                                // 滚动到底部
                                coroutineScope.launch {
                                    if (uiState.messages.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.messages.size - 1)
                                    }
                                }
                            } else {
                                SnackBarUtils.showSnackBar("请输入消息或选择图片")
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "发送")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // 聊天历史记录
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatMessageItem(message = message)
                }
            }

            // 显示错误信息
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // 显示加载指示器
//            if (uiState.isLoading) {
//                CircularProgressIndicator(
//                    modifier = Modifier
//                        .align(Alignment.Center)
//                        .padding(16.dp)
//                )
//            }
        }
    }
}

/**
 * 聊天消息项组件
 */
@Composable
fun ChatMessageItem(message: ChatMessage) {
    // 添加思考窗口展开/收起状态
    var isThinkingExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        // 发送者标签
        Text(
            text = if (message.isUser) "" else "",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 图片内容
        if (message.images.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                items(message.images) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Message Image",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // 思考过程显示
        if (!message.isUser && message.thinking != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 思考过程标题和展开/收起按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "思考过程",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )

                        // 展开/收起按钮
                        IconButton(
                            onClick = { isThinkingExpanded = !isThinkingExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isThinkingExpanded) Icons.Default.Close else Icons.Default.ArrowDropDown,
                                contentDescription = if (isThinkingExpanded) "收起" else "展开",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 思考过程内容，根据展开状态显示或隐藏
                if (isThinkingExpanded) {
                    Text(
                        text = "\n" + message.thinking,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 文本内容
        if (message.content.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                if (message.isUser) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Left
                    )
                } else {
                    MarkdownText(
                        markdown = message.content,
                        modifier = Modifier.padding(12.dp),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Left,
                        ),
                    )
                }


            }
        }
    }
}