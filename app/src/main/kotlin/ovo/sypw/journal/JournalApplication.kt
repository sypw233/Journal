package ovo.sypw.journal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.ImageLoadUtils
import ovo.sypw.journal.di.AppDependencyManager
import javax.inject.Inject

/**
 * 应用程序类
 * 使用@HiltAndroidApp注解启用Hilt依赖注入
 */
@HiltAndroidApp
class JournalApplication : Application() {
    
    // 注入依赖管理器
    @Inject
    lateinit var dependencyManager: AppDependencyManager
    
    // 单例实例
    companion object {
        private lateinit var instance: JournalApplication
        
        fun getInstance(): JournalApplication {
            return instance
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化工具类
        AMapLocationUtils.initLocationClient(this)
        ImageLoadUtils.init(this)
        
        // 使用依赖管理器初始化所有依赖项
        dependencyManager.initializeAll()
    }
}