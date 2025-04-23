package ovo.sypw.journal.ui.list

import ovo.sypw.journal.data.model.JournalData
import java.util.Date

/**
 * 日记列表UI状态
 * 包含列表所需的所有状态信息
 * 整合了EntryViewModel的同步状态
 */
data class JournalListState(
    val journals: List<JournalData> = emptyList(),
    val markedItems: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val hasMoreData: Boolean = true,
    val isScrolling: Boolean = false,
    val error: String? = null,
    val canUndo: Boolean = false,
    val scrollToPosition: Int? = null, // 需要滚动到的位置，null表示不需要滚动
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val syncTotal: Int = 0,
    val lastSyncTime: Date? = null,
    val forceRefresh: Long = 0 // 强制刷新触发器，使用时间戳确保每次同步都会触发UI重绘
) {
    companion object {
        val Initial = JournalListState(isLoading = true)
    }
}