package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.common.utils.SentimentApiService
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

} 