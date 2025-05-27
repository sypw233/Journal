package ovo.sypw.journal.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.theme.SentimentColors
import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData
import ovo.sypw.journal.presentation.components.JournalCard
import ovo.sypw.journal.presentation.components.SentimentAnalysisCard
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel
import ovo.sypw.journal.presentation.viewmodels.SentimentViewModel

/**
 * 情感分析报告屏幕
 * 显示所有日记的情感分析结果统计和分布
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentimentReportScreen(
    onBackClick: () -> Unit,
    viewModel: SentimentViewModel = hiltViewModel(),
    journalListViewModel: JournalListViewModel = hiltViewModel()
) {
    // 获取界面状态
    val journals by journalListViewModel.journals.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val batchAnalysisProgress by viewModel.batchAnalysisProgress.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val currentTimePeriod by viewModel.currentTimePeriod.collectAsState()
    val filteredResults by viewModel.filteredResults.collectAsState()
    
    // 本地UI状态
    var showBatchAnalysisDialog by remember { mutableStateOf(false) }
    var showPeriodSelectorDialog by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 加载日记列表
    LaunchedEffect(Unit) {
        try {
            journalListViewModel.loadJournals()
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Log.e("SentimentReportScreen", "加载日记列表失败", e)
            }
        }
    }
    
    // 当日记列表加载完成后，从数据库加载情感分析结果
    LaunchedEffect(journals) {
        if (journals.isNotEmpty()) {
            try {
                // 从数据库加载日记与情感分析结果
                viewModel.loadJournalsWithSentiments(journals)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("SentimentReportScreen", "加载情感分析结果失败", e)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("情感分析报告") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 时间周期筛选按钮
                    IconButton(
                        onClick = { showPeriodSelectorDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "筛选时间周期"
                        )
                    }
                    
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
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 显示当前选择的时间周期
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "时间范围: ${currentTimePeriod.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { showPeriodSelectorDialog = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "更改",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider()
                
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
                } else {
                    val sentiments = filteredResults.map { it.second }
                    val hasData = sentiments.isNotEmpty()
                    
                    if (!hasData && !isAnalyzing) {
                        // 无分析结果状态
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "在${currentTimePeriod.displayName}内暂无情感分析结果",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showBatchAnalysisDialog = true }
                                ) {
                                    Text("开始分析")
                                }
                                OutlinedButton(
                                    onClick = { showPeriodSelectorDialog = true }
                                ) {
                                    Text("更改时间范围")
                                }
                            }
                        }
                    } else {
                        // 分析报告界面
                        SentimentReportContent(
                            journals = journals,
                            results = filteredResults,
                            isAnalyzing = isAnalyzing,
                            currentTimePeriod = currentTimePeriod
                        )
                    }
                }
            }
            
            // 显示加载进度
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(progress = { batchAnalysisProgress })
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在分析... ${(batchAnalysisProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                    Text("将对所有日记进行情感分析，每篇日记单独请求，每次请求之间会有0.5秒延迟。")
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
    
    // 时间周期选择对话框
    if (showPeriodSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showPeriodSelectorDialog = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择时间周期")
                }
            },
            text = { 
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SentimentViewModel.TimePeriod.values().forEach { period ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        viewModel.setTimePeriod(period)
                                        viewModel.updateFilteredResults(journals)
                                        showPeriodSelectorDialog = false
                                    }
                                },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = period.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (period == currentTimePeriod) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (period == currentTimePeriod) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        if (period != SentimentViewModel.TimePeriod.values().last()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPeriodSelectorDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/**
 * 情感分析报告内容
 */
