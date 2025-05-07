package ovo.sypw.journal.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.data.remote.api.AuthService
import javax.inject.Singleton

/**
 * 认证模块
 * 提供认证服务的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * 提供AuthService实例
     */
    @Provides
    @Singleton
    fun provideAuthService(@ApplicationContext context: Context): AuthService {
        return AuthService(context)
    }
}