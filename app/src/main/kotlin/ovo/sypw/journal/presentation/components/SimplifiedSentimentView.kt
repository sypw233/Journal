package ovo.sypw.journal.presentation.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ovo.sypw.journal.common.theme.SentimentColors
import ovo.sypw.journal.common.utils.SentimentType
import ovo.sypw.journal.data.model.SentimentData

/**
 * 简化版情感分析结果视图
 * 直接显示情感分析结果，不使用卡片嵌套
 */
@Composable
fun SimplifiedSentimentView(
    sentimentData: SentimentData
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 情感图标和类型
        SentimentHeader(sentimentData)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 情感得分条
        SentimentProgressBars(sentimentData)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 情感类型和主要情绪
        SentimentTypeRow(sentimentData)
    }
}

@Composable
private fun SentimentHeader(data: SentimentData) {
    // 根据情感类型选择图标和颜色
    val (icon, color) = when (data.sentimentType) {
        SentimentType.POSITIVE -> Pair(
            Icons.Default.SentimentSatisfied, 
            SentimentColors.POSITIVE
        )
        SentimentType.NEGATIVE -> Pair(
            Icons.Default.SentimentVeryDissatisfied, 
            SentimentColors.NEGATIVE
        )
        SentimentType.NEUTRAL -> Pair(
            Icons.Default.SentimentSatisfied, 
            SentimentColors.NEUTRAL
        )
        else -> Pair(
            Icons.Default.SentimentSatisfied, 
            SentimentColors.UNKNOWN
        )
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = data.getSentimentDescription(),
            tint = color,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Text(
            text = data.getSentimentDescription(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun SentimentProgressBars(data: SentimentData) {
    // 动画进度值
    val positiveProgress by animateFloatAsState(
        targetValue = data.positiveScore, 
        label = "positive"
    )
    val negativeProgress by animateFloatAsState(
        targetValue = data.negativeScore, 
        label = "negative"
    )
    
    // 根据情感类型确定主要和次要进度条的标签和颜色
    val isNegativeType = data.sentimentType == SentimentType.NEGATIVE
    
    // 第一个进度条（主要情感）
    val firstLabel = if (isNegativeType) "消极" else "积极" 
    val firstColor = if (isNegativeType) SentimentColors.NEGATIVE else SentimentColors.POSITIVE
    val firstValue = positiveProgress  // 无论哪种类型，正面得分始终是 positiveProgress
    
    // 第二个进度条（次要情感）
    val secondLabel = if (isNegativeType) "积极" else "消极"
    val secondColor = if (isNegativeType) SentimentColors.POSITIVE else SentimentColors.NEGATIVE
    val secondValue = negativeProgress  // 无论哪种类型，负面得分始终是 negativeProgress
    
    // 主要情感进度条
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = firstLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = firstColor,
            modifier = Modifier.width(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { firstValue },
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = firstColor,
            trackColor = firstColor.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(firstValue * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 次要情感进度条
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = secondLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = secondColor,
            modifier = Modifier.width(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { secondValue },
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = secondColor,
            trackColor = secondColor.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(secondValue * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SentimentTypeRow(data: SentimentData) {
    // 获取情感类型颜色
    val sentimentColor = when (data.sentimentType) {
        SentimentType.POSITIVE -> SentimentColors.POSITIVE
        SentimentType.NEGATIVE -> SentimentColors.NEGATIVE
        SentimentType.NEUTRAL -> SentimentColors.NEUTRAL
        else -> SentimentColors.UNKNOWN
    }
    
    // 情感类型行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 根据情感类型选择图标
            val typeIcon = when (data.sentimentType) {
                SentimentType.POSITIVE -> Icons.Default.SentimentSatisfied
                SentimentType.NEGATIVE -> Icons.Default.SentimentVeryDissatisfied
                else -> Icons.Default.SentimentSatisfied
            }
            
            Icon(
                imageVector = typeIcon,
                contentDescription = "情感类型",
                tint = sentimentColor,
                modifier = Modifier.padding(end = 4.dp)
            )
            
            Text(
                text = "情感类型",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = data.getSentimentDescription(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = sentimentColor
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 如果有主要情绪，显示主要情绪
    if (data.dominantEmotion.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "主要情绪",
                    tint = sentimentColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "主要情绪",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = data.dominantEmotion,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = sentimentColor
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 根据情感类型决定得分显示顺序
    val isNegativeType = data.sentimentType == SentimentType.NEGATIVE
    
    // 主要得分
    val firstScoreLabel = if (isNegativeType) "消极得分" else "积极得分"
    val firstScoreValue = data.positiveScore  // 无论哪种类型，正面得分始终是 data.positiveScore
    val firstScoreColor = if (isNegativeType) SentimentColors.NEGATIVE else SentimentColors.POSITIVE
    
    // 次要得分
    val secondScoreLabel = if (isNegativeType) "积极得分" else "消极得分"
    val secondScoreValue = data.negativeScore  // 无论哪种类型，负面得分始终是 data.negativeScore
    val secondScoreColor = if (isNegativeType) SentimentColors.POSITIVE else SentimentColors.NEGATIVE
    
    // 主要得分行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = firstScoreLabel,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = String.format("%.2f", firstScoreValue),
            style = MaterialTheme.typography.bodyMedium,
            color = firstScoreColor
        )
    }
    
    Spacer(modifier = Modifier.height(4.dp))
    
    // 次要得分行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = secondScoreLabel,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = String.format("%.2f", secondScoreValue),
            style = MaterialTheme.typography.bodyMedium,
            color = secondScoreColor
        )
    }
} 