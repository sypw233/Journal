package ovo.sypw.journal.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.presentation.viewmodels.SentimentViewModel

/**
 * 情感分析对话框
 * 用于在日记长按时显示情感分析结果
 */
@Composable
fun SentimentAnalysisDialog(
    journal: JournalData,
    viewModel: SentimentViewModel,
    onDismiss: () -> Unit,
    onViewReport: () -> Unit
) {
    // 获取状态
    val selectedSentiment by viewModel.selectedJournalSentiment.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    // 添加动画状态
    var showContent by remember { mutableStateOf(false) }

    // 当对话框显示时，尝试分析或加载情感数据
    LaunchedEffect(journal.id) {
        viewModel.analyzeSentiment(journal)
    }

    // 延迟显示内容，创建动画效果
    LaunchedEffect(Unit) {
        showContent = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("情感分析") },
        text = {
            // 使用Box替代Column，减少嵌套层级
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200)) +
                            scaleOut(targetScale = 0.8f, animationSpec = tween(200))
                ) {
                    when {
                        isAnalyzing -> {
                            // 加载中状态
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }

                        selectedSentiment != null -> {
                            // 直接显示分析结果
                            SimplifiedSentimentView(sentimentData = selectedSentiment!!)
                        }

                        else -> {
                            // 无数据状态
                            Text(
                                text = "暂无情感分析数据",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AnimatedVisibility(
                visible = showContent && !isAnalyzing && selectedSentiment != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TextButton(onClick = onViewReport) {
                    Text("查看情感报告")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
} 