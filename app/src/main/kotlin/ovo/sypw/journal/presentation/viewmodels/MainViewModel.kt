package ovo.sypw.journal.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.presentation.screens.MainScreenState
import javax.inject.Inject

private const val TAG = "MainViewModel"

/**
 * MainScreen的ViewModel
 * 负责管理UI状态和处理业务逻辑
 * 主要管理底部表单和滚动状态
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val preferences: JournalPreferences
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<MainScreenState>(MainScreenState.Initial)
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        // 初始化数据
        initialize()
    }

    /**
     * 初始化数据
     */
    fun initialize() {
        _uiState.value = MainScreenState.Loading

        // 检查是否是首次启动
        if (preferences.isFirstLaunch()) {
            viewModelScope.launch {
                // 首次启动初始化示例数据
                val sampleData = List(10) { index ->
                    JournalData(
                        id = index,
                        isMarkdown = true,
                        text="# $index \n这是一个日记应用")
                }
                repository.insertJournals(sampleData)
                // 设置首次启动标志为false
                preferences.setFirstLaunch(false)
            }
        }

        // 更新UI状态为成功
        _uiState.update { MainScreenState.Success() }
    }

    /**
     * 设置底部表单状态
     */
    fun setBottomSheetExpanded(expanded: Boolean) {
        val currentState = _uiState.value
        if (currentState is MainScreenState.Success) {
            _uiState.value = currentState.copy(isBottomSheetExpanded = expanded)
        }
    }

    /**
     * 设置底部表单高度
     */
    fun setBottomSheetHeight(height: Float) {
        val currentState = _uiState.value
        if (currentState is MainScreenState.Success) {
            _uiState.value = currentState.copy(bottomSheetHeight = height)
        }
    }

    /**
     * 设置滚动状态
     */
    fun setScrolling(isScrolling: Boolean) {
        val currentState = _uiState.value
        if (currentState is MainScreenState.Success) {
            _uiState.value = currentState.copy(isScrolling = isScrolling)
        }
    }

    /**
     * 添加新日记
     */
    fun addJournal(newJournal: JournalData) {
        viewModelScope.launch {
            try {
                // 使用Repository直接添加日记
                val id = repository.getJournalLastId() + 1
                val journalWithId = newJournal.copy(id = id)
                repository.insertJournal(journalWithId)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}