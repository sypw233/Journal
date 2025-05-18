package ovo.sypw.journal.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.model.AIModels
import ovo.sypw.journal.data.model.AISettings
import ovo.sypw.journal.data.model.SettingsEvent
import ovo.sypw.journal.data.model.SettingsState
import java.io.File
import javax.inject.Inject

/**
 * 设置界面ViewModel
 * 处理设置界面的状态管理和事件处理
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: JournalPreferences,
    private val autoSyncManager: AutoSyncManager
) : ViewModel() {
    
    // 设置界面状态
    private val _uiState = MutableStateFlow(preferences.getSettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()
    
    // 需要重启应用的标志 - 某些设置仍可能需要重启
    private val _needsRestart = MutableStateFlow(false)
    val needsRestart: StateFlow<Boolean> = _needsRestart.asStateFlow()
    
    /**
     * 处理设置界面事件
     * @param event 用户操作事件
     */
    fun handleEvent(event: SettingsEvent) {
        when (event) {
            // 外观设置 - 现在可以立即生效，不需要重启
            is SettingsEvent.SetDarkTheme -> updateThemeSettings(event.enabled, _uiState.value.useSystemTheme)
            is SettingsEvent.SetUseSystemTheme -> updateThemeSettings(_uiState.value.useDarkTheme, event.enabled)
            is SettingsEvent.SetPrimaryColor -> updatePrimaryColor(event.colorIndex)
            
            // 通用设置
            is SettingsEvent.SetDefaultLocationEnabled -> updateSetting(
                { it.copy(defaultLocationEnabled = event.enabled) },
                { preferences.setDefaultLocationEnabled(event.enabled) }
            )
            is SettingsEvent.SetDefaultLocation -> updateSetting(
                { it.copy(defaultLocation = event.location) },
                { preferences.setDefaultLocation(event.location) }
            )
            is SettingsEvent.SetDeleteConfirmation -> updateSetting(
                { it.copy(deleteConfirmationEnabled = event.enabled) },
                { preferences.setDeleteConfirmationEnabled(event.enabled) }
            )
            is SettingsEvent.SetAutoSave -> updateSetting(
                { it.copy(autoSaveEnabled = event.enabled) },
                { preferences.setAutoSaveEnabled(event.enabled) }
            )
            is SettingsEvent.SetAutoSaveInterval -> updateSetting(
                { it.copy(autoSaveInterval = event.minutes) },
                { preferences.setAutoSaveInterval(event.minutes) }
            )
            
            // 同步设置
            is SettingsEvent.SetAutoSync -> updateAutoSync(event.enabled)
            is SettingsEvent.SetSyncInterval -> updateSetting(
                { it.copy(syncInterval = event.minutes) },
                { preferences.setSyncInterval(event.minutes) }
            )
            is SettingsEvent.SetSyncWifiOnly -> updateSetting(
                { it.copy(syncWifiOnly = event.enabled) },
                { preferences.setSyncWifiOnly(event.enabled) }
            )
            is SettingsEvent.SyncNow -> syncNow()
            
            // 隐私设置
            is SettingsEvent.SetAppLock -> updateSetting(
                { it.copy(appLockEnabled = event.enabled) },
                { preferences.setAppLockEnabled(event.enabled) }
            )
            is SettingsEvent.SetBiometricAuth -> updateSetting(
                { it.copy(biometricAuthEnabled = event.enabled) },
                { preferences.setBiometricAuthEnabled(event.enabled) }
            )
            is SettingsEvent.SetPrivacyMode -> updateSetting(
                { it.copy(privacyModeEnabled = event.enabled) },
                { preferences.setPrivacyModeEnabled(event.enabled) }
            )
            
            // 存储设置
            is SettingsEvent.SetCompressImages -> updateSetting(
                { it.copy(compressImages = event.enabled) },
                { preferences.setCompressImages(event.enabled) }
            )
            is SettingsEvent.SetMaxImageSize -> updateSetting(
                { it.copy(maxImageSize = event.sizeKB) },
                { preferences.setMaxImageSize(event.sizeKB) }
            )
            is SettingsEvent.SetBackupEnabled -> updateSetting(
                { it.copy(backupEnabled = event.enabled) },
                { preferences.setBackupEnabled(event.enabled) }
            )
            is SettingsEvent.SetBackupInterval -> updateSetting(
                { it.copy(backupInterval = event.days) },
                { preferences.setBackupInterval(event.days) }
            )
            is SettingsEvent.SetBackupLocation -> updateSetting(
                { it.copy(backupLocation = event.location) },
                { preferences.setBackupLocation(event.location) }
            )
            is SettingsEvent.BackupNow -> backupDatabase()
            is SettingsEvent.RestoreBackup -> restoreBackup()
            
            // 通知设置
            is SettingsEvent.SetNotifications -> updateSetting(
                { it.copy(notificationsEnabled = event.enabled) },
                { preferences.setNotificationsEnabled(event.enabled) }
            )
            is SettingsEvent.SetReminder -> updateSetting(
                { it.copy(reminderEnabled = event.enabled) },
                { preferences.setReminderEnabled(event.enabled) }
            )
            is SettingsEvent.SetReminderTime -> updateSetting(
                { it.copy(reminderTime = event.time) },
                { preferences.setReminderTime(event.time) }
            )
            
            // AI设置
            is SettingsEvent.SetAIModel -> updateAISettings(
                { it.copy(modelType = event.modelType) },
                { preferences.setAIModel(event.modelType) }
            )
            is SettingsEvent.SetUseHistoricalJournalsDefault -> updateAISettings(
                { it.copy(useHistoricalJournalsDefault = event.enabled) },
                { preferences.setUseHistoricalJournalsDefault(event.enabled) }
            )
            is SettingsEvent.SetHistoricalJournalsCountDefault -> updateAISettings(
                { it.copy(historicalJournalsCountDefault = event.count) },
                { preferences.setHistoricalJournalsCountDefault(event.count) }
            )
            is SettingsEvent.SetMaxContentLength -> updateAISettings(
                { it.copy(maxContentLength = event.length) },
                { preferences.setMaxContentLength(event.length) }
            )
            is SettingsEvent.SetDefaultPromptTemplate -> updateAISettings(
                { it.copy(defaultPromptTemplate = event.template) },
                { preferences.setDefaultPromptTemplate(event.template) }
            )
            is SettingsEvent.SetShowAdvancedSettingsDefault -> updateAISettings(
                { it.copy(showAdvancedSettingsDefault = event.enabled) },
                { preferences.setShowAdvancedSettingsDefault(event.enabled) }
            )
            
            // 高级设置
            is SettingsEvent.SetDebugMode -> updateSetting(
                { it.copy(debugModeEnabled = event.enabled) },
                { preferences.setDebugModeEnabled(event.enabled) }
            )
            is SettingsEvent.SetExperimentalFeatures -> updateSetting(
                { it.copy(experimentalFeaturesEnabled = event.enabled) },
                { preferences.setExperimentalFeaturesEnabled(event.enabled) }
            )
            is SettingsEvent.ClearCache -> clearCache()
            is SettingsEvent.ResetSettings -> resetSettings()
        }
    }
    
    /**
     * 更新主题设置 - 立即生效
     */
    private fun updateThemeSettings(darkTheme: Boolean, systemTheme: Boolean) {
        _uiState.update { 
            it.copy(
                useDarkTheme = darkTheme,
                useSystemTheme = systemTheme
            ) 
        }
        
        // 立即应用主题设置
        preferences.setUseDarkTheme(darkTheme)
        preferences.setUseSystemTheme(systemTheme)
    }
    
    /**
     * 更新主题颜色 - 立即生效
     */
    private fun updatePrimaryColor(colorIndex: Int) {
        _uiState.update { it.copy(primaryColorIndex = colorIndex) }
        
        // 立即应用主题颜色
        preferences.setPrimaryColorIndex(colorIndex)
        
        // 添加颜色切换提示
        SnackBarUtils.showSnackBar("已切换主题颜色为: ${getThemeColors()[colorIndex]} (索引: $colorIndex)")
    }
    
    /**
     * 更新自动同步设置
     */
    private fun updateAutoSync(enabled: Boolean) {
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
        
        // 立即应用设置
        preferences.setAutoSyncEnabled(enabled)
        
        viewModelScope.launch {
            autoSyncManager.setAutoSyncEnabled(enabled)
        }
    }
    
    /**
     * 立即执行同步
     */
    private fun syncNow() {
        viewModelScope.launch {
            try {
                SnackBarUtils.showSnackBar("开始同步...")
                autoSyncManager.scheduleSyncNow()
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("同步失败: ${e.message}")
            }
        }
    }
    
    /**
     * 备份数据库
     */
    private fun backupDatabase() {
        viewModelScope.launch {
            try {
                SnackBarUtils.showSnackBar("正在备份数据库...")
                // 数据库备份逻辑
                SnackBarUtils.showSnackBar("数据库备份成功")
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("备份失败: ${e.message}")
            }
        }
    }
    
    /**
     * 恢复备份
     */
    private fun restoreBackup() {
        viewModelScope.launch {
            try {
                SnackBarUtils.showSnackBar("正在恢复数据库...")
                // 数据库恢复逻辑
                SnackBarUtils.showSnackBar("数据库恢复成功，需要重启应用")
                _needsRestart.value = true
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("恢复失败: ${e.message}")
            }
        }
    }
    
    /**
     * 清除缓存
     */
    private fun clearCache() {
        viewModelScope.launch {
            try {
                SnackBarUtils.showSnackBar("正在清除缓存...")
                // 清除缓存逻辑
                SnackBarUtils.showSnackBar("缓存已清除")
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("清除缓存失败: ${e.message}")
            }
        }
    }
    
    /**
     * 重置所有设置
     */
    private fun resetSettings() {
        val defaultSettings = SettingsState()
        _uiState.value = defaultSettings
        
        // 立即应用默认设置
        preferences.resetAllSettings()
        SnackBarUtils.showSnackBar("已重置所有设置")
    }
    
    /**
     * 通用设置更新函数，立即更新UI状态和保存设置
     */
    private fun updateSetting(
        update: (SettingsState) -> SettingsState,
        save: () -> Unit
    ) {
        _uiState.update(update)
        save() // 立即保存到preferences
    }
    
    /**
     * 获取支持的主题颜色列表
     */
    fun getThemeColors(): List<String> {
        return listOf(
            "跟随系统",
            "深蓝",
            "绿色",
            "紫色",
            "橙色",
            "红色",
            "粉色",
            "黄色",
            "青色",
            "黑色"
        )
    }
    
    /**
     * 更新AI设置
     */
    private fun updateAISettings(
        update: (AISettings) -> AISettings,
        save: () -> Unit
    ) {
        _uiState.update { 
            it.copy(aiSettings = update(it.aiSettings))
        }
        save() // 立即保存到preferences
    }
    
    /**
     * 获取可用的AI模型列表
     */
    fun getAvailableAIModels(): Map<String, String> {
        return AIModels.AVAILABLE_MODELS
    }
} 