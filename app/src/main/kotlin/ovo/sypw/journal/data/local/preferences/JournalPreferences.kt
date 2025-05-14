package ovo.sypw.journal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class JournalPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val keyFirstLaunch = "is_first_launch"
    private val keyAutoSyncEnabled = "auto_sync_enabled"
    private val keyLastSyncTime = "last_sync_time"

    /**
     * 检查是否是第一次启动
     */
    fun isFirstLaunch(): Boolean {
        val isFirstLaunch = sharedPreferences.getBoolean(keyFirstLaunch, true)
        if (isFirstLaunch) {
//            SnackBarUtils.showSnackBar("First Launch")
            sharedPreferences.edit { putBoolean(keyFirstLaunch, false) }
        } else {
//            SnackBarUtils.showSnackBar("Not First Launch")
        }
        return isFirstLaunch
    }
    
    /**
     * 获取自动同步是否启用
     * @return 是否启用自动同步
     */
    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyAutoSyncEnabled, false)
    }
    
    /**
     * 设置自动同步状态
     * @param enabled 是否启用自动同步
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyAutoSyncEnabled, enabled) }
    }
    
    /**
     * 获取上次同步时间
     * @return 上次同步的时间戳（毫秒）
     */
    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(keyLastSyncTime, 0L)
    }
    
    /**
     * 设置上次同步时间
     * @param timestamp 同步时间戳（毫秒）
     */
    fun setLastSyncTime(timestamp: Long) {
        sharedPreferences.edit { putLong(keyLastSyncTime, timestamp) }
    }
}