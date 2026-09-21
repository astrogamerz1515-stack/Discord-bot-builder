package com.example.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotLanguage
import com.example.ui.ai.AiStudioAssistantScreen
import com.example.ui.ai.ApiKeysManagementScreen
import com.example.ui.config.BotConfigScreen
import com.example.ui.deploy.DeploymentScreen
import com.example.ui.editor.CodeEditorScreen
import com.example.ui.editor.CodeSnippets
import com.example.ui.embedbuilder.VisualEmbedDesignerScreen
import com.example.ui.extensions.ExtensionsAndDynamicLoadingScreen
import com.example.ui.gradle.GradleConfigScreen
import com.example.ui.install.PackageInstallScreen
import com.example.ui.simulator.DiscordSimulatorScreen
import com.example.ui.storage.AdvancedStorageScreen
import com.example.ui.terminal.TerminalScreen
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

@Composable
fun MainScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val project by viewModel.currentProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val isRunning by viewModel.runtimeEngine.isRunning.collectAsState()

    val showNewProjectDialog by viewModel.showNewProjectDialog.collectAsState()
    val showNewFileDialog by viewModel.showNewFileDialog.collectAsState()
    val showProjectSwitchDialog by viewModel.showProjectSwitchDialog.collectAsState()
    val showSnippetsDialog by viewModel.showSnippetsDialog.collectAsState()
    val showGeneratedCodeDialog by viewModel.showGeneratedCodeDialog.collectAsState()
    val generatedCodeText by viewModel.generatedCodeText.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = DiscordBackground,
        topBar = {
            // Top App Bar
            Surface(
                color = DiscordDarker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo & App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.showProjectSwitchDialog.value = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(DiscordBlurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "BotStudio",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project?.name ?: "BotStudio",
                                    color = DiscordTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Project",
                                    tint = DiscordTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isRunning) DiscordGreen else DiscordRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = project?.language ?: "JavaScript",
                                    color = DiscordTextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = DiscordGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "Open Access",
                                        color = DiscordGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Quick Gradle Button
                        IconButton(
                            onClick = { viewModel.setTab(AppTab.GRADLE) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Gradle", tint = if (currentTab == AppTab.GRADLE) DiscordBlurple else DiscordTextSecondary, modifier = Modifier.size(18.dp))
                        }

                        // Quick Extensions Button
                        IconButton(
                            onClick = { viewModel.setTab(AppTab.EXTENSIONS) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = "Extensions", tint = if (currentTab == AppTab.EXTENSIONS) DiscordBlurple else DiscordTextSecondary, modifier = Modifier.size(18.dp))
                        }

                        // New Project Button
                        Button(
                            onClick = { viewModel.showNewProjectDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordHover),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_top_new_project")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Project", tint = DiscordTextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Bot", color = DiscordTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Sub-header Scrollable Tab Strip for direct access to all tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DiscordBackground)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        Surface(
                            color = if (isSelected) DiscordBlurple else DiscordDarker,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { viewModel.setTab(tab) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = when (tab) {
                                    AppTab.EDITOR -> Icons.Default.Code
                                    AppTab.TERMINAL -> Icons.Default.Terminal
                                    AppTab.SIMULATOR -> Icons.Default.Chat
                                    AppTab.EMBED_BUILDER -> Icons.Default.DashboardCustomize
                                    AppTab.STORAGE -> Icons.Default.Storage
                                    AppTab.EXTENSIONS -> Icons.Default.Extension
                                    AppTab.GRADLE -> Icons.Default.Build
                                    AppTab.AI_ASSISTANT -> Icons.Default.AutoAwesome
                                    AppTab.API_KEYS -> Icons.Default.Key
                                    AppTab.PACKAGES -> Icons.Default.Extension
                                    AppTab.BOT_CONFIG -> Icons.Default.Settings
                                    AppTab.DEPLOY -> Icons.Default.CloudUpload
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else DiscordTextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) Color.White else DiscordTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // M3 Bottom Navigation Bar
            val bottomTabs = listOf(
                AppTab.EDITOR,
                AppTab.TERMINAL,
                AppTab.SIMULATOR,
                AppTab.EMBED_BUILDER,
                AppTab.STORAGE,
                AppTab.EXTENSIONS,
                AppTab.GRADLE,
                AppTab.AI_ASSISTANT,
                AppTab.BOT_CONFIG
            )
            NavigationBar(
                containerColor = DiscordDarker,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                bottomTabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    val (icon, label) = when (tab) {
                        AppTab.EDITOR -> Icons.Default.Code to "Editor"
                        AppTab.TERMINAL -> Icons.Default.Terminal to "Terminal"
                        AppTab.SIMULATOR -> Icons.Default.Chat to "Simulator"
                        AppTab.EMBED_BUILDER -> Icons.Default.DashboardCustomize to "Embeds"
                        AppTab.STORAGE -> Icons.Default.Storage to "Storage"
                        AppTab.EXTENSIONS -> Icons.Default.Extension to "Extensions"
                        AppTab.GRADLE -> Icons.Default.Build to "Gradle"
                        AppTab.AI_ASSISTANT -> Icons.Default.AutoAwesome to "AI Studio"
                        AppTab.API_KEYS -> Icons.Default.Key to "AI Keys"
                        AppTab.PACKAGES -> Icons.Default.Extension to "Install"
                        AppTab.BOT_CONFIG -> Icons.Default.Settings to "Config"
                        AppTab.DEPLOY -> Icons.Default.CloudUpload to "Deploy"
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DiscordBlurple,
                            selectedTextColor = DiscordBlurple,
                            indicatorColor = DiscordHover,
                            unselectedIconColor = DiscordTextMuted,
                            unselectedTextColor = DiscordTextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.EDITOR -> CodeEditorScreen(viewModel)
                AppTab.TERMINAL -> TerminalScreen(viewModel)
                AppTab.SIMULATOR -> DiscordSimulatorScreen(viewModel)
                AppTab.EMBED_BUILDER -> VisualEmbedDesignerScreen(viewModel)
                AppTab.STORAGE -> AdvancedStorageScreen(viewModel)
                AppTab.EXTENSIONS -> ExtensionsAndDynamicLoadingScreen(viewModel)
                AppTab.GRADLE -> GradleConfigScreen(viewModel)
                AppTab.AI_ASSISTANT -> AiStudioAssistantScreen(viewModel)
                AppTab.API_KEYS -> ApiKeysManagementScreen()
                AppTab.PACKAGES -> PackageInstallScreen(viewModel)
                AppTab.BOT_CONFIG -> BotConfigScreen(viewModel)
                AppTab.DEPLOY -> DeploymentScreen(viewModel)
            }
        }
    }

    // Dialog: Create New Project
    if (showNewProjectDialog) {
        var newName by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var newPrefix by remember { mutableStateOf("!") }
        var selectedLang by remember { mutableStateOf(BotLanguage.JAVASCRIPT) }
        var showLangDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.showNewProjectDialog.value = false },
            containerColor = DiscordSurface,
            title = { Text("Create Discord Bot Project", color = DiscordTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Bot Project Name") },
                        placeholder = { Text("e.g. Aegis Moderation Bot") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )

                    // Language Selector
                    Box {
                        OutlinedTextField(
                            value = selectedLang.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Programming Language & Framework") },
                            trailingIcon = {
                                IconButton(onClick = { showLangDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLangDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showLangDropdown,
                            onDismissRequest = { showLangDropdown = false },
                            modifier = Modifier.background(DiscordElevated)
                        ) {
                            BotLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.displayName, color = DiscordTextPrimary) },
                                    onClick = {
                                        selectedLang = lang
                                        showLangDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newPrefix,
                        onValueChange = { newPrefix = it },
                        label = { Text("Command Prefix") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description (optional)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createNewProject(newName, newDesc, selectedLang, newPrefix)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Create Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewProjectDialog.value = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }

    // Dialog: Switch Project
    if (showProjectSwitchDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showProjectSwitchDialog.value = false },
            containerColor = DiscordSurface,
            title = { Text("Your Bot Projects", color = DiscordTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allProjects.forEach { proj ->
                        val isCurrent = project?.id == proj.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) DiscordElevated else DiscordDarker
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectProject(proj)
                                    viewModel.showProjectSwitchDialog.value = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proj.name,
                                        color = if (isCurrent) DiscordBlurple else DiscordTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${proj.language} • Prefix '${proj.prefix}'",
                                        color = DiscordTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (allProjects.size > 1) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteProject(proj.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = DiscordRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showProjectSwitchDialog.value = false }) {
                    Text("Close", color = DiscordTextSecondary)
                }
            }
        )
    }

    // Dialog: Create New File
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.showNewFileDialog.value = false },
            containerColor = DiscordSurface,
            title = { Text("Create New File", color = DiscordTextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter file path (e.g. 'commands/kick.js', 'events/ready.py')",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Path") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewFileDialog.value = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }

    // Dialog: Snippets Modal
    if (showSnippetsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showSnippetsDialog.value = false },
            containerColor = DiscordSurface,
            title = { Text("Discord Code Snippets", color = DiscordTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPy = project?.language?.contains("Python", ignoreCase = true) == true
                    CodeSnippets.ALL_SNIPPETS.forEach { snip ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DiscordDarker),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val code = if (isPy) snip.codeForPy else snip.codeForJs
                                    viewModel.insertSnippet(code)
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(snip.title, color = DiscordBlurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(snip.description, color = DiscordTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showSnippetsDialog.value = false }) {
                    Text("Close", color = DiscordTextSecondary)
                }
            }
        )
    }

    // Dialog: Generated Embed Code
    if (showGeneratedCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showGeneratedCodeDialog.value = false },
            containerColor = DiscordSurface,
            title = { Text("Generated Embed Code (${project?.language})", color = DiscordTextPrimary) },
            text = {
                Surface(
                    color = DiscordDarker,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = generatedCodeText,
                        color = DiscordTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Embed Code", generatedCodeText))
                            Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordHover)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    Button(
                        onClick = {
                            viewModel.insertSnippet(generatedCodeText)
                            viewModel.showGeneratedCodeDialog.value = false
                            viewModel.setTab(AppTab.EDITOR)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                    ) {
                        Text("Insert in Editor")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showGeneratedCodeDialog.value = false }) {
                    Text("Close", color = DiscordTextSecondary)
                }
            }
        )
    }
}
