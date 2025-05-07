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
     * 获取标记的日记条目
     */
    @Query("SELECT * FROM journals WHERE isMark = 1 ORDER BY date DESC")
    fun getMarkedJournals(): Flow<List<JournalEntity>>

    /**
     * 获取日记条目总数
     */
    @Query("SELECT COUNT(*) FROM journals")
    suspend fun getJournalCount(): Int

    @Query("SELECT MAX(id) FROM journals")
    fun getJournalLastId(): Int
}