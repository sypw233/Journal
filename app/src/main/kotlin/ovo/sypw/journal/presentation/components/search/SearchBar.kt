package ovo.sypw.journal.presentation.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel
import java.util.Date

/**
 * 搜索栏主组件
 * 整合了搜索框、搜索类型选择、日期范围选择器和搜索结果信息等子组件
 */
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    journalListViewModel: JournalListViewModel,
    onBackClick: () -> Unit = {},
    showBackButton: Boolean = true,
    onSearchIconPosition: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    // 从ViewModel获取UI状态
    val uiState by journalListViewModel.uiState.collectAsState()
    
    // 本地状态管理
    var searchState by remember { 
        mutableStateOf(
            SearchState(
                searchQuery = uiState.searchQuery
            )
        ) 
    }
    
    // 处理搜索事件
    val handleSearchEvent: (SearchEvent) -> Unit = { event ->
        when (event) {
            is SearchEvent.UpdateSearchQuery -> {
                searchState = searchState.copy(searchQuery = event.query)
                if (event.query.isEmpty()) {
                    journalListViewModel.resetSearchMode()
                }
            }
            is SearchEvent.ToggleSearchByContent -> {
                searchState = searchState.copy(searchByContent = event.enabled)
            }
            is SearchEvent.ToggleSearchByLocation -> {
                searchState = searchState.copy(searchByLocation = event.enabled)
            }
            is SearchEvent.ToggleSearchByDate -> {
                searchState = searchState.copy(searchByDate = event.enabled)
            }
            is SearchEvent.SetStartDate -> {
                searchState = searchState.copy(startDate = event.date)
            }
            is SearchEvent.SetEndDate -> {
                searchState = searchState.copy(endDate = event.date)
            }
            is SearchEvent.ToggleStartDatePicker -> {
                searchState = searchState.copy(showStartDatePicker = event.show)
            }
            is SearchEvent.ToggleEndDatePicker -> {
                searchState = searchState.copy(showEndDatePicker = event.show)
            }
            SearchEvent.Search -> {
                performSearch(searchState, journalListViewModel)
            }
            SearchEvent.Reset -> {
                searchState = SearchState()
                journalListViewModel.resetSearchMode()
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 搜索栏顶部（搜索输入框和返回按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            if (showBackButton) {
                IconButton(onClick = {
                    onBackClick()
                    handleSearchEvent(SearchEvent.Reset)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 搜索输入框和搜索按钮
            SearchInputField(
                searchQuery = searchState.searchQuery,
                onSearchQueryChange = { 
                    handleSearchEvent(SearchEvent.UpdateSearchQuery(it))
                },
                onSearch = { handleSearchEvent(SearchEvent.Search) },
                onClear = { handleSearchEvent(SearchEvent.Reset) },
                onSearchIconPosition = onSearchIconPosition
            )
        }
        
        // 搜索类型选择
        SearchTypeSelector(
            searchByContent = searchState.searchByContent,
            searchByLocation = searchState.searchByLocation,
            searchByDate = searchState.searchByDate,
            onSearchByContentChange = { handleSearchEvent(SearchEvent.ToggleSearchByContent(it)) },
            onSearchByLocationChange = { handleSearchEvent(SearchEvent.ToggleSearchByLocation(it)) },
            onSearchByDateChange = { handleSearchEvent(SearchEvent.ToggleSearchByDate(it)) }
        )
        
        // 日期范围选择
        DateRangeSelector(
            visible = searchState.searchByDate,
            startDate = searchState.startDate,
            endDate = searchState.endDate,
            showStartDatePicker = searchState.showStartDatePicker,
            showEndDatePicker = searchState.showEndDatePicker,
            onStartDateChange = { handleSearchEvent(SearchEvent.SetStartDate(it)) },
            onEndDateChange = { handleSearchEvent(SearchEvent.SetEndDate(it)) },
            onShowStartDatePicker = { handleSearchEvent(SearchEvent.ToggleStartDatePicker(it)) },
            onShowEndDatePicker = { handleSearchEvent(SearchEvent.ToggleEndDatePicker(it)) }
        )
        
        // 搜索结果信息
        SearchResultsInfo(
            isSearchMode = uiState.isSearchMode,
            isSearching = uiState.isSearching,
            resultCount = uiState.searchResults.size,
            onClearResults = { handleSearchEvent(SearchEvent.Reset) }
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
    }
}

/**
 * 执行搜索操作
 */
private fun performSearch(
    searchState: SearchState,
    journalListViewModel: JournalListViewModel
) {
    with(searchState) {
        when {
            searchByDate && startDate != null && endDate != null -> {
                journalListViewModel.searchJournalsByDateRange(startDate, endDate)
            }
            searchByLocation -> {
                journalListViewModel.searchJournalsByLocation(searchQuery)
            }
            searchByContent && searchByLocation -> {
                journalListViewModel.searchJournalsByContentOrLocation(searchQuery)
            }
            else -> {
                journalListViewModel.searchJournalsByContent(searchQuery)
            }
        }
    }
} 