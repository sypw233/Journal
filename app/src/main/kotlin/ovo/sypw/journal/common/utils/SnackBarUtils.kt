package ovo.sypw.journal.common.utils

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SnackBarUtils {
    private lateinit var snackBarHostState: SnackbarHostState
    private lateinit var coroutineScope: CoroutineScope

    // 初始化工具类
    fun initialize(snackbarHostState: SnackbarHostState, coroutineScope: CoroutineScope) {
        this.snackBarHostState = snackbarHostState
        this.coroutineScope = coroutineScope
    }

    // 显示 SnackBar
    fun showSnackBar(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        Log.d("SNACK_BAR_SHOW", message)
        coroutineScope.launch {
            // 关闭当前SnackBar
            snackBarHostState.currentSnackbarData?.dismiss()
            snackBarHostState.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }

    fun showActionSnackBar(
        message: String, duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String, onActionPerformed: () -> Unit, onDismissed: () -> Unit
    ) {
        coroutineScope.launch {
            // 关闭当前SnackBar
            snackBarHostState.currentSnackbarData?.dismiss()
            val result = snackBarHostState.showSnackbar(
                message = message,
                duration = duration,
                actionLabel = actionLabel,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    // 当前SnackBar的按钮被点击
                    onActionPerformed()
                }

                SnackbarResult.Dismissed -> {
                    // 当前SnackBar被关闭或超时
                    onDismissed()
                }
            }
        }
    }
    
    // 获取协程作用域
    fun getCoroutineScope(): CoroutineScope? {
        return if (::coroutineScope.isInitialized) coroutineScope else null
    }
}

// 自定义顶部 SnackbarHost 组件
@Composable
fun TopSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
    ) { snackbarData ->
        Snackbar(
//            modifier = Modifier.padding(bottom = 700.dp),
            snackbarData = snackbarData
        )
    }

}