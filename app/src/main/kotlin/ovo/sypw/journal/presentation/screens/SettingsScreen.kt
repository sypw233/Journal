package ovo.sypw.journal.presentation.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AIModels
import ovo.sypw.journal.data.model.SettingsEvent
import ovo.sypw.journal.presentation.components.SettingsItem
import ovo.sypw.journal.presentation.viewmodels.SettingsViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 设置界面
 * 显示并管理用户设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE)
    
    val uiState by viewModel.uiState.collectAsState()
    val needsRestart by viewModel.needsRestart.collectAsState()
    
    // 重启提示对话框
    var showRestartDialog by remember { mutableStateOf(false) }
    
    // 主题颜色选择对话框
    var showColorPickerDialog by remember { mutableStateOf(false) }

    // AI模型选择对话框
    var showModelPickerDialog by remember { mutableStateOf(false) }

    var colorEgg by remember { mutableIntStateOf(0) }

    // 从SharedPreferences获取API密钥
    var apiKey by remember { 
        mutableStateOf(sharedPrefs.getString("sentiment_api_key", "") ?: "") 
    }
    var passwordVisible by remember { mutableStateOf(false) }
    
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("需要重启应用") },
            text = { Text("部分设置需要重启应用才能生效，现在重启应用吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        // 重启应用逻辑
                    }
                ) {
                    Text("重启")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        onBackClick()
                    }
                ) {
                    Text("稍后")
                }
            }
        )
    }
    
    // 主题颜色选择对话框
    if (showColorPickerDialog) {
        ColorPickerDialog(
            selectedIndex = uiState.primaryColorIndex,
            colors = viewModel.getThemeColors(),
            onColorSelected = { index ->
                viewModel.handleEvent(SettingsEvent.SetPrimaryColor(index))
            },
            onDismiss = { showColorPickerDialog = false }
        )
    }
    
    // AI模型选择对话框
    if (showModelPickerDialog) {
        AIModelPickerDialog(
            selectedModel = uiState.aiSettings.modelType,
            onModelSelected = { modelType ->
                viewModel.handleEvent(SettingsEvent.SetAIModel(modelType))
                showModelPickerDialog = false
            },
            onDismiss = { showModelPickerDialog = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // 常规设置
            Text(
                text = "常规设置",
                style = MaterialTheme.typography.titleMedium
            )
            
            // 外观设置
            SettingsCategory(
                title = "外观设置",
                icon = Icons.Default.ColorLens
            ) {
                // 主题预览卡片
                ThemePreviewCard(
                    isDarkMode = if (uiState.useSystemTheme) isSystemInDarkTheme() else uiState.useDarkTheme,
                    primaryColor = getColorForIndex(uiState.primaryColorIndex)
                )
                
                // 主题模式设置
                SettingItem(
                    title = "深色模式",
                    description = "启用应用的深色主题",
                    icon = Icons.Default.BrightnessHigh
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.useDarkTheme) "开启" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = uiState.useDarkTheme,
                            onCheckedChange = { 
                                viewModel.handleEvent(SettingsEvent.SetDarkTheme(it)) 
                            },
                            enabled = !uiState.useSystemTheme
                        )
                    }
                }
                
                // 跟随系统主题
                SettingItem(
                    title = "跟随系统主题",
                    description = "根据系统设置自动切换明暗主题",
                    icon = Icons.Default.BrightnessAuto
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.useSystemTheme) "开启" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = uiState.useSystemTheme,
                            onCheckedChange = { 
                                viewModel.handleEvent(SettingsEvent.SetUseSystemTheme(it)) 
                            }
                        )
                    }
                }
                
                // 主题颜色
                SettingItem(
                    title = "主题颜色",
                    description = "选择应用的主色调",
                    icon = Icons.Default.ColorLens,
                    onClick = {
                        // 打开颜色选择对话框
                        showColorPickerDialog = true
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 显示当前选择的颜色预览
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    getColorForIndex(uiState.primaryColorIndex),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = viewModel.getThemeColors()[uiState.primaryColorIndex],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // 通用设置
            SettingsCategory(
                title = "通用设置",
                icon = Icons.Default.Settings
            ) {
                // 自动保存
                SettingItem(
                    title = "自动保存",
                    description = "编辑日记时自动保存草稿",
                    icon = Icons.Default.Save
                ) {
                    Switch(
                        checked = uiState.autoSaveEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetAutoSave(it)) 
                        }
                    )
                }
                
                // 自动保存间隔
                SettingItem(
                    title = "自动保存间隔",
                    description = "每${uiState.autoSaveInterval}分钟自动保存一次",
                    icon = Icons.Default.Refresh,
                    enabled = uiState.autoSaveEnabled
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val options = listOf(1, 5, 10, 15, 30)
                        val selectedOption = if (options.contains(uiState.autoSaveInterval)) {
                            uiState.autoSaveInterval
                        } else {
                            5 // 默认值
                        }
                        Text(
                            text = "$selectedOption 分钟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.autoSaveEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val currentIndex = options.indexOf(selectedOption)
                                val nextIndex = (currentIndex + 1) % options.size
                                viewModel.handleEvent(SettingsEvent.SetAutoSaveInterval(options[nextIndex]))
                            },
                            enabled = uiState.autoSaveEnabled
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "更改时间",
                                tint = if (uiState.autoSaveEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // 删除确认
                SettingItem(
                    title = "删除确认",
                    description = "删除日记前显示确认对话框",
                    icon = Icons.Default.Info
                ) {
                    Switch(
                        checked = uiState.deleteConfirmationEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetDeleteConfirmation(it)) 
                        }
                    )
                }
            }
            
            // 同步设置
            SettingsCategory(
                title = "同步设置",
                icon = Icons.Default.Sync
            ) {
                // 自动同步
                SettingItem(
                    title = "自动同步",
                    description = "启用自动同步功能",
                    icon = Icons.Default.Sync
                ) {
                    Switch(
                        checked = uiState.autoSyncEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetAutoSync(it)) 
                        }
                    )
                }
                
                // 仅WiFi同步
                SettingItem(
                    title = "仅WiFi下同步(待完成)",
                    description = "只在WiFi连接时执行同步",
                    icon = Icons.Default.Refresh,
                    enabled = uiState.autoSyncEnabled
                ) {
                    Switch(
                        checked = uiState.syncWifiOnly,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetSyncWifiOnly(it)) 
                        },
                        enabled = uiState.autoSyncEnabled
                    )
                }
                
                // 立即同步
                SettingButton(
                    title = "立即同步",
                    description = "立即执行数据同步",
                    icon = Icons.Default.Refresh,
                    enabled = true
                ) {
                    viewModel.handleEvent(SettingsEvent.SyncNow)
                }
            }
            
            // 隐私设置
            SettingsCategory(
                title = "隐私设置(待完成)",
                icon = Icons.Default.Lock
            ) {
                // 应用锁定
                SettingItem(
                    title = "应用锁定",
                    description = "启用应用锁定功能",
                    icon = Icons.Default.Lock
                ) {
                    Switch(
                        checked = uiState.appLockEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetAppLock(it)) 
                        }
                    )
                }
                
                // 指纹解锁
                SettingItem(
                    title = "指纹解锁",
                    description = "使用设备指纹解锁应用",
                    icon = Icons.Default.Fingerprint,
                    enabled = uiState.appLockEnabled
                ) {
                    Switch(
                        checked = uiState.biometricAuthEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetBiometricAuth(it)) 
                        },
                        enabled = uiState.appLockEnabled
                    )
                }
                
                // 隐私模式
                SettingItem(
                    title = "隐私模式",
                    description = "隐藏日记内容预览",
                    icon = Icons.Default.Lock
                ) {
                    Switch(
                        checked = uiState.privacyModeEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetPrivacyMode(it)) 
                        }
                    )
                }
            }
            
            // 存储设置
            SettingsCategory(
                title = "存储设置(待完成)",
                icon = Icons.Default.Save
            ) {
                // 图片压缩
                SettingItem(
                    title = "压缩图片",
                    description = "保存日记时压缩图片以节省空间",
                    icon = Icons.Default.ImageSearch
                ) {
                    Switch(
                        checked = uiState.compressImages,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetCompressImages(it)) 
                        }
                    )
                }
                
                // 备份功能
                SettingItem(
                    title = "自动备份",
                    description = "定期自动备份数据库",
                    icon = Icons.Default.Save
                ) {
                    Switch(
                        checked = uiState.backupEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetBackupEnabled(it)) 
                        }
                    )
                }
                
                // 立即备份
                SettingButton(
                    title = "立即备份",
                    description = "立即备份数据库",
                    icon = Icons.Default.Save,
                    enabled = true
                ) {
                    viewModel.handleEvent(SettingsEvent.BackupNow)
                }
                
                // 恢复备份
                SettingButton(
                    title = "恢复备份",
                    description = "从备份恢复数据",
                    icon = Icons.Default.Refresh,
                    enabled = true
                ) {
                    viewModel.handleEvent(SettingsEvent.RestoreBackup)
                }
            }
            
            // 通知设置
            SettingsCategory(
                title = "通知设置",
                icon = Icons.Default.Notifications
            ) {
                SettingItem(
                    title = "应用通知",
                    description = "启用应用推送通知",
                    icon = Icons.Default.Notifications
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.notificationsEnabled) "开启" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetNotifications(it)) 
                        }
                    )
                    }
                }
                
                SettingItem(
                    title = "日记提醒",
                    description = "每日提醒你记录日记",
                    icon = Icons.Default.Notifications
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.reminderEnabled) "开启" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetReminder(it)) 
                            }
                        )
                    }
                }
                
                if (uiState.reminderEnabled) {
                    SettingItem(
                        title = "提醒时间",
                        description = "设置每日提醒时间",
                        icon = Icons.Default.Notifications,
                        onClick = {
                            // 时间选择器逻辑
                        }
                    ) {
                        Text(
                            text = uiState.reminderTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // AI设置
            SettingsCategory(
                title = "AI写作设置",
                icon = Icons.Default.Create
            ) {
                // AI模型选择
                SettingItem(
                    title = "AI模型选择",
                    description = "选择用于生成内容的AI模型",
                    icon = Icons.Default.Settings,
                    onClick = {
                        // 打开模型选择对话框
                        showModelPickerDialog = true
                    }
                ) {
                    Text(
                        text = AIModels.getModelDisplayName(uiState.aiSettings.modelType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 默认使用历史日记
                SettingItem(
                    title = "默认使用历史日记参考",
                    description = "AI生成内容时默认参考历史日记风格",
                    icon = Icons.Default.History
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.aiSettings.useHistoricalJournalsDefault) "开启" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = uiState.aiSettings.useHistoricalJournalsDefault,
                            onCheckedChange = { 
                                viewModel.handleEvent(SettingsEvent.SetUseHistoricalJournalsDefault(it)) 
                            }
                        )
                    }
                }
                
                // 参考历史日记数量
                if (uiState.aiSettings.useHistoricalJournalsDefault) {
                    SettingItem(
                        title = "默认参考日记数量",
                        description = "AI生成内容时参考的历史日记数量",
                        icon = Icons.Default.Numbers
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { 
                                    val newCount = (uiState.aiSettings.historicalJournalsCountDefault - 1).coerceAtLeast(1)
                                    viewModel.handleEvent(SettingsEvent.SetHistoricalJournalsCountDefault(newCount))
                                },
                                enabled = uiState.aiSettings.historicalJournalsCountDefault > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "减少数量"
                                )
                            }
                            
                            Text(
                                text = "${uiState.aiSettings.historicalJournalsCountDefault}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            IconButton(
                                onClick = { 
                                    val newCount = (uiState.aiSettings.historicalJournalsCountDefault + 1).coerceAtMost(10)
                                    viewModel.handleEvent(SettingsEvent.SetHistoricalJournalsCountDefault(newCount))
                                },
                                enabled = uiState.aiSettings.historicalJournalsCountDefault < 10
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "增加数量"
                                )
                            }
                        }
                    }
                }
                
                // 默认显示高级设置
                SettingItem(
                    title = "默认显示高级设置",
                    description = "AI写作界面默认展开高级设置选项",
                    icon = Icons.Default.Settings
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.aiSettings.showAdvancedSettingsDefault) "开启" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = uiState.aiSettings.showAdvancedSettingsDefault,
                            onCheckedChange = { 
                                viewModel.handleEvent(SettingsEvent.SetShowAdvancedSettingsDefault(it)) 
                            }
                        )
                    }
                }
                
                // 内容长度上限
                SettingItem(
                    title = "内容长度上限",
                    description = "AI生成内容的最大字符数",
                    icon = Icons.Default.TextFields
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                val newLength = (uiState.aiSettings.maxContentLength - 100).coerceAtLeast(200)
                                viewModel.handleEvent(SettingsEvent.SetMaxContentLength(newLength))
                            },
                            enabled = uiState.aiSettings.maxContentLength > 200
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "减少长度"
                            )
                        }
                        
                        Text(
                            text = "${uiState.aiSettings.maxContentLength}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        IconButton(
                            onClick = { 
                                val newLength = (uiState.aiSettings.maxContentLength + 100).coerceAtMost(1000)
                                viewModel.handleEvent(SettingsEvent.SetMaxContentLength(newLength))
                            },
                            enabled = uiState.aiSettings.maxContentLength < 1000
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "增加长度"
                            )
                        }
                    }
                }
            }
            
            // 高级设置
            SettingsCategory(
                title = "高级设置",
                icon = Icons.Default.Settings
            ) {
                // 调试模式
                SettingItem(
                    title = "调试模式",
                    description = "启用应用调试功能",
                    icon = Icons.Default.Settings
                ) {
                    Switch(
                        checked = uiState.debugModeEnabled,
                        onCheckedChange = {
                            viewModel.handleEvent(SettingsEvent.SetDebugMode(it))
                        }
                    )
                }

                // 实验功能
                SettingItem(
                    title = "实验性功能",
                    description = "启用应用实验性功能",
                    icon = Icons.Default.Settings
                ) {
                    Switch(
                        checked = uiState.experimentalFeaturesEnabled,
                        onCheckedChange = {
                            viewModel.handleEvent(SettingsEvent.SetExperimentalFeatures(it))
                        }
                    )
                }

                // 清除缓存
                SettingButton(
                    title = "清除缓存",
                    description = "清除应用缓存数据",
                    icon = Icons.Default.Refresh,
                    enabled = true
                ) {
                    viewModel.handleEvent(SettingsEvent.ClearCache)
                }

                // 重置设置
                SettingButton(
                    title = "重置设置",
                    description = "将所有设置重置为默认值",
                    icon = Icons.Default.Refresh,
                    enabled = true
                ) {
                    viewModel.handleEvent(SettingsEvent.ResetSettings)
                }
            }
            
            // API设置
            Text(
                text = "API设置",
                style = MaterialTheme.typography.titleMedium
            )
            
            // 情感分析API密钥设置
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("情感分析API密钥") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        if (passwordVisible) {
                            Text("隐藏")
                        } else {
                            Text("显示")
                        }
                    }
                }
            )
            
            Button(
                onClick = {
                    // 保存API密钥到SharedPreferences
                    sharedPrefs.edit().putString("sentiment_api_key", apiKey).apply()
                    SnackBarUtils.showSnackBar("API密钥已保存")
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存API密钥")
            }
            
            Text(
                text = "注意：API密钥用于情感分析功能，请从百度千帆平台获取",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 关于
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium
            )
            
            SettingsItem(
                title = "版本信息",
                description = "查看应用版本和更新日志",
                onClick = { viewModel.showVersionInfo() }
            )
            
            SettingsItem(
                title = "隐私政策",
                description = "查看应用的隐私政策",
                onClick = { viewModel.showPrivacyPolicy() }
            )
            
            Spacer(modifier = Modifier.height(80.dp)) // 为FAB留出空间
        }
    }
}

