package ovo.sypw.journal.common.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * 数据库导出调试工具类
 * 用于诊断数据库导出相关问题
 */
object DatabaseExportDebugger {
    private const val TAG = "DBExportDebugger"

    /**
     * 检查数据库导出环境并记录详细日志
     * 
     * @param context 上下文
     * @param databaseName 数据库名称
     * @return 环境检查结果摘要
     */
    fun checkExportEnvironment(context: Context, databaseName: String = "journal_database"): String {
        val logBuilder = StringBuilder()
        var summary = "正常"
        
        try {
            // 1. 检查数据库文件是否存在
            val dbFile = context.getDatabasePath(databaseName)
            val dbExists = dbFile.exists()
            val dbSize = if (dbExists) "${dbFile.length() / 1024} KB" else "不存在"
            val dbCanRead = if (dbExists) dbFile.canRead() else false
            
            logAndAppend(logBuilder, "数据库文件路径: ${dbFile.absolutePath}")
            logAndAppend(logBuilder, "数据库文件是否存在: $dbExists")
            logAndAppend(logBuilder, "数据库文件大小: $dbSize")
            logAndAppend(logBuilder, "数据库文件可读: $dbCanRead")
            
            if (!dbExists) {
                summary = "数据库文件不存在"
            } else if (!dbCanRead) {
                summary = "数据库文件不可读"
            }
            
            // 2. 检查导出目录
            val exportDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "database_exports"
            )
            val exportDirExists = exportDir.exists()
            val exportDirCanWrite = if (exportDirExists) exportDir.canWrite() else false
            
            logAndAppend(logBuilder, "导出目录路径: ${exportDir.absolutePath}")
            logAndAppend(logBuilder, "导出目录是否存在: $exportDirExists")
            logAndAppend(logBuilder, "导出目录可写: $exportDirCanWrite")
            
            if (!exportDirExists) {
                // 尝试创建目录
                val created = exportDir.mkdirs()
                logAndAppend(logBuilder, "尝试创建导出目录: $created")
                
                if (created) {
                    // 重新检查写入权限
                    val canWriteAfterCreate = exportDir.canWrite()
                    logAndAppend(logBuilder, "创建后导出目录可写: $canWriteAfterCreate")
                    
                    if (!canWriteAfterCreate) {
                        summary = "导出目录不可写"
                    }
                } else {
                    summary = "无法创建导出目录"
                }
            } else if (!exportDirCanWrite) {
                summary = "导出目录不可写"
            }
            
            // 3. 检查应用外部存储状态
            val externalFilesDir = context.getExternalFilesDir(null)
            logAndAppend(logBuilder, "应用外部存储目录: ${externalFilesDir?.absolutePath ?: "不可用"}")
            logAndAppend(logBuilder, "外部存储状态: ${Environment.getExternalStorageState()}")
            
            // 4. 检查内部存储空间
            val internalDir = context.filesDir
            val internalFreeSpace = internalDir.freeSpace / (1024 * 1024)
            logAndAppend(logBuilder, "内部存储空闲空间: $internalFreeSpace MB")
            
            // 5. 检查外部存储空间
            val externalFreeSpace = externalFilesDir?.freeSpace?.div(1024 * 1024) ?: 0
            logAndAppend(logBuilder, "外部存储空闲空间: $externalFreeSpace MB")
            
            if (externalFreeSpace < 10) {
                summary = "外部存储空间不足"
            }
            
            // 6. 列出已存在的导出文件
            if (exportDirExists) {
                val exportedFiles = exportDir.listFiles()?.filter { it.isFile && it.name.endsWith(".db") } ?: emptyList()
                logAndAppend(logBuilder, "已存在的导出文件数: ${exportedFiles.size}")
                
                exportedFiles.forEachIndexed { index, file ->
                    logAndAppend(logBuilder, "导出文件 #${index + 1}: ${file.name}, 大小: ${file.length() / 1024} KB")
                }
            }
            
            // 7. 检查临时文件目录
            val tempDir = context.cacheDir
            val tempDirCanWrite = tempDir.canWrite()
            logAndAppend(logBuilder, "临时目录路径: ${tempDir.absolutePath}")
            logAndAppend(logBuilder, "临时目录可写: $tempDirCanWrite")
            
            if (!tempDirCanWrite) {
                summary = "临时目录不可写"
            }
        } catch (e: Exception) {
            val errorMsg = "检查导出环境时出错: ${e.message}"
            logAndAppend(logBuilder, errorMsg)
            summary = "检查环境出错"
            Log.e(TAG, errorMsg, e)
        }
        
