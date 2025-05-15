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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import java.util.Date

/**
 * 日记编辑底部弹出表单
 * 以BottomSheet方式展示日记编辑界面，符合Material Design 3规范
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditBottomSheet(
    isVisible: Boolean,
    journalData: JournalData,
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
    var editedText by remember { mutableStateOf(journalData.text) }
    var editedDate by remember { mutableStateOf(journalData.date) }
    var editedLocationName by remember { mutableStateOf(journalData.location?.name ?: "") }
    var editedLocationData by remember { mutableStateOf(journalData.location) }
    var editedImages by remember { mutableStateOf(journalData.images ?: mutableListOf()) }
    
    // 保存状态
    var isSaving by remember { mutableStateOf(false) }
    
    // 如果保存成功，关闭底部表单
    LaunchedEffect(isSaving) {
        if (isSaving) {
            // 构建更新后的日记数据
            val updatedJournal = JournalData(
                id = journalData.id,
                isMark = journalData.isMark,
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
        dragHandle = { 
            // 自定义顶部栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 关闭按钮
                    IconButton(
                        onClick = { 
                            coroutineScope.launch {
                                sheetState.hide()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 标题
                    Text(
                        text = "编辑日记",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 保存按钮
                    IconButton(
                        onClick = { isSaving = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp) // 增加底部间距以适应软键盘
        ) {
            // 使用JournalEditContent组件
            JournalEditContent(
                modifier = Modifier.fillMaxWidth(),
                initialJournalData = journalData,
                onSave = { updatedJournal ->
                    // 更新本地状态
                    editedText = updatedJournal.text
                    editedDate = updatedJournal.date
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
                onImagesChanged = { editedImages = it }
            )
        }
    }
} 