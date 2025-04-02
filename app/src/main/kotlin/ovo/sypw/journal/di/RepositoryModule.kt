package ovo.sypw.journal.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.data.repository.LocalJournalRepository
import javax.inject.Singleton

/**
 * 仓库模块
 * 提供Repository的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 绑定JournalRepository接口到LocalJournalRepository实现
     */
    @Binds
    @Singleton
    abstract fun bindJournalRepository(repository: LocalJournalRepository): JournalRepository
}