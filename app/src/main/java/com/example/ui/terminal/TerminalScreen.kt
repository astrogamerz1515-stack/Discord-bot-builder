package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TerminalLog
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
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import kotlinx.coroutines.launch

enum class TerminalFilter(val label: String) {
    ALL("All"),
    OUTPUT("Output"),
    ERRORS("Errors"),
    SYSTEM("System")
}

@Composable
fun TerminalScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val project by viewModel.currentProject.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val terminalInput by viewModel.terminalInput.collectAsState()
    val isRunning by viewModel.runtimeEngine.isRunning.collectAsState()
    val gatewayPing by viewModel.runtimeEngine.gatewayPingMs.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()

    var activeFilter by remember { mutableStateOf(TerminalFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    // Filter logs based on category and search query
    val filteredLogs = remember(logs, activeFilter, searchQuery) {
        logs.filter { log ->
            val matchesFilter = when (activeFilter) {
                TerminalFilter.ALL -> true
                TerminalFilter.OUTPUT -> log.type == "STDOUT" || log.type == "SUCCESS" || log.type == "INPUT"
                TerminalFilter.ERRORS -> log.type == "STDERR" || log.type == "WARN"
                TerminalFilter.SYSTEM -> log.type == "SYSTEM" || log.type == "INFO"
            }
            val matchesSearch = searchQuery.isBlank() || log.text.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    // Auto-scroll to bottom on new log when autoScroll is enabled
    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // Terminal Status & Top Process Controls
        Surface(
            color = DiscordDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge with Live Ping
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isRunning) DiscordGreen else DiscordRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "ONLINE (Gateway ${gatewayPing}ms)" else "OFFLINE",
                            color = if (isRunning) DiscordGreen else DiscordTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Process Control Buttons & Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("btn_terminal_toggle_bot")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Stop" else "Start",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRunning) "Kill" else "Run",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isRunning) {
                            IconButton(
                                onClick = { project?.let { viewModel.runtimeEngine.restartBot(it) } },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(DiscordElevated, RoundedCornerShape(6.dp))
                                    .testTag("btn_terminal_restart")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart",
                                    tint = DiscordYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Search log toggle
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (showSearchBar) DiscordBlurple else DiscordElevated, RoundedCornerShape(6.dp))
                                .testTag("btn_terminal_search_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Logs",
                                tint = if (showSearchBar) Color.White else DiscordTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy all logs
                        IconButton(
                            onClick = {
                                val allText = logs.joinToString("\n") { it.text }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Logs", allText))
                                Toast.makeText(context, "Copied ${logs.size} log lines to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(DiscordElevated, RoundedCornerShape(6.dp))
                                .testTag("btn_terminal_copy_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Logs",
                                tint = DiscordTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Clear logs
                        IconButton(
                            onClick = { viewModel.clearTerminal() },
                            modifier = Modifier
                                .size(30.dp)
                                .background(DiscordElevated, RoundedCornerShape(6.dp))
                                .testTag("btn_terminal_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = DiscordTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Optional Search Input Bar
                if (showSearchBar) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter terminal output text...", color = DiscordTextMuted, fontSize = 11.sp) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = DiscordTextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = DiscordTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedContainerColor = DiscordBackground,
                            unfocusedContainerColor = DiscordBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter tabs + Quick Scroll
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TerminalFilter.values().forEach { filter ->
                            val isSelected = activeFilter == filter
                            Surface(
                                color = if (isSelected) DiscordBlurple else DiscordElevated,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .clickable { activeFilter = filter }
                                    .testTag("terminal_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) Color.White else DiscordTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Auto-scroll indicator & Jump to bottom button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (autoScroll) DiscordGreen.copy(alpha = 0.15f) else DiscordElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { autoScroll = !autoScroll }
                        ) {
                            Text(
                                text = if (autoScroll) "Auto-scroll: ON" else "Auto-scroll: OFF",
                                color = if (autoScroll) DiscordGreen else DiscordTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (filteredLogs.isNotEmpty()) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(filteredLogs.size - 1)
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerticalAlignBottom,
                                contentDescription = "Scroll to bottom",
                                tint = DiscordTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Categorized Quick Commands Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickCmds = listOf(
                        "node index.js",
                        "status",
                        "ping",
                        "npm test",
                        "npm install",
                        "ls -la",
                        "cat index.js",
                        "python bot.py",
                        "env",
                        "help",
                        "clear"
                    )

                    quickCmds.forEach { cmd ->
                        Surface(
                            color = DiscordElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clickable { viewModel.executeQuickTerminalCommand(cmd) }
                                .testTag("chip_cmd_$cmd")
                        ) {
                            Text(
                                text = cmd,
                                color = TerminalCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Terminal Log Console Output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("terminal_output_list"),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Text(
                        text = if (logs.isEmpty()) "• Terminal initialized. Type 'help' or click quick commands above." else "No log entries match your filter.",
                        color = DiscordTextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            items(filteredLogs) { log ->
                val (color, prefix) = when (log.type) {
                    "INPUT" -> DiscordTextPrimary to ""
                    "SYSTEM" -> TerminalCyan to "• "
                    "SUCCESS" -> TerminalGreen to "✔ "
                    "WARN" -> TerminalYellow to "▲ "
                    "STDERR" -> TerminalRed to "✖ "
                    else -> DiscordTextSecondary to ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Log line", log.text))
                            Toast.makeText(context, "Copied log line", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    if (log.type == "INPUT") {
                        Text(
                            text = "bot@studio:~$ ",
                            color = TerminalGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$prefix${log.text}",
                        color = color,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Interactive Terminal Prompt & Input Bar with Command History Navigation
        Surface(
            color = DiscordDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "bot@studio:~$",
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Command history up/down buttons
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateCommandHistory(up = true) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(DiscordElevated, RoundedCornerShape(4.dp))
                            .testTag("btn_history_up")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Previous Command",
                            tint = if (commandHistory.isNotEmpty()) DiscordTextPrimary else DiscordTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateCommandHistory(up = false) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(DiscordElevated, RoundedCornerShape(4.dp))
                            .testTag("btn_history_down")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Next Command",
                            tint = if (commandHistory.isNotEmpty()) DiscordTextPrimary else DiscordTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = terminalInput,
                    onValueChange = { viewModel.setTerminalInput(it) },
                    placeholder = {
                        Text(
                            "node index.js, help, status...",
                            color = DiscordTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = DiscordTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.executeTerminalCommand() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordGreen,
                        unfocusedBorderColor = DiscordHover,
                        focusedContainerColor = DiscordBackground,
                        unfocusedContainerColor = DiscordBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("input_terminal_command")
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.executeTerminalCommand() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(DiscordGreen, RoundedCornerShape(6.dp))
                        .testTag("btn_send_terminal_cmd")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Execute",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
