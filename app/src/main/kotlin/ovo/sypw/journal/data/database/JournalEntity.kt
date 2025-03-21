package ovo.sypw.journal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.model.LocationData
import java.util.Date

/**
 * 日记实体类，用于Room数据库存储
 */
@Entity(tableName = "journals")
@TypeConverters(JournalConverters::class)
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val isMark: Boolean = false,
    val date: Date = Date(),
    val text: String? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // 图片列表以JSON字符串形式存储
    val imagesJson: String? = null
) {
    /**
     * 转换为JournalData模型
     */
    fun toJournalData(): JournalData {
        val location = if (locationName != null || latitude != null || longitude != null) {
            LocationData(locationName, latitude, longitude)
        } else null

        val images = imagesJson?.let {
            try {
                // 解析存储的图片数据，可能是资源ID(Int)或URI字符串
                if (it.isNotEmpty()) {
                    it.split(",").mapNotNull { str ->
                        // 尝试解析为整数（资源ID），如果失败则保留原始字符串（URI）
                        str.toIntOrNull() ?: str
                    }.toMutableList()
                } else null
            } catch (e: Exception) {
                null
            }
        }

        return JournalData(
            id = id,
            isMark = isMark,
            date = date,
            text = text,
            location = location,
            images = images as MutableList<Any>?
        )
    }

    companion object {
        /**
         * 从JournalData创建JournalEntity
         */
        fun fromJournalData(journalData: JournalData): JournalEntity {
            return JournalEntity(
                id = journalData.id,
                isMark = journalData.isMark ?: false,
                date = journalData.date ?: Date(),
                text = journalData.text,
                locationName = journalData.location?.name,
                latitude = journalData.location?.latitude,
                longitude = journalData.location?.longitude,
                imagesJson = journalData.images?.joinToString(",") { image ->
                    // 确保URI字符串和资源ID都能正确存储
                    when (image) {
                        is Int -> image.toString()
                        else -> image.toString()
                    }
                }
            )
        }
    }
}

/**
 * 类型转换器，用于Room数据库存储复杂类型
 */
class JournalConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}