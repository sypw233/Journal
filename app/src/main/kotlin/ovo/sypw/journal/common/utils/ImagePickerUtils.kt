package ovo.sypw.journal.common.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext



/**
 * 智能图片选择器工具类
 * 根据Android版本选择合适的图片选择器实现
 */
object ImagePickerUtils {
    private const val TAG = "ImagePickerUtils"
    /**
     * 创建适合当前系统版本的图片选择器
     * 
     * @param onImagesPicked 图片选择完成的回调
     * @param maxSelectionCount 最大选择数量 (仅对Android 13+有效)
     * @return 图片选择启动函数
     */
    @Composable
    fun rememberImagePicker(
        onImagesPicked: (List<Uri>) -> Unit,
        maxSelectionCount: Int = 10
    ): () -> Unit {
        val context = LocalContext.current
        
        // 检查系统版本
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): 使用系统PhotoPicker
//            Log.d(TAG, "使用系统PhotoPicker (Android ${Build.VERSION.SDK_INT})")
            createSystemPhotoPicker(context, onImagesPicked, maxSelectionCount)
        } else {
            // Android 11及以下: 使用DocumentsUI
//            Log.d(TAG, "使用DocumentsUI (Android ${Build.VERSION.SDK_INT})")
            createLegacyDocumentsPicker(context, onImagesPicked)
        }
    }
    
    /**
     * 创建系统PhotoPicker (Android 12+)
     */
    @Composable
    private fun createSystemPhotoPicker(
        context: Context,
        onImagesPicked: (List<Uri>) -> Unit,
        maxSelectionCount: Int
    ): () -> Unit {
        val multiplePhotoPickerLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+): 支持限制最大选择数量
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxSelectionCount)
            ) { uris ->
                if (uris.isNotEmpty()) {
                    processPickedUris(context, uris, onImagesPicked)
                }
            }
        } else {
            // Android 12 (API 31-32): 不支持限制最大选择数量
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    processPickedUris(context, uris, onImagesPicked)
                }
            }
        }

        return {
            multiplePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
    
    /**
     * 创建传统DocumentsUI选择器 (适用于所有Android版本)
     */
    @Composable
    private fun createLegacyDocumentsPicker(
        context: Context,
        onImagesPicked: (List<Uri>) -> Unit
    ): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNotEmpty()) {
                processPickedUris(context, uris, onImagesPicked)
            }
        }
        
        return {
            launcher.launch(arrayOf("image/*"))
        }
    }
    
    /**
     * 处理选择的URI列表
     */
    private fun processPickedUris(
        context: Context,
        uris: List<Uri>,
        onImagesPicked: (List<Uri>) -> Unit
    ) {
        val processedUris = uris.mapNotNull { uri ->
            try {
                // 对于DocumentsUI返回的URI，我们需要获取持久权限
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && 
                    uri.toString().startsWith("content://com.android.providers.media.documents")) {
                    ImageUriUtils.takePersistablePermission(context, uri)
                }
                
                // 对于PhotoPicker返回的URI，系统已经处理好权限问题
                uri
            } catch (e: Exception) {
                Log.e(TAG, "处理图片URI错误: ${e.message}")
                null
            }
        }
        
        if (processedUris.isNotEmpty()) {
            Log.d(TAG, "已选择${processedUris.size}张图片: $processedUris")
            onImagesPicked(processedUris)
        }
    }
} 