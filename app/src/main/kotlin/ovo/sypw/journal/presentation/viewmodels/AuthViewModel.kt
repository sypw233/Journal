package ovo.sypw.journal.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.data.model.ChangePasswordRequest
import ovo.sypw.journal.data.model.LoginRequest
import ovo.sypw.journal.data.model.RegisterRequest
import ovo.sypw.journal.data.model.User
import ovo.sypw.journal.data.remote.api.AuthService
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

    /**
     * 修改密码
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            SnackBarUtils.showSnackBar("请填写完整信息")
            return
        }

        viewModelScope.launch {
            authService.changePassword(ChangePasswordRequest(oldPassword, newPassword))
        }
    }

    /**
     * 获取用户详情
     */
    fun getUserProfile() {
        viewModelScope.launch {
            authService.getUserProfile()
        }
    }

    /**
     * 获取当前用户
     */
    fun getCurrentUser(): User? {
        return authService.getCurrentUser()
    }
}