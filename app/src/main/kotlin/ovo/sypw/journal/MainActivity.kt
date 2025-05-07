package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ovo.sypw.journal.common.theme.JournalTheme
import ovo.sypw.journal.data.remote.api.AuthService
import ovo.sypw.journal.presentation.screens.MainScreen
import ovo.sypw.journal.presentation.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {
    // 使用Hilt注入ViewModel
    private val viewModel: MainViewModel by viewModels()
    
    // 注入AuthService
    @Inject
    lateinit var authService: AuthService
    
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
                // 传递ViewModel和AuthService到MainScreen
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

