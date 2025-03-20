package ovo.sypw.journal.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovo.sypw.journal.data.database.JournalDatabase
import ovo.sypw.journal.data.database.JournalRepository
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.utils.SnackBarUtils

/**
 * 自定义日记数据源，用于替代Paging3的实现
 * 提供分页加载功能和数据操作方法
 * 集成Room数据库实现数据持久化
 */
class JournalDataSource private constructor() {
    companion object {
        private val instance = JournalDataSource()
        fun getInstance(): JournalDataSource = instance

        // 所有数据项
        private val allItems = mutableListOf<JournalData>()
        fun getAllItemsList() = allItems.toList()

        // 数据库相关
        private lateinit var repository: JournalRepository
        private val coroutineScope = CoroutineScope(Dispatchers.IO)
        private var isInitialized = false

        /**
         * 初始化数据库
         */
        fun initDatabase(context: Context) {
            if (!isInitialized) {
                val database = JournalDatabase.getDatabase(context)
                repository = JournalRepository(database.journalDao())
                isInitialized = true

                // 从数据库加载数据
//                loadDataFromDatabase()
            }
        }

        fun firstLaunchDatabaseInit() {
            coroutineScope.launch {
                val sampleData = List(10) { index ->
                    JournalData(
                        id = index,
                        images = SampleDataProvider.generateBitmapList(0),
                        text = "《恋爱猪脚饭》——工地与猪脚饭交织的浪漫邂逅！\n" + "\"当你以为人生已经烂尾时，命运的混凝土搅拌机正在偷偷运转！\"\n" + "破产老哥黄夏揣着最后的房租钱，逃进花都城中村的握手楼。本想和小茂等挂壁老哥一起吃猪脚饭躺平摆烂，却意外邂逅工地女神\"陈嘉怡\"，从而开启新的土木逆袭人生。\n" + "爽了，干土木的又爽了！即使在底层已经彻底有了的我们，也能通过奋斗拥有美好的明天！"
                    )
                }
                repository.insertJournals(sampleData)
            }
        }

        /**
         * 从数据库加载数据
         */
        private fun loadDataFromDatabase() {
            coroutineScope.launch {
                // 检查数据库是否有数据
                val count = repository.getJournalCount()

                if (count == 0) {
                    SnackBarUtils.showSnackBar("DataBase has no data")
                    // 如果数据库为空，添加示例数据
                    val sampleData = List(10) { index ->
                        JournalData(
                            id = index,
                            images = SampleDataProvider.generateBitmapList(0),
                            text = "《恋爱猪脚饭》——工地与猪脚饭交织的浪漫邂逅！\n" + "\"当你以为人生已经烂尾时，命运的混凝土搅拌机正在偷偷运转！\"\n" + "破产老哥黄夏揣着最后的房租钱，逃进花都城中村的握手楼。本想和小茂等挂壁老哥一起吃猪脚饭躺平摆烂，却意外邂逅工地女神\"陈嘉怡\"，从而开启新的土木逆袭人生。\n" + "爽了，干土木的又爽了！即使在底层已经彻底有了的我们，也能通过奋斗拥有美好的明天！"
                        )
                    }
                    repository.insertJournals(sampleData)
                    allItems.addAll(sampleData)
                }
            }
        }
    }

    // 当前加载的数据项
    private val _loadedItems = mutableStateListOf<JournalData>()
    val loadedItems: SnapshotStateList<JournalData> = _loadedItems

    // 分页参数
    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private var hasMoreData = true

    /**
     * 初始化数据源，加载第一页数据
     */
    fun initialize() {
        _loadedItems.clear()
        currentPage = 0
        hasMoreData = true
        loadNextPage()
    }

    /**
     * 加载下一页数据
     * @return 是否成功加载数据
     */
    fun loadNextPage(): Boolean {
        if (isLoading || !hasMoreData) return false

        isLoading = true
        try {
            if (isInitialized) {
                // 从数据库加载数据
                coroutineScope.launch {
//                    SnackBarUtils.showSnackBar("Loading page $currentPage")
                    val offset = currentPage * pageSize
                    val journals = repository.getJournalsPaged(offset, pageSize)
                    SnackBarUtils.showSnackBar("Loading page $currentPage, now has ${journals.size}")

                    withContext(Dispatchers.Main) {
                        if (journals.isEmpty()) {
                            hasMoreData = false
                        } else {
                            allItems.addAll(journals)
                            _loadedItems.addAll(journals)
                            currentPage += 1
                            isLoading = false
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            throw e
            return false
        }
    }

    /**
     * 添加新的数据项
     * @param item 要添加的数据项
     */
    fun addItem(item: JournalData, index: Int = 0) {
        allItems.add(index, item)
        _loadedItems.add(index, item)
    }

    /**
     * 移除指定ID的数据项
     * @param id 要移除的数据项ID
     * @return 是否成功移除
     */
    fun removeItem(id: Int): Boolean {
        val removed = allItems.removeAll { it.id == id }
        _loadedItems.removeAll { it.id == id }
        repository.deleteJournalById(id)
        return removed
    }

    /**
     * 移除指定ID的数据项
     * @param id 要移除的数据项ID
     */
    fun removeItemData(id: Int) {
        repository.deleteJournalById(id)
    }

    /**
     * 更新指定ID的数据项
     * @param id 要更新的数据项ID
     * @param updater 更新函数
     * @return 是否成功更新
     */
    fun updateItem(id: Int, updater: (JournalData) -> JournalData): Boolean {
        val allItemIndex = allItems.indexOfFirst { it.id == id }
        val loadedItemIndex = _loadedItems.indexOfFirst { it.id == id }

        if (allItemIndex != -1) {
            val updated = updater(allItems[allItemIndex])
            allItems[allItemIndex] = updated

            if (loadedItemIndex != -1) {
                _loadedItems[loadedItemIndex] = updated
            }

            return true
        }

        return false
    }

    /**
     * 检查是否还有更多数据可加载
     * @return 是否有更多数据
     */
    fun hasMoreData(): Boolean = hasMoreData

    /**
     * 检查是否正在加载数据
     * @return 是否正在加载
     */
    fun isLoading(): Boolean = isLoading

    /**
     * 重置数据源状态
     */
    fun reset() {
        _loadedItems.clear()
        currentPage = 0
        hasMoreData = true
        isLoading = false
    }
}