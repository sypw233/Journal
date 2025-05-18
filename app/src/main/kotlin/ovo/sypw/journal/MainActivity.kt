package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.theme.JournalTheme
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.presentation.screens.MainScreen
import ovo.sypw.journal.presentation.screens.SettingsScreen
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
import ovo.sypw.journal.presentation.viewmodels.MainViewModel
import javax.inject.Inject

/**
 * 主题数据类，包含主题的各项设置
 */
data class ThemeSettings(
    val useDarkTheme: Boolean = false,
    val useSystemTheme: Boolean = true,
    val primaryColorIndex: Int = 0
)

@AndroidEntryPoint
@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {
    // 使用Hilt注入ViewModel
    private val viewModel: MainViewModel by viewModels()
    
    // 数据库管理ViewModel
    private val databaseManagementViewModel: DatabaseManagementViewModel by viewModels()
    
    // 注入AuthService
    @Inject
    lateinit var authService: AuthService
    
    // 注入AutoSyncManager
    @Inject
    lateinit var autoSyncManager: AutoSyncManager
    
    // 注入JournalPreferences
    @Inject
    lateinit var preferences: JournalPreferences
    
    // 主题设置状态流
    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings
    
    // 用于定期检查token
    private val handler = Handler(Looper.getMainLooper())
    private val tokenCheckInterval = 30 * 60 * 1000L // 30分钟检查一次
    
    // token检查任务
    private val tokenCheckRunnable = object : Runnable {
        override fun run() {
            // 在lifecycleScope中验证token
            lifecycleScope.launch {
                authService.validateToken()
            }
            // 再次安排检查
            handler.postDelayed(this, tokenCheckInterval)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化主题设置
        updateThemeSettings()
        
        // 监听SharedPreferences的变化
        lifecycleScope.launch {
            preferences.observeThemeChanges().collect { newThemeSettings ->
                // 打印调试信息
                println("Theme settings changed: $newThemeSettings")
                _themeSettings.value = newThemeSettings
            }
        }
        
        setContent {
            // 从状态流中获取实时主题设置
            val currentThemeSettings by themeSettings.collectAsState()
            
            // 打印主题设置调试信息
            println("Applying theme: ${currentThemeSettings.primaryColorIndex}")
            
            // 根据设置应用主题
            JournalTheme(
                darkTheme = if (currentThemeSettings.useSystemTheme) 
                    androidx.compose.foundation.isSystemInDarkTheme() 
                else 
                    currentThemeSettings.useDarkTheme,
                dynamicColor = false, // 禁用动态颜色，确保我们的颜色方案生效
                colorIndex = currentThemeSettings.primaryColorIndex
            ) {
                // 使用Navigation组件
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    databaseManagementViewModel = databaseManagementViewModel,
                    autoSyncManager = autoSyncManager
                )
            }
        }
        
        // 启动定期token检查
        startTokenCheck()
    }
    
    /**
     * 初始化或更新主题设置状态
     */
    private fun updateThemeSettings() {
        _themeSettings.value = ThemeSettings(
            useDarkTheme = preferences.isUseDarkTheme(),
            useSystemTheme = preferences.isUseSystemTheme(),
            primaryColorIndex = preferences.getPrimaryColorIndex()
        )
    }
    
    override fun onResume() {
        super.onResume()
        // 应用回到前台时，立即验证token
        lifecycleScope.launch {
            authService.validateToken()
        }
        // 刷新主题设置
        updateThemeSettings()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止token检查
        stopTokenCheck()
    }
    
    /**
     * 启动定期token检查
     */
    private fun startTokenCheck() {
        handler.postDelayed(tokenCheckRunnable, tokenCheckInterval)
    }
    
    /**
     * 停止token检查
     */
    private fun stopTokenCheck() {
        handler.removeCallbacks(tokenCheckRunnable)
    }
}

/**
 * 应用导航宿主
 * 定义应用的导航图
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    databaseManagementViewModel: DatabaseManagementViewModel,
    autoSyncManager: AutoSyncManager
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // 主界面路由
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                databaseManagementViewModel = databaseManagementViewModel,
                autoSyncManager = autoSyncManager,
                navController = navController
            )
        }
        
        // 设置界面路由
        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

