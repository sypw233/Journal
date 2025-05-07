package ovo.sypw.journal.presentation.screens

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
     * @property isBottomSheetExpanded 底部表单是否展开
     * @property bottomSheetHeight 底部表单高度
     * @property isScrolling 是否正在滚动
     */
    data class Success(
        val isBottomSheetExpanded: Boolean = false,
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