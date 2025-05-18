package ovo.sypw.journal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ovo.sypw.journal.ThemeSettings
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
     * 观察主题设置的变化
     * 返回一个Flow，当设置改变时会发出新的ThemeSettings对象
     */
    fun observeThemeChanges(): Flow<ThemeSettings> = callbackFlow {
        // 初始发送当前的主题设置
        trySend(getCurrentThemeSettings())
        
        // 创建监听器
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                keyUseDarkTheme, keyUseSystemTheme, keyPrimaryColorIndex -> {
                    trySend(getCurrentThemeSettings())
                }
            }
        }
        
        // 注册监听器
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        
        // 当Flow被取消收集时，注销监听器
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    /**
     * 获取当前主题设置
     */
    private fun getCurrentThemeSettings(): ThemeSettings {
        return ThemeSettings(
            useDarkTheme = isUseDarkTheme(),
            useSystemTheme = isUseSystemTheme(),
            primaryColorIndex = getPrimaryColorIndex()
        )
    }
    
    // 应用设置
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(keyFirstLaunch, true)
    }
    
    fun setFirstLaunch(isFirst: Boolean) {
        sharedPreferences.edit { putBoolean(keyFirstLaunch, isFirst) }
    }
    
    // 自动同步设置
    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyAutoSyncEnabled, enabled) }
    }
    
    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyAutoSyncEnabled, false)
    }
    
    fun setLastSyncTime(time: Long) {
        sharedPreferences.edit { putLong(keyLastSyncTime, time) }
    }
    
    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(keyLastSyncTime, 0L)
    }
    
    // 外观设置方法
    fun setUseDarkTheme(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyUseDarkTheme, enabled) }
    }
    
    fun isUseDarkTheme(): Boolean {
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
    
    // 通用设置方法
    fun setDefaultLocationEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyDefaultLocationEnabled, enabled) }
    }
    
    fun isDefaultLocationEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyDefaultLocationEnabled, false)
    }
    
    fun setDefaultLocation(location: String) {
        sharedPreferences.edit { putString(keyDefaultLocation, location) }
    }
    
    fun getDefaultLocation(): String {
        return sharedPreferences.getString(keyDefaultLocation, "") ?: ""
    }
    
    fun setDeleteConfirmationEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyDeleteConfirmationEnabled, enabled) }
    }
    
    fun isDeleteConfirmationEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyDeleteConfirmationEnabled, true)
    }
    
    fun setAutoSaveEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyAutoSaveEnabled, enabled) }
    }
    
    fun isAutoSaveEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyAutoSaveEnabled, true)
    }
    
    fun setAutoSaveInterval(minutes: Int) {
        sharedPreferences.edit { putInt(keyAutoSaveInterval, minutes) }
    }
    
    fun getAutoSaveInterval(): Int {
        return sharedPreferences.getInt(keyAutoSaveInterval, 5)
    }
    
    // 同步设置方法
    fun setSyncInterval(minutes: Int) {
        sharedPreferences.edit { putInt(keySyncInterval, minutes) }
    }
    
    fun getSyncInterval(): Int {
        return sharedPreferences.getInt(keySyncInterval, 30)
    }
    
    fun setSyncWifiOnly(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keySyncWifiOnly, enabled) }
    }
    
    fun isSyncWifiOnly(): Boolean {
        return sharedPreferences.getBoolean(keySyncWifiOnly, true)
    }
    
    // 存储设置方法
    fun setCompressImages(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyCompressImages, enabled) }
    }
    
    fun isCompressImages(): Boolean {
        return sharedPreferences.getBoolean(keyCompressImages, true)
    }
    
    fun setMaxImageSize(sizeKB: Int) {
        sharedPreferences.edit { putInt(keyMaxImageSize, sizeKB) }
    }
    
    fun getMaxImageSize(): Int {
        return sharedPreferences.getInt(keyMaxImageSize, 1024)
    }
    
    fun setBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyBackupEnabled, enabled) }
    }
    
    fun isBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyBackupEnabled, false)
    }
    
    fun setBackupInterval(days: Int) {
        sharedPreferences.edit { putInt(keyBackupInterval, days) }
    }
    
    fun getBackupInterval(): Int {
        return sharedPreferences.getInt(keyBackupInterval, 7)
    }
    
    fun setBackupLocation(location: String) {
        sharedPreferences.edit { putString(keyBackupLocation, location) }
    }
    
    fun getBackupLocation(): String {
        return sharedPreferences.getString(keyBackupLocation, "内部存储/Journal/备份") ?: "内部存储/Journal/备份"
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
    
    fun setPrivacyModeEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyPrivacyModeEnabled, enabled) }
    }
    
    fun isPrivacyModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyPrivacyModeEnabled, false)
    }
    
    // 通知设置方法
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyNotificationsEnabled, enabled) }
    }
    
    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyNotificationsEnabled, true)
    }
    
    fun setReminderEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyReminderEnabled, enabled) }
    }
    
    fun isReminderEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyReminderEnabled, false)
    }
    
    fun setReminderTime(time: String) {
        sharedPreferences.edit { putString(keyReminderTime, time) }
    }
    
    fun getReminderTime(): String {
        return sharedPreferences.getString(keyReminderTime, "21:00") ?: "21:00"
    }
    
    // 高级设置方法
    fun setDebugModeEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyDebugModeEnabled, enabled) }
    }
    
    fun isDebugModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyDebugModeEnabled, false)
    }
    
    fun setExperimentalFeaturesEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(keyExperimentalFeaturesEnabled, enabled) }
    }
    
    fun isExperimentalFeaturesEnabled(): Boolean {
        return sharedPreferences.getBoolean(keyExperimentalFeaturesEnabled, false)
    }
    
    /**
     * 获取设置状态
     * 返回包含所有设置值的SettingsState对象
     */
    fun getSettingsState(): SettingsState {
        return SettingsState(
            // 外观设置
            useDarkTheme = isUseDarkTheme(),
            useSystemTheme = isUseSystemTheme(),
            primaryColorIndex = getPrimaryColorIndex(),
            
            // 通用设置
            defaultLocationEnabled = isDefaultLocationEnabled(),
            defaultLocation = getDefaultLocation(),
            deleteConfirmationEnabled = isDeleteConfirmationEnabled(),
            autoSaveEnabled = isAutoSaveEnabled(),
            autoSaveInterval = getAutoSaveInterval(),
            
            // 同步设置
            autoSyncEnabled = isAutoSyncEnabled(),
            syncInterval = getSyncInterval(),
            syncWifiOnly = isSyncWifiOnly(),
            lastSyncTime = getLastSyncTime(),
            
            // 隐私设置
            appLockEnabled = isAppLockEnabled(),
            biometricAuthEnabled = isBiometricAuthEnabled(),
            privacyModeEnabled = isPrivacyModeEnabled(),
            
            // 存储设置
            compressImages = isCompressImages(),
            maxImageSize = getMaxImageSize(),
            backupEnabled = isBackupEnabled(),
            backupInterval = getBackupInterval(),
            backupLocation = getBackupLocation(),
            
            // 通知设置
            notificationsEnabled = isNotificationsEnabled(),
            reminderEnabled = isReminderEnabled(),
            reminderTime = getReminderTime(),
            
            // 高级设置
            debugModeEnabled = isDebugModeEnabled(),
            experimentalFeaturesEnabled = isExperimentalFeaturesEnabled()
        )
    }
    
    /**
     * 更新设置
     * 将SettingsState对象中的所有设置值保存到SharedPreferences
     */
    fun updateSettings(settings: SettingsState) {
        // 外观设置
        setUseDarkTheme(settings.useDarkTheme)
        setUseSystemTheme(settings.useSystemTheme)
        setPrimaryColorIndex(settings.primaryColorIndex)
        
        // 通用设置
        setDefaultLocationEnabled(settings.defaultLocationEnabled)
        setDefaultLocation(settings.defaultLocation)
        setDeleteConfirmationEnabled(settings.deleteConfirmationEnabled)
        setAutoSaveEnabled(settings.autoSaveEnabled)
        setAutoSaveInterval(settings.autoSaveInterval)
        
        // 同步设置
        setAutoSyncEnabled(settings.autoSyncEnabled)
        setSyncInterval(settings.syncInterval)
        setSyncWifiOnly(settings.syncWifiOnly)
        
        // 隐私设置
        setAppLockEnabled(settings.appLockEnabled)
        setBiometricAuthEnabled(settings.biometricAuthEnabled)
        setPrivacyModeEnabled(settings.privacyModeEnabled)
        
        // 存储设置
        setCompressImages(settings.compressImages)
        setMaxImageSize(settings.maxImageSize)
        setBackupEnabled(settings.backupEnabled)
        setBackupInterval(settings.backupInterval)
        setBackupLocation(settings.backupLocation)
        
        // 通知设置
        setNotificationsEnabled(settings.notificationsEnabled)
        setReminderEnabled(settings.reminderEnabled)
        setReminderTime(settings.reminderTime)
        
        // 高级设置
        setDebugModeEnabled(settings.debugModeEnabled)
        setExperimentalFeaturesEnabled(settings.experimentalFeaturesEnabled)
    }
    
    // 重置所有设置
    fun resetAllSettings() {
        updateSettings(SettingsState())
    }
} 