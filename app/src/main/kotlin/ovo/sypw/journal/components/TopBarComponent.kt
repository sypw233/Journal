package ovo.sypw.journal.components

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import ovo.sypw.journal.R
import ovo.sypw.journal.TestActivity
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
    val titleFontSizeAnimate = lerp(30.sp, 20.sp, scrollBehavior.state.overlappedFraction)
    MediumTopAppBar(
        title = {
            Text(
                text = "Journal",
                fontSize = titleFontSizeAnimate
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

            val context = LocalContext.current
            val forActivityResult = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    SnackBarUtils.showSnackBar(data?.data.toString())
                }
            }
            // 添加其他操作按钮
            IconButton(onClick = {
                val intent = Intent(context, TestActivity::class.java)
                forActivityResult.launch(intent)
                SnackBarUtils.showSnackBar("Turn to TestActivity")
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