package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.DatabaseManager
import ovo.sypw.journal.data.JournalPreferences
import javax.inject.Singleton

/**
 * 同步模块
 * 提供自动同步相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    /**
     * 提供AutoSyncManager实例
     */
    @Provides
    @Singleton
    fun provideAutoSyncManager(
        @ApplicationContext context: Context,
        databaseManager: DatabaseManager,
        preferences: JournalPreferences
    ): AutoSyncManager {
        return AutoSyncManager(context, databaseManager, preferences)
    }
} 