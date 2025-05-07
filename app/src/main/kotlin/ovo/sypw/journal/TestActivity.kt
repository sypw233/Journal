package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ovo.sypw.journal.common.theme.JournalTheme
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.presentation.screens.TestScreen

@SuppressLint("RestrictedApi")
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AMapLocationUtils.initLocationClient(this)
        setContent {
            JournalTheme {
                TestScreen()
            }
        }
    }
}

