package com.example.ui.visualbuilder

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data model for simulated Discord messages in the live test simulator.
 */
data class SimulatedDiscordMessage(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val isBot: Boolean = false,
    val avatarColorHex: String = "#5865F2",
    val content: String = "",
    val timestamp: String = "",
    val embedTitle: String? = null,
    val embedDescription: String? = null,
    val embedColorHex: String? = null,
    val reactions: List<String> = emptyList()
)

/**
 * ViewModel managing block canvas state, drag-and-drop operations,
 * block configuration editing, undo/redo history, multi-language code export (Python & JS),
 * and interactive real-time Discord bot simulation.
 */
class VisualBuilderViewModel : ViewModel() {

    // Canvas blocks list
    private val _blocks = MutableStateFlow<List<VisualBlock>>(
        listOf(
            VisualBlock(
                id = "seed-event-1",
                category = BlockCategory.EVENT,
                title = "When message received",
                subtitle = "triggers on any server message",
                icon = "⚡"
            ),
            VisualBlock(
                id = "seed-logic-2",
                category = BlockCategory.LOGIC,
                title = "If ... then",
                subtitle = "if message.content == \"!ping\"",
                icon = "🔀",
                conditionField = "message.content",
                conditionOperator = "==",
                conditionValue = "!ping",
                isContainer = true
            ),
            VisualBlock(
                id = "seed-message-3",
                category = BlockCategory.MESSAGE,
                title = "Reply to message",
                subtitle = "\"Pong! 🏓 Latency: 24ms\"",
                icon = "↩️",
                messageContent = "Pong! 🏓 Latency: 24ms",
                indentLevel = 1
            ),
            VisualBlock(
                id = "seed-reaction-4",
                category = BlockCategory.MESSAGE,
                title = "Add reaction",
                subtitle = "add 🏓 to message",
                icon = "✨",
                reactionEmoji = "🏓",
                indentLevel = 1
            )
        )
    )
    val blocks: StateFlow<List<VisualBlock>> = _blocks.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<List<VisualBlock>>()
    private val redoStack = mutableListOf<List<VisualBlock>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Drag-and-drop state
    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    private val _draggedBlock = MutableStateFlow<VisualBlock?>(null)
    val draggedBlock: StateFlow<VisualBlock?> = _draggedBlock.asStateFlow()

    private val _dragPosition = MutableStateFlow(Offset.Zero)
    val dragPosition: StateFlow<Offset> = _dragPosition.asStateFlow()

    private val _dropTargetIndex = MutableStateFlow<Int?>(null)
    val dropTargetIndex: StateFlow<Int?> = _dropTargetIndex.asStateFlow()

    // Active executing block ID (for live canvas glow during simulation)
    private val _activeExecutingBlockId = MutableStateFlow<String?>(null)
    val activeExecutingBlockId: StateFlow<String?> = _activeExecutingBlockId.asStateFlow()

    // Canvas Zoom State (0.6f to 2.0f)
    private val _canvasZoom = MutableStateFlow(1.0f)
    val canvasZoom: StateFlow<Float> = _canvasZoom.asStateFlow()

    fun zoomIn() {
        _canvasZoom.value = (_canvasZoom.value + 0.15f).coerceAtMost(2.0f)
    }

    fun zoomOut() {
        _canvasZoom.value = (_canvasZoom.value - 0.15f).coerceAtLeast(0.6f)
    }

    fun resetZoom() {
        _canvasZoom.value = 1.0f
    }

    fun setZoom(scale: Float) {
        _canvasZoom.value = scale.coerceIn(0.5f, 2.5f)
    }

    fun dropOnTrash(blockId: String) {
        removeBlock(blockId)
        viewModelScope.launch {
            _snackbarMessage.emit("Block moved to trash 🗑️")
        }
    }

    // Palette & Category State
    private val _selectedCategory = MutableStateFlow(BlockCategory.EVENT)
    val selectedCategory: StateFlow<BlockCategory> = _selectedCategory.asStateFlow()

    private val _isPaletteExpanded = MutableStateFlow(true)
    val isPaletteExpanded: StateFlow<Boolean> = _isPaletteExpanded.asStateFlow()

