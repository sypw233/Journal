package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.theme.JournalTheme
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.data.JournalPreferences
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.di.AppDependencyManager
import ovo.sypw.journal.presentation.screens.AIChatScreen
import ovo.sypw.journal.presentation.screens.DatabaseManagementScreen
import ovo.sypw.journal.presentation.screens.MainScreen
import ovo.sypw.journal.presentation.screens.SentimentReportScreen
import ovo.sypw.journal.presentation.screens.SettingsScreen
import ovo.sypw.journal.presentation.viewmodels.AIChatViewModel
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
import ovo.sypw.journal.presentation.viewmodels.MainViewModel

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

    // 依赖管理器
    private val dependencyManager: AppDependencyManager by lazy {
        (application as JournalApplication).dependencyManager
    }

    // 依赖项的快捷引用
    private val authService: AuthService
        get() = dependencyManager.authService

    private val autoSyncManager: AutoSyncManager
        get() = dependencyManager.autoSyncManager

    private val preferences: JournalPreferences
        get() = dependencyManager.preferences

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
                    isSystemInDarkTheme()
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
        startDestination = "main",
        enterTransition = {
            fadeIn(animationSpec = tween(300, easing = EaseInOut)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = EaseInOut))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300, easing = EaseOut)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(300, easing = EaseOut))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300, easing = EaseInOut)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = EaseInOut))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300, easing = EaseOut)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(300, easing = EaseOut))
        }
    ) {
        // 主界面路由
        composable(
            "main",
            enterTransition = {
                // 主界面进入时的平滑淡入动画
                fadeIn(animationSpec = tween(500, easing = EaseInOut))
            },
            exitTransition = {
                // 主界面退出时的平滑淡出动画
                fadeOut(animationSpec = tween(500, easing = EaseOut))
            },
            popEnterTransition = {
                // 返回到主界面时的动画
                fadeIn(animationSpec = tween(500, easing = EaseInOut))
            },
            popExitTransition = {
                // 从主界面返回时的动画
                fadeOut(animationSpec = tween(500, easing = EaseOut))
            }
        ) {
            MainScreen(
                viewModel = viewModel,
                databaseManagementViewModel = databaseManagementViewModel,
                autoSyncManager = autoSyncManager,
                navController = navController
            )
        }

        // 设置界面路由
        composable(
            "settings",
            enterTransition = {
                // 淡入并从底部滑入的组合动画
                fadeIn(animationSpec = tween(300, easing = EaseInOut)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(300, easing = EaseInOut)
                        )
            },
            exitTransition = {
                // 淡出并向底部滑出的组合动画
                fadeOut(animationSpec = tween(300, easing = EaseOut)) +
                        scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(300, easing = EaseOut)
                        )
            },
            popEnterTransition = {
                // 返回时的淡入动画
                fadeIn(animationSpec = tween(300, easing = EaseInOut)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(300, easing = EaseInOut)
                        )
            },
            popExitTransition = {
                // 返回时的淡出动画
                fadeOut(animationSpec = tween(300, easing = EaseOut)) +
                        scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(300, easing = EaseOut)
                        )
            }
        ) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        // 数据库管理界面路由
        composable(
            "database_management",
            enterTransition = {
                // 自定义进入动画：淡入并缩放
                fadeIn(animationSpec = tween(400, easing = EaseInOut)) +
                        scaleIn(
                            initialScale = 0.85f,
                            animationSpec = tween(400, easing = EaseInOut)
                        )
            },
            exitTransition = {
                // 自定义退出动画：淡出并缩放
                fadeOut(animationSpec = tween(400, easing = EaseOut)) +
                        scaleOut(
                            targetScale = 0.85f,
                            animationSpec = tween(400, easing = EaseOut)
                        )
            },
            popEnterTransition = {
                // 返回时的进入动画
                fadeIn(animationSpec = tween(400, easing = EaseInOut)) +
                        scaleIn(
                            initialScale = 0.85f,
                            animationSpec = tween(400, easing = EaseInOut)
                        )
            },
            popExitTransition = {
                // 返回时的退出动画
                fadeOut(animationSpec = tween(400, easing = EaseOut)) +
                        scaleOut(
                            targetScale = 0.85f,
                            animationSpec = tween(400, easing = EaseOut)
                        )
            }
        ) {
            DatabaseManagementScreen(
                onBackClick = {
                    navController.popBackStack()
                })
        }

        // 情感分析报告界面路由
        composable(
            "sentiment_report",
            enterTransition = {
                // 自定义进入动画：从中心放大并淡入
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(350, easing = EaseInOut)
                ) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                // 自定义退出动画：缩小并淡出
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(350, easing = EaseOut)
                ) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                // 返回时的动画
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(350, easing = EaseInOut)
                ) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                // 返回时的退出动画
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(350, easing = EaseOut)
                ) + fadeOut(animationSpec = tween(350))
            }
        ) {
            SentimentReportScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // AI聊天界面路由
        composable(
            "ai_chat",
            enterTransition = {
                // 进入时的动画
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(350, easing = EaseInOut)
                ) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                // 退出时的动画
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(350, easing = EaseOut)
                ) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                // 返回时的动画
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(350, easing = EaseInOut)
                ) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                // 返回时的退出动画
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(350, easing = EaseOut)
                ) + fadeOut(animationSpec = tween(350))
            }
        ) {
            // 创建AIChatViewModel
            val context = LocalContext.current
            val appDepManager =
                remember { (context.applicationContext as JournalApplication).dependencyManager }
            val chatViewModel = remember { AIChatViewModel(context, appDepManager) }

            AIChatScreen(viewModel = chatViewModel)
        }
    }
}

