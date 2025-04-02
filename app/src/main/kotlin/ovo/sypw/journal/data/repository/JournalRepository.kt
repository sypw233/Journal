package ovo.sypw.journal.data.repository

import kotlinx.coroutines.flow.Flow
import ovo.sypw.journal.model.JournalData

/**
 * 日记仓库接口
 * 定义所有与日记数据相关的操作
 * 遵循依赖倒置原则，高层模块不应该依赖低层模块，两者都应该依赖抽象
 */
interface JournalRepository {
    /**
     * 获取所有日记条目
     */
    fun getAllJournals(): Flow<List<JournalData>>

    /**
     * 分页获取日记条目
     */
    suspend fun getJournalsPaged(offset: Int, limit: Int): List<JournalData>

    /**
     * 根据ID获取日记条目
     */
    suspend fun getJournalById(id: Int): JournalData?

    /**
     * 插入日记条目
     */
    suspend fun insertJournal(journal: JournalData)

    /**
     * 批量插入日记条目
     */
    suspend fun insertJournals(journals: List<JournalData>)

    /**
     * 更新日记条目
     */
    suspend fun updateJournal(journal: JournalData)

    /**
     * 删除日记条目
     */
    suspend fun deleteJournal(journal: JournalData)

    /**
     * 根据ID删除日记条目
     */
    suspend fun deleteJournalById(id: Int)

    /**
     * 获取标记的日记条目
     */
    fun getMarkedJournals(): Flow<List<JournalData>>

    /**
     * 获取日记条目总数
     */
    suspend fun getJournalCount(): Int

    /**
     * 获取最后一个日记的ID
     */
    suspend fun getJournalLastId(): Int
}