/**
 * MVVM架构实践示例
 * 本示例展示了如何在重组后的MVVM架构中实现日记功能
 */

// 1. Domain Layer 领域层 - 模型和接口

// 领域模型 - app/src/main/kotlin/ovo/sypw/journal/domain/models/Journal.kt
package ovo.sypw.journal.domain.models

import java.util.Date

// 领域模型应该是纯数据类，不依赖于Android框架或第三方库
data class Journal(
    val id: Int = 0,
    val text: String,
    val date: Date,
    val location: Location? = null,
    val images: List<String> = emptyList(),
    val isMark: Boolean = false
)

data class Location(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

// 领域层仓库接口 - app/src/main/kotlin/ovo/sypw/journal/domain/repositories/JournalRepository.kt
package ovo.sypw.journal.domain.repositories

import kotlinx.coroutines.flow.Flow
import ovo.sypw.journal.domain.models.Journal

// 定义仓库接口，不包含实现细节
interface JournalRepository {
    suspend fun getJournalById(id: Int): Journal?
    suspend fun getJournals(): Flow<List<Journal>>
    suspend fun getJournalsPaged(offset: Int, limit: Int): List<Journal>
    suspend fun insertJournal(journal: Journal): Long
    suspend fun updateJournal(journal: Journal)
    suspend fun deleteJournal(id: Int)
    suspend fun syncWithServer(): Result<Int> // 返回同步的条目数
}

// 用例 - app/src/main/kotlin/ovo/sypw/journal/domain/usecases/GetJournalsUseCase.kt
package ovo.sypw.journal.domain.usecases

import kotlinx.coroutines.flow.Flow
import ovo.sypw.journal.domain.models.Journal
import ovo.sypw.journal.domain.repositories.JournalRepository
import javax.inject.Inject

// 用例类应该只有一个公共方法，表示一个业务操作
class GetJournalsUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    // 这是主要方法，可以有参数
    suspend operator fun invoke(offset: Int, limit: Int): List<Journal> {
        return repository.getJournalsPaged(offset, limit)
    }
}

// 2. Data Layer 数据层 - 实现

// 数据模型 - app/src/main/kotlin/ovo/sypw/journal/data/local/database/JournalEntity.kt
package ovo.sypw.journal.data .local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val date: Date,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val imagesJson: String?, // 存储为JSON字符串
    val isMark: Boolean,
    val syncStatus: Int = 0 // 0: 已同步, 1: 待上传, 2: 待更新, 3: 待删除
)

// 数据层DAO - app/src/main/kotlin/ovo/sypw/journal/data/local/database/JournalDao.kt
package ovo.sypw.journal.data .local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: Int): JournalEntity?

    @Query("SELECT * FROM journals ORDER BY date DESC")
    fun getJournals(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journals ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getJournalsPaged(offset: Int, limit: Int): List<JournalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journalEntity: JournalEntity): Long

    @Update
    suspend fun updateJournal(journalEntity: JournalEntity)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournal(id: Int)
}

