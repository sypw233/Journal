package ovo.sypw.journal.presentation.components.search

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 搜索结果信息组件
 * 显示搜索结果数量和清除搜索结果的按钮
 */
@Composable
fun SearchResultsInfo(
    isSearchMode: Boolean,
    isSearching: Boolean,
    resultCount: Int,
    onClearResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearchMode && !isSearching) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "找到 $resultCount 条结果",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (resultCount > 0) {
                TextButton(onClick = onClearResults) {
                    Text("清除")
                }
            }
        }
    }
} 