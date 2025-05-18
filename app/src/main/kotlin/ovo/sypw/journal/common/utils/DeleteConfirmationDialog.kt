package ovo.sypw.journal.common.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import ovo.sypw.journal.data.JournalPreferences

/**
 * 删除确认对话框
 * 根据用户设置决定是否显示删除确认对话框
 */
object DeleteConfirmationUtils {
    
    /**
     * 删除确认流程
     * @param preferences 用户偏好设置
     * @param itemName 要删除的项目名称
     * @param onConfirm 确认删除的回调
     */
    @Composable
    fun ConfirmDelete(
        preferences: JournalPreferences,
        itemName: String,
        onConfirm: () -> Unit
    ): MutableState<Boolean> {
        // 创建确认对话框显示状态
        val showDialog = remember { mutableStateOf(false) }
        
        // 如果需要确认并且对话框被触发
        if (preferences.isDeleteConfirmationEnabled() && showDialog.value) {
            DeleteConfirmationDialog(
                title = "删除确认",
                message = "确定要删除「$itemName」吗？此操作无法撤销。",
                onConfirm = {
                    onConfirm()
                    showDialog.value = false
                },
                onDismiss = {
                    showDialog.value = false
                }
            )
        }
        
        return showDialog
    }
    
    /**
     * 直接执行删除操作
     * 根据用户设置决定是直接删除还是先显示确认对话框
     *
     * @param preferences 用户偏好设置
     * @param showDialog 对话框显示状态
     * @param onDelete 直接删除的回调
     */
    fun delete(
        preferences: JournalPreferences,
        showDialog: MutableState<Boolean>,
        onDelete: () -> Unit
    ) {
        if (preferences.isDeleteConfirmationEnabled()) {
            // 如果启用了删除确认，则显示确认对话框
            showDialog.value = true
        } else {
            // 否则直接删除
            onDelete()
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
} 