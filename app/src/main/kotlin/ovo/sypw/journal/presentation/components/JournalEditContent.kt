package ovo.sypw.journal.presentation.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.jeziellago.compose.markdowntext.MarkdownText
import ovo.sypw.journal.R
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.PermissionUtils
import ovo.sypw.journal.common.utils.RequestPermissions
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 日记编辑内容组件
 * 用于编辑现有日记或创建新日记
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditContent(
    modifier: Modifier = Modifier,
    initialJournalData: JournalData? = null,
    onSave: (JournalData) -> Unit,
    onTextChanged: ((String) -> Unit)? = null,
    onDateChanged: ((Date) -> Unit)? = null,
    onLocationChanged: ((String, LocationData?) -> Unit)? = null,
    onImagesChanged: ((MutableList<Any>) -> Unit)? = null,
    onIsMarkdownChanged: ((Boolean) -> Unit)? = null,
    showSaveButton: Boolean = true
) {
    val context = LocalContext.current

    // 日记数据状态
    var journalText by remember { mutableStateOf(initialJournalData?.text ?: "") }
    var journalDate by remember { mutableStateOf(initialJournalData?.date ?: Date()) }
    var locationName by remember { mutableStateOf(initialJournalData?.location?.name ?: "") }
    var locationData by remember { mutableStateOf(initialJournalData?.location) }
    var isMarkdown by remember { mutableStateOf(initialJournalData?.isMarkdown == true) }
    var showMarkdownPreview by remember { mutableStateOf(false) }
    
    // 格式化日期显示
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE) }
    val formattedDate = remember(journalDate) { dateFormat.format(journalDate) }
    
    val selectedImages = remember { 
        mutableStateListOf<Any>().apply {
            initialJournalData?.images?.let { addAll(it) }
        }
    }
    
    // 初始化日记数据
    LaunchedEffect(initialJournalData) {
        journalText = initialJournalData?.text ?: ""
        journalDate = initialJournalData?.date ?: Date()
        locationName = initialJournalData?.location?.name ?: ""
        locationData = initialJournalData?.location
        isMarkdown = initialJournalData?.isMarkdown == true
        selectedImages.clear()
        initialJournalData?.images?.let { selectedImages.addAll(it) }
    }
    
    // 当数据变化时，通知父组件
    LaunchedEffect(journalText) {
        onTextChanged?.invoke(journalText)
    }
    
    LaunchedEffect(journalDate) {
        onDateChanged?.invoke(journalDate)
    }
    
    LaunchedEffect(locationName, locationData) {
        onLocationChanged?.invoke(locationName, locationData)
    }
    
    LaunchedEffect(selectedImages.size) {
        onImagesChanged?.invoke(selectedImages.toMutableList())
    }
    LaunchedEffect(isMarkdown) {
        onIsMarkdownChanged?.invoke(isMarkdown)
    }
    
    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = journalDate.time)
    
    // 地图选择器状态
    var showMapPicker by remember { mutableStateOf(false) }

    // 图片选择器（多选）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // 获取持久化URI权限，确保应用重启后仍能访问图片
            var successCount = 0
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    selectedImages.add(uri)
                    successCount++
                } catch (e: Exception) {
                    SnackBarUtils.showSnackBar("无法获取某些图片的持久访问权限: ${e.message}")
                }
            }
            if (successCount > 0) {
                SnackBarUtils.showSnackBar("已添加${successCount}张图片")
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 日期选择
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showDatePicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "选择日期",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 位置选择
        OutlinedTextField(
            value = locationName,

            onValueChange = { 
                locationName = it
                // 位置名称变更时，重置位置数据
                if (locationData != null && locationData?.name != it) {
                    locationData = if (it.isNotEmpty()) LocationData(name = it) else null
                }
            },
            label = { Text("位置") },
            placeholder = { Text("添加位置...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "位置图标",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (locationName.isNotEmpty()) {
                    IconButton(onClick = { 
                        locationName = ""
                        locationData = null
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清除位置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row {
                        // 地图选择位置按钮
                        IconButton(
                            onClick = { showMapPicker = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = "地图选择",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // 获取当前位置按钮
                        IconButton(
                            onClick = {
                                // 检查定位权限
                                if (PermissionUtils.hasPermissions(
                                        context,
                                        PermissionUtils.LOCATION_PERMISSIONS
                                    )
                                ) {
                                    // 已有权限，直接获取位置
                                    AMapLocationUtils.getCurrentLocation(
                                        context = context,
                                        onSuccess = { location ->
                                            locationName = location.name ?: ""
                                            locationData = location
//                                            SnackBarUtils.showSnackBar("已获取当前位置")
                                        },
                                        onError = { errorMsg ->
                                            SnackBarUtils.showSnackBar("获取位置失败: $errorMsg")
                                        }
                                    )
                                } else {
                                    // 请求权限
                                    SnackBarUtils.showSnackBar("需要定位权限才能获取当前位置")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "获取位置",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        )

        // 权限请求组件
        RequestPermissions(
            permissions = PermissionUtils.LOCATION_PERMISSIONS,
            onPermissionResult = { granted ->
                if (granted) {
                    // 权限已授予，可以获取位置
//                    SnackBarUtils.showSnackBar("已获取定位权限")
                } else {
                    // 权限被拒绝
                    SnackBarUtils.showSnackBar("无法获取定位权限")
                }
            }
        )

        // Markdown切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "使用Markdown格式",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isMarkdown,
                onCheckedChange = { isMarkdown = it
                    Log.d("TAG", "JournalEditContent: $it")}
            )
        }

        // 文字内容
        OutlinedTextField(
            value = journalText,
            onValueChange = { journalText = it },
            label = { Text("内容") },
            placeholder = { Text("写下今天的故事...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )
        
        // Markdown预览
        AnimatedVisibility(
            visible = isMarkdown && journalText.isNotEmpty() && showMarkdownPreview,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Markdown预览",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    MarkdownText(
                        markdown = journalText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Markdown预览切换按钮
        if (isMarkdown && journalText.isNotEmpty()) {
            TextButton(
                onClick = { showMarkdownPreview = !showMarkdownPreview },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(if (showMarkdownPreview) "隐藏预览" else "显示预览")
            }
        }

        // 图片选择
        if (selectedImages.isEmpty()) {
            FilledTonalButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "添加图片",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加图片")
            }
        }

        // 已选图片预览
        AnimatedVisibility(
            visible = selectedImages.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已添加 ${selectedImages.size} 张图片",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "添加更多图片",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(selectedImages) { image ->
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(120.dp)
                        ) {
                            // 图片预览
                            AsyncImage(
                                model = image,
                                contentDescription = "已选图片",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            
                            // 删除按钮
                            IconButton(
                                onClick = { selectedImages.remove(image) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "删除图片",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 保存按钮，仅在showSaveButton为true时显示
        if (showSaveButton) {
            ElevatedButton(
                onClick = {
                    // 创建新的日记对象，保留原始ID
                    val newJournal = JournalData(
                        id = initialJournalData?.id ?: 0,
                        date = journalDate,
                        text = journalText,
                        images = selectedImages.toMutableList(),
                        location = locationData
                            ?: (if (locationName.isNotEmpty()) LocationData(name = locationName) else null),
                        isMarkdown = isMarkdown
                    )
                    onSave(newJournal)
                    SnackBarUtils.showSnackBar("日记已保存")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text("保存")
            }
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        journalDate = Date(it)
                    }
                    showDatePicker = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                // 设置为false，确保对话框不超出屏幕边界，修复被遮挡问题
                usePlatformDefaultWidth = false
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
//                title = { Text("选择日期", style = MaterialTheme.typography.titleMedium) },
//                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
    
    // 地图选择器对话框
    if (showMapPicker) {
        MapPickerDialog(
            isVisible = true,
            initialLocation = locationData,
            onLocationSelected = { selectedLocation ->
                locationName = selectedLocation.name ?: ""
                locationData = selectedLocation
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }
} 