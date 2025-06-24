package ovo.sypw.journal.data.repositories

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.data.database.SentimentDao
import ovo.sypw.journal.data.database.SentimentEntity
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.model.SentimentData
import ovo.sypw.journal.data.repository.SentimentRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 情感分析仓库的本地实现类
 * 实现SentimentRepository接口，处理本地数据库的情感分析数据操作
 */
@Singleton
class LocalSentimentRepository @Inject constructor(
    private val sentimentDao: SentimentDao
) : SentimentRepository {
    private val TAG = "LocalSentimentRepository"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override suspend fun getSentimentByJournalId(journalId: Int): SentimentData? {
        return withContext(Dispatchers.IO) {
            sentimentDao.getSentimentByJournalId(journalId)?.toSentimentData()
        }
    }

    override suspend fun getAllSentiments(): List<SentimentData> {
        return withContext(Dispatchers.IO) {
            sentimentDao.getAllSentiments().map { it.toSentimentData() }
        }
    }

    override suspend fun getSentimentsByType(sentimentType: SentimentType): List<SentimentData> {
        return withContext(Dispatchers.IO) {
            sentimentDao.getSentimentsByType(sentimentType.name).map { it.toSentimentData() }
        }
    }

    override suspend fun saveSentiment(sentiment: SentimentData) {
        withContext(Dispatchers.IO) {
            try {
                sentimentDao.insertSentiment(SentimentEntity.fromSentimentData(sentiment))
                Log.d(TAG, "情感分析结果已保存到数据库: journalId=${sentiment.journalId}")
            } catch (e: Exception) {
                Log.e(TAG, "保存情感分析结果失败: ${e.message}", e)
            }
        }
    }

    override suspend fun saveSentiments(sentiments: List<SentimentData>) {
        withContext(Dispatchers.IO) {
            try {
                val entities = sentiments.map { SentimentEntity.fromSentimentData(it) }
                sentimentDao.insertSentiments(entities)
                Log.d(TAG, "批量保存了${entities.size}条情感分析结果")
            } catch (e: Exception) {
                Log.e(TAG, "批量保存情感分析结果失败: ${e.message}", e)
            }
        }
    }

    override suspend fun deleteSentimentByJournalId(journalId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val count = sentimentDao.deleteSentimentByJournalId(journalId)
                Log.d(TAG, "删除了${count}条情感分析结果: journalId=$journalId")
            } catch (e: Exception) {
                Log.e(TAG, "删除情感分析结果失败: ${e.message}", e)
            }
        }
    }

    override suspend fun deleteAllSentiments() {
        withContext(Dispatchers.IO) {
            try {
                sentimentDao.deleteAllSentiments()
                Log.d(TAG, "已删除所有情感分析结果")
            } catch (e: Exception) {
                Log.e(TAG, "删除所有情感分析结果失败: ${e.message}", e)
            }
        }
    }

    override suspend fun getSentimentCount(): Int {
        return withContext(Dispatchers.IO) {
            sentimentDao.getSentimentCount()
        }
    }

    override suspend fun getJournalIdsByType(sentimentType: SentimentType): List<Int> {
        return withContext(Dispatchers.IO) {
            sentimentDao.getJournalIdsByType(sentimentType.name)
        }
    }

    override suspend fun getJournalsWithSentiments(): List<Pair<JournalData, SentimentData?>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = sentimentDao.getJournalsWithSentiments()
                Log.d(TAG, "获取到${result.size}条日记与情感分析结果")
                result.map { it.toModel() }
            } catch (e: Exception) {
                Log.e(TAG, "获取日记与情感分析结果失败: ${e.message}", e)
                emptyList()
            }
        }
    }
} 