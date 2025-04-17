package ovo.sypw.journal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.api.AuthService
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.data.model.LoginRequest
import ovo.sypw.journal.data.model.RegisterRequest
import ovo.sypw.journal.utils.SnackBarUtils
import javax.inject.Inject

/**
 * 认证相关的ViewModel，处理登录和注册的业务逻辑
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {
    
    // 暴露AuthService的认证状态
    val authState: StateFlow<AuthState> = authService.authState
    
    /**
     * 登录方法
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            SnackBarUtils.showSnackBar("请填写完整信息")
            return
        }
        
        viewModelScope.launch {
            authService.login(LoginRequest(username, password))
        }
    }
    
    /**
     * 注册方法
     */
    fun register(username: String, email: String, password: String, phone: String) {
        if (username.isBlank() || password.isBlank()) {
            SnackBarUtils.showSnackBar("请填写完整信息")
            return
        }
        
        viewModelScope.launch {
            authService.register(RegisterRequest(username, email, password, phone))
        }
    }
    
    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }
}