package ovo.sypw.journal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ovo.sypw.journal.data.model.SettingsState

/**
 * 日记应用偏好设置工具类
 * 负责存储和获取用户设置
 */
class JournalPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    // 基本设置键
    private val keyFirstLaunch = "is_first_launch"
    private val keyAutoSyncEnabled = "auto_sync_enabled"
    private val keyLastSyncTime = "last_sync_time"
    
    // 外观设置键
    private val keyUseDarkTheme = "use_dark_theme"
    private val keyUseSystemTheme = "use_system_theme"
    private val keyPrimaryColorIndex = "primary_color_index"
    
    // 通用设置键
    private val keyDefaultLocationEnabled = "default_location_enabled"
    private val keyDefaultLocation = "default_location"
    private val keyDeleteConfirmationEnabled = "delete_confirmation_enabled"
    private val keyAutoSaveEnabled = "auto_save_enabled"
    private val keyAutoSaveInterval = "auto_save_interval"
    
    // 同步设置键
    private val keySyncInterval = "sync_interval"
    private val keySyncWifiOnly = "sync_wifi_only"
    
    // 隐私设置键
    private val keyAppLockEnabled = "app_lock_enabled"
    private val keyBiometricAuthEnabled = "biometric_auth_enabled"
    private val keyPrivacyModeEnabled = "privacy_mode_enabled"
    
    // 存储设置键
    private val keyCompressImages = "compress_images"
    private val keyMaxImageSize = "max_image_size"
    private val keyBackupEnabled = "backup_enabled"
    private val keyBackupInterval = "backup_interval"
    private val keyBackupLocation = "backup_location"
    
    // 通知设置键
    private val keyNotificationsEnabled = "notifications_enabled"
    private val keyReminderEnabled = "reminder_enabled"
    private val keyReminderTime = "reminder_time"
    
    // 高级设置键
    private val keyDebugModeEnabled = "debug_mode_enabled"
    private val keyExperimentalFeaturesEnabled = "experimental_features_enabled"

    /**
     * 获取完整的设置状态
     * @return 当前的所有设置项
     */
    fun getSettingsState(): SettingsState {
        return SettingsState(
            // 外观设置
            useDarkTheme = sharedPreferences.getBoolean(keyUseDarkTheme, false),
            useSystemTheme = sharedPreferences.getBoolean(keyUseSystemTheme, true),
            primaryColorIndex = sharedPreferences.getInt(keyPrimaryColorIndex, 0),
            
            // 通用设置
            defaultLocationEnabled = sharedPreferences.getBoolean(keyDefaultLocationEnabled, false),
            defaultLocation = sharedPreferences.getString(keyDefaultLocation, "") ?: "",
            deleteConfirmationEnabled = sharedPreferences.getBoolean(keyDeleteConfirmationEnabled, true),
            autoSaveEnabled = sharedPreferences.getBoolean(keyAutoSaveEnabled, true),
            autoSaveInterval = sharedPreferences.getInt(keyAutoSaveInterval, 5),
            
            // 同步设置
            autoSyncEnabled = sharedPreferences.getBoolean(keyAutoSyncEnabled, false),
            syncInterval = sharedPreferences.getInt(keySyncInterval, 30),
            syncWifiOnly = sharedPreferences.getBoolean(keySyncWifiOnly, true),
            lastSyncTime = sharedPreferences.getLong(keyLastSyncTime, 0L),
            
            // 隐私设置
            appLockEnabled = sharedPreferences.getBoolean(keyAppLockEnabled, false),
            biometricAuthEnabled = sharedPreferences.getBoolean(keyBiometricAuthEnabled, false),
            privacyModeEnabled = sharedPreferences.getBoolean(keyPrivacyModeEnabled, false),
            
            // 存储设置
            compressImages = sharedPreferences.getBoolean(keyCompressImages, true),
            maxImageSize = sharedPreferences.getInt(keyMaxImageSize, 1024),
            backupEnabled = sharedPreferences.getBoolean(keyBackupEnabled, false),
            backupInterval = sharedPreferences.getInt(keyBackupInterval, 7),
            backupLocation = sharedPreferences.getString(keyBackupLocation, "内部存储/Journal/备份") ?: "内部存储/Journal/备份",
            
            // 通知设置
            notificationsEnabled = sharedPreferences.getBoolean(keyNotificationsEnabled, true),
            reminderEnabled = sharedPreferences.getBoolean(keyReminderEnabled, false),
            reminderTime = sharedPreferences.getString(keyReminderTime, "21:00") ?: "21:00",
            
            // 高级设置
            debugModeEnabled = sharedPreferences.getBoolean(keyDebugModeEnabled, false),
            experimentalFeaturesEnabled = sharedPreferences.getBoolean(keyExperimentalFeaturesEnabled, false)
        )
    }
    
    /**
     * 更新设置状态
     * @param settingsState 要保存的设置状态
     */
    fun updateSettings(settingsState: SettingsState) {
        sharedPreferences.edit {
            // 外观设置
            putBoolean(keyUseDarkTheme, settingsState.useDarkTheme)
            putBoolean(keyUseSystemTheme, settingsState.useSystemTheme)
            putInt(keyPrimaryColorIndex, settingsState.primaryColorIndex)
            
            // 通用设置
            putBoolean(keyDefaultLocationEnabled, settingsState.defaultLocationEnabled)
            putString(keyDefaultLocation, settingsState.defaultLocation)
            putBoolean(keyDeleteConfirmationEnabled, settingsState.deleteConfirmationEnabled)
            putBoolean(keyAutoSaveEnabled, settingsState.autoSaveEnabled)
            putInt(keyAutoSaveInterval, settingsState.autoSaveInterval)
            
            // 同步设置
            putBoolean(keyAutoSyncEnabled, settingsState.autoSyncEnabled)
            putInt(keySyncInterval, settingsState.syncInterval)
            putBoolean(keySyncWifiOnly, settingsState.syncWifiOnly)
            putLong(keyLastSyncTime, settingsState.lastSyncTime)
            
            // 隐私设置
            putBoolean(keyAppLockEnabled, settingsState.appLockEnabled)
            putBoolean(keyBiometricAuthEnabled, settingsState.biometricAuthEnabled)
            putBoolean(keyPrivacyModeEnabled, settingsState.privacyModeEnabled)
            
            // 存储设置
            putBoolean(keyCompressImages, settingsState.compressImages)
            putInt(keyMaxImageSize, settingsState.maxImageSize)
            putBoolean(keyBackupEnabled, settingsState.backupEnabled)
            putInt(keyBackupInterval, settingsState.backupInterval)
            putString(keyBackupLocation, settingsState.backupLocation)
            
            // 通知设置
            putBoolean(keyNotificationsEnabled, settingsState.notificationsEnabled)
            putBoolean(keyReminderEnabled, settingsState.reminderEnabled)
            putString(keyReminderTime, settingsState.reminderTime)
            
            // 高级设置
            putBoolean(keyDebugModeEnabled, settingsState.debugModeEnabled)
            putBoolean(keyExperimentalFeaturesEnabled, settingsState.experimentalFeaturesEnabled)
        }
    }

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
    
    // 外观设置方法
    fun setDarkTheme(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyUseDarkTheme, enabled) }
    }
    
    fun isDarkTheme(): Boolean {
        return sharedPreferences.getBoolean(keyUseDarkTheme, false)
    }
    
    fun setUseSystemTheme(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyUseSystemTheme, enabled) }
    }
    
    fun isUseSystemTheme(): Boolean {
        return sharedPreferences.getBoolean(keyUseSystemTheme, true)
    }
    
    fun setPrimaryColorIndex(index: Int) {
        sharedPreferences.edit { putInt(keyPrimaryColorIndex, index) }
    }
    
    fun getPrimaryColorIndex(): Int {
        return sharedPreferences.getInt(keyPrimaryColorIndex, 0)
    }
    
    // 隐私设置方法
    fun setAppLockEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyAppLockEnabled, enabled) }
    }
    
    fun isAppLockEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyAppLockEnabled, false)
    }
    
    fun setBiometricAuthEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyBiometricAuthEnabled, enabled) }
    }
    
    fun isBiometricAuthEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyBiometricAuthEnabled, false)
    }
    
    // 重置所有设置
    fun resetAllSettings() {
        updateSettings(SettingsState())
    }
}