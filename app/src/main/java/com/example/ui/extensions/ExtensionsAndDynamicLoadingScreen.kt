package com.example.ui.extensions

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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.engine.DevicePerformanceTier
import com.example.engine.DynamicLoadingService
import com.example.engine.DynamicModule
import com.example.engine.DynamicModuleStatus
import com.example.engine.SystemDeviceOptimizer
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
fun ExtensionsAndDynamicLoadingScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val modules by DynamicLoadingService.modules.collectAsState()
    val deviceSettings by SystemDeviceOptimizer.settings.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Extensions",
                            tint = DiscordBlurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extensions & Dynamic Loader",
                            color = DiscordTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Load on-demand modules to keep low-end & high-end devices smooth",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Performance Tier & Memory Optimizer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DiscordSurface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = DiscordGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Performance Tier: ${deviceSettings.tier.name}",
                                color = DiscordTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = when (deviceSettings.tier) {
                                DevicePerformanceTier.LOW_END -> DiscordRed.copy(alpha = 0.2f)
                                DevicePerformanceTier.MID_RANGE -> DiscordYellow.copy(alpha = 0.2f)
                                DevicePerformanceTier.HIGH_END -> DiscordGreen.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${deviceSettings.availableRamMb} MB Free / ${deviceSettings.totalRamMb} MB",
                                color = when (deviceSettings.tier) {
                                    DevicePerformanceTier.LOW_END -> DiscordRed
                                    DevicePerformanceTier.MID_RANGE -> DiscordYellow
                                    DevicePerformanceTier.HIGH_END -> DiscordGreen
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tier selection chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DevicePerformanceTier.values().forEach { tier ->
                            val isSelected = deviceSettings.tier == tier
                            Surface(
                                color = if (isSelected) DiscordBlurple else DiscordDarker,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { SystemDeviceOptimizer.setPerformanceTier(tier) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tier.name.replace("_", " "),
                                        color = if (isSelected) Color.White else DiscordTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (tier) {
                                            DevicePerformanceTier.LOW_END -> "Battery & RAM Saver"
                                            DevicePerformanceTier.MID_RANGE -> "Balanced 60 FPS"
                                            DevicePerformanceTier.HIGH_END -> "Full Graphics & AST"
                                        },
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else DiscordTextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Free Memory Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dynamic Heap: ${DynamicLoadingService.getTotalAllocatedMemoryKb() / 1024} MB in active modules",
                                color = DiscordTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                val freed = DynamicLoadingService.unloadInactiveModulesToFreeRam()
                                Toast.makeText(context, "Purged $freed idle module(s) from memory", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordElevated),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(14.dp), tint = DiscordGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purge RAM Cache", fontSize = 11.sp, color = DiscordTextPrimary)
                        }
                    }
                }
            }
        }

        // On-Demand Modules List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DYNAMIC BOT EXTENSIONS (${modules.count { it.status == DynamicModuleStatus.ACTIVE }} LOADED)",
                    color = DiscordTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(modules) { mod ->
            DynamicModuleCard(
                module = mod,
                onLoad = { DynamicLoadingService.loadModule(mod.id) },
                onUnload = { DynamicLoadingService.unloadModule(mod.id) }
            )
        }
    }
}

@Composable
fun DynamicModuleCard(
    module: DynamicModule,
    onLoad: () -> Unit,
    onUnload: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                when (module.status) {
                                    DynamicModuleStatus.ACTIVE -> DiscordGreen
                                    DynamicModuleStatus.DOWNLOADING, DynamicModuleStatus.LOADING -> DiscordYellow
                                    DynamicModuleStatus.UNLOADED, DynamicModuleStatus.FAILED -> DiscordTextMuted
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = module.name,
                        color = DiscordTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = DiscordElevated,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${module.sizeKb} KB • RAM: ${module.memoryFootprintKb / 1024} MB",
                        color = DiscordTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = module.description,
                color = DiscordTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                module.exportedApis.forEach { api ->
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = api,
                            color = DiscordGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (module.status == DynamicModuleStatus.DOWNLOADING || module.status == DynamicModuleStatus.LOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = module.progress,
                    color = DiscordBlurple,
                    trackColor = DiscordDarker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (module.status == DynamicModuleStatus.ACTIVE) {
                    Button(
                        onClick = onUnload,
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordDarker),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(12.dp), tint = DiscordRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unload (Free RAM)", fontSize = 11.sp, color = DiscordRed)
                    }
                } else {
                    Button(
                        onClick = onLoad,
                        enabled = module.status != DynamicModuleStatus.DOWNLOADING && module.status != DynamicModuleStatus.LOADING,
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        if (module.status == DynamicModuleStatus.DOWNLOADING || module.status == DynamicModuleStatus.LOADING) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loading...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dynamically Load", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
