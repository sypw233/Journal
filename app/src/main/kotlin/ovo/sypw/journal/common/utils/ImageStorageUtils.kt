package ovo.sypw.journal.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图片存储工具类
 * 用于处理图片的持久化存储，特别是针对Android 12+设备
 */
object ImageStorageUtils {
    private const val TAG = "ImageStorageUtils"
    private const val IMAGES_DIR = "journal_images"
    private const val IMAGE_QUALITY = 90

    /**
     * 将URI指向的图片复制到应用私有存储空间，返回新的文件URI
     *
     * @param context 上下文
     * @param uri 原始图片URI
     * @return 复制后的图片文件URI，失败返回null
     */
    fun copyImageToPrivateStorage(context: Context, uri: Uri): Uri? {
        try {
            // 创建图片目录
            val imagesDir = File(context.filesDir, IMAGES_DIR).apply {
                if (!exists()) mkdirs()
            }

            // 生成唯一文件名
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)

            // 使用ContentResolver获取输入流
            context.contentResolver.openInputStream(uri)?.use { input ->
                // 将图片解码为Bitmap并压缩保存，减小存储空间占用
                val bitmap = BitmapFactory.decodeStream(input)
                FileOutputStream(destFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, output)
                }
                bitmap.recycle()
            } ?: run {
                Log.e(TAG, "无法打开图片输入流: $uri")
                return null
            }

            // 返回新文件的URI
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "复制图片到私有存储空间失败: ${e.message}", e)
            return null
        }
    }

    /**
     * 检查图片是否需要特殊处理
     * Android 12+的Photo Picker返回的URI需要特殊处理
     */
    fun needsSpecialHandling(uri: Uri): Boolean {
        // 针对Android 12+的Photo Picker URI
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                uri.toString().contains("com.android.providers.media.photopicker")
    }

    /**
     * 处理图片URI，确保可以持久访问
     * - 对于普通URI，尝试获取持久访问权限
     * - 对于Android 12+ Photo Picker URI，复制到私有存储
     *
     * @param context 上下文
     * @param uri 图片URI
     * @return 处理后的URI (可能是原始URI或新的私有存储URI)
     */
    fun ensureImageAccessible(context: Context, uri: Uri): Uri? {
        return when {
            // 对于Android 12+的Photo Picker URI，需要复制到私有存储
            needsSpecialHandling(uri) -> {
                Log.d(TAG, "检测到Android 12+ Photo Picker URI，复制到私有存储: $uri")
                copyImageToPrivateStorage(context, uri)
            }

            // 对于file://URI，无需处理
            uri.scheme == "file" -> {
                uri
            }

            // 对于content://URI，尝试获取持久权限
            uri.scheme == "content" -> {
                val hasPermission = ImageUriUtils.takePersistablePermission(context, uri)
                if (hasPermission) {
                    uri
                } else {
                    // 如果无法获取持久权限，也复制到私有存储
                    Log.d(TAG, "无法获取URI持久权限，复制到私有存储: $uri")
                    copyImageToPrivateStorage(context, uri)
                }
            }

            // 其他情况，返回null
            else -> {
                Log.e(TAG, "不支持的URI类型: $uri")
                null
            }
        }
    }

    /**
     * 清理图片私有存储空间中的孤立文件
     * 可在应用启动时或图片管理界面调用
     */
    fun cleanupOrphanedImages(context: Context, activeImageUris: List<Uri>) {
        try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) return

            // 获取所有存储的图片文件
            val storedFiles = imagesDir.listFiles() ?: return

            // 获取当前活跃的文件路径
            val activeFilePaths = activeImageUris
                .filter { it.scheme == "file" }
                .mapNotNull { it.path }
                .toSet()

            // 删除不在活跃列表中的文件
            var deletedCount = 0
            storedFiles.forEach { file ->
                if (!activeFilePaths.contains(file.absolutePath)) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            if (deletedCount > 0) {
                Log.d(TAG, "已清理 $deletedCount 个未使用的图片文件")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理孤立图片文件失败: ${e.message}", e)
        }
    }
} 