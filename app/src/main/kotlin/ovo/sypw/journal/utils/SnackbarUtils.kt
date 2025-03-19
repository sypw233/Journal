package ovo.sypw.journal.utils
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SnackbarUtils {
    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var coroutineScope: CoroutineScope

    // 初始化工具类
    fun initialize(snackbarHostState: SnackbarHostState, coroutineScope: CoroutineScope) {
        this.snackbarHostState = snackbarHostState
        this.coroutineScope = coroutineScope
    }

    // 显示 Snackbar
    fun showSnackbar(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        println(message)
        coroutineScope.launch {
//            关闭当前SnackBar
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }
}