package ovo.sypw.journal.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.viewmodel.JournalListViewModel

/**
 * 刷新按钮组件
 * 显示在顶部应用栏中，用于刷新日记列表
 * 使用JournalListViewModel管理刷新操作
 */
@Composable
fun SyncButton(
    journalListViewModel: JournalListViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by journalListViewModel.uiState.collectAsState()
    
    // 旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500)
        ),
        label = "refresh_rotation"
    )
    
    Box(modifier = modifier) {
        if (uiState.isLoading) {
            // 显示加载进度
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            // 显示刷新按钮
            IconButton(
                onClick = {
                    journalListViewModel.resetList()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新列表",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}