    // Dialogs & Sheets State
    private val _selectedBlockForEdit = MutableStateFlow<VisualBlock?>(null)
    val selectedBlockForEdit: StateFlow<VisualBlock?> = _selectedBlockForEdit.asStateFlow()

    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    private val _selectedExportLanguage = MutableStateFlow("Python") // "Python" or "JavaScript"
    val selectedExportLanguage: StateFlow<String> = _selectedExportLanguage.asStateFlow()

    private val _isRecipeDialogOpen = MutableStateFlow(false)
    val isRecipeDialogOpen: StateFlow<Boolean> = _isRecipeDialogOpen.asStateFlow()

    // Simulation State
    private val _showSimulationSheet = MutableStateFlow(false)
    val showSimulationSheet: StateFlow<Boolean> = _showSimulationSheet.asStateFlow()

    private val _isSimulationRunning = MutableStateFlow(false)
    val isSimulationRunning: StateFlow<Boolean> = _isSimulationRunning.asStateFlow()

    private val _simulationLogs = MutableStateFlow<List<String>>(emptyList())
    val simulationLogs: StateFlow<List<String>> = _simulationLogs.asStateFlow()

    private val _simulatedMessages = MutableStateFlow<List<SimulatedDiscordMessage>>(emptyList())
    val simulatedMessages: StateFlow<List<SimulatedDiscordMessage>> = _simulatedMessages.asStateFlow()

    private var simulationJob: Job? = null

