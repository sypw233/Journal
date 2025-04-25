package ovo.sypw.journal.data.sync

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovo.sypw.journal.data.api.EntryService
import ovo.sypw.journal.data.database.ConflictResolutionStrategy
import ovo.sypw.journal.data.database.JournalDao
import ovo.sypw.journal.data.database.JournalEntity
import ovo.sypw.journal.data.database.SyncConflict
import ovo.sypw.journal.data.database.SyncDao
import ovo.sypw.journal.data.database.SyncEntity
import ovo.sypw.journal.data.database.SyncStatus
import ovo.sypw.journal.data.model.JournalData
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步管理器类
 * 负责处理本地数据库与云端的双向同步
 */
@SuppressLint("HardwareIds")
@Singleton
class SyncManager @Inject constructor(
    private val context: Context,
    private val journalDao: JournalDao,
    private val syncDao: SyncDao,
    private val entryService: EntryService
) {
    private val TAG = "SyncManager"

    // 设备ID，用于标识数据来源
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    }

    // 同步状态
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // 冲突列表
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 初始化同步管理器
     */
    fun initialize() {
        coroutineScope.launch {
            // 监听冲突变化
            syncDao.getConflictEntitiesFlow().collect { conflictEntities ->
                // 加载冲突详情
                val conflicts = conflictEntities.mapNotNull { syncEntity ->
                    val parts = syncEntity.entityId.split(":")
                    if (parts.size != 2) return@mapNotNull null

                    val tableName = parts[0]
                    val id = parts[1].toIntOrNull() ?: return@mapNotNull null

                    if (tableName == "journals") {
                        val localEntity = journalDao.getJournalById(id) ?: return@mapNotNull null
                        // 这里应该从服务器获取远程实体，但为简化示例，我们使用本地实体
                        val remoteEntity = localEntity.copy()

                        SyncConflict(
                            entityId = syncEntity.entityId,
                            localEntity = localEntity,
                            remoteEntity = remoteEntity,
                            localSyncInfo = syncEntity,
                            remoteSyncInfo = syncEntity.copy(deviceId = "remote")
                        )
                    } else null
                }
                _conflicts.value = conflicts
            }
        }
    }

    /**
     * 执行同步操作
     * 包括上传本地修改和下载远程更新
     */
    suspend fun synchronize() {
        withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                // 1. 获取上次同步时间
                val lastSyncTime = syncDao.getLastSyncTime()

                // 2. 上传本地修改
                uploadLocalChanges()

                // 3. 下载远程更新
                downloadRemoteChanges(lastSyncTime)

                // 4. 更新最后同步时间
                val now = Date()
                // 这里应该更新用户的lastDataSyncTime

                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 上传本地修改到服务器
     */
    private suspend fun uploadLocalChanges() {
        // 获取所有待同步的日记实体
        val pendingSyncEntities = syncDao.getPendingSyncEntitiesByTable("journals:")

        for (syncEntity in pendingSyncEntities) {
            val parts = syncEntity.entityId.split(":")
            if (parts.size != 2) continue

            val id = parts[1].toIntOrNull() ?: continue
            val journalEntity = journalDao.getJournalById(id) ?: continue

            try {
                if (syncEntity.deleted) {
                    // 删除远程实体
                    // entryService.deleteEntry(id)
                    // 删除成功后，删除本地同步信息
                    syncDao.deleteSyncInfo(syncEntity.entityId)
                } else {
                    // 上传或更新远程实体
                    val journalData = journalEntity.toJournalData()
                    // val result = entryService.createOrUpdateEntry(journalData)

                    // 更新同步状态和时间
                    syncDao.updateLastSyncTime(syncEntity.entityId, Date())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload changes for ${syncEntity.entityId}", e)
                syncDao.updateSyncStatus(syncEntity.entityId, SyncStatus.ERROR)
            }
        }
    }

    /**
     * 从服务器下载远程更新
     */
    private suspend fun downloadRemoteChanges(lastSyncTime: Date?) {
        try {
            // 从服务器获取自上次同步以来的更新
            // val remoteEntries = entryService.getEntriesSince(lastSyncTime)

            // 模拟远程数据
            val remoteEntries = emptyList<JournalData>()

            for (remoteEntry in remoteEntries) {
                val localEntry = journalDao.getJournalById(remoteEntry.id)
                val entityId = "journals:${remoteEntry.id}"
                val syncInfo = syncDao.getSyncInfoByEntityId(entityId)

                if (localEntry == null) {
                    // 本地不存在，直接插入
                    val entity = JournalEntity.fromJournalData(remoteEntry)
                    journalDao.insertJournal(entity)

                    // 创建同步信息
                    val newSyncInfo = SyncEntity(
                        entityId = entityId,
                        lastModified = Date(),
                        deviceId = "remote",
                        syncStatus = SyncStatus.SYNCED,
                        lastSyncTime = Date()
                    )
                    syncDao.insertSyncInfo(newSyncInfo)
                } else if (syncInfo == null || syncInfo.syncStatus != SyncStatus.PENDING) {
                    // 本地存在但未修改，更新本地数据
                    val entity = JournalEntity.fromJournalData(remoteEntry)
                    journalDao.updateJournal(entity)

                    // 更新同步信息
                    val newSyncInfo = SyncEntity(
                        entityId = entityId,
                        lastModified = Date(),
                        deviceId = "remote",
                        syncStatus = SyncStatus.SYNCED,
                        lastSyncTime = Date()
                    )
                    syncDao.insertSyncInfo(newSyncInfo)
                } else {
                    // 本地已修改，存在冲突
                    syncDao.updateSyncStatus(entityId, SyncStatus.CONFLICT)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download remote changes", e)
            throw e
        }
    }

    /**
     * 解决同步冲突
     */
    suspend fun resolveConflict(entityId: String, strategy: ConflictResolutionStrategy) {
        withContext(Dispatchers.IO) {
            val syncEntity = syncDao.getSyncInfoByEntityId(entityId) ?: return@withContext
            if (syncEntity.syncStatus != SyncStatus.CONFLICT) return@withContext

            val parts = entityId.split(":")
            if (parts.size != 2) return@withContext

            val tableName = parts[0]
            val id = parts[1].toIntOrNull() ?: return@withContext

            when (strategy) {
                ConflictResolutionStrategy.LOCAL_WINS -> {
                    // 本地优先，将本地数据上传到服务器
                    if (tableName == "journals") {
                        val localEntity = journalDao.getJournalById(id) ?: return@withContext
                        // entryService.updateEntry(localEntity.toJournalData())
                    }
                }

                ConflictResolutionStrategy.REMOTE_WINS -> {
                    // 远程优先，用远程数据覆盖本地数据
                    if (tableName == "journals") {
                        // val remoteEntry = entryService.getEntry(id)
                        // val entity = JournalEntity.fromJournalData(remoteEntry)
                        // journalDao.updateJournal(entity)
                    }
                }

                ConflictResolutionStrategy.MANUAL -> {
                    // 手动解决，由用户选择具体字段
                    // 这里需要UI交互，不在此实现
                    return@withContext
                }
            }

            // 更新同步状态和时间
            syncDao.updateLastSyncTime(entityId, Date())
        }
    }

    /**
     * 标记实体为待同步状态
     */
    suspend fun markForSync(tableName: String, id: Int) {
        withContext(Dispatchers.IO) {
            val entityId = "$tableName:$id"
            val existingSyncInfo = syncDao.getSyncInfoByEntityId(entityId)

            val syncInfo = if (existingSyncInfo != null) {
                existingSyncInfo.copy(
                    lastModified = Date(),
                    syncStatus = SyncStatus.PENDING,
                    version = existingSyncInfo.version + 1
                )
            } else {
                SyncEntity(
                    entityId = entityId,
                    lastModified = Date(),
                    deviceId = deviceId,
                    syncStatus = SyncStatus.PENDING,
                    version = 1
                )
            }

            syncDao.insertSyncInfo(syncInfo)
        }
    }

    /**
     * 标记实体为已删除
     */
    suspend fun markAsDeleted(tableName: String, id: Int) {
        withContext(Dispatchers.IO) {
            val entityId = "$tableName:$id"
            val existingSyncInfo = syncDao.getSyncInfoByEntityId(entityId)

            val syncInfo = if (existingSyncInfo != null) {
                existingSyncInfo.copy(
                    lastModified = Date(),
                    syncStatus = SyncStatus.PENDING,
                    version = existingSyncInfo.version + 1,
                    deleted = true
                )
            } else {
                SyncEntity(
                    entityId = entityId,
                    lastModified = Date(),
                    deviceId = deviceId,
                    syncStatus = SyncStatus.PENDING,
                    version = 1,
                    deleted = true
                )
            }

            syncDao.insertSyncInfo(syncInfo)
        }
    }
}

/**
 * 同步状态
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}