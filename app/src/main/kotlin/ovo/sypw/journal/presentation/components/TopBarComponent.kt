package ovo.sypw.journal.presentation.components

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AssistWalker
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovo.sypw.journal.R
import ovo.sypw.journal.TestActivity
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.presentation.components.UserDetailDialog
import ovo.sypw.journal.presentation.viewmodels.AuthViewModel
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
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
    journalListViewModel: JournalListViewModel = viewModel(),
    databaseManagementViewModel: DatabaseManagementViewModel = viewModel(),
    autoSyncManager: AutoSyncManager? = null,
    onShowLoginDialog: () -> Unit
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
            // 添加操作按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 添加同步按钮
                autoSyncManager?.let {
                    SyncStatusButton(
                        autoSyncManager = it,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 打开添加框
                IconButton(onClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加日记"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 更多选项下拉菜单
                CustomDropdownMenu(
                    authViewModel = authViewModel,
                    onShowLoginDialog = onShowLoginDialog,
                    onOpenTestActivity = {
                        val intent = Intent(context, TestActivity::class.java)
                        forActivityResult.launch(intent)
                        SnackBarUtils.showSnackBar("Turn to TestActivity")
                    }
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun CustomDropdownMenu(
    authViewModel: AuthViewModel,
    onShowLoginDialog: () -> Unit,
    onOpenTestActivity: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    // 用户详情对话框状态
    var showUserDetailDialog by remember { mutableStateOf(false) }

    // 检查用户是否已登录
    val isLoggedIn = authState is AuthState.Authenticated
    val username = if (isLoggedIn) {
        (authState as AuthState.Authenticated).user.username
    } else {
        "未登录"
    }

    // 显示用户详情对话框
    if (showUserDetailDialog && isLoggedIn) {
        UserDetailDialog(
            authViewModel = authViewModel,
            onDismiss = { showUserDetailDialog = false }
        )
    }

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 用户信息项 - 使用自定义布局而不是标准的DropdownMenuItem
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            expanded = false
                            if (isLoggedIn) {
                                // 已登录状态，显示用户详情
                                showUserDetailDialog = true
                            } else {
                                // 未登录状态，显示登录对话框
                                onShowLoginDialog()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 用户头像
                    if (isLoggedIn) {
                        // 已登录状态显示头像
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = username.first().toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    } else {
                        // 未登录状态显示默认图标
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "默认头像",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 用户名和状态信息
                    Column {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(4.dp))

            // 设置选项
            DropdownMenuItem(
                text = { Text("设置") },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    /* 打开设置页面 */
                }
            )

            // 关于选项
            DropdownMenuItem(
                text = { Text("关于") },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    /* 打开关于页面 */
                }
            )

            // 测试页面选项
            DropdownMenuItem(
                text = { Text("测试页面") },
                leadingIcon = { Icon(Icons.Outlined.AssistWalker, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenTestActivity()
                }
            )
        }
    }
}