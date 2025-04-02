package ovo.sypw.journal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.data.database.JournalDao
import ovo.sypw.journal.data.database.JournalDatabase
import javax.inject.Singleton

/**
 * 数据库模块
 * 提供数据库和DAO的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供JournalDatabase实例
     */
    @Provides
    @Singleton
    fun provideJournalDatabase(@ApplicationContext context: Context): JournalDatabase {
        return JournalDatabase.getDatabase(context)
    }

    /**
     * 提供JournalDao实例
     */
    @Provides
    @Singleton
    fun provideJournalDao(database: JournalDatabase): JournalDao {
        return database.journalDao()
    }
}