package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BotFile
import com.example.data.model.BotLanguage
import com.example.data.model.BotProject
import com.example.data.model.SavedEmbed
import com.example.data.model.TerminalLog
import com.example.data.repository.BotRepository
import com.example.engine.BotRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String, val iconName: String) {
    HOME("Home", "home"),
    EDITOR("Editor", "code"),
    VISUAL_BUILDER("Visual Builder", "extension"),
    TERMINAL("Terminal", "terminal"),
    SIMULATOR("Discord Simulator", "chat"),
    EMBED_BUILDER("Embed Designer", "dashboard_customize"),
    STORAGE("Storage Manager", "storage"),
    EXTENSIONS("Extensions", "extension"),
    GRADLE("Gradle", "build"),
    PACKAGES("Install", "extension"),
    BOT_CONFIG("Bot Config", "settings"),
    DEPLOY("Deploy", "cloud_upload")
}

data class EmbedFieldItem(val name: String, val value: String, val inline: Boolean = true)
data class EmbedButtonItem(val label: String, val style: String = "PRIMARY", val emoji: String = "")
data class EmbedSelectOptionItem(val label: String, val value: String, val description: String = "", val emoji: String = "")

class BotStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = BotRepository(
        database.botProjectDao(),
        database.botFileDao(),
        database.terminalLogDao(),
        database.savedEmbedDao(),
        database.botKeyValueDao()
    )

    val runtimeEngine = com.example.engine.BotRuntimeManager.getEngine(repository)

    val allProjects: StateFlow<List<BotProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<BotProject?>(null)
    val currentProject: StateFlow<BotProject?> = _currentProject.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<BotFile>>(emptyList())
    val projectFiles: StateFlow<List<BotFile>> = _projectFiles.asStateFlow()

    private val _activeFile = MutableStateFlow<BotFile?>(null)
    val activeFile: StateFlow<BotFile?> = _activeFile.asStateFlow()

    private val _activeFileContent = MutableStateFlow("")
    val activeFileContent: StateFlow<String> = _activeFileContent.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    private val _terminalInput = MutableStateFlow("")
    val terminalInput: StateFlow<String> = _terminalInput.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()
    private var historyIndex = -1

    // Autosave State
    private val _autoSaveEnabled = MutableStateFlow(true)
    val autoSaveEnabled: StateFlow<Boolean> = _autoSaveEnabled.asStateFlow()

    private val _saveStatus = MutableStateFlow("Saved")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var autoSaveJob: Job? = null

    // Storage State
    private val _storageEntries = MutableStateFlow<List<com.example.data.model.BotKeyValue>>(emptyList())
    val storageEntries: StateFlow<List<com.example.data.model.BotKeyValue>> = _storageEntries.asStateFlow()

    // Code Diagnostics / Error Marker State
    private val _codeDiagnostics = MutableStateFlow<List<com.example.ui.editor.CodeDiagnostic>>(emptyList())
    val codeDiagnostics: StateFlow<List<com.example.ui.editor.CodeDiagnostic>> = _codeDiagnostics.asStateFlow()

    // Embed Builder State
    private val _embedTitle = MutableStateFlow("🛡️ Server Welcome & Rules")
    val embedTitle: StateFlow<String> = _embedTitle.asStateFlow()

    private val _embedDescription = MutableStateFlow("Welcome to the community! Please respect everyone and enjoy your stay.")
    val embedDescription: StateFlow<String> = _embedDescription.asStateFlow()

    private val _embedColorHex = MutableStateFlow("#5865F2")
    val embedColorHex: StateFlow<String> = _embedColorHex.asStateFlow()

    private val _embedAuthorName = MutableStateFlow("Aegis Bot System")
    val embedAuthorName: StateFlow<String> = _embedAuthorName.asStateFlow()

    private val _embedFooterText = MutableStateFlow("Discord Bot Studio • 2026")
    val embedFooterText: StateFlow<String> = _embedFooterText.asStateFlow()

    private val _embedFields = MutableStateFlow<List<EmbedFieldItem>>(
        listOf(
            EmbedFieldItem("Getting Started", "Visit #roles to get your server tags", inline = true),
            EmbedFieldItem("Support", "Open a ticket in #help", inline = true)
        )
    )
    val embedFields: StateFlow<List<EmbedFieldItem>> = _embedFields.asStateFlow()

    private val _embedButtons = MutableStateFlow<List<EmbedButtonItem>>(
        listOf(
            EmbedButtonItem("Verify Account", "SUCCESS", "✅"),
            EmbedButtonItem("View Rules", "PRIMARY", "📜")
        )
    )
    val embedButtons: StateFlow<List<EmbedButtonItem>> = _embedButtons.asStateFlow()

    // Embed Drop-down Select Menu State
    private val _embedSelectMenuPlaceholder = MutableStateFlow("Select a server role or category...")
    val embedSelectMenuPlaceholder: StateFlow<String> = _embedSelectMenuPlaceholder.asStateFlow()

    private val _embedSelectOptions = MutableStateFlow<List<EmbedSelectOptionItem>>(
        listOf(
            EmbedSelectOptionItem("Developer Role", "role_dev", "Access code and API channels", "💻"),
            EmbedSelectOptionItem("Gamer Role", "role_gamer", "Join multiplayer game nights", "🎮"),
            EmbedSelectOptionItem("Designer Role", "role_designer", "UI/UX & asset showcases", "🎨")
        )
    )
    val embedSelectOptions: StateFlow<List<EmbedSelectOptionItem>> = _embedSelectOptions.asStateFlow()

    // Dialog flags
    val showNewProjectDialog = MutableStateFlow(false)
    val showNewFileDialog = MutableStateFlow(false)
    val showProjectSwitchDialog = MutableStateFlow(false)
    val showSnippetsDialog = MutableStateFlow(false)
    val showGeneratedCodeDialog = MutableStateFlow(false)
    val showTokenInputDialog = MutableStateFlow(false)
    val connectionError: StateFlow<String?> = runtimeEngine.connectionError

    fun dismissConnectionError() {
        runtimeEngine.clearConnectionError()
    }
    val generatedCodeText = MutableStateFlow("")

    // Editor display configuration states
    val editorFullscreenMode = MutableStateFlow(false)
    val showBottomNavInEditor = MutableStateFlow(false)

    fun toggleEditorFullscreen() {
        editorFullscreenMode.value = !editorFullscreenMode.value
    }

    fun toggleBottomNavInEditor() {
        showBottomNavInEditor.value = !showBottomNavInEditor.value
    }

    // Import & Auto-Correction Summary Dialog State
    private val _lastImportFixes = MutableStateFlow<List<String>>(emptyList())
    val lastImportFixes: StateFlow<List<String>> = _lastImportFixes.asStateFlow()

    private val _lastImportedProject = MutableStateFlow<BotProject?>(null)
    val lastImportedProject: StateFlow<BotProject?> = _lastImportedProject.asStateFlow()

    val showImportSummaryDialog = MutableStateFlow(false)

    // Large File Streaming & Zero-Lag Paging States (15MB+ safe)
    private val _isLargeFileMode = MutableStateFlow(false)
    val isLargeFileMode: StateFlow<Boolean> = _isLargeFileMode.asStateFlow()

    private val _largeFileLineIndex = MutableStateFlow<com.example.util.LargeFileEngine.LineIndex?>(null)
    val largeFileLineIndex: StateFlow<com.example.util.LargeFileEngine.LineIndex?> = _largeFileLineIndex.asStateFlow()

    private val _currentLargeFilePage = MutableStateFlow<com.example.util.LargeFileEngine.PageData?>(null)
    val currentLargeFilePage: StateFlow<com.example.util.LargeFileEngine.PageData?> = _currentLargeFilePage.asStateFlow()

    private val _isLargeFileLoading = MutableStateFlow(false)
    val isLargeFileLoading: StateFlow<Boolean> = _isLargeFileLoading.asStateFlow()

    private val _largeFilePageSize = MutableStateFlow(com.example.util.LargeFileEngine.DEFAULT_PAGE_SIZE)
    val largeFilePageSize: StateFlow<Int> = _largeFilePageSize.asStateFlow()

    private var projectFilesJob: Job? = null
    private var terminalLogsJob: Job? = null
    private var storageJob: Job? = null
    private var diagnosticsJob: Job? = null

    init {
        com.example.engine.Pm2ProcessManager.initialize(application, repository)
        com.example.engine.CommandResponseOptimizer.startMonitoring(viewModelScope)
        com.example.service.BotBackgroundService.onServiceStopListener = {
            stopBotProcess()
        }

        val prefs = com.example.service.BotBackgroundService.getPrefs(application)
        val savedRunningId = prefs.getLong(com.example.service.BotBackgroundService.PREF_PROJECT_ID, 0L)
        val isMarkedRunning = prefs.getBoolean(com.example.service.BotBackgroundService.PREF_IS_RUNNING, false)

        val settingsPrefs = application.getSharedPreferences("bot_studio_settings", android.content.Context.MODE_PRIVATE)
        val hasCleanedTemplates = settingsPrefs.getBoolean("has_removed_readymade_templates_v4", false)
        if (!hasCleanedTemplates) {
            settingsPrefs.edit().putBoolean("has_removed_readymade_templates_v4", true).apply()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val templateNames = setOf(
                    "Aegis Moderation & Utility",
                    "Aegis",
                    "Node.js Discord Bot",
                    "Python Music Bot",
                    "Advanced Moderator Bot",
                    "TypeScript Guild Bot",
                    "Slash Command Studio",
                    "Discord Economy Bot",
                    "Rust High-Perf Bot",
                    "Go Gateway Bot",
                    "Java JDA Bot",
                    "C# DSharpPlus Bot"
                )
                val allProjs = repository.getAllProjectsSync()
                for (proj in allProjs) {
                    if (proj.name in templateNames) {
                        repository.deleteProject(proj.id)
                    }
                }
            }
        }

        viewModelScope.launch {
            allProjects.collect { projects ->
                if (projects.isEmpty()) {
                    // User requested: No readymade template bots. Completely blank slate.
                    _currentProject.value = null
                    _projectFiles.value = emptyList()
                } else if (_currentProject.value == null || !projects.any { it.id == _currentProject.value?.id }) {
                    val target = if (isMarkedRunning && savedRunningId > 0) {
                        projects.find { it.id == savedRunningId } ?: projects.first()
                    } else {
                        projects.first()
                    }
                    selectProject(target)
                    if (isMarkedRunning) {
                        if (!runtimeEngine.isRunning.value) {
                            startBotProcess()
                        } else {
                            try {
                                com.example.service.BotBackgroundService.start(application, target.id, target.name)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    fun selectProject(project: BotProject) {
        _currentProject.value = project
        runtimeEngine.setProject(project.id)

        // Cancel previous observers to prevent duplicate or conflicting flow collectors
        projectFilesJob?.cancel()
        terminalLogsJob?.cancel()
        storageJob?.cancel()

        // Observe files for this project
        projectFilesJob = viewModelScope.launch {
            repository.getFiles(project.id).collect { files ->
                _projectFiles.value = files
                val current = _activeFile.value
                if (current == null || !files.any { it.id == current.id }) {
                    val entrypoint = files.find { it.isEntrypoint } ?: files.firstOrNull()
                    if (entrypoint != null) {
                        selectFile(entrypoint)
                    }
                }
            }
        }

        // Observe terminal logs for this project
        terminalLogsJob = viewModelScope.launch {
            repository.getTerminalLogs(project.id).collect { logs ->
                val maxLimit = com.example.engine.SystemDeviceOptimizer.settings.value.maxLogBufferSize
                _terminalLogs.value = if (logs.size > maxLimit) logs.takeLast(maxLimit) else logs
            }
        }

        // Observe storage key-values for this project
        storageJob = viewModelScope.launch {
            repository.getStorage(project.id).collect { entries ->
                _storageEntries.value = entries
            }
        }
    }

    fun selectFile(file: BotFile) {
        val currentFile = _activeFile.value
        if (currentFile != null && currentFile.id == file.id) {
            // Already active file, avoid redundant reloads
            return
        }

        // Cancel any pending debounced autosave for the previous file
        autoSaveJob?.cancel()

        // Asynchronously persist any modifications on the previous file
        if (currentFile != null) {
            val contentToSave = _activeFileContent.value
            if (_isLargeFileMode.value) {
                saveActiveLargeFilePage(contentToSave)
            } else if (contentToSave != currentFile.content) {
                val fileToSave = currentFile.copy(content = contentToSave)
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveFile(fileToSave)
                    val localFile = com.example.util.LargeFileEngine.getLocalFile(getApplication(), fileToSave.projectId, fileToSave.filePath)
                    com.example.util.LargeFileEngine.writeEntireContent(localFile, contentToSave)
                }
            }
        }

        val localDiskFile = com.example.util.LargeFileEngine.getLocalFile(getApplication(), file.projectId, file.filePath)
        val isLarge = (localDiskFile.exists() && localDiskFile.length() > com.example.util.LargeFileEngine.LARGE_FILE_THRESHOLD_BYTES) ||
                file.content.length > com.example.util.LargeFileEngine.LARGE_FILE_THRESHOLD_BYTES ||
                file.content.startsWith("[DISK_STORED_FILE:")

        if (isLarge) {
            _isLargeFileMode.value = true
            _isLargeFileLoading.value = true
            _activeFile.value = file
            _saveStatus.value = "Saved (Large File Mode)"

            viewModelScope.launch(Dispatchers.IO) {
                // Ensure local disk file contains the content if not already populated
                if (!localDiskFile.exists() || localDiskFile.length() == 0L) {
                    if (file.content.isNotEmpty() && !file.content.startsWith("[DISK_STORED_FILE:")) {
                        com.example.util.LargeFileEngine.writeEntireContent(localDiskFile, file.content)
                    }
                }

                val index = com.example.util.LargeFileEngine.buildPageIndex(localDiskFile, _largeFilePageSize.value)
                val page0 = com.example.util.LargeFileEngine.readPage(index, 0)
                withContext(Dispatchers.Main) {
                    _largeFileLineIndex.value = index
                    _currentLargeFilePage.value = page0
                    _activeFileContent.value = page0.pageText
                    _isLargeFileLoading.value = false
                    runDiagnostics(page0.pageText, file.filePath)
                }
            }
            return
        }

        // Standard size file: direct loading
        _isLargeFileMode.value = false
        _largeFileLineIndex.value = null
        _currentLargeFilePage.value = null
        _activeFile.value = file
        _activeFileContent.value = file.content
        _saveStatus.value = "Saved"

        // Run diagnostics on background dispatcher
        diagnosticsJob?.cancel()
        runDiagnostics(file.content, file.filePath)
    }

    fun loadLargeFilePage(targetPageIndex: Int) {
        val index = _largeFileLineIndex.value ?: return
        val currentFile = _activeFile.value ?: return
        val totalPages = index.pageOffsets.size.coerceAtLeast(1)
        val safeIndex = targetPageIndex.coerceIn(0, totalPages - 1)

        _isLargeFileLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val page = com.example.util.LargeFileEngine.readPage(index, safeIndex)
            withContext(Dispatchers.Main) {
                _currentLargeFilePage.value = page
                _activeFileContent.value = page.pageText
                _isLargeFileLoading.value = false
                _saveStatus.value = "Page ${page.pageIndex + 1} of ${page.totalPages} loaded"
                runDiagnostics(page.pageText, currentFile.filePath)
            }
        }
    }

    fun saveActiveLargeFilePage(pageText: String) {
        val index = _largeFileLineIndex.value ?: return
        val currentPage = _currentLargeFilePage.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedIndex = com.example.util.LargeFileEngine.savePage(index, currentPage.pageIndex, pageText)
            val updatedPage = com.example.util.LargeFileEngine.readPage(updatedIndex, currentPage.pageIndex)
            withContext(Dispatchers.Main) {
                _largeFileLineIndex.value = updatedIndex
                _currentLargeFilePage.value = updatedPage
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _saveStatus.value = "Saved page ${updatedPage.pageIndex + 1} of ${updatedPage.totalPages} at $timeStr"
            }
        }
    }

    fun jumpToLargeFileLine(targetLine: Int) {
        val index = _largeFileLineIndex.value ?: return
        val safeLine = targetLine.coerceIn(1, index.totalLines)
        val targetPageIndex = (safeLine - 1) / index.pageSize
        loadLargeFilePage(targetPageIndex)
    }

    fun setLargeFilePageSize(newPageSize: Int) {
        val index = _largeFileLineIndex.value ?: return
        val file = index.file
        _largeFilePageSize.value = newPageSize
        _isLargeFileLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val newIndex = com.example.util.LargeFileEngine.buildPageIndex(file, newPageSize)
            val page0 = com.example.util.LargeFileEngine.readPage(newIndex, 0)
            withContext(Dispatchers.Main) {
                _largeFileLineIndex.value = newIndex
                _currentLargeFilePage.value = page0
                _activeFileContent.value = page0.pageText
                _isLargeFileLoading.value = false
            }
        }
    }

    fun updateActiveFileContent(newContent: String) {
        _activeFileContent.value = newContent
        val currentFile = _activeFile.value
        if (currentFile != null && !_isLargeFileMode.value) {
            val updatedFile = currentFile.copy(content = newContent)
            _activeFile.value = updatedFile
            val currentFiles = _projectFiles.value.map { if (it.id == updatedFile.id) updatedFile else it }
            _projectFiles.value = currentFiles
            runtimeEngine.updateCachedFiles(updatedFile.projectId, currentFiles)
        }

        // Smooth background debounced linting to prevent typing stutter
        diagnosticsJob?.cancel()
        diagnosticsJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            delay(400)
            val file = _activeFile.value
            if (file != null) {
                val diags = com.example.ui.editor.CodeLinter.lintCode(newContent, file.filePath)
                _codeDiagnostics.value = diags
            }
        }

        if (_autoSaveEnabled.value) {
            _saveStatus.value = "Saving..."
            autoSaveJob?.cancel()
            autoSaveJob = viewModelScope.launch {
                delay(1200) // 1.2s debounce for responsive smooth typing
                saveActiveFile()
            }
        } else {
            _saveStatus.value = "Unsaved changes"
        }
    }

    fun toggleAutoSave(enabled: Boolean) {
        _autoSaveEnabled.value = enabled
        if (enabled) {
            saveActiveFile()
        } else {
            _saveStatus.value = "Auto-save paused"
        }
    }

    fun runDiagnostics(code: String, filePath: String) {
        diagnosticsJob?.cancel()
        diagnosticsJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val diags = com.example.ui.editor.CodeLinter.lintCode(code, filePath)
            _codeDiagnostics.value = diags
        }
    }

    fun addStorageEntry(key: String, value: String, type: String = "STRING") {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            repository.setStorageValue(project.id, key, value, type)
            repository.addTerminalLog(project.id, "[STORAGE] Upsert key '$key' ($type)", "STDOUT")
        }
    }

    fun deleteStorageEntry(id: Long) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            repository.deleteStorageValue(id)
            repository.addTerminalLog(project.id, "[STORAGE] Deleted entry id=$id", "STDOUT")
        }
    }

    fun clearStorage() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            repository.clearStorage(project.id)
            repository.addTerminalLog(project.id, "[STORAGE] Datastore cleared", "STDOUT")
        }
    }

    fun saveActiveFile() {
        val file = _activeFile.value ?: return
        val currentContent = _activeFileContent.value

        if (_isLargeFileMode.value) {
            saveActiveLargeFilePage(currentContent)
            return
        }

        val updatedFile = file.copy(content = currentContent)
        _activeFile.value = updatedFile

        // Update in-memory project files immediately
        val currentFiles = _projectFiles.value.map { if (it.id == updatedFile.id) updatedFile else it }
        _projectFiles.value = currentFiles

        // Push immediately into runtime engine in-memory cache to guarantee zero-latency hot reload
        runtimeEngine.updateCachedFiles(file.projectId, currentFiles)

        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _saveStatus.value = "Saved at $timeStr"

        val project = _currentProject.value
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.saveFile(updatedFile)
            val localFile = com.example.util.LargeFileEngine.getLocalFile(getApplication(), file.projectId, file.filePath)
            com.example.util.LargeFileEngine.writeEntireContent(localFile, currentContent)
            if (project != null) {
                com.example.engine.BotFileAutoCreator.autoCreateReferencedFiles(project, currentFiles, repository)
            }
        }
    }

    fun autoCreateFilesFromCode() {
        val project = _currentProject.value ?: return
        val currentFiles = _projectFiles.value
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val created = com.example.engine.BotFileAutoCreator.autoCreateReferencedFiles(project, currentFiles, repository)
            if (created.isNotEmpty()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "📁 Auto-created ${created.size} file(s): ${created.joinToString(", ")}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "✓ All files in code are up to date!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun setTab(tab: AppTab) {
        if (_currentTab.value == AppTab.EDITOR && tab != AppTab.EDITOR) {
            saveActiveFile()
        }
        _currentTab.value = tab
    }

    fun createNewProject(
        name: String,
        description: String,
        language: BotLanguage,
        prefix: String,
        botToken: String = ""
    ) {
        val sanitized = runtimeEngine.sanitizeToken(botToken)
        viewModelScope.launch {
            val newId = repository.createProject(name, description, language, prefix, sanitized)
            val created = repository.getProjectSync(newId)
            if (created != null) {
                selectProject(created)
                _currentTab.value = AppTab.EDITOR
            }
            showNewProjectDialog.value = false
        }
    }

    fun importProjectFromZip(
        uri: Uri,
        context: Context,
        onResult: (Boolean, String, BotProject?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "Imported Bot"
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val resolved = cursor.getString(nameIndex)
                            if (!resolved.isNullOrBlank()) {
                                fileName = resolved
                            }
                        }
                    }
                } catch (_: Exception) {}

                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read selected file")

                val parsed = com.example.util.BotZipManager.parseImportBytes(bytes, fileName)

                if (parsed.files.isEmpty()) {
                    throw IllegalArgumentException("No readable source code files found in selected archive or file.")
                }

                val newProject = BotProject(
                    name = parsed.name,
                    description = parsed.description,
                    language = parsed.language,
                    prefix = parsed.prefix,
                    botToken = parsed.botToken.ifBlank { "MTE4OTIzNDU2Nzg5MDEyMzQ1Ng.G-DiscordSecretBotTokenHere" },
                    clientId = parsed.clientId,
                    status = parsed.status,
                    activityType = parsed.activityType,
                    activityText = parsed.activityText
                )

                val newId = repository.importProjectWithFiles(newProject, parsed.files)
                val created = repository.getProjectSync(newId)

                withContext(Dispatchers.Main) {
                    if (created != null) {
                        selectProject(created)
                        _currentTab.value = AppTab.EDITOR
                        _lastImportedProject.value = created
                        _lastImportFixes.value = parsed.autoFixes
                        showImportSummaryDialog.value = parsed.autoFixes.isNotEmpty()
                    }
                    val fixSuffix = if (parsed.autoFixes.isNotEmpty()) " (${parsed.autoFixes.size} auto-corrections applied)" else ""
                    onResult(true, "Successfully imported '${parsed.name}' (${parsed.files.size} file${if (parsed.files.size == 1) "" else "s"})$fixSuffix!", created)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage ?: "Unknown error"}", null)
                }
            }
        }
    }

    fun importSingleFileToProject(
        uri: Uri,
        context: Context,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val proj = _currentProject.value ?: run {
            onResult?.invoke(false, "No active project selected")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "imported_file.js"
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val resolved = cursor.getString(nameIndex)
                            if (!resolved.isNullOrBlank()) {
                                fileName = resolved
                            }
                        }
                    }
                } catch (_: Exception) {}

                val cleanPath = fileName.trimStart('/')
                val localFile = com.example.util.LargeFileEngine.getLocalFile(context, proj.id, cleanPath)

                // Stream directly from ContentResolver into local disk file using 32KB buffer (0 heap exhaustion)
                contentResolver.openInputStream(uri)?.use { inStream ->
                    com.example.util.LargeFileEngine.streamToFile(inStream, localFile)
                } ?: throw IllegalArgumentException("Could not open file stream")

                val fileSize = localFile.length()
                val isLarge = fileSize > com.example.util.LargeFileEngine.LARGE_FILE_THRESHOLD_BYTES

                val finalContent = if (isLarge) {
                    "[DISK_STORED_FILE:${localFile.name}:SIZE=$fileSize]"
                } else {
                    val text = localFile.bufferedReader(java.nio.charset.StandardCharsets.UTF_8).use { it.readText() }
                        .removePrefix("\uFEFF")
                        .replace("\r\n", "\n")
                    val autoFixed = com.example.util.BotCodeAutoFixer.autoFixFile(text, cleanPath, proj)
                    autoFixed.fixedCode
                }

                val newFileId = repository.createFile(proj.id, cleanPath, finalContent)
                val createdFile = repository.getFileById(newFileId)

                withContext(Dispatchers.Main) {
                    if (createdFile != null) {
                        selectFile(createdFile)
                    }
                    val sizeKb = fileSize / 1024
                    val modeNote = if (isLarge) " (⚡ Large File Mode: ${sizeKb} KB streamed)" else ""
                    onResult?.invoke(true, "Imported '$cleanPath'$modeNote successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Failed to import file: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun autoFixActiveFile(onResult: (Int, List<String>) -> Unit) {
        val file = _activeFile.value ?: return
        val proj = _currentProject.value
        val currentText = _activeFileContent.value
        val fixResult = com.example.util.BotCodeAutoFixer.autoFixFile(currentText, file.filePath, proj)
        if (fixResult.wasModified) {
            updateActiveFileContent(fixResult.fixedCode)
            saveActiveFile()
        }
        onResult(fixResult.fixesApplied.size, fixResult.fixesApplied)
    }

    fun autoFixEntireProject(onResult: (Int, List<String>) -> Unit) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val files = repository.getFilesDirect(proj.id)
            val report = com.example.util.BotCodeAutoFixer.autoFixProject(proj, files)
            for (f in report.updatedFiles) {
                if (f.id == 0L) {
                    repository.createFile(proj.id, f.filePath, f.content)
                } else {
                    repository.saveFile(f)
                }
            }
            withContext(Dispatchers.Main) {
                _activeFile.value?.let { currentActive ->
                    val updated = repository.getFilesDirect(proj.id).find { it.filePath == currentActive.filePath }
                    if (updated != null) {
                        selectFile(updated)
                    }
                }
                onResult(report.fixesApplied.size, report.fixesApplied)
            }
        }
    }

    fun copyProjectCodeToClipboard(context: Context, onResult: (Boolean, String) -> Unit) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = repository.getFilesDirect(project.id)
                val sb = StringBuilder()
                sb.append("// ==================================================\n")
                sb.append("// 🤖 ${project.name} (${project.language})\n")
                sb.append("// Exported from BotStudio - Complete Project Code\n")
                sb.append("// ==================================================\n\n")

                for (file in files) {
                    sb.append("// --------------------------------------------------\n")
                    sb.append("// File: ${file.filePath} ${if (file.isEntrypoint) "(Main Entrypoint)" else ""}\n")
                    sb.append("// --------------------------------------------------\n")
                    sb.append(file.content.trimEnd())
                    sb.append("\n\n")
                }

                withContext(Dispatchers.Main) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("${project.name} Code", sb.toString())
                    clipboard.setPrimaryClip(clip)
                    onResult(true, "All ${files.size} project files copied to clipboard!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to copy: ${e.message}")
                }
            }
        }
    }

    fun getDefaultExportTreeUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences("bot_studio_settings", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("default_export_folder_uri", null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
            if (hasPerm) {
                uri
            } else {
                val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                if (doc != null && doc.canWrite()) uri else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getExportFolderDisplayName(context: Context, treeUri: Uri): String {
        return try {
            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            doc?.name ?: "Selected Folder"
        } catch (_: Exception) {
            "Selected Folder"
        }
    }

    fun setDefaultExportTreeUri(context: Context, treeUri: Uri) {
        try {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (_: Exception) {}
        context.getSharedPreferences("bot_studio_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("default_export_folder_uri", treeUri.toString())
            .apply()
    }

    fun clearDefaultExportTreeUri(context: Context) {
        context.getSharedPreferences("bot_studio_settings", Context.MODE_PRIVATE)
            .edit()
            .remove("default_export_folder_uri")
            .apply()
    }

    fun exportProjectToTree(
        project: BotProject,
        treeUri: Uri,
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val treeDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                    ?: throw IllegalArgumentException("Cannot access selected folder. Please choose a folder again.")

                val safeName = project.name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifEmpty { "bot" }
                val fileName = "${safeName}.zip"

                // Delete old export with same name if it exists so we overwrite cleanly
                val existing = treeDir.findFile(fileName)
                existing?.delete()

                val newZipDoc = treeDir.createFile("application/zip", fileName)
                    ?: throw IllegalArgumentException("Could not create '${fileName}' in '${treeDir.name}'")

                val outputStream = context.contentResolver.openOutputStream(newZipDoc.uri)
                    ?: throw IllegalArgumentException("Could not open destination stream")

                val files = if (_currentProject.value?.id == project.id && _projectFiles.value.isNotEmpty()) {
                    _projectFiles.value
                } else {
                    repository.getFilesDirect(project.id)
                }

                val summary = com.example.util.BotZipManager.exportToZipStream(outputStream, project, files, context)
                val folderName = treeDir.name ?: "folder"
                val message = if (summary.success) {
                    "✅ Exported '${fileName}' to '$folderName'!"
                } else {
                    summary.message
                }

                withContext(Dispatchers.Main) {
                    onResult(summary.success, message)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun exportProjectToZip(
        project: BotProject,
        destinationUri: Uri,
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = repository.getFilesDirect(project.id)
                val outputStream = context.contentResolver.openOutputStream(destinationUri)
                    ?: throw IllegalArgumentException("Could not open destination file for writing")

                val summary = com.example.util.BotZipManager.exportToZipStream(outputStream, project, files, context)
                withContext(Dispatchers.Main) {
                    onResult(summary.success, summary.message)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun shareProjectZip(
        project: BotProject,
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = repository.getFilesDirect(project.id)
                val zipFile = com.example.util.BotZipManager.createZipFileForSharing(context, project, files)
                if (zipFile != null && zipFile.exists()) {
                    val shareUri = com.example.util.BotZipManager.getShareUriForFile(context, zipFile)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "${project.name} Discord Bot Archive")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Exported Discord bot '${project.name}' ready to run.")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    withContext(Dispatchers.Main) {
                        val chooser = android.content.Intent.createChooser(shareIntent, "Export & Share ${project.name} ZIP")
                        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                        onResult(true, "ZIP archive created and ready to share!")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Could not generate shareable ZIP file.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Share failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun createNewFile(fileName: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            // User requested: When I create a file or a bot, it comes with only blank
            val initialContent = ""
            val fileId = repository.createFile(project.id, fileName, initialContent)
            val created = repository.getFileById(fileId)
            showNewFileDialog.value = false
            if (created != null) {
                selectFile(created)
                _currentTab.value = AppTab.EDITOR
            }
        }
    }

    fun clearActiveFile() {
        val file = _activeFile.value ?: return
        clearFileContent(file.id)
    }

    fun clearFileContent(fileId: Long) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val target = _projectFiles.value.firstOrNull { it.id == fileId }
            if (target != null) {
                repository.saveFile(target.copy(content = ""))
            }
            if (_activeFile.value?.id == fileId) {
                _activeFileContent.value = ""
                _saveStatus.value = "Cleared"
            }
            repository.addTerminalLog(project.id, "[EDITOR] Cleared content for file id $fileId", "STDOUT")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            runtimeEngine.stopBot(projectId)
            repository.deleteProject(projectId)
            if (_currentProject.value?.id == projectId) {
                val remaining = allProjects.value.filter { it.id != projectId }
                if (remaining.isNotEmpty()) {
                    selectProject(remaining.first())
                } else {
                    _currentProject.value = null
                    _projectFiles.value = emptyList()
                    _activeFile.value = null
                    _activeFileContent.value = ""
                }
            }
        }
    }

    fun deleteFile(file: BotFile) {
        viewModelScope.launch {
            repository.deleteFile(file.id)
            if (_activeFile.value?.id == file.id) {
                val remaining = _projectFiles.value.filter { it.id != file.id }
                _activeFile.value = null
                _activeFileContent.value = ""
                if (remaining.isNotEmpty()) {
                    selectFile(remaining.first())
                }
            }
        }
    }

    fun updateProjectSettings(
        name: String,
        prefix: String,
        status: String,
        activityType: String,
        activityText: String,
        token: String,
        clientId: String,
        intentMessageContent: Boolean,
        intentGuildMembers: Boolean,
        intentPresences: Boolean
    ) {
        val current = _currentProject.value ?: return
        val cleanToken = runtimeEngine.sanitizeToken(token).ifEmpty { current.botToken }
        val updated = current.copy(
            name = name,
            prefix = prefix,
            status = status,
            activityType = activityType,
            activityText = activityText,
            botToken = cleanToken,
            clientId = clientId.trim(),
            intentMessageContent = intentMessageContent,
            intentGuildMembers = intentGuildMembers,
            intentPresences = intentPresences
        )
        _currentProject.value = updated
        viewModelScope.launch {
            repository.updateProject(updated)
        }
    }

    fun updateBotToken(rawToken: String) {
        val current = _currentProject.value ?: return
        val sanitized = runtimeEngine.sanitizeToken(rawToken)
        if (sanitized.isBlank()) return
        val updated = current.copy(botToken = sanitized)
        _currentProject.value = updated
        viewModelScope.launch {
            repository.updateProject(updated)
        }
    }

    val tokenVerificationState = MutableStateFlow<String?>(null)
    val isVerifyingToken = MutableStateFlow(false)

    fun testToken(rawToken: String) {
        val sanitized = runtimeEngine.sanitizeToken(rawToken)
        viewModelScope.launch {
            isVerifyingToken.value = true
            tokenVerificationState.value = "Testing token with Discord API..."
            val (success, message) = runtimeEngine.verifyDiscordToken(sanitized)
            tokenVerificationState.value = message
            isVerifyingToken.value = false

            // If token is verified or non-empty, auto-save to current project
            if (sanitized.isNotBlank()) {
                val current = _currentProject.value
                if (current != null) {
                    val updated = current.copy(botToken = sanitized)
                    _currentProject.value = updated
                    repository.updateProject(updated)
                }
            }
        }
    }

    fun updateProjectTokenAndStart(token: String) {
        val project = _currentProject.value ?: return
        val sanitized = runtimeEngine.sanitizeToken(token)
        viewModelScope.launch {
            val updated = project.copy(botToken = sanitized)
            _currentProject.value = updated
            repository.updateProject(updated)
            showTokenInputDialog.value = false
            startBotProcess()
        }
    }

    fun startBotProcess() {
        val project = _currentProject.value ?: return
        saveActiveFile()
        val cleanToken = runtimeEngine.sanitizeToken(project.botToken)

        // Check if token is present and valid
        if (!runtimeEngine.isRealToken(cleanToken)) {
            showTokenInputDialog.value = true
            return
        }

        val cleanProject = if (cleanToken != project.botToken) project.copy(botToken = cleanToken) else project
        val currentFiles = _projectFiles.value
        runtimeEngine.updateCachedFiles(cleanProject.id, currentFiles)
        runtimeEngine.startBot(cleanProject, currentFiles)

        // Run bot in background with Foreground Service and WakeLock
        try {
            com.example.service.BotBackgroundService.start(getApplication(), project.id, project.name)
        } catch (e: Exception) {
            // Service startup fallback
        }
    }

    fun stopBotProcess() {
        val project = _currentProject.value ?: return
        runtimeEngine.stopBot(project.id)

        // Stop background service
        try {
            com.example.service.BotBackgroundService.stop(getApplication())
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun restartBotProcess() {
        val project = _currentProject.value ?: return
        saveActiveFile()
        val currentFiles = _projectFiles.value
        runtimeEngine.updateCachedFiles(project.id, currentFiles)
        runtimeEngine.restartBot(project)
    }

    val gatewayStatus: StateFlow<com.example.engine.GatewayStatus> = runtimeEngine.gatewayStatus
    val runningProjectId: StateFlow<Long?> = runtimeEngine.runningProjectId
    val voiceConnectionState = runtimeEngine.voiceEngine.connectionState
    val isDaveActive = runtimeEngine.voiceEngine.isDaveActive

    fun hotReload() {
        val project = _currentProject.value ?: return
        saveActiveFile()
        val currentFiles = _projectFiles.value
        runtimeEngine.updateCachedFiles(project.id, currentFiles)
        runtimeEngine.hotReload(project.id)
    }

    fun syncGuildCommands(guildId: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val (success, msg) = runtimeEngine.syncGuildCommands(project, guildId)
            onResult(success, msg)
        }
    }

    val pm2Processes = com.example.engine.Pm2ProcessManager.processes
    val isPm2DaemonActive = com.example.engine.Pm2ProcessManager.isDaemonActive
    fun runPm2Script(script: String? = null) = runScriptWithPm2(script)

    fun runScriptWithPm2(script: String? = null) {
        val project = _currentProject.value ?: return
        saveActiveFile()
        val files = _projectFiles.value
        val targetScript = script ?: files.firstOrNull { it.isEntrypoint }?.filePath
            ?: files.firstOrNull { it.filePath == "index.js" || it.filePath == "bot.py" }?.filePath
            ?: "index.js"

        addPm2ToPackageJson()

        val cleanToken = runtimeEngine.sanitizeToken(project.botToken)
        val cleanProject = if (cleanToken != project.botToken) project.copy(botToken = cleanToken) else project

        com.example.engine.Pm2ProcessManager.startScript(
            script = targetScript,
            name = project.name.lowercase().replace(" ", "-"),
            project = cleanProject,
            files = files,
            engine = runtimeEngine,
            watch = true
        )

        try {
            com.example.service.BotBackgroundService.start(getApplication(), project.id, project.name)
        } catch (_: Exception) {}

        runtimeEngine.logAsync(project.id, "🚀 [AUTO PM2] Started '$targetScript' under PM2 24/7 Process Manager!", "SUCCESS")
        runtimeEngine.logAsync(project.id, "⚡ Script will stay ALIVE 24/7 with zero downtime even when app is closed.", "SUCCESS")
    }

    fun stopPm2Script(target: String = "all") {
        val project = _currentProject.value ?: return
        com.example.engine.Pm2ProcessManager.stopProcess(target, runtimeEngine, project.id)
        runtimeEngine.logAsync(project.id, "🛑 [PM2] Stopped process [$target].", "WARN")
    }

    fun addPm2ToPackageJson() {
        val current = _currentProject.value ?: return
        viewModelScope.launch {
            val files = repository.getFilesDirect(current.id)
            val pkgFile = files.find { it.filePath == "package.json" }
            if (pkgFile != null) {
                val content = pkgFile.content
                if (!content.contains("\"pm2\"")) {
                    val updatedContent = if (content.contains("\"scripts\": {")) {
                        content.replace(
                            "\"scripts\": {",
                            "\"scripts\": {\n    \"pm2\": \"pm2 start index.js --name bot\",\n    \"pm2:24/7\": \"pm2 start index.js --name bot --max-restarts 999\",\n    \"pm2:status\": \"pm2 status\",\n    \"pm2:stop\": \"pm2 stop bot\","
                        )
                    } else if (content.contains("\"scripts\":{")) {
                        content.replace(
                            "\"scripts\":{",
                            "\"scripts\":{\n    \"pm2\": \"pm2 start index.js --name bot\",\n    \"pm2:24/7\": \"pm2 start index.js --name bot --max-restarts 999\","
                        )
                    } else {
                        content.replaceFirst(
                            "{",
                            "{\n  \"scripts\": {\n    \"start\": \"node index.js\",\n    \"pm2\": \"pm2 start index.js --name bot\",\n    \"pm2:24/7\": \"pm2 start index.js --name bot --max-restarts 999\"\n  },"
                        )
                    }
                    val updated = pkgFile.copy(content = updatedContent, updatedAt = System.currentTimeMillis())
                    repository.saveFile(updated)
                    runtimeEngine.logAsync(current.id, "📦 [PM2] Added 24/7 PM2 scripts to package.json!", "SUCCESS")
                }
            } else {
                repository.createFile(
                    projectId = current.id,
                    filePath = "package.json",
                    content = """{
  "name": "${current.name.lowercase().replace(" ", "-")}",
  "version": "1.0.0",
  "main": "index.js",
  "scripts": {
    "start": "node index.js",
    "pm2": "pm2 start index.js --name bot",
    "pm2:24/7": "pm2 start index.js --name bot --max-restarts 999",
    "pm2:status": "pm2 status",
    "pm2:stop": "pm2 stop bot"
  },
  "dependencies": {
    "discord.js": "^14.15.3",
    "dotenv": "^16.4.5"
  }
}"""
                )
                runtimeEngine.logAsync(current.id, "📦 [PM2] Created package.json with Auto PM2 24/7 scripts!", "SUCCESS")
            }
        }
    }

    fun setTerminalInput(input: String) {
        _terminalInput.value = input
    }

    fun executeTerminalCommand() {
        val cmd = _terminalInput.value.trim()
        val project = _currentProject.value ?: return
        if (cmd.isEmpty()) return

        _commandHistory.value = _commandHistory.value + cmd
        historyIndex = _commandHistory.value.size
        _terminalInput.value = ""

        runtimeEngine.executeTerminalCommand(cmd, project, _projectFiles.value)
    }

    fun executeQuickTerminalCommand(cmd: String) {
        val project = _currentProject.value ?: return
        runtimeEngine.executeTerminalCommand(cmd, project, _projectFiles.value)
    }

    fun clearTerminal() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            repository.clearTerminalLogs(project.id)
        }
    }

    fun navigateCommandHistory(up: Boolean) {
        val history = _commandHistory.value
        if (history.isEmpty()) return
        if (up) {
            if (historyIndex > 0) {
                historyIndex--
                _terminalInput.value = history[historyIndex]
            } else if (historyIndex == 0) {
                _terminalInput.value = history[0]
            } else {
                historyIndex = history.size - 1
                _terminalInput.value = history[historyIndex]
            }
        } else {
            if (historyIndex < history.size - 1) {
                historyIndex++
                _terminalInput.value = history[historyIndex]
            } else {
                historyIndex = history.size
                _terminalInput.value = ""
            }
        }
    }

    fun insertPackageImport(importStatement: String) {
        val current = _activeFileContent.value
        val newContent = importStatement.trim() + "\n" + current
        updateActiveFileContent(newContent)
        saveActiveFile()
    }

    fun insertTextAtCursor(text: String) {
        val newContent = _activeFileContent.value + text
        updateActiveFileContent(newContent)
    }

    fun insertSnippet(snippetCode: String) {
        val current = _activeFileContent.value
        val separator = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        val newContent = current + separator + snippetCode.trimIndent() + "\n"
        updateActiveFileContent(newContent)
        showSnippetsDialog.value = false
    }

    // Embed Builder actions
    fun setEmbedTitle(title: String) { _embedTitle.value = title }
    fun setEmbedDescription(desc: String) { _embedDescription.value = desc }
    fun setEmbedColor(colorHex: String) { _embedColorHex.value = colorHex }
    fun setEmbedAuthor(author: String) { _embedAuthorName.value = author }
    fun setEmbedFooter(footer: String) { _embedFooterText.value = footer }

    fun addEmbedField(name: String, value: String, inline: Boolean = true) {
        _embedFields.value = _embedFields.value + EmbedFieldItem(name, value, inline)
    }

    fun removeEmbedField(index: Int) {
        if (index in _embedFields.value.indices) {
            _embedFields.value = _embedFields.value.toMutableList().also { it.removeAt(index) }
        }
    }

    fun addEmbedButton(label: String, style: String, emoji: String) {
        _embedButtons.value = _embedButtons.value + EmbedButtonItem(label, style, emoji)
    }

    fun removeEmbedButton(index: Int) {
        if (index in _embedButtons.value.indices) {
            _embedButtons.value = _embedButtons.value.toMutableList().also { it.removeAt(index) }
        }
    }

    fun setEmbedSelectPlaceholder(placeholder: String) {
        _embedSelectMenuPlaceholder.value = placeholder
    }

    fun addEmbedSelectOption(label: String, value: String, description: String = "", emoji: String = "") {
        _embedSelectOptions.value = _embedSelectOptions.value + EmbedSelectOptionItem(label, value, description, emoji)
    }

    fun removeEmbedSelectOption(index: Int) {
        if (index in _embedSelectOptions.value.indices) {
            _embedSelectOptions.value = _embedSelectOptions.value.toMutableList().also { it.removeAt(index) }
        }
    }

    fun generateEmbedCode(targetLanguage: String): String {
        val title = _embedTitle.value
        val desc = _embedDescription.value
        val colorHex = _embedColorHex.value.removePrefix("#")
        val author = _embedAuthorName.value
        val footer = _embedFooterText.value
        val fields = _embedFields.value
        val buttons = _embedButtons.value
        val selectOptions = _embedSelectOptions.value
        val placeholder = _embedSelectMenuPlaceholder.value

        return when {
            targetLanguage.contains("Python", ignoreCase = true) -> {
                val fieldsCode = fields.joinToString("\n") {
                    "embed.add_field(name=\"${it.name}\", value=\"${it.value}\", inline=${if (it.inline) "True" else "False"})"
                }
                """
embed = discord.Embed(
    title="$title",
    description="$desc",
    color=0x$colorHex
)
${if (author.isNotBlank()) "embed.set_author(name=\"$author\")" else ""}
$fieldsCode
${if (footer.isNotBlank()) "embed.set_footer(text=\"$footer\")" else ""}

class InteractiveView(discord.ui.View):
    def __init__(self):
        super().__init__(timeout=None)
${buttons.mapIndexed { idx, b ->
"""    @discord.ui.button(label="${b.label}", style=discord.ButtonStyle.${b.style.lowercase()}${if (b.emoji.isNotBlank()) ", emoji='${b.emoji}'" else ""})
    async def btn_$idx(self, interaction: discord.Interaction, button: discord.ui.Button):
        await interaction.response.send_message(f"Clicked ${b.label}!", ephemeral=True)"""
}.joinToString("\n\n")}

    @discord.ui.select(
        placeholder="$placeholder",
        options=[
${selectOptions.joinToString(",\n") { "            discord.SelectOption(label=\"${it.label}\", value=\"${it.value}\"${if (it.description.isNotBlank()) ", description=\"${it.description}\"" else ""}${if (it.emoji.isNotBlank()) ", emoji=\"${it.emoji}\"" else ""})" }}
        ]
    )
    async def select_callback(self, interaction: discord.Interaction, select: discord.ui.Select):
        await interaction.response.send_message(f"Selected: {select.values[0]}", ephemeral=True)

await interaction.response.send_message(embed=embed, view=InteractiveView())
""".trimIndent()
            }
            targetLanguage.contains("Rust", ignoreCase = true) -> {
                """
let embed = serenity::CreateEmbed::default()
    .title("$title")
    .description("$desc")
    .color(0x$colorHex)
    .footer(serenity::CreateEmbedFooter::new("$footer"));

let reply = poise::CreateReply::default().embed(embed);
ctx.send(reply).await?;
""".trimIndent()
            }
            else -> {
                // Default JavaScript discord.js v14
                val fieldsCode = fields.joinToString(",\n") {
                    "    { name: '${it.name}', value: '${it.value}', inline: ${it.inline} }"
                }
                val buttonsCode = buttons.mapIndexed { idx, b ->
                    """new ButtonBuilder().setCustomId('btn_$idx').setLabel('${b.label}').setStyle(ButtonStyle.${b.style})${if (b.emoji.isNotBlank()) ".setEmoji('${b.emoji}')" else ""}"""
                }.joinToString(",\n        ")

                val selectCode = """new StringSelectMenuBuilder()
      .setCustomId('custom_select_menu')
      .setPlaceholder('$placeholder')
      .addOptions(
${selectOptions.joinToString(",\n") { opt ->
"""        new StringSelectMenuOptionBuilder().setLabel('${opt.label}').setValue('${opt.value}')${if (opt.description.isNotBlank()) ".setDescription('${opt.description}')" else ""}${if (opt.emoji.isNotBlank()) ".setEmoji('${opt.emoji}')" else ""}"""
}}
      )"""

                """
const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, StringSelectMenuBuilder, StringSelectMenuOptionBuilder } = require('discord.js');

const embed = new EmbedBuilder()
  .setTitle('$title')
  .setDescription('$desc')
  .setColor(0x$colorHex)
${if (author.isNotBlank()) "  .setAuthor({ name: '$author' })" else ""}
  .addFields(
$fieldsCode
  )
${if (footer.isNotBlank()) "  .setFooter({ text: '$footer' })" else ""}
  .setTimestamp();

const buttonRow = new ActionRowBuilder()
  .addComponents(
    $buttonsCode
  );

const selectRow = new ActionRowBuilder()
  .addComponents(
    $selectCode
  );

await interaction.reply({
  embeds: [embed],
  components: [buttonRow, selectRow]
});
""".trimIndent()
            }
        }
    }
}
