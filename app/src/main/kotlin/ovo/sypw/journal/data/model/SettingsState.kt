package ovo.sypw.journal.data.model

/**
 * 设置界面状态数据类
 * 包含所有用户可配置的设置项
 */
data class SettingsState(
    // 外观设置
    val useDarkTheme: Boolean = false,
    val useSystemTheme: Boolean = true,
    val primaryColorIndex: Int = 0,

    // 通用设置
    val defaultLocationEnabled: Boolean = false,
    val defaultLocation: String = "",
    val deleteConfirmationEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val autoSaveInterval: Int = 5, // 分钟

    // 同步设置
    val autoSyncEnabled: Boolean = false,
    val syncInterval: Int = 30, // 分钟
    val syncWifiOnly: Boolean = true,
    val lastSyncTime: Long = 0L,

    // 隐私设置
    val appLockEnabled: Boolean = false,
    val biometricAuthEnabled: Boolean = false,
    val privacyModeEnabled: Boolean = false,

    // 存储设置
    val compressImages: Boolean = true,
    val maxImageSize: Int = 1024, // KB
    val backupEnabled: Boolean = false,
    val backupInterval: Int = 7, // 天
    val backupLocation: String = "内部存储/Journal/备份",

    // 通知设置
    val notificationsEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "21:00",

    // AI设置
    val aiSettings: AISettings = AISettings(),

    // 高级设置
    val debugModeEnabled: Boolean = false,
    val experimentalFeaturesEnabled: Boolean = false
)

/**
 * 设置界面事件
 * 用于处理设置界面的用户交互
 */
sealed class SettingsEvent {
    // 外观设置
    data class SetDarkTheme(val enabled: Boolean) : SettingsEvent()
    data class SetUseSystemTheme(val enabled: Boolean) : SettingsEvent()
    data class SetPrimaryColor(val colorIndex: Int) : SettingsEvent()

    // 通用设置
    data class SetDefaultLocationEnabled(val enabled: Boolean) : SettingsEvent()
    data class SetDefaultLocation(val location: String) : SettingsEvent()
    data class SetDeleteConfirmation(val enabled: Boolean) : SettingsEvent()
    data class SetAutoSave(val enabled: Boolean) : SettingsEvent()
    data class SetAutoSaveInterval(val minutes: Int) : SettingsEvent()

    // 同步设置
    data class SetAutoSync(val enabled: Boolean) : SettingsEvent()
    data class SetSyncInterval(val minutes: Int) : SettingsEvent()
    data class SetSyncWifiOnly(val enabled: Boolean) : SettingsEvent()
    object SyncNow : SettingsEvent()

    // 隐私设置
    data class SetAppLock(val enabled: Boolean) : SettingsEvent()
    data class SetBiometricAuth(val enabled: Boolean) : SettingsEvent()
    data class SetPrivacyMode(val enabled: Boolean) : SettingsEvent()

    // 存储设置
    data class SetCompressImages(val enabled: Boolean) : SettingsEvent()
    data class SetMaxImageSize(val sizeKB: Int) : SettingsEvent()
    data class SetBackupEnabled(val enabled: Boolean) : SettingsEvent()
    data class SetBackupInterval(val days: Int) : SettingsEvent()
    data class SetBackupLocation(val location: String) : SettingsEvent()
    object BackupNow : SettingsEvent()
    object RestoreBackup : SettingsEvent()

    // 通知设置
    data class SetNotifications(val enabled: Boolean) : SettingsEvent()
    data class SetReminder(val enabled: Boolean) : SettingsEvent()
    data class SetReminderTime(val time: String) : SettingsEvent()

    // AI设置
    data class SetAIModel(val modelType: String) : SettingsEvent()
    data class SetUseHistoricalJournalsDefault(val enabled: Boolean) : SettingsEvent()
    data class SetHistoricalJournalsCountDefault(val count: Int) : SettingsEvent()
    data class SetMaxContentLength(val length: Int) : SettingsEvent()
    data class SetDefaultPromptTemplate(val template: String) : SettingsEvent()
    data class SetShowAdvancedSettingsDefault(val enabled: Boolean) : SettingsEvent()

    // 高级设置
    data class SetDebugMode(val enabled: Boolean) : SettingsEvent()
    data class SetExperimentalFeatures(val enabled: Boolean) : SettingsEvent()
    object ClearCache : SettingsEvent()
    object ResetSettings : SettingsEvent()
} 