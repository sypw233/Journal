package ovo.sypw.journal.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.PermissionUtils
import ovo.sypw.journal.common.utils.RequestPermissions
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import java.util.Date

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
    onImagesChanged: ((MutableList<Any>) -> Unit)? = null
) {
    val context = LocalContext.current

    // 日记数据状态
    var journalText by remember { mutableStateOf(initialJournalData?.text ?: "") }
    var journalDate by remember { mutableStateOf(initialJournalData?.date ?: Date()) }
    var locationName by remember { mutableStateOf(initialJournalData?.location?.name ?: "") }
    var locationData by remember { mutableStateOf(initialJournalData?.location) }
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
    
    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = journalDate.time)

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showDatePicker = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "选择日期",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "日期: $journalDate")
        }

        // 位置选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "选择位置",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = {
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
                                    SnackBarUtils.showSnackBar("已获取当前位置")
                                },
                                onError = { errorMsg ->
                                    SnackBarUtils.showSnackBar("获取位置失败: $errorMsg")
                                }
                            )
                        } else {
                            // 请求权限
                            SnackBarUtils.showSnackBar("需要定位权限才能获取当前位置")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "获取位置"
                        )
                    }
                }
            )
        }

        // 权限请求组件
        RequestPermissions(
            permissions = PermissionUtils.LOCATION_PERMISSIONS,
            onPermissionResult = { granted ->
                if (granted) {
                    // 权限已授予，可以获取位置
                    SnackBarUtils.showSnackBar("已获取定位权限")
                } else {
                    // 权限被拒绝
                    SnackBarUtils.showSnackBar("无法获取定位权限")
                }
            }
        )

        // 文字内容
        OutlinedTextField(
            value = journalText,
            onValueChange = { journalText = it },
            label = { Text("日记内容") },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(vertical = 8.dp)
        )

        // 图片选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "选择图片",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "添加图片")
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Icon(Icons.Default.Add, contentDescription = "添加图片")
            }
        }

        // 已选图片预览
        if (selectedImages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(selectedImages) { imageRes ->
                    Box(
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        AsyncImage(
                            model = imageRes,
                            contentDescription = "已选图片",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        )
                        IconButton(
                            onClick = { selectedImages.remove(imageRes) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(
                                    Color.White.copy(alpha = 0.7f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "移除图片",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 保存按钮
        Button(
            onClick = {
                // 创建新的日记对象，保留原始ID
                val newJournal = JournalData(
                    id = initialJournalData?.id ?: 0,
                    isMark = initialJournalData?.isMark,
                    date = journalDate,
                    text = journalText,
                    images = selectedImages.toMutableList(),
                    location = locationData
                        ?: (if (locationName.isNotEmpty()) LocationData(name = locationName) else null)
                )
                onSave(newJournal)
                SnackBarUtils.showSnackBar("日记已保存")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("保存")
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
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
} 