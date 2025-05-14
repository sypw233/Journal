package ovo.sypw.journal

import android.app.Application
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.common.utils.ImageLoadUtils
import ovo.sypw.journal.data.remote.api.AuthService
import javax.inject.Inject

/**
 * 应用程序类
 * 使用@HiltAndroidApp注解启用Hilt依赖注入
 */
@HiltAndroidApp
class JournalApplication : Application() {
    
    // 注入AuthService
    @Inject
    lateinit var authService: AuthService
    
    // 注入自动同步管理器
    @Inject
    lateinit var autoSyncManager: AutoSyncManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化工具类
        AMapLocationUtils.initLocationClient(this)
        ImageLoadUtils.init(this)
        
        // 应用启动时验证token
        validateTokenOnAppStart()
    }
    
    /**
     * 在应用启动时验证token有效性
     */
    private fun validateTokenOnAppStart() {
        Thread {
            try {
                // 注意：不能直接在Application的onCreate中调用协程，因此使用线程
                Thread.sleep(500) // 等待组件初始化完成
                
                // 使用runBlocking来调用suspend函数
                kotlinx.coroutines.runBlocking {
                    authService.validateToken()
                }
            } catch (e: Exception) {
                // 忽略错误，登录状态会在AuthService中处理
            }
        }.start()
    }
}