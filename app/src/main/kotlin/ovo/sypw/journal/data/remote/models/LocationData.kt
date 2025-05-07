package ovo.sypw.journal.data.model

/**
 * 位置数据类
 * @property name 位置名称
 * @property latitude 纬度
 * @property longitude 经度
 */
data class LocationData(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)