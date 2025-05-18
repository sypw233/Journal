package ovo.sypw.journal.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.common.utils.SentimentAnalyzer
import ovo.sypw.journal.data.model.SentimentData

/**
 * 情感分析结果卡片
 * 
 * @param sentimentData 情感分析数据
 * @param isAnalyzing 是否正在分析中
 * @param expanded 是否展开显示详细信息
 * @param onReAnalyze 重新分析的回调
 * @param modifier Modifier
 */
@Composable
fun SentimentAnalysisCard(
    sentimentData: SentimentData?,
    isAnalyzing: Boolean,
    expanded: Boolean = false,
    onReAnalyze: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(expanded) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SentimentSatisfied,
                        contentDescription = "情感分析",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "情感分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row {
                    // 重新分析按钮
                    IconButton(onClick = onReAnalyze, enabled = !isAnalyzing) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "重新分析",
                            tint = if (isAnalyzing) 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 展开/收起按钮
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 加载中显示
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "正在分析情感...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } 
            // 无数据显示
            else if (sentimentData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无分析结果，点击心形图标开始分析",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } 
            // 错误状态
            else if (sentimentData.sentimentType == SentimentAnalyzer.SentimentType.UNKNOWN) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "情感分析失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onReAnalyze) {
                            Text("重试")
                        }
                    }
                }
            }
            // 结果显示
            else {
                // 简要结果
                SentimentSummary(sentimentData)
                
                // 详细结果（可展开/收起）
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        SentimentDetails(sentimentData)
                    }
                }
            }
        }
    }
}

/**
 * 情感分析结果概要
 */
@Composable
private fun SentimentSummary(data: SentimentData) {
    val sentimentIcon = when (data.sentimentType) {
        SentimentAnalyzer.SentimentType.POSITIVE -> Icons.Default.SentimentSatisfied
        SentimentAnalyzer.SentimentType.NEGATIVE -> Icons.Default.SentimentVeryDissatisfied
        else -> Icons.Default.SentimentSatisfied
    }
    
    val sentimentColor = when (data.sentimentType) {
        SentimentAnalyzer.SentimentType.POSITIVE -> Color(0xFF4CAF50)  // 绿色
        SentimentAnalyzer.SentimentType.NEGATIVE -> Color(0xFFF44336)  // 红色
        SentimentAnalyzer.SentimentType.NEUTRAL -> Color(0xFF9E9E9E)   // 灰色
        SentimentAnalyzer.SentimentType.UNKNOWN -> Color(0xFF9E9E9E)   // 灰色
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Icon(
            imageVector = sentimentIcon,
            contentDescription = data.getSentimentDescription(),
            tint = sentimentColor,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        // 文字说明
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (data.dominantEmotion.isNotEmpty()) {
                    "主要情绪: ${data.dominantEmotion}"
                } else {
                    "情感倾向: ${data.getSentimentDescription()}"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 情感得分条
            val positiveProgress by animateFloatAsState(
                targetValue = data.positiveScore, 
                label = "positive"
            )
            val negativeProgress by animateFloatAsState(
                targetValue = data.negativeScore, 
                label = "negative"
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "积极",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { positiveProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(data.positiveScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "消极",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { negativeProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFF44336),
                    trackColor = Color(0xFFF44336).copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(data.negativeScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * 情感分析详细信息
 */
@Composable
private fun SentimentDetails(data: SentimentData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DetailRow(
            title = "情感类型",
            value = data.getSentimentDescription(),
            icon = when (data.sentimentType) {
                SentimentAnalyzer.SentimentType.POSITIVE -> Icons.Default.SentimentSatisfied
                SentimentAnalyzer.SentimentType.NEGATIVE -> Icons.Default.SentimentVeryDissatisfied
                else -> Icons.Default.SentimentSatisfied
            },
            iconTint = when (data.sentimentType) {
                SentimentAnalyzer.SentimentType.POSITIVE -> Color(0xFF4CAF50)
                SentimentAnalyzer.SentimentType.NEGATIVE -> Color(0xFFF44336)
                SentimentAnalyzer.SentimentType.NEUTRAL -> Color(0xFF9E9E9E)
                SentimentAnalyzer.SentimentType.UNKNOWN -> Color(0xFF9E9E9E)
            }
        )
        
        if (data.dominantEmotion.isNotEmpty()) {
            DetailRow(
                title = "主要情绪",
                value = data.dominantEmotion,
                icon = Icons.Default.Favorite,
                iconTint = MaterialTheme.colorScheme.primary
            )
        }
        
        DetailRow(
            title = "积极得分",
            value = String.format("%.2f", data.positiveScore),
            valueColor = Color(0xFF4CAF50)
        )
        
        DetailRow(
            title = "消极得分",
            value = String.format("%.2f", data.negativeScore),
            valueColor = Color(0xFFF44336)
        )
        
        DetailRow(
            title = "置信度",
            value = String.format("%.2f", data.confidence),
            valueColor = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 详情行
 */
@Composable
private fun DetailRow(
    title: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
} 