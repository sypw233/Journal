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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.theme.JournalTheme
import ovo.sypw.journal.common.utils.AutoSyncManager
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.presentation.screens.MainScreen
import ovo.sypw.journal.presentation.screens.SettingsScreen
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel
import ovo.sypw.journal.presentation.viewmodels.MainViewModel
import javax.inject.Inject

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
//        val forActivityResult =
//            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == RESULT_OK) {
//                    val data = result.data
//                    SnackBarUtils.showSnackBar(data?.data.toString())
//                }
//            }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // GetLocation已在Application中初始化
        setContent {
            JournalTheme {
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
    
    override fun onResume() {
        super.onResume()
        // 应用回到前台时，立即验证token
        lifecycleScope.launch {
            authService.validateToken()
        }
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

