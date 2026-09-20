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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotFile
import com.example.ui.AppTab
import com.example.ui.BotStudioViewModel
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

    var showFilesSheet by remember { mutableStateOf(false) }
    var showDiagnosticsPanel by remember { mutableStateOf(false) }

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
                    .size(36.dp)
                    .testTag("btn_open_file_drawer")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Project Files",
                    tint = DiscordTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // File Tabs
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                files.forEach { file ->
                    val isSelected = activeFile?.id == file.id
                    Surface(
                        color = if (isSelected) DiscordBackground else DiscordSurface,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        modifier = Modifier
                            .clickable { viewModel.selectFile(file) }
                            .testTag("tab_file_${file.filePath}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
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
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else DiscordGreen
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("btn_quick_run_bot")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
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
            // Snippets Button
            Surface(
                color = DiscordBlurple,
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
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Snippets",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // AI Architect Assistant Button
            Surface(
                color = DiscordBlurple.copy(alpha = 0.35f),
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
                    val formatted = formatSourceCode(fileContent)
                    viewModel.updateActiveFileContent(formatted)
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

            // Quick Insertion characters
            listOf("(", ")", "{", "}", "[", "]", "=>", ";", "\"", "'", ":", "=", "+", "$").forEach { symbol ->
                Surface(
                    color = DiscordElevated,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { viewModel.insertTextAtCursor(symbol) }
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

            // Save File button
            Surface(
                color = DiscordHover,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { viewModel.saveActiveFile() }
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

        // Code Editor Body with Line Numbers
        val lines = remember(fileContent) { fileContent.lines() }
        val lineCount = lines.size.coerceAtLeast(1)

        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(TerminalBg)
                .verticalScroll(scrollState)
        ) {
            // Line numbers column with error indicators
            val errorLines = remember(diagnostics) { diagnostics.map { it.lineNumber }.toSet() }

            Column(
                modifier = Modifier
                    .background(DiscordDarker)
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .width(48.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    val hasError = errorLines.contains(i)
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
                        }
                        Text(
                            text = "$i",
                            color = if (hasError) DiscordRed else DiscordTextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (hasError) FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Editor content text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = fileContent,
                    onValueChange = { viewModel.updateActiveFileContent(it) },
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
