package ovo.sypw.journal.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 支持同步的日记数据访问对象接口
 * 扩展原有JournalDao，添加同步相关操作
 */
@Dao
interface SyncAwareJournalDao : JournalDao {
    /**
     * 插入日记条目并更新同步信息
     * 事务操作确保数据一致性
     */
    @Transaction
    suspend fun insertJournalWithSync(
        journal: JournalEntity,
        syncDao: SyncDao,
        deviceId: String
    ): Long {
        val id = insertJournal(journal)

        // 创建同步信息
        val syncEntity = SyncEntity(
            entityId = "journals:$id",
            lastModified = Date(),
            deviceId = deviceId,
            syncStatus = SyncStatus.PENDING
        )
        syncDao.insertSyncInfo(syncEntity)

        return id
    }

    /**
     * 更新日记条目并更新同步信息
     * 事务操作确保数据一致性
     */
    @Transaction
    suspend fun updateJournalWithSync(journal: JournalEntity, syncDao: SyncDao, deviceId: String) {
        updateJournal(journal)

        val entityId = "journals:${journal.id}"
        val existingSyncInfo = syncDao.getSyncInfoByEntityId(entityId)

        val syncEntity = if (existingSyncInfo != null) {
            existingSyncInfo.copy(
                lastModified = Date(),
                deviceId = deviceId,
                syncStatus = SyncStatus.PENDING,
                version = existingSyncInfo.version + 1
            )
        } else {
            SyncEntity(
                entityId = entityId,
                lastModified = Date(),
                deviceId = deviceId,
                syncStatus = SyncStatus.PENDING
            )
        }

        syncDao.insertSyncInfo(syncEntity)
    }

    /**
     * 删除日记条目并更新同步信息
     * 事务操作确保数据一致性
     */
    @Transaction
    suspend fun deleteJournalWithSync(journal: JournalEntity, syncDao: SyncDao, deviceId: String) {
        deleteJournal(journal)

        val entityId = "journals:${journal.id}"
        val existingSyncInfo = syncDao.getSyncInfoByEntityId(entityId)

        val syncEntity = if (existingSyncInfo != null) {
            existingSyncInfo.copy(
                lastModified = Date(),
                deviceId = deviceId,
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
                deleted = true
            )
        }

        syncDao.insertSyncInfo(syncEntity)
    }

    /**
     * 根据ID删除日记条目并更新同步信息
     * 事务操作确保数据一致性
     */
    @Transaction
    suspend fun deleteJournalByIdWithSync(id: Int, syncDao: SyncDao, deviceId: String): Int {
        val result = deleteJournalById(id)

        if (result > 0) {
            val entityId = "journals:$id"
            val existingSyncInfo = syncDao.getSyncInfoByEntityId(entityId)

            val syncEntity = if (existingSyncInfo != null) {
                existingSyncInfo.copy(
                    lastModified = Date(),
                    deviceId = deviceId,
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
                    deleted = true
                )
            }

            syncDao.insertSyncInfo(syncEntity)
        }

        return result
    }

    /**
     * 获取自上次同步后修改的日记条目
     */
    @Query("SELECT j.* FROM journals j INNER JOIN sync_info s ON 'journals:' || j.id = s.entityId WHERE s.lastModified > :lastSyncTime AND s.syncStatus = 'PENDING'")
    suspend fun getJournalsModifiedSince(lastSyncTime: Date): List<JournalEntity>

    /**
     * 获取存在冲突的日记条目
     */
    @Query("SELECT j.* FROM journals j INNER JOIN sync_info s ON 'journals:' || j.id = s.entityId WHERE s.syncStatus = 'CONFLICT'")
    suspend fun getConflictJournals(): List<JournalEntity>

    /**
     * 获取存在冲突的日记条目（Flow版本）
     */
    @Query("SELECT j.* FROM journals j INNER JOIN sync_info s ON 'journals:' || j.id = s.entityId WHERE s.syncStatus = 'CONFLICT'")
    fun getConflictJournalsFlow(): Flow<List<JournalEntity>>
}