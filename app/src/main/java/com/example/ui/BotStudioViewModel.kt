package com.example.ui

import android.app.Application
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String, val iconName: String) {
    EDITOR("Editor", "code"),
    TERMINAL("Terminal", "terminal"),
    SIMULATOR("Discord Simulator", "chat"),
    EMBED_BUILDER("Embed Designer", "dashboard_customize"),
    STORAGE("Storage Manager", "storage"),
    EXTENSIONS("Extensions", "extension"),
    GRADLE("Gradle", "build"),
    AI_ASSISTANT("AI Studio", "auto_awesome"),
    API_KEYS("AI Models & Keys", "key"),
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

    val runtimeEngine = BotRuntimeEngine(repository, viewModelScope)

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

    private val _currentTab = MutableStateFlow(AppTab.EDITOR)
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
    val generatedCodeText = MutableStateFlow("")

    private var projectFilesJob: Job? = null
    private var terminalLogsJob: Job? = null
    private var storageJob: Job? = null
    private var diagnosticsJob: Job? = null

    init {
        viewModelScope.launch {
            allProjects.collect { projects ->
                if (_currentProject.value == null && projects.isNotEmpty()) {
                    selectProject(projects.first())
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

        // Asynchronously persist any modifications on the previous file without mutating activeFile
        if (currentFile != null) {
            val contentToSave = _activeFileContent.value
            if (contentToSave != currentFile.content) {
                val fileToSave = currentFile.copy(content = contentToSave)
                viewModelScope.launch {
                    repository.saveFile(fileToSave)
                }
            }
        }

        // Switch to the target file immediately
        _activeFile.value = file
        _activeFileContent.value = file.content
        _saveStatus.value = "Saved"

        // Run diagnostics on background dispatcher
        diagnosticsJob?.cancel()
        runDiagnostics(file.content, file.filePath)
    }

    fun updateActiveFileContent(newContent: String) {
        _activeFileContent.value = newContent

        // Smooth background debounced linting to prevent typing stutter
        diagnosticsJob?.cancel()
        diagnosticsJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            delay(400)
            val currentFile = _activeFile.value
            if (currentFile != null) {
                val diags = com.example.ui.editor.CodeLinter.lintCode(newContent, currentFile.filePath)
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
        viewModelScope.launch {
            val updatedFile = file.copy(content = currentContent)
            repository.saveFile(updatedFile)
            // CRITICAL: Only update _activeFile.value if the user hasn't switched to another file
            if (_activeFile.value?.id == updatedFile.id) {
                _activeFile.value = updatedFile
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _saveStatus.value = "Saved at $timeStr"
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
            }
            showNewProjectDialog.value = false
        }
    }

    fun createNewFile(fileName: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val fileId = repository.createFile(project.id, fileName, "")
            val created = repository.getFileById(fileId)
            showNewFileDialog.value = false
            if (created != null) {
                selectFile(created)
                _currentTab.value = AppTab.EDITOR
            }
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

    fun startBotProcess() {
        val project = _currentProject.value ?: return
        saveActiveFile()
        val cleanToken = runtimeEngine.sanitizeToken(project.botToken)
        val cleanProject = if (cleanToken != project.botToken) project.copy(botToken = cleanToken) else project
        runtimeEngine.startBot(cleanProject)
    }

    fun stopBotProcess() {
        val project = _currentProject.value ?: return
        runtimeEngine.stopBot(project.id)
    }

    fun restartBotProcess() {
        val project = _currentProject.value ?: return
        saveActiveFile()
        runtimeEngine.restartBot(project)
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
