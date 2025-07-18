package ovo.sypw.journal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 日记数据库类
 */
@Database(
    entities = [JournalEntity::class, SentimentEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(JournalConverters::class)
abstract class JournalDatabase : RoomDatabase() {
    /**
     * 获取日记DAO
     */
    abstract fun journalDao(): JournalDao

    /**
     * 获取情感分析DAO
     */
    abstract fun sentimentDao(): SentimentDao

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