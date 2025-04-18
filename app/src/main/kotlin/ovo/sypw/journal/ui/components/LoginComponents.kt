package ovo.sypw.journal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.utils.SnackBarUtils
import ovo.sypw.journal.viewmodel.AuthViewModel

/**
 * 登录对话框组件
 */
@Composable
fun LoginDialog(
    authViewModel: AuthViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("sypw") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("wesd2008") }
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
    authViewModel: AuthViewModel = viewModel(),
    onClick: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var showUserDetailDialog by remember { mutableStateOf(false) }

    when (authState) {
        is AuthState.Authenticated -> {
            val user = (authState as AuthState.Authenticated).user
            // 显示用户头像或用户名首字母
            Surface(
                modifier = Modifier
                    .size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                onClick = { showUserDetailDialog = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.username.first().toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // 显示用户详情浮窗Card
            if (showUserDetailDialog) {
                UserDetailDialog(
                    authViewModel = authViewModel,
                    onDismiss = { showUserDetailDialog = false }
                )
            }
        }

        else -> {
            // 显示登录按钮
            Button(
                onClick = onClick,
                modifier = Modifier.height(36.dp)
            ) {
                Text("登录")
            }
        }
    }
}


// UserMenu组件已被UserDetailDialog替代