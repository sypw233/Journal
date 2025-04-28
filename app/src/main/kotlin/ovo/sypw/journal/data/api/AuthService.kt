package ovo.sypw.journal.data.api

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
import ovo.sypw.journal.data.model.AuthResponse
import ovo.sypw.journal.data.model.AuthState
import ovo.sypw.journal.data.model.ChangePasswordRequest
import ovo.sypw.journal.data.model.LoginRequest
import ovo.sypw.journal.data.model.RegisterRequest
import ovo.sypw.journal.data.model.User
import ovo.sypw.journal.utils.SnackBarUtils
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证服务类，处理用户注册、登录和认证状态管理
 */
@Singleton
class AuthService @Inject constructor(private val context: Context) {
    private val TAG = "AuthService"
    
    // API基础URL
    private val BASE_URL = "http://10.0.2.2:8000" // 替换为实际的API地址
    
    // OkHttp客户端
    private val client = OkHttpClient.Builder().build()
    
    // SharedPreferences用于存储认证信息
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    // 认证状态
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        // 检查是否有保存的access token
        val savedAccessToken = prefs.getString(KEY_TOKEN, null)
        val savedUsername = prefs.getString(KEY_USERNAME, null)
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        
        if (savedAccessToken != null && savedUsername != null && savedEmail != null) {
            // 如果有保存的access token，创建用户对象并更新认证状态
            val user = User(
                username = savedUsername,
                email = savedEmail
            )
            _authState.value = AuthState.Authenticated(user, savedAccessToken)
        } else {
            _authState.value = AuthState.Unauthenticated()
        }
    }
    
    /**
     * 用户注册
     */
    suspend fun register(request: RegisterRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            
            val jsonObject = JSONObject().apply {
                put("username", request.username)
                put("email", request.email)
                put("phone",request.phone)
                put("password", request.password)
                request.phone?.let { put("phone", it) }
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            
            val httpRequest = Request.Builder()
                .url("$BASE_URL/users/register/")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
                
            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    
                    val refreshToken = jsonResponse.getString("refresh")
                    val accessToken = jsonResponse.getString("access")
                    val userJson = jsonResponse.getJSONObject("user")
                    
                    val user = User(
                        id = userJson.optInt("id", 0),
                        username = userJson.getString("username"),
                        email = userJson.optString("email",null),
                        phone = userJson.optString("phone", null),
                    )
                    
                    val authResponse = AuthResponse(user, refreshToken, accessToken)
                    
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
                .url("$BASE_URL/users/login/")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
                
            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    
                    val refreshToken = jsonResponse.getString("refresh")
                    val accessToken = jsonResponse.getString("access")
                    val userJson = jsonResponse.getJSONObject("user")
                    Log.d(TAG, "login: $userJson")
                    val user = User(
                        id = userJson.optInt("id", 0),
                        username = userJson.getString("username"),
                        email = userJson.optString("email",""),
                        phone = userJson.optString("phone", ""),
                        lastDataSyncTime = userJson.optString("last_data_sync_time", ""),
                        registerTime = userJson.optString("register_time", ""),
                        isStaff = userJson.optBoolean("is_staff", false),
                    )
                    
                    val authResponse = AuthResponse(user, refreshToken, accessToken)
                    
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
                .url("$BASE_URL/users/profile/")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val userJson = JSONObject(responseBody)
                    
                    val user = User(
                        id = userJson.optInt("id", 0),
                        username = userJson.getString("username"),
                        email = userJson.optString("email", null),
                        phone = userJson.optString("phone", null),
                        lastDataSyncTime = userJson.optString("last_data_sync_time", null),
                        registerTime = userJson.optString("register_time", null),
                        isStaff = userJson.optBoolean("is_staff", false),
                    )
                    
                    // 更新当前认证状态中的用户信息
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
    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> = withContext(Dispatchers.IO) {
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
                .url("$BASE_URL/users/change-password/")
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
        prefs.edit().apply {
            putString(KEY_TOKEN, authResponse.access)
            putString(KEY_REFRESH_TOKEN, authResponse.refresh)
            putString(KEY_USERNAME, authResponse.user.username)
            putString(KEY_EMAIL, authResponse.user.email)
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
     * 获取刷新令牌
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 刷新访问令牌
     * 使用刷新令牌获取新的访问令牌
     * @return 刷新结果，成功返回新的访问令牌，失败返回null
     */
    suspend fun refreshToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = getRefreshToken()
            if (refreshToken == null) {
                val errorMessage = "刷新令牌不存在"
                _authState.value = AuthState.Unauthenticated(errorMessage)
                return@withContext Result.failure(IOException(errorMessage))
            }

            val jsonObject = JSONObject().apply {
                put("refresh", refreshToken)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url("$BASE_URL/users/token/refresh/")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)

                    val newAccessToken = jsonResponse.getString("access")

                    // 更新保存的访问令牌
                    prefs.edit().putString(KEY_TOKEN, newAccessToken).apply()

                    // 更新认证状态
                    val currentUser = getCurrentUser()
                    if (currentUser != null) {
                        _authState.value = AuthState.Authenticated(currentUser, newAccessToken)
                    }

                    return@withContext Result.success(newAccessToken)
                } else {
                    // 如果刷新令牌也过期，则退出登录
                    if (response.code == 401) {
                        logout()
                        val errorMessage = "登录已过期，请重新登录"
                        _authState.value = AuthState.Unauthenticated(errorMessage)
                        return@withContext Result.failure(IOException(errorMessage))
                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "刷新令牌失败")
                        } catch (e: Exception) {
                            "刷新令牌失败: ${response.code}"
                        }

                        _authState.value = AuthState.Error(errorMessage)
                        return@withContext Result.failure(IOException(errorMessage))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh token error", e)
            val errorMessage = "刷新令牌失败: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            return@withContext Result.failure(e)
        }
    }
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_EMAIL = "auth_email"
    }
}