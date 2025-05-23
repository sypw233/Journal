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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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


@Composable
fun SingleImage(image: Any, onImageClick: (Int) -> Unit) {

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image)
            .size(Int.MAX_VALUE, 180)
            .build(),
        contentDescription = "Journal Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onImageClick(0) },
        clipToBounds = true
    )
}

@Composable
fun TwoImages(images: MutableList<Any>, onImageClick: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 2.dp)
                .clip(RoundedCornerShape(12.dp))
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
                .padding(start = 2.dp)
                .clip(RoundedCornerShape(12.dp))
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
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(end = 4.dp)
                .clip(RoundedCornerShape(12.dp))
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

        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(1) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[1])
                        .size(Int.MAX_VALUE, 180)
                        .build(),
                    imageLoader = ImageLoadUtils.getImageLoader(),
                    contentDescription = "Journal Image 2",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    clipToBounds = true
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxWidth()
                    .clip(ROUNDED_SHAPE)
                    .clickable { onImageClick(2) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[2])
                        .size(Int.MAX_VALUE, 180)
                        .build(),
                    imageLoader = ImageLoadUtils.getImageLoader(),
                    contentDescription = "Journal Image 3",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    clipToBounds = true
                )
            }
        }
    }
}

@Composable
fun MultipleImages(images: MutableList<Any>, onImageClick: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(end = 4.dp)
                .clip(ROUNDED_SHAPE)
                .clickable { onImageClick(0) }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(images[0])
                    .size(Int.MAX_VALUE, 180)
                    .build(),
                imageLoader = ImageLoadUtils.getImageLoader(),
                contentDescription = "Journal Image 1",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        }

        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val maxRightImages = minOf(3, images.size - 1)
            for (i in 1..maxRightImages) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(ROUNDED_SHAPE)
                        .clickable { onImageClick(i) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[i])
                            .size(Int.MAX_VALUE, 180)
                            .build(),
                        imageLoader = ImageLoadUtils.getImageLoader(),
                        contentDescription = "Journal Image ${i + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        clipToBounds = true
                    )

                    if (i == maxRightImages && images.size > maxRightImages + 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x80000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${images.size - maxRightImages - 1}",
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
//    onExpandClick: () -> Unit,
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
                // 普通文本显示
                Text(
                    text = text,
                    style = textStyle,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(16.dp),
//                    .clickable(onClick = onExpandClick),
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
fun JournalCard(modifier: Modifier, journalData: JournalData) {
    // 控制文本展开状态
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // 添加图片预览状态
    val (showImagePreview, setShowImagePreview) = remember { mutableStateOf(false) }
    val (selectedImageIndex, setSelectedImageIndex) = remember { mutableIntStateOf(0) }
//    val journalDataRem = remember { mutableStateOf(journalData) }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(
                remember { MutableInteractionSource() },
                indication = null
            ) {
                setExpanded(!expanded)
            }
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
//                onExpandClick = { setExpanded(!expanded) },
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

