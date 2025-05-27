package ovo.sypw.journal.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.data.model.SentimentData

/**
 * 情感分析结果实体类，用于Room数据库存储
 * 通过外键关联到JournalEntity
 */
@Entity(
    tableName = "sentiments",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalId"],
            onDelete = ForeignKey.CASCADE // 当删除日记时级联删除情感分析结果
        )
    ],
    indices = [Index("journalId", unique = true)] // 为外键建立索引，并保证每篇日记只有一个情感分析结果
)
data class SentimentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val journalId: Int,                // 关联的日记ID
    val sentimentType: String,         // 情感类型，存储SentimentType枚举的名称
    val positiveScore: Float,          // 积极情感得分 (0.0-1.0)
    val negativeScore: Float,          // 消极情感得分 (0.0-1.0)
    val dominantEmotion: String,       // 主要情绪
    val confidence: Float,             // 置信度
    val timestamp: Long                // 分析时间戳
) {
    /**
     * 转换为SentimentData对象
     */
    fun toSentimentData(): SentimentData {
        return SentimentData(
            journalId = journalId,
            sentimentType = SentimentType.valueOf(sentimentType),
            positiveScore = positiveScore,
            negativeScore = negativeScore,
            dominantEmotion = dominantEmotion,
            confidence = confidence,
            timestamp = timestamp
        )
    }

    companion object {
        /**
         * 从SentimentData创建实体对象
         */
        fun fromSentimentData(sentimentData: SentimentData): SentimentEntity {
            return SentimentEntity(
                journalId = sentimentData.journalId,
                sentimentType = sentimentData.sentimentType.name,
                positiveScore = sentimentData.positiveScore,
                negativeScore = sentimentData.negativeScore,
                dominantEmotion = sentimentData.dominantEmotion,
                confidence = sentimentData.confidence,
                timestamp = sentimentData.timestamp
            )
        }
    }
} 