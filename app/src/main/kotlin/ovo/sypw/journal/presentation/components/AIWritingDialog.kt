package ovo.sypw.journal.presentation.components

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import ovo.sypw.journal.common.utils.ImageUriUtils
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AIModels
import ovo.sypw.journal.di.AppDependencyManager
import ovo.sypw.journal.presentation.viewmodels.AIWritingViewModel
import androidx.core.net.toUri

/**
 * AI写作对话框
 * 用于用户输入提示词并生成AI写作内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWritingDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onContentGenerated: (String) -> Unit,
    useMarkdown: Boolean = false,
    dependencyManager: AppDependencyManager,
    initialImages: List<Any> = emptyList()
) {
    if (!isVisible) return

    // 使用自定义ViewModel
    val viewModel: AIWritingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AIWritingViewModel(dependencyManager) as T
        }
    })

    // 获取UI状态
    val uiState by viewModel.uiState.collectAsState()

    // 输入提示词
    var prompt by remember { mutableStateOf("") }

    // 是否显示生成的内容
    var showGeneratedContent by remember { mutableStateOf(false) }

    // 是否显示高级设置 - 使用用户设置的默认值
    var showAdvancedSettings by remember { mutableStateOf(false) }

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
                    Log.e("AIWritingDialog", "处理图片URI错误: ${e.message}")
                    null
                }
            }

            if (processedUris.isNotEmpty()) {
                viewModel.addImages(processedUris)
                SnackBarUtils.showSnackBar("已选择${processedUris.size}张图片")
            }
        }
    }

    // 加载初始图片（如果有）
    LaunchedEffect(initialImages) {
        if (initialImages.isNotEmpty()) {
            // 将initialImages转换为Uri列表
            val uriList = initialImages.mapNotNull { image ->
                when (image) {
                    is Uri -> image
                    is String -> image.toString().toUri()
                    else -> null
                }
            }

            if (uriList.isNotEmpty()) {
                viewModel.addImages(uriList)
            }
        }
    }

    // 读取默认设置
    LaunchedEffect(Unit) {
        try {
            showAdvancedSettings =
                dependencyManager.preferences.getAISettings().showAdvancedSettingsDefault
        } catch (e: Exception) {
            Log.e("AIWritingDialog", "Failed to get default settings", e)
        }
    }

    // 模型选择对话框
    var showModelSelectionDialog by remember { mutableStateOf(false) }

    // 当对话框关闭时清理资源
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearState()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!uiState.isLoading) {
                onDismiss()
            } else {
                SnackBarUtils.showSnackBar("正在生成内容，请稍候...")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !uiState.isLoading,
            dismissOnClickOutside = !uiState.isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI写作助手",
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(
                        onClick = {
                            if (!uiState.isLoading) onDismiss()
                            else SnackBarUtils.showSnackBar("正在生成内容，请稍候...")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 图片选择和预览区域
                if (!showGeneratedContent) {
                    // 图片选择按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 使用按钮而不是IconToggleButton，以确保文本和图标都能正确显示
                        Button(
                            onClick = {
                                if (!uiState.images.isEmpty()) {
                                    viewModel.toggleUseImages()
                                } else {
                                    Toast.makeText(context, "请先添加图片", Toast.LENGTH_SHORT).show()
                                }
                            },
//                            modifier = Modifier.padding(vertical = 8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (uiState.useImages)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isLoading
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = if (uiState.useImages) "关闭图片理解" else "启用图片理解",
                                    tint = if (uiState.useImages)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "图片理解",
                                    color = if (uiState.useImages)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 提示词输入框（仅在未显示生成内容时显示）
                    // 当使用图片时，提示词是可选的
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text(if (uiState.useImages) "提示词（可选）" else "提示词") },
                        placeholder = {
                            Text(
                                if (uiState.useImages)
                                    "可选：补充说明或特定要求..."
                                else
                                    "例如：今天去公园散步，看到美丽的花朵..."
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 高级设置折叠面板
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { showAdvancedSettings = !showAdvancedSettings }
                        ) {
                            Text(if (showAdvancedSettings) "隐藏高级设置" else "显示高级设置")
                        }

                        // 显示当前选择的模型
                        TextButton(
                            onClick = { showModelSelectionDialog = true },
                            enabled = !uiState.isLoading
                        ) {
                            Text(
                                text = "模型: ${AIModels.getModelDisplayName(uiState.selectedModel)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showAdvancedSettings) {
                        // 历史日记参考设置
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "历史日记参考设置",
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 使用历史日记开关
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "使用历史日记作为参考",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Switch(
                                        checked = uiState.useHistoricalJournals,
                                        onCheckedChange = { viewModel.toggleUseHistoricalJournals() },
                                        enabled = !uiState.isLoading
                                    )
                                }

                                // 只有当启用历史日记时才显示数量选择器
                                if (uiState.useHistoricalJournals) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 历史日记数量选择器
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "参考日记数量:",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (uiState.historicalJournalsCount > 1) {
                                                        viewModel.setHistoricalJournalsCount(uiState.historicalJournalsCount - 1)
                                                    }
                                                },
                                                enabled = !uiState.isLoading && uiState.historicalJournalsCount > 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "减少数量"
                                                )
                                            }

                                            Text(
                                                text = "${uiState.historicalJournalsCount}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (uiState.historicalJournalsCount < 10) {
                                                        viewModel.setHistoricalJournalsCount(uiState.historicalJournalsCount + 1)
                                                    }
                                                },
                                                enabled = !uiState.isLoading && uiState.historicalJournalsCount < 10,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "增加数量"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 错误提示
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 生成的内容显示（当有内容或正在生成时显示）
                if (uiState.generatedContent.isNotBlank() || (uiState.isLoading && showGeneratedContent)) {
                    // 思考过程显示
                    val thinking = uiState.thinking
                    if (thinking != null && thinking.isNotBlank()) {
                        var isThinkingExpanded by remember { mutableStateOf(true) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                            imageVector = if (isThinkingExpanded) Icons.Default.Close else Icons.Default.Add,
                                            contentDescription = if (isThinkingExpanded) "收起" else "展开",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // 思考过程内容，根据展开状态显示或隐藏
                                if (isThinkingExpanded) {
                                    Text(
                                        text = "\n" + thinking,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                            .height(200.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (useMarkdown) {
                                MarkdownText(
                                    markdown = uiState.generatedContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = uiState.generatedContent,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 生成按钮和加载指示器
                if (uiState.isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "正在生成内容...",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (uiState.useHistoricalJournals) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(参考${uiState.historicalJournalsCount}篇历史日记)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (uiState.generatedContent.isBlank() || !showGeneratedContent) {
                    // 底部按钮 - 生成阶段
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onDismiss() }
                        ) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.size(8.dp))

                        Button(
                            onClick = {
                                if (prompt.isBlank() && !uiState.useImages) {
                                    SnackBarUtils.showSnackBar("请输入提示词")
                                } else if (uiState.useImages && uiState.images.isEmpty()) {
                                    SnackBarUtils.showSnackBar("请选择至少一张图片")
                                } else {
                                    // 显示生成内容区域
                                    showGeneratedContent = true
                                    showAdvancedSettings = false // 隐藏高级设置
                                    // 触发内容生成
                                    viewModel.generateContent(prompt, useMarkdown)
                                }
                            }
                        ) {
                            Text("生成内容")
                        }
                    }
                } else {
                    // 底部按钮 - 应用阶段（当有生成内容时）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                // 清空生成内容并返回提示词输入阶段
                                viewModel.clearGeneratedContent()
                                showGeneratedContent = false
                            }
                        ) {
                            Text("重新生成")
                        }

                        Spacer(modifier = Modifier.size(8.dp))

                        Button(
                            onClick = {
                                // 将内容传递给回调
                                onContentGenerated(uiState.generatedContent)
                                // 关闭对话框
                                onDismiss()
                            }
                        ) {
                            Text("应用内容")
                        }
                    }
                }
            }
        }
    }

    // 模型选择对话框
    if (showModelSelectionDialog) {
        AIModelPickerDialog(
            selectedModel = uiState.selectedModel,
            onModelSelected = { selectedModel ->
                // 如果用户选择了不支持图片的模型，但已启用图片功能，提示用户
                if (uiState.useImages && !viewModel.isModelSupportImage(selectedModel)) {
                    SnackBarUtils.showSnackBar("所选模型不支持图片功能，已自动禁用图片")
                    viewModel.toggleUseImages() // 禁用图片功能
                }
                viewModel.setAIModel(selectedModel)
                showModelSelectionDialog = false
            },
            onDismiss = { showModelSelectionDialog = false },
            highlightImageModels = uiState.useImages,
            isModelSupportImage = { modelId -> viewModel.isModelSupportImage(modelId) }
        )
    }
}

/**
 * AI模型选择对话框
 * 按照模型分类展示不同系列的模型
 */
