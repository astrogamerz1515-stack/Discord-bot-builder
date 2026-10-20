package com.example.ui.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotFile
import com.example.ui.AppTab
import com.example.ui.BotStudioViewModel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import com.example.ui.editor.intellisense.InlineSuggestion
import com.example.ui.editor.intellisense.InlineSuggestionEngine
import com.example.ui.editor.intellisense.CompletionItem
import com.example.ui.editor.intellisense.IntelliSenseBar
import com.example.ui.editor.intellisense.IntelliSenseDocDialog
import com.example.ui.editor.intellisense.IntelliSenseEngine
import com.example.ui.editor.intellisense.IntelliSenseExplorerDialog
import com.example.ui.editor.intellisense.SignatureHelpBar
import com.example.ui.theme.DiscordBackground
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordDarker
import com.example.ui.theme.DiscordElevated
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordHover
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordYellow
import com.example.ui.theme.TerminalBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(viewModel: BotStudioViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val files by viewModel.projectFiles.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val fileContent by viewModel.activeFileContent.collectAsState()
    val isRunning by viewModel.runtimeEngine.isRunning.collectAsState()
    val diagnostics by viewModel.codeDiagnostics.collectAsState()
    val autoSaveEnabled by viewModel.autoSaveEnabled.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val editorFullscreenMode by viewModel.editorFullscreenMode.collectAsState()
    val showBottomNavInEditor by viewModel.showBottomNavInEditor.collectAsState()
    val pm2Processes by com.example.engine.Pm2ProcessManager.processes.collectAsState()

    var showFilesSheet by remember { mutableStateOf(false) }
    var showDiagnosticsPanel by remember { mutableStateOf(false) }
    var showIntelliSenseExplorer by remember { mutableStateOf(false) }
    var selectedDocItem by remember { mutableStateOf<CompletionItem?>(null) }
    var showIntelliSenseBar by remember { mutableStateOf(false) }
    var showSignatureHelp by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showFindReplaceBar by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var inlineDismissedOffset by remember { mutableIntStateOf(-1) }

    // Find & Replace Search State
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var matchIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // Undo / Redo History Stacks
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Launcher for exporting project to ZIP from editor
    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val proj = project
        if (uri != null && proj != null) {
            viewModel.exportProjectToZip(proj, uri, context) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Editor Focus Requester and Software Keyboard Controller
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Pinch-to-zoom editor scaling
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    var showZoomToast by remember { mutableStateOf(false) }
    var zoomToastJob by remember { mutableStateOf<Job?>(null) }
    val editorScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val currentFontSize = (13f * fontScale).sp
    val currentLineHeight = (20f * fontScale).sp
    val currentLineHeightDp = with(density) { currentLineHeight.toDp() }
    val lineHeightPx = with(density) { currentLineHeight.toPx() }

    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Synchronize TextFieldValue with file content and track cursor
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(fileContent, TextRange(fileContent.length)))
    }
    var lastEditorText by remember { mutableStateOf(fileContent) }

    // High-performance allocation-free line count calculation
    val lineCount = remember(textFieldValue.text) {
        val text = textFieldValue.text
        var count = 1
        for (i in 0 until text.length) {
            if (text[i] == '\n') count++
        }
        count
    }

    // Move cursor to specific line, request focus and open keyboard without modifying zoom
    val moveCursorToLine: (Int) -> Unit = { targetLine ->
        val text = textFieldValue.text
        if (text.isEmpty()) {
            textFieldValue = textFieldValue.copy(selection = TextRange(0))
        } else {
            var currentLine = 1
            var targetIndex = 0
            for (i in text.indices) {
                if (currentLine == targetLine) {
                    targetIndex = i
                    break
                }
                if (text[i] == '\n') {
                    currentLine++
                    if (currentLine == targetLine) {
                        targetIndex = i + 1
                        break
                    }
                }
            }
            if (currentLine < targetLine) {
                targetIndex = text.length
            }

            var codeStartIndex = targetIndex
            while (codeStartIndex < text.length && (text[codeStartIndex] == ' ' || text[codeStartIndex] == '\t')) {
                codeStartIndex++
            }
            val finalCursorPos = if (codeStartIndex < text.length && text[codeStartIndex] != '\n') codeStartIndex else targetIndex

            textFieldValue = textFieldValue.copy(selection = TextRange(finalCursorPos.coerceIn(0, text.length)))
        }

        focusRequester.requestFocus()
        keyboardController?.show()
        editorScope.launch {
            delay(40)
            val lineTopPx = ((targetLine - 1) * lineHeightPx).toInt()
            val vViewport = scrollState.viewportSize
            val targetV = (lineTopPx - (vViewport / 3)).coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(targetV, animationSpec = androidx.compose.animation.core.tween(150))
        }
    }

    // Ensure an active file is selected whenever files exist but activeFile is null
    LaunchedEffect(files, activeFile) {
        if (files.isNotEmpty() && (activeFile == null || !files.any { it.id == activeFile?.id })) {
            val entry = files.find { it.isEntrypoint } ?: files.first()
            viewModel.selectFile(entry)
        }
    }

    // Reset textFieldValue whenever active file changes to the new file's content
    LaunchedEffect(activeFile?.id) {
        if (activeFile != null) {
            val currentContent = activeFile?.content ?: viewModel.activeFileContent.value
            textFieldValue = TextFieldValue(currentContent, TextRange(currentContent.length))
            lastEditorText = currentContent
            undoStack.clear()
            redoStack.clear()
            inlineDismissedOffset = -1
        }
    }

    // Only sync if fileContent changed from an external source (not from typing)
    LaunchedEffect(fileContent) {
        if (fileContent != lastEditorText && fileContent != textFieldValue.text) {
            lastEditorText = fileContent
            val safeStart = textFieldValue.selection.start.coerceIn(0, fileContent.length)
            val safeEnd = textFieldValue.selection.end.coerceIn(0, fileContent.length)
            textFieldValue = textFieldValue.copy(text = fileContent, selection = TextRange(safeStart, safeEnd))
        }
    }

    // Launcher for importing an external code file into the current project
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSingleFileToProject(uri, context) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isBinaryFile = remember(activeFile?.filePath, fileContent) {
        val ext = activeFile?.filePath?.substringAfterLast('.', "")?.lowercase() ?: ""
        val binaryExts = setOf("png", "jpg", "jpeg", "gif", "ico", "webp", "mp3", "wav", "zip", "pdf", "exe", "db", "sqlite")
        binaryExts.contains(ext) || fileContent.take(500).any { it == '\u0000' }
    }

    val cursorPosition = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)

    // Language override state allowing user to switch languages for any file
    var customLanguageOverride by remember(activeFile?.id) { mutableStateOf<String?>(null) }
    var showLanguageSelectorDialog by remember { mutableStateOf(false) }

    val effectiveFilePath = remember(activeFile?.filePath, customLanguageOverride) {
        val path = activeFile?.filePath ?: "index.js"
        if (customLanguageOverride != null) {
            val base = if (path.contains('.')) path.substringBeforeLast('.') else path
            "$base.$customLanguageOverride"
        } else {
            path
        }
    }

    val currentFileExtension = remember(effectiveFilePath) {
        effectiveFilePath.substringAfterLast('.', "").lowercase()
    }

    val currentLanguageDisplayName = remember(currentFileExtension) {
        when (currentFileExtension) {
            "js", "mjs", "cjs" -> "JavaScript (v14)"
            "ts", "tsx" -> "TypeScript"
            "py" -> "Python 3.x"
            "json" -> "JSON"
            "kt", "kts" -> "Kotlin"
            "lua" -> "Lua"
            "sql" -> "SQL"
            "sh", "bash" -> "Shell / Bash"
            "html", "htm" -> "HTML5"
            "css" -> "CSS3"
            "md", "markdown" -> "Markdown"
            "env", "properties" -> "ENV / Config"
            "yaml", "yml" -> "YAML"
            "toml" -> "TOML"
            else -> currentFileExtension.uppercase().ifEmpty { "Plain Text" }
        }
    }

    val isPython = currentFileExtension == "py"

    // Real-time Inline Suggestion Calculation (capped at 35k chars for silky smooth 60fps typing)
    val inlineSuggestion = remember(textFieldValue.text, cursorPosition, effectiveFilePath, isBinaryFile, inlineDismissedOffset) {
        if (isBinaryFile || textFieldValue.text.length > 35_000 || cursorPosition == inlineDismissedOffset) null
        else {
            try {
                InlineSuggestionEngine.computeInlineSuggestion(
                    code = textFieldValue.text,
                    cursorIndex = cursorPosition,
                    filePath = effectiveFilePath
                )
            } catch (_: Throwable) {
                null
            }
        }
    }

    // Compute inline ghost text (single line or first line with ↵ indicator)
    val ghostTextToDisplay = remember(inlineSuggestion) {
        val sugg = inlineSuggestion
        if (sugg == null) null
        else {
            if (sugg.completionText.contains('\n')) {
                sugg.completionText.substringBefore('\n') + " ↵"
            } else {
                sugg.completionText
            }
        }
    }

    // Fast and stable syntax highlighting transformation with inline ghost text
    val syntaxTransformation = remember(effectiveFilePath, ghostTextToDisplay, cursorPosition) {
        SyntaxHighlightTransformation(
            filePath = effectiveFilePath,
            ghostText = ghostTextToDisplay,
            ghostOffset = if (ghostTextToDisplay != null) cursorPosition else -1
        )
    }

    // High-performance allocation-free line & column calculation for huge files
    val currentLineAndCol = remember(textFieldValue.text, cursorPosition) {
        val text = textFieldValue.text
        val safePos = cursorPosition.coerceIn(0, text.length)
        var line = 1
        var lastNewlinePos = -1
        for (i in 0 until safePos) {
            if (text[i] == '\n') {
                line++
                lastNewlinePos = i
            }
        }
        val col = safePos - lastNewlinePos
        line to col
    }

    // Compute dynamic IntelliSense completions with safety bounds (capped at 35k chars)
    val completions = remember(textFieldValue.text, cursorPosition, effectiveFilePath, isBinaryFile) {
        if (isBinaryFile || textFieldValue.text.length > 35_000) emptyList()
        else {
            try {
                IntelliSenseEngine.computeCompletions(
                    code = textFieldValue.text,
                    cursorIndex = cursorPosition,
                    filePath = effectiveFilePath
                )
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    // Compute active function signature help with safety bounds
    val signatureHelp = remember(textFieldValue.text, cursorPosition, isBinaryFile) {
        if (isBinaryFile || textFieldValue.text.length > 35_000) null
        else {
            try {
                IntelliSenseEngine.computeSignatureHelp(
                    code = textFieldValue.text,
                    cursorIndex = cursorPosition
                )
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun applyCompletionItem(item: CompletionItem) {
        if (undoStack.size > 50) undoStack.removeAt(0)
        undoStack.add(textFieldValue)
        redoStack.clear()
        val (newCode, newCursor) = IntelliSenseEngine.applyCompletion(
            textFieldValue.text,
            cursorPosition,
            item
        )
        lastEditorText = newCode
        textFieldValue = TextFieldValue(newCode, TextRange(newCursor))
        viewModel.updateActiveFileContent(newCode)
        inlineDismissedOffset = -1
    }

    fun applyInlineSuggestion(suggestion: InlineSuggestion) {
        val insert = suggestion.completionText
        val selStart = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val selEnd = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        val oldText = textFieldValue.text
        if (undoStack.size > 50) undoStack.removeAt(0)
        undoStack.add(textFieldValue)
        redoStack.clear()
        val newText = oldText.substring(0, selStart) + insert + oldText.substring(selEnd)
        val newCursor = selStart + insert.length
        lastEditorText = newText
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateActiveFileContent(newText)
        inlineDismissedOffset = -1
    }

    fun applyInlineWord(suggestion: InlineSuggestion) {
        val word = suggestion.nextWord
        if (word.isEmpty()) return
        val selStart = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val selEnd = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        val oldText = textFieldValue.text
        if (undoStack.size > 50) undoStack.removeAt(0)
        undoStack.add(textFieldValue)
        redoStack.clear()
        val newText = oldText.substring(0, selStart) + word + oldText.substring(selEnd)
        val newCursor = selStart + word.length
        lastEditorText = newText
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateActiveFileContent(newText)
        inlineDismissedOffset = -1
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue)
            lastEditorText = previous.text
            textFieldValue = previous
            viewModel.updateActiveFileContent(previous.text)
            inlineDismissedOffset = -1
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue)
            lastEditorText = next.text
            textFieldValue = next
            viewModel.updateActiveFileContent(next.text)
            inlineDismissedOffset = -1
        }
    }

    fun insertAtCursor(symbol: String) {
        val selStart = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val selEnd = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        val oldText = textFieldValue.text
        if (undoStack.size > 50) undoStack.removeAt(0)
        undoStack.add(textFieldValue)
        redoStack.clear()
        val newText = oldText.substring(0, selStart) + symbol + oldText.substring(selEnd)
        val newCursor = selStart + symbol.length
        lastEditorText = newText
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateActiveFileContent(newText)
        inlineDismissedOffset = -1
    }

    fun moveCursor(delta: Int) {
        val newPos = (textFieldValue.selection.end + delta).coerceIn(0, textFieldValue.text.length)
        textFieldValue = textFieldValue.copy(selection = TextRange(newPos))
        inlineDismissedOffset = -1
    }

    fun pasteFromClipboard(replaceAll: Boolean = false) {
        try {
            val systemClipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clipData = systemClipboard?.primaryClip
            var clipText: String? = null
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                clipText = item.coerceToText(context)?.toString()
            }
            if (clipText.isNullOrEmpty()) {
                clipText = clipboardManager.getText()?.text
            }

            if (clipText.isNullOrEmpty()) {
                Toast.makeText(context, "Clipboard is empty or contains no text", Toast.LENGTH_SHORT).show()
                return
            }

            if (undoStack.size > 50) undoStack.removeAt(0)
            undoStack.add(textFieldValue)
            redoStack.clear()

            val oldText = textFieldValue.text
            val newText: String
            val newCursor: Int

            if (replaceAll || oldText.isEmpty() || textFieldValue.selection.length == oldText.length) {
                newText = clipText
                newCursor = clipText.length
            } else {
                val selStart = textFieldValue.selection.min.coerceIn(0, oldText.length)
                val selEnd = textFieldValue.selection.max.coerceIn(0, oldText.length)
                newText = oldText.substring(0, selStart) + clipText + oldText.substring(selEnd)
                newCursor = (selStart + clipText.length).coerceIn(0, newText.length)
            }

            lastEditorText = newText
            textFieldValue = TextFieldValue(newText, TextRange(newCursor))
            viewModel.updateActiveFileContent(newText)
            inlineDismissedOffset = -1

            val lineCount = clipText.count { it == '\n' } + 1
            Toast.makeText(
                context,
                "📋 Pasted full code: ${clipText.length} chars ($lineCount lines)",
                Toast.LENGTH_SHORT
            ).show()
            focusRequester.requestFocus()
        } catch (e: Exception) {
            Toast.makeText(context, "Paste failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Find & Replace Search Logic
    LaunchedEffect(findQuery, textFieldValue.text) {
        if (findQuery.isNotEmpty() && textFieldValue.text.isNotEmpty()) {
            val list = mutableListOf<Int>()
            var idx = 0
            val code = textFieldValue.text
            while (idx < code.length) {
                val found = code.indexOf(findQuery, idx, ignoreCase = true)
                if (found == -1) break
                list.add(found)
                idx = found + findQuery.length.coerceAtLeast(1)
            }
            matchIndices = list
            if (currentMatchIndex >= list.size) {
                currentMatchIndex = 0
            }
        } else {
            matchIndices = emptyList()
            currentMatchIndex = 0
        }
    }

    fun jumpToMatch(index: Int) {
        if (matchIndices.isNotEmpty() && index in matchIndices.indices) {
            val start = matchIndices[index]
            val end = (start + findQuery.length).coerceAtMost(textFieldValue.text.length)
            textFieldValue = textFieldValue.copy(selection = TextRange(start, end))
            currentMatchIndex = index
        }
    }

    fun findNext() {
        if (matchIndices.isEmpty()) return
        val next = (currentMatchIndex + 1) % matchIndices.size
        jumpToMatch(next)
    }

    fun findPrev() {
        if (matchIndices.isEmpty()) return
        val prev = if (currentMatchIndex - 1 < 0) matchIndices.lastIndex else currentMatchIndex - 1
        jumpToMatch(prev)
    }

    fun replaceOne() {
        if (matchIndices.isEmpty() || findQuery.isEmpty()) return
        val start = matchIndices[currentMatchIndex]
        val end = (start + findQuery.length).coerceAtMost(textFieldValue.text.length)
        if (undoStack.size > 50) undoStack.removeAt(0)
        undoStack.add(textFieldValue)
        redoStack.clear()
        val oldText = textFieldValue.text
        val newText = oldText.substring(0, start) + replaceQuery + oldText.substring(end)
        lastEditorText = newText
        textFieldValue = TextFieldValue(newText, TextRange(start + replaceQuery.length))
        viewModel.updateActiveFileContent(newText)
    }

    fun replaceAll() {
        if (matchIndices.isEmpty() || findQuery.isEmpty()) return
        if (undoStack.size > 50) undoStack.removeAt(0)
        undoStack.add(textFieldValue)
        redoStack.clear()
        val count = matchIndices.size
        val oldText = textFieldValue.text
        val newText = oldText.replace(findQuery, replaceQuery, ignoreCase = true)
        lastEditorText = newText
        textFieldValue = TextFieldValue(newText, TextRange(textFieldValue.selection.end.coerceIn(0, newText.length)))
        viewModel.updateActiveFileContent(newText)
        Toast.makeText(context, "Replaced $count occurrence(s)", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .imePadding()
    ) {
        // Top File Tabs & Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DiscordDarker)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Files Drawer button
            IconButton(
                onClick = { showFilesSheet = true },
                modifier = Modifier
                    .size(34.dp)
                    .background(DiscordElevated, RoundedCornerShape(6.dp))
                    .testTag("btn_open_file_drawer")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Project Files",
                    tint = DiscordTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // File Tabs
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                files.forEach { file ->
                    val isSelected = activeFile?.id == file.id
                    val ext = file.filePath.substringAfterLast('.', "").lowercase()
                    val (badgeText, badgeColor) = when (ext) {
                        "js", "mjs", "cjs" -> "JS" to Color(0xFFF7DF1E)
                        "ts" -> "TS" to Color(0xFF3178C6)
                        "jsx" -> "JSX" to Color(0xFF61DAFB)
                        "tsx" -> "TSX" to Color(0xFF61DAFB)
                        "py" -> "PY" to Color(0xFF3776AB)
                        "json" -> "{}" to Color(0xFF00B0F4)
                        "html", "htm" -> "HTML" to Color(0xFFE44D26)
                        "css", "scss" -> "CSS" to Color(0xFF264DE4)
                        "sql" -> "SQL" to Color(0xFF00758F)
                        "sh", "bash", "zsh" -> "SH" to Color(0xFF4EAA25)
                        "yaml", "yml" -> "YML" to Color(0xFFCB171E)
                        "toml" -> "TOML" to Color(0xFF9C4221)
                        "md" -> "MD" to Color(0xFF0891B2)
                        "kt", "kts" -> "KT" to Color(0xFF7F52FF)
                        "java" -> "JAVA" to Color(0xFFEA2D2E)
                        "rs" -> "RS" to Color(0xFFDEA584)
                        "c", "h" -> "C" to Color(0xFF555555)
                        "cpp", "hpp" -> "C++" to Color(0xFF00599C)
                        "cs" -> "C#" to Color(0xFF178600)
                        "xml", "svg" -> "XML" to Color(0xFFFF6600)
                        "env", "properties" -> "ENV" to DiscordGreen
                        else -> ext.uppercase().take(4).ifEmpty { "TXT" } to DiscordTextMuted
                    }

                    Surface(
                        color = if (isSelected) DiscordBackground else DiscordSurface,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DiscordHover) else null,
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clickable { viewModel.selectFile(file) }
                            .testTag("tab_file_${file.filePath}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Surface(
                                color = badgeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.filePath,
                                color = if (isSelected) DiscordTextPrimary else DiscordTextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // When running: provide Reload button to apply new code instantly without disconnecting Gateway
            if (isRunning) {
                Button(
                    onClick = {
                        viewModel.updateActiveFileContent(textFieldValue.text)
                        viewModel.saveActiveFile()
                        viewModel.hotReload()
                        Toast.makeText(context, "🔥 Applied editor code to active bot!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscordBlurple
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_quick_reload_bot")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Bot Code",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Reload",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Quick Run / Stop button
            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.stopBotProcess()
                    } else {
                        viewModel.updateActiveFileContent(textFieldValue.text)
                        viewModel.saveActiveFile()
                        viewModel.startBotProcess()
                        Toast.makeText(context, "🟢 Bot running with your editor code!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) DiscordRed else DiscordGreen
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("btn_quick_run_bot")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Stop Bot" else "Run Bot",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isRunning) "Stop" else "Run",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Snippets & Helper Keys Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DiscordSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Undo Button
            Surface(
                color = if (undoStack.isNotEmpty()) DiscordElevated else DiscordDarker,
                shape = RoundedCornerShape(4.dp),
                border = if (undoStack.isNotEmpty()) androidx.compose.foundation.BorderStroke(0.5.dp, DiscordHover) else null,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable(enabled = undoStack.isNotEmpty()) { undo() }
                    .testTag("btn_undo")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) DiscordTextPrimary else DiscordTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Redo Button
            Surface(
                color = if (redoStack.isNotEmpty()) DiscordElevated else DiscordDarker,
                shape = RoundedCornerShape(4.dp),
                border = if (redoStack.isNotEmpty()) androidx.compose.foundation.BorderStroke(0.5.dp, DiscordHover) else null,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable(enabled = redoStack.isNotEmpty()) { redo() }
                    .testTag("btn_redo")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) DiscordTextPrimary else DiscordTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Find & Replace Search Toggle Button
            Surface(
                color = if (showFindReplaceBar) DiscordBlurple.copy(alpha = 0.25f) else DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = if (showFindReplaceBar) androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple) else null,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable { showFindReplaceBar = !showFindReplaceBar }
                    .testTag("btn_find_replace")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find & Replace",
                        tint = if (showFindReplaceBar) DiscordBlurple else DiscordTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Find",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Follow Caret / Center Cursor Button
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DiscordBlurple.copy(alpha = 0.6f)),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                        editorScope.launch {
                            val lineTopPx = ((currentLineAndCol.first - 1) * lineHeightPx).toInt()
                            val vViewport = scrollState.viewportSize
                            val targetV = (lineTopPx - (vViewport / 3)).coerceIn(0, scrollState.maxValue)
                            scrollState.animateScrollTo(targetV, animationSpec = androidx.compose.animation.core.tween(150))
                            val hViewport = horizontalScrollState.viewportSize
                            if (hViewport > 0) {
                                val charWidthPx = with(density) { (currentFontSize.value * 0.605f).sp.toPx() }
                                val targetH = (((currentLineAndCol.second - 1) * charWidthPx) - (hViewport / 4)).toInt().coerceIn(0, horizontalScrollState.maxValue)
                                horizontalScrollState.animateScrollTo(targetH, animationSpec = androidx.compose.animation.core.tween(150))
                            }
                        }
                    }
                    .testTag("btn_follow_caret")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Follow Caret",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Follow Caret",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // IntelliSense API Catalog Explorer Button
            Surface(
                color = DiscordBlurple,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { showIntelliSenseExplorer = true }
                    .testTag("btn_open_intellisense_catalog")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "IntelliSense",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "IntelliSense",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Snippets Button
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { viewModel.showSnippetsDialog.value = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Snippets",
                        tint = DiscordTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Snippets",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Clear Code of Selected File Button
            Surface(
                color = DiscordRed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordRed.copy(alpha = 0.6f)),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable { showClearConfirmDialog = true }
                    .testTag("btn_clear_code")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Code",
                        tint = DiscordRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear Code",
                        color = DiscordRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 📋 Paste Full Code from Clipboard
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple.copy(alpha = 0.5f)),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable { pasteFromClipboard() }
                    .testTag("btn_toolbar_paste")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste Code",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Paste",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Prettier Code Formatter Button
            Surface(
                color = DiscordHover,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable {
                        val formatted = formatSourceCode(textFieldValue.text, activeFile?.filePath ?: "")
                        lastEditorText = formatted
                        textFieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                        viewModel.updateActiveFileContent(formatted)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Format Code",
                        tint = DiscordTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Format",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ⚡ Auto-Fix & Mistake Corrector Button
            Surface(
                color = DiscordBlurple.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable {
                        viewModel.autoFixActiveFile { count, fixes ->
                            if (count > 0) {
                                val newContent = viewModel.activeFileContent.value
                                lastEditorText = newContent
                                textFieldValue = TextFieldValue(newContent, TextRange(newContent.length))
                                val summary = fixes.take(2).joinToString("; ")
                                Toast.makeText(context, "⚡ Auto-fixed $count issue(s): $summary", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "✓ Code is clean and active! No errors found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .testTag("btn_autofix_code")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Auto-Fix Code",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto-Fix",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ⌨ Quick Keyboard Toggle Button
            Surface(
                color = DiscordBlurple,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .testTag("btn_show_keyboard")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⌨ Keyboard",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 📋 Quick Paste Button (Preserves full code without truncation)
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable { pasteFromClipboard() }
                    .testTag("btn_quick_paste_code")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Paste",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            // Cursor navigation buttons
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable { moveCursor(-1) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cursor Left",
                        tint = DiscordTextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable { moveCursor(1) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Cursor Right",
                        tint = DiscordTextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Indent Tab button (accepts suggestion if present, else 2 spaces)
            Surface(
                color = if (inlineSuggestion != null) DiscordGreen.copy(alpha = 0.2f) else DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = if (inlineSuggestion != null) androidx.compose.foundation.BorderStroke(1.dp, DiscordGreen) else null,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clickable {
                        val suggestion = inlineSuggestion
                        if (suggestion != null) {
                            applyInlineSuggestion(suggestion)
                        } else {
                            insertAtCursor("  ")
                        }
                    }
                    .testTag("btn_tab_indent")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = if (inlineSuggestion != null) "Tab ⇥" else "Tab",
                        color = if (inlineSuggestion != null) DiscordGreen else DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (inlineSuggestion != null) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }

            // Quick Insertion characters with generous touch area
            listOf("(", ")", "{", "}", "[", "]", "=>", ";", "\"", "'", "`", ":", "=", "+", "$", ".").forEach { symbol ->
                Surface(
                    color = DiscordElevated,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .widthIn(min = 32.dp)
                        .clickable { insertAtCursor(symbol) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = symbol,
                            color = DiscordTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Auto-Save indicator and toggle button
            Surface(
                color = if (autoSaveEnabled) DiscordGreen.copy(alpha = 0.15f) else DiscordHover,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (autoSaveEnabled) DiscordGreen.copy(alpha = 0.4f) else DiscordTextMuted.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .clickable { viewModel.toggleAutoSave(!autoSaveEnabled) }
                    .testTag("btn_toggle_autosave")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (autoSaveEnabled) {
                                    if (saveStatus.startsWith("Saving")) DiscordYellow else DiscordGreen
                                } else DiscordTextMuted,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (autoSaveEnabled) "Auto-save: ON ($saveStatus)" else "Auto-save: OFF",
                        color = if (autoSaveEnabled) DiscordGreen else DiscordTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Save File button
            Surface(
                color = DiscordHover,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { viewModel.saveActiveFile() }
                    .testTag("btn_save_code_file")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        tint = DiscordGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Save",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Export Bot Project Button
            Surface(
                color = DiscordHover,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { showExportDialog = true }
                    .testTag("btn_export_code_file")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Export Project",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Error Inspector Badge / Toggle
            Surface(
                color = if (diagnostics.isNotEmpty()) DiscordRed.copy(alpha = 0.2f) else DiscordHover,
                shape = RoundedCornerShape(4.dp),
                border = if (diagnostics.isNotEmpty()) androidx.compose.foundation.BorderStroke(1.dp, DiscordRed) else null,
                modifier = Modifier.clickable { showDiagnosticsPanel = !showDiagnosticsPanel }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (diagnostics.isNotEmpty()) "⚠️ ${diagnostics.size} ${if (diagnostics.size == 1) "Error" else "Errors"}" else "✓ No Errors",
                        color = if (diagnostics.isNotEmpty()) DiscordRed else DiscordGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Caret Tracker & Go to Line Button
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DiscordBlurple.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable { showGoToLineDialog = true }
                    .testTag("btn_caret_tracker_metric")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Ln ${currentLineAndCol.first}:${currentLineAndCol.second} (Go to)",
                        color = DiscordBlurple,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Auto-Create Referenced Files Button
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DiscordGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable { viewModel.autoCreateFilesFromCode() }
                    .testTag("btn_auto_create_files")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Auto Create Files",
                        tint = DiscordGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto-Files",
                        color = DiscordGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Multi-Language Selector Chip
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DiscordBlurple),
                modifier = Modifier
                    .clickable { showLanguageSelectorDialog = true }
                    .testTag("btn_select_language_mode")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentLanguageDisplayName,
                        color = DiscordBlurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "▾", color = DiscordTextSecondary, fontSize = 10.sp)
                }
            }

            // Fullscreen Editor Toggle Button
            Surface(
                color = if (editorFullscreenMode) DiscordBlurple else DiscordHover,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { viewModel.toggleEditorFullscreen() }
                    .testTag("btn_toggle_fullscreen")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (editorFullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (editorFullscreenMode) "Exit Fullscreen" else "Fullscreen Editor",
                        tint = if (editorFullscreenMode) Color.White else DiscordTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (editorFullscreenMode) "Exit" else "Full",
                        color = if (editorFullscreenMode) Color.White else DiscordTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // PM2 24/7 Service Toggle Button
            val isPm2Online = pm2Processes.any { it.status == com.example.engine.Pm2Status.ONLINE }
            Surface(
                color = if (isPm2Online) DiscordGreen.copy(alpha = 0.2f) else DiscordHover,
                shape = RoundedCornerShape(4.dp),
                border = if (isPm2Online) androidx.compose.foundation.BorderStroke(0.5.dp, DiscordGreen) else null,
                modifier = Modifier
                    .clickable {
                        viewModel.runScriptWithPm2()
                        Toast.makeText(context, "⚡ PM2 24/7 Active! Bot runs continuously even when app is closed.", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("btn_pm2_247_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isPm2Online) DiscordGreen else DiscordTextMuted, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isPm2Online) "PM2 24/7" else "PM2",
                        color = if (isPm2Online) DiscordGreen else DiscordTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


        // Live Signature Help Parameter Hint Bar if typing inside a function call
        if (showSignatureHelp && signatureHelp != null) {
            SignatureHelpBar(
                signatureHelp = signatureHelp,
                onDismiss = { showSignatureHelp = false }
            )
        }

        // Live IntelliSense Suggestion Shelf above the editor
        if (showIntelliSenseBar && completions.isNotEmpty()) {
            IntelliSenseBar(
                suggestions = completions,
                onSelectSuggestion = { applyCompletionItem(it) },
                onShowDoc = { selectedDocItem = it },
                onDismiss = { showIntelliSenseBar = false }
            )
        }

        // Code Editor Body with Line Numbers
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DiscordBackground)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = DiscordTextMuted, modifier = Modifier.size(48.dp))
                    Text("No Files in this Bot Project", color = DiscordTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Create a new file or import existing bot code (.js, .py, .json, etc.) to get started.", color = DiscordTextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { fileImportLauncher.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import File")
                        }
                        Button(
                            onClick = { viewModel.showNewFileDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordGreen),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New File", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (isBinaryFile) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DiscordBackground)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Binary File Detected", color = DiscordYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("File '${activeFile?.filePath}' contains binary data and cannot be displayed in text editor.", color = DiscordTextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            val displayLineCount = minOf(lineCount, 5000)
            val lineNumbersString = remember(displayLineCount, lineCount) {
                val sb = StringBuilder(displayLineCount * 6)
                for (i in 1..displayLineCount) {
                    if (i > 1) sb.append('\n')
                    sb.append(i)
                }
                if (lineCount > displayLineCount) {
                    sb.append("\n...")
                }
                sb.toString()
            }

            // Caret Tracker & Bidirectional Auto-Scroll: Keeps caret and active line smoothly in view
            val activeLine = currentLineAndCol.first
            val activeCol = currentLineAndCol.second

            // Accurate visual column taking into account tab indent width
            val visualCol = remember(textFieldValue.text, cursorPosition) {
                val text = textFieldValue.text
                val safePos = cursorPosition.coerceIn(0, text.length)
                val lineStart = text.lastIndexOf('\n', (safePos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                var col = 1
                for (i in lineStart until safePos) {
                    if (text[i] == '\t') col += 2
                    else col++
                }
                col
            }

            val charWidthPx = with(density) { (currentFontSize.value * 0.605f).sp.toPx() }

            var lastTrackedLine by remember { mutableIntStateOf(-1) }
            var lastTrackedCol by remember { mutableIntStateOf(-1) }

            val scrollToCaret: suspend (animate: Boolean) -> Unit = { animate ->
                if (activeLine > 0 && lineHeightPx > 0f) {
                    val lineTopPx = ((activeLine - 1) * lineHeightPx).toInt()
                    val lineBottomPx = lineTopPx + lineHeightPx.toInt()
                    val vViewport = scrollState.viewportSize
                    if (vViewport > 0) {
                        val currentVScroll = scrollState.value
                        // Keep a generous comfort band around the caret so user always sees surrounding lines
                        val topMargin = (vViewport * 0.20f).toInt().coerceIn(lineHeightPx.toInt(), (lineHeightPx * 4).toInt())
                        val bottomMargin = (vViewport * 0.35f).toInt().coerceIn((lineHeightPx * 2).toInt(), (lineHeightPx * 6).toInt())

                        val targetVScroll = when {
                            lineTopPx < currentVScroll + topMargin -> {
                                (lineTopPx - topMargin).coerceAtLeast(0)
                            }
                            lineBottomPx > currentVScroll + vViewport - bottomMargin -> {
                                (lineBottomPx - vViewport + bottomMargin).coerceAtLeast(0)
                            }
                            else -> null
                        }

                        if (targetVScroll != null) {
                            val clampedTarget = targetVScroll.coerceIn(0, scrollState.maxValue)
                            if (animate) {
                                scrollState.animateScrollTo(clampedTarget, animationSpec = androidx.compose.animation.core.tween(130))
                            } else {
                                scrollState.scrollTo(clampedTarget)
                            }
                        }
                    }

                    // Horizontal caret tracking
                    val hViewport = horizontalScrollState.viewportSize
                    if (hViewport > 0 && charWidthPx > 0f) {
                        val caretXPx = ((visualCol - 1) * charWidthPx).toInt()
                        val currentHScroll = horizontalScrollState.value
                        val leftMargin = with(density) { 36.dp.toPx() }.toInt()
                        val rightMargin = with(density) { 80.dp.toPx() }.toInt()

                        val targetHScroll = when {
                            caretXPx < currentHScroll + leftMargin -> {
                                (caretXPx - leftMargin).coerceAtLeast(0)
                            }
                            caretXPx > currentHScroll + hViewport - rightMargin -> {
                                (caretXPx - hViewport + rightMargin).coerceAtLeast(0)
                            }
                            else -> null
                        }

                        if (targetHScroll != null) {
                            val clampedH = targetHScroll.coerceIn(0, horizontalScrollState.maxValue)
                            if (animate) {
                                horizontalScrollState.animateScrollTo(clampedH, animationSpec = androidx.compose.animation.core.tween(130))
                            } else {
                                horizontalScrollState.scrollTo(clampedH)
                            }
                        }
                    }
                }
            }

            LaunchedEffect(cursorPosition, activeLine, visualCol, fontScale, scrollState.viewportSize, horizontalScrollState.viewportSize) {
                val lineDiff = kotlin.math.abs(activeLine - lastTrackedLine)
                val colDiff = kotlin.math.abs(visualCol - lastTrackedCol)
                val isTyping = (lineDiff <= 1 && colDiff <= 2)

                lastTrackedLine = activeLine
                lastTrackedCol = visualCol

                // When typing, use instant non-blocking scrollTo so it never stutters or cancels mid-animation!
                // For big cursor jumps (e.g. clicking line, Go to line, find & replace), use quick animation.
                scrollToCaret(!isTyping && lineDiff > 3)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DiscordBackground)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                // Pinch-to-zoom: ONLY consume when 2 or more fingers are down!
                                // Single-finger touches pass through cleanly for typing, cursor, and scrolling.
                                if (event.changes.size >= 2) {
                                    val zoom = event.calculateZoom()
                                    if (zoom != 1.0f) {
                                        val oldScale = fontScale
                                        val newScale = (fontScale * zoom).coerceIn(0.65f, 2.5f)
                                        if (kotlin.math.abs(newScale - oldScale) > 0.003f) {
                                            fontScale = newScale
                                            showZoomToast = true
                                            zoomToastJob?.cancel()
                                            zoomToastJob = editorScope.launch {
                                                delay(1400)
                                                showZoomToast = false
                                            }
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .defaultMinSize(minHeight = 800.dp)
                        .background(DiscordBackground)
                        .verticalScroll(scrollState)
                        .padding(bottom = 32.dp)
                ) {
                    // Line numbers column with active line highlight and error indicators
                    val errorLines = remember(diagnostics) { diagnostics.map { it.lineNumber }.toSet() }
                    val lineDigits = remember(lineCount) { lineCount.toString().length }
                    val gutterWidth = remember(lineDigits, fontScale) {
                        (maxOf(36f, lineDigits * 9f + 20f) * fontScale).dp
                    }

                    Box(
                        modifier = Modifier
                            .width(gutterWidth)
                            .defaultMinSize(minHeight = 800.dp)
                            .background(DiscordBackground)
                            .border(
                                width = 0.5.dp,
                                color = DiscordHover,
                                shape = RoundedCornerShape(0.dp)
                            )
                            .pointerInput(lineCount, lineHeightPx) {
                                detectTapGestures { offset ->
                                    val tappedLine = ((offset.y / lineHeightPx).toInt() + 1).coerceIn(1, lineCount)
                                    moveCursorToLine(tappedLine)
                                }
                            }
                            .padding(top = 4.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        // Gutter Caret Tracker Indicator (Active Line Highlight in Gutter)
                        if (activeLine in 1..lineCount) {
                            val gutterLineTop = with(density) { ((activeLine - 1) * lineHeightPx).toDp() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = gutterLineTop)
                                    .height(currentLineHeightDp)
                                    .background(DiscordBlurple.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(3.dp)
                                        .height(currentLineHeightDp)
                                        .background(DiscordBlurple, RoundedCornerShape(1.dp))
                                )
                            }
                        }

                        if (lineCount <= 80) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                for (i in 1..lineCount) {
                                    val hasError = errorLines.contains(i)
                                    val isCurrentLine = i == currentLineAndCol.first

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                moveCursorToLine(i)
                                            }
                                    ) {
                                        if (hasError) {
                                            Text(
                                                text = "●",
                                                color = DiscordRed,
                                                fontSize = (9f * fontScale).sp,
                                                modifier = Modifier.padding(end = 2.dp)
                                            )
                                        } else if (isCurrentLine) {
                                            Text(
                                                text = "›",
                                                color = DiscordBlurple,
                                                fontSize = (11f * fontScale).sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = "$i",
                                            color = if (hasError) DiscordRed
                                            else if (isCurrentLine) DiscordBlurple
                                            else DiscordTextMuted,
                                            fontSize = currentFontSize,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (hasError || isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = currentLineHeight,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim = LineHeightStyle.Trim.None
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // High-performance single-Text rendering for large files (prevents Compose slot table OOM)
                            Text(
                                text = lineNumbersString,
                                color = DiscordTextMuted,
                                fontSize = currentFontSize,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = currentLineHeight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.None
                                    )
                                )
                            )
                        }
                    }

                    // Editor content text field with direct touch access, long-press paste, and line auto-focusing
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 800.dp)
                            .horizontalScroll(horizontalScrollState)
                            .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                    ) {
                        // Editor Caret Tracker: Active Line Background Highlight
                        if (activeLine in 1..lineCount) {
                            val activeLineTop = with(density) { ((activeLine - 1) * lineHeightPx).toDp() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = activeLineTop)
                                    .height(currentLineHeightDp)
                                    .background(
                                        color = DiscordBlurple.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = DiscordBlurple.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                lastEditorText = newValue.text
                                textFieldValue = newValue
                                viewModel.updateActiveFileContent(newValue.text)
                            },
                            visualTransformation = syntaxTransformation,
                            textStyle = TextStyle(
                                color = DiscordTextPrimary,
                                fontSize = currentFontSize,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = currentLineHeight,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None
                                )
                            ),
                            cursorBrush = SolidColor(DiscordBlurple),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.None
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 800.dp)
                                .widthIn(min = 600.dp)
                                .focusRequester(focusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                                        val suggestion = inlineSuggestion
                                        if (suggestion != null) {
                                            applyInlineSuggestion(suggestion)
                                            true
                                        } else {
                                            insertAtCursor("  ")
                                            true
                                        }
                                    } else {
                                        false
                                    }
                                }
                                .testTag("code_editor_input")
                        )
                    }
                }

                // Floating Zoom Feedback Pill
                if (showZoomToast) {
                    Surface(
                        color = DiscordElevated.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔍 Zoom: ${(fontScale * 100).toInt()}%",
                                color = DiscordTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }



        // Error Diagnostics Tray at bottom of editor (only shown when explicitly opened by user)
        if (showDiagnosticsPanel) {
            Surface(
                color = DiscordSurface,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (diagnostics.isNotEmpty()) DiscordRed.copy(alpha = 0.5f) else DiscordHover),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTICS & ERRORS (${diagnostics.size})",
                            color = if (diagnostics.isNotEmpty()) DiscordRed else DiscordGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showDiagnosticsPanel = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Text("✕", color = DiscordTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (diagnostics.isEmpty()) {
                        Text("No syntax or runtime issues detected in active file.", color = DiscordTextMuted, fontSize = 12.sp)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            diagnostics.forEach { diag ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        color = when (diag.severity) {
                                            DiagnosticSeverity.ERROR -> DiscordRed.copy(alpha = 0.2f)
                                            DiagnosticSeverity.WARNING -> DiscordYellow.copy(alpha = 0.2f)
                                            DiagnosticSeverity.INFO -> DiscordBlurple.copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Line ${diag.lineNumber}:${diag.columnNumber}",
                                            color = when (diag.severity) {
                                                DiagnosticSeverity.ERROR -> DiscordRed
                                                DiagnosticSeverity.WARNING -> DiscordYellow
                                                DiagnosticSeverity.INFO -> DiscordBlurple
                                            },
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = diag.message,
                                            color = DiscordTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (diag.sourceLine.isNotBlank()) {
                                            Text(
                                                text = "> ${diag.sourceLine}",
                                                color = DiscordTextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full IntelliSense and API Explorer Dialog
    if (showIntelliSenseExplorer) {
        IntelliSenseExplorerDialog(
            isPython = isPython,
            onDismiss = { showIntelliSenseExplorer = false },
            onInsert = { item ->
                applyCompletionItem(item)
            }
        )
    }

    // Single item documentation dialog
    selectedDocItem?.let { item ->
        IntelliSenseDocDialog(
            item = item,
            onDismiss = { selectedDocItem = null },
            onInsert = { itemToInsert ->
                applyCompletionItem(itemToInsert)
            }
        )
    }

    // Language Mode Selection Dialog
    if (showLanguageSelectorDialog) {
        val supportedLanguages = listOf(
            "js" to "JavaScript (Node.js / Discord.js)",
            "ts" to "TypeScript",
            "py" to "Python (discord.py)",
            "json" to "JSON",
            "kt" to "Kotlin",
            "lua" to "Lua (Discordia)",
            "sh" to "Shell / Bash",
            "sql" to "SQL (SQLite, PostgreSQL)",
            "html" to "HTML5",
            "css" to "CSS3",
            "md" to "Markdown",
            "env" to "Environment / Config",
            "yaml" to "YAML",
            "toml" to "TOML"
        )

        AlertDialog(
            onDismissRequest = { showLanguageSelectorDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text(
                    text = "Select Language Mode",
                    color = DiscordTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Change syntax highlighting, IntelliSense completions, and inline code suggestions for '${activeFile?.filePath ?: "current file"}':",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    supportedLanguages.forEach { (ext, displayName) ->
                        val isSelected = currentFileExtension == ext
                        Surface(
                            color = if (isSelected) DiscordBlurple.copy(alpha = 0.2f) else DiscordElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    customLanguageOverride = ext
                                    showLanguageSelectorDialog = false
                                    Toast.makeText(context, "Language set to $displayName", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 2.dp)
                                .testTag("lang_option_$ext")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (ext) {
                                            "js" -> "📜 "
                                            "ts" -> "🔷 "
                                            "py" -> "🐍 "
                                            "json" -> "⚙️ "
                                            "kt" -> "☕ "
                                            "lua" -> "🌙 "
                                            "sh" -> "🐚 "
                                            "sql" -> "🗄️ "
                                            "html" -> "🌐 "
                                            "css" -> "🎨 "
                                            "md" -> "📝 "
                                            "env" -> "🔑 "
                                            else -> "📄 "
                                        },
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = displayName,
                                        color = if (isSelected) DiscordBlurple else DiscordTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = DiscordGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageSelectorDialog = false }) {
                    Text("Close", color = DiscordBlurple)
                }
            }
        )
    }

    // Confirmation Dialog to Clear Code of Selected File
    if (showClearConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = DiscordSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = DiscordRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear File Code",
                        color = DiscordTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all code in \"${activeFile?.filePath ?: "the active file"}\"? This will erase all text in this file and cannot be undone.",
                    color = DiscordTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        lastEditorText = ""
                        textFieldValue = TextFieldValue("", TextRange(0))
                        viewModel.updateActiveFileContent("")
                        viewModel.saveActiveFile()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("btn_confirm_clear_code")
                ) {
                    Text("Clear All Code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Project Files Bottom Sheet
    if (showFilesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilesSheet = false },
            containerColor = DiscordSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project Files",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showFilesSheet = false
                                fileImportLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordElevated),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("btn_import_code_file")
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = "Import File", tint = DiscordTextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import File", color = DiscordTextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                showFilesSheet = false
                                viewModel.showNewFileDialog.value = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("btn_new_code_file")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add File", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New File", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                files.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectFile(file)
                                showFilesSheet = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (file.filePath.endsWith(".js") || file.filePath.endsWith(".ts")) "📜 "
                                else if (file.filePath.endsWith(".py")) "🐍 "
                                else if (file.filePath.endsWith(".json")) "⚙️ "
                                else if (file.filePath.endsWith(".env")) "🔑 "
                                else "📄 ",
                                fontSize = 14.sp
                            )
                            Text(
                                text = file.filePath,
                                color = if (activeFile?.id == file.id) DiscordBlurple else DiscordTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = if (activeFile?.id == file.id) FontWeight.Bold else FontWeight.Normal
                            )
                            if (file.isEntrypoint) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = DiscordGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "entry",
                                        color = DiscordGreen,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    viewModel.clearFileContent(file.id)
                                    if (activeFile?.id == file.id) {
                                        textFieldValue = TextFieldValue("", TextRange(0))
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Code",
                                    tint = DiscordYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (!file.isEntrypoint) {
                                IconButton(
                                    onClick = { viewModel.deleteFile(file) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = DiscordTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Go To Line (Caret Tracker Jump) Dialog
    if (showGoToLineDialog) {
        var inputLine by remember { mutableStateOf("${currentLineAndCol.first}") }
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text(
                    text = "🎯 Caret Tracker • Go to Line",
                    color = DiscordTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current Caret: Line ${currentLineAndCol.first}, Column ${currentLineAndCol.second} (Total: $lineCount lines)",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = inputLine,
                        onValueChange = { inputLine = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Line Number (1 - $lineCount)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            focusedLabelColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goto_line")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = inputLine.toIntOrNull()
                        if (target != null && target in 1..lineCount) {
                            moveCursorToLine(target)
                            showGoToLineDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a valid line between 1 and $lineCount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Go to Line", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }
}

fun formatSourceCode(rawCode: String, filePath: String = ""): String {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    if (ext == "json") {
        return try {
            org.json.JSONObject(rawCode).toString(2)
        } catch (_: Exception) {
            try {
                org.json.JSONArray(rawCode).toString(2)
            } catch (_: Exception) {
                rawCode
            }
        }
    }
    if (ext == "py") {
        // In Python, do not manipulate indentation based on braces; trim trailing whitespace and preserve block structure
        return rawCode.lines().joinToString("\n") { it.trimEnd() }.trimEnd()
    }

    val lines = rawCode.lines()
    val formatted = StringBuilder()
    var indentLevel = 0
    val indent = "  "

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            formatted.append("\n")
            continue
        }

        // Check if line closes block
        if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]") ||
            trimmed.startsWith("};") || trimmed.startsWith("});") || trimmed.startsWith("]);")
        ) {
            indentLevel = (indentLevel - 1).coerceAtLeast(0)
        }

        formatted.append(indent.repeat(indentLevel)).append(trimmed).append("\n")

        // Check if line opens block
        val opens = trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[")
        val closes = trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")
        if (opens && !closes) {
            indentLevel++
        }
    }
    return formatted.toString().trimEnd()
}
