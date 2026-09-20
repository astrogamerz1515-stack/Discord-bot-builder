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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val iconName: String) {
    EDITOR("Editor", "code"),
    TERMINAL("Terminal", "terminal"),
    SIMULATOR("Discord Simulator", "chat"),
    EMBED_BUILDER("Embed Designer", "dashboard_customize"),
    STORAGE("Database KV", "storage"),
    AI_ASSISTANT("AI Assistant", "auto_awesome"),
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
    private var historyIndex = -1

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

        // Observe files for this project
        viewModelScope.launch {
            repository.getFiles(project.id).collect { files ->
                _projectFiles.value = files
                if (_activeFile.value == null || !_projectFiles.value.any { it.id == _activeFile.value?.id }) {
                    val entrypoint = files.find { it.isEntrypoint } ?: files.firstOrNull()
                    if (entrypoint != null) {
                        selectFile(entrypoint)
                    }
                }
            }
        }

        // Observe terminal logs for this project
        viewModelScope.launch {
            repository.getTerminalLogs(project.id).collect { logs ->
                _terminalLogs.value = logs
            }
        }

        // Observe storage key-values for this project
        viewModelScope.launch {
            repository.getStorage(project.id).collect { entries ->
                _storageEntries.value = entries
            }
        }
    }

    fun selectFile(file: BotFile) {
        // Auto-save previous file if modified
        saveActiveFile()
        _activeFile.value = file
        _activeFileContent.value = file.content
        runDiagnostics(file.content, file.filePath)
    }

    fun updateActiveFileContent(newContent: String) {
        _activeFileContent.value = newContent
        _activeFile.value?.let { runDiagnostics(newContent, it.filePath) }
    }

    fun runDiagnostics(code: String, filePath: String) {
        _codeDiagnostics.value = com.example.ui.editor.CodeLinter.lintCode(code, filePath)
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
        if (file.content != currentContent) {
            viewModelScope.launch {
                repository.saveFile(file.copy(content = currentContent))
            }
        }
    }

    fun setTab(tab: AppTab) {
        if (_currentTab.value == AppTab.EDITOR && tab != AppTab.EDITOR) {
            saveActiveFile()
        }
        _currentTab.value = tab
    }

    fun createNewProject(name: String, description: String, language: BotLanguage, prefix: String) {
        viewModelScope.launch {
            val newId = repository.createProject(name, description, language, prefix)
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
            val created = repository.getFiles(project.id)
            showNewFileDialog.value = false
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
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
                if (remaining.isNotEmpty()) {
                    selectFile(remaining.first())
                } else {
                    _activeFile.value = null
                    _activeFileContent.value = ""
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
        val updated = current.copy(
            name = name,
            prefix = prefix,
            status = status,
            activityType = activityType,
            activityText = activityText,
            botToken = token,
            clientId = clientId,
            intentMessageContent = intentMessageContent,
            intentGuildMembers = intentGuildMembers,
            intentPresences = intentPresences
        )
        _currentProject.value = updated
        viewModelScope.launch {
            repository.updateProject(updated)
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

    fun insertTextAtCursor(text: String) {
        _activeFileContent.value += text
    }

    fun insertSnippet(snippetCode: String) {
        _activeFileContent.value += "\n" + snippetCode.trimIndent() + "\n"
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
