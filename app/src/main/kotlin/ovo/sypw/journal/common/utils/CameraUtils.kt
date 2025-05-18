package ovo.sypw.journal.common.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
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

/**
 * 相机工具类
 * 处理拍照相关的功能
 */
object CameraUtils {
    
    /**
     * 检查是否有相机权限
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 相机拍照组件
     * 处理权限请求和拍照逻辑
     * 
     * @param onPhotoTaken 拍照完成后的回调，传入照片的Uri
     */
    @Composable
    fun CameraCapture(
        onPhotoTaken: (Uri) -> Unit
    ): MutableState<Boolean> {
        val context = LocalContext.current
        val showCamera = remember { mutableStateOf(false) }
        val showPermissionDialog = remember { mutableStateOf(false) }
        
        // 当前拍照图片URI
        val photoUri = remember { mutableStateOf<Uri?>(null) }
        
        // 创建临时图片文件并获取URI
        fun createPhotoUri(): Uri {
            val photoFile = ImagePickerUtils.createImageFile(context)
            return ImagePickerUtils.getUriForFile(context, photoFile)
        }
        
        // 相机启动器
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && photoUri.value != null) {
                // 拍照成功，回调照片URI
                onPhotoTaken(photoUri.value!!)
            }
        }
        
        // 权限请求启动器
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // 权限已授予，启动相机
                photoUri.value = createPhotoUri()
                cameraLauncher.launch(photoUri.value)
            } else {
                // 显示权限说明对话框
                showPermissionDialog.value = true
            }
        }
        
        // 设置页面启动器
        val settingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            // 从设置页面返回后，检查权限状态
            if (hasCameraPermission(context)) {
                photoUri.value = createPhotoUri()
                cameraLauncher.launch(photoUri.value)
            }
        }
        
        // 显示权限对话框
        if (showPermissionDialog.value) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog.value = false },
                title = { Text("需要相机权限") },
                text = { Text("拍照功能需要相机权限。请在设置中授予权限。") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog.value = false
                            // 跳转到应用权限设置页面
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
        
        // 监听拍照状态
        if (showCamera.value) {
            // 重置状态
            showCamera.value = false
            
            // 检查权限
            if (!hasCameraPermission(context)) {
                // 请求相机权限
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                // 已有权限，直接启动相机
                photoUri.value = createPhotoUri()
                cameraLauncher.launch(photoUri.value)
            }
        }
        
        return showCamera
    }
    
    /**
     * 获取相机Intent
     * 用于在Activity中启动相机
     */
    fun getCameraIntent(context: Context, photoUri: Uri): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
    }
} 