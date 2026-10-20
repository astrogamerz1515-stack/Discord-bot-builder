package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BotFileDao
import com.example.data.dao.BotProjectDao
import com.example.data.dao.SavedEmbedDao
import com.example.data.dao.TerminalLogDao
import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.data.model.SavedEmbed
import com.example.data.model.TerminalLog
import com.example.data.templates.BotTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BotProject::class,
        BotFile::class,
        TerminalLog::class,
        SavedEmbed::class,
        com.example.data.model.BotKeyValue::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun botProjectDao(): BotProjectDao
    abstract fun botFileDao(): BotFileDao
    abstract fun terminalLogDao(): TerminalLogDao
    abstract fun savedEmbedDao(): SavedEmbedDao
    abstract fun botKeyValueDao(): com.example.data.dao.BotKeyValueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "discord_bot_studio.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
