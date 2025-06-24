package ovo.sypw.journal.data.repository

import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData

/**
 * 情感分析仓库接口
 * 定义所有与情感分析数据相关的操作
 */
interface SentimentRepository {
    /**
     * 根据日记ID获取情感分析结果
     */
    suspend fun getSentimentByJournalId(journalId: Int): SentimentData?

    /**
     * 获取所有情感分析结果
     */
    suspend fun getAllSentiments(): List<SentimentData>

    /**
     * 获取特定类型的情感分析结果
     */
    suspend fun getSentimentsByType(sentimentType: SentimentType): List<SentimentData>

    /**
     * 保存情感分析结果
     */
    suspend fun saveSentiment(sentiment: SentimentData)

    /**
     * 批量保存情感分析结果
     */
    suspend fun saveSentiments(sentiments: List<SentimentData>)

    /**
     * 根据日记ID删除情感分析结果
     */
    suspend fun deleteSentimentByJournalId(journalId: Int)

    /**
     * 删除所有情感分析结果
     */
    suspend fun deleteAllSentiments()

    /**
     * 获取情感分析结果总数
     */
    suspend fun getSentimentCount(): Int

    /**
     * 根据情感类型获取日记ID列表
     */
    suspend fun getJournalIdsByType(sentimentType: SentimentType): List<Int>

    /**
     * 获取日记与情感分析结果的联合查询
     * @return 日记与情感分析结果的列表，情感分析结果可能为null
     */
    suspend fun getJournalsWithSentiments(): List<Pair<JournalData, SentimentData?>>
} 