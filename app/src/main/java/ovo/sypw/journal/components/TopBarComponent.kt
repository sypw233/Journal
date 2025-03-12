package ovo.sypw.journal.components

import SnackbarUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
                listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, scrollThreshold)
            }
            val progress = (scrollOffset / scrollThreshold).coerceIn(0f, 1f)
            lerp(maxFontSize, minFontSize, progress)
        }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        title = {
            Text(
                modifier = Modifier.padding(bottom = 5.dp),
                text = "Journal",
                fontSize = titleFontSize,
                maxLines = 1,
            )
        }, actions = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )

            ) {
                IconButton(
                    onClick = {
                        SnackbarUtils.showSnackbar("MARK $markedList")
                    },

                    ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.filter_alt_icon_24),
                        contentDescription = "Filter Icon",
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )

            ) {
                IconButton(onClick = {
//                TODO
                }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu Icon"
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))

        }, scrollBehavior = scrollBehavior
    )
}