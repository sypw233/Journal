package ovo.sypw.journal.presentation.components

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovo.sypw.journal.R
import ovo.sypw.journal.TestActivity
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.presentation.viewmodels.AuthViewModel
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel

/**
 * 顶部应用栏组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarView(
    scope: CoroutineScope,
    scrollBehavior: TopAppBarScrollBehavior,
    scaffoldState: BottomSheetScaffoldState,
    markedSet: Set<Any?>,
    authViewModel: AuthViewModel = viewModel(),
    journalListViewModel: JournalListViewModel = viewModel()
) {
    val titleFontSizeAnimate = lerp(30.sp, 20.sp, scrollBehavior.state.overlappedFraction)
    var showLoginDialog by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }
    // 显示登录对话框
    if (showLoginDialog) {
        LoginDialog(
            authViewModel = authViewModel,
            onDismiss = { showLoginDialog = false }
        )
    }

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


            val context = LocalContext.current
            val forActivityResult = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    SnackBarUtils.showSnackBar(data?.data.toString())
                }
            }
            // 添加登录/用户头像按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 显示标记数量
//                Box(
//                    modifier = Modifier
//                        .size(24.dp)
//                        .background(MaterialTheme.colorScheme.primary, CircleShape)
//                        .padding(4.dp)
//                ) {
//                    Text(
//                        text = markedSet.size.toString(),
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        fontSize = 12.sp
//                    )
//                }
//                Spacer(modifier = Modifier.width(8.dp))
                UserAvatar(
                    authViewModel = authViewModel,
                    onClick = {
                        val currentState = authViewModel.authState.value
                        if (currentState is AuthState.Authenticated) {
                            showUserMenu = true
                        } else {
                            showLoginDialog = true
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

//              打开添加框
                IconButton(onClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }

                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Search"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
//                添加新条目
                IconButton(onClick = {

                }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 跳转到测试页面
                IconButton(onClick = {
//                    showSyncDialog=true
                    val intent = Intent(context, TestActivity::class.java)
                    forActivityResult.launch(intent)
                    SnackBarUtils.showSnackBar("Turn to TestActivity")
                }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Test"
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}