@Composable
private fun SentimentReportContent(
    journals: List<JournalData>,
    results: List<Pair<JournalData, SentimentData>>,
    isAnalyzing: Boolean,
    currentTimePeriod: SentimentViewModel.TimePeriod
) {
    // 记录日记和筛选结果数量
    val totalJournals = journals.size
    val filteredCount = results.size
    Log.d("SentimentReportScreen", "报告内容: 总日记数=${totalJournals}, 筛选结果数=${filteredCount}, 时间周期=${currentTimePeriod.displayName}")
    
    // 计算当前时间范围内应该有的日记数量
    val currentTime = System.currentTimeMillis()
    val journalsInCurrentPeriod = when (currentTimePeriod) {
        SentimentViewModel.TimePeriod.ALL -> journals.size
        SentimentViewModel.TimePeriod.LAST_WEEK -> {
            journals.count { it.date?.time ?: 0 > currentTime - 7L * 24L * 60L * 60L * 1000L }
        }
        SentimentViewModel.TimePeriod.LAST_MONTH -> {
            journals.count { it.date?.time ?: 0 > currentTime - 30L * 24L * 60L * 60L * 1000L }
        }
        SentimentViewModel.TimePeriod.LAST_THREE_MONTHS -> {
            journals.count { it.date?.time ?: 0 > currentTime - 90L * 24L * 60L * 60L * 1000L }
        }
        SentimentViewModel.TimePeriod.LAST_YEAR -> {
            journals.count { it.date?.time ?: 0 > currentTime - 365L * 24L * 60L * 60L * 1000L }
        }
    }
    Log.d("SentimentReportScreen", "当前时间范围应有日记数=${journalsInCurrentPeriod}")
    
    // 计算情感分布统计
    val totalCount = results.size
    val positiveCount = results.count { it.second.sentimentType == SentimentType.POSITIVE }
    val negativeCount = results.count { it.second.sentimentType == SentimentType.NEGATIVE }
    val neutralCount = results.count { it.second.sentimentType == SentimentType.NEUTRAL }
    
    // 计算情感占比
    val positivePercentage = if (totalCount > 0) positiveCount * 100f / totalCount else 0f
    val negativePercentage = if (totalCount > 0) negativeCount * 100f / totalCount else 0f
    val neutralPercentage = if (totalCount > 0) neutralCount * 100f / totalCount else 0f
    
    // 计算最近7天的数据，只有在查看全部时间或时间段超过7天时才计算
    val oneWeekAgo = currentTime - 7L * 24L * 60L * 60L * 1000L
    val showRecentWeekSection = currentTimePeriod == SentimentViewModel.TimePeriod.ALL || 
        currentTimePeriod == SentimentViewModel.TimePeriod.LAST_MONTH || 
        currentTimePeriod == SentimentViewModel.TimePeriod.LAST_THREE_MONTHS || 
        currentTimePeriod == SentimentViewModel.TimePeriod.LAST_YEAR
    
    val recentResults = if (showRecentWeekSection) {
        results.filter { it.first.date?.time ?: 0 > oneWeekAgo }
    } else {
        emptyList()
    }
    
    // 最近情感统计
    val recentCount = recentResults.size
    val recentPositiveCount = recentResults.count { it.second.sentimentType == SentimentType.POSITIVE }
    val recentNegativeCount = recentResults.count { it.second.sentimentType == SentimentType.NEGATIVE }
    val recentNeutralCount = recentResults.count { it.second.sentimentType == SentimentType.NEUTRAL }
    
    // 最近情感占比
    val recentPositivePercentage = if (recentCount > 0) recentPositiveCount * 100f / recentCount else 0f
    val recentNegativePercentage = if (recentCount > 0) recentNegativeCount * 100f / recentCount else 0f
    val recentNeutralPercentage = if (recentCount > 0) recentNeutralCount * 100f / recentCount else 0f
    
    // 计算平均情感分数
    val avgPositiveScore = results.map { it.second.positiveScore }.average().toFloat()
    val avgNegativeScore = results.map { it.second.negativeScore }.average().toFloat()
    val recentAvgPositiveScore = if (recentResults.isNotEmpty()) 
        recentResults.map { it.second.positiveScore }.average().toFloat() else 0f
    val recentAvgNegativeScore = if (recentResults.isNotEmpty()) 
        recentResults.map { it.second.negativeScore }.average().toFloat() else 0f
    
    // 找出主要情绪
    val emotionGroups = results
        .filter { it.second.dominantEmotion.isNotBlank() }
        .groupBy { it.second.dominantEmotion }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 筛选结果为空的提示
        if (totalCount == 0) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentVeryDissatisfied,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "在${currentTimePeriod.displayName}内未找到情感分析结果",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请尝试更改时间范围或添加更多日记",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // 分析概况
        if (totalCount > 0) {
            item {
                ReportSection(
                    title = "情感分析概况",
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val journalsInPeriod = when (currentTimePeriod) {
                                SentimentViewModel.TimePeriod.ALL -> journals.size
                                SentimentViewModel.TimePeriod.LAST_WEEK -> {
                                    journals.count { it.date?.time ?: 0 > currentTime - 7L * 24L * 60L * 60L * 1000L }
                                }
                                SentimentViewModel.TimePeriod.LAST_MONTH -> {
                                    journals.count { it.date?.time ?: 0 > currentTime - 30L * 24L * 60L * 60L * 1000L }
                                }
                                SentimentViewModel.TimePeriod.LAST_THREE_MONTHS -> {
                                    journals.count { it.date?.time ?: 0 > currentTime - 90L * 24L * 60L * 60L * 1000L }
                                }
                                SentimentViewModel.TimePeriod.LAST_YEAR -> {
                                    journals.count { it.date?.time ?: 0 > currentTime - 365L * 24L * 60L * 60L * 1000L }
                                }
                            }
                            
                            // 添加信息卡片样式
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "已分析日记",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$totalCount / $journalsInPeriod",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Text(
                                            text = " (${currentTimePeriod.displayName})",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // 总体情感倾向
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    positivePercentage > neutralPercentage && positivePercentage > negativePercentage -> 
                                        SentimentColors.POSITIVE_LIGHT // 使用统一颜色
                                    negativePercentage > neutralPercentage && negativePercentage > positivePercentage -> 
                                        SentimentColors.NEGATIVE_LIGHT // 使用统一颜色
                                    else -> 
                                        SentimentColors.NEUTRAL_LIGHT // 使用统一颜色
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "总体情感倾向",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = getDominantSentimentType(positivePercentage, negativePercentage, neutralPercentage),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            positivePercentage > neutralPercentage && positivePercentage > negativePercentage -> 
                                                SentimentColors.POSITIVE // 使用统一颜色
                                            negativePercentage > neutralPercentage && negativePercentage > positivePercentage -> 
                                                SentimentColors.NEGATIVE // 使用统一颜色
                                            else -> 
                                                SentimentColors.NEUTRAL // 使用统一颜色
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 情感分布柱状图
                            Text("整体情感分布:", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                if (totalCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(positivePercentage)
                                            .fillMaxHeight()
                                            .background(SentimentColors.POSITIVE) // 使用统一颜色
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(neutralPercentage)
                                            .fillMaxHeight()
                                            .background(SentimentColors.NEUTRAL) // 使用统一颜色
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(negativePercentage)
                                            .fillMaxHeight()
                                            .background(SentimentColors.NEGATIVE) // 使用统一颜色
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(SentimentColors.PROGRESS_TRACK) // 使用统一颜色
                                    )
                                }
                            }
                            
                            // 情感分布图例
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LegendItem(
                                    color = SentimentColors.POSITIVE, // 使用统一颜色
                                    text = "积极: ${String.format("%.1f", positivePercentage)}%"
                                )
                                LegendItem(
                                    color = SentimentColors.NEUTRAL, // 使用统一颜色
                                    text = "中性: ${String.format("%.1f", neutralPercentage)}%"
                                )
                                LegendItem(
                                    color = SentimentColors.NEGATIVE, // 使用统一颜色
                                    text = "消极: ${String.format("%.1f", negativePercentage)}%"
                                )
                            }
                        }
                    }
                )
            }
        }
        
        // 最近7天情感报告，仅当选择的时间范围足够大时显示
        if (showRecentWeekSection && recentCount > 0) {
            item {
                ReportSection(
                    title = "最近7天情感报告",
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 统计信息卡片
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "最近7天日记数量",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "$recentCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Text(
                                text = "最近情感倾向: ${getDominantSentimentType(recentPositivePercentage, recentNegativePercentage, recentNeutralPercentage)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 最近情感分布柱状图
                            Text("最近情感分布:", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(recentPositivePercentage)
                                        .fillMaxHeight()
                                        .background(SentimentColors.POSITIVE) // 使用统一颜色
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(recentNeutralPercentage)
                                        .fillMaxHeight()
                                        .background(SentimentColors.NEUTRAL) // 使用统一颜色
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(recentNegativePercentage)
                                        .fillMaxHeight()
                                        .background(SentimentColors.NEGATIVE) // 使用统一颜色
                                )
                            }
                            
                            // 情感分布图例
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LegendItem(
                                    color = SentimentColors.POSITIVE, // 使用统一颜色
                                    text = "积极: ${String.format("%.1f", recentPositivePercentage)}%"
                                )
                                LegendItem(
                                    color = SentimentColors.NEUTRAL, // 使用统一颜色
                                    text = "中性: ${String.format("%.1f", recentNeutralPercentage)}%"
                                )
                                LegendItem(
                                    color = SentimentColors.NEGATIVE, // 使用统一颜色
                                    text = "消极: ${String.format("%.1f", recentNegativePercentage)}%"
                                )
                            }
                            
                            // 情感对比
                            if (totalCount > recentCount) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("与整体情感对比:", style = MaterialTheme.typography.titleSmall)
                                
                                val positiveChange = recentPositivePercentage - positivePercentage
                                val negativeChange = recentNegativePercentage - negativePercentage
                                val neutralChange = recentNeutralPercentage - neutralPercentage
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    SentimentChangeItem(
                                        label = "积极",
                                        change = positiveChange
                                    )
                                    SentimentChangeItem(
                                        label = "中性",
                                        change = neutralChange
                                    )
                                    SentimentChangeItem(
                                        label = "消极",
                                        change = negativeChange
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
        
        // 情感强度报告
        item {
            ReportSection(
                title = "情感强度分析",
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 积极情感强度 - 使用绿色
                        Text("积极情感强度: ${String.format("%.1f", avgPositiveScore * 100)}%")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SentimentColors.PROGRESS_TRACK)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(avgPositiveScore)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SentimentColors.POSITIVE)
                            )
                        }
                        
                        // 消极情感强度 - 使用红色
                        Text("消极情感强度: ${String.format("%.1f", avgNegativeScore * 100)}%")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SentimentColors.PROGRESS_TRACK)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(avgNegativeScore)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SentimentColors.NEGATIVE)
                            )
                        }
                        
                        // 最近7天情感强度，仅当选择的时间范围足够大时显示
                        if (showRecentWeekSection && recentCount > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("最近7天情感强度:", style = MaterialTheme.typography.titleSmall)
                            
                            // 最近7天积极情感
                            Text("积极情感: ${String.format("%.1f", recentAvgPositiveScore * 100)}%")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SentimentColors.PROGRESS_TRACK)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(recentAvgPositiveScore)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SentimentColors.POSITIVE)
                                )
                            }

                            
                            // 最近7天消极情感
                            Text("消极情感: ${String.format("%.1f", recentAvgNegativeScore * 100)}%")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SentimentColors.PROGRESS_TRACK)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(recentAvgNegativeScore)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SentimentColors.NEGATIVE)
                                )
                            }
                        }
                    }
                }
            )
        }
        
        // 主要情绪词统计
        item {
            ReportSection(
                title = "主要情绪词统计",
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 定义固定的五种情绪类型
                        val fixedEmotionTypes = listOf("非常正面", "正面", "中性", "负面", "非常负面")
                        
                        // 创建情绪类型映射，包含实际统计的情绪和固定情绪类型
                        val emotionMap = fixedEmotionTypes.associateWith { emotionType ->
                            emotionGroups.find { it.first == emotionType }?.second ?: 0
                        }.toList()
                        
                        // 显示所有固定情绪类型，包括数量为0的
                        emotionMap.forEach { (emotion, count) ->
                            val percentage = if (totalCount > 0) count * 100f / totalCount else 0f
                            
                            // 根据情绪类型选择颜色
                            val barColor = when (emotion) {
                                "非常正面" -> SentimentColors.POSITIVE
                                "正面" -> SentimentColors.POSITIVE.copy(alpha = 0.8f)
                                "中性" -> SentimentColors.NEUTRAL
                                "负面" -> SentimentColors.NEGATIVE.copy(alpha = 0.8f)
                                "非常负面" -> SentimentColors.NEGATIVE
                                else -> SentimentColors.NEUTRAL
                            }
                            
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = emotion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$count 次",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${String.format("%.1f", percentage)}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SentimentColors.PROGRESS_TRACK)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(percentage / 100f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                        
                        // 如果有其他情绪类型，也显示出来（不在固定五种类型中的）
                        val otherEmotions = emotionGroups.filter { (emotion, _) -> 
                            !fixedEmotionTypes.contains(emotion)
                        }
                        
                        if (otherEmotions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "其他情绪类型:",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            otherEmotions.forEach { (emotion, count) ->
                                val percentage = count * 100f / totalCount
                                val barColor = when {
                                    percentage > 60f -> SentimentColors.POSITIVE
                                    percentage > 30f -> SentimentColors.NEUTRAL
                                    else -> SentimentColors.NEGATIVE
                                }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = emotion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$count 次",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "(${String.format("%.1f", percentage)}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SentimentColors.PROGRESS_TRACK)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(percentage / 100f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(barColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * 报告章节组件
 */
@Composable
private fun ReportSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

/**
 * 图例项组件
 */
@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * 情感变化项组件
 */
@Composable
private fun SentimentChangeItem(
    label: String,
    change: Float
) {
    val changeText = if (change > 0) "+${String.format("%.1f", change)}%" 
                     else String.format("%.1f", change) + "%"
    val changeColor = when {
        change > 5 -> SentimentColors.POSITIVE // 明显上升-绿色
        change > 0 -> SentimentColors.POSITIVE_LIGHT // 轻微上升-浅绿色
        change < -5 -> SentimentColors.NEGATIVE // 明显下降-红色
        change < 0 -> SentimentColors.NEGATIVE_LIGHT // 轻微下降-橙色
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = changeColor.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = changeText,
                style = MaterialTheme.typography.bodyMedium,
                color = changeColor,
                fontWeight = FontWeight.Bold
            )
            
            // 添加箭头指示方向
            Icon(
                imageVector = if (change > 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = changeColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 获取主要情感类型
 */
private fun getDominantSentimentType(
    positivePercentage: Float,
    negativePercentage: Float,
    neutralPercentage: Float
): String {
    return when {
        positivePercentage >= negativePercentage && positivePercentage >= neutralPercentage -> 
            "积极"
        negativePercentage >= positivePercentage && negativePercentage >= neutralPercentage -> 
            "消极"
        else -> 
            "中性"
    }
} 