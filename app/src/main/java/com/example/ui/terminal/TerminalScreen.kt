package com.example.ui.terminal

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BotStudioViewModel
import com.example.ui.theme.DiscordBackground
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
import com.example.ui.theme.TerminalGray
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow

@Composable
fun TerminalScreen(viewModel: BotStudioViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val terminalInput by viewModel.terminalInput.collectAsState()
    val isRunning by viewModel.runtimeEngine.isRunning.collectAsState()
    val gatewayPing by viewModel.runtimeEngine.gatewayPingMs.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // Terminal Status & Control Header
        Surface(
            color = DiscordDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
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

                    // Process Controls
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                .height(32.dp)
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
                                    .size(32.dp)
                                    .background(DiscordHover, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart",
                                    tint = DiscordYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.clearTerminal() },
                            modifier = Modifier
                                .size(32.dp)
                                .background(DiscordHover, RoundedCornerShape(6.dp))
                                .testTag("btn_clear_terminal")
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

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Terminal Command Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickCommands = listOf(
                        "node index.js",
                        "python bot.py",
                        "npm test",
                        "npm start",
                        "npm install",
                        "status",
                        "ping",
                        "help",
                        "ls",
                        "env",
                        "clear"
                    )

                    quickCommands.forEach { cmd ->
                        Surface(
                            color = DiscordElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("chip_cmd_$cmd")
                        ) {
                            Text(
                                text = cmd,
                                color = TerminalCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable {
                                        viewModel.executeQuickTerminalCommand(cmd)
                                    }
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                val (color, prefix) = when (log.type) {
                    "INPUT" -> DiscordTextPrimary to ""
                    "SYSTEM" -> TerminalCyan to "• "
                    "SUCCESS" -> TerminalGreen to "✔ "
                    "WARN" -> TerminalYellow to "▲ "
                    "STDERR" -> TerminalRed to "✖ "
                    else -> DiscordTextSecondary to ""
                }

                Row(modifier = Modifier.fillMaxWidth()) {
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

        // Terminal Prompt & Input Bar
        Surface(
            color = DiscordDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "bot@studio:~$",
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = terminalInput,
                    onValueChange = { viewModel.setTerminalInput(it) },
                    placeholder = {
                        Text(
                            "node index.js, status, help...",
                            color = DiscordTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = DiscordTextPrimary,
                        fontSize = 13.sp,
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
                        .height(48.dp)
                        .testTag("input_terminal_command")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.executeTerminalCommand() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DiscordGreen, RoundedCornerShape(8.dp))
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
