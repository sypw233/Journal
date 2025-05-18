package ovo.sypw.journal.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.common.utils.ImagePickerUtils

/**
 * 图片选择器组件
 * 根据Android版本自动选择合适的选择器实现
 */
@Composable
fun ImagePicker(
    onImagesPicked: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "选择图片",
    maxSelectionCount: Int = 10
) {
    // 使用ImagePickerUtils获取适合当前系统版本的图片选择器
    val launchImagePicker = ImagePickerUtils.rememberImagePicker(
        onImagesPicked = onImagesPicked,
        maxSelectionCount = maxSelectionCount
    )
    
    Button(
        onClick = launchImagePicker,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Text(buttonText)
    }
} 