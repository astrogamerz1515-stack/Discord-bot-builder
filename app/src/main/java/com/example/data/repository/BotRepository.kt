package com.example.data.repository

import com.example.data.dao.BotFileDao
import com.example.data.dao.BotProjectDao
import com.example.data.dao.SavedEmbedDao
import com.example.data.dao.TerminalLogDao
import com.example.data.model.BotFile
import com.example.data.model.BotLanguage
import com.example.data.model.BotProject
import com.example.data.model.SavedEmbed
import com.example.data.model.TerminalLog
import com.example.data.templates.BotTemplates
import kotlinx.coroutines.flow.Flow

class BotRepository(
    private val projectDao: BotProjectDao,
    private val fileDao: BotFileDao,
    private val logDao: TerminalLogDao,
    private val embedDao: SavedEmbedDao,
    private val keyValueDao: com.example.data.dao.BotKeyValueDao
) {
    val allProjects: Flow<List<BotProject>> = projectDao.getAllProjects()

    fun getProject(id: Long): Flow<BotProject?> = projectDao.observeProjectById(id)

    suspend fun getProjectSync(id: Long): BotProject? = projectDao.getProjectById(id)

    fun getFiles(projectId: Long): Flow<List<BotFile>> = fileDao.getFilesForProject(projectId)

    suspend fun getFileById(fileId: Long): BotFile? = fileDao.getFileById(fileId)

    fun getTerminalLogs(projectId: Long): Flow<List<TerminalLog>> = logDao.getLogsForProject(projectId)

    fun getEmbeds(projectId: Long): Flow<List<SavedEmbed>> = embedDao.getEmbedsForProject(projectId)

    fun getStorage(projectId: Long): Flow<List<com.example.data.model.BotKeyValue>> = keyValueDao.getStorageForProject(projectId)

    suspend fun setStorageValue(projectId: Long, key: String, value: String, type: String = "STRING") {
        keyValueDao.insertOrUpdate(
            com.example.data.model.BotKeyValue(
                projectId = projectId,
                storageKey = key,
                storageValue = value,
                valueType = type,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteStorageValue(id: Long) {
        keyValueDao.deleteById(id)
    }

    suspend fun clearStorage(projectId: Long) {
        keyValueDao.clearAllForProject(projectId)
    }

    suspend fun createProject(
        name: String,
        description: String,
        language: BotLanguage,
        prefix: String
    ): Long {
        val newProject = BotProject(
            name = name,
            description = description,
            language = language.name,
            prefix = prefix,
            status = "Online",
            activityType = "PLAYING",
            activityText = "$prefix help | ${language.displayName}"
        )
        val projectId = projectDao.insertProject(newProject)

        // Populate initial files from template
        val templateFiles = BotTemplates.getTemplateForLanguage(language, name)
        val projectFiles = templateFiles.map { it.copy(projectId = projectId) }
        fileDao.insertFiles(projectFiles)

        // Log initial message in terminal
        logDao.insertLog(
            TerminalLog(
                projectId = projectId,
                text = "Initialized new ${language.displayName} project '$name'",
                type = "SYSTEM"
            )
        )
        logDao.insertLog(
            TerminalLog(
                projectId = projectId,
                text = "Ready to code. Entrypoint: ${language.defaultEntryFile}",
                type = "SYSTEM"
            )
        )

        return projectId
    }

    suspend fun updateProject(project: BotProject) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
        fileDao.deleteFilesForProject(projectId)
        logDao.clearLogsForProject(projectId)
    }

    suspend fun saveFile(file: BotFile) {
        fileDao.updateFile(file.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun createFile(projectId: Long, filePath: String, content: String = ""): Long {
        val file = BotFile(
            projectId = projectId,
            filePath = filePath,
            content = content,
            isEntrypoint = false,
            updatedAt = System.currentTimeMillis()
        )
        return fileDao.insertFile(file)
    }

    suspend fun deleteFile(fileId: Long) {
        fileDao.deleteFileById(fileId)
    }

    suspend fun addTerminalLog(projectId: Long, text: String, type: String = "STDOUT") {
        logDao.insertLog(
            TerminalLog(
                projectId = projectId,
                text = text,
                type = type,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearTerminalLogs(projectId: Long) {
        logDao.clearLogsForProject(projectId)
    }

    suspend fun saveEmbed(embed: SavedEmbed): Long {
        return embedDao.insertEmbed(embed)
    }

    suspend fun deleteEmbed(embed: SavedEmbed) {
        embedDao.deleteEmbed(embed)
    }
}
