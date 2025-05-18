package ovo.sypw.journal.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ovo.sypw.journal.common.utils.AutoSaveManager
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.LocationData
import ovo.sypw.journal.presentation.components.JournalEditContent
import ovo.sypw.journal.presentation.viewmodels.JournalEditViewModel
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel
import java.util.Date
import javax.inject.Inject

/**
 * 日记编辑界面
 * 支持编辑现有日记
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditScreen(
    journalData: JournalData,
    onSave: (JournalData) -> Unit,
    onCancel: () -> Unit,
    viewModel: JournalListViewModel? = null,
    editViewModel: JournalEditViewModel = hiltViewModel()
) {
    // 处理返回按钮
    BackHandler {
        onCancel()
    }
    
    // 创建可变状态来存储编辑中的数据
    var editedText by remember { mutableStateOf(journalData.text) }
    var editedDate by remember { mutableStateOf(journalData.date) }
    var editedLocationName by remember { mutableStateOf(journalData.location?.name ?: "") }
    var editedLocationData by remember { mutableStateOf(journalData.location) }
    var editedImages by remember { mutableStateOf(journalData.images ?: mutableListOf()) }
    
    // 保存状态
    var isSaving by remember { mutableStateOf(false) }
    
    // 获取生命周期所有者
    val lifecycleOwner = LocalLifecycleOwner.current
    val autoSaveEnabled by editViewModel.autoSaveEnabled.collectAsStateWithLifecycle(initialValue = false)
    
    // 自动保存功能
    DisposableEffect(lifecycleOwner) {
        if (autoSaveEnabled) {
            // 配置自动保存
            editViewModel.startAutoSave(lifecycleOwner) {
                // 构建更新后的日记数据
                val updatedJournal = JournalData(
                    id = journalData.id,
                    isMarkdown = journalData.isMarkdown,
                    date = editedDate,
                    text = editedText,
                    images = editedImages,
                    location = editedLocationData
                        ?: (if (editedLocationName.isNotEmpty()) LocationData(name = editedLocationName) else null)
                )
                
                // 保存更新后的日记
                onSave(updatedJournal)
            }
        }
        
        // 清理
        onDispose {
            editViewModel.stopAutoSave()
        }
    }
    
    // 如果保存成功，关闭界面
    LaunchedEffect(isSaving) {
        if (isSaving) {
            // 构建更新后的日记数据
            val updatedJournal = JournalData(
                id = journalData.id,
                isMarkdown = journalData.isMarkdown,
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
            onCancel()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "编辑日记",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onCancel() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 触发保存操作
                        isSaving = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Transparent)
                .imePadding()
        ) {
            // 使用一个具有状态更新回调的JournalEditContent
            JournalEditContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                initialJournalData = journalData,
                onSave = { updatedJournal ->
                    // 更新本地状态
                    editedText = updatedJournal.text
                    editedDate = updatedJournal.date
                    editedLocationName = updatedJournal.location?.name ?: ""
                    editedLocationData = updatedJournal.location
                    editedImages = updatedJournal.images ?: mutableListOf()
                    
                    // 如果底部保存按钮被点击，则直接保存
                    onSave(updatedJournal)
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