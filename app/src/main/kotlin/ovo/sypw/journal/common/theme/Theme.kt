package ovo.sypw.journal.common.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 默认紫色主题
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// 蓝色主题
private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = LightBlue80
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = LightBlue40
)

// 绿色主题
private val GreenDarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = LightGreen80
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = LightGreen40
)

// 紫色主题
private val PurpleDarkColorScheme = darkColorScheme(
    primary = DeepPurple80,
    secondary = PurpleGrey80, 
    tertiary = Indigo80
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = DeepPurple40,
    secondary = PurpleGrey40,
    tertiary = Indigo40
)

// 橙色主题
private val OrangeDarkColorScheme = darkColorScheme(
    primary = Orange80,
    secondary = DeepOrange80,
    tertiary = Amber80
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = Orange40,
    secondary = DeepOrange40,
    tertiary = Amber40
)

// 红色主题
private val RedDarkColorScheme = darkColorScheme(
    primary = Red80,
    secondary = RedGrey80,
    tertiary = Pink80
)

private val RedLightColorScheme = lightColorScheme(
    primary = Red40,
    secondary = RedGrey40,
    tertiary = Pink40
)

// 粉色主题
private val PinkDarkColorScheme = darkColorScheme(
    primary = Pink80,
    secondary = PinkGrey80,
    tertiary = LightPink80
)

private val PinkLightColorScheme = lightColorScheme(
    primary = Pink40,
    secondary = PinkGrey40,
    tertiary = LightPink40
)

// 黄色主题
private val YellowDarkColorScheme = darkColorScheme(
    primary = Yellow80,
    secondary = YellowGrey80,
    tertiary = Amber80
)

private val YellowLightColorScheme = lightColorScheme(
    primary = Yellow40,
    secondary = YellowGrey40,
    tertiary = Amber40
)

// 青色主题
private val CyanDarkColorScheme = darkColorScheme(
    primary = Cyan80,
    secondary = CyanGrey80,
    tertiary = LightBlue80
)

private val CyanLightColorScheme = lightColorScheme(
    primary = Cyan40,
    secondary = CyanGrey40,
    tertiary = LightBlue40
)

// 黑色主题
private val BlackDarkColorScheme = darkColorScheme(
    primary = Black80,
    secondary = BlackGrey80,
    tertiary = DarkGrey80
)

private val BlackLightColorScheme = lightColorScheme(
    primary = Black40,
    secondary = BlackGrey40,
    tertiary = DarkGrey40
)

/**
 * 根据颜色索引获取对应的颜色方案
 */
private fun getColorSchemeForIndex(index: Int, isDark: Boolean): androidx.compose.material3.ColorScheme {
    return when (index) {
        0 -> if (isDark) DarkColorScheme else LightColorScheme // 默认紫色
        1 -> if (isDark) BlueDarkColorScheme else BlueLightColorScheme // 深蓝
        2 -> if (isDark) GreenDarkColorScheme else GreenLightColorScheme // 绿色
        3 -> if (isDark) PurpleDarkColorScheme else PurpleLightColorScheme // 紫色
        4 -> if (isDark) OrangeDarkColorScheme else OrangeLightColorScheme // 橙色
        5 -> if (isDark) RedDarkColorScheme else RedLightColorScheme // 红色
        6 -> if (isDark) PinkDarkColorScheme else PinkLightColorScheme // 粉色
        7 -> if (isDark) YellowDarkColorScheme else YellowLightColorScheme // 黄色
        8 -> if (isDark) CyanDarkColorScheme else CyanLightColorScheme // 青色
        9 -> if (isDark) BlackDarkColorScheme else BlackLightColorScheme // 黑色
        else -> if (isDark) DarkColorScheme else LightColorScheme
    }
}

@Composable
fun JournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // 默认禁用动态颜色，确保我们的自定义颜色方案生效
    colorIndex: Int = 0, // 颜色索引，对应设置中的选择
    content: @Composable () -> Unit
) {
    // 添加日志或注释以确认颜色索引
    // println("JournalTheme: Using colorIndex=$colorIndex, darkTheme=$darkTheme, dynamicColor=$dynamicColor")
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getColorSchemeForIndex(colorIndex, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}