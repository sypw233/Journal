package ovo.sypw.journal.ui.main

import ovo.sypw.journal.data.model.JournalData

/**
 * MainScreen的UI状态
 * 使用sealed class定义不同的状态
 */
sealed class MainScreenState {
    /**
     * 初始状态
     */
    object Initial : MainScreenState()

    /**
     * 加载中状态
     */
    object Loading : MainScreenState()

    /**
     * 加载成功状态
     * @property journals 日记列表
     * @property hasMoreData 是否有更多数据
     * @property isBottomSheetExpanded 底部表单是否展开
     * @property markedItems 标记的日记ID集合
     */
    data class Success(
        val journals: List<JournalData> = emptyList(),
        val hasMoreData: Boolean = true,
        val isBottomSheetExpanded: Boolean = false,
        val markedItems: Set<Int> = emptySet(),
        val bottomSheetHeight: Float = 30f,
        val isScrolling: Boolean = false
    ) : MainScreenState()

    /**
     * 加载失败状态
     * @property error 错误信息
     */
    data class Error(val error: String) : MainScreenState()
}

/**
 * BottomSheet的UI状态
 */
data class BottomSheetState(
    val isExpanded: Boolean = false,
    val height: Float = 30f
)