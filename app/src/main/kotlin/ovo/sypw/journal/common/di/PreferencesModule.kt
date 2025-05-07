package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.data.JournalPreferences
import javax.inject.Singleton

/**
 * 偏好设置模块
 * 提供JournalPreferences的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    /**
     * 提供JournalPreferences实例
     */
    @Provides
    @Singleton
    fun provideJournalPreferences(@ApplicationContext context: Context): JournalPreferences {
        return JournalPreferences(context)
    }
}