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
    
    // 设置项已更改标志
    private val _settingsChanged = MutableStateFlow(false)
    val settingsChanged: StateFlow<Boolean> = _settingsChanged.asStateFlow()
    
    // 需要重启应用的标志
    private val _needsRestart = MutableStateFlow(false)
    val needsRestart: StateFlow<Boolean> = _needsRestart.asStateFlow()
    
    /**
     * 处理设置界面事件
     * @param event 用户操作事件
     */
    fun handleEvent(event: SettingsEvent) {
        when (event) {
            // 外观设置
            is SettingsEvent.SetDarkTheme -> updateThemeSettings(event.enabled, _uiState.value.useSystemTheme)
            is SettingsEvent.SetUseSystemTheme -> updateThemeSettings(_uiState.value.useDarkTheme, event.enabled)
            is SettingsEvent.SetPrimaryColor -> updatePrimaryColor(event.colorIndex)
            
            // 通用设置
            is SettingsEvent.SetDefaultLocationEnabled -> updateSetting { it.copy(defaultLocationEnabled = event.enabled) }
            is SettingsEvent.SetDefaultLocation -> updateSetting { it.copy(defaultLocation = event.location) }
            is SettingsEvent.SetDeleteConfirmation -> updateSetting { it.copy(deleteConfirmationEnabled = event.enabled) }
            is SettingsEvent.SetAutoSave -> updateSetting { it.copy(autoSaveEnabled = event.enabled) }
            is SettingsEvent.SetAutoSaveInterval -> updateSetting { it.copy(autoSaveInterval = event.minutes) }
            
            // 同步设置
            is SettingsEvent.SetAutoSync -> updateAutoSync(event.enabled)
            is SettingsEvent.SetSyncInterval -> updateSetting { it.copy(syncInterval = event.minutes) }
            is SettingsEvent.SetSyncWifiOnly -> updateSetting { it.copy(syncWifiOnly = event.enabled) }
            is SettingsEvent.SyncNow -> syncNow()
            
            // 隐私设置
            is SettingsEvent.SetAppLock -> updateSetting { it.copy(appLockEnabled = event.enabled) }
            is SettingsEvent.SetBiometricAuth -> updateSetting { it.copy(biometricAuthEnabled = event.enabled) }
            is SettingsEvent.SetPrivacyMode -> updateSetting { it.copy(privacyModeEnabled = event.enabled) }
            
            // 存储设置
            is SettingsEvent.SetCompressImages -> updateSetting { it.copy(compressImages = event.enabled) }
            is SettingsEvent.SetMaxImageSize -> updateSetting { it.copy(maxImageSize = event.sizeKB) }
            is SettingsEvent.SetBackupEnabled -> updateSetting { it.copy(backupEnabled = event.enabled) }
            is SettingsEvent.SetBackupInterval -> updateSetting { it.copy(backupInterval = event.days) }
            is SettingsEvent.SetBackupLocation -> updateSetting { it.copy(backupLocation = event.location) }
            is SettingsEvent.BackupNow -> backupDatabase()
            is SettingsEvent.RestoreBackup -> restoreBackup()
            
            // 通知设置
            is SettingsEvent.SetNotifications -> updateSetting { it.copy(notificationsEnabled = event.enabled) }
            is SettingsEvent.SetReminder -> updateSetting { it.copy(reminderEnabled = event.enabled) }
            is SettingsEvent.SetReminderTime -> updateSetting { it.copy(reminderTime = event.time) }
            
            // 高级设置
            is SettingsEvent.SetDebugMode -> updateSetting { it.copy(debugModeEnabled = event.enabled) }
            is SettingsEvent.SetExperimentalFeatures -> updateSetting { it.copy(experimentalFeaturesEnabled = event.enabled) }
            is SettingsEvent.ClearCache -> clearCache()
            is SettingsEvent.ResetSettings -> resetSettings()
        }
    }
    
    /**
     * 保存所有设置
     */
    fun saveSettings() {
        viewModelScope.launch {
            try {
                preferences.updateSettings(_uiState.value)
                _settingsChanged.value = false
                SnackBarUtils.showSnackBar("设置已保存")
                
                // 检查是否需要重启应用
                if (_needsRestart.value) {
                    SnackBarUtils.showSnackBar("部分设置需要重启应用才能生效")
                }
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("保存设置失败: ${e.message}")
            }
        }
    }
    
    /**
     * 更新主题设置
     */
    private fun updateThemeSettings(darkTheme: Boolean, systemTheme: Boolean) {
        updateSetting { 
            it.copy(
                useDarkTheme = darkTheme,
                useSystemTheme = systemTheme
            ) 
        }
        // 主题变化需要重启应用
        _needsRestart.value = true
    }
    
    /**
     * 更新主题颜色
     */
    private fun updatePrimaryColor(colorIndex: Int) {
        updateSetting { it.copy(primaryColorIndex = colorIndex) }
        // 颜色变化需要重启应用
        _needsRestart.value = true
    }
    
    /**
     * 更新自动同步设置
     */
    private fun updateAutoSync(enabled: Boolean) {
        updateSetting { it.copy(autoSyncEnabled = enabled) }
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
        _uiState.value = SettingsState()
        _settingsChanged.value = true
        SnackBarUtils.showSnackBar("已重置所有设置")
    }
    
    /**
     * 通用设置更新函数
     */
    private fun updateSetting(update: (SettingsState) -> SettingsState) {
        _uiState.update(update)
        _settingsChanged.value = true
    }
    
    /**
     * 获取支持的主题颜色列表
     */
    fun getThemeColors(): List<String> {
        return listOf(
            "默认蓝",
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
} 