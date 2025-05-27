package ovo.sypw.journal.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovo.sypw.journal.common.utils.SentimentApiService
import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData
import ovo.sypw.journal.data.repository.SentimentRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * 情感分析ViewModel
 * 用于处理日记情感分析的业务逻辑
 */
@HiltViewModel
class SentimentViewModel @Inject constructor(
    private val sentimentApiService: SentimentApiService,
    private val sentimentRepository: SentimentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val TAG = "SentimentViewModel"
    
    // 情感分析结果缓存，避免重复分析
    private val sentimentCache = ConcurrentHashMap<Int, SentimentData>()
    
    // 当前选中日记的情感分析结果
    private val _selectedJournalSentiment = MutableStateFlow<SentimentData?>(null)
    val selectedJournalSentiment: StateFlow<SentimentData?> = _selectedJournalSentiment.asStateFlow()
    
    // 分析状态
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    // 批量分析进度
    private val _batchAnalysisProgress = MutableStateFlow(0f)
    val batchAnalysisProgress: StateFlow<Float> = _batchAnalysisProgress.asStateFlow()
    
    // 情感过滤类型
    private val _currentFilter = MutableStateFlow<SentimentType?>(null)
    val currentFilter: StateFlow<SentimentType?> = _currentFilter.asStateFlow()
    
    // 时间周期过滤
    private val _currentTimePeriod = MutableStateFlow(TimePeriod.ALL)
    val currentTimePeriod: StateFlow<TimePeriod> = _currentTimePeriod.asStateFlow()
    
    // 过滤后的结果
    private val _filteredResults = MutableStateFlow<List<Pair<JournalData, SentimentData>>>(emptyList())
    val filteredResults: StateFlow<List<Pair<JournalData, SentimentData>>> = _filteredResults.asStateFlow()
    
    // 时间周期枚举
    enum class TimePeriod(val displayName: String) {
        ALL("全部时间"),
        LAST_WEEK("最近一周"),
        LAST_MONTH("最近一个月"),
        LAST_THREE_MONTHS("最近三个月"),
        LAST_YEAR("最近一年")
    }
    
    init {
        // 初始化时从数据库加载情感分析结果
        loadSentimentsFromDatabase()
    }
    
    /**
     * 从数据库加载情感分析结果到缓存
     */
    private fun loadSentimentsFromDatabase() {
        viewModelScope.launch {
            try {
                val sentiments = sentimentRepository.getAllSentiments()
                sentiments.forEach { sentiment ->
                    sentimentCache[sentiment.journalId] = sentiment
                }
                Log.d(TAG, "从数据库加载了${sentiments.size}条情感分析结果")
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("加载情感分析数据失败: ${e.message}")
            }
        }
    }
    
    /**
     * 从数据库加载日记与情感分析结果，并直接更新过滤结果
     * @param journals 日记列表，用于过滤结果
     */
    fun loadJournalsWithSentiments(journals: List<JournalData>) {
        viewModelScope.launch {
            try {
                val journalMap = journals.associateBy { it.id }
                val results = sentimentRepository.getJournalsWithSentiments()
                
                // 更新缓存
                results.forEach { (journal, sentiment) ->
                    if (sentiment != null) {
                        sentimentCache[journal.id] = sentiment
                        Log.d(TAG, "加载情感分析结果: journalId=${journal.id}, type=${sentiment.sentimentType}")
                    }
                }
                
                // 统计有情感分析结果的日记数量
                val sentimentCount = results.count { it.second != null }
                Log.d(TAG, "从数据库加载了${results.size}条日记，其中${sentimentCount}条有情感分析结果")
                
                // 调试日记日期信息
                debugJournalDates(journals)
                
                // 根据当前过滤器更新结果
                updateFilteredResults(journals)
            } catch (e: Exception) {
                // 只处理非取消异常
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "加载日记与情感分析数据失败", e)
                    SnackBarUtils.showSnackBar("加载日记与情感分析数据失败: ${e.message}")
                } else {
                    Log.d(TAG, "情感分析数据加载已取消")
                }
            }
        }
    }
    
    /**
     * 调试日记日期信息
     */
    private fun debugJournalDates(journals: List<JournalData>) {
        val currentTime = System.currentTimeMillis()
        val oneMonthAgo = currentTime - 30 * 24 * 60 * 60 * 1000L
        val threeMonthsAgo = currentTime - 90 * 24 * 60 * 60 * 1000L
        
        Log.d(TAG, "====== 日记日期调试信息 ======")
        Log.d(TAG, "当前时间: $currentTime (${java.util.Date(currentTime)})")
        Log.d(TAG, "一个月前: $oneMonthAgo (${java.util.Date(oneMonthAgo)})")
        Log.d(TAG, "三个月前: $threeMonthsAgo (${java.util.Date(threeMonthsAgo)})")
        
        journals.forEach { journal ->
            val date = journal.date
            val time = date?.time ?: 0
            val isInOneMonth = time > oneMonthAgo
            val isInThreeMonths = time > threeMonthsAgo
            
            Log.d(TAG, "日记ID=${journal.id}, 日期=${date}, 时间戳=${time}")
            Log.d(TAG, "  一个月内=${isInOneMonth}, 三个月内=${isInThreeMonths}")
        }
    }
    
    /**
     * 分析单篇日记的情感
     * @param journal 日记数据
     * @param forceReAnalyze 是否强制重新分析（忽略缓存）
     */
    fun analyzeSentiment(journal: JournalData, forceReAnalyze: Boolean = false) {
        if (journal.id == 0 || journal.text.isNullOrBlank()) {
            _selectedJournalSentiment.value = SentimentData.createNeutral(journal.id)
            return
        }
        
        // 检查缓存
        if (!forceReAnalyze && sentimentCache.containsKey(journal.id)) {
            _selectedJournalSentiment.value = sentimentCache[journal.id]
            return
        }
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            
            try {
                // 尝试从数据库加载
                if (!forceReAnalyze) {
                    val savedSentiment = sentimentRepository.getSentimentByJournalId(journal.id)
                    if (savedSentiment != null) {
                        sentimentCache[journal.id] = savedSentiment
                        _selectedJournalSentiment.value = savedSentiment
                        _isAnalyzing.value = false
                        return@launch
                    }
                }
                
                // 调用API进行分析
                try {
                    val result = sentimentApiService.analyzeSentiment(journal.text)
                    
                    // 创建分析结果数据对象
                    val sentimentData = SentimentData.fromApiResult(journal.id, result)
                    
                    // 更新状态和缓存
                    _selectedJournalSentiment.value = sentimentData
                    sentimentCache[journal.id] = sentimentData
                    
                    // 保存到数据库
                    sentimentRepository.saveSentiment(sentimentData)
                    
                } catch (e: IllegalStateException) {
                    // API密钥未设置，提示用户
                    _selectedJournalSentiment.value = SentimentData.createNeutral(journal.id)
                    SnackBarUtils.showSnackBar("请在设置中配置情感分析API密钥")
                }
            } catch (e: Exception) {
                _selectedJournalSentiment.value = SentimentData.createNeutral(journal.id)
                SnackBarUtils.showSnackBar("分析过程中出现错误: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * 批量分析多篇日记的情感（单次请求形式）
     * @param journals 日记列表
     * @param forceReAnalyze 是否强制重新分析
     * @param delayMillis 每次请求之间的延迟时间（毫秒）
     */
    fun batchAnalyzeSentiment(
        journals: List<JournalData>,
        forceReAnalyze: Boolean = false,
        delayMillis: Long = 500
    ) {
        if (journals.isEmpty()) {
            SnackBarUtils.showSnackBar("没有可分析的日记")
            return
        }
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _batchAnalysisProgress.value = 0f
            
            try {
                // 检查API密钥是否已设置
                try {
                    if (journals.isNotEmpty() && journals[0].text?.isNotBlank() == true) {
                        // 只检查密钥，不需要真正分析
                        sentimentApiService.getApiKey() ?: throw IllegalStateException("API密钥未设置")
                    }
                    
                    // 如果不强制重新分析，先加载已有的数据库记录
                    if (!forceReAnalyze) {
                        val allSentiments = sentimentRepository.getAllSentiments()
                        allSentiments.forEach { sentiment ->
                            sentimentCache[sentiment.journalId] = sentiment
                        }
                        Log.d(TAG, "从数据库加载了${allSentiments.size}条情感分析结果")
                    }
                    
                    // 过滤出需要分析的日记
                    val journalsToAnalyze = journals.filter { journal -> 
                        journal.text?.isNotBlank() == true && 
                        (forceReAnalyze || !sentimentCache.containsKey(journal.id)) 
                    }
                    
                    Log.d(TAG, "需要分析的日记数量: ${journalsToAnalyze.size}")
                    
                    // 已有缓存的日记直接加载
                    val cachedJournals = journals.filter { journal ->
                        !forceReAnalyze && sentimentCache.containsKey(journal.id)
                    }
                    
                    Log.d(TAG, "已有缓存的日记数量: ${cachedJournals.size}")
                    
                    // 计算总日记数（包括缓存的和需要分析的）
                    val totalJournals = journals.size
                    var processedCount = cachedJournals.size
                    
                    // 更新进度
                    _batchAnalysisProgress.value = processedCount.toFloat() / totalJournals
                    
                    // 如果有需要分析的日记
                    if (journalsToAnalyze.isNotEmpty()) {
                        // 逐个处理每篇日记
                        for ((index, journal) in journalsToAnalyze.withIndex()) {
                            if (journal.text.isNullOrBlank()) {
                                Log.w(TAG, "日记 ${index + 1}/${journalsToAnalyze.size} 没有有效的文本内容")
                                continue
                            }
                            
                            Log.d(TAG, "开始处理日记 ${index + 1}/${journalsToAnalyze.size}, id=${journal.id}")
                            
                            try {
                                // 单次分析
                                val result = sentimentApiService.analyzeSentiment(journal.text)
                                val sentimentData = SentimentData.fromApiResult(journal.id, result)
                                
                                // 更新缓存
                                sentimentCache[journal.id] = sentimentData
                                
                                // 如果是当前选中的日记，更新选中状态
                                if (_selectedJournalSentiment.value?.journalId == journal.id) {
                                    _selectedJournalSentiment.value = sentimentData
                                }
                                
                                // 保存到数据库
                                sentimentRepository.saveSentiment(sentimentData)
                                Log.d(TAG, "成功保存日记${journal.id}的情感分析结果: ${sentimentData.sentimentType}")
                                
                                // 更新处理计数和进度
                                processedCount++
                                _batchAnalysisProgress.value = processedCount.toFloat() / totalJournals
                                
                                // 每篇日记处理完成后都更新界面
                                updateFilteredResults(journals)
                                
                                // 不是最后一篇，延迟后继续
                                if (index < journalsToAnalyze.size - 1) {
                                    delay(delayMillis)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "日记 ${index + 1} 处理失败", e)
                                SnackBarUtils.showSnackBar("日记 ${index + 1} 处理失败: ${e.message}")
                                // 继续处理下一篇，不中断整个流程
                            }
                        }
                    }
                    
                    // 分析完成，更新界面
                    updateFilteredResults(journals)
                    SnackBarUtils.showSnackBar("分析完成，共分析${journalsToAnalyze.size}篇日记")
                    
                } catch (e: IllegalStateException) {
                    // API密钥未设置，提示用户
                    SnackBarUtils.showSnackBar("请在设置中配置情感分析API密钥")
                    // 使用中性情感作为默认结果
                    journals.forEach { journal ->
                        if (!sentimentCache.containsKey(journal.id)) {
                            sentimentCache[journal.id] = SentimentData.createNeutral(journal.id)
                        }
                    }
                    _batchAnalysisProgress.value = 1f
                    updateFilteredResults(journals)
                }
            } catch (e: Exception) {
                Log.e(TAG, "批量分析过程中出现错误", e)
                SnackBarUtils.showSnackBar("分析过程中出现错误: ${e.message}")
            } finally {
                _isAnalyzing.value = false
                _batchAnalysisProgress.value = 1f
            }
        }
    }
    
    /**
     * 设置情感过滤类型
     */
    fun setFilter(type: SentimentType?) {
        _currentFilter.value = type
    }
    
    /**
     * 设置时间周期过滤
     */
    fun setTimePeriod(period: TimePeriod) {
        _currentTimePeriod.value = period
    }
    
    /**
     * 根据当前的过滤类型更新过滤结果
     */
    fun updateFilteredResults(journals: List<JournalData>) {
        val filter = _currentFilter.value
        val timePeriod = _currentTimePeriod.value
        val journalMap = journals.associateBy { it.id }
        
        // 先按时间过滤日记
        val timeFilteredJournals = filterJournalsByTimePeriod(journals, timePeriod)
        val timeFilteredJournalIds = timeFilteredJournals.map { it.id }.toSet()
        
        val filtered = if (filter == null) {
            // 不按情感类型过滤，返回所有有情感分析结果且在时间范围内的日记
            sentimentCache.entries
                .filter { entry -> timeFilteredJournalIds.contains(entry.key) }
                .mapNotNull { entry -> 
                    journalMap[entry.key]?.let { journal -> 
                        Pair(journal, entry.value)
                    }
                }
        } else {
            // 按情感类型和时间过滤
            sentimentCache.entries
                .filter { entry -> 
                    timeFilteredJournalIds.contains(entry.key) && 
                    entry.value.sentimentType == filter 
                }
                .mapNotNull { entry -> 
                    journalMap[entry.key]?.let { journal -> 
                        Pair(journal, entry.value)
                    }
                }
        }
        
        // 按情感强度排序（从高到低）
        val sortedResults = filtered.sortedByDescending { 
            when (it.second.sentimentType) {
                SentimentType.POSITIVE -> it.second.positiveScore
                SentimentType.NEGATIVE -> it.second.negativeScore
                else -> it.second.confidence
            }
        }
        
        _filteredResults.value = sortedResults
        Log.d(TAG, "过滤结果更新: 共${sortedResults.size}条结果，时间周期：${timePeriod.displayName}")
    }
    
    /**
     * 根据时间周期过滤日记
     */
    private fun filterJournalsByTimePeriod(journals: List<JournalData>, period: TimePeriod): List<JournalData> {
        if (period == TimePeriod.ALL) {
            return journals
        }
        
        val currentTime = System.currentTimeMillis()
        
        // 确保使用Long类型避免整数溢出
        val msInDay = 24L * 60L * 60L * 1000L
        val cutoffTime = when (period) {
            TimePeriod.LAST_WEEK -> currentTime - 7L * msInDay
            TimePeriod.LAST_MONTH -> currentTime - 30L * msInDay
            TimePeriod.LAST_THREE_MONTHS -> currentTime - 90L * msInDay
            TimePeriod.LAST_YEAR -> currentTime - 365L * msInDay
            else -> 0L
        }
        
        // 输出调试信息，查看过滤时间点
        Log.d(TAG, "过滤时间点: 当前时间=${currentTime}, 截止时间=${cutoffTime}, 周期=${period.displayName}")
        Log.d(TAG, "过滤时间点(可读): 当前=${java.util.Date(currentTime)}, 截止=${java.util.Date(cutoffTime)}")
        
        // 输出每一个截止时间点，便于调试
        Log.d(TAG, "一周前: ${currentTime - 7L * msInDay} (${java.util.Date(currentTime - 7L * msInDay)})")
        Log.d(TAG, "一月前: ${currentTime - 30L * msInDay} (${java.util.Date(currentTime - 30L * msInDay)})")
        Log.d(TAG, "三月前: ${currentTime - 90L * msInDay} (${java.util.Date(currentTime - 90L * msInDay)})")
        Log.d(TAG, "一年前: ${currentTime - 365L * msInDay} (${java.util.Date(currentTime - 365L * msInDay)})")
        
        // 过滤日记
        val filteredJournals = journals.filter { journal ->
            val journalTime = journal.date?.time ?: 0L
            
            // 确保这里使用Long类型比较
            val isInRange = journalTime > cutoffTime
            
            // 记录每篇日记的筛选结果，帮助调试
            if (period == TimePeriod.LAST_MONTH || period == TimePeriod.LAST_THREE_MONTHS) {
                Log.d(TAG, "日记ID=${journal.id}, 时间=${journalTime} (${journal.date}), 是否在范围内=${isInRange}")
                Log.d(TAG, "  时间差=${currentTime - journalTime}毫秒, 约${(currentTime - journalTime) / msInDay}天")
            }
            
            isInRange
        }
        
        // 记录筛选结果
        Log.d(TAG, "时间筛选结果: 共${journals.size}篇日记，筛选后${filteredJournals.size}篇，周期=${period.displayName}")
        
        return filteredJournals
    }
    
    /**
     * 获取情感分析结果列表（按情感类型过滤）
     * @param type 情感类型，null表示不过滤
     */
    fun getSentimentByType(type: SentimentType?): List<SentimentData> {
        return sentimentCache.values.filter { 
            type == null || it.sentimentType == type 
        }.toList()
    }
    
    /**
     * 获取情感分布统计
     * @return 各情感类型的数量和百分比
     */
    fun getSentimentDistribution(): Map<SentimentType, Pair<Int, Float>> {
        val total = sentimentCache.size
        if (total == 0) return emptyMap()
        
        val distribution = mutableMapOf<SentimentType, Int>()
        
        // 统计各类型数量
        sentimentCache.values.forEach { sentiment ->
            distribution[sentiment.sentimentType] = distribution.getOrDefault(sentiment.sentimentType, 0) + 1
        }
        
        // 计算百分比
        return distribution.mapValues { (_, count) ->
            Pair(count, count.toFloat() / total)
        }
    }
    
    /**
     * 清除情感分析缓存和数据库
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                sentimentRepository.deleteAllSentiments()
                sentimentCache.clear()
                _selectedJournalSentiment.value = null
                _filteredResults.value = emptyList()
                SnackBarUtils.showSnackBar("情感分析数据已清除")
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("清除情感分析数据失败: ${e.message}")
            }
        }
    }
    
    /**
     * 清除单个日记的情感分析缓存和数据库记录
     */
    fun clearJournalCache(journalId: Int) {
        viewModelScope.launch {
            try {
                sentimentRepository.deleteSentimentByJournalId(journalId)
                sentimentCache.remove(journalId)
                if (_selectedJournalSentiment.value?.journalId == journalId) {
                    _selectedJournalSentiment.value = null
                }
                
                // 更新过滤结果
                _filteredResults.value = _filteredResults.value.filter { it.first.id != journalId }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("清除日记情感分析数据失败: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        sentimentCache.clear()
    }
} 