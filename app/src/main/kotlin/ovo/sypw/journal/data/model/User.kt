package ovo.sypw.journal.data.model

/**
 * 用户数据模型
 */
data class User(
    val id: Int = 0,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val userType: String = "1",  // 1 or 0
    val isActive: Boolean = true,
    val isStaff: Boolean = false,
    val isSuperuser: Boolean = false,
    val lastLogin: String? = null,
    val dateJoined: String? = null
)

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