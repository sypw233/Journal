package ovo.sypw.journal.common.theme

import androidx.compose.ui.graphics.Color

/**
 * 情感分析相关的颜色常量
 * 用于确保整个应用中情感分析相关的颜色统一
 */
object SentimentColors {
    // 主要颜色
    val POSITIVE = Color(0xFF4CAF50) // 积极情感 - 绿色
    val NEGATIVE = Color(0xFFF44336) // 消极情感 - 红色
    val NEUTRAL = Color(0xFFFFB74D)  // 中性情感 - 黄色
    val UNKNOWN = Color(0xFF9E9E9E)  // 未知情感 - 灰色

    // 浅色背景（用于卡片、进度条背景等）
    val POSITIVE_LIGHT = Color(0x1F4CAF50) // 浅绿色背景 (12% 透明度)
    val NEGATIVE_LIGHT = Color(0x1FF44336) // 浅红色背景 (12% 透明度)
    val NEUTRAL_LIGHT = Color(0x1FFFB74D)  // 浅黄色背景 (12% 透明度)
    val UNKNOWN_LIGHT = Color(0x1F9E9E9E)  // 浅灰色背景 (12% 透明度)

    // 进度条背景
    val PROGRESS_TRACK = Color(0xFFEBEBEB) // 进度条背景色

    // 获取情感类型对应的颜色
    fun getColorForSentiment(isPositive: Boolean, isNegative: Boolean, isNeutral: Boolean): Color {
        return when {
            isPositive -> POSITIVE
            isNegative -> NEGATIVE
            isNeutral -> NEUTRAL
            else -> UNKNOWN
        }
    }

    // 获取情感类型对应的浅色背景
    fun getLightColorForSentiment(
        isPositive: Boolean,
        isNegative: Boolean,
        isNeutral: Boolean
    ): Color {
        return when {
            isPositive -> POSITIVE_LIGHT
            isNegative -> NEGATIVE_LIGHT
            isNeutral -> NEUTRAL_LIGHT
            else -> UNKNOWN_LIGHT
        }
    }
} 