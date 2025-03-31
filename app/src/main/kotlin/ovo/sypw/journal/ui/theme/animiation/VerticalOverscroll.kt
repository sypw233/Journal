package ovo.sypw.journal.ui.theme.animiation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
class VerticalOverscroll(
    val scope: CoroutineScope,
    val resistance: Float = 0.5f,
) : OverscrollEffect {

    // 存储当前垂直方向的过度滚动偏移量，使用Animatable实现平滑动画
    // 通过mutableStateOf包装，使Compose可以感知变化并重组UI
    var overscrollY by mutableStateOf(Animatable(0f))

    // 辅助函数：过滤过小的滚动增量（<0.5），避免浮点数精度问题导致滚动卡顿
    private fun Float.isDeltaValid(): Boolean = abs(this) > 0.5

    // 核心方法：处理滚动事件，实现过度滚动效果
    override fun applyToScroll(
        delta: Offset,               // 滚动增量
        source: NestedScrollSource,  // 滚动来源（拖动/惯性）
        performScroll: (Offset) -> Offset  // 实际执行滚动的方法
    ): Offset {
        val deltaY = delta.y  // 提取垂直方向的滚动增量

        // 判断当前滚动方向是否与已有过度滚动方向一致
        val sameDirection = sign(deltaY) == sign(overscrollY.value)

        // 处理反向滚动（优先消耗过度滚动）
        val undoOverscrollDelta = if (overscrollY.value.isDeltaValid() && !sameDirection) {
            val oldOverscrollY = overscrollY.value
            val newOverscrollY = overscrollY.value + deltaY

            // 情况1：过度滚动被完全抵消（符号变化）
            if (sign(oldOverscrollY) != sign(newOverscrollY)) {
                scope.launch { overscrollY.snapTo(0f) }  // 立即归零
                deltaY + oldOverscrollY  // 返回剩余滚动量
            }
            // 情况2：仍有过度滚动需要消耗
            else {
                scope.launch {
                    // 更新过度滚动值（乘以阻力系数）
                    overscrollY.snapTo(overscrollY.value + deltaY * resistance)
                }
                deltaY  // 返回完整增量，阻止实际滚动
            }
        } else {
            0f  // 不需要处理反向滚动
        }

        // 执行实际滚动（扣除已用于消耗过度滚动的部分）
        val adjustedDelta = deltaY - undoOverscrollDelta
        val scrolledDelta = performScroll(Offset(0f, adjustedDelta)).y

        // 计算剩余的过度滚动量（实际滚动未能消耗的部分）
        val overscrollDelta = adjustedDelta - scrolledDelta

        // 如果是拖动事件且存在有效过度滚动量，则应用过度滚动效果
        if (overscrollDelta.isDeltaValid() && source == NestedScrollSource.Drag) {
            scope.launch {
                overscrollY.snapTo(overscrollY.value + overscrollDelta * resistance)
            }
        }

        // 返回最终偏移量（反向滚动消耗量 + 实际滚动量）
        return Offset(0f, undoOverscrollDelta + scrolledDelta)
    }

    // 处理惯性滚动（Fling）事件
    override suspend fun applyToFling(
        velocity: Velocity,  // 当前速度
        performFling: suspend (Velocity) -> Velocity  // 实际执行惯性滚动的方法
    ) {
        // 先让容器内部消费速度
        val consumed = performFling(velocity)
        val remaining = velocity - consumed  // 计算剩余速度

        // 使用剩余速度驱动过度滚动值的归零动画
        overscrollY.animateTo(
            targetValue = 0f,  // 目标值归零
            initialVelocity = remaining.y,  // 初始速度为剩余速度
            animationSpec = tween(
                durationMillis = 500,  // 动画时长500ms
                easing = EaseOutQuad   // 使用缓出曲线
            )
        )
    }

    // 判断当前是否有过度滚动效果
    override val isInProgress: Boolean
        get() = overscrollY.value != 0f

    // 实现视觉效果的Modifier：通过offset动态偏移内容
    override val effectModifier: Modifier = Modifier.offset {
        IntOffset(x = 0, y = overscrollY.value.roundToInt())
    }
}

