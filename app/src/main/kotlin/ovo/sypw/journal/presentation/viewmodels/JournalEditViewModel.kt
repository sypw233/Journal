package ovo.sypw.journal.presentation.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.AutoSaveManager
import ovo.sypw.journal.data.JournalPreferences
import javax.inject.Inject

/**
 * 日记编辑ViewModel
 * 处理日记编辑界面的业务逻辑，包括自动保存等功能
 */
@HiltViewModel
class JournalEditViewModel @Inject constructor(
    private val preferences: JournalPreferences,
    private val autoSaveManager: AutoSaveManager
) : ViewModel() {
    
    // 自动保存是否启用
    val autoSaveEnabled: StateFlow<Boolean> = MutableStateFlow(preferences.isAutoSaveEnabled()).asStateFlow()
    
    /**
     * 开始自动保存
     */
    fun startAutoSave(lifecycleOwner: LifecycleOwner, onSave: suspend () -> Unit) {
        autoSaveManager.startAutoSave(lifecycleOwner, onSave)
    }
    
    /**
     * 停止自动保存
     */
    fun stopAutoSave() {
        autoSaveManager.stopAutoSave()
    }
    
    /**
     * 立即保存
     */
    fun saveNow() {
        autoSaveManager.saveNow()
    }
} 