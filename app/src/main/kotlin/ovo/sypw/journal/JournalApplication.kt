package ovo.sypw.journal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ovo.sypw.journal.utils.GetLocation
import ovo.sypw.journal.utils.ImageLoadUtils

/**
 * 应用程序类
 * 使用@HiltAndroidApp注解启用Hilt依赖注入
 */
@HiltAndroidApp
class JournalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化工具类
        GetLocation.initLocationClient(this)
        ImageLoadUtils.init(this)
    }
}