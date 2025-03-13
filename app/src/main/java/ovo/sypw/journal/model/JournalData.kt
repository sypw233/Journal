package ovo.sypw.journal.model

import android.graphics.Bitmap
import android.location.Location
import java.util.Date

/**
 * 日记数据类
 * @property isMark 是否标记
 * @property date 日期
 * @property text 文字内容
 * @property images 图片列表
 * @property location 位置信息
 */
data class JournalData(
    var isMark: Boolean? = false,
    val date: Date? = Date(),
    val images: MutableList<Bitmap>? = null,
    val location: Location? = null,
    val text: String? = null,
)