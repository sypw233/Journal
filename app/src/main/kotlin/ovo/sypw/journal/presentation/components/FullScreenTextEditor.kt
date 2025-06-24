package ovo.sypw.journal.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * 全屏文本编辑器组件
 * 提供沉浸式编辑体验和Markdown预览功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenTextEditor(
    isVisible: Boolean,
    initialText: String,
    isMarkdown: Boolean,
    onTextChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    // 本地状态
    var text by remember { mutableStateOf(initialText) }
    var showPreview by remember { mutableStateOf(isMarkdown && initialText.isNotEmpty()) }
    var splitView by remember { mutableStateOf(isMarkdown && initialText.isNotEmpty()) }

    // 创建动画状态
    val animationState = remember { MutableTransitionState(false) }

    // 当isVisible变为true时，开始显示动画
    LaunchedEffect(isVisible) {
        if (isVisible) {
            animationState.targetState = true
        }
    }

    // 处理关闭动画
    fun handleDismiss() {
        animationState.targetState = false
    }

    // 监听动画状态，当动画完全消失时调用onDismiss
    LaunchedEffect(animationState.currentState) {
        if (!animationState.currentState && !animationState.targetState) {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visibleState = animationState,
        enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.9f) +
                slideInVertically(tween(300)) { it / 10 },
        exit = fadeOut(tween(200)) + scaleOut(tween(300), targetScale = 0.9f) +
                slideOutVertically(tween(300)) { it / 10 }
    ) {
        Dialog(
            onDismissRequest = { handleDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text(if (showPreview && !splitView && isMarkdown) "预览" else "编辑内容") },
                        navigationIcon = {
                            IconButton(onClick = { handleDismiss() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                        },
                        actions = {
                            // Markdown预览模式切换按钮
                            if (isMarkdown) {
                                // 分屏切换按钮
                                IconButton(onClick = {
                                    splitView = !splitView
                                    if (splitView) showPreview = true
                                }) {
                                    Icon(
                                        imageVector = if (splitView) Icons.Outlined.ViewAgenda else Icons.Outlined.ViewColumn,
                                        contentDescription = if (splitView) "单屏模式" else "分屏模式"
                                    )
                                }

                                // 预览切换按钮（仅在非分屏模式下显示）
                                if (!splitView) {
                                    IconButton(onClick = { showPreview = !showPreview }) {
                                        Icon(
                                            imageVector = if (showPreview) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = if (showPreview) "隐藏预览" else "显示预览"
                                        )
                                    }
                                }
                            }

                            // 确认按钮
                            IconButton(onClick = {
                                onTextChanged(text)
                                handleDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "确认"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (splitView && isMarkdown) {
                        // 分屏模式：左侧编辑，右侧预览
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 编辑区域
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                TextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    placeholder = { Text("写下你的想法...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            }

                            // 分隔线
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            // 预览区域
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "预览",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    MarkdownText(
                                        markdown = text,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else if (showPreview && isMarkdown) {
                        // 全屏预览模式
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MarkdownText(
                                    markdown = text,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // 全屏编辑模式
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            placeholder = { Text("写下你的想法...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
} 