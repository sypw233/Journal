package ovo.sypw.journal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import ovo.sypw.journal.components.screen.MainScreen
import ovo.sypw.journal.ui.theme.JournalTheme
import ovo.sypw.journal.utils.GetLocation
import ovo.sypw.journal.utils.SnackBarUtils

@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val forActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    SnackBarUtils.showSnackBar(data?.data.toString())
                }
            }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GetLocation.initLocationClient(this)
        setContent {
            JournalTheme {
                MainScreen()
            }
        }
    }
}

