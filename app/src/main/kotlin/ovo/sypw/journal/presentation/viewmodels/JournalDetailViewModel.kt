package ovo.sypw.journal.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.repository.JournalRepository
import javax.inject.Inject

/**
 * 日记详情视图模型
 */
@HiltViewModel
class JournalDetailViewModel @Inject constructor(
    private val repository: JournalRepository
) : ViewModel() {

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
                repository.updateJournal(journal)
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