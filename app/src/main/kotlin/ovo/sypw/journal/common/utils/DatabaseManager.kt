package ovo.sypw.journal.common.utils

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ovo.sypw.journal.data.remote.api.FileService
import ovo.sypw.journal.data.database.JournalDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库比较结果
 */
data class DatabaseCompareInfo(
    val localEntryCount: Int,
    val remoteEntryCount: Int
)

/**
 * 数据库管理器工具类
 * 处理数据库的导出、上传、下载等操作
 */
@Singleton
class DatabaseManager @Inject constructor(
    private val context: Context,
    private val fileService: FileService
) {
    companion object {
        private const val TAG = "DatabaseManager"
        private const val DB_NAME = "journal_database"
    }

    // 保存一个数据库实例的弱引用
    private var dbInstance: JournalDatabase? = null

    /**
     * 设置数据库实例引用，用于在恢复数据库时关闭连接
     */
    fun setDatabaseInstance(db: JournalDatabase) {
        dbInstance = db
    }

    /**
     * 导出数据库
     * 将数据库导出到应用专属存储空间
     *
     * @return 导出的文件，如果失败则返回null
     */
    suspend fun exportDatabase(): File? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始导出数据库")
            
            // 首先使用调试工具检查环境
            val envCheckResult = DatabaseExportDebugger.checkExportEnvironment(context, DB_NAME)
            Log.d(TAG, "环境检查结果: $envCheckResult")
            
            // 如果环境检查结果不正常，进行文件写入测试
            if (envCheckResult != "正常") {
                val writeTestResult = DatabaseExportDebugger.testFileWrite(context)
                Log.d(TAG, "文件写入测试结果: $writeTestResult")
            }
            
            // 创建导出目录（应用专属外部存储）
            val exportDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "database_exports"
            )
            if (!exportDir.exists()) {
                val created = exportDir.mkdirs()
                Log.d(TAG, "创建导出目录: $created")
                
                if (!created) {
                    Log.e(TAG, "无法创建导出目录: ${exportDir.absolutePath}")
                    return@withContext null
                }
            }

            // 创建带时间戳的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "${DB_NAME}_$timestamp.db")
            
            // 获取数据库文件
            val dbFile = context.getDatabasePath(DB_NAME)
            
            Log.d(TAG, "数据库路径: ${dbFile.absolutePath}")
            Log.d(TAG, "导出路径: ${exportFile.absolutePath}")
            
            if (!dbFile.exists()) {
                Log.e(TAG, "数据库文件不存在")
                return@withContext null
            }
            
            // 记录更多关于数据库文件的信息
            Log.d(TAG, "数据库文件大小: ${dbFile.length() / 1024} KB")
            Log.d(TAG, "数据库文件可读: ${dbFile.canRead()}")
            
            // 记录导出目录信息
            Log.d(TAG, "导出目录可写: ${exportDir.canWrite()}")
            Log.d(TAG, "导出目录可读: ${exportDir.canRead()}")
            
            // 注意：我们不再关闭数据库连接，以避免干扰Room的运行
            // 而是采用安全的数据库文件复制方式
            
            // =========== 使用文件复制方式导出数据库 ===========
            Log.d(TAG, "使用文件复制方式导出数据库")
            
            // 等待短暂时间，确保所有写操作完成
            delay(300)
            
            // 尝试复制数据库文件到临时位置
            val tempDbFile = File(exportDir, "temp_${timestamp}.db")
            
            try {
                // 使用文件流复制，以便更好地处理文件锁定情况
                var success = false
                
                try {
                    Log.d(TAG, "开始使用文件流复制数据库 ${dbFile.absolutePath} -> ${tempDbFile.absolutePath}")
                    
                    FileInputStream(dbFile).use { input ->
                        if (input == null) {
                            Log.e(TAG, "无法创建数据库文件的输入流")
                            return@withContext null
                        }
                        
                        FileOutputStream(tempDbFile).use { output ->
                            if (output == null) {
                                Log.e(TAG, "无法创建临时文件的输出流")
                                return@withContext null
                            }
                            
                            val buffer = ByteArray(1024)
                            var length: Int
                            var totalBytesRead = 0L
                            
                            try {
                                while (input.read(buffer).also { length = it } > 0) {
                                    output.write(buffer, 0, length)
                                    totalBytesRead += length
                                }
                                Log.d(TAG, "数据库文件复制完成，共复制 $totalBytesRead 字节")
                                success = true
                            } catch (e: Exception) {
                                Log.e(TAG, "复制数据库文件时读写错误", e)
                            }
                        }
                    }
                    
                    if (success) {
                        Log.d(TAG, "成功复制数据库文件到临时位置")
                    } else {
                        Log.e(TAG, "复制数据库文件到临时位置失败")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "复制数据库文件到临时位置失败", e)
                }
                
                if (!success) {
                    // 如果文件流复制失败，尝试使用标准复制方法
                    try {
                        Log.d(TAG, "尝试使用标准方法复制数据库文件")
                        dbFile.copyTo(tempDbFile, overwrite = true)
                        success = true
                        Log.d(TAG, "使用标准方法成功复制数据库文件到临时位置")
                    } catch (e: Exception) {
                        Log.e(TAG, "使用标准方法复制数据库文件到临时位置失败", e)
                    }
                }
                
                if (success) {
                    // 将临时文件复制到最终位置
                    try {
                        Log.d(TAG, "开始将临时文件复制到最终位置 ${tempDbFile.absolutePath} -> ${exportFile.absolutePath}")
                        tempDbFile.copyTo(exportFile, overwrite = true)
                        Log.d(TAG, "成功将临时文件复制到最终位置")
                    } catch (e: Exception) {
                        Log.e(TAG, "将临时文件复制到最终位置失败", e)
                        return@withContext null
                    }
                    
                    // 验证导出的数据库（只读模式打开，不影响原数据库）
                    try {
                        Log.d(TAG, "开始验证导出的数据库")
                        val isValid = verifyExportedDatabase(exportFile)
                        
                        if (isValid) {
                            // 导出成功
                            Log.d(TAG, "数据库导出成功")
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "数据库已导出到: ${exportFile.absolutePath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            
                            // 清理临时文件
                            if (tempDbFile.exists()) {
                                val deleted = tempDbFile.delete()
                                Log.d(TAG, "清理临时文件: $deleted")
                            }
                            
                            return@withContext exportFile
                        } else {
                            Log.e(TAG, "导出的数据库验证失败")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "验证导出的数据库时出错", e)
                    }
                } else {
                    Log.e(TAG, "复制数据库文件失败，无法继续导出过程")
                }
                
                // 清理临时文件
                if (tempDbFile.exists()) {
                    val deleted = tempDbFile.delete()
                    Log.d(TAG, "清理临时文件: $deleted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "导出数据库过程中出现异常", e)
            }
            
            // 如果以上方法都失败，尝试使用替代导出方法
            Log.d(TAG, "标准导出方法失败，尝试使用替代方法")
            val alternativeSuccess = tryAlternativeExport(dbFile, exportFile)
            
            if (alternativeSuccess) {
                Log.d(TAG, "使用替代方法成功导出数据库")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "数据库已导出到: ${exportFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext exportFile
            }
            
            // 如果以上方法都失败，尝试直接使用SAF框架导出
            try {
                Log.d(TAG, "尝试使用SAF框架导出数据库")
                val outputStream = FileOutputStream(exportFile)
                val inputStream = FileInputStream(dbFile)
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                // 验证导出的数据库
                val isValid = verifyExportedDatabase(exportFile)
                if (isValid) {
                    Log.d(TAG, "使用SAF框架成功导出数据库")
                    return@withContext exportFile
                } else {
                    Log.e(TAG, "使用SAF框架导出的数据库验证失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "使用SAF框架导出数据库失败", e)
            }
            
            // 如果以上方法都失败，返回null
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出数据库失败", Toast.LENGTH_SHORT).show()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "导出数据库过程中出现异常", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }
    
    /**
     * 验证导出的数据库
     */
    private fun verifyExportedDatabase(dbFile: File): Boolean {
        try {
            Log.d(TAG, "开始验证导出数据库: ${dbFile.absolutePath}, 文件大小: ${dbFile.length() / 1024} KB")
            
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath, 
                null, 
                SQLiteDatabase.OPEN_READONLY
            )
            
            // 记录数据库文件结构信息
            val tables = mutableListOf<String>()
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'",
                null
            )
            
            Log.d(TAG, "数据库表结构:")
            while (cursor.moveToNext()) {
                val tableName = cursor.getString(0)
                tables.add(tableName)
                Log.d(TAG, "表: $tableName")
                
                // 获取表结构
                try {
                    val tableInfoCursor = db.rawQuery("PRAGMA table_info($tableName)", null)
                    val columns = mutableListOf<String>()
                    while (tableInfoCursor.moveToNext()) {
                        val columnName = tableInfoCursor.getString(1)
                        val columnType = tableInfoCursor.getString(2)
                        columns.add("$columnName ($columnType)")
                    }
                    tableInfoCursor.close()
                    Log.d(TAG, "表 $tableName 结构: ${columns.joinToString(", ")}")
                    
                    // 取样检查表中的数据
                    try {
                        val sampleCursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
                        if (sampleCursor.moveToFirst()) {
                            val count = sampleCursor.getInt(0)
                            Log.d(TAG, "表 $tableName 中的记录数: $count")
                            
                            // 如果表有数据，取出几条记录做样本
                            if (count > 0) {
                                val dataSampleCursor = db.rawQuery("SELECT * FROM $tableName LIMIT 2", null)
                                if (dataSampleCursor.moveToFirst()) {
                                    val columnCount = dataSampleCursor.columnCount
                                    Log.d(TAG, "表 $tableName 样本数据 (前2条):")
                                    do {
                                        val rowData = StringBuilder()
                                        for (i in 0 until columnCount) {
                                            rowData.append("${dataSampleCursor.getColumnName(i)}: ")
                                            try {
                                                val value = dataSampleCursor.getString(i)
                                                rowData.append(if (value != null) {
                                                    if (value.length > 50) value.substring(0, 50) + "..." else value
                                                } else "null")
                                            } catch (e: Exception) {
                                                rowData.append("(无法读取)")
                                            }
                                            if (i < columnCount - 1) rowData.append(", ")
                                        }
                                        Log.d(TAG, "行数据: $rowData")
                                    } while (dataSampleCursor.moveToNext())
                                }
                                dataSampleCursor.close()
                            }
                        }
                        sampleCursor.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "读取表 $tableName 数据时出错", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "获取表 $tableName 结构时出错", e)
                }
            }
            cursor.close()
            
            if (tables.isEmpty()) {
                Log.e(TAG, "导出的数据库没有用户表")
            }
            
            // 计算记录总数
            val count = countRecords(db)
            db.close()
            
            Log.d(TAG, "导出的数据库共有 $count 条记录")
            
            // 验证文件大小是否正常
            val expectedMinSize = 10 * 1024 // 预期最小10KB
            if (dbFile.length() < expectedMinSize && count > 10) {
                Log.w(TAG, "警告: 数据库文件大小(${dbFile.length() / 1024}KB)小于预期(${expectedMinSize / 1024}KB)，但包含 $count 条记录")
            }
            
            return count > 0
        } catch (e: Exception) {
            Log.e(TAG, "验证导出的数据库失败", e)
            return false
        }
    }
    
    /**
     * 计算数据库中的记录总数
     */
    private fun countRecords(db: SQLiteDatabase): Int {
        var totalCount = 0
        try {
            // 获取所有表
            val tables = mutableListOf<String>()
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'",
                null
            )
            
            while (cursor.moveToNext()) {
                val tableName = cursor.getString(0)
                tables.add(tableName)
            }
            cursor.close()
            
            // 计算每个表的记录数
            for (table in tables) {
                try {
                    val countCursor = db.rawQuery("SELECT COUNT(*) FROM $table", null)
                    if (countCursor.moveToFirst()) {
                        val count = countCursor.getInt(0)
                        totalCount += count
                    }
                    countCursor.close()
                } catch (e: Exception) {
                    Log.e(TAG, "计算表 $table 的记录数时出错", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "计算记录总数时出错", e)
        }
        return totalCount
    }

    /**
     * 获取导出的数据库文件列表
     *
     * @return 导出的数据库文件列表
     */
    fun getExportedDatabaseFiles(): List<File> {
        val exportDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "database_exports")
        return if (exportDir.exists()) {
            exportDir.listFiles()?.filter { it.isFile && it.name.endsWith(".db") } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 删除导出的数据库文件
     *
     * @param file 要删除的文件
     * @return 是否删除成功
     */
    fun deleteExportedFile(file: File): Boolean {
        return file.delete().also { success ->
            Log.i(
                TAG,
                if (success) "文件已删除: ${file.absolutePath}" else "删除文件失败: ${file.absolutePath}"
            )
        }
    }

    /**
     * 上传数据库到服务器
     *
     * @param file 要上传的数据库文件，如果为null则自动导出当前数据库再上传
     * @param remotePath 上传到服务器的路径（可选）
     * @return 上传结果
     */
    suspend fun uploadDatabaseToServer(
        file: File? = null,
        remotePath: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 如果没有提供文件，则先导出当前数据库
            val fileToUpload = file ?: exportDatabase() ?: return@withContext Result.failure(
                Exception("无法导出数据库")
            )

            // 上传文件
            val result = fileService.uploadDatabaseFile(fileToUpload, remotePath)

            if (result.isSuccess) {
                val response = result.getOrThrow()
                val message = "数据库已上传到: ${response.path}"
                Log.i(TAG, message)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//                }
                Result.success(response.path)
            } else {
                val exception = result.exceptionOrNull() ?: Exception("上传失败")
                Log.e(TAG, "上传数据库失败", exception)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(context, "上传失败: ${exception.message}", Toast.LENGTH_SHORT)
//                        .show()
//                }
                Result.failure(exception)
            }
        } catch (e: Exception) {
            Log.e(TAG, "上传数据库失败", e)
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
            Result.failure(e)
        }
    }

    /**
     * 从服务器下载数据库
     *
     * @param remotePath 服务器上的数据库文件路径
     * @return 下载的文件，如果失败则返回null
     */
    suspend fun downloadDatabaseFromServer(remotePath: String): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                // 创建下载目录
                val downloadDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "database_downloads"
                )
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                // 下载文件
                val result = fileService.downloadDatabaseFile(remotePath, downloadDir)

                if (result.isSuccess) {
                    val file = result.getOrThrow()
                    val message = "数据库已下载到: ${file.absolutePath}"
                    Log.i(TAG, message)
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//                    }
                    Result.success(file)
                } else {
                    val exception = result.exceptionOrNull() ?: Exception("下载失败")
                    Log.e(TAG, "下载数据库失败", exception)
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            context,
//                            "下载失败: ${exception.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
                    Result.failure(exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载数据库失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Result.failure(e)
            }
        }

    /**
     * 关闭数据库连接
     */
    private fun closeDatabase() {
        try {
            // 注意：这个方法不应该在正常同步过程中调用
            // 仅在必要时（如恢复数据库前）才调用
            dbInstance?.let { db ->
                try {
                    db.close()
                    Log.i(TAG, "成功关闭数据库实例")
                } catch (e: Exception) {
                    Log.e(TAG, "关闭数据库实例失败", e)
                }
            }
            
            // 设置 dbInstance 为 null，让垃圾回收器处理它
            dbInstance = null
        } catch (e: Exception) {
            Log.e(TAG, "关闭数据库连接时出错", e)
        }
    }

    /**
     * 从下载的文件恢复数据库
     *
     * @param file 下载的数据库文件
     * @return 是否恢复成功
     */
    suspend fun restoreDatabaseFromFile(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 获取当前数据库文件
            val dbFile = context.getDatabasePath(DB_NAME)

            // 备份当前数据库
            val backupDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "database_backups"
            )
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "${DB_NAME}_backup_$timestamp.db")

            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
                Log.i(TAG, "已备份当前数据库到: ${backupFile.absolutePath}")
            }

            // 关闭数据库连接
            closeDatabase()
            
            // 可选：检查是否还有打开的数据库连接
            val dbFileLocked = isFileLocked(dbFile)
            if (dbFileLocked) {
                Log.w(TAG, "警告：数据库文件仍然被锁定，可能有其他进程正在使用数据库")
            }

            // 替换数据库文件
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                if (!deleted) {
                    Log.e(TAG, "无法删除旧的数据库文件: ${dbFile.absolutePath}")
                    throw IOException("无法删除旧的数据库文件")
                }
            }
            
            // 复制新数据库文件
            file.copyTo(dbFile, overwrite = true)
            
            // 确保文件权限正确
            dbFile.setReadable(true)
            dbFile.setWritable(true)

            val message = "数据库已恢复"
            Log.i(TAG, message)
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "恢复数据库失败", e)
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
            Result.failure(e)
        }
    }
    
    /**
     * 检查文件是否被锁定（正在被使用）
     */
    private fun isFileLocked(file: File): Boolean {
        try {
            // 尝试重命名文件，如果文件被锁定，这个操作会失败
            val temp = File(file.absolutePath + ".temp")
            val success = file.renameTo(temp)
            if (success) {
                // 如果成功了，重命名回来
                temp.renameTo(file)
                return false
            }
            return true
        } catch (e: Exception) {
            // 出现异常也视为文件被锁定
            return true
        }
    }
    
    /**
     * 比较本地和远程数据库文件
     * 计算两个数据库中的记录数
     *
     * @param localFile 本地数据库文件
     * @param remoteFile 远程数据库文件
     * @return 比较结果，包含两个数据库的记录数
     */
    suspend fun compareDatabases(localFile: File, remoteFile: File): DatabaseCompareInfo = withContext(Dispatchers.IO) {
        try {
            // 打开本地数据库
            val localDb = SQLiteDatabase.openDatabase(localFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            // 打开远程数据库
            val remoteDb = SQLiteDatabase.openDatabase(remoteFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            var localEntryCount = 0
            var remoteEntryCount = 0
            
            try {
                // 获取表名（以journal_entries为例，实际应根据你的数据库结构调整）
                val tableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'"
                
                // 找到与日记相关的表
                val localTables = mutableListOf<String>()
                localDb.rawQuery(tableQuery, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0)
                        localTables.add(tableName)
                    }
                }
                
                // 查找可能的日记表
                val journalTable = localTables.find { it.contains("journals", ignoreCase = true) || it.contains("entries", ignoreCase = true) }
                
                if (journalTable != null) {
                    // 计算本地数据库条目数
                    localDb.rawQuery("SELECT COUNT(*) FROM $journalTable", null).use { cursor ->
                        if (cursor.moveToFirst()) {
                            localEntryCount = cursor.getInt(0)
                        }
                    }
                    
                    // 计算远程数据库条目数
                    remoteDb.rawQuery("SELECT COUNT(*) FROM $journalTable", null).use { cursor ->
                        if (cursor.moveToFirst()) {
                            remoteEntryCount = cursor.getInt(0)
                        }
                    }
                } else {
                    // 如果找不到日记表，使用主表（假设为第一个非系统表）
                    if (localTables.isNotEmpty()) {
                        val mainTable = localTables[0]
                        
                        // 计算本地数据库条目数
                        localDb.rawQuery("SELECT COUNT(*) FROM $mainTable", null).use { cursor ->
                            if (cursor.moveToFirst()) {
                                localEntryCount = cursor.getInt(0)
                            }
                        }
                        
                        // 计算远程数据库条目数
                        remoteDb.rawQuery("SELECT COUNT(*) FROM $mainTable", null).use { cursor ->
                            if (cursor.moveToFirst()) {
                                remoteEntryCount = cursor.getInt(0)
                            }
                        }
                    }
                }
            } finally {
                // 关闭数据库连接
                localDb.close()
                remoteDb.close()
            }
            
            Log.i(TAG, "数据库比较结果 - 本地条目: $localEntryCount, 远程条目: $remoteEntryCount")
            
            DatabaseCompareInfo(localEntryCount, remoteEntryCount)
        } catch (e: Exception) {
            Log.e(TAG, "比较数据库失败", e)
            // 如果比较失败，返回默认值
            DatabaseCompareInfo(0, 0)
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach { byte ->
            val i = byte.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    /**
     * 确保远程目录存在
     * 如果目录不存在则创建
     * @return 操作是否成功
     */
    suspend fun ensureRemoteDirectoryExists(path: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 确定目录路径
            val directoryPath = path ?: getRemoteDirectoryPath()
            
            // 检查目录是否存在
            val result = fileService.listFiles(directoryPath)
            if (result.isFailure) {
                // 如果目录不存在，尝试创建
                val createResult = fileService.createDirectory(directoryPath)
                if (createResult.isSuccess) {
                    Log.i(TAG, "已创建远程目录: $directoryPath")
                    return@withContext Result.success(true)
                } else {
                    Log.e(TAG, "创建远程目录失败: $directoryPath")
                    return@withContext Result.failure(createResult.exceptionOrNull() ?: Exception("创建目录失败"))
                }
            } else {
                // 目录已存在
                Log.d(TAG, "远程目录已存在: $directoryPath")
                return@withContext Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "确保远程目录存在时出错", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * 获取远程目录路径
     * 优先使用用户名创建个人目录
     */
    private suspend fun getRemoteDirectoryPath(): String {
        // 尝试从FileService获取用户名
        try {
            // 通过反射获取FileService中的authService
            val field = fileService.javaClass.getDeclaredField("authService")
            field.isAccessible = true
            val authService = field.get(fileService)
            
            // 获取authService中的getCurrentUser方法
            val getCurrentUserMethod = authService.javaClass.getDeclaredMethod("getCurrentUser")
            val user = getCurrentUserMethod.invoke(authService)
            
            // 如果用户存在，获取username字段
            if (user != null) {
                val usernameField = user.javaClass.getDeclaredField("username")
                usernameField.isAccessible = true
                val username = usernameField.get(user) as? String
                
                if (!username.isNullOrEmpty()) {
                    return "/$username/databases"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取用户名失败，使用默认目录", e)
        }
        
        // 默认目录
        return "/databases"
    }

    /**
     * 获取上下文
     */
    fun getContext(): Context {
        return context
    }

    /**
     * 尝试使用替代方法导出数据库
     * 这个方法尝试检测WAL模式并处理可能的文件锁定问题
     */
    private suspend fun tryAlternativeExport(dbFile: File, exportFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "尝试使用替代方法导出数据库")
            
            // 检查是否存在 WAL 和 SHM 文件
            val walFile = File("${dbFile.absolutePath}-wal")
            val shmFile = File("${dbFile.absolutePath}-shm")
            
            val hasWalFile = walFile.exists()
            val hasShmFile = shmFile.exists()
            
            Log.d(TAG, "WAL文件存在: $hasWalFile, 大小: ${if (hasWalFile) walFile.length() / 1024 else 0} KB")
            Log.d(TAG, "SHM文件存在: $hasShmFile, 大小: ${if (hasShmFile) shmFile.length() / 1024 else 0} KB")
            
            // 方法1: 使用系统方法直接复制数据库（不处理WAL）
            var success = false
            
            try {
                dbFile.copyTo(exportFile, overwrite = true)
                
                // 如果有WAL文件，也复制它们
                if (hasWalFile) {
                    val exportWalFile = File("${exportFile.absolutePath}-wal")
                    walFile.copyTo(exportWalFile, overwrite = true)
                }
                
                if (hasShmFile) {
                    val exportShmFile = File("${exportFile.absolutePath}-shm")
                    shmFile.copyTo(exportShmFile, overwrite = true)
                }
                
                // 验证导出的数据库
                success = verifyExportedDatabase(exportFile)
                
                // 如果验证成功，删除可能复制的WAL和SHM文件
                if (success) {
                    File("${exportFile.absolutePath}-wal").delete()
                    File("${exportFile.absolutePath}-shm").delete()
                    Log.d(TAG, "使用系统方法成功导出数据库")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "使用系统方法导出数据库失败", e)
            }
            
            // 方法2: 尝试打开数据库并强制写入变更以合并WAL
            try {
                Log.d(TAG, "尝试强制合并WAL文件")
                
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                
                // 执行一个简单的查询强制合并WAL
                db.rawQuery("SELECT count(*) FROM sqlite_master", null).close()
                
                // 执行VACUUM操作清理数据库
                db.execSQL("VACUUM")
                
                // 关闭数据库
                db.close()
                
                // 再次尝试复制
                dbFile.copyTo(exportFile, overwrite = true)
                
                // 验证导出的数据库
                success = verifyExportedDatabase(exportFile)
                
                if (success) {
                    Log.d(TAG, "合并WAL后成功导出数据库")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "合并WAL导出数据库失败", e)
            }
            
            // 方法3: 尝试使用数据库备份API (Android 4.4+)
            try {
                Log.d(TAG, "尝试使用数据库备份API")
                
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                
                db.enableWriteAheadLogging()
                
                // 备份数据库
                db.beginTransaction()
                try {
                    // 使用标准的 Java IO
                    FileOutputStream(exportFile).use { output ->
                        FileInputStream(dbFile).use { input ->
                            input.channel.transferTo(0, input.channel.size(), output.channel)
                        }
                    }
                    
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                
                db.close()
                
                // 验证导出的数据库
                success = verifyExportedDatabase(exportFile)
                
                if (success) {
                    Log.d(TAG, "使用数据库备份API成功导出数据库")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "使用数据库备份API导出数据库失败", e)
            }
            
            // 方法4: 作为最后手段，尝试使用runtime执行sqlite3命令导出
            try {
                Log.d(TAG, "尝试使用sqlite3命令导出数据库")
                
                // 检查是否有sqlite3可执行文件
                val process = Runtime.getRuntime().exec("which sqlite3")
                val scanner = java.util.Scanner(process.inputStream).useDelimiter("\\A")
                val sqlitePath = if (scanner.hasNext()) scanner.next().trim() else ""
                
                if (sqlitePath.isNotEmpty()) {
                    // 创建一个导出SQL的临时文件
                    val sqlFile = File(exportFile.parent, "temp_export.sql")
                    
                    // 使用sqlite3导出数据库为SQL
                    val exportCmd = "$sqlitePath ${dbFile.absolutePath} .dump > ${sqlFile.absolutePath}"
                    val exportProcess = Runtime.getRuntime().exec(exportCmd)
                    exportProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
                    
                    // 然后用sqlite3导入SQL到新数据库
                    val importCmd = "$sqlitePath ${exportFile.absolutePath} < ${sqlFile.absolutePath}"
                    val importProcess = Runtime.getRuntime().exec(importCmd)
                    importProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
                    
                    // 删除临时SQL文件
                    sqlFile.delete()
                    
                    // 验证导出的数据库
                    success = verifyExportedDatabase(exportFile)
                    
                    if (success) {
                        Log.d(TAG, "使用sqlite3命令成功导出数据库")
                        return@withContext true
                    }
                } else {
                    Log.d(TAG, "未找到sqlite3命令行工具")
                }
            } catch (e: Exception) {
                Log.e(TAG, "使用sqlite3命令导出数据库失败", e)
            }
            
            Log.d(TAG, "所有替代方法均失败")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "替代导出方法出错", e)
            return@withContext false
        }
    }
} 

