package ovo.sypw.journal.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.viewmodel.AuthViewModel
import ovo.sypw.journal.viewmodel.SyncViewModel


@Composable
fun UserDetailDialog(
    authViewModel: AuthViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    
    // 在组件加载时获取用户详情
    remember {
        authViewModel.getUserProfile()
        true
    }
    
    if (authState is AuthState.Authenticated) {
        val user = (authState as AuthState.Authenticated).user
        Log.d(TAG, "UserDetailDialog: $user")
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 用户头像
                    Surface(
                        modifier = Modifier
                            .size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.username.first().toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    
                    // 用户名
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 用户信息列表
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // 邮箱
                        user.email?.let {
                            Text(
                                text = "邮箱: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        // 手机号
                        user.phone?.let {
                            Text(
                                text = "手机号:$it",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        // 注册时间
                        user.registerDateTime()?.let {
                            Text(
                                text = "注册时间: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // 最后同步时间
                        user.lastSyncDateTime()?.let {
                            Text(
                                text = "最后同步时间: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    // 同步按钮
                    SyncButton(
                        syncViewModel = syncViewModel,
                        onSyncClick = { showSyncDialog = true }
                    )
                    
                    // 修改密码按钮
                    Button(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("修改密码")
                    }
                    
                    // 退出登录按钮
                    Button(
                        onClick = {
                            authViewModel.logout()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("退出登录")
                    }
                }
            }
        }
    }
    
    // 显示修改密码对话框
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            authViewModel = authViewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    // 显示同步状态对话框
    if (showSyncDialog) {
        SyncStatusDialog(
            syncViewModel = syncViewModel,
            onDismiss = { showSyncDialog = false }
        )
    }
}

@Composable
@Preview
fun UserDetailToastPreview() {
    var showToast = false
    UserDetailDialog(
        onDismiss = { showToast = false }
    )

}
