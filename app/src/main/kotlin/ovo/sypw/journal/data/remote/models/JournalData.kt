package ovo.sypw.journal.data.model

import java.util.Date

/**
 * 日记数据类
 * @property isMark 是否标记
 * @property date 日期
 * @property text 文字内容
 * @property images 图片列表
 * @property location 位置信息
 * @property isMarkdown 是否使用Markdown格式
 */
data class JournalData(
    val id: Int,
    var isMarkdown: Boolean = false,
    val date: Date? = Date(),
    val images: MutableList<Any>? = null,
    val location: LocationData? = null,
    val text: String? = null,
)