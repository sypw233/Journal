package ovo.sypw.journal.common.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.remote.api.AuthService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步状态枚举
 */
enum class SyncState {
    IDLE,       // 空闲状态
    SYNCING,    // 同步中
    SUCCESS,    // 同步成功
    FAILED,     // 同步失败
    PAUSED      // 同步暂停（自动同步已禁用）
}

/**
 * 自动同步管理器
 * 处理数据库的自动同步功能
 */
@Singleton
class AutoSyncManager @Inject constructor(
    private val context: Context,
    private val databaseManager: DatabaseManager,
    private val preferences: JournalPreferences,
    private val authService: AuthService
) {
    companion object {
        private const val TAG = "AutoSyncManager"
        private const val MIN_SYNC_INTERVAL = 5 * 60 * 1000L // 最小同步间隔 5 分钟
        private const val DELAYED_SYNC_INTERVAL = 5 * 1000L // 延迟同步间隔 5 秒
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    private var delayedSyncJob: Job? = null

    // 数据库操作互斥锁，防止同时操作导致锁定问题
    private val syncMutex = Mutex()

    // 自动同步是否已启用
    private val _autoSyncEnabled = MutableStateFlow(false)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    // 上次同步时间
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // 同步状态
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 详细同步状态
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // 同步错误信息
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // 上次数据变更时间，用于追踪短时间内的多次变更
    private var lastDataChangeTime = 0L

    init {
        // 初始化时从配置中读取自动同步设置
        _autoSyncEnabled.value = preferences.isAutoSyncEnabled()
        _lastSyncTime.value = preferences.getLastSyncTime()

        // 设置初始同步状态
        updateSyncState()

        Log.d(TAG, "自动同步状态: ${_autoSyncEnabled.value}, 上次同步时间: ${_lastSyncTime.value}")
    }

    /**
     * 更新同步状态
     */
    private fun updateSyncState() {
        _syncState.value = when {
            _isSyncing.value -> SyncState.SYNCING
            !_autoSyncEnabled.value -> SyncState.PAUSED
            else -> SyncState.IDLE
        }
    }

    /**
     * 设置自动同步状态
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        preferences.setAutoSyncEnabled(enabled)

        if (enabled) {
            // 如果启用了自动同步，立即执行一次同步
            scheduleSyncNow()
        } else {
            // 如果禁用了自动同步，取消正在进行的同步任务
            cancelSync()
            _syncState.value = SyncState.PAUSED
        }
    }

    /**
     * 数据变更通知
     * 当本地数据发生变更时调用此方法，触发同步
     */
    fun notifyDataChanged() {
        if (!_autoSyncEnabled.value || _isSyncing.value) {
            // 如果自动同步未启用或同步正在进行中，忽略此次通知
            return
        }

        // 记录数据变更时间
        lastDataChangeTime = System.currentTimeMillis()

        // 检查是否已经过了最小同步间隔
        val now = System.currentTimeMillis()
        val timeSinceLastSync = now - _lastSyncTime.value

        if (timeSinceLastSync >= MIN_SYNC_INTERVAL) {
            Log.d(TAG, "数据变更，执行同步")
            scheduleSyncNow()
        } else {
            Log.d(TAG, "数据变更，但距离上次同步时间过短，等待延迟同步")
            // 安排延迟同步，使用固定的5秒延迟
            scheduleDelayedSync()
        }
    }

    /**
     * 立即执行同步
     * @return true 如果成功调度了同步任务，false 如果同步正在进行中
     */
    fun scheduleSyncNow(): Boolean {
        // 如果同步已在进行中，不要重复执行
        if (_isSyncing.value) {
            Log.d(TAG, "同步正在进行中，忽略此次同步请求")
            return false
        }

        // 检查是否已登录
        if (!authService.isLoggedIn()) {
            Log.e(TAG, "未登录，无法进行同步")
            _syncError.value = "未登录，无法进行同步"
            _syncState.value = SyncState.FAILED

            // 一段时间后重置失败状态为空闲状态
            coroutineScope.launch {
                delay(5000) // 5秒后重置状态
                if (_syncState.value == SyncState.FAILED) {
                    updateSyncState()
                }
            }

            return false
        }

        // 取消之前的同步任务
        syncJob?.cancel()
        delayedSyncJob?.cancel()

        // 重置同步错误
        _syncError.value = null

        syncJob = coroutineScope.launch {
            try {
                // 设置同步状态
                _isSyncing.value = true
                _syncState.value = SyncState.SYNCING
                Log.d(TAG, "开始执行自动同步")

                // 使用互斥锁防止多个同步操作同时进行
                syncMutex.withLock {
                    // 在同步前保证一定的延迟，让其他数据库操作完成
                    delay(500)

                    // 导出数据库前先检查确保没有活跃的数据库操作
                    try {
                        // 导出数据库
                        val localFile = databaseManager.exportDatabase()
                        if (localFile == null) {
                            val errorMsg = "导出数据库失败，无法进行同步"
                            Log.e(TAG, errorMsg)
                            _syncError.value = errorMsg
                            _syncState.value = SyncState.FAILED
                            return@withLock
                        }

                        // 确保导出后有足够时间让数据库稳定
                        delay(300)

                        // 确保远程目录存在
                        val dirResult = databaseManager.ensureRemoteDirectoryExists()
                        if (dirResult.isFailure) {
                            val errorMsg =
                                "创建远程目录失败: ${dirResult.exceptionOrNull()?.message}"
                            Log.e(TAG, errorMsg)
                            _syncError.value = errorMsg
                            _syncState.value = SyncState.FAILED
                            return@withLock
                        }

                        // 上传数据库
                        val result = databaseManager.uploadDatabaseToServer(localFile)
                        if (result.isSuccess) {
                            // 更新上次同步时间
                            _lastSyncTime.value = System.currentTimeMillis()
                            preferences.setLastSyncTime(_lastSyncTime.value)
                            _syncState.value = SyncState.SUCCESS
                            Log.d(TAG, "自动同步成功，更新上次同步时间: ${_lastSyncTime.value}")
                        } else {
                            val errorMsg = "自动同步失败: ${result.exceptionOrNull()?.message}"
                            Log.e(TAG, errorMsg)
                            _syncError.value = errorMsg
                            _syncState.value = SyncState.FAILED
                        }
                    } catch (e: Exception) {
                        val errorMsg = "自动同步过程中发生错误: ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        _syncError.value = errorMsg
                        _syncState.value = SyncState.FAILED
                    }
                }
            } finally {
                // 确保无论如何都会重置同步状态
                _isSyncing.value = false

                // 如果同步状态仍为SYNCING，说明有异常导致没有正确设置状态
                if (_syncState.value == SyncState.SYNCING) {
                    _syncState.value = SyncState.FAILED
                    _syncError.value = "同步过程异常中断"
                }

                // 一段时间后重置成功或失败状态为空闲状态
                if (_syncState.value == SyncState.SUCCESS || _syncState.value == SyncState.FAILED) {
                    coroutineScope.launch {
                        delay(5000) // 5秒后重置状态
                        if (_syncState.value == SyncState.SUCCESS || _syncState.value == SyncState.FAILED) {
                            updateSyncState()
                        }
                    }
                }
            }
        }

        return true
    }

    /**
     * 安排延迟同步
     * 使用固定的5秒延迟，如果在这5秒内有新的数据变更，则重新计时
     */
    private fun scheduleDelayedSync() {
        // 取消之前的延迟同步任务
        delayedSyncJob?.cancel()

        delayedSyncJob = coroutineScope.launch {
            try {
                // 记录当前的数据变更时间
                val currentChangeTime = lastDataChangeTime

                // 固定等待5秒
                Log.d(TAG, "安排延迟同步，将在 5 秒后执行")
                delay(DELAYED_SYNC_INTERVAL)

                // 检查在等待期间是否有新的数据变更
                if (currentChangeTime != lastDataChangeTime) {
                    // 如果有新的数据变更，重新安排延迟同步
                    Log.d(TAG, "在等待期间检测到新的数据变更，重新安排延迟同步")
                    scheduleDelayedSync()
                } else {
                    // 如果没有新的数据变更且同步未在进行中，执行同步
                    if (!_isSyncing.value) {
                        Log.d(TAG, "延迟同步时间到，执行同步")
                        scheduleSyncNow()
                    }
                }
            } catch (e: Exception) {
                // 如果是任务取消异常，则不记录错误日志
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "延迟同步任务已取消")
                } else {
                    Log.e(TAG, "安排延迟同步失败", e)
                }
            }
        }
    }

    /**
     * 取消同步任务
     */
    private fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        delayedSyncJob?.cancel()
        delayedSyncJob = null
        _isSyncing.value = false
        updateSyncState()
    }
} 