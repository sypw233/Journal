package ovo.sypw.journal.data.model

import java.util.Date

/**
 * 日记条目请求模型
 * 用于创建或更新日记条目的API请求
 */
data class EntryRequest(
    val text: String? = null,
    val date: Date = Date(),
    val location: LocationData? = null,
    val images: List<String>? = null,  // Base64编码的图片或图片URL
    val isMark: Boolean = false
)

/**
 * 日记条目响应模型
 * 从API获取的日记条目数据
 */
data class Entry(
    val id: Int,
    val text: String? = null,
    val date: Date = Date(),
    val location: LocationData? = null,
    val images: List<String>? = null,  // 图片URL列表
    val isMark: Boolean = false,
    val userId: Int? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) {
    /**
     * 转换为JournalData
     */
    fun toJournalData(): JournalData {
        return JournalData(
            id = id,
            text = text,
            date = date,
            location = location,
            images = images?.toMutableList(),
            isMark = isMark
        )
    }
    
    companion object {
        /**
         * 从JournalData创建Entry
         */
        fun fromJournalData(journalData: JournalData): Entry {
            return Entry(
                id = journalData.id,
                text = journalData.text,
                date = journalData.date ?: Date(),
                location = journalData.location,
                images = journalData.images?.map { it.toString() },
                isMark = journalData.isMark == true
            )
        }
    }
}