package ovo.sypw.journal.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.DatabaseManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.data.remote.api.FileItem
import ovo.sypw.journal.data.remote.api.FileService
import java.io.File
import javax.inject.Inject

/**
 * 数据库管理ViewModel
 * 处理数据库的导出、上传、下载等操作
 */
@HiltViewModel
class DatabaseManagementViewModel @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val fileService: FileService,
    private val authService: AuthService
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

    init {
        refreshLocalFiles()
        updateRemotePath()
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
} 