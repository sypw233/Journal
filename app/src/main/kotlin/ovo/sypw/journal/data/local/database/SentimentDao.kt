package ovo.sypw.journal.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 情感分析数据访问对象接口
 */
@Dao
interface SentimentDao {
    /**
     * 根据日记ID获取情感分析结果
     */
    @Query("SELECT * FROM sentiments WHERE journalId = :journalId")
    suspend fun getSentimentByJournalId(journalId: Int): SentimentEntity?
    
    /**
     * 获取所有情感分析结果
     */
    @Query("SELECT * FROM sentiments")
    suspend fun getAllSentiments(): List<SentimentEntity>
    
    /**
     * 获取特定类型的情感分析结果
     */
    @Query("SELECT * FROM sentiments WHERE sentimentType = :sentimentType")
    suspend fun getSentimentsByType(sentimentType: String): List<SentimentEntity>
    
    /**
     * 插入情感分析结果，如果已存在则替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentiment(sentiment: SentimentEntity): Long
    
    /**
     * 批量插入情感分析结果
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentiments(sentiments: List<SentimentEntity>): List<Long>
    
    /**
     * 更新情感分析结果
     */
    @Update
    suspend fun updateSentiment(sentiment: SentimentEntity)
    
    /**
     * 根据日记ID删除情感分析结果
     */
    @Query("DELETE FROM sentiments WHERE journalId = :journalId")
    suspend fun deleteSentimentByJournalId(journalId: Int): Int
    
    /**
     * 删除所有情感分析结果
     */
    @Query("DELETE FROM sentiments")
    suspend fun deleteAllSentiments()
    
    /**
     * 获取情感分析结果总数
     */
    @Query("SELECT COUNT(*) FROM sentiments")
    suspend fun getSentimentCount(): Int
    
    /**
     * 根据情感类型获取日记ID列表
     */
    @Query("SELECT journalId FROM sentiments WHERE sentimentType = :sentimentType")
    suspend fun getJournalIdsByType(sentimentType: String): List<Int>
    
    /**
     * 获取情感分析结果与日记的联合查询
     * 使用Room的关系查询
     */
    @Transaction
    @Query("SELECT * FROM journals ORDER BY date DESC")
    suspend fun getJournalsWithSentiments(): List<JournalWithSentiment>
} 