        // 输出完整日志到控制台
        Log.i(TAG, "数据库导出环境检查完成，结果: $summary\n${logBuilder}")
        
        return summary
    }
    
    /**
     * 测试文件写入
     * 
     * @param context 上下文
     * @return 写入测试结果
     */
    fun testFileWrite(context: Context): String {
        val logBuilder = StringBuilder()
        var result = "成功"
        
        try {
            // 1. 测试内部存储写入
            val internalDir = context.filesDir
            val internalTestFile = File(internalDir, "db_test_internal.txt")
            
            try {
                internalTestFile.writeText("测试内部存储写入 - ${System.currentTimeMillis()}")
                logAndAppend(logBuilder, "内部存储写入测试成功: ${internalTestFile.absolutePath}")
                internalTestFile.delete() // 清理测试文件
            } catch (e: Exception) {
                logAndAppend(logBuilder, "内部存储写入测试失败: ${e.message}")
                result = "内部存储写入失败"
            }
            
            // 2. 测试应用专属外部存储写入
            val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (externalDir != null) {
                val externalTestFile = File(externalDir, "db_test_external.txt")
                
                try {
                    externalTestFile.writeText("测试外部存储写入 - ${System.currentTimeMillis()}")
                    logAndAppend(logBuilder, "应用专属外部存储写入测试成功: ${externalTestFile.absolutePath}")
                    externalTestFile.delete() // 清理测试文件
                } catch (e: Exception) {
                    logAndAppend(logBuilder, "应用专属外部存储写入测试失败: ${e.message}")
                    result = "应用专属外部存储写入失败"
                }
            } else {
                logAndAppend(logBuilder, "应用专属外部存储不可用")
                result = "应用专属外部存储不可用"
            }
            
            // 3. 测试导出目录写入
            val exportDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "database_exports"
            )
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            if (exportDir.exists()) {
                val exportTestFile = File(exportDir, "db_test_export.txt")
                
                try {
                    exportTestFile.writeText("测试导出目录写入 - ${System.currentTimeMillis()}")
                    logAndAppend(logBuilder, "导出目录写入测试成功: ${exportTestFile.absolutePath}")
                    exportTestFile.delete() // 清理测试文件
                } catch (e: Exception) {
                    logAndAppend(logBuilder, "导出目录写入测试失败: ${e.message}")
                    result = "导出目录写入失败"
                }
            } else {
                logAndAppend(logBuilder, "导出目录创建失败")
                result = "导出目录创建失败"
            }
            
            // 4. 测试缓存目录写入
            val cacheDir = context.cacheDir
            val cacheTestFile = File(cacheDir, "db_test_cache.txt")
            
            try {
                cacheTestFile.writeText("测试缓存目录写入 - ${System.currentTimeMillis()}")
                logAndAppend(logBuilder, "缓存目录写入测试成功: ${cacheTestFile.absolutePath}")
                cacheTestFile.delete() // 清理测试文件
            } catch (e: Exception) {
                logAndAppend(logBuilder, "缓存目录写入测试失败: ${e.message}")
                result = "缓存目录写入失败"
            }
        } catch (e: Exception) {
            val errorMsg = "文件写入测试出错: ${e.message}"
            logAndAppend(logBuilder, errorMsg)
            result = "文件写入测试出错"
            Log.e(TAG, errorMsg, e)
        }
        
        // 输出完整日志到控制台
        Log.i(TAG, "文件写入测试完成，结果: $result\n${logBuilder}")
        
        return result
    }
    
    /**
     * 记录日志并追加到StringBuilder
     */
    private fun logAndAppend(builder: StringBuilder, message: String) {
        Log.d(TAG, message)
        builder.append("- ").append(message).append("\n")
    }
} 