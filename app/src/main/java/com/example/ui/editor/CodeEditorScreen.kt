package com.example.ui.editor

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.OutlinedTextField
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

    val syntaxTransformation = remember(activeFile?.filePath) {
        SyntaxHighlightTransformation(activeFile?.filePath ?: "index.js")
    }

    var showFilesSheet by remember { mutableStateOf(false) }
    var showDiagnosticsPanel by remember { mutableStateOf(false) }
    var showIntelliSenseExplorer by remember { mutableStateOf(false) }
    var selectedDocItem by remember { mutableStateOf<CompletionItem?>(null) }
    var showIntelliSenseBar by remember { mutableStateOf(true) }
    var showSignatureHelp by remember { mutableStateOf(true) }

    // Synchronize TextFieldValue with file content and track cursor
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(fileContent, TextRange(fileContent.length)))
    }

    LaunchedEffect(fileContent) {
        if (textFieldValue.text != fileContent) {
            val safeStart = textFieldValue.selection.start.coerceIn(0, fileContent.length)
            val safeEnd = textFieldValue.selection.end.coerceIn(0, fileContent.length)
            textFieldValue = textFieldValue.copy(text = fileContent, selection = TextRange(safeStart, safeEnd))
        }
    }

    val cursorPosition = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
    val isPython = activeFile?.filePath?.endsWith(".py", ignoreCase = true) == true

    // Compute active line and column for status bar and gutter
    val currentLineAndCol = remember(textFieldValue.text, cursorPosition) {
        val textBefore = textFieldValue.text.substring(0, cursorPosition)
        val linesBefore = textBefore.lines()
        val line = linesBefore.size
        val col = linesBefore.lastOrNull()?.length?.plus(1) ?: 1
        line to col
    }

    // Compute dynamic IntelliSense completions
    val completions = remember(textFieldValue.text, cursorPosition, activeFile?.filePath) {
        IntelliSenseEngine.computeCompletions(
            code = textFieldValue.text,
            cursorIndex = cursorPosition,
            filePath = activeFile?.filePath ?: "index.js"
        )
    }

    // Compute active function signature help
    val signatureHelp = remember(textFieldValue.text, cursorPosition) {
        IntelliSenseEngine.computeSignatureHelp(
            code = textFieldValue.text,
            cursorIndex = cursorPosition
        )
    }

    fun applyCompletionItem(item: CompletionItem) {
        val (newCode, newCursor) = IntelliSenseEngine.applyCompletion(
            textFieldValue.text,
            cursorPosition,
            item
        )
        textFieldValue = TextFieldValue(newCode, TextRange(newCursor))
        viewModel.updateActiveFileContent(newCode)
    }

    fun insertAtCursor(symbol: String) {
        val selStart = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val selEnd = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        val oldText = textFieldValue.text
        val newText = oldText.substring(0, selStart) + symbol + oldText.substring(selEnd)
        val newCursor = selStart + symbol.length
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateActiveFileContent(newText)
    }

    fun moveCursor(delta: Int) {
        val newPos = (textFieldValue.selection.end + delta).coerceIn(0, textFieldValue.text.length)
        textFieldValue = textFieldValue.copy(selection = TextRange(newPos))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
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
                        "js" -> "JS" to Color(0xFFF7DF1E)
                        "ts" -> "TS" to Color(0xFF3178C6)
                        "py" -> "PY" to Color(0xFF3776AB)
                        "json" -> "{}" to Color(0xFF00B0F4)
                        "env" -> "ENV" to DiscordGreen
                        else -> "TXT" to DiscordTextMuted
                    }

                    Surface(
                        color = if (isSelected) DiscordBackground else DiscordSurface,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, DiscordHover) else null,
                        modifier = Modifier
                            .clickable { viewModel.selectFile(file) }
                            .testTag("tab_file_${file.filePath}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

            // Quick Run / Stop button
            Button(
                onClick = {
                    project?.let {
                        if (isRunning) viewModel.runtimeEngine.stopBot(it.id)
                        else viewModel.runtimeEngine.startBot(it)
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

            // AI Architect Assistant Button
            Surface(
                color = DiscordBlurple.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordBlurple),
                modifier = Modifier.clickable { viewModel.setTab(AppTab.AI_ASSISTANT) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI Assist",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Prettier Code Formatter Button
            Surface(
                color = DiscordHover,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable {
                    val formatted = formatSourceCode(textFieldValue.text)
                    viewModel.updateActiveFileContent(formatted)
                    textFieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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

            // Cursor navigation buttons
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { moveCursor(-1) }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Cursor Left",
                    tint = DiscordTextPrimary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .size(14.dp)
                )
            }

            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { moveCursor(1) }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Cursor Right",
                    tint = DiscordTextPrimary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .size(14.dp)
                )
            }

            // Indent Tab button (2 spaces)
            Surface(
                color = DiscordElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { insertAtCursor("  ") }
            ) {
                Text(
                    text = "Tab",
                    color = DiscordTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Quick Insertion characters
            listOf("(", ")", "{", "}", "[", "]", "=>", ";", "\"", "'", "`", ":", "=", "+", "$", ".").forEach { symbol ->
                Surface(
                    color = DiscordElevated,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { insertAtCursor(symbol) }
                ) {
                    Text(
                        text = symbol,
                        color = DiscordTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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
        val lines = remember(textFieldValue.text) { textFieldValue.text.lines() }
        val lineCount = lines.size.coerceAtLeast(1)
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(TerminalBg)
                .verticalScroll(scrollState)
        ) {
            // Line numbers column with active line highlight and error indicators
            val errorLines = remember(diagnostics) { diagnostics.map { it.lineNumber }.toSet() }

            Column(
                modifier = Modifier
                    .background(DiscordDarker)
                    .border(
                        width = 0.5.dp,
                        color = DiscordHover,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .width(48.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    val hasError = errorLines.contains(i)
                    val isCurrentLine = i == currentLineAndCol.first

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (hasError) {
                            Text(
                                text = "●",
                                color = DiscordRed,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        } else if (isCurrentLine) {
                            Text(
                                text = "›",
                                color = DiscordBlurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        Text(
                            text = "$i",
                            color = if (hasError) DiscordRed
                            else if (isCurrentLine) DiscordBlurple
                            else DiscordTextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (hasError || isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Editor content text field with IntelliSense trigger on value change
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.updateActiveFileContent(newValue.text)
                        showIntelliSenseBar = true
                        showSignatureHelp = true
                    },
                    visualTransformation = syntaxTransformation,
                    textStyle = TextStyle(
                        color = DiscordTextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(DiscordBlurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_editor_input")
                )
            }
        }

        // Professional IDE Bottom Status Bar
        Surface(
            color = DiscordDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cursor position and file metrics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ln ${currentLineAndCol.first}, Col ${currentLineAndCol.second}",
                        color = DiscordTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "•",
                        color = DiscordTextMuted,
                        fontSize = 10.sp
                    )

                    Text(
                        text = "${textFieldValue.text.length} chars",
                        color = DiscordTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "•",
                        color = DiscordTextMuted,
                        fontSize = 10.sp
                    )

                    Text(
                        text = if (isPython) "Python 3.x" else "JavaScript (v14)",
                        color = DiscordBlurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Diagnostics and Encoding status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (diagnostics.isNotEmpty()) DiscordRed.copy(alpha = 0.2f) else DiscordGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.clickable { showDiagnosticsPanel = !showDiagnosticsPanel }
                    ) {
                        Text(
                            text = if (diagnostics.isNotEmpty()) "⚠️ ${diagnostics.size} issues" else "✓ 0 issues",
                            color = if (diagnostics.isNotEmpty()) DiscordRed else DiscordGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "UTF-8",
                        color = DiscordTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Error Diagnostics Tray at bottom of editor if errors exist or toggled
        if (diagnostics.isNotEmpty() || showDiagnosticsPanel) {
            Surface(
                color = DiscordDarker,
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
                    Button(
                        onClick = {
                            showFilesSheet = false
                            viewModel.showNewFileDialog.value = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add File", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New File", fontSize = 12.sp)
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
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

fun formatSourceCode(rawCode: String): String {
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
        if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
            indentLevel = (indentLevel - 1).coerceAtLeast(0)
        }

        formatted.append(indent.repeat(indentLevel)).append(trimmed).append("\n")

        // Check if line opens block
        if (trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[")) {
            indentLevel++
        }
    }
    return formatted.toString().trimEnd()
}
