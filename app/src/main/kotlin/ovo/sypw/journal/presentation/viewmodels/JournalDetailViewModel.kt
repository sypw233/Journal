package ovo.sypw.journal.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.data.repository.SentimentRepository
import javax.inject.Inject

/**
 * 日记详情视图模型
 */
@HiltViewModel
class JournalDetailViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val sentimentRepository: SentimentRepository
) : ViewModel() {

    private val TAG = "JournalDetailViewModel"
    private val _journal = MutableStateFlow<JournalData?>(null)
    val journal: StateFlow<JournalData?> = _journal.asStateFlow()

    /**
     * 加载日记详情
     */
    fun loadJournal(id: Int) {
        viewModelScope.launch {
            _journal.value = repository.getJournalById(id)
        }
    }

    /**
     * 保存日记
     */
    fun saveJournal(journal: JournalData) {
        viewModelScope.launch {
            if (journal.id == 0) {
                repository.insertJournal(journal)
            } else {
                // 获取原始日记内容
                val originalJournal = repository.getJournalById(journal.id)

                // 更新日记
                repository.updateJournal(journal)

                // 如果文本内容发生变化，删除对应的情感分析结果
                if (originalJournal != null && originalJournal.text != journal.text) {
                    try {
                        Log.d(TAG, "日记内容已变更，删除对应的情感分析结果: journalId=${journal.id}")
                        sentimentRepository.deleteSentimentByJournalId(journal.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "删除情感分析结果失败", e)
                        // 不影响主流程，继续执行
                    }
                }
            }
        }
    }

    /**
     * 删除日记
     */
    fun deleteJournal(journal: JournalData) {
        viewModelScope.launch {
            repository.deleteJournal(journal)
        }
    }
} 