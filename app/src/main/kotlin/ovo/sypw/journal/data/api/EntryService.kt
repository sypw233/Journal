package ovo.sypw.journal.data.api

import android.content.Context
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
import org.json.JSONArray
import org.json.JSONObject
import ovo.sypw.journal.data.model.Entry
import ovo.sypw.journal.data.model.EntryRequest
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日记条目服务类，处理日记条目的API请求
 */
@Singleton
class EntryService @Inject constructor(
    private val context: Context,
    private val authService: AuthService
) {
    private val TAG = "EntryService"
    
    // API基础URL
    private val BASE_URL = "http://10.0.2.2:8000/users" // 替换为实际的API地址

    // 创建一个互斥锁，用于防止多个请求同时刷新令牌
    private val refreshTokenLock = Any()

    // 标记是否正在刷新令牌
    private var isRefreshingToken = false

    // OkHttp客户端，添加拦截器处理401错误
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // 原始请求
            val originalRequest = chain.request()

            // 执行请求
            val response = chain.proceed(originalRequest)

            // 如果返回401 Unauthorized，尝试刷新令牌并重试请求
            if (response.code == 401) {
                response.close() // 关闭原始响应

                // 使用互斥锁确保同一时间只有一个请求在刷新令牌
                synchronized(refreshTokenLock) {
                    // 如果已经有其他请求在刷新令牌，等待完成
                    if (isRefreshingToken) {
                        // 等待其他请求完成刷新
                        while (isRefreshingToken) {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                Thread.currentThread().interrupt()
                                break
                            }
                        }
                    } else {
                        // 标记为正在刷新令牌
                        isRefreshingToken = true

                        try {
                            // 尝试刷新令牌
                            val refreshResult =
                                kotlinx.coroutines.runBlocking { authService.refreshToken() }

                            if (refreshResult.isSuccess) {
                                // 令牌刷新成功，使用新令牌重试请求
                                val newToken = refreshResult.getOrThrow()
                                val newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer $newToken")
                                    .build()

                                isRefreshingToken = false
                                return@addInterceptor chain.proceed(newRequest)
                            } else {
                                // 令牌刷新失败，可能是refresh token也过期了
                                // AuthService的refreshToken方法会处理登出逻辑
                                Log.e(
                                    TAG,
                                    "Token refresh failed: ${refreshResult.exceptionOrNull()?.message}"
                                )
                                isRefreshingToken = false
                                return@addInterceptor response
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error refreshing token", e)
                            isRefreshingToken = false
                            return@addInterceptor response
                        }
                    }

                    // 如果等待其他请求刷新令牌后，重试当前请求
                    val newToken = authService.getAccessToken()
                    if (newToken != null) {
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        return@addInterceptor chain.proceed(newRequest)
                    }
                }
            }

            // 返回原始响应
            return@addInterceptor response
        }
        .build()
    
    // 日期格式化
    private val dateFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * 创建新的日记条目
     */
    suspend fun createEntry(entryRequest: EntryRequest): Result<Entry> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            
            val token = authService.getAccessToken()
            if (token == null) {
                _errorMessage.value = "未登录，请先登录"
                return@withContext Result.failure(IOException("未登录，请先登录"))
            }
            
            val jsonObject = JSONObject().apply {
                put("text", entryRequest.text ?: "")
                put("date", dateFormat.format(entryRequest.date as TemporalAccessor?))
                put("is_mark", entryRequest.isMark)
                
                entryRequest.location?.let { location ->
                    val locationObj = JSONObject()
                    location.name?.let { locationObj.put("name", it) }
                    location.latitude?.let { locationObj.put("latitude", it) }
                    location.longitude?.let { locationObj.put("longitude", it) }
                    put("location", locationObj)
                }
                
                entryRequest.images?.let { images ->
                    val imagesArray = JSONArray()
                    images.forEach { imagesArray.put(it) }
                    put("images", imagesArray)
                }
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$BASE_URL/entries/")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    
                    val entry = parseEntryJson(jsonResponse)
                    return@withContext Result.success(entry)
                } else {
                    val errorMessage = "创建日记失败: ${response.message}"
                    _errorMessage.value = errorMessage
                    return@withContext Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建日记失败", e)
            val errorMessage = "创建日记失败: ${e.message}"
            _errorMessage.value = errorMessage
            return@withContext Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 获取所有日记条目
     */
    suspend fun getAllEntries(): Result<List<Entry>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            
            val token = authService.getAccessToken()
            if (token == null) {
                _errorMessage.value = "未登录，请先登录"
                return@withContext Result.failure(IOException("未登录，请先登录"))
            }
            
            val request = Request.Builder()
                .url("$BASE_URL/entries/")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonArray = JSONArray(responseBody)
                    Log.d(TAG, "getAllEntries: $jsonArray")
                    val entries = mutableListOf<Entry>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        entries.add(parseEntryJson(jsonObject))
                    }
                    
                    return@withContext Result.success(entries)
                } else {
                    val errorMessage = "获取日记列表失败: ${response.message}"
                    _errorMessage.value = errorMessage
                    return@withContext Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取日记列表失败", e)
            val errorMessage = "获取日记列表失败: ${e.message}"
            _errorMessage.value = errorMessage
            return@withContext Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 获取特定日记条目
     */
    suspend fun getEntry(id: Int): Result<Entry> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            
            val token = authService.getAccessToken()
            if (token == null) {
                _errorMessage.value = "未登录，请先登录"
                return@withContext Result.failure(IOException("未登录，请先登录"))
            }
            
            val request = Request.Builder()
                .url("$BASE_URL/entries/$id/")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody)
                    
                    val entry = parseEntryJson(jsonObject)
                    return@withContext Result.success(entry)
                } else {
                    val errorMessage = "获取日记详情失败: ${response.message}"
                    _errorMessage.value = errorMessage
                    return@withContext Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取日记详情失败", e)
            val errorMessage = "获取日记详情失败: ${e.message}"
            _errorMessage.value = errorMessage
            return@withContext Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 更新日记条目
     */
    suspend fun updateEntry(id: Int, entryRequest: EntryRequest): Result<Entry> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            
            val token = authService.getAccessToken()
            if (token == null) {
                _errorMessage.value = "未登录，请先登录"
                return@withContext Result.failure(IOException("未登录，请先登录"))
            }
            
            val jsonObject = JSONObject().apply {
                put("text", entryRequest.text ?: "")
                put("date", dateFormat.format(entryRequest.date as TemporalAccessor?))
                put("is_mark", entryRequest.isMark)
                
                entryRequest.location?.let { location ->
                    val locationObj = JSONObject()
                    location.name?.let { locationObj.put("name", it) }
                    location.latitude?.let { locationObj.put("latitude", it) }
                    location.longitude?.let { locationObj.put("longitude", it) }
                    put("location", locationObj)
                }
                
                entryRequest.images?.let { images ->
                    val imagesArray = JSONArray()
                    images.forEach { imagesArray.put(it) }
                    put("images", imagesArray)
                }
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$BASE_URL/entries/$id/")
                .put(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    
                    val entry = parseEntryJson(jsonResponse)
                    return@withContext Result.success(entry)
                } else {
                    val errorMessage = "更新日记失败: ${response.message}"
                    _errorMessage.value = errorMessage
                    return@withContext Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新日记失败", e)
            val errorMessage = "更新日记失败: ${e.message}"
            _errorMessage.value = errorMessage
            return@withContext Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 删除日记条目
     */
    suspend fun deleteEntry(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _errorMessage.value = null
            
            val token = authService.getAccessToken()
            if (token == null) {
                _errorMessage.value = "未登录，请先登录"
                return@withContext Result.failure(IOException("未登录，请先登录"))
            }
            
            val request = Request.Builder()
                .url("$BASE_URL/entries/$id/")
                .delete()
                .addHeader("Authorization", "Bearer $token")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext Result.success(true)
                } else {
                    val errorMessage = "删除日记失败: ${response.message}"
                    _errorMessage.value = errorMessage
                    return@withContext Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除日记失败", e)
            val errorMessage = "删除日记失败: ${e.message}"
            _errorMessage.value = errorMessage
            return@withContext Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 解析日记条目JSON
     */
    private fun parseEntryJson(json: JSONObject): Entry {
        val id = json.getInt("id")
        val text = if (json.has("text")) json.getString("text") else null
        val dateStr = json.getString("date")
        val date = try {
            OffsetDateTime.parse(dateStr, dateFormat).toInstant().let { Date.from(it) }
        } catch (e: Exception) {
            Date()
        }
        val isMark = if (json.has("is_mark")) json.getBoolean("is_mark") else false
        val userId = if (json.has("user_id")) json.getInt("user_id") else null
        
        // 解析位置信息
        val location = if (json.has("location") && !json.isNull("location")) {
            val locationJson = json.getJSONObject("location")
            val name = if (locationJson.has("name")) locationJson.getString("name") else null
            val latitude = if (locationJson.has("latitude")) locationJson.getDouble("latitude") else null
            val longitude = if (locationJson.has("longitude")) locationJson.getDouble("longitude") else null
            ovo.sypw.journal.data.model.LocationData(name, latitude, longitude)
        } else null
        
        // 解析图片列表
        val images = if (json.has("images") && !json.isNull("images")) {
            val imagesArray = json.getJSONArray("images")
            val imagesList = mutableListOf<String>()
            for (i in 0 until imagesArray.length()) {
                imagesList.add(imagesArray.getString(i))
            }
            imagesList
        } else null
        
        // 解析创建和更新时间
        val createdAt = if (json.has("created_at")) {
            try {
                OffsetDateTime.parse(json.getString("created_at"), dateFormat).toInstant().let { Date.from(it) }
            } catch (e: Exception) {
                null
            }
        } else null
        
        val updatedAt = if (json.has("updated_at")) {
            try {
                OffsetDateTime.parse(json.getString("updated_at"), dateFormat).toInstant().let { Date.from(it) }
            } catch (e: Exception) {
                null
            }
        } else null
        
        return Entry(
            id = id,
            text = text,
            date = date,
            location = location,
            images = images,
            isMark = isMark,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}