    // Snackbar notifications
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private fun pushHistoryState() {
        undoStack.add(_blocks.value.toList())
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_blocks.value.toList())
            val previous = undoStack.removeAt(undoStack.lastIndex)
            _blocks.value = previous
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            viewModelScope.launch { _snackbarMessage.emit("Undone") }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_blocks.value.toList())
            val next = redoStack.removeAt(redoStack.lastIndex)
            _blocks.value = next
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            viewModelScope.launch { _snackbarMessage.emit("Redone") }
        }
    }

    fun selectCategory(category: BlockCategory) {
        _selectedCategory.value = category
    }

    fun togglePalette() {
        _isPaletteExpanded.value = !_isPaletteExpanded.value
    }

    fun openEditSheet(block: VisualBlock) {
        _selectedBlockForEdit.value = block
    }

    fun closeEditSheet() {
        _selectedBlockForEdit.value = null
    }

    fun openExportDialog() {
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun selectExportLanguage(lang: String) {
        _selectedExportLanguage.value = lang
    }

    fun openRecipeDialog() {
        _isRecipeDialogOpen.value = true
    }

    fun closeRecipeDialog() {
        _isRecipeDialogOpen.value = false
    }

    fun loadRecipe(recipe: BotRecipe) {
        pushHistoryState()
        _blocks.value = recipe.blocks
        _isRecipeDialogOpen.value = false
        viewModelScope.launch {
            _snackbarMessage.emit("Loaded recipe: ${recipe.name}")
        }
    }

    fun clearCanvas() {
        if (_blocks.value.isEmpty()) return
        pushHistoryState()
        _blocks.value = emptyList()
        viewModelScope.launch {
            _snackbarMessage.emit("Canvas cleared")
        }
    }

    fun startDragFromPalette(template: BlockTemplate, initialOffset: Offset) {
        val newBlock = createBlockFromTemplate(template)
        _draggedBlock.value = newBlock
        _dragPosition.value = initialOffset
        _isDragging.value = true
        _dropTargetIndex.value = _blocks.value.size
    }

    fun startDragCanvasBlock(block: VisualBlock, initialOffset: Offset) {
        _draggedBlock.value = block
        _dragPosition.value = initialOffset
        _isDragging.value = true
    }

    fun updateDrag(delta: Offset, canvasY: Float? = null) {
        _dragPosition.value = Offset(
            _dragPosition.value.x + delta.x,
            _dragPosition.value.y + delta.y
        )
        if (canvasY != null) {
            val approximateBlockHeightPx = 150f
            val index = (canvasY / approximateBlockHeightPx).toInt().coerceIn(0, _blocks.value.size)
            _dropTargetIndex.value = index
        }
    }

    fun endDrag(success: Boolean) {
        val block = _draggedBlock.value
        val targetIdx = _dropTargetIndex.value ?: _blocks.value.size

        if (success && block != null) {
            pushHistoryState()
            val currentList = _blocks.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == block.id }
            if (existingIndex >= 0) {
                currentList.removeAt(existingIndex)
                val safeIdx = targetIdx.coerceIn(0, currentList.size)
                currentList.add(safeIdx, block)
            } else {
                val safeIdx = targetIdx.coerceIn(0, currentList.size)
                currentList.add(safeIdx, block)
            }
            _blocks.value = currentList
        }

        _isDragging.value = false
        _draggedBlock.value = null
        _dropTargetIndex.value = null
    }

    fun addBlockFromPalette(template: BlockTemplate, dropIndex: Int = -1) {
        pushHistoryState()
        val newBlock = createBlockFromTemplate(template)
        val currentList = _blocks.value.toMutableList()
        if (dropIndex in 0..currentList.size) {
            currentList.add(dropIndex, newBlock)
        } else {
            currentList.add(newBlock)
        }
        _blocks.value = currentList
        viewModelScope.launch {
            _snackbarMessage.emit("Added ${newBlock.title}")
        }
    }

    private fun createBlockFromTemplate(template: BlockTemplate): VisualBlock {
        val hasContainerParent = _blocks.value.any { it.isContainer }
        val defaultIndent = if (hasContainerParent && template.category != BlockCategory.EVENT && !template.isContainer) 1 else 0

        return VisualBlock(
            category = template.category,
            title = template.title,
            subtitle = template.subtitle,
            icon = template.icon,
            indentLevel = defaultIndent,
            isContainer = template.isContainer,
            messageContent = template.defaultMessage,
            targetChannel = template.defaultChannel,
            targetUser = template.defaultUser,
            conditionField = template.defaultConditionField,
            conditionOperator = template.defaultConditionOp,
            conditionValue = template.defaultConditionVal,
            waitSeconds = template.defaultWait,
            roleName = template.defaultRole,
            purgeCount = template.defaultPurge,
            reactionEmoji = template.defaultEmoji,
            embedTitle = template.defaultEmbedTitle,
            slashCommandName = template.defaultSlashName,
            slashCommandDesc = template.defaultSlashDesc,
            requiredPermission = template.defaultPermission,
            variableName = template.defaultVarName,
            variableValue = template.defaultVarVal,
            buttonLabel = template.defaultButtonLabel,
            buttonCustomId = template.defaultButtonCustomId,
            selectCustomId = template.defaultSelectCustomId,
            modalCustomId = template.defaultModalCustomId,
            cronSchedule = template.defaultCronSchedule,
            discordObjectType = template.defaultDiscordObjectType,
            objectProperty = template.defaultObjectProperty,
            httpMethod = template.defaultHttpMethod,
            httpUrl = template.defaultHttpUrl,
            jsonPath = template.defaultJsonPath
        )
    }

    fun moveBlockUp(blockId: String) {
        val list = _blocks.value.toMutableList()
        val idx = list.indexOfFirst { it.id == blockId }
        if (idx > 0) {
            pushHistoryState()
            val item = list.removeAt(idx)
            list.add(idx - 1, item)
            _blocks.value = list
        }
    }

    fun moveBlockDown(blockId: String) {
        val list = _blocks.value.toMutableList()
        val idx = list.indexOfFirst { it.id == blockId }
        if (idx >= 0 && idx < list.size - 1) {
            pushHistoryState()
            val item = list.removeAt(idx)
            list.add(idx + 1, item)
            _blocks.value = list
        }
    }

    fun removeBlock(id: String) {
        pushHistoryState()
        _blocks.value = _blocks.value.filterNot { it.id == id }
        viewModelScope.launch {
            _snackbarMessage.emit("Block removed")
        }
    }

    fun duplicateBlock(id: String) {
        val currentList = _blocks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            pushHistoryState()
            val original = currentList[index]
            val copy = original.copy(id = UUID.randomUUID().toString())
            currentList.add(index + 1, copy)
            _blocks.value = currentList
            viewModelScope.launch {
                _snackbarMessage.emit("Duplicated ${original.title}")
            }
        }
    }

    fun updateBlock(updated: VisualBlock) {
        pushHistoryState()
        _blocks.value = _blocks.value.map { if (it.id == updated.id) updated else it }
        _selectedBlockForEdit.value = null
        viewModelScope.launch {
            _snackbarMessage.emit("Updated ${updated.title}")
        }
    }

    fun saveScript() {
        viewModelScope.launch {
            _snackbarMessage.emit("💾 Script saved (${_blocks.value.size} blocks)")
        }
    }

    fun openSimulation() {
        _showSimulationSheet.value = true
        if (_simulatedMessages.value.isEmpty()) {
            runSimulation()
        }
    }

    fun closeSimulationSheet() {
        _showSimulationSheet.value = false
        _activeExecutingBlockId.value = null
    }

    /**
     * Executes the visual bot logic against an incoming simulated message or event.
     */
    fun sendSimulatedMessage(userText: String) {
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMsg = SimulatedDiscordMessage(
            authorName = "Alex (User)",
            isBot = false,
            avatarColorHex = "#3D7EFF",
            content = userText,
            timestamp = "Today at $timeNow"
        )
        _simulatedMessages.value = _simulatedMessages.value + userMsg

        executeBlocksWithInput(userText)
    }

    fun runSimulation() {
        simulationJob?.cancel()
        _showSimulationSheet.value = true
        _isSimulationRunning.value = true

        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        _simulatedMessages.value = listOf(
            SimulatedDiscordMessage(
                authorName = "Alex (User)",
                isBot = false,
                avatarColorHex = "#3D7EFF",
                content = "!ping",
                timestamp = "Today at $timeNow"
            )
        )
        _simulationLogs.value = listOf("🚀 Initializing Visual Bot Runtime...")

        executeBlocksWithInput("!ping")
    }

    private fun executeBlocksWithInput(inputContent: String) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _isSimulationRunning.value = true
            delay(300)

            val currentBlocks = _blocks.value
            if (currentBlocks.isEmpty()) {
                addLog("⚠️ Canvas is empty. Drag or tap blocks from the palette.")
                _isSimulationRunning.value = false
                return@launch
            }

            addLog("🌐 Connected to Discord Gateway as VisualBot#0001 (Ping: 22ms)")
            delay(300)

            var conditionPassed = true
            var isInsideContainer = false

            for (block in currentBlocks) {
                _activeExecutingBlockId.value = block.id

                when (block.category) {
                    BlockCategory.EVENT -> {
                        addLog("⚡ [EVENT] ${block.title} triggered for \"$inputContent\"")
                        delay(250)
                    }
                    BlockCategory.LOGIC -> {
                        if (block.title.contains("If", ignoreCase = true)) {
                            val op = block.conditionOperator
                            val target = block.conditionValue
                            conditionPassed = when (op) {
                                "==" -> inputContent.equals(target, ignoreCase = true)
                                "!=" -> !inputContent.equals(target, ignoreCase = true)
                                "contains" -> inputContent.contains(target, ignoreCase = true)
                                "startswith" -> inputContent.startsWith(target, ignoreCase = true)
                                else -> true
                            }
                            isInsideContainer = true
                            addLog("🔀 [LOGIC] Evaluated (${block.conditionField} $op \"$target\") -> ${if (conditionPassed) "TRUE ✅" else "FALSE ❌"}")
                            delay(200)
                        } else if (block.title.contains("Wait", ignoreCase = true)) {
                            addLog("⏱️ [LOGIC] Sleeping for ${block.waitSeconds} seconds...")
                            delay((block.waitSeconds * 400L).coerceAtLeast(300L))
                        }
                    }
                    BlockCategory.MESSAGE -> {
                        if (!isInsideContainer || conditionPassed) {
                            delay(200)
                            val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                            if (block.title.contains("embed", ignoreCase = true)) {
                                addLog("📋 [MESSAGE] Sent embed card: \"${block.embedTitle}\" in ${block.targetChannel}")
                                val botMsg = SimulatedDiscordMessage(
                                    authorName = "VisualBot",
                                    isBot = true,
                                    avatarColorHex = "#5865F2",
                                    timestamp = "Today at $timeNow",
                                    embedTitle = block.embedTitle,
                                    embedDescription = block.embedDescription,
                                    embedColorHex = block.embedColorHex
                                )
                                _simulatedMessages.value = _simulatedMessages.value + botMsg
                            } else if (block.title.contains("reaction", ignoreCase = true)) {
                                addLog("✨ [MESSAGE] Added reaction ${block.reactionEmoji} to message")
                                val current = _simulatedMessages.value
                                if (current.isNotEmpty()) {
                                    val last = current.last()
                                    val updatedLast = last.copy(reactions = last.reactions + "${block.reactionEmoji} 1")
                                    _simulatedMessages.value = current.dropLast(1) + updatedLast
                                }
                            } else {
                                val replyContent = block.messageContent
                                addLog("💬 [MESSAGE] Sent: \"$replyContent\" to ${block.targetChannel}")
                                val botMsg = SimulatedDiscordMessage(
                                    authorName = "VisualBot",
                                    isBot = true,
                                    avatarColorHex = "#5865F2",
                                    content = replyContent,
                                    timestamp = "Today at $timeNow"
                                )
                                _simulatedMessages.value = _simulatedMessages.value + botMsg
                            }
                        }
                    }
                    BlockCategory.ACTION -> {
                        if (!isInsideContainer || conditionPassed) {
                            delay(150)
                            addLog("⚙️ [ACTION] Executed ${block.title}: ${block.displaySubtitle}")
                        }
                    }
                    BlockCategory.VARIABLE -> {
                        if (!isInsideContainer || conditionPassed) {
                            delay(100)
                            addLog("📦 [VARIABLE] ${block.displaySubtitle}")
                        }
                    }
                    BlockCategory.DISCORD_OBJECT -> {
                        if (!isInsideContainer || conditionPassed) {
                            delay(100)
                            addLog("👑 [DISCORD] Read ${block.discordObjectType}.${block.objectProperty}: ${block.displaySubtitle}")
                        }
                    }
                    BlockCategory.NETWORK -> {
                        if (!isInsideContainer || conditionPassed) {
                            delay(200)
                            addLog("🌐 [NETWORK] ${block.httpMethod} ${block.httpUrl} -> Status: 200 OK")
                        }
                    }
                    BlockCategory.DESTRUCTIVE -> {
                        if (!isInsideContainer || conditionPassed) {
                            delay(150)
                            addLog("⚠️ [DESTRUCTIVE] ${block.displaySubtitle}")
                        }
                    }
                }
            }

            delay(300)
            _activeExecutingBlockId.value = null
            addLog("✅ [COMPLETE] Script execution cycle completed.")
            _isSimulationRunning.value = false
        }
    }

    private fun addLog(entry: String) {
        _simulationLogs.value = _simulationLogs.value + entry
    }

    /**
     * Generates complete, modern Python discord.py script.
     */
    fun exportToDiscordPy(): String {
        val sb = StringBuilder()
        sb.appendLine("#!/usr/bin/env python3")
        sb.appendLine("\"\"\"")
        sb.appendLine(" Discord Bot generated with Visual Builder")
        sb.appendLine(" Target: discord.py >= 2.3.0 | Python 3.10+")
        sb.appendLine("\"\"\"")
        sb.appendLine("import discord")
        sb.appendLine("from discord.ext import commands, tasks")
        sb.appendLine("import asyncio")
        sb.appendLine("import aiohttp")
        sb.appendLine("import json")
        sb.appendLine()
        sb.appendLine("intents = discord.Intents.default()")
        sb.appendLine("intents.message_content = True")
        sb.appendLine("intents.members = True")
        sb.appendLine()
        sb.appendLine("bot = commands.Bot(command_prefix=\"!\", intents=intents)")
        sb.appendLine()

        val eventBlocks = _blocks.value.filter { it.category == BlockCategory.EVENT }
        if (eventBlocks.isEmpty()) {
            sb.appendLine("@bot.event")
            sb.appendLine("async def on_ready():")
            sb.appendLine("    print(f'Logged in as {bot.user} (ID: {bot.user.id})')")
            sb.appendLine()
        }

        var inIf = false
        var ifIndent = "    "

        for (block in _blocks.value) {
            when (block.category) {
                BlockCategory.EVENT -> {
                    if (inIf) { inIf = false; ifIndent = "    " }
                    if (block.title.contains("slash", ignoreCase = true)) {
                        sb.appendLine("@bot.tree.command(name=\"${block.slashCommandName}\", description=\"${block.slashCommandDesc}\")")
                        sb.appendLine("async def ${block.slashCommandName}_cmd(interaction: discord.Interaction):")
                        sb.appendLine("    # The #1 Fix: Immediate deferReply to eliminate 3s timeouts")
                        sb.appendLine("    await interaction.response.defer()")
                        ifIndent = "    "
                    } else if (block.title.contains("button", ignoreCase = true)) {
                        sb.appendLine("@bot.event")
                        sb.appendLine("async def on_interaction(interaction: discord.Interaction):")
                        sb.appendLine("    if interaction.type == discord.InteractionType.component and interaction.data.get('custom_id') == \"${block.buttonCustomId}\":")
                        sb.appendLine("        await interaction.response.defer()")
                        ifIndent = "        "
                    } else if (block.title.contains("joins", ignoreCase = true)) {
                        sb.appendLine("@bot.event")
                        sb.appendLine("async def on_member_join(member: discord.Member):")
                        ifIndent = "    "
                    } else if (block.title.contains("schedule", ignoreCase = true)) {
                        sb.appendLine("@tasks.loop(hours=1)")
                        sb.appendLine("async def scheduled_cron_task():")
                        ifIndent = "    "
                    } else {
                        sb.appendLine("@bot.event")
                        sb.appendLine("async def on_message(message: discord.Message):")
                        sb.appendLine("    if message.author.bot:")
                        sb.appendLine("        return")
                        ifIndent = "    "
                    }
                }
                BlockCategory.LOGIC -> {
                    if (block.title.contains("If", ignoreCase = true)) {
                        val field = if (block.conditionField == "message.content") "message.content.lower()" else block.conditionField
                        val op = if (block.conditionOperator == "contains") "in" else block.conditionOperator
                        val valStr = "\"${block.conditionValue.lowercase()}\""
                        val expr = if (op == "in") "$valStr in $field" else "$field $op $valStr"
                        sb.appendLine("${ifIndent}if $expr:")
                        inIf = true
                        ifIndent = "        "
                    } else if (block.title.contains("Wait", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await asyncio.sleep(${block.waitSeconds})")
                    } else if (block.title.contains("Repeat", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}for _ in range(${block.repeatCount}):")
                        ifIndent = "        "
                    }
                }
                BlockCategory.MESSAGE -> {
                    if (block.title.contains("embed", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}embed = discord.Embed(title=\"${block.embedTitle}\", description=\"${block.embedDescription}\", color=0x3D7EFF)")
                        sb.appendLine("${ifIndent}await message.channel.send(embed=embed)")
                    } else if (block.title.contains("button", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}view = discord.ui.View()")
                        sb.appendLine("${ifIndent}view.add_item(discord.ui.Button(label=\"${block.buttonLabel}\", custom_id=\"${block.buttonCustomId}\", style=discord.ButtonStyle.primary))")
                        sb.appendLine("${ifIndent}await message.channel.send(\"Choose an action:\", view=view)")
                    } else if (block.title.contains("typing", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.channel.typing()")
                    } else if (block.title.contains("Reply", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.reply(\"${block.messageContent}\")")
                    } else if (block.title.contains("reaction", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.add_reaction(\"${block.reactionEmoji}\")")
                    } else if (block.title.contains("DM", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.author.send(\"${block.messageContent}\")")
                    } else {
                        sb.appendLine("${ifIndent}channel = discord.utils.get(message.guild.text_channels, name=\"${block.targetChannel.removePrefix("#")}\") or message.channel")
                        sb.appendLine("${ifIndent}await channel.send(\"${block.messageContent}\")")
                    }
                }
                BlockCategory.ACTION -> {
                    if (block.title.contains("role", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}role = discord.utils.get(message.guild.roles, name=\"${block.roleName}\")")
                        sb.appendLine("${ifIndent}if role: await message.author.add_roles(role)")
                    } else if (block.title.contains("Timeout", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}import datetime")
                        sb.appendLine("${ifIndent}await message.author.timeout(datetime.timedelta(minutes=${block.timeoutMinutes}), reason=\"${block.actionReason}\")")
                    } else if (block.title.contains("Kick", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.author.kick(reason=\"${block.actionReason}\")")
                    } else if (block.title.contains("Create channel", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.guild.create_text_channel(\"${block.newChannelName}\")")
                    }
                }
                BlockCategory.VARIABLE -> {
                    sb.appendLine("${ifIndent}${block.variableName} = ${block.variableValue}")
                }
                BlockCategory.DISCORD_OBJECT -> {
                    sb.appendLine("${ifIndent}# Read Discord Object Property: ${block.discordObjectType}.${block.objectProperty}")
                    sb.appendLine("${ifIndent}obj_value = getattr(message.author, '${block.objectProperty}', str(message.author))")
                }
                BlockCategory.NETWORK -> {
                    if (block.title.contains("HTTP", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}async with aiohttp.ClientSession() as session:")
                        sb.appendLine("${ifIndent}    async with session.get(\"${block.httpUrl}\") as resp:")
                        sb.appendLine("${ifIndent}        api_data = await resp.json() if resp.content_type == 'application/json' else await resp.text()")
                    } else if (block.title.contains("Webhook", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}async with aiohttp.ClientSession() as session:")
                        sb.appendLine("${ifIndent}    webhook = discord.Webhook.from_url(\"${block.webhookUrl}\", session=session)")
                        sb.appendLine("${ifIndent}    await webhook.send(\"${block.messageContent}\")")
                    }
                }
                BlockCategory.DESTRUCTIVE -> {
                    if (block.title.contains("Purge", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.channel.purge(limit=${block.purgeCount})")
                    }
                }
            }
        }

        sb.appendLine()
        sb.appendLine("if __name__ == '__main__':")
        sb.appendLine("    # Replace with your bot token from AI Studio Secrets or BotConfig")
        sb.appendLine("    bot.run(\"DISCORD_BOT_TOKEN\")")
        return sb.toString()
    }

    /**
     * Generates complete, modern JavaScript (discord.js v14) code.
     */
    fun exportToDiscordJs(): String {
        val sb = StringBuilder()
        sb.appendLine("/**")
        sb.appendLine(" * Discord Bot generated with Visual Builder")
        sb.appendLine(" * Target: discord.js v14 | Node.js 18+")
        sb.appendLine(" */")
        sb.appendLine("const { Client, GatewayIntentBits, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');")
        sb.appendLine()
        sb.appendLine("const client = new Client({")
        sb.appendLine("  intents: [")
        sb.appendLine("    GatewayIntentBits.Guilds,")
        sb.appendLine("    GatewayIntentBits.GuildMessages,")
        sb.appendLine("    GatewayIntentBits.MessageContent,")
        sb.appendLine("    GatewayIntentBits.GuildMembers")
        sb.appendLine("  ]")
        sb.appendLine("});")
        sb.appendLine()
        sb.appendLine("client.once('ready', () => {")
        sb.appendLine("  console.log(`Ready! Logged in as \${client.user.tag}`);")
        sb.appendLine("});")
        sb.appendLine()

        var ifIndent = "  "
        var inIf = false

        for (block in _blocks.value) {
            when (block.category) {
                BlockCategory.EVENT -> {
                    if (inIf) { sb.appendLine("  }"); inIf = false }
                    if (block.title.contains("slash", ignoreCase = true)) {
                        sb.appendLine("client.on('interactionCreate', async (interaction) => {")
                        sb.appendLine("  if (!interaction.isChatInputCommand()) return;")
                        sb.appendLine("  if (interaction.commandName === '${block.slashCommandName}') {")
                        sb.appendLine("    // The #1 Fix: Call deferReply immediately before heavy work")
                        sb.appendLine("    await interaction.deferReply();")
                        ifIndent = "    "
                        inIf = true
                    } else if (block.title.contains("button", ignoreCase = true)) {
                        sb.appendLine("client.on('interactionCreate', async (interaction) => {")
                        sb.appendLine("  if (interaction.isButton() && interaction.customId === '${block.buttonCustomId}') {")
                        sb.appendLine("    await interaction.deferUpdate();")
                        ifIndent = "    "
                        inIf = true
                    } else if (block.title.contains("joins", ignoreCase = true)) {
                        sb.appendLine("client.on('guildMemberAdd', async (member) => {")
                        ifIndent = "  "
                    } else {
                        sb.appendLine("client.on('messageCreate', async (message) => {")
                        sb.appendLine("  if (message.author.bot) return;")
                        ifIndent = "  "
                    }
                }
                BlockCategory.LOGIC -> {
                    if (block.title.contains("If", ignoreCase = true)) {
                        val op = if (block.conditionOperator == "==") "===" else block.conditionOperator
                        val cond = if (block.conditionOperator == "contains") {
                            "message.content.includes(\"${block.conditionValue}\")"
                        } else {
                            "message.content $op \"${block.conditionValue}\""
                        }
                        sb.appendLine("${ifIndent}if ($cond) {")
                        inIf = true
                        ifIndent = "    "
                    } else if (block.title.contains("Wait", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await new Promise(r => setTimeout(r, ${block.waitSeconds * 1000}));")
                    } else if (block.title.contains("Repeat", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}for (let i = 0; i < ${block.repeatCount}; i++) {")
                        ifIndent = "    "
                    }
                }
                BlockCategory.MESSAGE -> {
                    if (block.title.contains("embed", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}const embed = new EmbedBuilder()")
                        sb.appendLine("${ifIndent}  .setTitle(\"${block.embedTitle}\")")
                        sb.appendLine("${ifIndent}  .setDescription(\"${block.embedDescription}\")")
                        sb.appendLine("${ifIndent}  .setColor(0x3D7EFF);")
                        sb.appendLine("${ifIndent}await message.channel.send({ embeds: [embed] });")
                    } else if (block.title.contains("button", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}const row = new ActionRowBuilder().addComponents(")
                        sb.appendLine("${ifIndent}  new ButtonBuilder().setCustomId('${block.buttonCustomId}').setLabel('${block.buttonLabel}').setStyle(ButtonStyle.Primary)")
                        sb.appendLine("${ifIndent});")
                        sb.appendLine("${ifIndent}await message.channel.send({ content: 'Action menu:', components: [row] });")
                    } else if (block.title.contains("typing", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.channel.sendTyping();")
                    } else if (block.title.contains("Reply", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.reply(\"${block.messageContent}\");")
                    } else if (block.title.contains("reaction", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.react(\"${block.reactionEmoji}\");")
                    } else {
                        sb.appendLine("${ifIndent}await message.channel.send(\"${block.messageContent}\");")
                    }
                }
                BlockCategory.ACTION -> {
                    if (block.title.contains("role", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}const role = message.guild.roles.cache.find(r => r.name === \"${block.roleName}\");")
                        sb.appendLine("${ifIndent}if (role) await message.member.roles.add(role);")
                    } else if (block.title.contains("Timeout", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.member.timeout(${block.timeoutMinutes} * 60 * 1000, \"${block.actionReason}\");")
                    } else if (block.title.contains("Create channel", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.guild.channels.create({ name: '${block.newChannelName}' });")
                    }
                }
                BlockCategory.VARIABLE -> {
                    sb.appendLine("${ifIndent}let ${block.variableName} = ${block.variableValue};")
                }
                BlockCategory.DISCORD_OBJECT -> {
                    sb.appendLine("${ifIndent}const propValue = message.author['${block.objectProperty}'] || message.author.tag;")
                }
                BlockCategory.NETWORK -> {
                    if (block.title.contains("HTTP", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}const res = await fetch(\"${block.httpUrl}\");")
                        sb.appendLine("${ifIndent}const apiJson = await res.json();")
                    }
                }
                BlockCategory.DESTRUCTIVE -> {
                    if (block.title.contains("Purge", ignoreCase = true)) {
                        sb.appendLine("${ifIndent}await message.channel.bulkDelete(${block.purgeCount});")
                    }
                }
            }
        }
        if (inIf) {
            sb.appendLine("  }")
        }
        sb.appendLine("});")
        sb.appendLine()
        sb.appendLine("client.login(process.env.DISCORD_TOKEN || 'YOUR_BOT_TOKEN');")
        return sb.toString()
    }
}
