package ovo.sypw.journal.presentation.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.jeziellago.compose.markdowntext.MarkdownText
import ovo.sypw.journal.R
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.PermissionUtils
import ovo.sypw.journal.common.utils.RequestPermissions
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Build
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material3.IconToggleButton
import ovo.sypw.journal.common.utils.ImageUriUtils
import ovo.sypw.journal.common.utils.ImagePickerUtils
import ovo.sypw.journal.di.AppDependencyManager
import ovo.sypw.journal.JournalApplication
import androidx.compose.ui.graphics.Color

/**
 * 日记编辑内容组件
 * 用于编辑现有日记或创建新日记
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun JournalEditContent(
    modifier: Modifier = Modifier,
    initialJournalData: JournalData? = null,
    onSave: (JournalData) -> Unit,
    onTextChanged: ((String) -> Unit)? = null,
    onDateChanged: ((Date) -> Unit)? = null,
    onLocationChanged: ((String, LocationData?) -> Unit)? = null,
    onImagesChanged: ((MutableList<Any>) -> Unit)? = null,
    onIsMarkdownChanged: ((Boolean) -> Unit)? = null,
    showSaveButton: Boolean = true
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // 获取依赖管理器
    val dependencyManager = remember {
        (context.applicationContext as JournalApplication).dependencyManager
    }

    // 创建可变状态来存储编辑中的数据
    var journalText by remember { mutableStateOf(initialJournalData?.text ?: "") }
    var journalDate by remember { mutableStateOf(initialJournalData?.date ?: Date()) }
    var locationName by remember { mutableStateOf(initialJournalData?.location?.name ?: "") }
    var locationData by remember { mutableStateOf(initialJournalData?.location) }
    var isMarkdown by remember { mutableStateOf(initialJournalData?.isMarkdown == true) }
    var showMarkdownPreview by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var showExpandedPreview by remember { mutableStateOf(isMarkdown) }
    var previewRatioMode by remember { mutableStateOf(0) } // 0: 60/40, 1: 50/50, 2: 40/60
    var showFullScreenEditor by remember { mutableStateOf(false) }

    // AI写作对话框状态
    var showAIWritingDialog by remember { mutableStateOf(false) }

    // 监听Markdown状态变化，确保非Markdown模式下不显示预览
    LaunchedEffect(isMarkdown) {
        if (!isMarkdown) {
            showExpandedPreview = false
            showMarkdownPreview = false
        }
    }

    // 格式化日期显示
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE) }
    val formattedDate = remember(journalDate) { dateFormat.format(journalDate) }

    // 动画比例值
    val editRatio by animateFloatAsState(
        targetValue = when (previewRatioMode) {
            0 -> 0.6f
            1 -> 0.5f
            else -> 0.4f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "editRatio"
    )

    val previewRatio by animateFloatAsState(
        targetValue = when (previewRatioMode) {
            0 -> 0.4f
            1 -> 0.5f
            else -> 0.6f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "previewRatio"
    )


    val selectedImages = remember {
        mutableStateListOf<Any>().apply {
            initialJournalData?.images?.let { addAll(it) }
        }
    }

    // 动画高度 - 展开时使用屏幕高度的80%
    val screenHeight = configuration.screenHeightDp
    val textFieldHeight by animateFloatAsState(
        targetValue = if (isExpanded) screenHeight * 0.8f else 180f,
        animationSpec = tween(300),
        label = "textFieldHeight"
    )

    // 初始化日记数据
    LaunchedEffect(initialJournalData) {
        journalText = initialJournalData?.text ?: ""
        journalDate = initialJournalData?.date ?: Date()
        locationName = initialJournalData?.location?.name ?: ""
        locationData = initialJournalData?.location
        isMarkdown = initialJournalData?.isMarkdown == true
        selectedImages.clear()
        initialJournalData?.images?.let { selectedImages.addAll(it) }
    }

    // 当数据变化时，通知父组件
    LaunchedEffect(journalText) {
        onTextChanged?.invoke(journalText)
    }

    LaunchedEffect(journalDate) {
        onDateChanged?.invoke(journalDate)
    }

    LaunchedEffect(locationName, locationData) {
        onLocationChanged?.invoke(locationName, locationData)
    }

    LaunchedEffect(selectedImages.size) {
        onImagesChanged?.invoke(selectedImages.toMutableList())
    }
    LaunchedEffect(isMarkdown) {
        onIsMarkdownChanged?.invoke(isMarkdown)
    }

    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = journalDate.time)

    // 地图选择器状态
    var showMapPicker by remember { mutableStateOf(false) }

    // 使用新的ImagePickerUtils处理图片选择
    val imagePickerLauncher = ImagePickerUtils.rememberImagePicker(
        onImagesPicked = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                // 获取持久化URI权限，确保应用重启后仍能访问图片
                var successCount = 0
                uris.forEach { uri ->
                    try {
                        // 添加到选中图片列表
                        selectedImages.add(uri)
                        successCount++
                    } catch (e: Exception) {
                        Log.e("JournalEditContent", "处理图片URI错误", e)
                        SnackBarUtils.showSnackBar("无法获取某些图片的持久访问权限: ${e.message}")
                    }
                }
                if (successCount > 0) {
                    SnackBarUtils.showSnackBar("已添加${successCount}张图片")
                }
            }
        }
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 只在非展开模式下显示日期选择
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            // 日期选择
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDatePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "选择日期",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        // 只在非展开模式下显示位置选择
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            // 位置选择
            OutlinedTextField(
                value = locationName,
                onValueChange = {
                    locationName = it
                    // 位置名称变更时，重置位置数据
                    if (locationData != null && locationData?.name != it) {
                        locationData = if (it.isNotEmpty()) LocationData(name = it) else null
                    }
                },
                label = { Text("位置") },
                placeholder = { Text("添加位置...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "位置图标",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (locationName.isNotEmpty()) {
                        IconButton(onClick = {
                            locationName = ""
                            locationData = null
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "清除位置",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row {
                            // 地图选择位置按钮
                            IconButton(
                                onClick = { showMapPicker = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = "地图选择",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // 获取当前位置按钮
                            IconButton(
                                onClick = {
                                    // 检查定位权限
                                    if (PermissionUtils.hasPermissions(
                                            context,
                                            PermissionUtils.LOCATION_PERMISSIONS
                                        )
                                    ) {
                                        // 已有权限，直接获取位置
                                        AMapLocationUtils.getCurrentLocation(
                                            context = context,
                                            onSuccess = { location ->
                                                locationName = location.name ?: ""
                                                locationData = location
//                                                SnackBarUtils.showSnackBar("已获取当前位置")
                                            },
                                            onError = { errorMsg ->
                                                SnackBarUtils.showSnackBar("获取位置失败: $errorMsg")
                                            }
                                        )
                                    } else {
                                        // 请求权限
                                        SnackBarUtils.showSnackBar("需要定位权限才能获取当前位置")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = "获取位置",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )
        }

        // 权限请求组件
        RequestPermissions(
            permissions = PermissionUtils.LOCATION_PERMISSIONS,
            onPermissionResult = { granted ->
                if (granted) {
                    // 权限已授予，可以获取位置
//                    SnackBarUtils.showSnackBar("已获取定位权限")
                } else {
                    // 权限被拒绝
                    SnackBarUtils.showSnackBar("无法获取定位权限")
                }
            }
        )

        // 文字内容
        Column(modifier = Modifier.fillMaxWidth()) {
            // 通用顶部按钮行 - 在所有模式下都使用同一个布局
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI写作按钮
                IconButton(
                    onClick = { showAIWritingDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Create,
                        contentDescription = "AI写作助手",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 预览切换按钮 - 仅在展开模式且启用Markdown时显示
                if (isExpanded && isMarkdown) {
                    IconButton(
                        onClick = { showExpandedPreview = !showExpandedPreview },
                        modifier = Modifier.size(40.dp)
                    ) {
                        // 使用AnimatedContent为图标添加旋转过渡效果
                        AnimatedContent(
                            targetState = showExpandedPreview,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(150))
                            },
                            label = "PreviewIcon"
                        ) { isPreviewVisible ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 为图标添加脉动动画（当预览隐藏时）
                                val infiniteTransition =
                                    rememberInfiniteTransition(label = "ButtonPulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (!isPreviewVisible) 1.2f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "PulseScale"
                                )

                                Icon(
                                    imageVector = if (isPreviewVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (isPreviewVisible) "隐藏预览" else "显示预览",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 比例切换按钮 - 仅在展开模式且启用Markdown且显示预览时可用
                    if (showExpandedPreview) {
                        // 添加轻微的旋转动画效果
                        val infiniteTransition =
                            rememberInfiniteTransition(label = "RatioButtonRotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = -2f,
                            targetValue = 2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Rotation"
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                // 循环切换三种比例模式
                                previewRatioMode = (previewRatioMode + 1) % 3
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AspectRatio,
                                contentDescription = "调整比例",
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotation
                                },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Markdown切换按钮 - 所有模式下都显示
                IconToggleButton(
                    checked = isMarkdown,
                    onCheckedChange = { 
                        isMarkdown = it
                        onIsMarkdownChanged?.invoke(it)
                        if (!it) {
                            // 如果切换到非Markdown模式，关闭预览
                            showExpandedPreview = false
                            showMarkdownPreview = false
                        }
                    },
                    modifier = Modifier
                        .background(
                            color = if (isMarkdown) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        contentDescription = if (isMarkdown) "关闭Markdown" else "启用Markdown",
                        tint = if (isMarkdown) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // 保存按钮 - 仅在展开模式下显示
                if (isExpanded && showSaveButton) {
                    TextButton(
                        onClick = {
                            // 创建新的日记对象，保留原始ID
                            val newJournal = JournalData(
                                id = initialJournalData?.id ?: 0,
                                date = journalDate,
                                text = journalText,
                                images = selectedImages.toMutableList(),
                                location = locationData
                                    ?: (if (locationName.isNotEmpty()) LocationData(name = locationName) else null),
                                isMarkdown = isMarkdown
                            )
                            onSave(newJournal)
                            SnackBarUtils.showSnackBar("日记已保存")
                            isExpanded = false
                        }
                    ) {
                        Text("保存")
                    }
                }

                // 展开/收起按钮
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 内容编辑区域 - 根据状态显示不同的编辑界面
            if (isExpanded) {
                // 展开模式
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textFieldHeight.dp)
                        .padding(bottom = 4.dp)
                ) {
                    // 使用AnimatedContent为整个编辑区域添加过渡动画
                    AnimatedContent(
                        targetState = if(isMarkdown) showExpandedPreview else false, // 非Markdown模式下强制关闭预览
                        transitionSpec = {
                            fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "PreviewToggle",
                        modifier = Modifier.weight(1f)
                    ) { showPreview ->
                        if (showPreview && isMarkdown) {
                            // 分屏模式：上下显示编辑区和预览区
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                            ) {
                                // 编辑区域
                                OutlinedTextField(
                                    value = journalText,
                                    onValueChange = { journalText = it },
                                    placeholder = { Text("写下今天的故事...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(editRatio),
                                    shape = RoundedCornerShape(12.dp),
                                )

                                // 分隔线
                                Spacer(modifier = Modifier.height(8.dp))

                                // 预览区域
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(previewRatio)
                                        .animateContentSize(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = "Markdown预览",
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        MarkdownText(
                                            markdown = journalText,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            // 只显示编辑区域（使用fillMaxHeight确保占满全屏空间）
                            OutlinedTextField(
                                value = journalText,
                                onValueChange = { journalText = it },
                                placeholder = { Text("写下今天的故事...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }
                }
            } else {
                // 非展开模式
                OutlinedTextField(
                    value = journalText,
                    onValueChange = { journalText = it },
                    label = { Text("内容") },
                    placeholder = { Text("写下今天的故事...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textFieldHeight.dp)
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        // 其余内容只在非展开模式下显示
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Markdown预览 - 只在isMarkdown为true且内容不为空时显示
                AnimatedVisibility(
                    visible = isMarkdown && journalText.isNotEmpty() && showMarkdownPreview,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Markdown预览",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            MarkdownText(
                                markdown = journalText,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Markdown预览切换按钮 - 只在isMarkdown为true且内容不为空时显示
                if (isMarkdown && journalText.isNotEmpty()) {
                    TextButton(
                        onClick = { showMarkdownPreview = !showMarkdownPreview },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showMarkdownPreview) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showMarkdownPreview) "隐藏预览" else "显示预览",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showMarkdownPreview) "隐藏预览" else "显示预览")
                        }
                    }
                }

                // 图片选择
                if (selectedImages.isEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            // 使用新的图片选择器，不需要传递MIME类型数组
                            imagePickerLauncher.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "添加图片",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加图片")
                    }
                }

                // 已选图片预览
                AnimatedVisibility(
                    visible = selectedImages.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已添加 ${selectedImages.size} 张图片",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { imagePickerLauncher.invoke() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = "添加更多图片",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            items(selectedImages) { image ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(120.dp)
                                ) {
                                    // 图片预览
                                    AsyncImage(
                                        model = image,
                                        contentDescription = "已选图片",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )

                                    // 删除按钮
                                    IconButton(
                                        onClick = { selectedImages.remove(image) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "删除图片",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 保存按钮，仅在showSaveButton为true时显示
                if (showSaveButton) {
                    ElevatedButton(
                        onClick = {
                            // 创建新的日记对象，保留原始ID
                            val newJournal = JournalData(
                                id = initialJournalData?.id ?: 0,
                                date = journalDate,
                                text = journalText,
                                images = selectedImages.toMutableList(),
                                location = locationData
                                    ?: (if (locationName.isNotEmpty()) LocationData(name = locationName) else null),
                                isMarkdown = isMarkdown
                            )
                            onSave(newJournal)
                            SnackBarUtils.showSnackBar("日记已保存")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        journalDate = Date(it)
                    }
                    showDatePicker = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                // 设置为false，确保对话框不超出屏幕边界，修复被遮挡问题
                usePlatformDefaultWidth = false
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
//                title = { Text("选择日期", style = MaterialTheme.typography.titleMedium) },
//                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }

    // 地图选择器对话框
    if (showMapPicker) {
        MapPickerDialog(
            isVisible = true,
            initialLocation = locationData,
            onLocationSelected = { selectedLocation ->
                locationName = selectedLocation.name ?: ""
                locationData = selectedLocation
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }

    // 全屏编辑器对话框（已不再使用，但保留代码以防需要）
    if (false) { // 将条件改为false，禁用全屏对话框
        FullScreenTextEditor(
            isVisible = showFullScreenEditor,
            initialText = journalText,
            isMarkdown = isMarkdown,
            onTextChanged = { journalText = it },
            onDismiss = { showFullScreenEditor = false }
        )
    }

    // AI写作对话框
    if (showAIWritingDialog) {
        AIWritingDialog(
            isVisible = true,
            onDismiss = { showAIWritingDialog = false },
            onContentGenerated = { generatedContent ->
                // 将生成的内容添加到现有文本中
                journalText = if (journalText.isBlank()) {
                    generatedContent
                } else {
                    "$journalText\n\n$generatedContent"
                }
                showAIWritingDialog = false
                SnackBarUtils.showSnackBar("AI创作内容已添加到编辑器")
            },
            useMarkdown = isMarkdown,
            dependencyManager = dependencyManager
        )
    }
} 