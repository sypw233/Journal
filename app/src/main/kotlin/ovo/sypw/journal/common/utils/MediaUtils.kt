package ovo.sypw.journal.common.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 媒体处理工具类
 * 提供图片压缩、保存和版本兼容处理
 */
object MediaUtils {
    private const val TAG = "MediaUtils"
    private const val QUALITY_DEFAULT = 80
    private const val MAX_IMAGE_WIDTH = 1280 // 最大宽度
    private const val MAX_IMAGE_HEIGHT = 1280 // 最大高度
    
    /**
     * 保存图片URI列表到应用私有目录
     * 
     * @param context 上下文
     * @param uris 图片URI列表
     * @param shouldCompress 是否压缩图片
     * @return 保存后的本地文件URI列表
     */
    suspend fun saveImageUris(
        context: Context,
        uris: List<Uri>?,
        shouldCompress: Boolean = true
    ): List<Uri> = withContext(Dispatchers.IO) {
        if (uris.isNullOrEmpty()) return@withContext emptyList()
        
        val resultUris = mutableListOf<Uri>()
        
        uris.forEach { uri ->
            try {
                // 获取输入流
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.let {
                    // 创建目标文件
                    val targetFile = createUniqueImageFile(context)
                    
                    if (shouldCompress) {
                        // 压缩图片
                        compressAndSaveImage(it, targetFile)
                    } else {
                        // 直接复制
                        copyToFile(it, targetFile)
                    }
                    
                    // 关闭输入流
                    it.close()
                    
                    // 获取文件URI
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile
                    )
                    
                    resultUris.add(fileUri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存图片失败: ${e.message}", e)
            }
        }
        
        return@withContext resultUris
    }
    
    /**
     * 创建唯一命名的图片文件
     */
    private fun createUniqueImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    /**
     * 压缩并保存图片
     */
    private fun compressAndSaveImage(
        inputStream: InputStream,
        outputFile: File,
        quality: Int = QUALITY_DEFAULT
    ) {
        // 解码图片 
        var bitmap = BitmapFactory.decodeStream(inputStream)
        
        // 判断是否需要缩放
        if (bitmap.width > MAX_IMAGE_WIDTH || bitmap.height > MAX_IMAGE_HEIGHT) {
            // 等比例缩放
            val scale = minOf(
                MAX_IMAGE_WIDTH.toFloat() / bitmap.width,
                MAX_IMAGE_HEIGHT.toFloat() / bitmap.height
            )
            
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        
        FileOutputStream(outputFile).use { output ->
            // 压缩图片
            ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, this)
                writeTo(output)
                close()
            }
        }
        
        // 释放资源
        bitmap.recycle()
    }
    
    /**
     * 直接将输入流复制到文件
     */
    private fun copyToFile(inputStream: InputStream, outputFile: File) {
        try {
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(4 * 1024) // 4K buffer
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        } catch (e: IOException) {
            Log.e(TAG, "复制文件失败", e)
            throw e
        }
    }
    
    /**
     * 从URI获取文件路径
     * 处理不同Android版本的兼容性
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        return try {
            // 对不同类型的URI进行处理
            when {
                // 文件URI
                uri.scheme.equals("file", ignoreCase = true) -> {
                    uri.path
                }
                // 媒体库URI
                uri.scheme.equals("content", ignoreCase = true) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10及以上使用ContentResolver复制文件
                        val targetFile = createUniqueImageFile(context)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            copyToFile(input, targetFile)
                        }
                        targetFile.absolutePath
                    } else {
                        // Android 9及以下可以直接获取路径
                        ImagePickerUtils.getRealPathFromUri(context, uri)
                    }
                }
                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取文件路径失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 处理兼容性问题，确保不同版本Android上都能正确使用图片URI
     */
    fun ensureUsableUri(context: Context, uri: Uri): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上，直接使用URI
            uri
        } else {
            try {
                // 获取路径并创建文件
                val path = getPathFromUri(context, uri)
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        // 返回FileProvider URI
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else {
                        uri
                    }
                } else {
                    uri
                }
            } catch (e: Exception) {
                Log.e(TAG, "URI兼容性处理失败: ${e.message}", e)
                uri
            }
        }
    }
    
    /**
     * 从URI创建临时文件
     * 用于解决某些第三方应用返回的URI在应用重启后无法访问的问题
     */
    suspend fun createTemporaryFile(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            
            val tempFile = createUniqueImageFile(context)
            
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            return@withContext tempFile
        } catch (e: Exception) {
            Log.e(TAG, "创建临时文件失败: ${e.message}", e)
            return@withContext null
        }
    }
} 