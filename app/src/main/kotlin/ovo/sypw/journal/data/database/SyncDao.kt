package ovo.sypw.journal.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 同步数据访问对象接口
 * 提供对同步实体的增删改查操作
 */
@Dao
interface SyncDao {
    /**
     * 插入同步信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncInfo(syncEntity: SyncEntity): Long

    /**
     * 批量插入同步信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncInfos(syncEntities: List<SyncEntity>): List<Long>

    /**
     * 更新同步信息
     */
    @Update
    suspend fun updateSyncInfo(syncEntity: SyncEntity)

    /**
     * 根据实体ID获取同步信息
     */
    @Query("SELECT * FROM sync_info WHERE entityId = :entityId")
    suspend fun getSyncInfoByEntityId(entityId: String): SyncEntity?

    /**
     * 获取所有待同步的实体
     */
    @Query("SELECT * FROM sync_info WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncEntities(): List<SyncEntity>

    /**
     * 获取特定表中所有待同步的实体
     * @param tablePrefix 表名前缀，例如 "journals:"
     */
    @Query("SELECT * FROM sync_info WHERE entityId LIKE :tablePrefix || '%' AND syncStatus = 'PENDING'")
    suspend fun getPendingSyncEntitiesByTable(tablePrefix: String): List<SyncEntity>

    /**
     * 获取自上次同步后修改的实体
     * @param lastSyncTime 上次同步时间
     */
    @Query("SELECT * FROM sync_info WHERE lastModified > :lastSyncTime")
    suspend fun getModifiedSinceLastSync(lastSyncTime: Date): List<SyncEntity>

    /**
     * 获取所有存在冲突的实体
     */
    @Query("SELECT * FROM sync_info WHERE syncStatus = 'CONFLICT'")
    suspend fun getConflictEntities(): List<SyncEntity>

    /**
     * 获取所有存在冲突的实体（Flow版本）
     */
    @Query("SELECT * FROM sync_info WHERE syncStatus = 'CONFLICT'")
    fun getConflictEntitiesFlow(): Flow<List<SyncEntity>>

    /**
     * 更新实体的同步状态
     */
    @Query("UPDATE sync_info SET syncStatus = :status WHERE entityId = :entityId")
    suspend fun updateSyncStatus(entityId: String, status: SyncStatus)

    /**
     * 更新实体的最后同步时间
     */
    @Query("UPDATE sync_info SET lastSyncTime = :syncTime, syncStatus = 'SYNCED' WHERE entityId = :entityId")
    suspend fun updateLastSyncTime(entityId: String, syncTime: Date)

    /**
     * 删除同步信息
     */
    @Query("DELETE FROM sync_info WHERE entityId = :entityId")
    suspend fun deleteSyncInfo(entityId: String)

    /**
     * 获取最后一次同步时间
     */
    @Query("SELECT MAX(lastSyncTime) FROM sync_info")
    suspend fun getLastSyncTime(): Date?
}