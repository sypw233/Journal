package ovo.sypw.journal.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.api.EntryService
import ovo.sypw.journal.data.model.Entry
import ovo.sypw.journal.data.model.EntryRequest
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.utils.SnackBarUtils
import java.util.Date
import javax.inject.Inject

/**
 * 日记条目ViewModel
 * 负责管理日记条目的状态和业务逻辑，连接UI和数据层
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val entryService: EntryService,
    private val repository: JournalRepository
) : ViewModel() {
    private val TAG = "EntryViewModel"
    
    // UI状态
    data class EntryUiState(
        val entries: List<Entry> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSyncing: Boolean = false,
        val syncProgress: Int = 0,
        val syncTotal: Int = 0,
        val lastSyncTime: Date? = null
    )
    
    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()
    
    init {
        // 初始化时从本地加载数据
        loadLocalEntries()
    }
    
    /**
     * 从本地数据库加载日记条目
     */
    private fun loadLocalEntries() {
        viewModelScope.launch {
            repository.getAllJournals().collect { journals ->
                _uiState.update { it.copy(entries = journals.map { Entry.fromJournalData(it) }) }
            }
        }
    }
    
    /**
     * 从服务器同步日记条目
     */
    fun syncEntriesFromServer() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSyncing = true, syncProgress = 0, syncTotal = 0) }
                
                // 获取远程日记条目
                val result = entryService.getAllEntries()
                if (result.isSuccess) {
                    val remoteEntries = result.getOrThrow()
                    _uiState.update { it.copy(syncTotal = remoteEntries.size) }
                    
                    // 将远程日记条目保存到本地数据库
                    remoteEntries.forEachIndexed { index, entry ->
                        Log.d(TAG, "syncEntriesFromServer: $entry")
                        repository.insertJournal(entry.toJournalData())
                        _uiState.update { it.copy(syncProgress = index + 1) }
                    }
                    
                    _uiState.update { it.copy(
                        entries = remoteEntries,
                        lastSyncTime = Date(),
                        error = null
                    ) }
                    
                    SnackBarUtils.showSnackBar("同步成功，共同步${remoteEntries.size}条日记")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "同步失败"
                    _uiState.update { it.copy(error = error) }
                    SnackBarUtils.showSnackBar(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步日记失败", e)
                val error = "同步失败: ${e.message}"
                _uiState.update { it.copy(error = error) }
                SnackBarUtils.showSnackBar(error)
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }
    
    /**
     * 创建新的日记条目
     */
    fun createEntry(journalData: JournalData, imageUris: List<Uri>? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // 处理图片，将Uri转换为Base64字符串
//                val base64Images = imageUris?.map { uri ->
//                    ImageBase64Utils.uriToBase64(uri)
//                }
                
                val entryRequest = EntryRequest(
                    text = journalData.text,
                    date = journalData.date ?: Date(),
                    location = journalData.location,
                    images = journalData.images as List<String>?,
                    isMark = journalData.isMark ?: false
                )
                
                val result = entryService.createEntry(entryRequest)
                if (result.isSuccess) {
                    val entry = result.getOrThrow()
                    // 保存到本地数据库
                    repository.insertJournal(entry.toJournalData())
                    SnackBarUtils.showSnackBar("创建日记成功")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "创建日记失败"
                    _uiState.update { it.copy(error = error) }
                    SnackBarUtils.showSnackBar(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建日记失败", e)
                val error = "创建日记失败: ${e.message}"
                _uiState.update { it.copy(error = error) }
                SnackBarUtils.showSnackBar(error)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 更新日记条目
     */
    fun updateEntry(journalData: JournalData, imageUris: List<Uri>? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // 处理图片，将Uri转换为Base64字符串
//                val base64Images = imageUris?.map { uri ->
//                    ImageBase64Utils.uriToBase64(uri)
//                }
                
                val entryRequest = EntryRequest(
                    text = journalData.text,
                    date = journalData.date ?: Date(),
                    location = journalData.location,
                    images = journalData.images as List<String>?,
                    isMark = journalData.isMark ?: false
                )
                
                val result = entryService.updateEntry(journalData.id, entryRequest)
                if (result.isSuccess) {
                    val entry = result.getOrThrow()
                    // 更新本地数据库
                    repository.updateJournal(entry.toJournalData())
                    SnackBarUtils.showSnackBar("更新日记成功")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "更新日记失败"
                    _uiState.update { it.copy(error = error) }
                    SnackBarUtils.showSnackBar(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新日记失败", e)
                val error = "更新日记失败: ${e.message}"
                _uiState.update { it.copy(error = error) }
                SnackBarUtils.showSnackBar(error)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 删除日记条目
     */
    fun deleteEntry(id: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = entryService.deleteEntry(id)
                if (result.isSuccess) {
                    // 从本地数据库删除
                    repository.deleteJournalById(id)
                    SnackBarUtils.showSnackBar("删除日记成功")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "删除日记失败"
                    _uiState.update { it.copy(error = error) }
                    SnackBarUtils.showSnackBar(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除日记失败", e)
                val error = "删除日记失败: ${e.message}"
                _uiState.update { it.copy(error = error) }
                SnackBarUtils.showSnackBar(error)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 获取特定日记条目
     */
    fun getEntry(id: Int, callback: (Entry?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = entryService.getEntry(id)
                if (result.isSuccess) {
                    val entry = result.getOrThrow()
                    callback(entry)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "获取日记详情失败"
                    _uiState.update { it.copy(error = error) }
                    SnackBarUtils.showSnackBar(error)
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取日记详情失败", e)
                val error = "获取日记详情失败: ${e.message}"
                _uiState.update { it.copy(error = error) }
                SnackBarUtils.showSnackBar(error)
                callback(null)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}