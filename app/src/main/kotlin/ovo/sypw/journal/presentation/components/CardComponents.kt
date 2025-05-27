package ovo.sypw.journal.presentation.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.jeziellago.compose.markdowntext.MarkdownText
import ovo.sypw.journal.R
import ovo.sypw.journal.common.utils.ImageLoadUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card高度
 * DEFAULT_HEIGHT 同时有图片和文字(3行)下的默认高度
 * 在有图片时默认最多显示3行文字
 * ONLY_IMAGE_HEIGHT 只有图片的高度
 * MIN_HEIGHT 只有1行文字时的高度
 */
private val IMAGE_HEIGHT = 180.dp
private val ROUNDED_SHAPE = RoundedCornerShape(12.dp)
private val IMAGE_SPACING = 4.dp

@Composable
fun SingleImage(image: Any, onImageClick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(ROUNDED_SHAPE)
            .clickable { onImageClick(0) }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image)
                .size(Int.MAX_VALUE, 180)
                .build(),
            contentDescription = "Journal Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            imageLoader = ImageLoadUtils.getImageLoader(),
            clipToBounds = true
        )
    }
}

@Composable
fun TwoImages(images: MutableList<Any>, onImageClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(ROUNDED_SHAPE)
                .clickable { onImageClick(0) }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(images[0])
                    .size(Int.MAX_VALUE, 180)
                    .build(),
                contentDescription = "Journal Image 1",
                contentScale = ContentScale.Crop,
                imageLoader = ImageLoadUtils.getImageLoader(),
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(ROUNDED_SHAPE)
                .clickable { onImageClick(1) }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(images[1])
                    .size(Int.MAX_VALUE, 180)
                    .build(),
                contentDescription = "Journal Image 2",
                contentScale = ContentScale.Crop,
                imageLoader = ImageLoadUtils.getImageLoader(),
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        }
    }
}

@Composable
fun ThreeImages(images: MutableList<Any>, onImageClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
    ) {
        // 左侧一张大图
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(ROUNDED_SHAPE)
                .clickable { onImageClick(0) }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(images[0])
                    .size(Int.MAX_VALUE, 180)
                    .build(),
                contentDescription = "Journal Image 1",
                contentScale = ContentScale.Crop,
                imageLoader = ImageLoadUtils.getImageLoader(),
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        }
        
        // 右侧上下两张小图
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
        ) {
            // 右上图片
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(ROUNDED_SHAPE)
                    .clickable { onImageClick(1) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[1])
                        .size(Int.MAX_VALUE, 90)
                        .build(),
                    contentDescription = "Journal Image 2",
                    contentScale = ContentScale.Crop,
                    imageLoader = ImageLoadUtils.getImageLoader(),
                    modifier = Modifier.fillMaxSize(),
                    clipToBounds = true
                )
            }
            
            // 右下图片
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(ROUNDED_SHAPE)
                    .clickable { onImageClick(2) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[2])
                        .size(Int.MAX_VALUE, 90)
                        .build(),
                    contentDescription = "Journal Image 3",
                    contentScale = ContentScale.Crop,
                    imageLoader = ImageLoadUtils.getImageLoader(),
                    modifier = Modifier.fillMaxSize(),
                    clipToBounds = true
                )
            }
        }
    }
}

@Composable
fun MultipleImages(images: MutableList<Any>, onImageClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
    ) {
        // 左侧一张大图
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(ROUNDED_SHAPE)
                .clickable { onImageClick(0) }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(images[0])
                    .size(Int.MAX_VALUE, 180)
                    .build(),
                contentDescription = "Journal Image 1",
                contentScale = ContentScale.Crop,
                imageLoader = ImageLoadUtils.getImageLoader(),
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        }

        // 右侧区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
        ) {
            if (images.size == 4) {
                // 四张图片布局：右侧上方一张，下方两张
                
                // 右上方一张
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(ROUNDED_SHAPE)
                        .clickable { onImageClick(1) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[1])
                            .size(Int.MAX_VALUE, 90)
                            .build(),
                        contentDescription = "Journal Image 2",
                        contentScale = ContentScale.Crop,
                        imageLoader = ImageLoadUtils.getImageLoader(),
                        modifier = Modifier.fillMaxSize(),
                        clipToBounds = true
                    )
                }
                
                // 右下方两张
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
                ) {
                    // 右下左图片
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(ROUNDED_SHAPE)
                            .clickable { onImageClick(2) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[2])
                                .size(Int.MAX_VALUE, 90)
                                .build(),
                            contentDescription = "Journal Image 3",
                            contentScale = ContentScale.Crop,
                            imageLoader = ImageLoadUtils.getImageLoader(),
                            modifier = Modifier.fillMaxSize(),
                            clipToBounds = true
                        )
                    }
                    
                    // 右下右图片
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(ROUNDED_SHAPE)
                            .clickable { onImageClick(3) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[3])
                                .size(Int.MAX_VALUE, 90)
                                .build(),
                            contentDescription = "Journal Image 4",
                            contentScale = ContentScale.Crop,
                            imageLoader = ImageLoadUtils.getImageLoader(),
                            modifier = Modifier.fillMaxSize(),
                            clipToBounds = true
                        )
                    }
                }
            } else {
                // 5张及以上图片布局：右侧2x2网格
                val maxRightImages = minOf(4, images.size - 1)
                val halfHeight = 1f / 2f
                
                // 上半部分：两张图片
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(halfHeight),
                    horizontalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
                ) {
                    // 右上左
                    if (images.size > 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ROUNDED_SHAPE)
                                .clickable { onImageClick(1) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[1])
                                    .size(Int.MAX_VALUE, 90)
                                    .build(),
                                contentDescription = "Journal Image 2",
                                contentScale = ContentScale.Crop,
                                imageLoader = ImageLoadUtils.getImageLoader(),
                                modifier = Modifier.fillMaxSize(),
                                clipToBounds = true
                            )
                        }
                    }
                    
                    // 右上右
                    if (images.size > 2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ROUNDED_SHAPE)
                                .clickable { onImageClick(2) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[2])
                                    .size(Int.MAX_VALUE, 90)
                                    .build(),
                                contentDescription = "Journal Image 3",
                                contentScale = ContentScale.Crop,
                                imageLoader = ImageLoadUtils.getImageLoader(),
                                modifier = Modifier.fillMaxSize(),
                                clipToBounds = true
                            )
                        }
                    }
                }
                
                // 下半部分：两张图片
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(halfHeight),
                    horizontalArrangement = Arrangement.spacedBy(IMAGE_SPACING)
                ) {
                    // 右下左
                    if (images.size > 3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ROUNDED_SHAPE)
                                .clickable { onImageClick(3) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[3])
                                    .size(Int.MAX_VALUE, 90)
                                    .build(),
                                contentDescription = "Journal Image 4",
                                contentScale = ContentScale.Crop,
                                imageLoader = ImageLoadUtils.getImageLoader(),
                                modifier = Modifier.fillMaxSize(),
                                clipToBounds = true
                            )
                        }
                    }
                    
                    // 右下右（如果有第5张图片）
                    if (images.size > 4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ROUNDED_SHAPE)
                                .clickable { onImageClick(4) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[4])
                                    .size(Int.MAX_VALUE, 90)
                                    .build(),
                                contentDescription = "Journal Image 5",
                                contentScale = ContentScale.Crop,
                                imageLoader = ImageLoadUtils.getImageLoader(),
                                modifier = Modifier.fillMaxSize(),
                                clipToBounds = true
                            )
                            
                            // 如果有超过5张图片，显示+N标记
                            if (images.size > 5) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x80000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${images.size - 5}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageSection(
    images: MutableList<Any>?,
    onImageClick: (Int) -> Unit,
) {
    if (!images.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
                .clip(ROUNDED_SHAPE)
        ) {
            when (images.size) {
                1 -> SingleImage(images[0], onImageClick)
                2 -> TwoImages(images, onImageClick)
                3 -> ThreeImages(images, onImageClick)
                else -> MultipleImages(images, onImageClick)
            }
        }
    }
}

