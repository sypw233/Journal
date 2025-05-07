package ovo.sypw.journal.presentation.screens

import ovo.sypw.journal.data.model.JournalData

/**
 * 日记列表UI状态
 * 包含列表所需的所有状态信息
 * 仅支持本地数据操作
 */
data class JournalListState(
    val journals: List<JournalData> = emptyList(),
    val markedItems: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val hasMoreData: Boolean = true,
    val isScrolling: Boolean = false,
    val error: String? = null,
    val canUndo: Boolean = false,
    val scrollToPosition: Int? = null // 需要滚动到的位置，null表示不需要滚动
) {
    companion object {
        val Initial = JournalListState(isLoading = true)
    }
}