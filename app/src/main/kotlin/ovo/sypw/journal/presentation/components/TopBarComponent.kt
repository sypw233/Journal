package ovo.sypw.journal.presentation.components

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AssistWalker
import androidx.compose.material.icons.outlined.DataUsage
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    scrollBehavior: TopAppBarScrollBehavior,
    authViewModel: AuthViewModel = hiltViewModel(),
    autoSyncManager: AutoSyncManager? = null,
    onShowLoginDialog: () -> Unit,
    onSearchClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDatabaseManagement: () -> Unit = {},
    onOpenSentimentAnalysis: () -> Unit = {},
    onOpenAIChat: () -> Unit = {},
    searchButtonAlpha: Float = 1f,
    onSearchButtonPosition: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    val titleFontSizeAnimate = lerp(30.sp, 20.sp, scrollBehavior.state.overlappedFraction)
    var showLoginDialog by remember { mutableStateOf(false) }
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
                // 添加搜索按钮
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .alpha(searchButtonAlpha)
                        .onGloballyPositioned { coordinates ->
                            onSearchButtonPosition?.invoke(coordinates)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 添加同步按钮
                autoSyncManager?.let {
                    SyncStatusButton(
                        autoSyncManager = it,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 更多选项下拉菜单
                CustomDropdownMenu(
                    authViewModel = authViewModel,
                    onShowLoginDialog = onShowLoginDialog,
                    onOpenTestActivity = {
                        val intent = Intent(context, TestActivity::class.java)
                        forActivityResult.launch(intent)
                    },
                    onOpenSettings = onOpenSettings,
                    onOpenDatabaseManagement = onOpenDatabaseManagement,
                    onOpenSentimentAnalysis = onOpenSentimentAnalysis,
                    onOpenAIChat = onOpenAIChat
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
    onOpenTestActivity: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenDatabaseManagement: () -> Unit = {},
    onOpenSentimentAnalysis: () -> Unit = {},
    onOpenAIChat: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    // 用户详情对话框状态
    var showUserDetailDialog by remember { mutableStateOf(false) }

    // 检查用户是否已登录
    val isLoggedIn = authState is AuthState.Authenticated
    val user = if (isLoggedIn) {
        (authState as AuthState.Authenticated).user
    } else {
        null
    }

    val username = user?.username ?: "未登录"

    // 显示用户详情对话框
    if (showUserDetailDialog && isLoggedIn) {
        UserDetailDialog(
            authViewModel = authViewModel,
            onDismiss = { showUserDetailDialog = false }
        )
    }

    Box {
        // 更多选项按钮
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多选项",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(280.dp)
        ) {
            // 用户信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
//                ),
                shape = MaterialTheme.shapes.large,
//                elevation = CardDefaults.cardElevation(
//                    defaultElevation = 2.dp
//                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = false
                            if (isLoggedIn) {
                                // 已登录状态，显示用户详情
                                showUserDetailDialog = true
                            } else {
                                // 未登录状态，显示登录对话框
                                onShowLoginDialog()
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 用户头像
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLoggedIn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoggedIn) {
                            // 已登录状态显示用户名首字母
                            Text(
                                text = username.first().toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        } else {
                            // 未登录状态显示默认图标
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "默认头像",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 用户信息
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isLoggedIn && !user?.email.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击登录账号",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 菜单项 - 设置
            DropdownMenuItem(
                text = {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            // 菜单项 - 数据库管理
            DropdownMenuItem(
                text = {
                    Text(
                        "数据库管理",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.DataUsage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onOpenDatabaseManagement()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 菜单项 - 情感分析
            DropdownMenuItem(
                text = {
                    Text(
                        "情感分析(施工中...)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onOpenSentimentAnalysis()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 菜单项 - AI聊天
            DropdownMenuItem(
                text = {
                    Text(
                        "AI聊天",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onOpenAIChat()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 菜单项 - 关于
            DropdownMenuItem(
                text = {
                    Text(
                        "关于",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    SnackBarUtils.showSnackBar("什么都没有")
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 菜单项 - 测试页面
            DropdownMenuItem(
                text = {
                    Text(
                        "测试页面",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.AssistWalker,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onOpenTestActivity()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}