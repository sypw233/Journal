package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ovo.sypw.journal.components.screen.TestScreen
import ovo.sypw.journal.ui.theme.JournalTheme
import ovo.sypw.journal.utils.GetLocation

@SuppressLint("RestrictedApi")
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GetLocation.initLocationClient(this)
        setContent {
            JournalTheme {
                TestScreen()
            }
        }
    }
}

