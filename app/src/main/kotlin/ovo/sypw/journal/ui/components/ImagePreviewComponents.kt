package ovo.sypw.journal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 图片预览画廊组件
 * 支持多张图片浏览和切换
 * 实现从缩略图直接过渡到全屏预览的动画效果
 * 支持左右滑动切换图片
 */
@Composable
fun ImageGalleryPreview(
    images: List<Any>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isVisible by remember { mutableStateOf(true) }

    // 动画过渡效果
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    // 缩略图到全屏的过渡动画状态
    var animateFromThumbnail by remember { mutableStateOf(true) }
    var initialScale by remember { mutableFloatStateOf(0.8f) }

    // 启动时的入场动画
    LaunchedEffect(Unit) {
        // 短暂延迟后重置动画状态，完成从缩略图到全屏的过渡
        delay(300)
        animateFromThumbnail = false
        initialScale = 1f
    }

    // 使用Dialog而不是ModalBottomSheet，实现全屏效果
    Dialog(
        onDismissRequest = {
            isVisible = false
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false, // 使用全屏宽度
            securePolicy = SecureFlagPolicy.Inherit
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .alpha(animatedAlpha)
                .graphicsLayer {
                    // 应用弹性动画效果到Dialog窗口

                }
        ) {
            // 当前显示的图片
            val currentImage = images[currentIndex]

            // 图片缩放和平移状态
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            // 滑动切换相关状态
            var dragOffsetX by remember { mutableFloatStateOf(0f) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            // 下滑关闭相关状态
            var backgroundAlpha by remember { mutableFloatStateOf(1f) }

            // 重置缩放和平移状态，当切换图片时
            LaunchedEffect(currentIndex) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            }

            // 图片预览，支持缩放、平移和左右滑动切换
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentImage)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = "Preview Image ${currentIndex + 1}/${images.size}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = if (animateFromThumbnail) initialScale else scale,
                        scaleY = if (animateFromThumbnail) initialScale else scale,
                        translationX = if (isDragging) dragOffsetX else offsetX,
                        translationY = if (isDragging && abs(dragOffsetY) > 0) dragOffsetY else offsetY,
                        alpha = backgroundAlpha
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // 只有在非拖动状态下才处理缩放和平移
                            if (!isDragging) {
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
                    }
                    // 添加双击放大/缩小功能
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapPosition ->
                                // 双击时在当前缩放和最大缩放之间切换
                                if (scale > 1.5f) {
                                    // 如果已经放大，则恢复到原始大小
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    // 放大到2.5倍，并将双击位置作为放大中心
                                    scale = 2.5f
                                    // 计算双击位置相对于屏幕中心的偏移
                                    val centerX = size.width / 2
                                    val centerY = size.height / 2
                                    offsetX = (centerX - tapPosition.x) * 1.5f
                                    offsetY = (centerY - tapPosition.y) * 1.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // 处理左右滑动切换图片和下滑关闭
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                // 判断是否为下滑关闭手势
                                if (abs(dragOffsetY) > size.height / 4 && abs(dragOffsetX) < abs(
                                        dragOffsetY
                                    )
                                ) {
                                    // 下滑距离足够且主要是垂直方向的滑动，执行关闭操作
                                    isVisible = false
                                    onDismiss()
                                }
                                // 判断是否需要切换图片（水平滑动）
                                else if (abs(dragOffsetX) > size.width / 3 && abs(dragOffsetY) < abs(
                                        dragOffsetX
                                    )
                                ) {
                                    if (dragOffsetX > 0 && currentIndex > 0) {
                                        // 向右滑动，显示上一张
                                        currentIndex--
                                    } else if (dragOffsetX < 0 && currentIndex < images.size - 1) {
                                        // 向左滑动，显示下一张
                                        currentIndex++
                                    }
                                }
                                // 重置拖动状态
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                backgroundAlpha = 1f
                                isDragging = false
                            },
                            onDragCancel = {
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                backgroundAlpha = 1f
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                // 处理垂直方向拖动（下滑关闭）
                                if (scale <= 1.1f) {
                                    // 更新垂直拖动偏移量
                                    dragOffsetY += dragAmount.y

                                    // 根据下滑距离调整背景透明度，实现渐变消失效果
                                    if (dragOffsetY > 0) {
                                        val dragPercentage =
                                            (dragOffsetY / size.height).coerceIn(0f, 0.5f)
                                        backgroundAlpha = 1f - dragPercentage * 2 // 最多降低到0透明度
                                    }

                                    // 处理水平方向拖动（切换图片）
                                    // 只有在缩放为1时才允许左右滑动切换图片
                                    // 更新水平拖动偏移量
                                    dragOffsetX += dragAmount.x

                                    // 限制拖动范围
                                    if ((currentIndex == 0 && dragOffsetX > 0) ||
                                        (currentIndex == images.size - 1 && dragOffsetX < 0)
                                    ) {
                                        // 第一张图片不能再向右滑，最后一张图片不能再向左滑
                                        dragOffsetX = dragOffsetX * 0.3f // 增加阻尼效果
                                    }
                                }
                            }
                        )
                    }
            )

            // 关闭按钮
            IconButton(
                onClick = {
                    isVisible = false
                    onDismiss()
                },
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