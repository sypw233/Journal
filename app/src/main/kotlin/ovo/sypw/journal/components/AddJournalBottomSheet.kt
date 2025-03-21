package ovo.sypw.journal.components

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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import ovo.sypw.journal.data.JournalDataSource.Companion.getDataBaseIdCountWithPositive
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.model.LocationData
import ovo.sypw.journal.utils.SnackBarUtils
import java.util.Date

/**
 * 添加日记的底部弹出框组件
 * 支持选择定位、日期、文字和图片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalBottomSheet(
    onDismiss: () -> Unit,
    onSave: (JournalData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current

    // 日记数据状态
    var journalText by remember { mutableStateOf("") }
    var journalDate by remember { mutableStateOf(Date()) }
    var locationName by remember { mutableStateOf("") }
    val selectedImages = remember { mutableStateListOf<Any>() }

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
                    SnackBarUtils.showSnackBar("无法获取部分图片的持久访问权限: ${e.message}")
                }
            }
            if (successCount > 0) {
                SnackBarUtils.showSnackBar("已添加${successCount}张图片")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "添加新日记",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                    onValueChange = { locationName = it },
                    label = { Text("位置") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 文字内容
            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                label = { Text("日记内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
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
                Text(text = "添加多张图片")
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Add, contentDescription = "添加多张图片")
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
                                contentDescription = "Selected Image",
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
                                    contentDescription = "Remove Image",
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
                    val id = getDataBaseIdCountWithPositive()
                    val newJournal = JournalData(
                        id = id,
                        date = journalDate,
                        text = journalText,
                        images = selectedImages.toMutableList(),
                        location = if (locationName.isNotEmpty()) LocationData(name = locationName) else null
                    )
                    onSave(newJournal)
                    onDismiss()
                    SnackBarUtils.showSnackBar("已添加新日记")
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
                        Text("确定")
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
}