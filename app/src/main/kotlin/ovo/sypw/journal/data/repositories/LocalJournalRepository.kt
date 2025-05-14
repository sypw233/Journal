package ovo.sypw.journal.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.data.database.JournalDao
import ovo.sypw.journal.data.database.JournalEntity
import ovo.sypw.journal.data.model.JournalData
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地日记仓库实现类
 * 实现JournalRepository接口，处理本地数据库的数据操作
 */
@Singleton
class LocalJournalRepository @Inject constructor(
    private val journalDao: JournalDao,
    private val autoSyncManager: AutoSyncManager
) : JournalRepository {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "LocalJournalRepository"

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
            try {
                journalDao.insertJournal(JournalEntity.fromJournalData(journal))
                // 安全地检查同步是否在进行中
                safelyNotifyDataChanged()
            } catch (e: Exception) {
                Log.e(TAG, "插入日记失败: ${e.message}", e)
                // 如果是数据库错误，不要尝试触发同步
            }
        }
    }

    override suspend fun insertJournals(journals: List<JournalData>) {
        withContext(Dispatchers.IO) {
            try {
                val entities = journals.map { JournalEntity.fromJournalData(it) }
                journalDao.insertJournals(entities)
                // 安全地检查同步是否在进行中
                safelyNotifyDataChanged()
            } catch (e: Exception) {
                Log.e(TAG, "批量插入日记失败: ${e.message}", e)
                // 如果是数据库错误，不要尝试触发同步
            }
        }
    }

    override suspend fun updateJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            try {
                journalDao.updateJournal(JournalEntity.fromJournalData(journal))
                // 安全地检查同步是否在进行中
                safelyNotifyDataChanged()
            } catch (e: Exception) {
                Log.e(TAG, "更新日记失败: ${e.message}", e)
                // 如果是数据库错误，不要尝试触发同步
            }
        }
    }

    override suspend fun deleteJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            try {
                journalDao.deleteJournal(JournalEntity.fromJournalData(journal))
                // 安全地检查同步是否在进行中
                safelyNotifyDataChanged()
            } catch (e: Exception) {
                Log.e(TAG, "删除日记失败: ${e.message}", e)
                // 如果是数据库错误，不要尝试触发同步
            }
        }
    }

    override suspend fun deleteJournalById(id: Int) {
        withContext(Dispatchers.IO) {
            try {
                journalDao.deleteJournalById(id)
                // 安全地检查同步是否在进行中
                safelyNotifyDataChanged()
            } catch (e: Exception) {
                Log.e(TAG, "通过ID删除日记失败: ${e.message}", e)
                // 如果是数据库错误，不要尝试触发同步
            }
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
    
    /**
     * 安全地通知数据变更
     * 确保在数据库正常操作时才触发同步
     */
    private suspend fun safelyNotifyDataChanged() {
        try {
            // 使用first()安全地获取当前同步状态
            val isSyncing = try {
                autoSyncManager.isSyncing.first()
            } catch (e: Exception) {
                Log.e(TAG, "检查同步状态时出错: ${e.message}")
                true // 如果出错，假设正在同步，不触发新的同步
            }
            
            if (!isSyncing) {
                // 如果没有正在进行的同步，通知数据变更
                autoSyncManager.notifyDataChanged()
            } else {
                Log.d(TAG, "同步正在进行中，不触发新的同步")
            }
        } catch (e: Exception) {
            Log.e(TAG, "通知数据变更时出错: ${e.message}", e)
        }
    }
}