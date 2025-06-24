package ovo.sypw.journal.presentation.components.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

/**
 * 搜索输入框组件
 * 包含搜索文本框和搜索按钮，并支持输入内容动画效果
 */
@Composable
fun SearchInputField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onSearchIconPosition: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 动画相关状态
    val searchBoxWeight = remember { Animatable(1f) }
    val buttonWeight = remember { Animatable(0f) }

    // 根据搜索内容更新动画
    LaunchedEffect(searchQuery.isNotEmpty()) {
        // 搜索框宽度动画
        searchBoxWeight.animateTo(
            targetValue = if (searchQuery.isNotEmpty()) 0.8f else 1f,
            animationSpec = tween(300)
        )

        // 搜索按钮宽度动画
        buttonWeight.animateTo(
            targetValue = if (searchQuery.isNotEmpty()) 0.2f else 0f,
            animationSpec = tween(300)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索输入框
        Box(
            modifier = Modifier
                .weight(searchBoxWeight.value)
                .padding(end = if (searchQuery.isNotEmpty()) 8.dp else 0.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索日记...") },
                leadingIcon = null,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // 搜索按钮
        Box(
            modifier = if (buttonWeight.value > 0) {
                Modifier
                    .weight(buttonWeight.value)
                    .height(48.dp)
                    .background(
                        color = if (searchQuery.isNotEmpty())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(36.dp)
                    )
                    .let {
                        if (onSearchIconPosition != null) {
                            it.onGloballyPositioned(onSearchIconPosition)
                        } else {
                            it
                        }
                    }
            } else {
                Modifier
                    .width(0.dp)
                    .height(48.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier.matchParentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
} 