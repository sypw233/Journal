package ovo.sypw.journal.utils

import android.content.Context
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import kotlinx.coroutines.suspendCancellableCoroutine
import ovo.sypw.journal.data.APIKey.API_KEY
import ovo.sypw.journal.model.LocationData
import kotlin.coroutines.resume

/**
 * 高德地图定位工具类
 * 封装了高德地图的定位API，提供获取用户位置信息的功能
 */
class GetLocation {
    companion object {
        private const val TAG = "GetLocation"


        /**
         * 初始化AMapLocationClient
         * 必须在使用定位功能前调用此方法
         * @param context 应用上下文
         */
        fun initLocationClient(context: Context) {
            try {
                // 设置高德隐私政策同意
                AMapLocationClient.updatePrivacyShow(context, true, true)
                AMapLocationClient.updatePrivacyAgree(context, true)
                AMapLocationClient.setApiKey(API_KEY)
            } catch (e: Exception) {
                SnackBarUtils.showSnackBar("初始化定位客户端失败: ${e.message}")
            }
        }

        /**
         * 获取当前位置信息
         * @param context 应用上下文
         * @param onSuccess 成功回调，返回LocationData对象
         * @param onError 错误回调，返回错误信息
         */
        fun getCurrentLocation(
            context: Context,
            onSuccess: (LocationData) -> Unit,
            onError: (String) -> Unit
        ) {
            try {
                // 创建定位客户端
                val locationClient = AMapLocationClient(context)

                // 配置定位参数
                val locationOption = AMapLocationClientOption().apply {
                    // 设置定位模式为高精度模式
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    // 设置是否返回地址信息
                    isNeedAddress = true
                    // 设置是否单次定位
                    isOnceLocation = true
                    // 设置是否允许模拟位置
                    isMockEnable = false
                    // 设置定位请求超时时间，默认为30秒
                    httpTimeOut = 20000
                    // 设置定位间隔，默认为2秒
                    interval = 2000
                }

                // 设置定位回调监听
                locationClient.setLocationListener { location ->
                    if (location != null) {
                        if (location.errorCode == 0) {
                            // 定位成功，返回位置信息
                            val locationData = LocationData(
                                name = location.address,
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            onSuccess(locationData)
                        } else {
                            // 定位失败，返回错误信息
                            onError("定位失败: ${location.errorInfo}, 错误码: ${location.errorCode}")
                        }
                    } else {
                        onError("定位失败: 未知错误")
                    }

                    // 停止定位
                    locationClient.stopLocation()
                    // 销毁定位客户端
                    locationClient.onDestroy()
                }

                // 设置定位参数
                locationClient.setLocationOption(locationOption)
                // 启动定位
                locationClient.startLocation()
            } catch (e: Exception) {
                onError("定位异常: ${e.message}")
                Log.e(TAG, "定位异常", e)
            }
        }

        /**
         * 获取当前位置信息（协程版本）
         * @param context 应用上下文
         * @return 返回LocationData对象，如果定位失败则返回null
         */
        suspend fun getCurrentLocationCoroutine(context: Context): Result<LocationData> {
            return suspendCancellableCoroutine { continuation ->
                getCurrentLocation(
                    context = context,
                    onSuccess = { locationData ->
                        if (continuation.isActive) {
                            continuation.resume(Result.success(locationData))
                        }
                    },
                    onError = { errorMsg ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception(errorMsg)))
                        }
                    }
                )

                // 取消时的处理
                continuation.invokeOnCancellation {
                    // 可以在这里添加取消定位的逻辑
                }
            }
        }
    }
}