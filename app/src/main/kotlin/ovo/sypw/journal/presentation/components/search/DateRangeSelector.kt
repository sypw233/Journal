package ovo.sypw.journal.presentation.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期范围选择组件
 * 允许用户选择搜索的起始和结束日期
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    visible: Boolean,
    startDate: Date?,
    endDate: Date?,
    showStartDatePicker: Boolean,
    showEndDatePicker: Boolean,
    onStartDateChange: (Date?) -> Unit,
    onEndDateChange: (Date?) -> Unit,
    onShowStartDatePicker: (Boolean) -> Unit,
    onShowEndDatePicker: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 日期格式化
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "选择日期范围",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 起始日期选择
                    DateSelector(
                        label = "起始日期",
                        selectedDate = startDate,
                        dateFormat = dateFormat,
                        onClick = { onShowStartDatePicker(true) },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 结束日期选择
                    DateSelector(
                        label = "结束日期",
                        selectedDate = endDate,
                        dateFormat = dateFormat,
                        onClick = { onShowEndDatePicker(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // 起始日期选择器对话框
    if (showStartDatePicker) {
        DatePickerDialogComponent(
            onDismiss = { onShowStartDatePicker(false) },
            onDateSelected = { date ->
                onStartDateChange(date)
                onShowStartDatePicker(false)
            }
        )
    }

    // 结束日期选择器对话框
    if (showEndDatePicker) {
        DatePickerDialogComponent(
            onDismiss = { onShowEndDatePicker(false) },
            onDateSelected = { date ->
                onEndDateChange(date)
                onShowEndDatePicker(false)
            }
        )
    }
}

/**
 * 日期选择项
 */
@Composable
private fun DateSelector(
    label: String,
    selectedDate: Date?,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.DateRange,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = selectedDate?.let { dateFormat.format(it) } ?: label,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

/**
 * 日期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogComponent(
    onDismiss: () -> Unit,
    onDateSelected: (Date) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(Date(it))
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
} 