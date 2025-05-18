package ovo.sypw.journal.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.common.utils.AutoSaveManager
import ovo.sypw.journal.data.JournalPreferences
import javax.inject.Singleton

/**
 * 自动保存模块
 * 提供AutoSaveManager的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object AutoSaveModule {

    /**
     * 提供AutoSaveManager实例
     */
    @Provides
    @Singleton
    fun provideAutoSaveManager(
        preferences: JournalPreferences
    ): AutoSaveManager {
        return AutoSaveManager(preferences)
    }
} 