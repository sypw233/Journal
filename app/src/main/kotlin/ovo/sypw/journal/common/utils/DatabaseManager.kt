package ovo.sypw.journal.common.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovo.sypw.journal.data.remote.api.FileService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

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

    /**
     * 导出数据库
     * 将数据库导出到应用专属存储空间
     *
     * @return 导出的文件，如果失败则返回null
     */
    suspend fun exportDatabase(): File? = withContext(Dispatchers.IO) {
        try {
            // 获取数据库文件
            val dbFile = context.getDatabasePath(DB_NAME)

            // 确保数据库存在
            if (!dbFile.exists()) {
                val message = "数据库文件不存在: $DB_NAME"
                Log.e(TAG, message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                return@withContext null
            }

            // 创建导出目录（应用专属外部存储）
            val exportDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "database_exports"
            )
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // 创建带时间戳的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "${DB_NAME}_$timestamp.db")

            // 复制数据库文件
            dbFile.copyTo(exportFile, overwrite = true)

            Log.i(TAG, "数据库已导出到: ${exportFile.absolutePath}")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "数据库已导出到: ${exportFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }

            exportFile
        } catch (e: Exception) {
            Log.e(TAG, "导出数据库失败", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                Result.success(response.path)
            } else {
                val exception = result.exceptionOrNull() ?: Exception("上传失败")
                Log.e(TAG, "上传数据库失败", exception)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "上传失败: ${exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                Result.failure(exception)
            }
        } catch (e: Exception) {
            Log.e(TAG, "上传数据库失败", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                    Result.success(file)
                } else {
                    val exception = result.exceptionOrNull() ?: Exception("下载失败")
                    Log.e(TAG, "下载数据库失败", exception)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "下载失败: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
            }

            // 关闭数据库连接（这里需要应用层面的支持）
            // TODO: 在应用层面实现关闭数据库连接的逻辑

            // 替换数据库文件
            file.copyTo(dbFile, overwrite = true)

            val message = "数据库已恢复"
            Log.i(TAG, message)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "恢复数据库失败", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            Result.failure(e)
        }
    }
} 