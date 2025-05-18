package ovo.sypw.journal.common.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 情感分析器
 * 使用TensorFlow Lite实现本地情感分析
 */
@Singleton
class SentimentAnalyzer @Inject constructor() {
    private val TAG = "SentimentAnalyzer"
    
    // 情感分类器
    private var classifier: NLClassifier? = null
    
    // 中文情感词典
    private var positiveWords: Set<String> = emptySet()
    private var negativeWords: Set<String> = emptySet()
    private var initialized = false
    
    // 情感类型
    enum class SentimentType {
        POSITIVE,    // 积极情感
        NEGATIVE,    // 消极情感
        NEUTRAL,     // 中性情感
        UNKNOWN      // 未知/错误
    }
    
    // 情感分析结果
    data class SentimentResult(
        val type: SentimentType,           // 情感类型
        val positiveScore: Float,          // 积极情感得分
        val negativeScore: Float,          // 消极情感得分
        val dominantEmotion: String = "",  // 主要情绪
        val confidence: Float = 0f         // 置信度
    )
    
    /**
     * 初始化情感分析器
     * @param context 上下文
     * @return 是否初始化成功
     */
    fun initialize(context: Context): Boolean {
        if (initialized) return true
        
        try {
            // 从assets加载模型文件
            classifier = NLClassifier.createFromFile(context, "sentiment_model.tflite")
            
            // 加载中文情感词典
            loadSentimentDictionary(context)
            
            initialized = true
            Log.d(TAG, "情感分析器初始化成功，词典大小：正面${positiveWords.size}，负面${negativeWords.size}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "初始化情感分析器失败", e)
            return false
        }
    }
    
    /**
     * 加载中文情感词典
     */
    private fun loadSentimentDictionary(context: Context) {
        try {
            val inputStream = context.assets.open("chinese_sentiment_words.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            
            val jsonObject = JSONObject(jsonString)
            
            // 读取积极词汇
            val positiveArray = jsonObject.getJSONArray("positive")
            val positiveList = mutableSetOf<String>()
            for (i in 0 until positiveArray.length()) {
                positiveList.add(positiveArray.getString(i))
            }
            positiveWords = positiveList
            
            // 读取消极词汇
            val negativeArray = jsonObject.getJSONArray("negative")
            val negativeList = mutableSetOf<String>()
            for (i in 0 until negativeArray.length()) {
                negativeList.add(negativeArray.getString(i))
            }
            negativeWords = negativeList
            
            Log.d(TAG, "情感词典加载成功")
        } catch (e: Exception) {
            Log.e(TAG, "加载情感词典失败", e)
            // 使用默认词典
            positiveWords = DEFAULT_POSITIVE_WORDS
            negativeWords = DEFAULT_NEGATIVE_WORDS
        }
    }
    
    /**
     * 分析文本情感
     * @param text 待分析文本
     * @return 情感分析结果
     */
    fun analyzeSentiment(text: String): SentimentResult {
        if (classifier == null) {
            Log.e(TAG, "情感分析器未初始化")
            return SentimentResult(SentimentType.UNKNOWN, 0f, 0f)
        }
        
        if (text.isBlank()) {
            return SentimentResult(SentimentType.NEUTRAL, 0.5f, 0.5f)
        }
        
        return try {
            // 执行TFLite模型分析
            val modelResults = classifier!!.classify(text)
            
            // 对分类结果进行排序
            val sortedCategories = modelResults.sortedByDescending { it.score }
            
            // 获取TFLite模型给出的分数
            val tfPositiveScore = getScoreForCategory(sortedCategories, "positive")
            val tfNegativeScore = getScoreForCategory(sortedCategories, "negative")
            
            // 增强：使用词典进行词频分析
            val (dictPositiveScore, dictNegativeScore) = analyzeSentimentWithDictionary(text)
            
            // 融合两种分析结果（70% TFLite + 30% 词典）
            val finalPositiveScore = tfPositiveScore * 0.7f + dictPositiveScore * 0.3f
            val finalNegativeScore = tfNegativeScore * 0.7f + dictNegativeScore * 0.3f
            
            // 根据综合得分确定情感类型
            val sentimentType = when {
                finalPositiveScore > finalNegativeScore + 0.1f -> SentimentType.POSITIVE
                finalNegativeScore > finalPositiveScore + 0.1f -> SentimentType.NEGATIVE
                else -> SentimentType.NEUTRAL
            }
            
            val confidence = getConfidence(finalPositiveScore, finalNegativeScore)
            val dominantEmotion = getDominantEmotion(text, sentimentType)
            
            SentimentResult(
                type = sentimentType,
                positiveScore = finalPositiveScore,
                negativeScore = finalNegativeScore,
                dominantEmotion = dominantEmotion,
                confidence = confidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "情感分析出错", e)
            SentimentResult(SentimentType.UNKNOWN, 0f, 0f)
        }
    }
    
    /**
     * 使用情感词典进行词频分析
     * @return Pair<正面得分, 负面得分>
     */
    private fun analyzeSentimentWithDictionary(text: String): Pair<Float, Float> {
        var positiveCount = 0
        var negativeCount = 0
        var totalCount = 0
        
        // 对文本进行简单分词（按字符分割）
        val words = text.replace("[\\p{P}\\s]".toRegex(), " ").split(" ")
        
        // 遍历每个词，计算情感词出现频率
        for (word in words) {
            if (word.isBlank()) continue
            totalCount++
            
            if (positiveWords.any { word.contains(it) }) {
                positiveCount++
            }
            
            if (negativeWords.any { word.contains(it) }) {
                negativeCount++
            }
        }
        
        // 计算正负向情感比例，避免除零错误
        val posScore = if (totalCount > 0) positiveCount.toFloat() / totalCount else 0f
        val negScore = if (totalCount > 0) negativeCount.toFloat() / totalCount else 0f
        
        // 归一化处理，确保分数在0-1之间
        val sum = posScore + negScore
        val normalizedPosScore = if (sum > 0) posScore / sum else 0.5f
        val normalizedNegScore = if (sum > 0) negScore / sum else 0.5f
        
        return Pair(normalizedPosScore, normalizedNegScore)
    }
    
    /**
     * 获取指定类别的得分
     */
    private fun getScoreForCategory(categories: List<Category>, targetLabel: String): Float {
        return categories.find { it.label.lowercase() == targetLabel.lowercase() }?.score ?: 0f
    }
    
    /**
     * 获取情感类型
     */
    private fun getSentimentType(categories: List<Category>): SentimentType {
        if (categories.isEmpty()) return SentimentType.UNKNOWN
        
        val topCategory = categories.first()
        return when {
            topCategory.score < 0.55f -> SentimentType.NEUTRAL
            topCategory.label.lowercase() == "positive" -> SentimentType.POSITIVE
            topCategory.label.lowercase() == "negative" -> SentimentType.NEGATIVE
            else -> SentimentType.UNKNOWN
        }
    }
    
    /**
     * 根据文本内容和情感类型识别主要情绪
     * 简单实现，实际可使用更复杂的情绪分析模型
     */
    private fun getDominantEmotion(text: String, sentimentType: SentimentType): String {
        val positiveEmotions = mapOf(
            "开心" to listOf("开心", "高兴", "欢乐", "喜悦", "快乐", "兴奋", "开怀"),
            "满足" to listOf("满足", "满意", "满心", "欣慰", "成就"),
            "感激" to listOf("感谢", "感恩", "感激", "谢谢", "祝福"),
            "自信" to listOf("自信", "坚定", "肯定", "确信", "相信"),
            "希望" to listOf("希望", "盼望", "期待", "憧憬", "向往"),
            "幸福" to listOf("幸福", "美好", "温馨", "甜蜜", "温暖")
        )
        
        val negativeEmotions = mapOf(
            "悲伤" to listOf("悲伤", "难过", "伤心", "哀愁", "哭泣", "痛苦"),
            "焦虑" to listOf("焦虑", "担心", "忧虑", "紧张", "不安"),
            "愤怒" to listOf("愤怒", "生气", "恼火", "烦躁", "气愤", "愤慨"),
            "失望" to listOf("失望", "沮丧", "绝望", "灰心", "丧气"),
            "疲惫" to listOf("疲惫", "疲劳", "累", "厌倦", "无力"),
            "孤独" to listOf("孤独", "寂寞", "孤单", "寂寥", "凄凉")
        )
        
        // 根据情感类型选择合适的情绪词典
        val emotionDict = when (sentimentType) {
            SentimentType.POSITIVE -> positiveEmotions
            SentimentType.NEGATIVE -> negativeEmotions
            else -> return ""
        }
        
        // 在文本中查找匹配的情绪词，记录出现频率最高的情绪
        val emotionCounts = mutableMapOf<String, Int>()
        
        for ((emotion, keywords) in emotionDict) {
            var count = 0
            for (keyword in keywords) {
                count += countOccurrences(text, keyword)
            }
            if (count > 0) {
                emotionCounts[emotion] = count
            }
        }
        
        // 返回出现频率最高的情绪，如果没有找到则返回默认值
        return emotionCounts.entries.maxByOrNull { it.value }?.key
            ?: if (sentimentType == SentimentType.POSITIVE) "积极" else "消极"
    }
    
    /**
     * 计算某个关键词在文本中出现的次数
     */
    private fun countOccurrences(text: String, word: String): Int {
        var count = 0
        var index = text.indexOf(word)
        while (index != -1) {
            count++
            index = text.indexOf(word, index + 1)
        }
        return count
    }
    
    /**
     * 计算置信度
     */
    private fun getConfidence(positiveScore: Float, negativeScore: Float): Float {
        return Math.abs(positiveScore - negativeScore)
    }
    
    /**
     * 释放资源
     */
    fun close() {
        try {
            classifier?.close()
            classifier = null
            initialized = false
        } catch (e: Exception) {
            Log.e(TAG, "关闭情感分析器出错", e)
        }
    }
    
    companion object {
        // 默认情感词库（当JSON加载失败时使用）
        private val DEFAULT_POSITIVE_WORDS = setOf(
            "开心", "高兴", "快乐", "满足", "感谢", "自信", "希望", "幸福"
        )
        
        private val DEFAULT_NEGATIVE_WORDS = setOf(
            "悲伤", "焦虑", "愤怒", "失望", "疲惫", "孤独"
        )
    }
} 