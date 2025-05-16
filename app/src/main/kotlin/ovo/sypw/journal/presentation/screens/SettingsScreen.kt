package ovo.sypw.journal.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import ovo.sypw.journal.data.model.SettingsEvent
import ovo.sypw.journal.data.model.SettingsState
import ovo.sypw.journal.presentation.viewmodels.SettingsViewModel

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
    val uiState by viewModel.uiState.collectAsState()
    val settingsChanged by viewModel.settingsChanged.collectAsState()
    val needsRestart by viewModel.needsRestart.collectAsState()
    
    // 重启提示对话框
    var showRestartDialog by remember { mutableStateOf(false) }
    
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
        },
        floatingActionButton = {
            if (settingsChanged) {
                FloatingActionButton(
                    onClick = {
                        viewModel.saveSettings()
                        if (needsRestart) {
                            showRestartDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "保存设置",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // 外观设置
            SettingsCategory(
                title = "外观设置",
                icon = Icons.Default.ColorLens
            ) {
                // 主题模式设置
                SettingItem(
                    title = "深色模式",
                    description = "启用应用的深色主题",
                    icon = Icons.Default.BrightnessHigh
                ) {
                    Switch(
                        checked = uiState.useDarkTheme,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetDarkTheme(it)) 
                        },
                        enabled = !uiState.useSystemTheme
                    )
                }
                
                // 跟随系统主题
                SettingItem(
                    title = "跟随系统主题",
                    description = "根据系统设置自动切换明暗主题",
                    icon = Icons.Default.BrightnessAuto
                ) {
                    Switch(
                        checked = uiState.useSystemTheme,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetUseSystemTheme(it)) 
                        }
                    )
                }
                
                // 主题颜色
                SettingItem(
                    title = "主题颜色",
                    description = "选择应用的主色调",
                    icon = Icons.Default.ColorLens,
                    onClick = {
                        // 打开颜色选择对话框
                    }
                ) {
                    val colors = viewModel.getThemeColors()
                    Text(
                        text = colors[uiState.primaryColorIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            // 通用设置
            SettingsCategory(
                title = "通用设置",
                icon = Icons.Default.Settings
            ) {
                // 默认位置
                SettingItem(
                    title = "启用默认位置",
                    description = "创建日记时自动填充默认位置",
                    icon = Icons.Default.ImageSearch
                ) {
                    Switch(
                        checked = uiState.defaultLocationEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetDefaultLocationEnabled(it)) 
                        }
                    )
                }
                
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
                    title = "仅WiFi下同步",
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
                title = "隐私设置",
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
                title = "存储设置",
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
                // 通知开关
                SettingItem(
                    title = "应用通知",
                    description = "启用应用通知功能",
                    icon = Icons.Default.Notifications
                ) {
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetNotifications(it)) 
                        }
                    )
                }
                
                // 日记提醒
                SettingItem(
                    title = "日记提醒",
                    description = "每天提醒您记录日记",
                    icon = Icons.Default.Notifications,
                    enabled = uiState.notificationsEnabled
                ) {
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = { 
                            viewModel.handleEvent(SettingsEvent.SetReminder(it)) 
                        },
                        enabled = uiState.notificationsEnabled
                    )
                }
            }
            
            // 高级设置
//            SettingsCategory(
//                title = "高级设置",
//                icon = Icons.Default.Tune
//            ) {
//                // 调试模式
//                SettingItem(
//                    title = "调试模式",
//                    description = "启用应用调试功能",
//                    icon = Icons.Default.Tune
//                ) {
//                    Switch(
//                        checked = uiState.debugModeEnabled,
//                        onCheckedChange = {
//                            viewModel.handleEvent(SettingsEvent.SetDebugMode(it))
//                        }
//                    )
//                }
//
////                 实验功能
//                SettingItem(
//                    title = "实验性功能",
//                    description = "启用应用实验性功能",
//                    icon = Icons.Default.Tune
//                ) {
//                    Switch(
//                        checked = uiState.experimentalFeaturesEnabled,
//                        onCheckedChange = {
//                            viewModel.handleEvent(SettingsEvent.SetExperimentalFeatures(it))
//                        }
//                    )
//                }
//
//                // 清除缓存
//                SettingButton(
//                    title = "清除缓存",
//                    description = "清除应用缓存数据",
//                    icon = Icons.Default.Refresh,
//                    enabled = true
//                ) {
//                    viewModel.handleEvent(SettingsEvent.ClearCache)
//                }
//
//                // 重置设置
//                SettingButton(
//                    title = "重置设置",
//                    description = "将所有设置重置为默认值",
//                    icon = Icons.Default.Refresh,
//                    enabled = true
//                ) {
//                    viewModel.handleEvent(SettingsEvent.ResetSettings)
//                }
//            }
            
            // 关于信息
            SettingItem(
                title = "关于",
                description = "应用版本 v1.0.0",
                icon = Icons.Default.Info,
                onClick = {
                    // 打开关于页面
                }
            ) {
//                Icon(
//                    imageVector = Icons.Default.KeyboardArrowRight,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                )
            }
            
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