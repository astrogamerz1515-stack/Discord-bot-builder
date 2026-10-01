package com.example.ui.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CommandResponseOptimizer
import com.example.engine.CommandSyncMode
import com.example.engine.HostingRegion
import com.example.engine.InteractionTransportMode
import com.example.ui.BotStudioViewModel
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordDarker
import com.example.ui.theme.DiscordElevated
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordYellow

/**
 * Slow Command Responses Quick-Fix & Latency Optimizer UI:
 * Implements the full diagnosis checklist, latency meters, the #1 deferReply() fix,
 * hosting region selector, event loop monitor, cold start keep-alive, command sync toggle,
 * REST cache layer, and gateway vs webhook routing.
 */
@Composable
fun CommandResponseOptimizerCard(
    viewModel: BotStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val optSettings by CommandResponseOptimizer.settings.collectAsState()
    val project by viewModel.currentProject.collectAsState()

    var isExpanded by remember { mutableStateOf(true) }
    var showRegionDropdown by remember { mutableStateOf(false) }

    // 6-Point Diagnostic Checklist state
    val checklist = remember {
        mutableStateListOf(
            false, // 1. deferReply() called?
            true,  // 2. Hosting in US East Ashburn?
            false, // 3. Bot sleeping / cold start?
            false, // 4. Event loop blocked (>50ms)?
            true,  // 5. Commands registered per-guild?
            true   // 6. Unnecessary REST calls eliminated?
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DiscordSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DiscordElevated),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_command_response_optimizer")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title + Expand Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(DiscordYellow.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Optimizer",
                            tint = DiscordYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Command Speed & Latency Optimizer",
                            color = DiscordTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Fix slow responses, 3s timeouts & cold starts",
                            color = DiscordTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = DiscordTextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Live Latency Triple Meters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gateway Latency Meter
                        Surface(
                            color = DiscordDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DiscordElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "GATEWAY",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DiscordTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${optSettings.gatewayLatencyMs}ms",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (optSettings.gatewayLatencyMs < 60) DiscordGreen else DiscordYellow,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Heartbeat ACK",
                                    fontSize = 9.sp,
                                    color = DiscordTextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // REST Latency Meter
                        Surface(
                            color = DiscordDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DiscordElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "REST API",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = DiscordTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Test REST",
                                        tint = DiscordTextSecondary,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                project?.let {
                                                    CommandResponseOptimizer.measureRestLatency(it.botToken, coroutineScope)
                                                }
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${optSettings.restLatencyMs}ms",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (optSettings.restLatencyMs < 80) DiscordGreen else DiscordYellow,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "GET /users/@me",
                                    fontSize = 9.sp,
                                    color = DiscordTextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Interaction -> Response Latency Badge
                        Surface(
                            color = DiscordDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (optSettings.isInteractionFlaggedSlow) DiscordRed else DiscordElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "INTERACTION",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DiscordTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${optSettings.lastInteractionResponseMs}ms",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        optSettings.lastInteractionResponseMs < 200 -> DiscordGreen
                                        optSettings.lastInteractionResponseMs < 500 -> DiscordYellow
                                        else -> DiscordRed
                                    },
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    if (optSettings.isInteractionFlaggedSlow) "FLAGGED SLOW!" else "Response time",
                                    fontSize = 9.sp,
                                    fontWeight = if (optSettings.isInteractionFlaggedSlow) FontWeight.Bold else FontWeight.Normal,
                                    color = if (optSettings.isInteractionFlaggedSlow) DiscordRed else DiscordTextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 2. The #1 Fix: Immediate deferReply() / deferUpdate() Banner & Generator
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DiscordGreen.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DiscordGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "THE #1 FIX: Immediate deferReply()",
                                    color = DiscordGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Never execute database queries or REST calls before the first reply. Calling deferReply() immediately grants a full 15-minute execution window, eliminating the dreaded 3-second \"Application did not respond\" error.",
                                color = DiscordTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val lang = project?.language ?: "JavaScript"
                                        val template = CommandResponseOptimizer.getAutoDeferCodeTemplate(lang)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Defer Reply Template", template))
                                        Toast.makeText(context, "Copied #1 Fix template to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DiscordGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy #1 Fix Code", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // 3. Hosting Location Selector (US East Ashburn <50ms)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "HOSTING REGION",
                                color = DiscordTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Target: <50ms to Discord",
                                color = DiscordGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        Box {
                            Surface(
                                color = DiscordDarker,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, DiscordElevated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRegionDropdown = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            optSettings.selectedRegion.displayName,
                                            color = DiscordTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "Estimated Gateway Latency: ~${optSettings.selectedRegion.estimatedPingToDiscordMs}ms",
                                            color = if (optSettings.selectedRegion.isRecommended) DiscordGreen else DiscordTextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = DiscordTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showRegionDropdown,
                                onDismissRequest = { showRegionDropdown = false },
                                modifier = Modifier.background(DiscordDarker)
                            ) {
                                HostingRegion.values().forEach { region ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    region.displayName,
                                                    color = if (region.isRecommended) DiscordGreen else DiscordTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (region.isRecommended) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    "~${region.estimatedPingToDiscordMs}ms ${if (region.isRecommended) "(Discord Primary Cluster)" else ""}",
                                                    color = DiscordTextMuted,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        },
                                        onClick = {
                                            CommandResponseOptimizer.setHostingRegion(region)
                                            showRegionDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        if (optSettings.selectedRegion.warning != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "⚠️ ${optSettings.selectedRegion.warning}",
                                color = DiscordYellow,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 4. Event Loop Lag Monitor
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (optSettings.isEventLoopLagged) DiscordRed else DiscordElevated)
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
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = if (optSettings.isEventLoopLagged) DiscordRed else DiscordGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Event Loop Lag: ${optSettings.eventLoopLagMs}ms",
                                        color = if (optSettings.isEventLoopLagged) DiscordRed else DiscordTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    if (optSettings.isEventLoopLagged)
                                        "⚠️ EVENT LOOP BLOCKED (>50ms)! Replace readFileSync with fs.promises.readFile and cache parsed JSON."
                                    else
                                        "Optimal (<50ms). Non-blocking async event loop healthy.",
                                    color = if (optSettings.isEventLoopLagged) DiscordRed else DiscordTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 5. Cold Starts & Keep-Alive HTTP Server Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Cold Start Keep-Alive (Ping every 5 min)",
                                color = DiscordTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                optSettings.lastColdStartPingTime,
                                color = DiscordTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = optSettings.coldStartKeepAliveEnabled,
                            onCheckedChange = { CommandResponseOptimizer.toggleColdStartKeepAlive(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = DiscordGreen, checkedTrackColor = DiscordGreen.copy(alpha = 0.5f))
                        )
                    }

                    // 6. Command Sync Toggle (Per-Guild vs Global)
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DiscordElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Command Sync Mode: ${optSettings.syncMode.label}",
                                    color = DiscordTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Propagation: ${optSettings.syncMode.propagationDelay}",
                                    color = if (optSettings.syncMode.isInstant) DiscordGreen else DiscordYellow,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            TextButton(
                                onClick = {
                                    val next = if (optSettings.syncMode == CommandSyncMode.PER_GUILD) CommandSyncMode.GLOBAL else CommandSyncMode.PER_GUILD
                                    CommandResponseOptimizer.setSyncMode(next)
                                }
                            ) {
                                Text(
                                    if (optSettings.syncMode == CommandSyncMode.PER_GUILD) "Switch to Global" else "Switch to Instant Dev",
                                    fontSize = 11.sp,
                                    color = DiscordBlurple,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 7. In-Memory REST Cache Layer Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "In-Memory REST Cache (2m TTL)",
                                color = DiscordTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Caches users/guilds/channels. Uses .cache.get() instead of .fetch()",
                                color = DiscordTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = optSettings.restCacheEnabled,
                            onCheckedChange = { CommandResponseOptimizer.toggleRestCache(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = DiscordGreen, checkedTrackColor = DiscordGreen.copy(alpha = 0.5f))
                        )
                    }

                    // 8. Gateway Interactions vs HTTP Webhook Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Interaction Route: ${optSettings.transportMode.label}",
                                color = DiscordTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                optSettings.transportMode.description,
                                color = DiscordTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TextButton(
                            onClick = {
                                val next = if (optSettings.transportMode == InteractionTransportMode.GATEWAY)
                                    InteractionTransportMode.HTTP_WEBHOOK else InteractionTransportMode.GATEWAY
                                CommandResponseOptimizer.setTransportMode(next)
                            }
                        ) {
                            Text(
                                if (optSettings.transportMode == InteractionTransportMode.GATEWAY) "Use Webhook" else "Use Gateway",
                                fontSize = 11.sp,
                                color = DiscordBlurple,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 9. Six-Point Diagnostic Order Checklist
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DiscordElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "DIAGNOSTIC ORDER CHECKLIST",
                                color = DiscordTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val questions = listOf(
                                "1. Is deferReply() being called immediately before DB/REST?",
                                "2. Is bot hosted in US East Ashburn (<50ms to Discord)?",
                                "3. Is the bot sleeping? (Keep-alive ping every 5 min)",
                                "4. Is the event loop blocked? (Lag monitor <50ms)",
                                "5. Are dev commands registered per-guild for 0s propagation?",
                                "6. Are redundant REST calls cached (.cache.get() vs .fetch())?"
                            )

                            questions.forEachIndexed { i, q ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { checklist[i] = !checklist[i] }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checklist[i],
                                        onCheckedChange = { checklist[i] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = DiscordGreen,
                                            checkmarkColor = Color.Black,
                                            uncheckedColor = DiscordTextMuted
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = q,
                                        fontSize = 11.sp,
                                        color = if (checklist[i]) DiscordTextPrimary else DiscordTextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
