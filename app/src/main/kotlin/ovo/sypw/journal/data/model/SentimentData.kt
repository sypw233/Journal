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
            // 计算正负面得分（API只返回一个总分）
            val normalizedScore = result.score / 100f
            
            // 修复正负面得分的计算逻辑
            // API分数直接作为积极得分比例，消极得分为1减去积极得分
            val positiveScore = normalizedScore
            val negativeScore = 1f - normalizedScore
            
            // 重新计算情感类型，确保与UI显示一致
            val type = when {
                negativeScore >= 0.8f -> SentimentType.NEGATIVE    // 负面得分>=80%时为强烈消极
                negativeScore >= 0.6f -> SentimentType.NEGATIVE    // 负面得分>=60%时为消极
                negativeScore > 0.4f && negativeScore < 0.6f -> SentimentType.NEUTRAL // 40-60%为中性
                positiveScore >= 0.6f -> SentimentType.POSITIVE    // 积极得分>=60%时为积极
                else -> SentimentType.NEUTRAL                      // 其它情况为中性
            }
            
            // 创建SentimentData对象，对于消极类型进行特殊处理
            return if (type == SentimentType.NEGATIVE) {
                // 消极类型下，交换积极和消极得分以符合UI显示习惯
                SentimentData(
                    journalId = journalId,
                    sentimentType = type,
                    positiveScore = negativeScore,  // 显示消极得分为主要得分
                    negativeScore = positiveScore,  // 显示积极得分为次要得分
                    dominantEmotion = result.label,
                    confidence = Math.abs(2 * normalizedScore - 1f)
                )
            } else {
                // 其他类型保持原有计算
                SentimentData(
                    journalId = journalId,
                    sentimentType = type,
                    positiveScore = positiveScore,
                    negativeScore = negativeScore,
                    dominantEmotion = result.label,
                    confidence = Math.abs(2 * normalizedScore - 1f)
                )
            }
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