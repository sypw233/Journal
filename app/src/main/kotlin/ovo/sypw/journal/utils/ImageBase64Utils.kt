package ovo.sypw.journal.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 图片Base64转换工具类
 * 提供图片转Base64编码的功能
 */
object ImageBase64Utils {

    /**
     * 将Uri指向的图片转换为Base64编码字符串
     * @param context 上下文
     * @param imageUri 图片Uri
     * @param quality 图片质量 (1-100)，仅对JPEG格式有效
     * @param maxWidth 最大宽度，如果图片宽度超过此值将被缩放
     * @param maxHeight 最大高度，如果图片高度超过此值将被缩放
     * @return 格式化的Base64字符串，格式为：data:image/{format};base64,{base64_string}
     */
    fun uriToBase64(context: Context, imageUri: Uri, quality: Int = 100): String? {
        try {
            // 获取图片的输入流
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                // 解码图片
                var bitmap = BitmapFactory.decodeStream(stream)

                // 如果需要，缩放图片
//                bitmap = scaleBitmapIfNeeded(bitmap, maxWidth, maxHeight)

                // 获取图片格式
                val mimeType = getMimeType(context, imageUri) ?: "image/jpeg"
                val format = when {
                    mimeType.contains("png") -> Bitmap.CompressFormat.PNG
                    mimeType.contains("webp") -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }

                // 压缩图片到字节数组
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(format, quality, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // 转换为Base64
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                // 根据格式构建完整的Data URL
                return when (format) {
                    Bitmap.CompressFormat.PNG -> "data:image/png;base64,$base64String"
                    Bitmap.CompressFormat.WEBP -> "data:image/webp;base64,$base64String"
                    else -> "data:image/jpeg;base64,$base64String"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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
     * 批量转换多个Uri为Base64字符串列表
     */
    fun urisToBase64List(
        context: Context,
        imageUris: List<Uri>,
        quality: Int = 90,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): List<String> {
        return imageUris.mapNotNull { uri ->
            uriToBase64(context, uri, quality)
        }
    }
}