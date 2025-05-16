package ovo.sypw.journal.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.common.utils.SnackBarUtils

@Composable
fun TestScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(0.dp),
    contentAlignment: Alignment = Alignment.Center
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackBarUtils.initialize(snackbarHostState, coroutineScope)
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        LocalContext.current
//        AIChatScreen(viewModel = viewModel { AIChatViewModel(content) })

//        Button(
//            onClick = {
//                DatabaseExporter.exportDatabaseToAppStorage(content,"journal_database")
//            }
//        ) {
//            Text(text = "导出数据库")
//        }
    }
}

