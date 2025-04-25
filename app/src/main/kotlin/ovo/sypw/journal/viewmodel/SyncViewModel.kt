package ovo.sypw.journal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.database.ConflictResolutionStrategy
import ovo.sypw.journal.data.database.SyncConflict
import ovo.sypw.journal.data.repository.SyncAwareJournalRepository
import ovo.sypw.journal.data.sync.SyncManager
import ovo.sypw.journal.data.sync.SyncState
import ovo.sypw.journal.utils.SnackBarUtils
import javax.inject.Inject

/**
 * 同步视图模型
 * 管理同步状态和操作，作为UI和同步管理器之间的桥梁
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncAwareRepository: SyncAwareJournalRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    // 同步状态
    val syncState: StateFlow<SyncState> = syncManager.syncState

    // 冲突列表
    val conflicts: StateFlow<List<SyncConflict>> = syncManager.conflicts

    init {
        // 初始化同步管理器
        syncManager.initialize()
    }

    /**
     * 执行同步操作
     * 将本地数据与云端同步
     */
    suspend fun synchronize() {
        try {
            syncManager.synchronize()
        } catch (e: Exception) {
            SnackBarUtils.showSnackBar("同步失败: ${e.message}")
        }
    }

    /**
     * 从视图模型中触发同步
     * 在协程作用域中执行同步操作
     */
    fun startSync() {
        viewModelScope.launch {
            synchronize()
        }
    }

    /**
     * 解决同步冲突
     * @param entityId 实体ID
     * @param strategy 冲突解决策略
     */
    suspend fun resolveConflict(entityId: String, strategy: ConflictResolutionStrategy) {
        try {
            syncManager.resolveConflict(entityId, strategy)
            SnackBarUtils.showSnackBar("冲突已解决")
        } catch (e: Exception) {
            SnackBarUtils.showSnackBar("解决冲突失败: ${e.message}")
        }
    }

    /**
     * 获取待同步的数据数量
     * @return 待同步的数据数量
     */
    suspend fun getPendingSyncCount(): Int {
        return syncAwareRepository.getModifiedJournalsSinceLastSync().size
    }

    /**
     * 获取冲突数量
     * @return 冲突数量
     */
    suspend fun getConflictCount(): Int {
        return syncAwareRepository.getConflictJournals().size
    }
}