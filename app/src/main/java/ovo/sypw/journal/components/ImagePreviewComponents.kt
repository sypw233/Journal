package ovo.sypw.journal.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * 图片预览组件
 * 支持缩放和平移操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreview(
    image: Bitmap,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 控制缩放和平移
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 图片预览，支持缩放和平移
            AsyncImage(
                model = image,
                contentDescription = "Preview Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f)

                            // 计算新的偏移量，考虑缩放因子
                            val newOffsetX = offsetX + pan.x * scale
                            val newOffsetY = offsetY + pan.y * scale

                            // 限制平移范围，防止图片完全移出屏幕
                            val maxOffset = 1000f * scale
                            offsetX = newOffsetX.coerceIn(-maxOffset, maxOffset)
                            offsetY = newOffsetY.coerceIn(-maxOffset, maxOffset)
                        }
                    }
            )

            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Preview",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 图片预览画廊组件
 * 支持多张图片浏览和切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryPreview(
    images: List<Bitmap>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentIndex by remember { mutableStateOf(initialIndex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 当前显示的图片
            val currentImage = images[currentIndex]

            // 使用单图预览组件的核心功能
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            // 图片预览，支持缩放和平移
            AsyncImage(
                model = currentImage,
                contentDescription = "Preview Image ${currentIndex + 1}/${images.size}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f)

                            // 计算新的偏移量，考虑缩放因子
                            val newOffsetX = offsetX + pan.x * scale
                            val newOffsetY = offsetY + pan.y * scale

                            // 限制平移范围，防止图片完全移出屏幕
                            val maxOffset = 1000f * scale
                            offsetX = newOffsetX.coerceIn(-maxOffset, maxOffset)
                            offsetY = newOffsetY.coerceIn(-maxOffset, maxOffset)
                        }
                    }
            )

            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Preview",
                    tint = Color.White
                )
            }
        }
    }
}