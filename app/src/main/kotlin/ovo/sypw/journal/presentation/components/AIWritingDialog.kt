package ovo.sypw.journal.presentation.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import ovo.sypw.journal.common.utils.ImageUriUtils
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AIModels
import ovo.sypw.journal.di.AppDependencyManager
import ovo.sypw.journal.presentation.viewmodels.AIWritingViewModel

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

    // 创建ViewModel
    val viewModel: AIWritingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AIWritingViewModel(dependencyManager) as T
        }
    })

    // 获取UI状态
    val uiState by viewModel.uiState.collectAsState()

    // 本地UI状态
    var prompt by remember { mutableStateOf("") }
    var showGeneratedContent by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showModelSelectionDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        handleImageSelection(uris, context, viewModel)
    }

    // 初始化
    LaunchedEffect(Unit) {
        try {
            showAdvancedSettings =
                dependencyManager.preferences.getAISettings().showAdvancedSettingsDefault
        } catch (e: Exception) {
            Log.e("AIWritingDialog", "获取默认设置失败", e)
        }
    }

    // 处理初始图片
    LaunchedEffect(initialImages) {
        processInitialImages(initialImages, viewModel)
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose { viewModel.clearState() }
    }

    // 对话框内容
    Dialog(
        onDismissRequest = {
            if (!uiState.isLoading) onDismiss()
            else SnackBarUtils.showSnackBar("正在生成内容，请先点击「取消生成」或等待完成...")
        },
        properties = DialogProperties(
            dismissOnBackPress = !uiState.isLoading,
            dismissOnClickOutside = !uiState.isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                DialogHeader(
                    onCloseClick = {
                        if (!uiState.isLoading) onDismiss()
                        else SnackBarUtils.showSnackBar("正在生成内容，请先点击「取消生成」或等待完成...")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 输入区域 - 仅在未显示生成内容时显示
                if (!showGeneratedContent) {
                    // 图片开关按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImageToggleButton(
                            useImages = uiState.useImages,
                            onToggleUseImages = { viewModel.toggleUseImages() }
                        )
                    }

                    // 提示词输入框
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text(if (uiState.useImages) "提示词（可选）" else "提示词") },
                        placeholder = {
                            Text(
                                if (uiState.useImages) "可选：补充说明或特定要求..."
                                else "例如：今天去公园散步，看到美丽的花朵..."
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 高级设置面板控制
                    AdvancedSettingsHeader(
                        showAdvancedSettings = showAdvancedSettings,
                        selectedModel = uiState.selectedModel,
                        isLoading = uiState.isLoading,
                        onToggleAdvancedSettings = { showAdvancedSettings = !showAdvancedSettings },
                        onSelectModelClick = { showModelSelectionDialog = true }
                    )

                    // 高级设置面板
                    if (showAdvancedSettings) {
                        HistoricalJournalSettings(
                            useHistoricalJournals = uiState.useHistoricalJournals,
                            historicalJournalsCount = uiState.historicalJournalsCount,
                            isLoading = uiState.isLoading,
                            onToggleUseHistoricalJournals = { viewModel.toggleUseHistoricalJournals() },
                            onHistoricalJournalsCountChange = {
                                viewModel.setHistoricalJournalsCount(
                                    it
                                )
                            }
                        )
                    }
                }

                // 错误信息显示
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 思考过程显示
                ThinkingProcessPanel(
                    thinking = uiState.thinking,
                    isLoading = uiState.isLoading
                )

                // 生成内容显示
                ContentDisplayPanel(
                    content = uiState.generatedContent,
                    useMarkdown = useMarkdown
                )

                // 底部状态栏和按钮区域
                if (uiState.isLoading) {
                    // 加载状态和取消按钮
                    LoadingStatusBar(
                        isLoading = uiState.isLoading,
                        canCancel = uiState.canCancel,
                        useHistoricalJournals = uiState.useHistoricalJournals,
                        historicalJournalsCount = uiState.historicalJournalsCount,
                        onCancelClick = { viewModel.cancelGeneration() }
                    )
                } else if (uiState.generatedContent.isBlank() || !showGeneratedContent) {
                    // 生成阶段按钮
                    GenerationActionBar(
                        prompt = prompt,
                        useImages = uiState.useImages,
                        hasImages = uiState.images.isNotEmpty(),
                        onDismiss = onDismiss,
                        onGenerate = {
                            showGeneratedContent = true
                            showAdvancedSettings = false
                            viewModel.generateContent(prompt, useMarkdown)
                        }
                    )
                } else {
                    // 结果阶段按钮
                    ResultActionBar(
                        onRegenerate = {
                            viewModel.clearGeneratedContent()
                            showGeneratedContent = false
                        },
                        onApply = {
                            onContentGenerated(uiState.generatedContent)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }

    // 模型选择对话框
    if (showModelSelectionDialog) {
        AIModelPickerDialog(
            selectedModel = uiState.selectedModel,
            onModelSelected = { selectedModel ->
                // 处理模型选择
                handleModelSelection(selectedModel, uiState.useImages, viewModel)
                showModelSelectionDialog = false
            },
            onDismiss = { showModelSelectionDialog = false },
            highlightImageModels = uiState.useImages,
            isModelSupportImage = { modelId -> viewModel.isModelSupportImage(modelId) }
        )
    }
}

/**
 * 对话框标题栏
 */
@Composable
private fun DialogHeader(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AI写作助手",
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭"
            )
        }
    }
}

/**
 * 高级设置面板的标题栏
 */
@Composable
private fun AdvancedSettingsHeader(
    showAdvancedSettings: Boolean,
    selectedModel: String,
    isLoading: Boolean,
    onToggleAdvancedSettings: () -> Unit,
    onSelectModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onToggleAdvancedSettings) {
            Text(if (showAdvancedSettings) "隐藏高级设置" else "显示高级设置")
        }

        // 显示当前选择的模型
        TextButton(
            onClick = onSelectModelClick,
            enabled = !isLoading
        ) {
            Text(
                text = "模型: ${AIModels.getModelDisplayName(selectedModel)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 处理图片选择结果
 */
private fun handleImageSelection(
    uris: List<Uri>,
    context: Context,
    viewModel: AIWritingViewModel
) {
    if (uris.isNotEmpty()) {
        val processedUris = uris.mapNotNull { uri ->
            try {
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

/**
 * 处理初始图片
 */
private fun processInitialImages(
    initialImages: List<Any>,
    viewModel: AIWritingViewModel
) {
    if (initialImages.isNotEmpty()) {
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

/**
 * 处理模型选择
 */
private fun handleModelSelection(
    selectedModel: String,
    useImages: Boolean,
    viewModel: AIWritingViewModel
) {
    // 如果用户选择了不支持图片的模型，但已启用图片功能，提示用户
    if (useImages && !viewModel.isModelSupportImage(selectedModel)) {
        SnackBarUtils.showSnackBar("所选模型不支持图片功能，已自动禁用图片")
        viewModel.toggleUseImages() // 禁用图片功能
    }
    viewModel.setAIModel(selectedModel)
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

/**
 * 思考过程面板组件
 * 显示AI模型的思考过程，支持展开/收起
 */
@Composable
private fun ThinkingProcessPanel(
    thinking: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // 如果没有思考内容，不显示
    if (thinking.isNullOrBlank()) return

    // 根据加载状态决定是否展开思考窗口，默认收起
    var isThinkingExpanded by remember { mutableStateOf(false) }

    // 当加载状态发生变化时重新评估展开状态
    LaunchedEffect(isLoading) {
        // 只在加载状态改变时更新展开状态
        isThinkingExpanded = isLoading
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 思考过程标题和展开/收起按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { isThinkingExpanded = !isThinkingExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "思考过程",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )

                // 展开/收起按钮
                Icon(
                    imageVector = if (isThinkingExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isThinkingExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 思考过程内容，根据展开状态显示或隐藏
            AnimatedVisibility(
                visible = isThinkingExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = thinking,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 生成内容显示面板
 * 用于显示AI生成的内容，支持Markdown和普通文本
 */
@Composable
private fun ContentDisplayPanel(
    content: String,
    useMarkdown: Boolean,
    modifier: Modifier = Modifier
) {
    if (content.isBlank()) return

    Column(modifier = modifier) {
        Text(
            text = "生成内容",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (useMarkdown) {
                    MarkdownText(
                        markdown = content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 加载状态栏
 * 显示加载指示器和取消按钮
 */
@Composable
private fun LoadingStatusBar(
    isLoading: Boolean,
    canCancel: Boolean,
    useHistoricalJournals: Boolean = false,
    historicalJournalsCount: Int = 0,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLoading) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧显示进度和状态
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "正在生成内容...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 右侧显示中止按钮
            if (canCancel) {
                Button(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("取消生成")
                }
            }
        }

        if (useHistoricalJournals) {
            Text(
                text = "(参考${historicalJournalsCount}篇历史日记)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * 生成阶段的按钮操作栏
 */
@Composable
private fun GenerationActionBar(
    prompt: String,
    useImages: Boolean,
    hasImages: Boolean,
    onDismiss: () -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onDismiss
        ) {
            Text("取消")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                if (prompt.isBlank() && !useImages) {
                    SnackBarUtils.showSnackBar("请输入提示词")
                } else if (useImages && !hasImages) {
                    SnackBarUtils.showSnackBar("请选择至少一张图片")
                } else {
                    onGenerate()
                }
            }
        ) {
            Text("生成内容")
        }
    }
}

/**
 * 结果阶段的按钮操作栏
 */
@Composable
private fun ResultActionBar(
    onRegenerate: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        TextButton(
            onClick = onRegenerate
        ) {
            Text("重新生成")
        }

        Button(
            onClick = onApply
        ) {
            Text("应用内容")
        }
    }
}

/**
 * 历史日记参考设置面板
 */
@Composable
private fun HistoricalJournalSettings(
    useHistoricalJournals: Boolean,
    historicalJournalsCount: Int,
    isLoading: Boolean,
    onToggleUseHistoricalJournals: () -> Unit,
    onHistoricalJournalsCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier
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
                    checked = useHistoricalJournals,
                    onCheckedChange = { onToggleUseHistoricalJournals() },
                    enabled = !isLoading
                )
            }

            // 只有当启用历史日记时才显示数量选择器
            if (useHistoricalJournals) {
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
                                if (historicalJournalsCount > 1) {
                                    onHistoricalJournalsCountChange(historicalJournalsCount - 1)
                                }
                            },
                            enabled = !isLoading && historicalJournalsCount > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "减少数量"
                            )
                        }

                        Text(
                            text = "$historicalJournalsCount",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (historicalJournalsCount < 10) {
                                    onHistoricalJournalsCountChange(historicalJournalsCount + 1)
                                }
                            },
                            enabled = !isLoading && historicalJournalsCount < 10,
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

/**
 * 图片功能开关按钮
 */
@Composable
private fun ImageToggleButton(
    useImages: Boolean,
    onToggleUseImages: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconToggleButton(
        checked = useImages,
        onCheckedChange = { onToggleUseImages() },
        modifier = modifier.width(120.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = if (useImages) "关闭图片理解" else "启用图片理解",
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "图片理解",
            )
        }
    }
} 