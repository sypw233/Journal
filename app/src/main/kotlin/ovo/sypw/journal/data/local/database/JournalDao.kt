package ovo.sypw.journal.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 日记数据访问对象接口
 */
@Dao
interface JournalDao {
    /**
     * 获取所有日记条目
     */
    @Query("SELECT * FROM journals ORDER BY date DESC")
    fun getAllJournals(): Flow<List<JournalEntity>>

    /**
     * 分页获取日记条目
     */
    @Query("SELECT * FROM journals ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getJournalsPaged(offset: Int, limit: Int): List<JournalEntity>

    /**
     * 根据ID获取日记条目
     */
    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: Int): JournalEntity?

    /**
     * 插入日记条目
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntity): Long

    /**
     * 批量插入日记条目
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournals(journals: List<JournalEntity>): List<Long>

    /**
     * 更新日记条目
     */
    @Update
    suspend fun updateJournal(journal: JournalEntity)

    /**
     * 删除日记条目
     */
    @Delete
    suspend fun deleteJournal(journal: JournalEntity)

    /**
     * 根据ID删除日记条目
     */
    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournalById(id: Int): Int



    /**
     * 获取日记条目总数
     */
    @Query("SELECT COUNT(*) FROM journals")
    suspend fun getJournalCount(): Int

    @Query("SELECT MAX(id) FROM journals")
    fun getJournalLastId(): Int

    /**
     * 根据内容搜索日记条目
     * @param query 搜索关键词，使用LIKE匹配
     */
    @Query("SELECT * FROM journals WHERE text LIKE '%' || :query || '%' ORDER BY date DESC")
    suspend fun searchJournalsByContent(query: String): List<JournalEntity>

    /**
     * 根据日期范围搜索日记条目
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     */
    @Query("SELECT * FROM journals WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun searchJournalsByDateRange(startDate: Long, endDate: Long): List<JournalEntity>

    /**
     * 根据位置名称搜索日记条目
     * @param locationName 位置名称，使用LIKE匹配
     */
    @Query("SELECT * FROM journals WHERE locationName LIKE '%' || :locationName || '%' ORDER BY date DESC")
    suspend fun searchJournalsByLocation(locationName: String): List<JournalEntity>

    /**
     * 复合搜索：同时根据内容和位置搜索
     */
    @Query("SELECT * FROM journals WHERE (text LIKE '%' || :query || '%' OR locationName LIKE '%' || :query || '%') ORDER BY date DESC")
    suspend fun searchJournalsByContentOrLocation(query: String): List<JournalEntity>
}