@OptIn(ExperimentalFoundationApi::class)
class VerticalOverscrollWithChange(
    val scope: CoroutineScope,
    val resistance: Float = 0.5f,
    val onChangeScroll: (scrollOffset: Float) -> Unit,
    val onChangeFling: (remaining: Velocity) -> Unit
) : OverscrollEffect {

    // 存储当前垂直方向的过度滚动偏移量，使用Animatable实现平滑动画
    // 通过mutableStateOf包装，使Compose可以感知变化并重组UI
    var overscrollY by mutableStateOf(Animatable(0f))

    // 辅助函数：过滤过小的滚动增量（<0.5），避免浮点数精度问题导致滚动卡顿
    private fun Float.isDeltaValid(): Boolean = abs(this) > 0.5

    // 核心方法：处理滚动事件，实现过度滚动效果
    override fun applyToScroll(
        delta: Offset,               // 滚动增量
        source: NestedScrollSource,  // 滚动来源（拖动/惯性）
        performScroll: (Offset) -> Offset  // 实际执行滚动的方法
    ): Offset {
        val deltaY = delta.y  // 提取垂直方向的滚动增量

        // 判断当前滚动方向是否与已有过度滚动方向一致
        val sameDirection = sign(deltaY) == sign(overscrollY.value)

        // 处理反向滚动（优先消耗过度滚动）
        val undoOverscrollDelta = if (overscrollY.value.isDeltaValid() && !sameDirection) {
            val oldOverscrollY = overscrollY.value
            val newOverscrollY = overscrollY.value + deltaY

            // 情况1：过度滚动被完全抵消（符号变化）
            if (sign(oldOverscrollY) != sign(newOverscrollY)) {
                scope.launch {
                    overscrollY.snapTo(0f)
                    onChangeScroll(deltaY + oldOverscrollY)
                }  // 立即归零
                deltaY + oldOverscrollY  // 返回剩余滚动量
            }
            // 情况2：仍有过度滚动需要消耗
            else {
                scope.launch {
                    // 更新过度滚动值（乘以阻力系数）
                    overscrollY.snapTo(overscrollY.value + deltaY * resistance)
                    onChangeScroll(deltaY)
                }
                deltaY  // 返回完整增量，阻止实际滚动
            }
        } else {
            0f  // 不需要处理反向滚动
        }

        // 执行实际滚动（扣除已用于消耗过度滚动的部分）
        val adjustedDelta = deltaY - undoOverscrollDelta
        val scrolledDelta = performScroll(Offset(0f, adjustedDelta)).y
        // 计算剩余的过度滚动量（实际滚动未能消耗的部分）
        val overscrollDelta = adjustedDelta - scrolledDelta

        // 如果是拖动事件且存在有效过度滚动量，则应用过度滚动效果
        if (overscrollDelta.isDeltaValid() && source == NestedScrollSource.UserInput) {
            scope.launch {
                overscrollY.snapTo(overscrollY.value + overscrollDelta * resistance)
//                添加跟随滚动效果
                onChangeScroll(overscrollDelta)
            }
        }

        // 返回最终偏移量（反向滚动消耗量 + 实际滚动量）
        return Offset(0f, undoOverscrollDelta + scrolledDelta)
    }

    // 处理惯性滚动（Fling）事件
    override suspend fun applyToFling(
        velocity: Velocity,  // 当前速度
        performFling: suspend (Velocity) -> Velocity  // 实际执行惯性滚动的方法
    ) {
        // 先让容器内部消费速度
        val consumed = performFling(velocity)
        val remaining = velocity - consumed  // 计算剩余速度

        // 使用剩余速度驱动过度滚动值的归零动画
        scope.launch {
            onChangeFling(remaining)
        }

        overscrollY.animateTo(
            targetValue = 0f,  // 目标值归零
            initialVelocity = remaining.y,  // 初始速度为剩余速度
            animationSpec = tween(
                durationMillis = 500,  // 动画时长500ms
                easing = EaseOutQuad   // 使用缓出曲线
            )
        )

    }

    // 判断当前是否有过度滚动效果
    override val isInProgress: Boolean
        get() = overscrollY.value != 0f

    // 实现视觉效果的Modifier：通过offset动态偏移内容
    override val effectModifier: Modifier = Modifier.offset {
        IntOffset(x = 0, y = overscrollY.value.roundToInt())
    }

}