// 仓库实现 - app/src/main/kotlin/ovo/sypw/journal/data/repositories/JournalRepositoryImpl.kt
package ovo.sypw.journal.data .repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ovo.sypw.journal.data.local.database.JournalDao
import ovo.sypw.journal.data.remote.api.EntryService
import ovo.sypw.journal.data.remote.models.EntryRequest
import ovo.sypw.journal.domain.models.Journal
import ovo.sypw.journal.domain.models.Location
import ovo.sypw.journal.domain.repositories.JournalRepository
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalDao: JournalDao,
    private val entryService: EntryService
) : JournalRepository {

    // 实现接口方法，协调本地数据源和远程数据源
    override suspend fun getJournalById(id: Int): Journal? {
        return journalDao.getJournalById(id)?.toDomainModel()
    }

    override suspend fun getJournals(): Flow<List<Journal>> {
        return journalDao.getJournals().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getJournalsPaged(offset: Int, limit: Int): List<Journal> {
        return journalDao.getJournalsPaged(offset, limit).map { it.toDomainModel() }
    }

    override suspend fun insertJournal(journal: Journal): Long {
        return journalDao.insertJournal(journal.toEntity())
    }

    override suspend fun updateJournal(journal: Journal) {
        journalDao.updateJournal(journal.toEntity())
    }

    override suspend fun deleteJournal(id: Int) {
        journalDao.deleteJournal(id)
    }

    override suspend fun syncWithServer(): Result<Int> {
        return try {
            val result = entryService.getAllEntries()
            if (result.isSuccess) {
                val entries = result.getOrThrow()
                entries.forEach { entry ->
                    val journal = entry.toDomainModel()
                    insertJournal(journal)
                }
                Result.success(entries.size)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 转换方法 - Entity 到 Domain
    private fun JournalEntity.toDomainModel(): Journal {
        val location = if (locationName != null && latitude != null && longitude != null) {
            Location(locationName, latitude, longitude)
        } else null

        val imagesList = imagesJson?.let {
            // 假设有工具类来解析JSON字符串为List<String>
            parseImagesJson(it)
        } ?: emptyList()

        return Journal(
            id = id,
            text = text,
            date = date,
            location = location,
            images = imagesList,
            isMark = isMark
        )
    }

    // 转换方法 - Domain 到 Entity
    private fun Journal.toEntity(): JournalEntity {
        return JournalEntity(
            id = id,
            text = text,
            date = date,
            locationName = location?.name,
            latitude = location?.latitude,
            longitude = location?.longitude,
            imagesJson = if (images.isNotEmpty()) {
                // 假设有工具类来转换List<String>为JSON字符串
                convertImagesToJson(images)
            } else null,
            isMark = isMark
        )
    }

    // 工具方法示例
    private fun parseImagesJson(json: String): List<String> {
        // 实际实现会使用JSON解析库
        return emptyList() // 示例实现
    }

    private fun convertImagesToJson(images: List<String>): String {
        // 实际实现会使用JSON转换库
        return "" // 示例实现
    }
}

// 3. Presentation Layer 表现层 - 视图模型和UI

// 视图状态 - app/src/main/kotlin/ovo/sypw/journal/presentation/screens/JournalListState.kt
package ovo.sypw.journal.presentation.screens

import ovo.sypw.journal.domain.models.Journal
import java.util.Date

data class JournalListState(
    val journals: List<Journal> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMoreData: Boolean = true,
    val markedItems: Set<Int> = emptySet(),
    val isScrolling: Boolean = false,
    val scrollToPosition: Int? = null,
    val canUndo: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val syncTotal: Int = 0,
    val lastSyncTime: Date? = null,
    val forceRefresh: Long = 0 // 用于强制刷新
) {
    companion object {
        val Initial = JournalListState(
            journals = emptyList(),
            isLoading = true,
            hasMoreData = true
        )
    }
}

// 视图模型 - app/src/main/kotlin/ovo/sypw/journal/presentation/viewmodels/JournalListViewModel.kt
package ovo.sypw.journal.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovo.sypw.journal.domain.models.Journal
import ovo.sypw.journal.domain.usecases.DeleteJournalUseCase
import ovo.sypw.journal.domain.usecases.GetJournalsUseCase
import ovo.sypw.journal.domain.usecases.SyncJournalsUseCase
import ovo.sypw.journal.domain.usecases.UpdateJournalUseCase
import ovo.sypw.journal.presentation.screens.JournalListState
import java.util.Date
import java.util.LinkedList
import javax.inject.Inject

private const val PAGE_SIZE = 10

@HiltViewModel
class JournalListViewModel @Inject constructor(
    private val getJournalsUseCase: GetJournalsUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
    private val deleteJournalUseCase: DeleteJournalUseCase,
    private val syncJournalsUseCase: SyncJournalsUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(JournalListState.Initial)
    val uiState: StateFlow<JournalListState> = _uiState.asStateFlow()

    // 分页参数
    private var currentPage = 0
    private var isLoading = false

    // 删除历史记录，用于撤销操作
    private val deletedJournals = LinkedList<Journal>()

    init {
        // 初始化加载第一页数据
        loadNextPage()
    }

    /**
     * 加载下一页数据
     */
    fun loadNextPage() {
        if (isLoading || !_uiState.value.hasMoreData) return

        isLoading = true
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val offset = currentPage * PAGE_SIZE
                val journals = getJournalsUseCase(offset, PAGE_SIZE)

                val hasMoreData = journals.isNotEmpty()
                if (hasMoreData) {
                    currentPage++
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        journals = currentState.journals + journals,
                        hasMoreData = hasMoreData,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 同步数据
     */
    fun syncData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isSyncing = true,
                    syncProgress = 0,
                    syncTotal = 0
                )
            }

            try {
                val result = syncJournalsUseCase()

                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            lastSyncTime = Date(),
                            isLoading = false,
                            isSyncing = false,
                            syncProgress = count,
                            syncTotal = count
                        )
                    }

                    // 重新加载数据
                    resetList()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSyncing = false,
                            error = result.exceptionOrNull()?.message ?: "同步失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSyncing = false,
                        error = e.message ?: "同步失败"
                    )
                }
            }
        }
    }

    /**
     * 重置列表
     */
    fun resetList() {
        currentPage = 0
        _uiState.value = JournalListState.Initial
        viewModelScope.launch {
            loadNextPage()
        }
    }
}

// UI屏幕 - app/src/main/kotlin/ovo/sypw/journal/presentation/screens/JournalListScreen.kt
package ovo.sypw.journal.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ovo.sypw.journal.presentation.components.JournalCard
import ovo.sypw.journal.presentation.components.SyncButton
import ovo.sypw.journal.presentation.viewmodels.JournalListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalListScreen(
    onAddClick: () -> Unit,
    onJournalClick: (Int) -> Unit,
    viewModel: JournalListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 检测是否到达底部，用于分页加载
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset

                (lastVisibleItem.index + 1 >= layoutInfo.totalItemsCount &&
                        lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight)
            }
        }
    }

    // 到达底部时加载更多
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && uiState.hasMoreData && !uiState.isLoading) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日记") },
                actions = {
                    // 同步按钮
                    SyncButton(
                        isSyncing = uiState.isSyncing,
                        progress = if (uiState.syncTotal > 0) {
                            uiState.syncProgress / uiState.syncTotal.toFloat()
                        } else 0f,
                        onClick = { viewModel.syncData() }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = "添加"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                LazyColumn(state = listState) {
                    items(
                        items = uiState.journals,
                        key = { it.id }
                    ) { journal ->
                        JournalCard(
                            journal = journal,
                            isMarked = uiState.markedItems.contains(journal.id),
                            onClick = { onJournalClick(journal.id) }
                        )
                    }

                    // 底部加载指示器
                    if (uiState.isLoading && uiState.journals.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            // 加载中指示器
            if (uiState.isLoading && uiState.journals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 错误提示
            uiState.error?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("错误: $error")
                }
            }
        }
    }
} 