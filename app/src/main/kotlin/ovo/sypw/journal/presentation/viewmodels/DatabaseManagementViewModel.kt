package ovo.sypw.journal.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import ovo.sypw.journal.common.utils.DatabaseManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.data.remote.api.FileItem
import ovo.sypw.journal.data.remote.api.FileService
import java.io.File
import javax.inject.Inject
import ovo.sypw.journal.common.utils.AutoSyncManager

// 数据库同步比较结果
data class DatabaseCompareResult(
    val localFile: File?,
    val remoteFile: FileItem?,
    val localEntryCount: Int,
    val remoteEntryCount: Int,
    val localLastModified: Long,
    val remoteLastModified: Long
)

/**
 * 数据库管理ViewModel
 * 处理数据库的导出、上传、下载等操作
 */
@HiltViewModel
class DatabaseManagementViewModel @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val fileService: FileService,
    private val authService: AuthService,
    private val autoSyncManager: AutoSyncManager
) : ViewModel() {

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 本地数据库文件列表
    private val _localDbFiles = MutableStateFlow<List<File>>(emptyList())
    val localDbFiles: StateFlow<List<File>> = _localDbFiles.asStateFlow()

    // 远程数据库文件列表
    private val _remoteDbFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val remoteDbFiles: StateFlow<List<FileItem>> = _remoteDbFiles.asStateFlow()

    // 远程数据库目录路径
    private val _remoteDatabaseDir = MutableStateFlow("")
    val remoteDatabaseDir: StateFlow<String> = _remoteDatabaseDir.asStateFlow()
    
    // 数据库比较结果
    private val _databaseCompareResult = MutableStateFlow<DatabaseCompareResult?>(null)
    val databaseCompareResult: StateFlow<DatabaseCompareResult?> = _databaseCompareResult.asStateFlow()

    // 重启提示对话框的状态
    private val _showRestartDialog = MutableStateFlow(false)
    val showRestartDialog: StateFlow<Boolean> = _showRestartDialog.asStateFlow()

    // 自动同步状态
    private val _autoSyncEnabled = MutableStateFlow(false)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()
    
    // 上次同步时间
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    // 自动同步进行中
    private val _isAutoSyncing = MutableStateFlow(false)
    val isAutoSyncing: StateFlow<Boolean> = _isAutoSyncing.asStateFlow()

    init {
        refreshLocalFiles()
        updateRemotePath()
        
        // 监听自动同步管理器状态
        viewModelScope.launch {
            autoSyncManager.autoSyncEnabled.collectLatest { enabled ->
                _autoSyncEnabled.value = enabled
            }
        }
        
        viewModelScope.launch {
            autoSyncManager.lastSyncTime.collectLatest { time ->
                _lastSyncTime.value = time
            }
        }
        
        viewModelScope.launch {
            autoSyncManager.isSyncing.collectLatest { syncing ->
                _isAutoSyncing.value = syncing
            }
        }
    }

    /**
     * 更新远程路径
     */
    private fun updateRemotePath() {
        viewModelScope.launch {
            val username = authService.getCurrentUser()?.username
            if (username != null) {
                // 设置为用户个人目录下的databases文件夹
                _remoteDatabaseDir.value = "/$username/databases"
                refreshRemoteFiles()
            } else {
                // 如果没有获取到用户名，则使用默认路径
                _remoteDatabaseDir.value = "/databases"
                refreshRemoteFiles()
            }
        }
    }

    /**
     * 刷新本地文件列表
     */
    fun refreshLocalFiles() {
        _localDbFiles.value = databaseManager.getExportedDatabaseFiles()
    }

    /**
     * 刷新远程文件列表
     */
    fun refreshRemoteFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 首先创建目录如果不存在
                ensureRemoteDirectoryExists()

                val result = fileService.listFiles(_remoteDatabaseDir.value)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    // 只保留数据库文件
                    _remoteDbFiles.value =
                        response.items.filter { it.type == "file" && it.name.endsWith(".db") }
                            .map { item ->
                                // 修复显示时间问题，服务器可能返回的是毫秒时间戳
                                val fixedTimestamp = if (item.modified > 9999999999L) {
                                    // 如果时间戳是毫秒级别的，转换为秒
                                    item.modified / 1000
                                } else {
                                    item.modified
                                }
                                item.copy(modified = fixedTimestamp)
                            }
                } else {
                    SnackBarUtils.showSnackBar("获取远程文件列表失败")
                    _remoteDbFiles.value = emptyList()
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("获取远程文件列表失败: ${e.message}")
                _remoteDbFiles.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 确保远程目录存在
     */
    private suspend fun ensureRemoteDirectoryExists() {
        try {
            val result = fileService.listFiles(_remoteDatabaseDir.value)
            if (result.isFailure) {
                // 如果目录不存在，尝试创建
                fileService.createDirectory(_remoteDatabaseDir.value)
            }
        } catch (e: Exception) {
            // 尝试创建目录
            try {
                fileService.createDirectory(_remoteDatabaseDir.value)
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("创建远程目录失败: ${e.message}")
            }
        }
    }

    /**
     * 导出数据库
     */
    fun exportDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                databaseManager.exportDatabase()
                refreshLocalFiles()
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("导出数据库失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 上传数据库
     *
     * @param file 要上传的文件，如果为null则导出当前数据库再上传
     */
    fun uploadDatabase(file: File? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 先确保远程目录存在
                ensureRemoteDirectoryExists()

                val result = databaseManager.uploadDatabaseToServer(file, _remoteDatabaseDir.value)
                if (result.isSuccess) {
                    refreshRemoteFiles()
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("上传数据库失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 下载数据库
     *
     * @param path 远程文件路径
     */
    fun downloadDatabase(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 将完整URL转换为相对路径
                val relativePath = if (path.startsWith("http")) {
                    // 提取URL中的路径部分，一般是/username/database/filename.db格式
                    val urlPath = path.substringAfter("/webdav")
                    Log.d(
                        "DatabaseManagementViewModel",
                        "Converting URL path: $path to relative path: $urlPath"
                    )
                    urlPath
                } else {
                    path
                }

                val result = databaseManager.downloadDatabaseFromServer(relativePath)
                if (result.isSuccess) {
                    SnackBarUtils.showSnackBar("下载成功，是否要恢复数据库？")
                    // 注意：这里需要用户确认后才能恢复数据库
                    // 实际应用中应该显示一个对话框，让用户确认是否恢复
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("下载数据库失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 恢复数据库
     *
     * @param file 下载的数据库文件
     */
    fun restoreDatabase(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = databaseManager.restoreDatabaseFromFile(file)
                if (result.isSuccess) {
                    // 显示重启提示对话框
                    _showRestartDialog.value = true
                    SnackBarUtils.showSnackBar("数据库恢复成功，请重启应用")
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("恢复数据库失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 删除本地文件
     *
     * @param file 要删除的文件
     */
    fun deleteLocalFile(file: File) {
        viewModelScope.launch {
            if (databaseManager.deleteExportedFile(file)) {
                refreshLocalFiles()
                SnackBarUtils.showSnackBar("文件已删除")
            } else {
                SnackBarUtils.showSnackBar("删除文件失败")
            }
        }
    }

    /**
     * 删除远程文件
     *
     * @param path 远程文件路径
     */
    fun deleteRemoteFile(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 将完整URL转换为相对路径
                val relativePath = if (path.startsWith("http")) {
                    // 提取URL中的路径部分，一般是/username/database/filename.db格式
                    val urlPath = path.substringAfter("/webdav")
                    Log.d(
                        "DatabaseManagementViewModel",
                        "Converting URL path: $path to relative path: $urlPath"
                    )
                    urlPath
                } else {
                    path
                }

                val result = fileService.deleteFile(relativePath)
                if (result.isSuccess) {
                    refreshRemoteFiles()
                    SnackBarUtils.showSnackBar("远程文件已删除")
                } else {
                    SnackBarUtils.showSnackBar("删除远程文件失败")
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("删除远程文件失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 同步数据库
     * 导出当前数据库，与远程最新数据库进行比较
     */
    fun syncDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. 导出当前数据库
                val localFile = databaseManager.exportDatabase()
                if (localFile == null) {
                    SnackBarUtils.showSnackBar("导出数据库失败，无法进行同步")
                    _isLoading.value = false
                    return@launch
                }
                
                // 显示导出成功的提示
                SnackBarUtils.showSnackBar("本地数据库导出成功，正在获取远程数据库...")
                
                // 2. 确保远程目录存在
                ensureRemoteDirectoryExists()
                
                // 3. 获取远程数据库列表
                val result = fileService.listFiles(_remoteDatabaseDir.value)
                if (result.isFailure) {
                    SnackBarUtils.showSnackBar("获取远程文件列表失败，无法进行同步")
                    _isLoading.value = false
                    return@launch
                }
                
                val fileList = result.getOrThrow()
                // 只保留数据库文件，并按修改时间排序（最新的在前面）
                val remoteDbFiles = fileList.items
                    .filter { it.type == "file" && it.name.endsWith(".db") }
                    .sortedByDescending { it.modified }
                
                if (remoteDbFiles.isEmpty()) {
                    // 如果远程没有数据库文件，则直接上传本地数据库
                    SnackBarUtils.showSnackBar("远程无数据库文件，正在上传本地数据库...")
                    uploadDatabase(localFile)
                    _isLoading.value = false
                    return@launch
                }
                
                // 4. 获取最新的远程数据库文件
                val latestRemoteFile = remoteDbFiles[0]
                
                // 显示下载提示
                SnackBarUtils.showSnackBar("正在下载远程数据库进行比较...")
                
                // 5. 下载远程数据库进行比较
                val remoteFilePath = if (latestRemoteFile.url?.startsWith("http") == true) {
                    latestRemoteFile.url.substringAfter("/webdav")
                } else {
                    latestRemoteFile.url ?: ""
                }
                
                if (remoteFilePath.isEmpty()) {
                    SnackBarUtils.showSnackBar("远程文件路径无效，无法进行同步")
                    _isLoading.value = false
                    return@launch
                }
                
                val downloadResult = databaseManager.downloadDatabaseFromServer(remoteFilePath)
                if (downloadResult.isFailure) {
                    SnackBarUtils.showSnackBar("下载远程数据库失败，无法进行同步")
                    _isLoading.value = false
                    return@launch
                }
                
                val remoteFile = downloadResult.getOrThrow()
                
                // 显示比较提示
                SnackBarUtils.showSnackBar("正在比较本地和远程数据库...")
                
                // 6. 比较本地和远程数据库
                val compareResult = databaseManager.compareDatabases(localFile, remoteFile)
                
                // 7. 保存比较结果，用于界面展示
                _databaseCompareResult.value = DatabaseCompareResult(
                    localFile = localFile,
                    remoteFile = latestRemoteFile,
                    localEntryCount = compareResult.localEntryCount,
                    remoteEntryCount = compareResult.remoteEntryCount,
                    localLastModified = localFile.lastModified(),
                    remoteLastModified = latestRemoteFile.modified * 1000 // 转换为毫秒
                )
                
                // 输出比较结果日志
                Log.d("DatabaseSync", "本地数据库条目数: ${compareResult.localEntryCount}, 修改时间: ${localFile.lastModified()}")
                Log.d("DatabaseSync", "远程数据库条目数: ${compareResult.remoteEntryCount}, 修改时间: ${latestRemoteFile.modified * 1000}")
                
                // 8. 如果远程数据库明显更新（条目更多且更新时间较新），自动使用远程数据库
                if (compareResult.remoteEntryCount > compareResult.localEntryCount && 
                    latestRemoteFile.modified * 1000 > localFile.lastModified()) {
                    SnackBarUtils.showSnackBar("远程数据库更新，正在自动同步...")
                    val restoreResult = databaseManager.restoreDatabaseFromFile(remoteFile)
                    if (restoreResult.isSuccess) {
                        // 显示重启对话框
                        _showRestartDialog.value = true
                        SnackBarUtils.showSnackBar("远程数据库更新，已自动同步")
                    } else {
                        SnackBarUtils.showSnackBar("自动同步失败，请手动选择同步方式")
                    }
                } else {
                    SnackBarUtils.showSnackBar("数据库已比较完成，请选择要保留的版本")
                }
                
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("同步数据库失败: ${e.message}")
                Log.e("DatabaseManagementViewModel", "同步数据库失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 使用本地数据库覆盖远程数据库
     */
    fun useLocalDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val localFile = _databaseCompareResult.value?.localFile
                if (localFile != null) {
                    SnackBarUtils.showSnackBar("正在上传本地数据库...")
                    uploadDatabase(localFile)
                    SnackBarUtils.showSnackBar("已使用本地数据库覆盖远程数据库")
                    
                    // 清除比较结果
                    _databaseCompareResult.value = null
                } else {
                    SnackBarUtils.showSnackBar("本地数据库文件不存在")
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("使用本地数据库失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 使用远程数据库覆盖本地数据库
     */
    fun useRemoteDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val remoteFile = _databaseCompareResult.value?.remoteFile
                if (remoteFile != null) {
                    val remoteFilePath = if (remoteFile.url?.startsWith("http") == true) {
                        remoteFile.url.substringAfter("/webdav")
                    } else {
                        remoteFile.url ?: ""
                    }
                    
                    if (remoteFilePath.isEmpty()) {
                        SnackBarUtils.showSnackBar("远程文件路径无效")
                        _isLoading.value = false
                        return@launch
                    }
                    
                    SnackBarUtils.showSnackBar("正在下载远程数据库...")
                    val downloadResult = databaseManager.downloadDatabaseFromServer(remoteFilePath)
                    if (downloadResult.isSuccess) {
                        val file = downloadResult.getOrThrow()
                        
                        SnackBarUtils.showSnackBar("正在恢复远程数据库...")
                        val restoreResult = databaseManager.restoreDatabaseFromFile(file)
                        if (restoreResult.isSuccess) {
                            // 显示重启对话框
                            _showRestartDialog.value = true
                            SnackBarUtils.showSnackBar("已恢复远程数据库，请重启应用")
                            
                            // 清除比较结果
                            _databaseCompareResult.value = null
                        } else {
                            SnackBarUtils.showSnackBar("恢复数据库失败")
                        }
                    } else {
                        SnackBarUtils.showSnackBar("下载远程数据库失败")
                    }
                } else {
                    SnackBarUtils.showSnackBar("远程数据库文件不存在")
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("使用远程数据库失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除比较结果
     */
    fun clearCompareResult() {
        _databaseCompareResult.value = null
    }

    /**
     * 关闭重启提示对话框
     */
    fun dismissRestartDialog() {
        _showRestartDialog.value = false
    }

    /**
     * 设置自动同步状态
     *
     * @param enabled 是否启用自动同步
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            autoSyncManager.setAutoSyncEnabled(enabled)
        }
    }
    
    /**
     * 立即执行自动同步
     */
    fun syncNow() {
        viewModelScope.launch {
            autoSyncManager.scheduleSyncNow()
        }
    }
} 