package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.common.utils.SentimentApiService
import ovo.sypw.journal.data.database.JournalDatabase
import ovo.sypw.journal.data.database.SentimentDao
import ovo.sypw.journal.data.repositories.LocalSentimentRepository
import ovo.sypw.journal.data.repository.SentimentRepository
import javax.inject.Singleton

/**
 * 情感分析模块
 * 提供情感分析相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object SentimentModule {


    /**
     * 提供情感分析API服务实例
     */
    @Provides
    @Singleton
    fun provideSentimentApiService(
        @ApplicationContext context: Context
    ): SentimentApiService {
        return SentimentApiService(context)
    }

    /**
     * 提供情感分析DAO实例
     */
    @Provides
    @Singleton
    fun provideSentimentDao(database: JournalDatabase): SentimentDao {
        return database.sentimentDao()
    }

    /**
     * 提供情感分析仓库实例
     */
    @Provides
    @Singleton
    fun provideSentimentRepository(sentimentDao: SentimentDao): SentimentRepository {
        return LocalSentimentRepository(sentimentDao)
    }

} 