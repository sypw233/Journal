package ovo.sypw.journal.utils

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.size.Precision
import okio.Path.Companion.toOkioPath

/**
 * 图片加载工具类
 * 提供全局图片加载配置和优化
 */
object ImageLoadUtils {
    private var imageLoader: ImageLoader? = null

    fun init(context: Context) {
        imageLoader = ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.6) // 使用25%的可用内存作为最大缓存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.1) // 使用2%的可用磁盘空间作为最大缓存
                    .build()
            }
            .precision(Precision.INEXACT)
            .crossfade(true) // 启用淡入淡出效果
            .crossfade(300) // 设置动画时长为300ms
            .memoryCachePolicy(CachePolicy.ENABLED) // 启用内存缓存
            .diskCachePolicy(CachePolicy.ENABLED) // 启用磁盘缓存
            .networkCachePolicy(CachePolicy.ENABLED) // 启用网络缓存
            .allowRgb565(true) // 允许使用RGB565格式，减少内存占用
            .build()
    }

    fun getImageLoader(): ImageLoader {
        return imageLoader ?: throw IllegalStateException("ImageLoader未初始化，请先调用init方法")
    }

    /**
     * 清理内存缓存
     */
    fun clearMemoryCache() {
        imageLoader?.memoryCache?.clear()
    }

    /**
     * 清理磁盘缓存
     */
    fun clearDiskCache() {
        imageLoader?.diskCache?.clear()
    }
}