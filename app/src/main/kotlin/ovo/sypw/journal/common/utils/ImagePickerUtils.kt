package ovo.sypw.journal.common.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图片选择工具类
 * 处理不同Android版本的图片选择逻辑
 */
object ImagePickerUtils {
    private const val TAG = "ImagePickerUtils"
    
    /**
     * 判断是否需要存储权限
     * Android 10(Q)以上不需要存储权限即可访问媒体文件
     */
    fun needsStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }
    
    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (needsStoragePermission()) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * 图片选择器组件
     * 处理不同Android版本的图片选择逻辑
     * 
     * @param onImagesSelected 选择图片后的回调
     * @param maxImages 最大可选图片数量
     */
    @Composable
    fun ImagePicker(
        onImagesSelected: (List<Uri>) -> Unit,
        maxImages: Int = 10
    ): MutableState<Boolean> {
        val context = LocalContext.current
        val showPicker = remember { mutableStateOf(false) }
        val showPermissionDialog = remember { mutableStateOf(false) }
        
        // Android 13 及以上使用 Photo Picker API
        val modernImagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
        ) { uris ->
            if (uris.isNotEmpty()) {
                onImagesSelected(uris)
            }
        }
        
        // Android 12 及以下使用传统图片选择器
        val legacyImagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            if (uris.isNotEmpty()) {
                onImagesSelected(uris)
            }
        }
        
        // 存储权限请求
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                launchAppropriateImagePicker(legacyImagePicker)
            } else {
                showPermissionDialog.value = true
            }
        }
        
        // 权限设置页面启动器
        val settingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            if (hasStoragePermission(context)) {
                launchAppropriateImagePicker(legacyImagePicker)
            }
        }
        
        // 显示权限对话框
        if (showPermissionDialog.value) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog.value = false },
                title = { Text("需要存储权限") },
                text = { Text("为了选择图片，应用需要访问您的存储。请在设置中授予权限。") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog.value = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            settingsLauncher.launch(intent)
                        }
                    ) {
                        Text("前往设置")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPermissionDialog.value = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 监听显示选择器状态
        if (showPicker.value) {
            // 重置状态
            showPicker.value = false
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用 Photo Picker API
                modernImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                // Android 12 及以下
                if (needsStoragePermission() && !hasStoragePermission(context)) {
                    // 需要请求权限
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    // 已有权限，启动选择器
                    launchAppropriateImagePicker(legacyImagePicker)
                }
            }
        }
        
        return showPicker
    }
    
    /**
     * 启动适合当前版本的图片选择器
     */
    private fun launchAppropriateImagePicker(legacyPicker: androidx.activity.result.ActivityResultLauncher<String>) {
        legacyPicker.launch("image/*")
    }
    
    /**
     * 创建用于拍照的临时文件
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    /**
     * 获取文件的内容URI
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * 获取真实文件路径
     * 处理不同Android版本的Uri解析
     */
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                
                if ("primary".equals(type, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }
            } 
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    id.toLong()
                )
                return getDataColumn(context, contentUri, null, null)
            } 
            // MediaProvider
            else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                
                val contentUri = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }
                
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } 
        // MediaStore (普通Gallery)
        else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } 
        // File
        else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        
        return null
    }
    
    /**
     * 获取数据列
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        uri ?: return null
        
        val column = "_data"
        val projection = arrayOf(column)
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取文件路径失败", e)
        } finally {
            cursor?.close()
        }
        
        return null
    }
    
    /**
     * 判断是否是外部存储文档
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    /**
     * 判断是否是下载文档
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    
    /**
     * 判断是否是媒体文档
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
} 