/**
 * 设置分类组件
 * 显示一个可折叠的设置分类
 */
@Composable
fun SettingsCategory(
    title: String,
    icon: ImageVector,
    initialExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * 设置项组件
 * 显示一个设置项，包含标题、描述和控制元素
 */
@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    control: @Composable () -> Unit
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.padding(horizontal = 12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        control()
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * 设置按钮组件
 * 显示一个带按钮的设置项
 */
@Composable
fun SettingButton(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    SettingItem(
        title = title,
        description = description,
        icon = icon,
        enabled = enabled
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("执行")
        }
    }
}

/**
 * 颜色选择对话框
 */
@Composable
fun ColorPickerDialog(
    selectedIndex: Int,
    colors: List<String>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 添加本地状态管理当前选中颜色
    var currentSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    
    // 显示选中的颜色索引和名称，方便调试
    val colorName = colors[currentSelectedIndex]
    val colorText = "当前选择: $colorName (索引: $currentSelectedIndex)"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题颜色", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // 当前选中颜色提示
                Text(
                    text = colorText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 颜色网格
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (index in 0 until minOf(5, colors.size)) {
                        ColorButton(
                            color = getColorForIndex(index),
                            isSelected = index == currentSelectedIndex,
                            onClick = { 
                                currentSelectedIndex = index
                                onColorSelected(index)
                            }
                        )
                    }
                }
                
                if (colors.size > 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (index in 5 until minOf(10, colors.size)) {
                            ColorButton(
                                color = getColorForIndex(index),
                                isSelected = index == currentSelectedIndex,
                                onClick = { 
                                    currentSelectedIndex = index
                                    onColorSelected(index)
                                }
                            )
                        }
                    }
                }
                
                // 重置按钮 - 恢复默认颜色
                Button(
                    onClick = {
                        currentSelectedIndex = 0 // 默认颜色的索引为0
                        onColorSelected(0)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("恢复默认颜色")
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // 颜色列表
                colors.forEachIndexed { index, colorName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                currentSelectedIndex = index
                                onColorSelected(index)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 颜色预览
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    getColorForIndex(index),
                                    shape = CircleShape
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 颜色名称
                        Text(
                            text = colorName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // 选中标记
                        if (index == currentSelectedIndex) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = getColorForIndex(currentSelectedIndex)
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = null // 移除取消按钮，因为我们已经实时应用颜色，确定按钮只是关闭对话框
    )
}

/**
 * 颜色按钮组件
 */
@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 背景圆形
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            // 显示选中标记
            if (isSelected) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 根据索引获取颜色
 */
@Composable
fun getColorForIndex(index: Int): Color {
    return when (index) {
        0 -> MaterialTheme.colorScheme.primary // 跟随系统
        1 -> Color(0xFF1A237E) // 深蓝
        2 -> Color(0xFF2E7D32) // 绿色
        3 -> Color(0xFF4A148C) // 紫色
        4 -> Color(0xFFE65100) // 橙色
        5 -> Color(0xFFB71C1C) // 红色
        6 -> Color(0xFFC2185B) // 粉色
        7 -> Color(0xFFF9A825) // 黄色
        8 -> Color(0xFF00838F) // 青色
        9 -> Color(0xFF212121) // 黑色
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * 主题预览卡片
 */
@Composable
fun ThemePreviewCard(
    isDarkMode: Boolean,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 浅色主题预览
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(
                width = if (!isDarkMode) 2.dp else 1.dp,
                color = if (!isDarkMode) primaryColor else Color.LightGray
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(primaryColor, CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                )
                
                if (!isDarkMode) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = primaryColor
                        )
                    }
                }
            }
        }
        
        // 深色主题预览
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF121212),
            border = BorderStroke(
                width = if (isDarkMode) 2.dp else 1.dp,
                color = if (isDarkMode) primaryColor else Color.DarkGray
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(primaryColor, CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                )
                
                if (isDarkMode) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = primaryColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * AI模型选择对话框
 * 按照模型分类展示不同系列的模型
 */
@Composable
fun AIModelPickerDialog(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 获取按分类的模型列表
    val modelsByCategory = AIModels.getModelsByCategory()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择AI模型") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 按照分类展示模型
                modelsByCategory.forEach { (category, models) ->
                    // 分类标题
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 分类下的模型列表
                    models.forEach { (modelId, displayName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModelSelected(modelId) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = modelId == selectedModel,
                                onClick = { onModelSelected(modelId) }
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                // 提取模型名称和功能说明
                                val nameWithDescription = displayName.split("（")
                                val modelName = nameWithDescription[0]
                                val modelDescription = if (nameWithDescription.size > 1) 
                                    "（${nameWithDescription[1]}" else ""
                                
                                Text(
                                    text = modelName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                if (modelDescription.isNotEmpty()) {
                                    Text(
                                        text = modelDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // 类别分隔符
                    if (category != modelsByCategory.keys.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 在SettingsViewModel类中添加这两个方法
fun SettingsViewModel.showVersionInfo() {
    // 版本信息逻辑
    SnackBarUtils.showSnackBar("应用版本: 1.0.0")
}

fun SettingsViewModel.showPrivacyPolicy() {
    // 隐私政策逻辑
    SnackBarUtils.showSnackBar("隐私政策: 暂未提供")
} 