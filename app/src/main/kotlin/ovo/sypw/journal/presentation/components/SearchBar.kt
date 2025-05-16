package ovo.sypw.journal.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovo.sypw.journal.presentation.components.search.SearchBar
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel

/**
 * 搜索栏组件的重定向
 * 为了向后兼容，保留了原始的导入路径，但实际使用新的模块化组件
 * @see ovo.sypw.journal.presentation.components.search.SearchBar
 */
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    journalListViewModel: JournalListViewModel,
    onBackClick: () -> Unit = {},
    showBackButton: Boolean = true,
    onSearchIconPosition: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    // 重定向到新的搜索组件
    ovo.sypw.journal.presentation.components.search.SearchBar(
        modifier = modifier,
        journalListViewModel = journalListViewModel,
        onBackClick = onBackClick,
        showBackButton = showBackButton,
        onSearchIconPosition = onSearchIconPosition
    )
} 