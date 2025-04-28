package ovo.sypw.journal.data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 用户数据模型
 */
data class User(
    val id: Int = 0,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val isStaff: Boolean = false,
    val registerTime: String? = null,
    val lastDataSyncTime: String? = null,
) {
    // 将ISO 8601格式的字符串转换为LocalDateTime
    fun registerDateTime(): LocalDateTime? = registerTime?.let {
        LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
    }
    
    fun lastSyncDateTime(): LocalDateTime? = lastDataSyncTime?.let {
        LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
    }
}

/**
 * 用户注册请求模型
 */
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

/**
 * 用户登录请求模型
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 修改密码请求模型
 */
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

/**
 * 认证响应模型
 */
data class AuthResponse(
    val user: User,
    val refresh: String,
    val access: String
)

/**
 * 认证状态
 */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User, val accessToken: String) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}