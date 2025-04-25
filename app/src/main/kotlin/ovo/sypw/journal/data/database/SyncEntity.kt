package ovo.sypw.journal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 同步实体类，用于跟踪数据同步状态
 * 包含必要的同步字段，如修改时间戳、设备ID、同步状态等
 */
@Entity(tableName = "sync_info")
data class SyncEntity(
    @PrimaryKey
    val entityId: String, // 关联的实体ID，格式为："表名:ID"，例如 "journals:123"
    val lastModified: Date = Date(), // 最后修改时间
    val deviceId: String, // 设备ID，用于识别哪个设备进行了修改
    val syncStatus: SyncStatus = SyncStatus.PENDING, // 同步状态
    val version: Long = 1, // 版本号，每次修改递增
    val deleted: Boolean = false, // 是否已删除（用于软删除）
    val lastSyncTime: Date? = null // 最后同步时间
)

/**
 * 同步状态枚举
 */
enum class SyncStatus {
    SYNCED,      // 已同步
    PENDING,     // 待同步
    CONFLICT,    // 冲突
    ERROR        // 错误
}

/**
 * 同步操作类型枚举
 */
enum class SyncOperation {
    CREATE,      // 创建
    UPDATE,      // 更新
    DELETE       // 删除
}

/**
 * 同步冲突解决策略枚举
 */
enum class ConflictResolutionStrategy {
    LOCAL_WINS,  // 本地优先
    REMOTE_WINS, // 远程优先
    MANUAL       // 手动解决
}

/**
 * 同步冲突数据类
 */
data class SyncConflict(
    val entityId: String,
    val localEntity: Any,
    val remoteEntity: Any,
    val localSyncInfo: SyncEntity,
    val remoteSyncInfo: SyncEntity
)