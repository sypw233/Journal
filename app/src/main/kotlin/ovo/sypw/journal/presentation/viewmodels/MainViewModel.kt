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
 * 主要管理底部表单和滚动状态，列表数据由JournalListViewModel管理
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val preferences: JournalPreferences,
) : ViewModel() {

    // 创建JournalListViewModel实例
    val journalListViewModel = JournalListViewModel(repository)

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
                        text = "《恋爱猪脚饭》——工地与猪脚饭交织的浪漫邂逅！\n" + "\"当你以为人生已经烂尾时，命运的混凝土搅拌机正在偷偷运转！\"\n" + "破产老哥黄夏揣着最后的房租钱，逃进花都城中村的握手楼。本想和小茂等挂壁老哥一起吃猪脚饭躺平摆烂，却意外邂逅工地女神\"陈嘉怡\"，从而开启新的土木逆袭人生。\n" + "爽了，干土木的又爽了！即使在底层已经彻底有了的我们，也能通过奋斗拥有美好的明天！"
                    )
                }
                repository.insertJournals(sampleData)
                // 重置JournalListViewModel以加载新数据
                journalListViewModel.resetList()
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

    fun addJournal(newJournal: JournalData) {
        journalListViewModel.addJournal(newJournal)

    }
}