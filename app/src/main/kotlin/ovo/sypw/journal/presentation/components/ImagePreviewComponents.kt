package ovo.sypw.journal.presentation.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * 图片预览画廊组件
 * 支持多张图片浏览和切换
 * 实现从缩略图直接过渡到全屏预览的动画效果
 * 支持双指放大缩小和平移
 * 支持左右滑动切换图片
 * 支持下滑关闭预览
 */
@Composable
fun ImageGalleryPreview(
    images: List<Any>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isVisible by remember { mutableStateOf(true) }

    // 添加协程作用域
    val scope = rememberCoroutineScope()

    // 使用线性动画，不带回弹效果
    val noBounceTween: AnimationSpec<Float> = tween(
        durationMillis = 200, // 加快动画速度
        easing = LinearEasing
    )

    // 动画过渡效果
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200), // 加快动画速度
        label = "alpha"
    )

    // 缩略图到全屏的过渡动画状态
    var animateFromThumbnail by remember { mutableStateOf(true) }
    var initialScale by remember { mutableFloatStateOf(0.7f) } // 起始比例更小，强化动画效果

    // 缩放和位置的动画，不带回弹
    val animatedScale by animateFloatAsState(
        targetValue = initialScale,
        animationSpec = noBounceTween,
        label = "scale"
    )

    // 双击放大动画的偏移量状态
    var targetOffsetX by remember { mutableFloatStateOf(0f) }
    var targetOffsetY by remember { mutableFloatStateOf(0f) }

    // 为偏移量添加动画
    val animatedOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing), // 加快动画速度
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing), // 加快动画速度
        label = "offsetY"
    )

    // 是否正在动画中
    var isAnimating by remember { mutableStateOf(false) }

    // 启动时的入场动画
    LaunchedEffect(Unit) {
        // 短暂延迟后重置动画状态，完成从缩略图到全屏的过渡
        delay(50) // 减少延迟，使动画更加流畅
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
                .background(Color.Black.copy(alpha = animatedAlpha))
                .alpha(animatedAlpha)
        ) {
            // 当前显示的图片
            val currentImage = images[currentIndex]

            // 图片缩放和平移状态
            var scale by remember { mutableFloatStateOf(1f) }
            var targetScale by remember { mutableFloatStateOf(1f) } // 目标缩放值，用于动画
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            // 使用动画状态实现平滑的缩放，无回弹效果
            val animatedScaleValue by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = noBounceTween,
                label = "zoomScale"
            )

            // 实时更新缩放值
            LaunchedEffect(animatedScaleValue) {
                scale = animatedScaleValue
            }

            // 滑动切换相关状态
            var dragOffsetX by remember { mutableFloatStateOf(0f) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            // 记录上一次的手势位置
            var lastPosition by remember { mutableStateOf(Offset.Zero) }

            // 下滑关闭相关状态
            var backgroundAlpha by remember { mutableFloatStateOf(1f) }

            // 双指缩放状态
            var minScale = 0.5f
            var maxScale = 5f // 允许更大的缩放范围

            // 是否处于放大状态
            val isZoomed = scale > 1.1f

            // 重置缩放和平移状态，当切换图片时
            LaunchedEffect(currentIndex) {
                targetScale = 1f
                scale = 1f
                offsetX = 0f
                offsetY = 0f
                dragOffsetX = 0f
                dragOffsetY = 0f
                targetOffsetX = 0f
                targetOffsetY = 0f
                isAnimating = false
            }

            // 图片预览，支持缩放、平移和左右滑动切换
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentImage)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = "预览图片 ${currentIndex + 1}/${images.size}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 合并动画和交互状态的缩放和平移
                        val effectiveScale = if (animateFromThumbnail) animatedScale else scale

                        scaleX = effectiveScale
                        scaleY = effectiveScale

                        // 使用动画偏移量或直接偏移量，取决于是否在动画中
                        val effectiveOffsetX =
                            if (isAnimating) animatedOffsetX else offsetX + dragOffsetX
                        val effectiveOffsetY =
                            if (isAnimating) animatedOffsetY else offsetY + dragOffsetY

                        translationX = effectiveOffsetX
                        translationY = effectiveOffsetY

                        // 应用背景透明度
                        alpha = backgroundAlpha
                    }
                    .pointerInput(scale) { // 添加scale作为key，确保放大状态变化时重组
                        // 增强的变换手势检测，支持双指缩放
                        detectTransformGestures(
                            onGesture = { centroid, pan, zoom, rotation ->
                                // 处理缩放
                                val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                                // 增加拖动灵敏度，同时保持平移和缩放的平衡
                                val panSensitivity = 1.2f

                                // 计算新的偏移量，考虑缩放中心点
                                val newOffsetX = offsetX + pan.x * panSensitivity
                                val newOffsetY = offsetY + pan.y * panSensitivity

                                // 只有当不在拖动状态时才更新
                                if (!isDragging) {
                                    targetScale = newScale
                                    offsetX = newOffsetX
                                    offsetY = newOffsetY
                                }
                            }
                        )
                    }
                    // 添加双击放大/缩小功能
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapPosition ->
                                // 双击时在当前缩放和最大缩放之间切换
                                if (scale > 1.5f) {
                                    // 如果已经放大，则恢复到原始大小
                                    isAnimating = true
                                    targetScale = 1f
                                    targetOffsetX = 0f
                                    targetOffsetY = 0f

                                    // 在动画完成后重置拖动状态
                                    scope.launch {
                                        delay(200) // 等待动画完成，与动画时长匹配
                                        offsetX = 0f
                                        offsetY = 0f
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        isAnimating = false
                                    }
                                } else {
                                    // 放大到2.5倍
                                    val targetZoom = 2.5f

                                    // 计算视图中心
                                    val viewCenterX = size.width / 2
                                    val viewCenterY = size.height / 2

                                    // 计算双击点相对于视图中心的偏移量
                                    val touchOffsetX = tapPosition.x - viewCenterX
                                    val touchOffsetY = tapPosition.y - viewCenterY

                                    // 计算放大后的偏移量
                                    // 关键公式：偏移量 = -双击点偏移 * (放大倍数 - 1)
                                    // 这样可以确保双击点在放大前后位置不变
                                    val finalOffsetX = -touchOffsetX * (targetZoom - 1)
                                    val finalOffsetY = -touchOffsetY * (targetZoom - 1)

                                    // 开始动画过程
                                    isAnimating = true
                                    targetScale = targetZoom

                                    // 从当前位置开始动画
                                    targetOffsetX = offsetX // 直接从当前固定偏移开始，不包含拖动偏移
                                    targetOffsetY = offsetY

                                    // 使用协程延迟设置目标位置，创建平滑动画
                                    scope.launch {
                                        delay(10) // 使用10ms延迟，确保起始状态被捕获

                                        // 设置动画目标位置
                                        targetOffsetX = finalOffsetX
                                        targetOffsetY = finalOffsetY

                                        // 等待动画完成后更新实际状态
                                        delay(200) // 与动画时长匹配
                                        offsetX = finalOffsetX
                                        offsetY = finalOffsetY
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        isAnimating = false
                                    }
                                }
                            },
                            // 单击切换UI
                            onTap = {
                                // 可以在这里实现单击切换UI可见性的功能
                            }
                        )
                    }
                    .pointerInput(scale) { // 添加scale作为key，确保放大状态变化时重组
                        // 拖动手势检测
                        detectDragGestures(
                            onDragStart = {
                                // 只有在非动画状态下才允许拖动
                                if (!isAnimating) {
                                    isDragging = true
                                    lastPosition = it
                                }
                            },
                            onDragEnd = {
                                // 只有在拖动状态下才处理拖动结束
                                if (isDragging) {
                                    if (isZoomed) {
                                        // 放大状态下，拖动结束时将dragOffset合并到offset
                                        offsetX += dragOffsetX
                                        offsetY += dragOffsetY

                                        // 计算最大允许偏移，根据实际缩放比例调整
                                        val maxOffsetX =
                                            (size.width * (scale - 1) / 2).coerceAtLeast(0f)
                                        val maxOffsetY =
                                            (size.height * (scale - 1) / 2).coerceAtLeast(0f)

                                        // 限制偏移范围
                                        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                    } else {
                                        // 非放大状态下进行图片切换或关闭判断
                                        val dragDistance = abs(dragOffsetX)
                                        val dragDistanceY = abs(dragOffsetY)

                                        // 判断是否为下滑关闭手势
                                        if (dragDistanceY > size.height / 5 && dragDistanceY > dragDistance) {
                                            // 执行关闭
                                            isVisible = false
                                            onDismiss()
                                        }
                                        // 判断是否为左右切换图片
                                        else if (dragDistance > size.width / 4 && dragDistance > dragDistanceY) {
                                            if (dragOffsetX > 0 && currentIndex > 0) {
                                                // 向右滑动，显示上一张
                                                currentIndex--
                                            } else if (dragOffsetX < 0 && currentIndex < images.size - 1) {
                                                // 向左滑动，显示下一张
                                                currentIndex++
                                            }
                                        }
                                    }
                                }

                                // 重置拖动状态
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                backgroundAlpha = 1f
                                isDragging = false
                            },
                            onDragCancel = {
                                if (isDragging && isZoomed) {
                                    // 放大状态下取消拖动时，也需要合并偏移量
                                    offsetX += dragOffsetX
                                    offsetY += dragOffsetY
                                }

                                // 重置拖动状态
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                backgroundAlpha = 1f
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                // 只有在拖动状态下才处理拖动
                                if (isDragging) {
                                    change.consume()

                                    // 记录位置
                                    lastPosition = change.position

                                    // 根据缩放状态处理拖动
                                    if (isZoomed) {
                                        // 放大状态：直接处理图片平移，增加拖动灵敏度
                                        val dragSensitivity = 1.5f
                                        dragOffsetX += dragAmount.x * dragSensitivity
                                        dragOffsetY += dragAmount.y * dragSensitivity

                                        // 计算可用的最大偏移量（基于缩放比例）
                                        val maxOffsetX =
                                            (size.width * (scale - 1) / 2).coerceAtLeast(0f)
                                        val maxOffsetY =
                                            (size.height * (scale - 1) / 2).coerceAtLeast(0f)

                                        // 计算总偏移量
                                        val totalOffsetX = offsetX + dragOffsetX
                                        val totalOffsetY = offsetY + dragOffsetY

                                        // 当接近边界时添加阻尼效果，使拖动更自然
                                        if (abs(totalOffsetX) > maxOffsetX) {
                                            // 更轻微的阻尼感，提高响应度
                                            dragOffsetX = (dragOffsetX * 0.7f)
                                        }

                                        if (abs(totalOffsetY) > maxOffsetY) {
                                            // 更轻微的阻尼感，提高响应度
                                            dragOffsetY = (dragOffsetY * 0.7f)
                                        }
                                    } else {
                                        // 未放大状态：处理图片切换和下滑关闭

                                        // 更新垂直拖动偏移量
                                        dragOffsetY += dragAmount.y

                                        // 根据下滑距离调整背景透明度
                                        if (dragOffsetY > 0) {
                                            val dragPercentage = min(
                                                (dragOffsetY / size.height).coerceIn(0f, 0.5f),
                                                0.8f
                                            )
                                            backgroundAlpha = 1f - dragPercentage * 2
                                        }

                                        // 更新水平拖动偏移量
                                        dragOffsetX += dragAmount.x

                                        // 边界阻尼效果
                                        if ((currentIndex == 0 && dragOffsetX > 0) ||
                                            (currentIndex == images.size - 1 && dragOffsetX < 0)
                                        ) {
                                            dragOffsetX = dragOffsetX * 0.3f
                                        }
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
                    .alpha(animatedAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭预览",
                    tint = Color.White
                )
            }
        }
    }
}