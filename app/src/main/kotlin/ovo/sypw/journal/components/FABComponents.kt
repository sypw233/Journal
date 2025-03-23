package ovo.sypw.journal.components

import android.util.Log
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.data.JournalDataSource
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.utils.PermissionUtils
import ovo.sypw.journal.utils.RequestPermissions
import ovo.sypw.journal.utils.SnackBarUtils

/**
 * 浮动操作按钮组件，包含添加和删除功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemFAB() {
    // 获取自定义数据源
    val dataSource = JournalDataSource.getInstance()
    RequestPermissions(
        permissions = PermissionUtils.LOCATION_PERMISSIONS,
        onPermissionResult = { granted ->
            if (granted) {
                // 权限已授予，可以获取位置
                Log.i(TAG, "已获得定位权限，可以获取当前位置")
            } else {
                // 权限被拒绝
                Log.e(TAG, "未获得定位权限，无法获取当前位置")
            }
        }
    )
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

        FloatingActionButton(
            onClick = {
                val id = JournalDataSource.getDataBaseIdCountWithPositive()
                dataSource.addItem(
                    JournalData(
                        id = id,
                        text = "New item $id"
                    )
                )
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