package com.example.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import com.example.ai.AiKeyManager
import com.example.ai.AiProvider
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
fun ApiKeysManagementScreen() {
    val context = LocalContext.current
    val keysList by AiKeyManager.configuredKeys.collectAsState()
    val selectedProvider by AiKeyManager.selectedProvider.collectAsState()
    val selectedModel by AiKeyManager.selectedModel.collectAsState()
    val autoFallback by AiKeyManager.autoFallbackEnabled.collectAsState()

    var editingProvider by remember { mutableStateOf<AiProvider?>(null) }
    var keyInputValue by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Keys",
                            tint = DiscordBlurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Models & API Keys Pool",
                            color = DiscordTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Multi-provider quota failover • Automatic fallback on limits",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Auto Failover Toggle
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DiscordSurface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Failover On Rate Limits & Quota Exhaustion",
                            color = DiscordTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "If one provider runs out of tokens or hits 429, instantly route requests to next active provider or offline bot generator.",
                            color = DiscordTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoFallback,
                        onCheckedChange = { AiKeyManager.setAutoFallback(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DiscordBlurple,
                            uncheckedTrackColor = DiscordDarker
                        )
                    )
                }
            }
        }

        // Provider Selector
        item {
            Text(
                text = "PRIMARY AI PROVIDER & ACTIVE MODEL",
                color = DiscordTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProvider.values().forEach { provider ->
                    val isChosen = selectedProvider == provider
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChosen) DiscordHover else DiscordSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AiKeyManager.selectProvider(provider)
                            }
                            .testTag("ai_provider_${provider.id}")
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
                                        text = provider.displayName,
                                        color = DiscordTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isChosen) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = DiscordBlurple,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Models: ${provider.availableModels.joinToString(", ")}",
                                    color = DiscordTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            val hasKey = AiKeyManager.getActiveKeyForProvider(provider) != null || provider == AiProvider.LOCAL_FALLBACK
                            Surface(
                                color = if (hasKey) DiscordGreen.copy(alpha = 0.2f) else DiscordRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (hasKey) "Ready" else "Missing Key",
                                    color = if (hasKey) DiscordGreen else DiscordRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Configure Keys List
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MANAGE YOUR API KEYS",
                color = DiscordTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(AiProvider.values().filter { it != AiProvider.LOCAL_FALLBACK }) { provider ->
            val existingKey = AiKeyManager.getActiveKeyForProvider(provider)
            val entry = keysList.find { it.provider == provider }

            Card(
                colors = CardDefaults.cardColors(containerColor = DiscordSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = provider.displayName,
                            color = DiscordTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (entry != null) {
                            Text(
                                text = "Status: ${entry.status} (${entry.requestsCount} calls)",
                                color = if (entry.status == "Quota Exceeded") DiscordYellow else DiscordGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (editingProvider == provider) {
                        // Edit input
                        OutlinedTextField(
                            value = keyInputValue,
                            onValueChange = { keyInputValue = it },
                            label = { Text("Paste ${provider.displayName} Key") },
                            placeholder = { Text("sk-...") },
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = DiscordTextSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DiscordTextPrimary,
                                unfocusedTextColor = DiscordTextPrimary,
                                focusedBorderColor = DiscordBlurple
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { editingProvider = null }) {
                                Text("Cancel", color = DiscordTextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (keyInputValue.isNotBlank()) {
                                        AiKeyManager.setKey(provider, keyInputValue)
                                        Toast.makeText(context, "Saved ${provider.displayName} key", Toast.LENGTH_SHORT).show()
                                    }
                                    editingProvider = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                            ) {
                                Text("Save Key")
                            }
                        }
                    } else {
                        // Display masked key
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (existingKey.isNullOrBlank()) "No key configured" else "Key: " + existingKey.take(5) + "••••••••" + existingKey.takeLast(4),
                                color = if (existingKey.isNullOrBlank()) DiscordTextMuted else DiscordTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )

                            Row {
                                Button(
                                    onClick = {
                                        editingProvider = provider
                                        keyInputValue = existingKey ?: ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DiscordElevated),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(if (existingKey.isNullOrBlank()) "Add Key" else "Change", fontSize = 11.sp, color = DiscordTextPrimary)
                                }

                                if (!existingKey.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            AiKeyManager.removeKey(provider)
                                            Toast.makeText(context, "Key removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiscordRed, modifier = Modifier.size(16.dp))
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
