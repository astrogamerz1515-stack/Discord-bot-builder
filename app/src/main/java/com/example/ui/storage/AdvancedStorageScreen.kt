package com.example.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotKeyValue
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

@Composable
fun AdvancedStorageScreen(viewModel: BotStudioViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val storageList by viewModel.storageEntries.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("STRING") }
    var typeDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .padding(16.dp)
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
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Database",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advanced Bot Storage",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Room-backed Key-Value & JSON datastore for ${project?.name ?: "bot"}",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_add_storage_entry")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Key", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Info Banner with Code Usage
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "💡 SDK Access Code Snippet",
                    color = DiscordTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "// Store and retrieve persistent bot state\n" +
                            "const val = await db.get('prefix'); // or bot_storage.get('key')\n" +
                            "await db.set('guild_102_welcome', { enabled: true, channel: '109283' });",
                    color = DiscordGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Entries List
        if (storageList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = DiscordTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No storage keys created yet",
                        color = DiscordTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap 'New Key' to save persistent variables, guild configs, or JSON documents.",
                        color = DiscordTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(storageList, key = { it.id }) { item ->
                    StorageItemCard(
                        item = item,
                        onDelete = { viewModel.deleteStorageEntry(item.id) }
                    )
                }
            }
        }
    }

    // Add Key-Value Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text("Add Storage Key", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Key Name (e.g. guild_settings, xp_table)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type Picker
                    Box {
                        Surface(
                            color = DiscordDarker,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { typeDropdownOpen = true }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Type: $selectedType", color = DiscordTextPrimary, fontSize = 13.sp)
                                Text("▼", color = DiscordTextSecondary, fontSize = 10.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = typeDropdownOpen,
                            onDismissRequest = { typeDropdownOpen = false },
                            modifier = Modifier.background(DiscordDarker)
                        ) {
                            listOf("STRING", "JSON", "NUMBER", "BOOLEAN").forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t, color = DiscordTextPrimary) },
                                    onClick = {
                                        selectedType = t
                                        typeDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = { Text(if (selectedType == "JSON") "JSON Object / Array" else "Value") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        minLines = if (selectedType == "JSON") 3 else 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyInput.isNotBlank()) {
                            viewModel.addStorageEntry(keyInput.trim(), valueInput, selectedType)
                            keyInput = ""
                            valueInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }
}

@Composable
fun StorageItemCard(item: BotKeyValue, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DiscordDarker),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.storageKey,
                        color = DiscordTextPrimary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when (item.valueType) {
                            "JSON" -> DiscordYellow.copy(alpha = 0.2f)
                            "NUMBER" -> DiscordGreen.copy(alpha = 0.2f)
                            "BOOLEAN" -> DiscordBlurple.copy(alpha = 0.2f)
                            else -> DiscordElevated
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.valueType,
                            color = when (item.valueType) {
                                "JSON" -> DiscordYellow
                                "NUMBER" -> DiscordGreen
                                "BOOLEAN" -> DiscordBlurple
                                else -> DiscordTextSecondary
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.storageValue,
                    color = DiscordTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 3
                )
            }

            IconButton(onClick = onDelete) {
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
