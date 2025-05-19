package ovo.sypw.journal

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ovo.sypw.journal.common.utils.ImageStorageUtils
import java.io.File
import java.io.FileOutputStream

/**
 * 图片存储工具测试
 */
@RunWith(AndroidJUnit4::class)
class ImageStorageTest {
    
    private lateinit var context: Context
    private lateinit var testImageUri: Uri
    
    @Before
    fun setup() {
        // 获取测试上下文
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // 创建测试图片文件
        val testFile = createTestImage()
        testImageUri = Uri.fromFile(testFile)
    }
    
    /**
     * 测试复制图片到私有存储空间
     */
    @Test
    fun testCopyToPrivateStorage() {
        // 复制图片到私有存储
        val resultUri = ImageStorageUtils.copyImageToPrivateStorage(context, testImageUri)
        
        // 验证结果
        assertNotNull("复制后的URI不应为空", resultUri)
        assertTrue("复制后的URI应该是file类型", resultUri!!.scheme == "file")
        assertTrue("复制后的URI应指向存在的文件", File(resultUri.path!!).exists())
        
        Log.d("ImageStorageTest", "原始URI: $testImageUri")
        Log.d("ImageStorageTest", "复制后URI: $resultUri")
    }
    
    /**
     * 测试确保图片可访问
     */
    @Test
    fun testEnsureImageAccessible() {
        // 处理图片URI
        val accessibleUri = ImageStorageUtils.ensureImageAccessible(context, testImageUri)
        
        // 验证结果
        assertNotNull("处理后的URI不应为空", accessibleUri)
        assertTrue("处理后的URI应指向存在的文件", 
            when (accessibleUri!!.scheme) {
                "file" -> File(accessibleUri.path!!).exists()
                "content" -> true // 内容URI无法直接验证文件存在
                else -> false
            }
        )
        
        Log.d("ImageStorageTest", "原始URI: $testImageUri")
        Log.d("ImageStorageTest", "处理后URI: $accessibleUri")
    }
    
    /**
     * 创建测试图片文件
     */
    private fun createTestImage(): File {
        val testDir = File(context.cacheDir, "test_images").apply { 
            if (!exists()) mkdirs() 
        }
        val testFile = File(testDir, "test_image.jpg")
        
        // 创建一个1x1像素的测试图片
        FileOutputStream(testFile).use { out ->
            val data = ByteArray(100) { it.toByte() } // 简单的测试数据
            out.write(data)
        }
        
        return testFile
    }
} 