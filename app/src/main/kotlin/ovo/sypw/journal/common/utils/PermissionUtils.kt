package ovo.sypw.journal.common.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限工具类，用于处理Android动态权限申请
 */
class PermissionUtils {
    companion object {
        // 定位所需权限
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        /**
         * 检查是否已授予指定权限
         * @param context 上下文
         * @param permissions 需要检查的权限数组
         * @return 如果所有权限都已授予则返回true，否则返回false
         */
        fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        /**
         * 检查是否需要显示权限请求说明
         * @param activity Activity实例
         * @param permission 需要检查的权限
         * @return 如果需要显示权限请求说明则返回true，否则返回false
         */
        fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
}

/**
 * Composable函数，用于在Compose中请求权限
 * @param permissions 需要请求的权限数组
 * @param onPermissionResult 权限请求结果回调
 */
@Composable
fun RequestPermissions(
    permissions: Array<String>,
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.all { it.value }
        onPermissionResult(allGranted)
    }


    DisposableEffect(permissions) {
        // 检查是否已有权限
        if (!PermissionUtils.hasPermissions(context, permissions)) {
            // 请求权限
            permissionLauncher.launch(permissions)
        } else {
            // 已有权限，直接回调
            onPermissionResult(true)
        }

        onDispose {}
    }
}