@Composable
fun AIModelPickerDialog(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    highlightImageModels: Boolean = false,
    isModelSupportImage: (String) -> Boolean = { false }
) {
    // 获取按分类的模型列表
    val modelsByCategory = AIModels.getModelsByCategory()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择AI模型") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 如果启用图片功能，显示提示信息
                if (highlightImageModels) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "已启用图片功能，只能选择支持图片的模型",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 按照分类展示模型
                modelsByCategory.forEach { (category, models) ->
                    // 分类标题
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 分类下的模型列表
                    models.forEach { (modelId, displayName) ->
                        // 如果启用图片功能，只显示支持图片的模型
                        val isSupported = !highlightImageModels || isModelSupportImage(modelId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isSupported) { onModelSelected(modelId) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .alpha(if (isSupported) 1f else 0.5f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = modelId == selectedModel,
                                onClick = { if (isSupported) onModelSelected(modelId) },
                                enabled = isSupported
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                // 提取模型名称和功能说明
                                val nameWithDescription = displayName.split("（")
                                val modelName = nameWithDescription[0]
                                val modelDescription = if (nameWithDescription.size > 1)
                                    "（${nameWithDescription[1]}" else ""

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = modelName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    // 如果是图像模型，显示图片图标
                                    if (isModelSupportImage(modelId)) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "支持图片",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .size(16.dp)
                                        )
                                    }
                                }

                                if (modelDescription.isNotEmpty()) {
                                    Text(
                                        text = modelDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 如果启用图片功能但模型不支持，显示提示
                                if (highlightImageModels && !isModelSupportImage(modelId)) {
                                    Text(
                                        text = "不支持图片功能",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // 类别分隔符
                    if (category != modelsByCategory.keys.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 添加 Modifier.alpha 扩展函数
private fun Modifier.alpha(alpha: Float) = this.then(
    Modifier.graphicsLayer(alpha = alpha)
) 