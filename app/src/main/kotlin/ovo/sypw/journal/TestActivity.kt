package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ovo.sypw.journal.common.theme.JournalTheme
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.presentation.screens.DatabaseManagementScreen
import ovo.sypw.journal.presentation.viewmodels.DatabaseManagementViewModel

@AndroidEntryPoint
@SuppressLint("RestrictedApi")
class TestActivity : ComponentActivity() {
    private val viewModel: DatabaseManagementViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AMapLocationUtils.initLocationClient(this)
        setContent {
            JournalTheme {
                DatabaseManagementScreen(
                    onNavigateBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
    }
}

