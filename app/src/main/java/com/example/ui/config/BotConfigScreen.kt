package com.example.ui.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun BotConfigScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()

    var name by remember(project) { mutableStateOf(project?.name ?: "") }
    var prefix by remember(project) { mutableStateOf(project?.prefix ?: "!") }
    var clientId by remember(project) { mutableStateOf(project?.clientId ?: "") }
    var token by remember(project) { mutableStateOf(project?.botToken ?: "") }
    var showToken by remember { mutableStateOf(false) }

    var status by remember(project) { mutableStateOf(project?.status ?: "Online") }
    var activityType by remember(project) { mutableStateOf(project?.activityType ?: "PLAYING") }
    var activityText by remember(project) { mutableStateOf(project?.activityText ?: "") }

    var intentMessageContent by remember(project) { mutableStateOf(project?.intentMessageContent ?: true) }
    var intentGuildMembers by remember(project) { mutableStateOf(project?.intentGuildMembers ?: true) }
    var intentPresences by remember(project) { mutableStateOf(project?.intentPresences ?: false) }

    // Permissions Calculator
    var permAdmin by remember { mutableStateOf(false) }
    var permManageServer by remember { mutableStateOf(false) }
    var permBanMembers by remember { mutableStateOf(true) }
    var permKickMembers by remember { mutableStateOf(true) }
    var permSendMessages by remember { mutableStateOf(true) }
    var permEmbedLinks by remember { mutableStateOf(true) }
    var permAttachFiles by remember { mutableStateOf(true) }
    var permReadHistory by remember { mutableStateOf(true) }
    var permSlashCommands by remember { mutableStateOf(true) }

    val calculatedPermissions = remember(
        permAdmin, permManageServer, permBanMembers, permKickMembers,
        permSendMessages, permEmbedLinks, permAttachFiles, permReadHistory, permSlashCommands
    ) {
        if (permAdmin) 8L
        else {
            var p = 0L
            if (permManageServer) p = p or 0x00000020L
            if (permBanMembers) p = p or 0x00000004L
            if (permKickMembers) p = p or 0x00000002L
            if (permSendMessages) p = p or 0x00000800L
            if (permEmbedLinks) p = p or 0x00004000L
            if (permAttachFiles) p = p or 0x00008000L
            if (permReadHistory) p = p or 0x00010000L
            if (permSlashCommands) p = p or 0x80000000L
            p
        }
    }

    val inviteUrl = remember(clientId, calculatedPermissions) {
        val cid = clientId.ifBlank { "118923456789012345" }
        "https://discord.com/api/oauth2/authorize?client_id=$cid&permissions=$calculatedPermissions&scope=bot%20applications.commands"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Save Settings Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bot Settings & Intents",
                    color = DiscordTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage credentials, presence and gateway permissions",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    viewModel.updateProjectSettings(
                        name = name,
                        prefix = prefix,
                        status = status,
                        activityType = activityType,
                        activityText = activityText,
                        token = token,
                        clientId = clientId,
                        intentMessageContent = intentMessageContent,
                        intentGuildMembers = intentGuildMembers,
                        intentPresences = intentPresences
                    )
                    Toast.makeText(context, "Bot configuration saved!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DiscordGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_bot_settings")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Quick Navigation to AI Models & Keys Pool
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setTab(com.example.ui.AppTab.API_KEYS) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Keys",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Models & API Key Pool",
                            color = DiscordTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Configure Gemini, OpenAI, Claude, Groq & auto-failover on quota limits",
                            color = DiscordTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Surface(
                    color = DiscordElevated,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Manage →",
                        color = DiscordTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Open Collaboration & Permissions Freedom Card
        var unrestrictedAccessEnabled by remember { mutableStateOf(true) }
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Access",
                            tint = DiscordGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Access & Permission Freedom",
                                color = DiscordTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Anyone can read, edit & manage codes and Gradle freely",
                                color = DiscordTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Surface(
                        color = DiscordGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "UNRESTRICTED",
                            color = DiscordGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DiscordDarker, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Public Access to Codes & Gradle",
                            color = DiscordTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Unrestricted public access is active. Anyone can edit bot code, create files, manage packages, and edit Gradle scripts without permission barriers.",
                            color = DiscordTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = unrestrictedAccessEnabled,
                        onCheckedChange = {
                            unrestrictedAccessEnabled = it
                            Toast.makeText(context, if (it) "Unrestricted access enabled: Anyone can edit codes & Gradle!" else "Public access restricted", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DiscordGreen
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setTab(com.example.ui.AppTab.EDITOR) },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordHover),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = DiscordTextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Bot Codes", color = DiscordTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { viewModel.setTab(com.example.ui.AppTab.GRADLE) },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordHover),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp), tint = DiscordBlurple)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Gradle", color = DiscordTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Bot Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Bot Identity", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Bot Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        modifier = Modifier.weight(2f)
                    )

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefix") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedTextColor = DiscordTextPrimary,
                            unfocusedTextColor = DiscordTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Application / Client ID") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bot Token Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = DiscordYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Discord Bot Token", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = "Keep this token secret! It grants full access to your Discord bot application.",
                    color = DiscordTextMuted,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showToken) "Hide" else "Show",
                                tint = DiscordTextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordYellow,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Presence & Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Presence & Rich Status", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                // Status selectors
                Text("Online Status:", color = DiscordTextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Online", "Idle", "DND", "Invisible").forEach { s ->
                        Surface(
                            color = if (status == s) DiscordBlurple else DiscordElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { status = s }
                        ) {
                            Text(
                                text = s,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Activity Type selectors
                Text("Activity Type:", color = DiscordTextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING").forEach { act ->
                        Surface(
                            color = if (activityType == act) DiscordBlurple else DiscordElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { activityType = act }
                        ) {
                            Text(
                                text = act,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = activityText,
                    onValueChange = { activityText = it },
                    label = { Text("Status Text (e.g. /help | v2.0)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedTextColor = DiscordTextPrimary,
                        unfocusedTextColor = DiscordTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Privileged Gateway Intents
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = DiscordGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Privileged Gateway Intents", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = "Ensure these intents are enabled in the Discord Developer Portal for your application.",
                    color = DiscordTextMuted,
                    fontSize = 11.sp
                )

                // Message Content Intent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Message Content Intent", color = DiscordTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Required to read user message content for prefix commands", color = DiscordTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = intentMessageContent,
                        onCheckedChange = { intentMessageContent = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DiscordGreen)
                    )
                }

                // Server Members Intent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Server Members Intent", color = DiscordTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Required for member join/leave events and role tracking", color = DiscordTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = intentGuildMembers,
                        onCheckedChange = { intentGuildMembers = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DiscordGreen)
                    )
                }

                // Presence Intent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Presence Intent", color = DiscordTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Required to track user activities and online/offline status", color = DiscordTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = intentPresences,
                        onCheckedChange = { intentPresences = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DiscordGreen)
                    )
                }
            }
        }

        // OAuth2 Bot Invite Link Generator
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bot Invite Link Generator", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = "Select required bot permissions to generate an authorization invite link:",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )

                // Permissions checkboxes grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PermCheckRow("Administrator (All Permissions)", permAdmin) { permAdmin = it }
                    PermCheckRow("Manage Server", permManageServer) { permManageServer = it }
                    PermCheckRow("Ban Members", permBanMembers) { permBanMembers = it }
                    PermCheckRow("Kick Members", permKickMembers) { permKickMembers = it }
                    PermCheckRow("Send Messages", permSendMessages) { permSendMessages = it }
                    PermCheckRow("Embed Links", permEmbedLinks) { permEmbedLinks = it }
                    PermCheckRow("Attach Files", permAttachFiles) { permAttachFiles = it }
                    PermCheckRow("Use Application (Slash) Commands", permSlashCommands) { permSlashCommands = it }
                }

                // Generated URL box
                Surface(
                    color = DiscordDarker,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = inviteUrl,
                            color = DiscordBlurple,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Discord Invite Link", inviteUrl))
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = DiscordTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = DiscordBlurple)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = DiscordTextPrimary, fontSize = 13.sp)
    }
}
