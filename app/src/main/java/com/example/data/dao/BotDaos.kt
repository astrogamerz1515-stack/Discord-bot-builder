package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.data.model.SavedEmbed
import com.example.data.model.TerminalLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BotProjectDao {
    @Query("SELECT * FROM bot_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<BotProject>>

    @Query("SELECT * FROM bot_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): BotProject?

    @Query("SELECT * FROM bot_projects WHERE id = :id")
    fun observeProjectById(id: Long): Flow<BotProject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: BotProject): Long

    @Update
    suspend fun updateProject(project: BotProject)

    @Delete
    suspend fun deleteProject(project: BotProject)

    @Query("DELETE FROM bot_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface BotFileDao {
    @Query("SELECT * FROM bot_files WHERE projectId = :projectId ORDER BY isEntrypoint DESC, filePath ASC")
    fun getFilesForProject(projectId: Long): Flow<List<BotFile>>

    @Query("SELECT * FROM bot_files WHERE projectId = :projectId AND filePath = :filePath LIMIT 1")
    suspend fun getFileByPath(projectId: Long, filePath: String): BotFile?

    @Query("SELECT * FROM bot_files WHERE id = :fileId LIMIT 1")
    suspend fun getFileById(fileId: Long): BotFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: BotFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<BotFile>)

    @Update
    suspend fun updateFile(file: BotFile)

    @Query("DELETE FROM bot_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: Long)

    @Query("DELETE FROM bot_files WHERE projectId = :projectId")
    suspend fun deleteFilesForProject(projectId: Long)
}

@Dao
interface TerminalLogDao {
    @Query("SELECT * FROM terminal_logs WHERE projectId = :projectId ORDER BY timestamp ASC LIMIT 500")
    fun getLogsForProject(projectId: Long): Flow<List<TerminalLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TerminalLog)

    @Query("DELETE FROM terminal_logs WHERE projectId = :projectId")
    suspend fun clearLogsForProject(projectId: Long)
}

@Dao
interface SavedEmbedDao {
    @Query("SELECT * FROM saved_embeds WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getEmbedsForProject(projectId: Long): Flow<List<SavedEmbed>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbed(embed: SavedEmbed): Long

    @Delete
    suspend fun deleteEmbed(embed: SavedEmbed)
}
