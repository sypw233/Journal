package ovo.sypw.journal.presentation.viewmodels

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.presentation.screens.JournalListState
import java.util.Date
import java.util.LinkedList
import javax.inject.Inject

private const val TAG = "JournalListViewModel"
private const val PAGE_SIZE = 10

/**
 * 日记列表ViewModel
 * 负责管理日记列表的状态和业务逻辑
 * 仅支持本地数据操作
 */
@HiltViewModel
class JournalListViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val preferences: JournalPreferences
) : ViewModel() {
    // UI 状态
    private val _uiState = MutableStateFlow(JournalListState())
    val uiState: StateFlow<JournalListState> = _uiState.asStateFlow()
    
    // 日记列表 - 用于情感分析屏幕
    private val _journals = MutableStateFlow<List<JournalData>>(emptyList())
    val journals: StateFlow<List<JournalData>> = _journals.asStateFlow()
    
    // 已删除日记的历史记录，用于撤销删除操作
    private val deletedJournals = LinkedList<JournalData>()
    
    // 分页相关
    private var currentPage = 0
    private var searchQuery = ""
    
    // 删除确认对话框状态
    val showDeleteConfirmDialog = mutableStateOf(false)
    var journalToDelete = mutableStateOf<Int?>(null)
    
    init {
        loadNextPage()
    }
    
    /**
     * 加载所有日记 - 用于情感分析屏幕
     */
    fun loadJournals() {
        viewModelScope.launch {
            try {
                val allJournalsFlow = repository.getAllJournals()
                allJournalsFlow.collect { journals ->
                    _journals.value = journals
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading all journals", e)
                SnackBarUtils.showSnackBar("加载失败: ${e.message}")
            }
        }
    }
    
    /**
     * 加载下一页数据
     */
    fun loadNextPage() {
        if (!_uiState.value.hasMoreData || _uiState.value.isLoading) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val offset = currentPage * PAGE_SIZE
                val journals = if (searchQuery.isEmpty()) {
                    repository.getJournalsPaged(offset, PAGE_SIZE)
                } else {
                    repository.searchJournalsByContent(searchQuery)
                }
                
                // 检查是否还有更多数据
                val hasMore = journals.isNotEmpty() && journals.size >= PAGE_SIZE
                
                // 更新UI状态
                _uiState.update { currentState ->
                    currentState.copy(
                        journals = currentState.journals + journals,
                        hasMoreData = hasMore,
                        isLoading = false
                    )
                }
                
                // 更新页码
                if (journals.isNotEmpty()) {
                    currentPage++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading journals", e)
                _uiState.update { it.copy(isLoading = false) }
                SnackBarUtils.showSnackBar("加载失败: ${e.message}")
            }
        }
    }

    /**
     * 删除日记
     * 从本地数据库删除日记
     */
    fun deleteJournal(id: Int) {
        // 保存要删除的日记ID
        journalToDelete.value = id
        
        // 使用DeleteConfirmationUtils中的delete方法
        // 这里不直接调用，而是设置状态，由UI层处理
        showDeleteConfirmDialog.value = preferences.isDeleteConfirmationEnabled()
        
        // 如果不需要确认，则直接删除
        if (!preferences.isDeleteConfirmationEnabled()) {
            performDelete(id)
        }
    }

    /**
     * 执行删除操作
     * 实际执行删除日记的逻辑
     */
    fun performDelete(id: Int) {
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
                
                // 使用带有撤销按钮的Snackbar
                SnackBarUtils.showActionSnackBar(
                    message = "已删除 #${id}",
                    actionLabel = "撤销",
                    onActionPerformed = { undoDelete() },
                    onDismissed = { }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error removing journal", e)
                SnackBarUtils.showSnackBar("删除失败: ${e.message}")
            }
        }
    }

    /**
     * 确认删除
     * 用于UI层确认删除后调用
     */
    fun confirmDelete() {
        journalToDelete.value?.let { id ->
            performDelete(id)
            journalToDelete.value = null
            showDeleteConfirmDialog.value = false
        }
    }

    /**
     * 取消删除
     * 用于UI层取消删除确认
     */
    fun cancelDelete() {
        journalToDelete.value = null
        showDeleteConfirmDialog.value = false
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
     * 重置列表数据
     * 用于在添加或修改日记后刷新列表
     */
    fun resetList() {
        currentPage = 0
        _uiState.update { it.copy(journals = emptyList(), hasMoreData = true) }
        loadNextPage()
    }

    /**
     * 添加新的日记
     */
    fun addJournal(journal: JournalData) {
        viewModelScope.launch {
            try {
                // 使用数据库ID计数器生成新ID
                val nextId = getNextId()
                val newJournal = journal.copy(id = nextId)
                repository.insertJournal(newJournal)
                SnackBarUtils.showSnackBar("添加成功: ${newJournal.id}")

                // 重置列表数据以刷新列表
                currentPage = 0
                _uiState.update { 
                    it.copy(
                        journals = emptyList(), 
                        hasMoreData = true,
                        scrollToPosition = 0  // 设置滚动位置为0，确保滚动到列表顶部
                    ) 
                }
                loadNextPage()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding journal", e)
                SnackBarUtils.showSnackBar("添加失败: ${e.message}")
            }
        }
    }

    /**
     * 获取下一个ID
     */
    private suspend fun getNextId(): Int {
        return repository.getJournalLastId() + 1
    }

    /**
     * 创建新的日记条目
     */
    fun createEntry(journalData: JournalData, imageUris: List<Uri>? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 保存到本地数据库
                repository.insertJournal(journalData)
                SnackBarUtils.showSnackBar("创建日记成功")

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

                // 更新本地数据库
                repository.updateJournal(journalData)
                SnackBarUtils.showSnackBar("更新日记成功")

                // 更新UI状态
                _uiState.update { currentState ->
                    currentState.copy(
                        journals = currentState.journals.map {
                            if (it.id == journalData.id) journalData else it
                        }
                    )
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
    fun getJournal(id: Int, callback: (JournalData?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 从本地获取
                val localJournal = repository.getJournalById(id)
                callback(localJournal)
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
     * 更新日记
     * 用于编辑界面更新日记
     */
    fun updateJournal(journalData: JournalData) {
        updateEntry(journalData)
    }

    /**
     * 搜索日记 - 按内容
     * @param query 搜索关键词
     */
    fun searchJournalsByContent(query: String) {
        if (query.isBlank()) {
            resetSearchMode()
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSearching = true, searchQuery = query) }
                
                val results = repository.searchJournalsByContent(query)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isSearchMode = true,
                        searchResults = results,
                        isSearching = false
                    )
                }
                
                Log.d(TAG, "Content search completed. Found ${results.size} results for query: $query")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching journals by content", e)
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = "搜索失败: ${e.message}"
                    ) 
                }
                SnackBarUtils.showSnackBar("搜索失败: ${e.message}")
            }
        }
    }
    
    /**
     * 搜索日记 - 按日期范围
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun searchJournalsByDateRange(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSearching = true) }
                
                val results = repository.searchJournalsByDateRange(startDate, endDate)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isSearchMode = true,
                        searchResults = results,
                        isSearching = false,
                        searchQuery = "${formatDate(startDate)} - ${formatDate(endDate)}"
                    )
                }
                
                Log.d(TAG, "Date range search completed. Found ${results.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching journals by date range", e)
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = "搜索失败: ${e.message}"
                    ) 
                }
                SnackBarUtils.showSnackBar("搜索失败: ${e.message}")
            }
        }
    }
    
    /**
     * 搜索日记 - 按位置
     * @param locationName 位置名称
     */
    fun searchJournalsByLocation(locationName: String) {
        if (locationName.isBlank()) {
            resetSearchMode()
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSearching = true, searchQuery = locationName) }
                
                val results = repository.searchJournalsByLocation(locationName)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isSearchMode = true,
                        searchResults = results,
                        isSearching = false
                    )
                }
                
                Log.d(TAG, "Location search completed. Found ${results.size} results for location: $locationName")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching journals by location", e)
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = "搜索失败: ${e.message}"
                    ) 
                }
                SnackBarUtils.showSnackBar("搜索失败: ${e.message}")
            }
        }
    }
    
    /**
     * 综合搜索日记(内容或位置)
     * @param query 搜索关键词
     */
    fun searchJournalsByContentOrLocation(query: String) {
        if (query.isBlank()) {
            resetSearchMode()
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSearching = true, searchQuery = query) }
                
                val results = repository.searchJournalsByContentOrLocation(query)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isSearchMode = true,
                        searchResults = results,
                        isSearching = false
                    )
                }
                
                Log.d(TAG, "Combined search completed. Found ${results.size} results for query: $query")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching journals by content or location", e)
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = "搜索失败: ${e.message}"
                    ) 
                }
                SnackBarUtils.showSnackBar("搜索失败: ${e.message}")
            }
        }
    }
    
    /**
     * 退出搜索模式
     */
    fun resetSearchMode() {
        _uiState.update { 
            it.copy(
                isSearchMode = false,
                searchResults = emptyList(),
                searchQuery = "",
                isSearching = false
            ) 
        }
    }
    
    /**
     * 格式化日期，用于显示
     */
    private fun formatDate(date: Date): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return format.format(date)
    }
}