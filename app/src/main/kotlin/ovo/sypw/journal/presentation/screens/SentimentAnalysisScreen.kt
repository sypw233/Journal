package ovo.sypw.journal.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData
import ovo.sypw.journal.presentation.components.JournalCard
import ovo.sypw.journal.presentation.components.SentimentAnalysisCard
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel
import ovo.sypw.journal.presentation.viewmodels.SentimentViewModel

/**
 * 情感分析屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentimentAnalysisScreen(
    onBackClick: () -> Unit,
    viewModel: SentimentViewModel = hiltViewModel(),
    journalListViewModel: JournalListViewModel = hiltViewModel()
) {
    // 获取界面状态
    val journals by journalListViewModel.journals.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val batchAnalysisProgress by viewModel.batchAnalysisProgress.collectAsState()
    val selectedJournalSentiment by viewModel.selectedJournalSentiment.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val filteredResults by viewModel.filteredResults.collectAsState()
    
    // 本地UI状态
    var selectedJournal by remember { mutableStateOf<JournalData?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showBatchAnalysisDialog by remember { mutableStateOf(false) }
    var showDistributionDialog by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 加载日记列表
    LaunchedEffect(Unit) {
        journalListViewModel.loadJournals()
    }
    
    // 初始化情感分析器并检查是否有日记需要分析
    LaunchedEffect(Unit) {
        if (journals.isNotEmpty()) {
            // 尝试分析第一篇日记以初始化情感分析器
            journals.firstOrNull()?.let { firstJournal ->
                viewModel.analyzeSentiment(firstJournal)
            }
        }
    }
    
    // 当日记列表改变时，更新过滤结果
    LaunchedEffect(journals, currentFilter) {
        if (journals.isNotEmpty()) {
            viewModel.updateFilteredResults(journals)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("情感分析") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 批量分析按钮
                    IconButton(
                        onClick = { showBatchAnalysisDialog = true },
                        enabled = !isAnalyzing && journals.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "批量分析"
                        )
                    }
                    
                    // 过滤按钮
                    IconButton(
                        onClick = { showFilterDialog = true },
                        enabled = !isAnalyzing
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "过滤"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isAnalyzing && journals.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDistributionDialog = true },
                    icon = { Icon(Icons.Default.FilterList, contentDescription = "情感分布") },
                    text = { Text("情感分布") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (journals.isEmpty()) {
                // 无日记状态
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "暂无日记",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackClick) {
                        Text("返回创建日记")
                    }
                }
            } else if (filteredResults.isEmpty() && !isAnalyzing) {
                // 无分析结果状态
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (currentFilter == null) 
                            "暂无情感分析结果，点击批量分析开始分析日记" 
                        else 
                            "没有${getFilterName(currentFilter)}类型的日记",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            if (currentFilter == null) {
                                showBatchAnalysisDialog = true 
                            } else {
                                viewModel.setFilter(null)
                            }
                        }
                    ) {
                        Text(
                            if (currentFilter == null) "开始分析" else "查看所有类型"
                        )
                    }
                }
            } else {
                // 日记列表和情感分析
                Column(modifier = Modifier.fillMaxSize()) {
                    // 选中的情感分析卡片
                    if (selectedJournal != null && selectedJournalSentiment != null) {
                        SentimentAnalysisCard(
                            sentimentData = selectedJournalSentiment,
                            isAnalyzing = isAnalyzing,
                            expanded = true,
                            onReAnalyze = {
                                selectedJournal?.let { 
                                    viewModel.analyzeSentiment(it, true)
                                    coroutineScope.launch {
                                        viewModel.updateFilteredResults(journals)
                                    }
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    // 过滤信息
                    if (currentFilter != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "当前过滤: ${getFilterName(currentFilter)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = { viewModel.setFilter(null) }) {
                                    Text("清除")
                                }
                            }
                        }
                    }
                    
                    // 日记列表
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredResults) { (journal, sentiment) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable {
                                        selectedJournal = journal
                                        viewModel.analyzeSentiment(journal)
                                    },
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 4.dp,
                                shadowElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // 日记卡片
                                    JournalCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        journalData = journal
                                    )
                                    
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    
                                    // 简要情感信息
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val sentimentColor = when (sentiment.sentimentType) {
                                            SentimentType.POSITIVE -> MaterialTheme.colorScheme.primary
                                            SentimentType.NEGATIVE -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (sentiment.sentimentType) {
                                                    SentimentType.POSITIVE -> Icons.Default.SentimentSatisfied
                                                    SentimentType.NEGATIVE -> Icons.Default.SentimentVeryDissatisfied
                                                    else -> Icons.Default.SentimentSatisfied
                                                },
                                                contentDescription = sentiment.getSentimentDescription(),
                                                tint = sentimentColor,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            
                                            Text(
                                                text = sentiment.getSentimentDescription(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = sentimentColor
                                            )
                                        }
                                        
                                        if (sentiment.dominantEmotion.isNotEmpty()) {
                                            Text(
                                                text = sentiment.dominantEmotion,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 批量分析对话框
    if (showBatchAnalysisDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAnalyzing) showBatchAnalysisDialog = false },
            title = { Text("批量情感分析") },
            text = { 
                Column {
                    Text("将对所有日记进行情感分析，每5条为一批次，每批次之间会有3秒延迟。")
                    Text("当前共有 ${journals.size} 篇日记需要分析")
                    
                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                if (!isAnalyzing) {
                    TextButton(
                        onClick = {
                            // 执行批量分析
                            viewModel.batchAnalyzeSentiment(journals, true)
                            showBatchAnalysisDialog = false
                        }
                    ) {
                        Text("开始分析")
                    }
                }
            },
            dismissButton = {
                if (!isAnalyzing) {
                    TextButton(onClick = { showBatchAnalysisDialog = false }) {
                        Text("取消")
                    }
                }
            }
        )
    }
    
    // 批量分析进度对话框
    if (isAnalyzing && batchAnalysisProgress > 0f) {
        AlertDialog(
            onDismissRequest = { /* 不允许关闭 */ },
            title = { Text("正在分析...") },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = { batchAnalysisProgress })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "已完成 ${(batchAnalysisProgress * 100).toInt()}%",
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "批量处理中，分析结果会实时显示",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { /* 无按钮 */ },
            dismissButton = { /* 无按钮 */ }
        )
    }
    
    // 情感过滤对话框
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = currentFilter,
            onFilterSelected = { filter ->
                viewModel.setFilter(filter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    // 情感分布对话框
    if (showDistributionDialog) {
        val distribution = viewModel.getSentimentDistribution()
        SentimentDistributionDialog(
            distribution = distribution,
            onDismiss = { showDistributionDialog = false }
        )
    }
}

/**
 * 情感过滤对话框
 */
@Composable
private fun FilterDialog(
    currentFilter: SentimentType?,
    onFilterSelected: (SentimentType?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("按情感类型过滤") },
        text = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                FilterOption(
                    title = "全部",
                    isSelected = currentFilter == null,
                    onClick = { onFilterSelected(null) }
                )
                FilterOption(
                    title = "积极情感",
                    isSelected = currentFilter == SentimentType.POSITIVE,
                    onClick = { onFilterSelected(SentimentType.POSITIVE) }
                )
                FilterOption(
                    title = "消极情感",
                    isSelected = currentFilter == SentimentType.NEGATIVE,
                    onClick = { onFilterSelected(SentimentType.NEGATIVE) }
                )
                FilterOption(
                    title = "中性情感",
                    isSelected = currentFilter == SentimentType.NEUTRAL,
                    onClick = { onFilterSelected(SentimentType.NEUTRAL) }
                )
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {}
    )
}

/**
 * 情感分布对话框
 */
@Composable
private fun SentimentDistributionDialog(
    distribution: Map<SentimentType, Pair<Int, Float>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("情感分布统计") },
        text = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                if (distribution.isEmpty()) {
                    Text("暂无情感分析数据")
                } else {
                    Text(
                        text = "总计分析: ${distribution.values.sumOf { it.first }} 篇日记",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    distribution.forEach { (type, data) ->
                        val (count, percentage) = data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = getFilterName(type),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when(type) {
                                    SentimentType.POSITIVE -> 
                                        MaterialTheme.colorScheme.primary
                                    SentimentType.NEGATIVE -> 
                                        MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            
                            Text(
                                text = "$count (${(percentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 过滤选项
 */
@Composable
private fun FilterOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 获取情感类型的可读名称
 */
private fun getFilterName(type: SentimentType?): String {
    return when(type) {
        SentimentType.POSITIVE -> "积极"
        SentimentType.NEGATIVE -> "消极"
        SentimentType.NEUTRAL -> "中性"
        SentimentType.UNKNOWN -> "未知"
        null -> "全部"
    }
} 