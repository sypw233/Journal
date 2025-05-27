package ovo.sypw.journal.data.model

import ovo.sypw.journal.common.utils.SentimentApiService
import ovo.sypw.journal.common.utils.SentimentType

/**
 * 情感分析数据类
 * 存储日记的情感分析结果
 */
data class SentimentData(
    val journalId: Int,                             // 日记ID
    val sentimentType: SentimentType,               // 情感类型
    val positiveScore: Float,                       // 积极情感得分 (0.0-1.0)
    val negativeScore: Float,                       // 消极情感得分 (0.0-1.0)
    val dominantEmotion: String,                    // 主要情绪
    val confidence: Float,                          // 置信度
    val timestamp: Long = System.currentTimeMillis() // 分析时间戳
) {
    companion object {

        /**
         * 从API情感分析结果创建数据对象
         */
        fun fromApiResult(journalId: Int, result: SentimentApiService.SentimentResult): SentimentData {
            // 根据API返回的score确定情感类型
            val type = when {
                result.score > 80 -> SentimentType.POSITIVE
                result.score > 60 -> SentimentType.POSITIVE
                result.score > 40 -> SentimentType.NEUTRAL
                result.score > 20 -> SentimentType.NEGATIVE
                else -> SentimentType.NEGATIVE
            }
            
            // 计算正负面得分（API只返回一个总分）
            val normalizedScore = result.score / 100f
            val positiveScore = if (result.score > 50) normalizedScore else 1f - normalizedScore
            val negativeScore = if (result.score <= 50) normalizedScore else 1f - normalizedScore
            
            return SentimentData(
                journalId = journalId,
                sentimentType = type,
                positiveScore = positiveScore,
                negativeScore = negativeScore,
                dominantEmotion = result.label,
                confidence = Math.abs(2 * normalizedScore - 1f) // 置信度：越接近0或1越高
            )
        }
        
        /**
         * 创建一个中性的默认分析结果
         */
        fun createNeutral(journalId: Int): SentimentData {
            return SentimentData(
                journalId = journalId,
                sentimentType = SentimentType.NEUTRAL,
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
            SentimentType.POSITIVE -> "积极"
            SentimentType.NEGATIVE -> "消极"
            SentimentType.NEUTRAL -> "中性"
            else -> "未知"
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