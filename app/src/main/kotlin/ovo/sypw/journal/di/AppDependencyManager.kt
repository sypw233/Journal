package ovo.sypw.journal.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.SentimentApiService
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.data.repository.JournalRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局依赖管理器
 * 使用单例模式集中管理所有全局依赖项
 */
@Singleton
class AppDependencyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val authService: AuthService,
    val autoSyncManager: AutoSyncManager,
    val sentimentApiService: SentimentApiService,
    val preferences: JournalPreferences,
    private val journalRepository: JournalRepository
) {
    // 可以在这里添加一些依赖项的共享功能

    /**
     * 获取应用上下文
     */
    fun getContext(): Context {
        return context
    }

    /**
     * 获取日记仓库
     */
    fun getJournalRepository(): JournalRepository {
        return journalRepository
    }

    /**
     * 初始化所有依赖项
     */
    fun initializeAll() {
        // 验证token
        validateToken()
    }

    /**
     * 验证token
     */
    private fun validateToken() {
        Thread {
            try {
                Thread.sleep(500)

                kotlinx.coroutines.runBlocking {
                    authService.validateToken()
                }
            } catch (e: Exception) {
                // 忽略错误，登录状态会在AuthService中处理
            }
        }.start()
    }
} 