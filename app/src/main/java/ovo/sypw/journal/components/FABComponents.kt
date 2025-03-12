package ovo.sypw.journal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 浮动操作按钮组件，包含添加和删除功能
 */
@Composable
fun AddItemFAB(cardItems: SnapshotStateList<Int>) {
    Column {
        FloatingActionButton(
            onClick = {
                cardItems.add(0, cardItems.size + 1)
            },
            shape = CircleShape,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Icon(Icons.Filled.Add, "Floating action button.")
        }
        FloatingActionButton(
            onClick = {
                if (cardItems.isNotEmpty()) {
                    cardItems.removeAt(0)
                }
            },
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Close, "Floating action button.")
        }
    }
}