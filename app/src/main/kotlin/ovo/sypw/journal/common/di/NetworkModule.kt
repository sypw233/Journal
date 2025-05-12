package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.common.utils.DatabaseManager
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.data.remote.api.FileService
import javax.inject.Singleton

/**
 * 网络模块
 * 提供网络服务相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供FileService实例
     */
    @Provides
    @Singleton
    fun provideFileService(
        @ApplicationContext context: Context,
        authService: AuthService
    ): FileService {
        return FileService(context, authService)
    }

    /**
     * 提供DatabaseManager实例
     */
    @Provides
    @Singleton
    fun provideDatabaseManager(
        @ApplicationContext context: Context,
        fileService: FileService
    ): DatabaseManager {
        return DatabaseManager(context, fileService)
    }
} 