package ovo.sypw.journal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.ui.list.JournalListState
import ovo.sypw.journal.utils.SnackBarUtils
import java.util.LinkedList
import javax.inject.Inject

private const val TAG = "JournalListViewModel"
private const val PAGE_SIZE = 10

/**
 * 日记列表ViewModel
 * 负责管理日记列表的状态和业务逻辑
 */
@HiltViewModel
class JournalListViewModel @Inject constructor(
    private val repository: JournalRepository
) : ViewModel() {

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
                        error = null
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
     */
    fun resetList() {
        currentPage = 0
        _uiState.value = JournalListState.Initial
        loadNextPage()
    }
}