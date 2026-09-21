package com.example.ui.storage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.platform.LocalContext
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
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AdvancedStorageScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val storageList by viewModel.storageEntries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("ALL") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("STRING") }
    var typeDropdownOpen by remember { mutableStateOf(false) }

    val filteredList = storageList.filter { item ->
        val matchesQuery = searchQuery.isBlank() ||
                item.storageKey.contains(searchQuery, ignoreCase = true) ||
                item.storageValue.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedFilterType == "ALL" || item.valueType == selectedFilterType
        matchesQuery && matchesType
    }

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
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Database",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage & Database Manager",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${storageList.size} keys • Room SQLite backed datastore for ${project?.name ?: "bot"}",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Export JSON
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export", tint = DiscordTextSecondary, modifier = Modifier.size(18.dp))
                }

                // Import JSON
                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Import", tint = DiscordTextSecondary, modifier = Modifier.size(18.dp))
                }

                // Add Key Button
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_add_storage_entry")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search & Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter keys or values...", color = DiscordTextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DiscordTextSecondary, modifier = Modifier.size(16.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DiscordBlurple,
                    unfocusedBorderColor = DiscordHover,
                    focusedTextColor = DiscordTextPrimary,
                    unfocusedTextColor = DiscordTextPrimary,
                    focusedContainerColor = DiscordDarker,
                    unfocusedContainerColor = DiscordDarker
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Clear all button if has entries
            if (storageList.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clear All", tint = DiscordRed, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "STRING", "JSON", "NUMBER", "BOOLEAN").forEach { type ->
                val isSelected = selectedFilterType == type
                Surface(
                    color = if (isSelected) DiscordBlurple else DiscordSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { selectedFilterType = type }
                ) {
                    Text(
                        text = type,
                        color = if (isSelected) Color.White else DiscordTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Storage Entries List
        if (filteredList.isEmpty()) {
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
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching keys found" else "No storage keys created yet",
                        color = DiscordTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use '+ New Key' or import JSON to persist server configs, economy balances, or level XP.",
                        color = DiscordTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    StorageItemCard(
                        item = item,
                        onDelete = { viewModel.deleteStorageEntry(item.id) },
                        onCopy = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Key Value", item.storageValue))
                            Toast.makeText(context, "Copied value for ${item.storageKey}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Add Key Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DiscordSurface,
            title = {
                Text("Add Storage Key", color = DiscordTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Key Name (e.g. guild_settings, user_xp)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

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
                                Text("Data Type: $selectedType", color = DiscordTextPrimary, fontSize = 13.sp)
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
                        label = { Text(if (selectedType == "JSON") "JSON Payload" else "Stored Value") },
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
                            Toast.makeText(context, "Saved key into datastore", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Save Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        val exportJson = remember(storageList) {
            val root = JSONObject()
            storageList.forEach {
                val sub = JSONObject()
                sub.put("value", it.storageValue)
                sub.put("type", it.valueType)
                root.put(it.storageKey, sub)
            }
            root.toString(2)
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Export Storage JSON", color = DiscordTextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Full snapshot of your bot's persistent database (${storageList.size} keys):",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DiscordDarker),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(text = exportJson, color = DiscordGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Exported Storage", exportJson))
                        showExportDialog = false
                        Toast.makeText(context, "Storage JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = DiscordTextMuted)
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Import Database Keys", color = DiscordTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a JSON object with key-value pairs to batch insert into Room:",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        placeholder = { Text("{\n  \"welcome_msg\": \"Hello!\",\n  \"starting_balance\": 500\n}", color = DiscordTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary,
                            focusedBorderColor = DiscordBlurple
                        ),
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val json = JSONObject(importInput)
                            var imported = 0
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val raw = json.get(k)
                                val (valStr, typeStr) = when (raw) {
                                    is JSONObject -> raw.optString("value", raw.toString()) to raw.optString("type", "JSON")
                                    is Number -> raw.toString() to "NUMBER"
                                    is Boolean -> raw.toString() to "BOOLEAN"
                                    else -> raw.toString() to "STRING"
                                }
                                viewModel.addStorageEntry(k, valStr, typeStr)
                                imported++
                            }
                            showImportDialog = false
                            Toast.makeText(context, "Imported $imported keys successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Invalid JSON: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Import Keys")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Clear All Storage?", color = DiscordRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all ${storageList.size} stored keys and variables for this bot.", color = DiscordTextPrimary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearStorage()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Database cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordRed)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }
}

@Composable
fun StorageItemCard(item: BotKeyValue, onDelete: () -> Unit, onCopy: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DiscordDarker),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.storageKey,
                        color = DiscordTextPrimary,
                        fontSize = 13.sp,
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
                    fontSize = 11.sp,
                    maxLines = 3
                )
            }

            Row {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = DiscordTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DiscordRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
