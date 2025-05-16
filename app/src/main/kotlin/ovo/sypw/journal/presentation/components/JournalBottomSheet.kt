package ovo.sypw.journal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import java.util.Date

/**
 * 日记底部弹出表单
 * 通用组件，用于添加或编辑日记，符合Material Design 3规范
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalBottomSheet(
    isVisible: Boolean,
    initialJournalData: JournalData? = null,
    onSave: (JournalData) -> Unit,
    onDismiss: () -> Unit
) {
    // 如果不可见，则不渲染内容
    if (!isVisible) return
    
    // 创建底部表单状态
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            // 只有当用户不是通过向下拖动来关闭表单时才调用onDismiss
            if (sheetValue == SheetValue.Hidden) {
                onDismiss()
            }
            true
        }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // 创建可变状态来存储编辑中的数据
    var editedText by remember { mutableStateOf(initialJournalData?.text ?: "") }
    var editedDate by remember { mutableStateOf(initialJournalData?.date ?: Date()) }
    var editedLocationName by remember { mutableStateOf(initialJournalData?.location?.name ?: "") }
    var editedLocationData by remember { mutableStateOf(initialJournalData?.location) }
    var editedImages by remember { mutableStateOf(initialJournalData?.images ?: mutableListOf()) }
    
    // 保存状态
    var isSaving by remember { mutableStateOf(false) }
    
    // 判断是新建还是编辑模式
    val isEditMode = initialJournalData != null
    val titleText = if (isEditMode) "编辑日记" else "新建日记"
    
    // 如果保存成功，关闭底部表单
    LaunchedEffect(isSaving) {
        if (isSaving) {
            // 构建更新后的日记数据
            val updatedJournal = JournalData(
                id = initialJournalData?.id ?: 0, // 新建时ID为0，编辑时保留原ID
                isMark = initialJournalData?.isMark ?: false,
                date = editedDate,
                text = editedText,
                images = editedImages,
                location = editedLocationData
                    ?: (if (editedLocationName.isNotEmpty()) LocationData(name = editedLocationName) else null)
            )
            
            // 保存更新后的日记
            onSave(updatedJournal)
            SnackBarUtils.showSnackBar("日记已保存")
            isSaving = false
            
            // 隐藏底部表单
            coroutineScope.launch {
                sheetState.hide()
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null, // 移除默认拖动条，使用自定义顶部栏
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 自定义顶部栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
//                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 标题
                    Text(
                        text = titleText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    // 保存按钮
                    IconButton(
                        onClick = {
                            // 构建更新后的日记数据
                            val updatedJournal = JournalData(
                                id = initialJournalData?.id ?: 0,
                                isMark = initialJournalData?.isMark ?: false,
                                date = editedDate,
                                text = editedText,
                                images = editedImages,
                                location = editedLocationData
                                    ?: (if (editedLocationName.isNotEmpty()) LocationData(name = editedLocationName) else null)
                            )
                            
                            // 触发保存
                            isSaving = true
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp) // 增加底部间距以适应软键盘
            ) {
                // 使用JournalEditContent组件
                JournalEditContent(
                    modifier = Modifier.fillMaxWidth(),
                    initialJournalData = initialJournalData,
                    onSave = { updatedJournal ->
                        // 更新本地状态
                        editedText = updatedJournal.text ?: ""
                        editedDate = updatedJournal.date ?: Date()
                        editedLocationName = updatedJournal.location?.name ?: ""
                        editedLocationData = updatedJournal.location
                        editedImages = updatedJournal.images ?: mutableListOf()
                        
                        // 触发保存
                        isSaving = true
                    },
                    onTextChanged = { editedText = it },
                    onDateChanged = { editedDate = it },
                    onLocationChanged = { name, data ->
                        editedLocationName = name
                        editedLocationData = data
                    },
                    onImagesChanged = { editedImages = it },
                    showSaveButton = false // 不显示保存按钮，因为已经在顶部有保存按钮了
                )
            }
        }
    }
} 