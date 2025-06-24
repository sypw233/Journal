package ovo.sypw.journal.data.database

import androidx.room.Embedded
import androidx.room.Relation
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData

/**
 * 日记与情感分析结果的联合查询结果
 * 使用Room的@Embedded和@Relation注解表示一对一关系
 */
data class JournalWithSentiment(
    @Embedded
    val journal: JournalEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "journalId"
    )
    val sentiment: SentimentEntity?
) {
    /**
     * 转换为业务层模型
     */
    fun toModel(): Pair<JournalData, SentimentData?> {
        return Pair(
            journal.toJournalData(),
            sentiment?.toSentimentData()
        )
    }
} 