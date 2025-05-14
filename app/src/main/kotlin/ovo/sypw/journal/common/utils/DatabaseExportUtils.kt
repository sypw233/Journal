package ovo.sypw.journal.common.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据库导出工具类
 */
object DatabaseExporter {
    private const val TAG = "DatabaseExporter"

    /**
     * 将数据库文件导出到应用专属存储空间
     *
     * @param context 上下文
     * @param databaseName 数据库名称（例如："app_database.db"）
     * @return 导出的文件，如果失败则返回null
     */
    fun exportDatabaseToAppStorage(context: Context, databaseName: String): File? {
        try {
            // 获取数据库文件
            val dbFile = context.getDatabasePath(databaseName)

            // 确保数据库存在
            if (!dbFile.exists()) {
                Log.e(TAG, "数据库文件不存在: $databaseName")
//                Toast.makeText(context, "数据库文件不存在", Toast.LENGTH_SHORT).show()
                return null
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
            val exportFile = File(exportDir, "${databaseName.removeSuffix(".db")}_$timestamp.db")

            // 复制数据库文件
            FileInputStream(dbFile).use { input ->
                FileOutputStream(exportFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "数据库已导出到: ${exportFile.absolutePath}")
//            Toast.makeText(context, "数据库已导出到: ${exportFile.absolutePath}", Toast.LENGTH_LONG)
//                .show()

            return exportFile

        } catch (e: Exception) {
            Log.e(TAG, "导出数据库失败", e)
//            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    /**
     * 获取导出的数据库文件列表
     *
     * @param context 上下文
     * @return 导出的数据库文件列表
     */
    fun getExportedDatabaseFiles(context: Context): List<File> {
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
}