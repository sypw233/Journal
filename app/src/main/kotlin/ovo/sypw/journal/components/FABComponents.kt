package ovo.sypw.journal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.data.JournalDataSource
import ovo.sypw.journal.data.JournalDataSource.Companion.getDataBaseIdCount
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.utils.SnackBarUtils

/**
 * 浮动操作按钮组件，包含添加和删除功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemFAB() {
    // 获取自定义数据源
    val dataSource = JournalDataSource.getInstance()
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    Column {
        FloatingActionButton(
            onClick = {
//                SnackbarUtils.showSnackbar("${cardItems[0].imagesThumbnail?.size}")
//                showBottomSheet = true
            },
            shape = CircleShape,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Icon(Icons.Filled.Info, "Show images")
        }
//
//        if (showBottomSheet) {
//            ModalBottomSheet(
//                onDismissRequest = { showBottomSheet = false },
//                sheetState = bottomSheetState
//            ) {
//                LazyRow(modifier = Modifier.padding(16.dp)) {
//                    items(bitmapList) { bitmap ->
//                        SnackbarUtils.showSnackbar("${bitmap.width} ${bitmap.height} ${bitmap.config.toString()}")
//                        AsyncImage(
//                            model = bitmap,
//                            contentDescription = "Image",
//                            modifier = Modifier
//                                .size(200.dp)
//                                .padding(horizontal = 8.dp)
//                        )
//                    }
//                }
//            }
//        }

        FloatingActionButton(
            onClick = {
                val id = getDataBaseIdCount()
                val newItem = JournalData(
                    id = id,
                    text = "和魏志阳邂逅${id}场鸡公煲的爱情"
                )
                dataSource.addItem(newItem)
                SnackBarUtils.showSnackBar("添加了新条目 #${newItem.id}")
            },
            shape = CircleShape,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Icon(Icons.Filled.Add, "Add item")
        }
        FloatingActionButton(
            onClick = {
                if (dataSource.loadedItems.isNotEmpty()) {
                    val firstItem = dataSource.loadedItems.first()
                    dataSource.removeItem(firstItem.id)
                    SnackBarUtils.showSnackBar("删除了条目 #${firstItem.id}")
                } else {
                    SnackBarUtils.showSnackBar("没有可删除的条目")
                }
            },
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Close, "Remove item")
        }
    }
}