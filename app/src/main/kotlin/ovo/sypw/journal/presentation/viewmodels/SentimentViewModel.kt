package ovo.sypw.journal.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovo.sypw.journal.common.utils.SentimentAnalyzer
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * 情感分析ViewModel
 * 用于处理日记情感分析的业务逻辑
 */
@HiltViewModel
class SentimentViewModel @Inject constructor(
    private val sentimentAnalyzer: SentimentAnalyzer,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
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
    private val _currentFilter = MutableStateFlow<SentimentAnalyzer.SentimentType?>(null)
    val currentFilter: StateFlow<SentimentAnalyzer.SentimentType?> = _currentFilter.asStateFlow()
    
    // 过滤后的结果
    private val _filteredResults = MutableStateFlow<List<Pair<JournalData, SentimentData>>>(emptyList())
    val filteredResults: StateFlow<List<Pair<JournalData, SentimentData>>> = _filteredResults.asStateFlow()
    
    // 初始化状态
    private var isInitialized = false
    
    /**
     * 确保情感分析器已初始化
     */
    private fun ensureInitialized(): Boolean {
        if (isInitialized) return true
        
        try {
            val result = sentimentAnalyzer.initialize(context)
            isInitialized = result
            if (!result) {
                SnackBarUtils.showSnackBar("情感分析器初始化失败")
            }
            return result
        } catch (e: Exception) {
            SnackBarUtils.showSnackBar("情感分析器初始化出错: ${e.message}")
            return false
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
                // 确保初始化
                val initialized = ensureInitialized()
                if (!initialized) {
                    _selectedJournalSentiment.value = SentimentData.createNeutral(journal.id)
                    return@launch
                }
                
                // 在IO线程执行分析
                val result = withContext(Dispatchers.IO) {
                    sentimentAnalyzer.analyzeSentiment(journal.text)
                }
                
                // 创建分析结果数据对象
                val sentimentData = SentimentData.fromResult(journal.id, result)
                
                // 更新状态和缓存
                _selectedJournalSentiment.value = sentimentData
                sentimentCache[journal.id] = sentimentData
            } catch (e: Exception) {
                _selectedJournalSentiment.value = SentimentData.createNeutral(journal.id)
                SnackBarUtils.showSnackBar("分析过程中出现错误: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * 批量分析多篇日记的情感
     * @param journals 日记列表
     * @param forceReAnalyze 是否强制重新分析
     */
    fun batchAnalyzeSentiment(
        journals: List<JournalData>,
        forceReAnalyze: Boolean = false
    ) {
        if (journals.isEmpty()) {
            SnackBarUtils.showSnackBar("没有可分析的日记")
            return
        }
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _batchAnalysisProgress.value = 0f
            
            try {
                // 确保初始化
                val initialized = ensureInitialized()
                if (!initialized) {
                    SnackBarUtils.showSnackBar("情感分析器初始化失败，无法进行批量分析")
                    return@launch
                }
                
                val results = withContext(Dispatchers.Default) {
                    val totalJournals = journals.size
                    val resultMap = mutableMapOf<Int, SentimentData>()
                    
                    journals.forEachIndexed { index, journal ->
                        if (journal.text.isNullOrBlank()) {
                            resultMap[journal.id] = SentimentData.createNeutral(journal.id)
                        } else if (!forceReAnalyze && sentimentCache.containsKey(journal.id)) {
                            resultMap[journal.id] = sentimentCache[journal.id]!!
                        } else {
                            try {
                                val result = sentimentAnalyzer.analyzeSentiment(journal.text)
                                val sentimentData = SentimentData.fromResult(journal.id, result)
                                resultMap[journal.id] = sentimentData
                                sentimentCache[journal.id] = sentimentData
                            } catch (e: Exception) {
                                resultMap[journal.id] = SentimentData.createNeutral(journal.id)
                            }
                        }
                        
                        // 更新进度
                        _batchAnalysisProgress.value = (index + 1).toFloat() / totalJournals
                    }
                    
                    resultMap
                }
                
                // 分析完成
                updateFilteredResults(journals)
                SnackBarUtils.showSnackBar("分析完成，共分析${journals.size}篇日记")
            } catch (e: Exception) {
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
    fun setFilter(type: SentimentAnalyzer.SentimentType?) {
        _currentFilter.value = type
    }
    
    /**
     * 根据当前的过滤类型更新过滤结果
     */
    fun updateFilteredResults(journals: List<JournalData>) {
        val filter = _currentFilter.value
        val journalMap = journals.associateBy { it.id }
        
        val filtered = if (filter == null) {
            // 不过滤，返回所有有情感分析结果的日记
            sentimentCache.entries
                .mapNotNull { entry -> 
                    journalMap[entry.key]?.let { journal -> 
                        Pair(journal, entry.value)
                    }
                }
        } else {
            // 按情感类型过滤
            sentimentCache.entries
                .filter { it.value.sentimentType == filter }
                .mapNotNull { entry -> 
                    journalMap[entry.key]?.let { journal -> 
                        Pair(journal, entry.value)
                    }
                }
        }
        
        // 按情感强度排序（从高到低）
        val sortedResults = filtered.sortedByDescending { 
            when (it.second.sentimentType) {
                SentimentAnalyzer.SentimentType.POSITIVE -> it.second.positiveScore
                SentimentAnalyzer.SentimentType.NEGATIVE -> it.second.negativeScore
                else -> it.second.confidence
            }
        }
        
        _filteredResults.value = sortedResults
    }
    
    /**
     * 获取情感分析结果列表（按情感类型过滤）
     * @param type 情感类型，null表示不过滤
     */
    fun getSentimentByType(type: SentimentAnalyzer.SentimentType?): List<SentimentData> {
        return sentimentCache.values.filter { 
            type == null || it.sentimentType == type 
        }.toList()
    }
    
    /**
     * 获取情感分布统计
     * @return 各情感类型的数量和百分比
     */
    fun getSentimentDistribution(): Map<SentimentAnalyzer.SentimentType, Pair<Int, Float>> {
        val total = sentimentCache.size
        if (total == 0) return emptyMap()
        
        val distribution = mutableMapOf<SentimentAnalyzer.SentimentType, Int>()
        
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
     * 清除情感分析缓存
     */
    fun clearCache() {
        sentimentCache.clear()
        _selectedJournalSentiment.value = null
        _filteredResults.value = emptyList()
    }
    
    /**
     * 清除单个日记的情感分析缓存
     */
    fun clearJournalCache(journalId: Int) {
        sentimentCache.remove(journalId)
        if (_selectedJournalSentiment.value?.journalId == journalId) {
            _selectedJournalSentiment.value = null
        }
        
        // 更新过滤结果
        _filteredResults.value = _filteredResults.value.filter { it.first.id != journalId }
    }
    
    override fun onCleared() {
        super.onCleared()
        sentimentCache.clear()
    }
} 