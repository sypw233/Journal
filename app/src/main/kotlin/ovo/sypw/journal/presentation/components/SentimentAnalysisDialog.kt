package ovo.sypw.journal.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData
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
    
    // 当对话框显示时，尝试分析或加载情感数据
    LaunchedEffect(journal.id) {
        viewModel.analyzeSentiment(journal)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("情感分析") },
        text = { 
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isAnalyzing) {
                    // 加载中状态
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "正在分析情感...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (selectedSentiment != null) {
                    // 显示分析结果
                    SentimentAnalysisCard(
                        sentimentData = selectedSentiment,
                        isAnalyzing = false,
                        expanded = true
                    )
                } else {
                    // 无数据状态
                    Text(
                        text = "暂无情感分析数据",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onViewReport) {
                Text("查看情感报告")
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