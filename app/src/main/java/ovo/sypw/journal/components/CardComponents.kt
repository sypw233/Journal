package ovo.sypw.journal.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
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
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import ovo.sypw.journal.R
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.utils.ImageLoadUtils
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Card高度
 * DEFAULT_HEIGHT 同时有图片和文字(3行)下的默认高度
 * 在有图片时默认最多显示3行文字
 * ONLY_IMAGE_HEIGHT 只有图片的高度
 * MIN_HEIGHT 只有1行文字时的高度
 */
private val CardHeight = {
    val DEFAULT_HEIGHT = 240.dp
}

/** 普通卡片组件 */
@Composable
fun ElevatedCard(showText: String) {
    ElevatedCard(
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(240.dp)
    ) { Text(text = showText, modifier = Modifier.padding(15.dp)) }
}

@Composable
fun JournalCard(journalData: JournalData) {
    // 控制文本展开状态
    val maxLines = 6
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // 添加图片预览状态
    val (showImagePreview, setShowImagePreview) = remember { mutableStateOf(false) }
    val (selectedImageIndex, setSelectedImageIndex) = remember { mutableStateOf(0) }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(
                enabled = true,
//                enabled = journalData.text != null &&
//                         (textLayoutResult.value?.lineCount ?: 0) > maxLines,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                setExpanded(!expanded)
            }
    ) {
        Column {
            // 图片区域 - 固定高度
            if (journalData.images != null && journalData.images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (journalData.images.size == 1) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(journalData.images[0])
                                .precision(Precision.EXACT)
                                .crossfade(true)
                                .build(),
                            imageLoader = ImageLoadUtils.getImageLoader(),
                            contentDescription = "Journal Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    setSelectedImageIndex(0)
                                    setShowImagePreview(true)
                                },
                            clipToBounds = true,
                            onState = { state ->
                                when (state) {
                                    is AsyncImagePainter.State.Loading -> {
                                        // 可以添加加载占位图
                                    }

                                    is AsyncImagePainter.State.Error -> {
                                        // 可以添加错误占位图
                                    }

                                    else -> {}
                                }
                            }
                        )
                    } else if (journalData.images.size == 2) {
                        // 两张图片并排显示
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 左侧图片
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        setSelectedImageIndex(0)
                                        setShowImagePreview(true)
                                    }
                            ) {
                                AsyncImage(
                                    model = journalData.images[0],
                                    contentDescription = "Journal Image 1",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    clipToBounds = true
                                )
                            }

                            // 右侧图片
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        setSelectedImageIndex(1)
                                        setShowImagePreview(true)
                                    }
                            ) {
                                AsyncImage(
                                    model = journalData.images[1],
                                    contentDescription = "Journal Image 2",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    clipToBounds = true
                                )
                            }
                        }
                    } else if (journalData.images.size == 3) {
                        // 三张图片：右侧一张大图在上方占一半高度，两张小图在下方并排
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 左侧大图
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight()
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        setSelectedImageIndex(0)
                                        setShowImagePreview(true)
                                    }
                            ) {
                                AsyncImage(
                                    model = journalData.images[0],
                                    contentDescription = "Journal Image 1",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    clipToBounds = true
                                )
                            }

                            // 右侧布局：上方一张大图，下方两张小图并排
                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 上方大图
                                Box(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            setSelectedImageIndex(1)
                                            setShowImagePreview(true)
                                        }
                                ) {
                                    AsyncImage(
                                        model = journalData.images[1],
                                        contentDescription = "Journal Image 2",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        clipToBounds = true
                                    )
                                }

                                // 下方小图
                                Box(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            setSelectedImageIndex(2)
                                            setShowImagePreview(true)
                                        }
                                ) {
                                    AsyncImage(
                                        model = journalData.images[2],
                                        contentDescription = "Journal Image 3",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        clipToBounds = true
                                    )
                                }
                            }
                        }
                    } else {
                        // 超过3张图片的情况
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 左侧大图
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight()
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        setSelectedImageIndex(0)
                                        setShowImagePreview(true)
                                    }
                            ) {
                                AsyncImage(
                                    model = journalData.images[0],
                                    contentDescription = "Journal Image 1",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    clipToBounds = true
                                )
                            }

                            // 右侧小图列表
                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 最多显示3张小图
                                val maxRightImages = minOf(3, journalData.images.size - 1)
                                for (i in 1..maxRightImages) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                setSelectedImageIndex(i)
                                                setShowImagePreview(true)
                                            }
                                    ) {
                                        AsyncImage(
                                            model = journalData.images[i],
                                            contentDescription = "Journal Image ${i + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                            clipToBounds = true
                                        )

                                        // 如果还有更多图片未显示，在最后一张上显示+N
                                        if (i == maxRightImages && journalData.images.size > maxRightImages + 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0x80000000))
                                                    .clickable {
                                                        setSelectedImageIndex(i)
                                                        setShowImagePreview(true)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+${journalData.images.size - maxRightImages - 1}",
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

            // 文字区域
            if (journalData.text != null) {
                val textStyle = MaterialTheme.typography.bodyMedium

                Text(
                    text = journalData.text,
                    style = textStyle,
                    maxLines = if (expanded) Int.MAX_VALUE else maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                    onTextLayout = { textLayoutResult.value = it }
                )
            }

            // 位置信息
            if (journalData.location != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // 位置信息内容保持不变
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${journalData.location.latitude}, ${journalData.location.longitude}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 日期信息保持不变
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
                Text(
                    text = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(journalData.date),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }

    // 显示图片预览
    if (showImagePreview && journalData.images != null && journalData.images.isNotEmpty()) {
        ImageGalleryPreview(
            images = journalData.images,
            initialIndex = selectedImageIndex,
            onDismiss = { setShowImagePreview(false) }
        )
    }
}

/** 可滑动卡片组件，支持左右滑动进行标记和删除操作 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(
//    showText: String,
    journalData: JournalData,
    onDismiss: () -> Unit,
    onMark: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentProgress = remember { mutableFloatStateOf(0f) }
    // 创建滑动状态
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                // 当滑动到END位置时触发删除
                if (value == SwipeToDismissBoxValue.EndToStart
                    && currentProgress.floatValue >= 0.5f && currentProgress.floatValue <= 1f
                ) {
                    onDismiss()
                    false // 返回false防止状态更新为EndToStart
                    // 在这里直接调用onDismiss并返回false，防止状态变为EndToStart

                } else if (value == SwipeToDismissBoxValue.StartToEnd
                    && currentProgress.floatValue >= 0.5f && currentProgress.floatValue <= 1f
                ) {
                    onMark()
                    false
                }
                false
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.4f }
        )
    ForUpdateData {
        currentProgress.floatValue = dismissState.progress
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true, // 允许从左向右滑动
        enableDismissFromEndToStart = true, // 允许从右向左滑动
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 25.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.bookmark_border_24),
                    contentDescription = "mark",
                    modifier = Modifier
                        .offset((-150).dp)
                        .padding(start = 16.dp)
                        .size(30.dp),
                )
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "delete",
                    modifier = Modifier
                        .offset(150.dp)
                        .padding(end = 16.dp)
                        .size(30.dp),
                )
            }
        },
        modifier = modifier.animateContentSize()
    ) {
        // 卡片内容
//        ElevatedCard(showText)
        JournalCard(journalData)
    }
}

@Composable
private fun ForUpdateData(onUpdate: () -> Unit) {
    onUpdate()
}