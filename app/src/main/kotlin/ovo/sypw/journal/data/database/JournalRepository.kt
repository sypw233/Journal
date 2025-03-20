package ovo.sypw.journal.data.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovo.sypw.journal.model.JournalData

/**
 * 日记仓库类，处理数据操作
 */
class JournalRepository(private val journalDao: JournalDao) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 获取所有日记条目
     */
    fun getAllJournals(): Flow<List<JournalData>> {
        return journalDao.getAllJournals().map { entities ->
            entities.map { it.toJournalData() }
        }
    }

    /**
     * 分页获取日记条目
     */
    suspend fun getJournalsPaged(offset: Int, limit: Int): List<JournalData> {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalsPaged(offset, limit).map { it.toJournalData() }
        }
    }

    /**
     * 根据ID获取日记条目
     */
    suspend fun getJournalById(id: Int): JournalData? {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalById(id)?.toJournalData()
        }
    }

    /**
     * 插入日记条目
     */
    fun insertJournal(journal: JournalData) {
        coroutineScope.launch {
            journalDao.insertJournal(JournalEntity.fromJournalData(journal))
        }
    }

    /**
     * 批量插入日记条目
     */
    fun insertJournals(journals: List<JournalData>) {
        coroutineScope.launch {
            val entities = journals.map { JournalEntity.fromJournalData(it) }
            journalDao.insertJournals(entities)
        }
    }

    /**
     * 更新日记条目
     */
    fun updateJournal(journal: JournalData) {
        coroutineScope.launch {
            journalDao.updateJournal(JournalEntity.fromJournalData(journal))
        }
    }

    /**
     * 删除日记条目
     */
    fun deleteJournal(journal: JournalData) {
        coroutineScope.launch {
            journalDao.deleteJournal(JournalEntity.fromJournalData(journal))
        }
    }

    /**
     * 根据ID删除日记条目
     */
    fun deleteJournalById(id: Int) {
        coroutineScope.launch {
            journalDao.deleteJournalById(id)
        }
    }

    /**
     * 获取标记的日记条目
     */
    fun getMarkedJournals(): Flow<List<JournalData>> {
        return journalDao.getMarkedJournals().map { entities ->
            entities.map { it.toJournalData() }
        }
    }

    /**
     * 获取日记条目总数
     */
    suspend fun getJournalCount(): Int {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalCount()
        }
    }

    suspend fun getJournalLastId(): Int {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalLastId()
        }

    }
}