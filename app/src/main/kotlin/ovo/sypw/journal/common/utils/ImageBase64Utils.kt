package ovo.sypw.journal.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * 图片Base64工具类
 * 用于将图片转换为Base64格式，主要用于API请求
 */
object ImageBase64Utils {
    private const val TAG = "ImageBase64Utils"
    private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
    private const val QUALITY = 80 // 压缩质量
    
    /**
     * 将Uri转换为Base64格式
     * @param context 上下文
     * @param uri 图片Uri
     * @return Base64字符串，格式为 data:image/jpeg;base64,xxx，失败返回null
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            // 打开输入流
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for uri: $uri")
                return null
            }
            
            // 解码为Bitmap
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from uri: $uri")
                return null
            }
            
            // 压缩并转换为Base64
            val base64 = compressAndEncodeToBase64(bitmap)
            bitmap.recycle()
            
            base64
        } catch (e: Exception) {
            Log.e(TAG, "Error converting uri to base64: $uri", e)
            null
        }
    }
    
    /**
     * 压缩Bitmap并转换为Base64格式
     * @param bitmap 原始Bitmap
     * @return Base64字符串，格式为 data:image/jpeg;base64,xxx
     */
    private fun compressAndEncodeToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        
        // 计算初始压缩质量
        var quality = QUALITY
        
        // 压缩图片
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        // 如果图片太大，继续压缩
        while (outputStream.size() > MAX_IMAGE_SIZE && quality > 10) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        
        // 转换为Base64
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        
        try {
            outputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing output stream", e)
        }
        
        // 返回带前缀的Base64字符串
        return "data:image/jpeg;base64,$base64"
    }

    /**
     * 获取Uri对应的MIME类型
     */
    private fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri) ?: run {
            // 如果ContentResolver无法确定类型，尝试从Uri的路径获取扩展名
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (fileExtension != null) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase())
            } else {
                null
            }
        }
    }

    /**
     * 如果需要，缩放位图以适应最大尺寸
     */
    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 如果图片尺寸已经在限制范围内，直接返回
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // 计算缩放比例
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scaleFactor = minOf(scaleWidth, scaleHeight)

        // 计算新尺寸
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        // 缩放位图
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 将多个Uri指向的图片转换为Base64编码字符串列表
     */
    fun urisToBase64(
        context: Context,
        imageUris: List<Uri>
    ): List<String> {
        return imageUris.mapNotNull { uri ->
            uriToBase64(context, uri)
        }
    }
}