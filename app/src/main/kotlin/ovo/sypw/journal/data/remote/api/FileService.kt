package ovo.sypw.journal.data.remote.api

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ovo.sypw.journal.common.utils.SnackBarUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件服务类，处理文件列表、上传、下载等操作
 */
@Singleton
class FileService @Inject constructor(
    private val context: Context,
    private val authService: AuthService
) {
    private val TAG = "FileService"

    // API基础URL
    private val BASE_URL = "http://10.0.2.2:8000/api"

    // OkHttp客户端
    private val client = OkHttpClient.Builder().build()

    /**
     * 获取文件列表
     *
     * @param path 要列出的目录路径（可选）
     * @return 文件列表的结果
     */
    suspend fun listFiles(path: String? = null): Result<FileListResponse> =
        withContext(Dispatchers.IO) {
            try {
                val token = authService.getAuthToken()
                if (token == null) {
                    val errorMessage = "未登录"
                    return@withContext Result.failure(IOException(errorMessage))
                }

                val url = if (path != null) {
                    "$BASE_URL/files/list/?path=$path"
                } else {
                    "$BASE_URL/files/list/"
                }

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body!!.string()
                        val jsonResponse = JSONObject(responseBody)

                        val status = jsonResponse.getString("status")
                        val currentPath = jsonResponse.getString("path")
                        val itemsArray = jsonResponse.getJSONArray("items")

                        val items = mutableListOf<FileItem>()
                        for (i in 0 until itemsArray.length()) {
                            val itemJson = itemsArray.getJSONObject(i)
                            val item = FileItem(
                                name = itemJson.getString("name"),
                                type = itemJson.getString("type"),
                                size = itemJson.getLong("size"),
                                modified = itemJson.getLong("modified"),
                                url = if (itemJson.has("url")) itemJson.getString("url") else null
                            )
                            items.add(item)
                        }

                        Result.success(FileListResponse(status, currentPath, items))
                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "获取文件列表失败")
                        } catch (e: Exception) {
                            "获取文件列表失败: ${response.code}"
                        }

                        Result.failure(IOException(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "List files error", e)
                val errorMessage = "获取文件列表失败: ${e.message}"
                Result.failure(e)
            }
        }

    /**
     * 上传数据库文件
     *
     * @param file 要上传的文件
     * @param path 上传目标路径（可选）
     * @return 上传结果
     */
    suspend fun uploadDatabaseFile(file: File, path: String? = null): Result<FileUploadResponse> =
        withContext(Dispatchers.IO) {
            try {
                val token = authService.getAuthToken()
                if (token == null) {
                    val errorMessage = "未登录"
                    return@withContext Result.failure(IOException(errorMessage))
                }

                // 获取文件MIME类型
                val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: "application/octet-stream"

                // 创建MultipartBody
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody(mimeType.toMediaTypeOrNull())
                    )

                // 添加路径参数（如果有）
                if (path != null) {
                    requestBody.addFormDataPart("path", path)
                }

                val request = Request.Builder()
                    .url("$BASE_URL/files/upload/")
                    .post(requestBody.build())
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body!!.string()
                        val jsonResponse = JSONObject(responseBody)

                        val status = jsonResponse.getString("status")
                        val filePath = jsonResponse.getString("path")
                        val url = jsonResponse.getString("url")
                        val size = jsonResponse.getLong("size")

                        Result.success(FileUploadResponse(status, filePath, url, size))
                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "上传文件失败")
                        } catch (e: Exception) {
                            "上传文件失败: ${response.code}"
                        }

                        Result.failure(IOException(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload file error", e)
                val errorMessage = "上传文件失败: ${e.message}"
                SnackBarUtils.showSnackBar(errorMessage)
                Result.failure(e)
            }
        }

    /**
     * 下载数据库文件
     *
     * @param path 文件路径
     * @param destinationDir 下载目标目录
     * @return 下载的文件，如果失败则返回null
     */
    suspend fun downloadDatabaseFile(path: String, destinationDir: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val token = authService.getAuthToken()
                if (token == null) {
                    val errorMessage = "未登录"
                    return@withContext Result.failure(IOException(errorMessage))
                }

                val request = Request.Builder()
                    .url("$BASE_URL/files/download/?path=$path")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        // 获取文件名
                        val fileName = path.substring(path.lastIndexOf('/') + 1)

                        // 确保目标目录存在
                        if (!destinationDir.exists()) {
                            destinationDir.mkdirs()
                        }

                        // 创建目标文件
                        val destinationFile = File(destinationDir, fileName)

                        // 将响应写入文件
                        response.body?.let { body ->
                            FileOutputStream(destinationFile).use { output ->
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(4096)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                    }
                                    output.flush()
                                }
                            }
                            Result.success(destinationFile)
                        } ?: Result.failure(IOException("响应体为空"))
                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "下载文件失败")
                        } catch (e: Exception) {
                            "下载文件失败: ${response.code}"
                        }

                        Result.failure(IOException(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download file error", e)
                val errorMessage = "下载文件失败: ${e.message}"
                SnackBarUtils.showSnackBar(errorMessage)
                Result.failure(e)
            }
        }

    /**
     * 删除文件或目录
     *
     * @param path 文件或目录路径
     * @return 删除结果
     */
    suspend fun deleteFile(path: String): Result<FileDeleteResponse> = withContext(Dispatchers.IO) {
        try {
            val token = authService.getAuthToken()
            if (token == null) {
                val errorMessage = "未登录"
                return@withContext Result.failure(IOException(errorMessage))
            }

            val jsonObject = JSONObject().apply {
                put("path", path)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            Log.d(TAG, "deleteFile: $jsonObject")
            Log.d(TAG, "deleteFile: ${requestBody}")
            val request = Request.Builder()
                .url("$BASE_URL/files/delete/")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
            Log.d(TAG, "deleteFile: $request")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body!!.string()
                    val jsonResponse = JSONObject(responseBody)

                    val status = jsonResponse.getString("status")
                    val message = jsonResponse.getString("message")

                    Result.success(FileDeleteResponse(status, message))
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", "删除文件失败")
                    } catch (e: Exception) {
                        "删除文件失败: ${response.code}"
                    }

                    Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete file error", e)
            val errorMessage = "删除文件失败: ${e.message}"
            SnackBarUtils.showSnackBar(errorMessage)
            Result.failure(e)
        }
    }

    /**
     * 创建目录
     *
     * @param path 新目录路径
     * @return 创建结果
     */
    suspend fun createDirectory(path: String): Result<FileDeleteResponse> =
        withContext(Dispatchers.IO) {
            try {
                val token = authService.getAuthToken()
                if (token == null) {
                    val errorMessage = "未登录"
                    return@withContext Result.failure(IOException(errorMessage))
                }

                val jsonObject = JSONObject().apply {
                    put("path", path)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL/files/mkdir/")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body!!.string()
                        val jsonResponse = JSONObject(responseBody)

                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        Result.success(FileDeleteResponse(status, message))
                    } else {
                        val errorBody = response.body?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").optString("message", "创建目录失败")
                        } catch (e: Exception) {
                            "创建目录失败: ${response.code}"
                        }

                        Result.failure(IOException(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create directory error", e)
                val errorMessage = "创建目录失败: ${e.message}"
                SnackBarUtils.showSnackBar(errorMessage)
                Result.failure(e)
            }
        }
}

/**
 * 文件列表响应
 */
data class FileListResponse(
    val status: String,
    val path: String,
    val items: List<FileItem>
)

/**
 * 文件项
 */
data class FileItem(
    val name: String,
    val type: String,
    val size: Long,
    val modified: Long,
    val url: String?
)

/**
 * 文件上传响应
 */
data class FileUploadResponse(
    val status: String,
    val path: String,
    val url: String,
    val size: Long
)

/**
 * 文件删除响应
 */
data class FileDeleteResponse(
    val status: String,
    val message: String
) 