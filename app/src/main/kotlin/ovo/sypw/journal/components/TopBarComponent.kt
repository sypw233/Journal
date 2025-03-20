package ovo.sypw.journal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import ovo.sypw.journal.R
import ovo.sypw.journal.utils.SnackBarUtils

/**
 * 顶部应用栏组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarView(
    scrollBehavior: TopAppBarScrollBehavior,
    listState: LazyListState,
    markedList: MutableSet<Int>
) {
    val titleFontSize =
        remember(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            val maxFontSize = 30.sp
            val minFontSize = 22.sp
            val scrollThreshold = 150f // 滚动阈值
            val scrollOffset = if (listState.firstVisibleItemIndex > 0) {
                scrollThreshold
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
            lerp(maxFontSize, minFontSize, (scrollOffset / scrollThreshold).coerceIn(0f, 1f))
        }

    TopAppBar(
        title = {
            Text(
                text = "Journal",
                fontSize = titleFontSize
            )
        },
//        navigationIcon = {
//            IconButton(onClick = {
//                SnackbarUtils.showSnackbar("Menu Clicked")
//            }) {
//                Icon(Icons.Filled.Menu, contentDescription = "Menu")
//            }
//        },
        actions = {
            // 显示标记数量
            if (markedList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp)
                ) {
                    Text(
                        text = markedList.size.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 添加其他操作按钮
            IconButton(onClick = {
                SnackBarUtils.showSnackBar("Search Clicked")
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Search"
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}