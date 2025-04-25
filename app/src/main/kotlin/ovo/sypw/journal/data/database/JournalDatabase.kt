package ovo.sypw.journal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 日记数据库类
 * 包含日记实体和同步实体
 */
@Database(
    entities = [JournalEntity::class, SyncEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(JournalConverters::class)
abstract class JournalDatabase : RoomDatabase() {
    /**
     * 获取日记DAO
     */
    abstract fun journalDao(): JournalDao

    /**
     * 获取支持同步的日记DAO
     */
    abstract fun syncAwareJournalDao(): SyncAwareJournalDao

    /**
     * 获取同步DAO
     */
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        /**
         * 获取数据库实例
         */
        fun getDatabase(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_database"
                )
                    .fallbackToDestructiveMigration() // 在版本升级时允许清空数据库
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}