package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BotKeyValue
import kotlinx.coroutines.flow.Flow

@Dao
interface BotKeyValueDao {
    @Query("SELECT * FROM bot_key_values WHERE projectId = :projectId ORDER BY storageKey ASC")
    fun getStorageForProject(projectId: Long): Flow<List<BotKeyValue>>

    @Query("SELECT * FROM bot_key_values WHERE projectId = :projectId AND storageKey = :key LIMIT 1")
    suspend fun getValue(projectId: Long, key: String): BotKeyValue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: BotKeyValue): Long

    @Query("DELETE FROM bot_key_values WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bot_key_values WHERE projectId = :projectId")
    suspend fun clearAllForProject(projectId: Long)
}
