package ovo.sypw.journal.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.presentation.viewmodels.AuthViewModel

/**
 * 登录对话框组件
 */
@Composable
fun LoginDialog(
    authViewModel: AuthViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("admin") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // 监听认证状态
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                SnackBarUtils.showSnackBar("登录成功")
                onDismiss()
            }

            is AuthState.Error -> {
                isLoading = false
                SnackBarUtils.showSnackBar((authState as AuthState.Error).message)
            }

            is AuthState.Loading -> {
                isLoading = true
            }

            is AuthState.Unauthenticated -> {
                isLoading = false
                val message = (authState as AuthState.Unauthenticated).message
                if (message != null) {
                    SnackBarUtils.showSnackBar(message)
                }
            }

            else -> {}
        }
    }

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
                Text(
                    text = if (isLogin) "登录" else "注册",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 用户名输入框
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 注册时显示邮箱输入框
                if (!isLogin) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("邮箱") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
                // 注册时显示手机号输入框
                if (!isLogin) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("手机号") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 密码输入框
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 登录/注册按钮
                Button(
                    onClick = {
                        if (isLogin) {
                            // 登录
                            authViewModel.login(username, password)
                        } else {
                            // 注册
                            authViewModel.register(username, email, password, phone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isLogin) "登录" else "注册")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 切换登录/注册模式
                TextButton(
                    onClick = { isLogin = !isLogin },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isLogin) "没有账号？去注册" else "已有账号？去登录")
                }
            }
        }
    }
}

/**
 * 用户头像组件
 * 点击后显示用户详细信息的浮窗Card
 */
@Composable
fun UserAvatar(
    userName: String = "User",
    size: Dp = 24.dp
) {
    var showUserDetailDialog by remember { mutableStateOf(false) }


    // 显示用户头像或用户名首字母
    Surface(
        modifier = Modifier
            .size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        onClick = { showUserDetailDialog = true }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = userName.first().toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

}


// UserMenu组件已被UserDetailDialog替代