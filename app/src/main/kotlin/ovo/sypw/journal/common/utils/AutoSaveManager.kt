package ovo.sypw.journal.common.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.JournalPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动保存管理器
 * 用于处理日记编辑时的自动保存功能
 */
@Singleton
class AutoSaveManager @Inject constructor(
    private val preferences: JournalPreferences
) : DefaultLifecycleObserver {
    
    private var autoSaveJob: Job? = null
    private var saveCallback: (suspend () -> Unit)? = null
    
    /**
     * 开始自动保存任务
     * @param lifecycleOwner 生命周期拥有者，通常是一个Activity或Fragment
     * @param onSave 保存操作的回调函数
     */
    fun startAutoSave(lifecycleOwner: LifecycleOwner, onSave: suspend () -> Unit) {
        // 如果自动保存设置被禁用，直接返回
        if (!preferences.isAutoSaveEnabled()) {
            return
        }
        
        // 取消之前的自动保存任务
        stopAutoSave()
        
        // 保存回调
        saveCallback = onSave
        
        // 获取自动保存间隔（分钟）
        val interval = preferences.getAutoSaveInterval().toLong() * 60 * 1000
        
        // 创建新的自动保存任务
        autoSaveJob = lifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(interval)
                if (preferences.isAutoSaveEnabled()) {
                    try {
                        onSave()
                        SnackBarUtils.showSnackBar("草稿已自动保存")
                    } catch (e: Exception) {
                        SnackBarUtils.showSnackBar("自动保存失败: ${e.message}")
                    }
                }
            }
        }
    }
    
    /**
     * 停止自动保存任务
     */
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }
    
    /**
     * 立即执行保存
     */
    fun saveNow() {
        saveCallback?.let {
            SnackBarUtils.getCoroutineScope()?.launch {
                try {
                    it()
                    SnackBarUtils.showSnackBar("内容已保存")
                } catch (e: Exception) {
                    SnackBarUtils.showSnackBar("保存失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 生命周期事件：当生命周期拥有者被销毁时取消自动保存
     */
    override fun onDestroy(owner: LifecycleOwner) {
        stopAutoSave()
        super.onDestroy(owner)
    }
} 