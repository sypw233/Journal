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
import ovo.sypw.journal.ui.list.JournalListState
import ovo.sypw.journal.utils.SnackBarUtils
import java.util.Date
import java.util.LinkedList
import javax.inject.Inject

private const val TAG = "JournalListViewModel"
private const val PAGE_SIZE = 10

/**
 * 日记列表ViewModel
 * 负责管理日记列表的状态和业务逻辑
 * 整合了EntryViewModel的网络同步功能
 */
@HiltViewModel
class JournalListViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val entryService: EntryService
) : ViewModel() {

    // 同步状态变量
    private var isSyncing = false
    private var syncProgress = 0
    private var syncTotal = 0
    private var lastSyncTime: Date? = null

    // UI状态
    private val _uiState = MutableStateFlow(JournalListState.Initial)
    val uiState: StateFlow<JournalListState> = _uiState.asStateFlow()

    // 分页参数
    private var currentPage = 0
    private var isLoading = false

    // 删除历史记录，用于撤销操作
    private val deletedJournals = LinkedList<JournalData>()

    init {
        // 初始化加载第一页数据
        loadNextPage()
    }

    /**
     * 加载下一页数据
     */
    fun loadNextPage() {
        if (isLoading || !_uiState.value.hasMoreData) return

        isLoading = true
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val offset = currentPage * PAGE_SIZE
                val journals = repository.getJournalsPaged(offset, PAGE_SIZE)
                Log.i(TAG, "Loading page $currentPage, now has ${journals.size}")

                val hasMoreData = journals.isNotEmpty()
                if (hasMoreData) {
                    currentPage++
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        journals = currentState.journals + journals,
                        hasMoreData = hasMoreData,
                        isLoading = false,
                        error = null,
                        isSyncing = isSyncing,
                        syncProgress = syncProgress,
                        syncTotal = syncTotal,
                        lastSyncTime = lastSyncTime
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading journals", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 删除日记
     */
    fun deleteJournal(id: Int) {
        viewModelScope.launch {
            try {
                // 先获取要删除的日记，用于撤销操作
                val journalToDelete = repository.getJournalById(id) ?: return@launch

                // 从数据库中删除
                repository.deleteJournalById(id)

                // 保存到删除历史记录中
                deletedJournals.addFirst(journalToDelete)
                if (deletedJournals.size > 10) { // 限制历史记录数量
                    deletedJournals.removeLast()
                }

                // 更新UI状态
                _uiState.update { currentState ->
                    currentState.copy(
                        journals = currentState.journals.filter { it.id != id },
                        markedItems = currentState.markedItems - id,
                        canUndo = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing journal", e)
                SnackBarUtils.showSnackBar("删除失败: ${e.message}")
            }
        }
    }

    fun deleteJournalFromServer(id: Int) {
        viewModelScope.launch {
            val result = entryService.deleteEntry(id)
            if (!result.isSuccess) {
                Log.w(TAG, "服务器删除失败: ${result.exceptionOrNull()?.message}")
            }
        }

    }

    /**
     * 切换日记标记状态
     */
    fun toggleMarkJournal(id: Int) {
        viewModelScope.launch {
            try {
                // 更新UI状态
                _uiState.update { currentState ->
                    val markedItems = if (currentState.markedItems.contains(id)) {
                        currentState.markedItems - id
                    } else {
                        currentState.markedItems + id
                    }
                    currentState.copy(markedItems = markedItems)
                }

                // 获取当前日记并更新标记状态
                val journal = repository.getJournalById(id) ?: return@launch
                val isCurrentlyMarked = _uiState.value.markedItems.contains(id)
                val updatedJournal = journal.copy(isMark = isCurrentlyMarked)
                repository.updateJournal(updatedJournal)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling mark", e)
                SnackBarUtils.showSnackBar("标记失败: ${e.message}")
            }
        }
    }

    /**
     * 设置滚动状态
     */
    fun setScrolling(isScrolling: Boolean) {
        _uiState.update { it.copy(isScrolling = isScrolling) }
    }

    /**
     * 重置滚动位置状态
     * 在滚动完成后调用此方法重置状态
     */
    fun resetScrollPosition() {
        _uiState.update { it.copy(scrollToPosition = null) }
    }

    /**
     * 创建新的日记条目
     */
    fun createEntry(journalData: JournalData, imageUris: List<Uri>? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 先保存到本地数据库
                repository.insertJournal(journalData)

                // 尝试同步到服务器
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
                    // 更新本地数据库中的ID以匹配服务器
                    repository.updateJournal(entry.toJournalData())
                    SnackBarUtils.showSnackBar("创建日记成功")
                } else {
                    // 服务器创建失败，但本地创建成功
                    Log.w(
                        TAG,
                        "服务器创建失败，但本地创建成功: ${result.exceptionOrNull()?.message}"
                    )
                    SnackBarUtils.showSnackBar("日记已保存到本地，但同步到服务器失败")
                }

                // 重置列表以显示新数据
                resetList()
            } catch (e: Exception) {
                Log.e(TAG, "创建日记失败", e)
                SnackBarUtils.showSnackBar("创建日记失败: ${e.message}")
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

                // 先更新本地数据库
                repository.updateJournal(journalData)

                // 尝试更新服务器
                val entryRequest = EntryRequest(
                    text = journalData.text,
                    date = journalData.date ?: Date(),
                    location = journalData.location,
                    images = journalData.images as List<String>?,
                    isMark = journalData.isMark ?: false
                )

                val result = entryService.updateEntry(journalData.id, entryRequest)
                if (result.isSuccess) {
                    SnackBarUtils.showSnackBar("更新日记成功")
                } else {
                    // 服务器更新失败，但本地更新成功
                    Log.w(
                        TAG,
                        "服务器更新失败，但本地更新成功: ${result.exceptionOrNull()?.message}"
                    )
                    SnackBarUtils.showSnackBar("日记已在本地更新，但同步到服务器失败")
                }

                // 更新UI状态
                _uiState.update { currentState ->
                    val updatedJournals = currentState.journals.map {
                        if (it.id == journalData.id) journalData else it
                    }
                    currentState.copy(journals = updatedJournals)
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新日记失败", e)
                SnackBarUtils.showSnackBar("更新日记失败: ${e.message}")
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

                // 先尝试从本地获取
                val localJournal = repository.getJournalById(id)
                if (localJournal != null) {
                    callback(Entry.fromJournalData(localJournal))
                    return@launch
                }

                // 如果本地没有，尝试从服务器获取
                val result = entryService.getEntry(id)
                if (result.isSuccess) {
                    val entry = result.getOrThrow()
                    // 保存到本地数据库
                    repository.insertJournal(entry.toJournalData())
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

    /**
     * 撤销删除操作
     * @return 是否成功撤销
     */
    fun undoDelete(): Boolean {
        if (deletedJournals.isEmpty()) return false

        viewModelScope.launch {
            try {
                // 从删除历史中取出最近删除的日记
                val journalToRestore = deletedJournals.removeFirst()

                // 重新插入到数据库
                repository.insertJournal(journalToRestore)

                // 更新UI状态，并设置滚动位置为0（恢复的日记总是添加到列表顶部）
                _uiState.update { currentState ->
                    currentState.copy(
                        journals = listOf(journalToRestore) + currentState.journals,
                        canUndo = deletedJournals.isNotEmpty(),
                        scrollToPosition = 0 // 设置滚动位置为0，因为恢复的日记被添加到了列表顶部
                    )
                }

                SnackBarUtils.showSnackBar("已恢复删除的日记")
            } catch (e: Exception) {
                Log.e(TAG, "Error undoing delete", e)
                SnackBarUtils.showSnackBar("撤销失败: ${e.message}")
            }
        }

        return true
    }

    /**
     * 重置列表状态
     * 清空当前列表并重新加载第一页数据
     */
    fun resetList() {
        Log.d(TAG, "resetList: ${_uiState.value}")
        currentPage = 0
        _uiState.value = JournalListState.Initial
        // 确保立即加载新数据
        viewModelScope.launch {
            loadNextPage()
            Log.d(TAG, "resetList: ${_uiState.value}")
        }
    }

    /**
     * 统一的服务器同步方法
     * 负责与服务器进行数据同步，并在同步完成后刷新本地UI界面
     * 处理同步状态的更新（开始同步、同步进度、同步完成），错误处理，以及成功后更新本地数据库和UI状态
     */
    /**
     * 统一的服务器同步方法
     * 负责与服务器进行数据同步，并在同步完成后刷新本地UI界面
     * 处理同步状态的更新（开始同步、同步进度、同步完成），错误处理，以及成功后更新本地数据库和UI状态
     */
    fun syncWithServer() {
        viewModelScope.launch {
            try {
                // 更新同步状态为开始同步
                isSyncing = true
                syncProgress = 0
                syncTotal = 0
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        isSyncing = true,
                        syncProgress = 0,
                        syncTotal = 0
                    )
                }

                // 获取远程日记条目
                val result = entryService.getAllEntries()
                if (result.isSuccess) {
                    val remoteEntries = result.getOrThrow()
                    syncTotal = remoteEntries.size
                    _uiState.update { it.copy(syncTotal = syncTotal) }

                    // 将远程日记条目保存到本地数据库并转换为JournalData对象
                    val journalDataList = mutableListOf<JournalData>()
                    remoteEntries.forEachIndexed { index, entry ->
                        val journalData = entry.toJournalData()
                        repository.insertJournal(journalData)
                        journalDataList.add(journalData)
                        Log.d(TAG, "syncWithServer: 同步条目 $journalData")

                        syncProgress = index + 1
                        _uiState.update { it.copy(syncProgress = syncProgress) }
                    }

                    // 更新最后同步时间
                    lastSyncTime = Date()

                    // 先清空当前页码，确保后续重新加载
                    currentPage = 0

                    // 完全重置状态，使用新的状态对象触发UI刷新
                    val refreshTimestamp = System.currentTimeMillis()
                    Log.d(TAG, "syncWithServer: 强制刷新触发器 $refreshTimestamp")

                    // 重新加载第一页数据
                    val freshJournals = repository.getJournalsPaged(0, PAGE_SIZE)
                    Log.d(TAG, "syncWithServer: 重新加载数据，获取到 ${freshJournals.size} 条记录")

                    // 使用value而不是update来确保状态完全替换，触发UI重组
                    _uiState.value = JournalListState(
                        journals = freshJournals,
                        hasMoreData = freshJournals.isNotEmpty(),
                        isLoading = false,
                        isSyncing = false,
                        syncProgress = syncProgress,
                        syncTotal = syncTotal,
                        lastSyncTime = lastSyncTime,
                        forceRefresh = refreshTimestamp // 使用时间戳确保每次同步都会触发UI重绘
                    )

                    // 如果有数据，增加当前页码
                    if (freshJournals.isNotEmpty()) {
                        currentPage++
                    }

                    logDPrintJournalList()
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
                // 确保同步状态被正确更新为完成
                isSyncing = false
            }
        }
    }

    fun logDPrintJournalList() {
        Log.d(TAG, "logDPrintJournalList: ${_uiState.value.journals}")
    }

}