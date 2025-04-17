package ovo.sypw.journal.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.material3.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
                        userType = userJson.optString("user_type", "1"),
                        isActive = userJson.optBoolean("is_active", true)
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
                    
                    val user = User(
                        id = userJson.optInt("id", 0),
                        username = userJson.getString("username"),
                        email = userJson.optString("email",""),
                        phone = userJson.optString("phone", ""),
                        userType = userJson.optString("user_type", "free"),
                        isActive = userJson.optBoolean("is_active", true)
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
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_EMAIL = "auth_email"
    }
}