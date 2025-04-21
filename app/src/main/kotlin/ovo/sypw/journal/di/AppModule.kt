package ovo.sypw.journal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用模块
 * 提供应用级别的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供应用Context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}