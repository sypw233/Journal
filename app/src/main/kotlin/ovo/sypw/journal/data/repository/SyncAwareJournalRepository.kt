package ovo.sypw.journal.data.repository

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ovo.sypw.journal.data.database.JournalEntity
import ovo.sypw.journal.data.database.SyncAwareJournalDao
import ovo.sypw.journal.data.database.SyncDao
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.sync.SyncManager
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * 支持同步的日记仓库实现类
 * 实现JournalRepository接口，并添加同步功能
 */
class SyncAwareJournalRepository @Inject constructor(
    private val context: Context,
    private val syncAwareJournalDao: SyncAwareJournalDao,
    private val syncDao: SyncDao,
    private val syncManager: SyncManager
) : JournalRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // 设备ID，用于标识数据来源
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    }

    /**
     * 获取所有日记条目
     */
    override fun getAllJournals(): Flow<List<JournalData>> {
        return syncAwareJournalDao.getAllJournals().map { entities ->
            entities.map { it.toJournalData() }
        }
    }

    /**
     * 分页获取日记条目
     */
    override suspend fun getJournalsPaged(offset: Int, limit: Int): List<JournalData> {
        return withContext(Dispatchers.IO) {
            syncAwareJournalDao.getJournalsPaged(offset, limit).map { it.toJournalData() }
        }
    }

    /**
     * 根据ID获取日记条目
     */
    override suspend fun getJournalById(id: Int): JournalData? {
        return withContext(Dispatchers.IO) {
            syncAwareJournalDao.getJournalById(id)?.toJournalData()
        }
    }

    /**
     * 插入日记条目
     * 同时更新同步信息
     */
    override suspend fun insertJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            val entity = JournalEntity.fromJournalData(journal)
            syncAwareJournalDao.insertJournalWithSync(entity, syncDao, deviceId)
        }
    }

    /**
     * 批量插入日记条目
     * 同时更新同步信息
     */
    override suspend fun insertJournals(journals: List<JournalData>) {
        withContext(Dispatchers.IO) {
            journals.forEach { journal ->
                val entity = JournalEntity.fromJournalData(journal)
                syncAwareJournalDao.insertJournalWithSync(entity, syncDao, deviceId)
            }
        }
    }

    /**
     * 更新日记条目
     * 同时更新同步信息
     */
    override suspend fun updateJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            val entity = JournalEntity.fromJournalData(journal)
            syncAwareJournalDao.updateJournalWithSync(entity, syncDao, deviceId)
        }
    }

    /**
     * 删除日记条目
     * 同时更新同步信息
     */
    override suspend fun deleteJournal(journal: JournalData) {
        withContext(Dispatchers.IO) {
            val entity = JournalEntity.fromJournalData(journal)
            syncAwareJournalDao.deleteJournalWithSync(entity, syncDao, deviceId)
        }
    }

    /**
     * 根据ID删除日记条目
     * 同时更新同步信息
     */
    override suspend fun deleteJournalById(id: Int) {
        withContext(Dispatchers.IO) {
            syncAwareJournalDao.deleteJournalByIdWithSync(id, syncDao, deviceId)
        }
    }

    /**
     * 获取标记的日记条目
     */
    override fun getMarkedJournals(): Flow<List<JournalData>> {
        return syncAwareJournalDao.getMarkedJournals().map { entities ->
            entities.map { it.toJournalData() }
        }
    }

    /**
     * 获取日记条目总数
     */
    override suspend fun getJournalCount(): Int {
        return withContext(Dispatchers.IO) {
            syncAwareJournalDao.getJournalCount()
        }
    }

    /**
     * 获取最后一个日记的ID
     */
    override suspend fun getJournalLastId(): Int {
        return withContext(Dispatchers.IO) {
            syncAwareJournalDao.getJournalLastId()
        }
    }

    /**
     * 执行同步操作
     * 将本地数据与云端同步
     */
    suspend fun synchronize() {
        syncManager.synchronize()
    }

    /**
     * 获取存在冲突的日记条目
     */
    suspend fun getConflictJournals(): List<JournalData> {
        return withContext(Dispatchers.IO) {
            syncAwareJournalDao.getConflictJournals().map { it.toJournalData() }
        }
    }

    /**
     * 获取自上次同步后修改的日记条目
     */
    suspend fun getModifiedJournalsSinceLastSync(): List<JournalData> {
        return withContext(Dispatchers.IO) {
            val lastSyncTime = syncDao.getLastSyncTime() ?: Date(0)
            syncAwareJournalDao.getJournalsModifiedSince(lastSyncTime).map { it.toJournalData() }
        }
    }
}