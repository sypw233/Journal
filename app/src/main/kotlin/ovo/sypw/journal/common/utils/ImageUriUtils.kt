package ovo.sypw.journal.common.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * 图片URI工具类
 * 处理图片URI的持久权限问题，特别是Android 12及更高版本
 */
object ImageUriUtils {
    private const val TAG = "ImageUriUtils"
    
    /**
     * 获取URI的持久访问权限
     * 
     * @param context 上下文
     * @param uri 需要获取权限的URI
     * @return 是否成功获取权限
     */
    fun takePersistablePermission(context: Context, uri: Uri): Boolean {
        return try {
            // 检查URI类型
            if (uri.scheme != "content") {
                Log.d(TAG, "URI不是content类型，无需获取持久权限: $uri")
                return false
            }
            
            // 检查是否已经有权限
            val hasExistingPermission = context.contentResolver.persistedUriPermissions
                .any { it.uri == uri && (it.isReadPermission || it.isWritePermission) }
                
            if (hasExistingPermission) {
                Log.d(TAG, "已有持久权限: $uri")
                return true
            }
            
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12及更高版本需要特殊处理
                // 记录URI详细信息用于调试
                Log.d(TAG, "Android 12+: 请求持久权限 - URI类型: ${uri.scheme}, 路径: ${uri.path}, 授权: ${uri.authority}")
                
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    Log.d(TAG, "Android 12+: 成功获取持久权限")
                    true
                } catch (e: SecurityException) {
                    // Android 12+可能会抛出SecurityException，尤其是使用GetContent获取的URI
                    Log.e(TAG, "Android 12+: 无法获取持久权限，请考虑使用OpenDocument代替GetContent: ${e.message}")
                    false
                }
            } else {
                // Android 11及更低版本
                context.contentResolver.takePersistableUriPermission(uri, flags)
                Log.d(TAG, "获取持久权限成功: $uri")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取持久权限异常: ${e.message}", e)
            false
        }
    }
    
    /**
     * 处理多个URI，获取持久权限
     * 
     * @param context 上下文
     * @param uris 需要处理的URI列表
     * @return 成功获取权限的URI列表
     */
    fun processManyUris(context: Context, uris: List<Uri>): List<Uri> {
        return uris.map { uri ->
            takePersistablePermission(context, uri)
            uri
        }
    }
    
    /**
     * 检查URI是否有持久权限
     */
    fun hasPermission(context: Context, uri: Uri): Boolean {
        if (uri.scheme != "content") return true // 非content URI不需要检查
        
        return context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && (it.isReadPermission || it.isWritePermission) }
    }
    
    /**
     * 清理不再需要的持久权限
     */
    fun releaseUnusedPermissions(context: Context, keepUris: List<Uri> = emptyList()) {
        context.contentResolver.persistedUriPermissions.forEach { permission ->
            if (keepUris.none { it == permission.uri }) {
                try {
                    // 释放不再需要的URI权限
                    val flags = if (permission.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0 or
                                if (permission.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0
                                
                    if (flags != 0) {
                        context.contentResolver.releasePersistableUriPermission(permission.uri, flags)
                        Log.d(TAG, "释放不再使用的URI权限: ${permission.uri}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "释放URI权限失败: ${e.message}", e)
                }
            }
        }
    }
} 