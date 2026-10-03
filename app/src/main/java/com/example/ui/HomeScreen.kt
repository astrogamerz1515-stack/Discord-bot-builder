package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotLanguage
import com.example.data.model.BotProject
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

/**
 * HomeScreen providing the 2 primary options:
 * 1. "New Bot" - Opens bot creation wizard/dialog.
 * 2. "My Bots" - Displays list of all bots created by the user; clicking any bot immediately opens the code editor.
 */
@Composable
fun HomeScreen(
    viewModel: BotStudioViewModel,
    onOpenEditor: (BotProject) -> Unit = { project ->
        viewModel.selectProject(project)
        viewModel.setTab(AppTab.EDITOR)
    },
    onCreateBotClick: () -> Unit = {
        viewModel.showNewProjectDialog.value = true
    }
) {
    val context = LocalContext.current
    val allProjects by viewModel.allProjects.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    val isRunning by viewModel.runtimeEngine.isRunning.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<BotProject?>(null) }

    val filteredProjects = remember(allProjects, searchQuery) {
        if (searchQuery.isBlank()) {
            allProjects
        } else {
            allProjects.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.language.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .testTag("home_screen")
    ) {
        // Top Header Banner
        Surface(
            color = DiscordDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = DiscordBlurple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "BotStudio",
                                tint = DiscordBlurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BotStudio",
                            color = DiscordTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Discord Bot IDE & 24/7 Runtime",
                            color = DiscordTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Background runtime active status
                Surface(
                    color = if (isRunning) DiscordGreen.copy(alpha = 0.15f) else DiscordSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isRunning) DiscordGreen.copy(alpha = 0.4f) else DiscordHover)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isRunning) DiscordGreen else DiscordTextMuted, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Runtime: RUNNING" else "Runtime: IDLE",
                            color = if (isRunning) DiscordGreen else DiscordTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ══════════════════════════════════════════════════════
            // OPTION 1: "NEW BOT" HERO OPTION CARD
            // ══════════════════════════════════════════════════════
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateBotClick() }
                        .testTag("card_option_new_bot")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF5865F2), // Discord Blurple
                                        Color(0xFF3D7EFF), // Tech Blue
                                        Color(0xFF00B4D8)  // Cyan
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "OPTION 1",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "＋ New Bot",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Create a new Discord bot with ready-to-use JavaScript, TypeScript, or Python templates.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action Button inside Card
                            Button(
                                onClick = { onCreateBotClick() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_home_new_bot")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Bot",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Create",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════
            // OPTION 2: "MY BOTS" HEADER & SEARCH
            // ══════════════════════════════════════════════════════
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🤖 My Bots",
                                color = DiscordTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = DiscordBlurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${allProjects.size} ${if (allProjects.size == 1) "bot" else "bots"}",
                                    color = DiscordBlurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Tap a bot to open editor",
                            color = DiscordTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Search input if there are bots
                    if (allProjects.size > 2) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search your bots...", color = DiscordTextMuted, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = DiscordTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DiscordTextPrimary,
                                unfocusedTextColor = DiscordTextPrimary,
                                focusedBorderColor = DiscordBlurple,
                                unfocusedBorderColor = DiscordHover,
                                focusedContainerColor = DiscordSurface,
                                unfocusedContainerColor = DiscordSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("input_search_bots")
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════
            // MY BOTS LIST (OR EMPTY STATE)
            // ══════════════════════════════════════════════════════
            if (filteredProjects.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DiscordSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DiscordHover),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("card_empty_bots_state")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = DiscordElevated,
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = DiscordBlurple,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (searchQuery.isNotBlank()) "No Bots Found" else "No Bots Created Yet",
                                color = DiscordTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "No bots match '$searchQuery'. Clear the search or create a new bot."
                                else
                                    "You haven't created any Discord bots yet. Tap 'New Bot' above to build your first bot in seconds!",
                                color = DiscordTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Button(
                                onClick = { onCreateBotClick() },
                                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_empty_create_bot")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Your First Bot", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(items = filteredProjects, key = { it.id }) { project ->
                    val isSelected = currentProject?.id == project.id
                    val isProjectRunning = isRunning && isSelected

                    HomeBotCard(
                        project = project,
                        isSelected = isSelected,
                        isRunning = isProjectRunning,
                        onClick = { onOpenEditor(project) },
                        onOpenEditorClick = { onOpenEditor(project) },
                        onRunToggleClick = {
                            viewModel.selectProject(project)
                            if (isRunning && isSelected) {
                                viewModel.stopBotProcess()
                                Toast.makeText(context, "Bot stopped", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.startBotProcess()
                                Toast.makeText(context, "Bot started in background", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onConfigureClick = {
                            viewModel.selectProject(project)
                            viewModel.setTab(AppTab.BOT_CONFIG)
                        },
                        onDeleteClick = {
                            projectToDelete = project
                        }
                    )
                }
            }
        }
    }

    // Confirmation Dialog to Delete Bot Project
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = DiscordSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = DiscordRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Bot",
                        color = DiscordTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${project.name}\"? All its files, database values, and configuration will be permanently removed.",
                    color = DiscordTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = project.id
                        projectToDelete = null
                        viewModel.deleteProject(id)
                        Toast.makeText(context, "Bot deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_project")
                ) {
                    Text("Delete Bot", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Individual Bot Card inside the "My Bots" section.
 * Clicking anywhere on the card or clicking "Open Editor" selects the bot and opens the code editor.
 */
@Composable
fun HomeBotCard(
    project: BotProject,
    isSelected: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit,
    onOpenEditorClick: () -> Unit,
    onRunToggleClick: () -> Unit,
    onConfigureClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (langBadge, langColor) = when (project.language.uppercase()) {
        "JAVASCRIPT", "JS" -> "JS" to Color(0xFFF7DF1E)
        "TYPESCRIPT", "TS" -> "TS" to Color(0xFF3178C6)
        "PYTHON", "PY" -> "PY" to Color(0xFF3776AB)
        "RUST", "RS" -> "RS" to Color(0xFFDEA584)
        "GO" -> "GO" to Color(0xFF00ADD8)
        "JAVA" -> "JAVA" to Color(0xFFE76F00)
        "CSHARP", "CS" -> "C#" to Color(0xFF239120)
        else -> "BOT" to DiscordBlurple
    }

    val botEmoji = remember(project.id) {
        when ((project.id % 5).toInt()) {
            0 -> "🤖"
            1 -> "🛡️"
            2 -> "🎵"
            3 -> "⚡"
            else -> "🎮"
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DiscordSurface else DiscordDarker
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) DiscordBlurple else DiscordHover
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bot_item_${project.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Avatar + Name + Language + Online Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle with emoji
                Surface(
                    color = DiscordElevated,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DiscordHover),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = botEmoji, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name and Description Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            color = DiscordTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Language Badge
                        Surface(
                            color = langColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = langBadge,
                                color = langColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (project.description.isNotBlank()) {
                        Text(
                            text = project.description,
                            color = DiscordTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Prefix: '${project.prefix}' • ${project.language}",
                            color = DiscordTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Online/Offline status dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isRunning) DiscordGreen else DiscordTextMuted,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "Active" else "Idle",
                        color = if (isRunning) DiscordGreen else DiscordTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Bottom Action Row: "Open Editor" + Run + Config + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Primary Action: Open in Editor
                Button(
                    onClick = onOpenEditorClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_open_editor_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Open Editor",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Editor",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Run/Stop Button
                    Button(
                        onClick = onRunToggleClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) DiscordRed.copy(alpha = 0.25f) else DiscordGreen.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_toggle_run_${project.id}")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Stop" else "Run",
                            tint = if (isRunning) DiscordRed else DiscordGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRunning) "Stop" else "Run",
                            color = if (isRunning) DiscordRed else DiscordGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bot Settings Button
                    IconButton(
                        onClick = onConfigureClick,
                        modifier = Modifier
                            .size(34.dp)
                            .background(DiscordElevated, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Config",
                            tint = DiscordTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete Bot Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(34.dp)
                            .background(DiscordElevated, RoundedCornerShape(6.dp))
                            .testTag("btn_delete_bot_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = DiscordRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
