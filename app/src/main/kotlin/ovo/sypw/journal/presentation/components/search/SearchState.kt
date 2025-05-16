package ovo.sypw.journal.presentation.components.search

import java.util.Date

/**
 * 搜索栏状态数据类
 * 用于在各个搜索相关组件之间共享状态
 */
data class SearchState(
    // 搜索关键词
    val searchQuery: String = "",
    // 搜索类型选择
    val searchByContent: Boolean = true,
    val searchByLocation: Boolean = false,
    val searchByDate: Boolean = false,
    // 日期范围
    val startDate: Date? = null,
    val endDate: Date? = null,
    // 日期选择器状态
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false
)

/**
 * 搜索事件，用于处理搜索相关操作
 */
sealed class SearchEvent {
    // 更新搜索关键词
    data class UpdateSearchQuery(val query: String) : SearchEvent()
    
    // 切换搜索类型
    data class ToggleSearchByContent(val enabled: Boolean) : SearchEvent()
    data class ToggleSearchByLocation(val enabled: Boolean) : SearchEvent()
    data class ToggleSearchByDate(val enabled: Boolean) : SearchEvent()
    
    // 日期选择
    data class SetStartDate(val date: Date?) : SearchEvent()
    data class SetEndDate(val date: Date?) : SearchEvent()
    data class ToggleStartDatePicker(val show: Boolean) : SearchEvent()
    data class ToggleEndDatePicker(val show: Boolean) : SearchEvent()
    
    // 执行搜索
    object Search : SearchEvent()
    
    // 重置搜索
    object Reset : SearchEvent()
} 