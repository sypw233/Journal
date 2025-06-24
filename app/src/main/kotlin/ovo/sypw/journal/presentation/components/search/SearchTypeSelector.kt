package ovo.sypw.journal.presentation.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 搜索类型选择组件
 * 允许用户选择搜索类型：内容、位置、日期范围
 */
@Composable
fun SearchTypeSelector(
    searchByContent: Boolean,
    searchByLocation: Boolean,
    searchByDate: Boolean,
    onSearchByContentChange: (Boolean) -> Unit,
    onSearchByLocationChange: (Boolean) -> Unit,
    onSearchByDateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 内容搜索选项
        SearchTypeOption(
            checked = searchByContent,
            onCheckedChange = onSearchByContentChange,
            label = "内容"
        )

        // 位置搜索选项
        SearchTypeOption(
            checked = searchByLocation,
            onCheckedChange = onSearchByLocationChange,
            label = "位置"
        )

        // 日期范围搜索选项
        SearchTypeOption(
            checked = searchByDate,
            onCheckedChange = onSearchByDateChange,
            label = "日期范围"
        )
    }
}

/**
 * 搜索类型单选项
 */
@Composable
private fun SearchTypeOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
} 