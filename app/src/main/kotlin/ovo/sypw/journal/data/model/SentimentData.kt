package ovo.sypw.journal.data.model

import ovo.sypw.journal.common.utils.SentimentAnalyzer

/**
 * 情感分析数据类
 * 存储日记的情感分析结果
 */
data class SentimentData(
    val journalId: Int,                             // 日记ID
    val sentimentType: SentimentAnalyzer.SentimentType, // 情感类型
    val positiveScore: Float,                       // 积极情感得分 (0.0-1.0)
    val negativeScore: Float,                       // 消极情感得分 (0.0-1.0)
    val dominantEmotion: String,                    // 主要情绪
    val confidence: Float,                          // 置信度
    val timestamp: Long = System.currentTimeMillis() // 分析时间戳
) {
    companion object {
        /**
         * 从情感分析结果创建数据对象
         */
        fun fromResult(journalId: Int, result: SentimentAnalyzer.SentimentResult): SentimentData {
            return SentimentData(
                journalId = journalId,
                sentimentType = result.type,
                positiveScore = result.positiveScore,
                negativeScore = result.negativeScore,
                dominantEmotion = result.dominantEmotion,
                confidence = result.confidence
            )
        }
        
        /**
         * 创建一个中性的默认分析结果
         */
        fun createNeutral(journalId: Int): SentimentData {
            return SentimentData(
                journalId = journalId,
                sentimentType = SentimentAnalyzer.SentimentType.NEUTRAL,
                positiveScore = 0.5f,
                negativeScore = 0.5f,
                dominantEmotion = "",
                confidence = 0f
            )
        }
    }
    
    /**
     * 获取情感状态描述
     */
    fun getSentimentDescription(): String {
        return when (sentimentType) {
            SentimentAnalyzer.SentimentType.POSITIVE -> "积极"
            SentimentAnalyzer.SentimentType.NEGATIVE -> "消极"
            SentimentAnalyzer.SentimentType.NEUTRAL -> "中性"
            SentimentAnalyzer.SentimentType.UNKNOWN -> "未知"
        }
    }
    
    /**
     * 获取情绪描述
     */
    fun getEmotionDescription(): String {
        return if (dominantEmotion.isNotEmpty()) {
            dominantEmotion
        } else {
            getSentimentDescription()
        }
    }
    
    /**
     * 获取可视化显示的积极情感百分比
     */
    fun getPositivePercentage(): Int {
        return (positiveScore * 100).toInt()
    }
    
    /**
     * 获取可视化显示的消极情感百分比
     */
    fun getNegativePercentage(): Int {
        return (negativeScore * 100).toInt()
    }
} 