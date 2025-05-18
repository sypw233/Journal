package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.common.utils.SentimentAnalyzer
import javax.inject.Singleton

/**
 * 情感分析模块
 * 提供情感分析相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object SentimentModule {
    
    /**
     * 提供情感分析器实例
     */
    @Provides
    @Singleton
    fun provideSentimentAnalyzer(): SentimentAnalyzer {
        return SentimentAnalyzer()
    }
    
    /**
     * 初始化情感分析器
     * 应用启动后自动调用
     */
    @Provides
    @Singleton
    fun initializeSentimentAnalyzer(
        @ApplicationContext context: Context,
        analyzer: SentimentAnalyzer
    ): Boolean {
        return analyzer.initialize(context)
    }
} 