package com.example.ui.embedbuilder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.engine.DiscordSimulatorButton
import com.example.engine.DiscordSimulatorEmbed
import com.example.ui.AppTab
import com.example.ui.BotStudioViewModel
import com.example.ui.simulator.DiscordEmbedCard
import com.example.ui.theme.DiscordBackground
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordDarker
import com.example.ui.theme.DiscordElevated
import com.example.ui.theme.DiscordFuchsia
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordHover
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordYellow

@Composable
fun VisualEmbedDesignerScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val title by viewModel.embedTitle.collectAsState()
    val description by viewModel.embedDescription.collectAsState()
    val colorHex by viewModel.embedColorHex.collectAsState()
    val author by viewModel.embedAuthorName.collectAsState()
    val footer by viewModel.embedFooterText.collectAsState()
    val fields by viewModel.embedFields.collectAsState()
    val buttons by viewModel.embedButtons.collectAsState()
    val selectPlaceholder by viewModel.embedSelectMenuPlaceholder.collectAsState()
    val selectOptions by viewModel.embedSelectOptions.collectAsState()

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    var showAddSelectOptionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar with Generate Code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Discord Embed Designer",
                    color = DiscordTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Design rich embeds and export to ${project?.language ?: "code"}",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    val code = viewModel.generateEmbedCode(project?.language ?: "JavaScript")
                    viewModel.generatedCodeText.value = code
                    viewModel.showGeneratedCodeDialog.value = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_export_embed_code")
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = "Export Code", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Live Preview Box
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordDarker),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "LIVE DISCORD PREVIEW",
                    color = DiscordTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                DiscordEmbedCard(
                    embed = DiscordSimulatorEmbed(
                        title = title,
                        description = description,
                        colorHex = colorHex,
                        authorName = author,
                        fields = fields.map { it.name to it.value },
                        footerText = footer
                    )
                )

                if (buttons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        buttons.forEach { btn ->
                            val btnBg = when (btn.style) {
                                "SUCCESS" -> DiscordGreen
                                "DANGER" -> DiscordRed
                                "SECONDARY" -> DiscordHover
                                else -> DiscordBlurple
                            }
                            val btnText = if (btn.style == "SUCCESS") Color.Black else Color.White
                            Surface(
                                color = btnBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (btn.emoji.isNotBlank()) {
                                        Text(btn.emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = btn.label,
                                        color = btnText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DiscordElevated,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectPlaceholder,
                                color = DiscordTextSecondary,
                                fontSize = 12.sp
                            )
                            Text("▼", color = DiscordTextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Color Presets Row
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Embed Accent Color ($colorHex)",
                    color = DiscordTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val presets = listOf(
                        "#5865F2" to DiscordBlurple,
                        "#57F287" to DiscordGreen,
                        "#FEE75C" to DiscordYellow,
                        "#EB459E" to DiscordFuchsia,
                        "#ED4245" to DiscordRed,
                        "#1E1F22" to DiscordDarker
                    )
                    presets.forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (colorHex.equals(hex, ignoreCase = true)) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setEmbedColor(hex) }
                        )
                    }
                }
            }
        }

        // Basic Info Inputs
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.setEmbedTitle(it) },
                    label = { Text("Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_embed_title")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.setEmbedDescription(it) },
                    label = { Text("Description") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { viewModel.setEmbedAuthor(it) },
                        label = { Text("Author Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = footer,
                        onValueChange = { viewModel.setEmbedFooter(it) },
                        label = { Text("Footer Text") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Fields Manager
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fields (${fields.size})",
                        color = DiscordTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddFieldDialog = true },
                        modifier = Modifier.testTag("btn_add_embed_field")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Field", color = DiscordBlurple, fontSize = 12.sp)
                    }
                }

                fields.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(field.name, color = DiscordTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(field.value, color = DiscordTextSecondary, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.removeEmbedField(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiscordRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Buttons Manager
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Action Buttons (${buttons.size})",
                        color = DiscordTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddButtonDialog = true },
                        modifier = Modifier.testTag("btn_add_embed_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Button", color = DiscordBlurple, fontSize = 12.sp)
                    }
                }

                buttons.forEachIndexed { index, btn ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${btn.emoji} ${btn.label} (${btn.style})",
                            color = DiscordTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { viewModel.removeEmbedButton(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiscordRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Dropdown Select Menu Manager
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Drop-down Menu Options (${selectOptions.size})",
                        color = DiscordTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddSelectOptionDialog = true },
                        modifier = Modifier.testTag("btn_add_embed_select_option")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option", color = DiscordBlurple, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = selectPlaceholder,
                    onValueChange = { viewModel.setEmbedSelectPlaceholder(it) },
                    label = { Text("Menu Placeholder Text") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                selectOptions.forEachIndexed { index, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${opt.emoji} ${opt.label} [${opt.value}]",
                                color = DiscordTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (opt.description.isNotBlank()) {
                                Text(opt.description, color = DiscordTextSecondary, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.removeEmbedSelectOption(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiscordRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Field Dialog
    if (showAddFieldDialog) {
        var fieldName by remember { mutableStateOf("") }
        var fieldValue by remember { mutableStateOf("") }
        var isInline by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Add Embed Field", color = DiscordTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = { Text("Field Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )
                    OutlinedTextField(
                        value = fieldValue,
                        onValueChange = { fieldValue = it },
                        label = { Text("Field Value") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Inline field", color = DiscordTextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isInline,
                            onCheckedChange = { isInline = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DiscordBlurple)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fieldName.isNotBlank() && fieldValue.isNotBlank()) {
                            viewModel.addEmbedField(fieldName, fieldValue, isInline)
                            showAddFieldDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }

    // Add Button Dialog
    if (showAddButtonDialog) {
        var btnLabel by remember { mutableStateOf("") }
        var btnEmoji by remember { mutableStateOf("⭐") }
        var btnStyle by remember { mutableStateOf("PRIMARY") }

        AlertDialog(
            onDismissRequest = { showAddButtonDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Add Action Button", color = DiscordTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = btnLabel,
                        onValueChange = { btnLabel = it },
                        label = { Text("Button Label") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )
                    OutlinedTextField(
                        value = btnEmoji,
                        onValueChange = { btnEmoji = it },
                        label = { Text("Emoji") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary)
                    )
                    Text("Style:", color = DiscordTextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PRIMARY", "SUCCESS", "DANGER", "SECONDARY").forEach { style ->
                            Surface(
                                color = if (btnStyle == style) DiscordBlurple else DiscordHover,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable { btnStyle = style }
                            ) {
                                Text(
                                    text = style,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (btnLabel.isNotBlank()) {
                            viewModel.addEmbedButton(btnLabel, btnStyle, btnEmoji)
                            showAddButtonDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddButtonDialog = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }

    // Add Select Menu Option Dialog
    if (showAddSelectOptionDialog) {
        var optLabel by remember { mutableStateOf("") }
        var optValue by remember { mutableStateOf("") }
        var optDesc by remember { mutableStateOf("") }
        var optEmoji by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSelectOptionDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Add Drop-down Option", color = DiscordTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = optLabel,
                        onValueChange = { optLabel = it },
                        label = { Text("Option Label (e.g. Roles, Verify)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = optValue,
                        onValueChange = { optValue = it },
                        label = { Text("Value / Custom ID (e.g. opt_roles)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = optDesc,
                        onValueChange = { optDesc = it },
                        label = { Text("Description (Optional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = optEmoji,
                        onValueChange = { optEmoji = it },
                        label = { Text("Emoji (e.g. ⭐, 🛡️, ⚙️)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (optLabel.isNotBlank()) {
                            val value = if (optValue.isNotBlank()) optValue.trim() else optLabel.lowercase().replace(" ", "_")
                            viewModel.addEmbedSelectOption(optLabel.trim(), value, optDesc.trim(), optEmoji.trim())
                            showAddSelectOptionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Add Option")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSelectOptionDialog = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }
}
