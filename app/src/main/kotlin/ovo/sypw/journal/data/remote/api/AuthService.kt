package ovo.sypw.journal.data.remote.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AuthResponse
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.data.model.ChangePasswordRequest
import ovo.sypw.journal.data.model.LoginRequest
import ovo.sypw.journal.data.model.RegisterRequest
import ovo.sypw.journal.data.model.User
import ovo.sypw.journal.data.remote.api.APIUrl.API_URL
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证服务类，处理用户注册、登录和认证状态管理
 */
@Singleton
class AuthService @Inject constructor(private val context: Context) {
    private val TAG = "AuthService"


    // OkHttp客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // SharedPreferences用于存储认证信息
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // 认证状态
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // 初始化时先设置为加载状态
        _authState.value = AuthState.Loading

        // 检查是否有保存的access token
        val savedAccessToken = prefs.getString(KEY_TOKEN, null)
        val savedUsername = prefs.getString(KEY_USERNAME, null)
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        val tokenExpiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0)

        if (savedAccessToken != null && savedUsername != null) {
            // 检查token是否已过期
            val currentTime = System.currentTimeMillis()
            if (tokenExpiryTime > currentTime) {
                // token没有过期，直接设置为认证状态
                val user = User(
                    username = savedUsername,
                    email = savedEmail ?: ""
                )
                _authState.value = AuthState.Authenticated(user, savedAccessToken)

                // 如果token快过期了（比如还有30分钟过期），可以在后台刷新token
                if (tokenExpiryTime - currentTime < 30 * 60 * 1000) {
                    // 后台刷新token
                    refreshTokenInBackground()
                }
            } else {
                // token已过期，设置为未认证状态
                Log.d(TAG, "Token已过期，需要重新登录")
                _authState.value = AuthState.Unauthenticated("登录已过期，请重新登录")
                // 清除过期的token信息
                logout()
            }
        } else {
            // 没有保存的token，设置为未认证状态
            _authState.value = AuthState.Unauthenticated()
        }
    }

    /**
     * 后台刷新token
     * 在token即将过期时调用，保持用户登录状态
     */
    private fun refreshTokenInBackground() {
        // 在后台线程中执行token刷新
        Thread {
            try {
                // 尝试刷新token
                val result = kotlinx.coroutines.runBlocking {
                    refreshToken()
                }

                if (!result) {
                    // 如果刷新失败，验证当前token
                    val isValid = kotlinx.coroutines.runBlocking {
                        validateToken()
                    }

                    if (!isValid) {
                        // token已失效且无法刷新，需要重新登录
                        Log.d(TAG, "Token已失效且无法刷新，需要重新登录")
                        _authState.value = AuthState.Unauthenticated("登录已过期，请重新登录")
                        logout()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新token失败", e)
            }
        }.start()
    }

    /**
     * 验证token有效性
     * 返回token是否有效
     */
    suspend fun validateToken(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            if (token == null) {
                Log.d(TAG, "无token，未登录状态")
                _authState.value = AuthState.Unauthenticated("未登录")
                return@withContext false
            }

            // 使用token调用用户信息接口来验证token有效性
            val httpRequest = Request.Builder()
                .url("$API_URL/userinfo/")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    // token有效，更新用户信息和token过期时间
                    val responseBody = response.body!!.string()
                    val userJson = JSONObject(responseBody).getJSONObject("user")

                    val user = User(
                        username = userJson.getString("username"),
                        email = userJson.optString("email", ""),
                        phone = userJson.optString("phone", ""),
                        isStaff = userJson.optBoolean("is_staff", false),
                    )

                    // 更新当前认证状态中的用户信息
                    _authState.value = AuthState.Authenticated(user, token)

                    // 更新token过期时间（假设为24小时）
                    updateTokenExpiry(24 * 60 * 60 * 1000L)

                    Log.d(TAG, "Token验证成功")
                    return@withContext true
                } else {
                    // token无效或过期
                    val errorMessage = when (response.code) {
                        401 -> "登录已过期，请重新登录"
                        403 -> "权限不足，请重新登录"
                        else -> "登录状态异常，请重新登录"
                    }

                    Log.d(TAG, "Token验证失败: ${response.code}")

                    // 尝试刷新token
                    val refreshed = refreshToken()
                    if (refreshed) {
                        Log.d(TAG, "Token刷新成功")
                        return@withContext true
                    }

                    // 刷新失败，清除认证信息并更新状态
                    logout()
                    _authState.value = AuthState.Unauthenticated(errorMessage)
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "验证token出错", e)
            // 网络错误或其他异常时，保持当前状态，不强制登出
            // 如果需要处理特定错误，可以在这里添加条件判断
            return@withContext false
        }
    }

    /**
     * 更新token过期时间
     * @param validityInMillis token有效期（毫秒）
     */
    private fun updateTokenExpiry(validityInMillis: Long) {
        val expiryTime = System.currentTimeMillis() + validityInMillis
        prefs.edit().putLong(KEY_TOKEN_EXPIRY, expiryTime).apply()
    }

    /**
     * 用户注册
     */
    suspend fun register(request: RegisterRequest): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                _authState.value = AuthState.Loading

                val jsonObject = JSONObject().apply {
                    put("username", request.username)
                    put("email", request.email)
                    put("password", request.password)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val httpRequest = Request.Builder()
                    .url("$API_URL/register/")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body!!.string()
                        val jsonResponse = JSONObject(responseBody)
                        val accessToken = jsonResponse.getString("token")
                        val userJson = jsonResponse.getJSONObject("user")

                        val user = User(
                            username = userJson.getString("username"),
                            email = userJson.optString("email", ""),
                        )

                        val authResponse = AuthResponse(user, accessToken)

                        // 保存认证信息
                        saveAuthInfo(authResponse)

                        // 更新认证状态
                        _authState.value = AuthState.Authenticated(user, accessToken)

                        Result.success(authResponse)


                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "注册失败")
                        } catch (e: Exception) {
                            "注册失败: ${response.code}"
                        }

                        _authState.value = AuthState.Error(errorMessage)
                        Result.failure(IOException(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Register error", e)
                val errorMessage = "注册失败: ${e.message}"
                SnackBarUtils.showSnackBar(errorMessage)
                _authState.value = AuthState.Error(errorMessage)
                Result.failure(e)
            }
        }

    /**
     * 用户登录
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading

            val jsonObject = JSONObject().apply {
                put("username", request.username)
                put("password", request.password)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url("$API_URL/token/")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body!!.string()
                    val jsonResponse = JSONObject(responseBody)

                    val accessToken = jsonResponse.getString("token")
                    val username = jsonResponse.getString("username")
                    val user = User(
                        username = username,
                    )

                    val authResponse = AuthResponse(user, accessToken)

                    // 保存认证信息
                    saveAuthInfo(authResponse)

                    // 更新认证状态
                    _authState.value = AuthState.Authenticated(user, accessToken)

                    Result.success(authResponse)
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", "登录失败")
                    } catch (e: Exception) {
                        "登录失败: ${response.code}"
                    }

                    _authState.value = AuthState.Unauthenticated(errorMessage)
                    Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            val errorMessage = "登录失败: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(e)
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        // 清除保存的认证信息
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            apply()
        }

        // 更新认证状态
        _authState.value = AuthState.Unauthenticated()
    }

    /**
     * 获取认证token
     */
    fun getAuthToken(): String? {
        return when (val state = authState.value) {
            is AuthState.Authenticated -> state.accessToken
            else -> null
        }
    }

    /**
     * 获取当前用户
     */
    fun getCurrentUser(): User? {
        return when (val state = authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    /**
     * 检查用户是否已登录
     * @return 如果用户已登录返回true，否则返回false
     */
    fun isLoggedIn(): Boolean {
        return authState.value is AuthState.Authenticated
    }

    /**
     * 获取用户详情
     */
    suspend fun getUserProfile(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken()
            if (token == null) {
                val errorMessage = "未登录"
                _authState.value = AuthState.Unauthenticated(errorMessage)
                return@withContext Result.failure(IOException(errorMessage))
            }

            val httpRequest = Request.Builder()
                .url("$API_URL/userinfo/")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body!!.string()
                    val userJson = JSONObject(responseBody).getJSONObject("user")

                    val user = User(
                        username = userJson.getString("username"),
                        email = userJson.optString("email", ""),
                        phone = userJson.optString("phone", ""),
                        isStaff = userJson.optBoolean("is_staff", false),
                    )

                    // 更新当前认证状态中的用户信
                    if (authState.value is AuthState.Authenticated) {
                        val currentToken = (authState.value as AuthState.Authenticated).accessToken
                        _authState.value = AuthState.Authenticated(user, currentToken)
                    }

                    Result.success(user)
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", "获取用户信息失败")
                    } catch (e: Exception) {
                        "获取用户信息失败: ${response.code}"
                    }

                    if (response.code == 401) {
                        _authState.value = AuthState.Unauthenticated(errorMessage)
                    } else {
                        _authState.value = AuthState.Error(errorMessage)
                    }

                    Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get user profile error", e)
            val errorMessage = "获取用户信息失败: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(e)
        }
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    val errorMessage = "未登录"
                    _authState.value = AuthState.Unauthenticated(errorMessage)
                    return@withContext Result.failure(IOException(errorMessage))
                }

                val jsonObject = JSONObject().apply {
                    put("old_password", request.oldPassword)
                    put("new_password", request.newPassword)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val httpRequest = Request.Builder()
                    .url("$API_URL/users/change-password/")
                    .put(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        SnackBarUtils.showSnackBar("密码修改成功")
                        Result.success(Unit)
                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "密码修改失败")
                        } catch (e: Exception) {
                            "密码修改失败: ${response.code}"
                        }

                        if (response.code == 401) {
                            _authState.value = AuthState.Unauthenticated(errorMessage)
                        } else {
                            SnackBarUtils.showSnackBar(errorMessage)
                        }

                        Result.failure(IOException(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Change password error", e)
                val errorMessage = "密码修改失败: ${e.message}"
                SnackBarUtils.showSnackBar(errorMessage)
                Result.failure(e)
            }
        }

    /**
     * 保存认证信息到SharedPreferences
     */
    private fun saveAuthInfo(authResponse: AuthResponse) {
        // 设置token过期时间，假设有效期为24小时
        val expiryTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000L

        prefs.edit().apply {
            putString(KEY_TOKEN, authResponse.access)
            putString(KEY_USERNAME, authResponse.user.username)
            putString(KEY_EMAIL, authResponse.user.email)
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
            apply()
        }
    }

    /**
     * 获取访问令牌
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * 刷新token
     * 尝试使用refresh token获取新的access token
     * 实际使用时应根据后端实现来修改
     */
    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentToken = getAuthToken()
            if (currentToken == null) {
                return@withContext false
            }

            // 示例：使用当前token获取新token
            // 注意：实际应用中，应该使用refresh_token而不是access_token来获取新token
            val httpRequest = Request.Builder()
                .url("$API_URL/token/refresh/")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .addHeader("Authorization", "Bearer $currentToken")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body!!.string()
                    val jsonResponse = JSONObject(responseBody)
                    val newToken = jsonResponse.optString("token", null)

                    if (newToken != null) {
                        // 更新存储的token
                        val currentUser = getCurrentUser()
                        if (currentUser != null) {
                            // 保存新token
                            val authResponse = AuthResponse(currentUser, newToken)
                            saveAuthInfo(authResponse)
                            // 更新认证状态
                            _authState.value = AuthState.Authenticated(currentUser, newToken)
                            Log.d(TAG, "Token刷新成功")
                            return@withContext true
                        }
                    }
                }
                // 刷新失败
                Log.d(TAG, "Token刷新失败: ${response.code}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新token出错", e)
            return@withContext false
        }
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_TOKEN_EXPIRY = "auth_token_expiry"
    }
}