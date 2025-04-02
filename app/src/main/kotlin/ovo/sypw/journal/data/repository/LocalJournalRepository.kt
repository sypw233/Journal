package ovo.sypw.journal.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ovo.sypw.journal.data.database.JournalDao
import ovo.sypw.journal.data.database.JournalEntity
import ovo.sypw.journal.model.JournalData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地日记仓库实现类
 * 实现JournalRepository接口，处理本地数据库的数据操作
 */
@Singleton
class LocalJournalRepository @Inject constructor(
    private val journalDao: JournalDao
) : JournalRepository {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getAllJournals(): Flow<List<JournalData>> {
        return journalDao.getAllJournals().map { entities ->
            entities.map { it.toJournalData() }
        }
    }

    override suspend fun getJournalsPaged(offset: Int, limit: Int): List<JournalData> {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalsPaged(offset, limit).map { it.toJournalData() }
        }
    }

    override suspend fun getJournalById(id: Int): JournalData? {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalById(id)?.toJournalData()
        }
    }

    override suspend fun insertJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            journalDao.insertJournal(JournalEntity.fromJournalData(journal))
        }
    }

    override suspend fun insertJournals(journals: List<JournalData>) {
        withContext(Dispatchers.IO) {
            val entities = journals.map { JournalEntity.fromJournalData(it) }
            journalDao.insertJournals(entities)
        }
    }

    override suspend fun updateJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            journalDao.updateJournal(JournalEntity.fromJournalData(journal))
        }
    }

    override suspend fun deleteJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            journalDao.deleteJournal(JournalEntity.fromJournalData(journal))
        }
    }

    override suspend fun deleteJournalById(id: Int) {
        withContext(Dispatchers.IO) {
            journalDao.deleteJournalById(id)
        }
    }

    override fun getMarkedJournals(): Flow<List<JournalData>> {
        return journalDao.getMarkedJournals().map { entities ->
            entities.map { it.toJournalData() }
        }
    }

    override suspend fun getJournalCount(): Int {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalCount()
        }
    }

    override suspend fun getJournalLastId(): Int {
        return withContext(Dispatchers.IO) {
            journalDao.getJournalLastId()
        }
    }
}