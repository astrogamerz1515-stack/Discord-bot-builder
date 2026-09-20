package com.example.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiApiClient
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
import kotlinx.coroutines.launch

enum class AiTaskMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String) {
    FLOW_GEN("Bot Flow Gen", Icons.Default.AutoAwesome, "Natural language to full Discord slash command & logic flow"),
    DEBUGGER("AI Debug & Explain", Icons.Default.BugReport, "Diagnose runtime errors, stack traces, and interaction bugs"),
    CODE_COMPLETION("Code Completion", Icons.Default.Code, "Generate next functions, event listeners, or embeds"),
    MIGRATION("API Migration", Icons.Default.Transform, "Upgrade discord.js v13 ➔ v14 or discord.py 1.x ➔ 2.x"),
    TEST_CASES("Generate Tests", Icons.Default.Speed, "Create unit & integration test suites for commands"),
    DOCS("Doc Generator", Icons.Default.Description, "Auto-generate README commands markdown table and docs")
}

@Composable
fun AiStudioAssistantScreen(viewModel: BotStudioViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val fileContent by viewModel.activeFileContent.collectAsState()
    val diagnostics by viewModel.codeDiagnostics.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(AiTaskMode.FLOW_GEN) }
    var userPrompt by remember { mutableStateOf("") }
    var aiResult by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val hasKey = remember { GeminiApiClient.hasApiKey() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Bot Architect & Automation",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Gemini 3.5 Flash • Natural language bot generation & diagnostics",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = if (hasKey) DiscordGreen.copy(alpha = 0.2f) else DiscordYellow.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (hasKey) DiscordGreen else DiscordYellow, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasKey) "Gemini Live" else "Demo Mode",
                        color = if (hasKey) DiscordGreen else DiscordYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiTaskMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                Surface(
                    color = if (isSelected) DiscordBlurple else DiscordSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable {
                            selectedMode = mode
                            // Prefill contextual prompts based on current project
                            userPrompt = when (mode) {
                                AiTaskMode.FLOW_GEN -> "Create a moderation slash command /warn with reason, database storage, and DM notification"
                                AiTaskMode.DEBUGGER -> if (diagnostics.isNotEmpty()) "Diagnose these syntax/runtime errors in ${activeFile?.filePath}:\n${diagnostics.joinToString("\n") { "Line ${it.lineNumber}: ${it.message}" }}" else "Explain common Discord API InteractionNotReplied error and how to fix deferReply timing"
                                AiTaskMode.CODE_COMPLETION -> "Complete this ${project?.language ?: "JavaScript"} command to fetch user avatar, joined date, and roles"
                                AiTaskMode.MIGRATION -> "Migrate my Discord.js v13 interaction listener and MessageEmbed code to Discord.js v14"
                                AiTaskMode.TEST_CASES -> "Write automated Jest / Pytest test cases for ${activeFile?.filePath ?: "index.js"} testing slash commands"
                                AiTaskMode.DOCS -> "Generate clean Discord bot documentation and commands table in Markdown for ${project?.name ?: "Bot"}"
                            }
                        }
                        .testTag("ai_tab_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.label,
                            tint = if (isSelected) Color.White else DiscordTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.label,
                            color = if (isSelected) Color.White else DiscordTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = selectedMode.description,
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("Describe what you want to generate or debug...", color = DiscordTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary,
                        focusedContainerColor = DiscordDarker,
                        unfocusedContainerColor = DiscordDarker
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Context pill
                    Surface(
                        color = DiscordElevated,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Target: ${project?.name ?: "Bot"} (${project?.language ?: "JS"})",
                            color = DiscordTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (userPrompt.isNotBlank() && !isGenerating) {
                                isGenerating = true
                                aiResult = ""
                                copiedToClipboard = false
                                coroutineScope.launch {
                                    val sysInstruction = """
                                        You are an expert Discord Bot Engineer specialized in Discord.js v14, Discord.py 2.0+, Serenity (Rust), and JDA.
                                        Provide production-ready, clean, well-annotated code and concise engineering explanations.
                                        Format output with clean markdown headings and code blocks.
                                    """.trimIndent()

                                    val fullPrompt = """
                                        Current Bot: ${project?.name} (${project?.language})
                                        Prefix: ${project?.prefix}
                                        Task: ${selectedMode.label}
                                        Prompt: $userPrompt
                                        Active File (${activeFile?.filePath}):
                                        ```
                                        ${fileContent.take(1500)}
                                        ```
                                    """.trimIndent()

                                    val res = GeminiApiClient.callGemini(fullPrompt, sysInstruction)
                                    aiResult = res
                                    isGenerating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                        enabled = !isGenerating && userPrompt.isNotBlank(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generating...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run AI Generation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Output Section
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Solution & Generated Artifact",
                        color = DiscordTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (aiResult.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Copy button
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(aiResult))
                                    copiedToClipboard = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DiscordElevated),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (copiedToClipboard) DiscordGreen else DiscordTextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copiedToClipboard) "Copied!" else "Copy",
                                    color = if (copiedToClipboard) DiscordGreen else DiscordTextPrimary,
                                    fontSize = 11.sp
                                )
                            }

                            // Insert into active file
                            Button(
                                onClick = {
                                    // Extract code block or insert full response
                                    val codeRegex = Regex("```(?:[a-zA-Z]*)\\n([\\s\\S]*?)```")
                                    val match = codeRegex.find(aiResult)
                                    val codeToInsert = match?.groupValues?.get(1) ?: aiResult
                                    viewModel.updateActiveFileContent(fileContent + "\n\n" + codeToInsert)
                                    viewModel.setTab(AppTab.EDITOR)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DiscordGreen),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Code, contentDescription = "Insert", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert to Editor", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = DiscordDarker,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (aiResult.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = DiscordTextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Choose an AI mode above or enter a prompt to generate slash commands, fix errors, or create test suites.",
                                    color = DiscordTextMuted,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = aiResult,
                                    color = DiscordTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