@Composable
fun LocationInfo(location: LocationData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = "location",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${location.name}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DateInfo(date: Date) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.access_time_24),
            contentDescription = "time",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ContentSection(
    modifier: Modifier,
    text: String?,
    location: LocationData?,
    date: Date?,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    textLayoutResult: MutableState<TextLayoutResult?>,
    isMarkdown: Boolean = false
) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        if (text != null) {
            val textStyle = MaterialTheme.typography.bodyMedium
            
            if (isMarkdown) {
                // 使用Markdown显示
                MarkdownText(
                    markdown = text,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    modifier = Modifier.padding(16.dp),
                    style = textStyle,
                )
            } else {
                // 普通文本显示 - 移除clickable修饰符，让事件传递到父级
                Text(
                    text = text,
                    style = textStyle,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                    onTextLayout = { textLayoutResult.value = it }
                )
            }
        }

        if (location != null) {
            LocationInfo(location)
        }

        if (date != null) {
            DateInfo(date)
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun JournalCard(
    modifier: Modifier, 
    journalData: JournalData,
    handleClickInternally: Boolean = false, // 控制是否在内部处理点击事件
    expandedState: Boolean? = null, // 外部控制的展开状态
    onExpandChange: ((Boolean) -> Unit)? = null // 展开状态变化回调
) {
    // 使用本地状态变量
    val expandedLocal = remember { mutableStateOf(false) }
    
    // 确定实际使用的展开状态
    val expanded = expandedState ?: expandedLocal.value
    
    // 处理展开状态变化的函数
    val toggleExpanded = {
        if (expandedState != null && onExpandChange != null) {
            // 如果有外部控制，调用外部回调
            onExpandChange(!expanded)
        } else {
            // 否则更新本地状态
            expandedLocal.value = !expandedLocal.value
        }
    }
    
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // 添加图片预览状态
    val (showImagePreview, setShowImagePreview) = remember { mutableStateOf(false) }
    val (selectedImageIndex, setSelectedImageIndex) = remember { mutableIntStateOf(0) }
    
    // 准备卡片的Modifier
    val cardModifier = if (handleClickInternally) {
        // 当内部处理点击时，添加clickable修饰符
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(
                remember { MutableInteractionSource() },
                indication = null  // 移除点击动画效果
            ) {
                // 点击卡片时切换展开状态
                toggleExpanded()
            }
    } else {
        // 否则，仅使用传入的modifier和基本padding
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    }
    
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = cardModifier
    ) {
        Column {
            // 图片区域
            ImageSection(
                images = journalData.images,
                onImageClick = { index ->
                    setSelectedImageIndex(index)
                    setShowImagePreview(true)
                }
            )

            // 内容区域
            ContentSection(
                modifier = modifier,
                text = journalData.text,
                location = journalData.location,
                date = journalData.date,
                expanded = expanded,
                onExpandClick = { toggleExpanded() },
                textLayoutResult = textLayoutResult,
                isMarkdown = journalData.isMarkdown
            )
        }
    }

    // 显示图片预览
    if (showImagePreview && !journalData.images.isNullOrEmpty()) {
        ImageGalleryPreview(
            images = journalData.images,
            initialIndex = selectedImageIndex,
            onDismiss = { setShowImagePreview(false) }
        )
    }
}

