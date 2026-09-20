package com.example.ui.install

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packages.InstallablePackage
import com.example.packages.PackageCategory
import com.example.packages.PackageManager
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

@Composable
fun PackageInstallScreen(viewModel: BotStudioViewModel) {
    val installedSet by PackageManager.installedPackages.collectAsState()
    val downloadingProgress by PackageManager.downloadingProgress.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf<PackageCategory?>(null) }
    val savedSizeMb = remember(installedSet) { PackageManager.calculateSavedApkSizeMb() }

    val filteredPackages = remember(selectedCategory) {
        if (selectedCategory == null) PackageManager.AVAILABLE_PACKAGES
        else PackageManager.AVAILABLE_PACKAGES.filter { it.category == selectedCategory }
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
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Package Manager",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extensions & Language Hub",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Modular on-demand runtimes to keep base APK slim and lightning fast",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = DiscordGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = DiscordGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "-%.1f MB APK".format(savedSizeMb),
                        color = DiscordGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allSelected = selectedCategory == null
            Surface(
                color = if (allSelected) DiscordBlurple else DiscordSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable { selectedCategory = null }
                    .testTag("filter_all")
            ) {
                Text(
                    text = "All Packages (${PackageManager.AVAILABLE_PACKAGES.size})",
                    color = if (allSelected) Color.White else DiscordTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            PackageCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    color = if (isSelected) DiscordBlurple else DiscordSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { selectedCategory = cat }
                        .testTag("filter_${cat.name.lowercase()}")
                ) {
                    Text(
                        text = cat.label,
                        color = if (isSelected) Color.White else DiscordTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Package Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPackages) { pkg ->
                val isInstalled = installedSet.contains(pkg.id)
                val progress = downloadingProgress[pkg.id]

                Card(
                    colors = CardDefaults.cardColors(containerColor = DiscordSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pkg_card_${pkg.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = if (isInstalled) DiscordGreen.copy(alpha = 0.2f) else DiscordElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (pkg.category) {
                                                PackageCategory.LANGUAGE -> Icons.Default.Terminal
                                                PackageCategory.DISCORD_SDK -> Icons.Default.Speed
                                                PackageCategory.EXTENSION -> Icons.Default.Extension
                                                PackageCategory.DATABASE -> Icons.Default.Storage
                                            },
                                            contentDescription = pkg.name,
                                            tint = if (isInstalled) DiscordGreen else DiscordTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pkg.name,
                                            color = DiscordTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "v${pkg.version}",
                                            color = DiscordTextMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "${pkg.category.label} • %.1f MB".format(pkg.sizeMb),
                                        color = DiscordTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Action Button (Install / Uninstall / Installed)
                            if (progress != null) {
                                Surface(
                                    color = DiscordBlurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        color = DiscordBlurple,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            } else if (isInstalled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = DiscordGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Installed",
                                                tint = DiscordGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (pkg.isCore) "Core" else "Active",
                                                color = DiscordGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (!pkg.isCore) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { PackageManager.uninstallPackage(pkg) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Uninstall",
                                                tint = DiscordTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            PackageManager.installPackage(pkg)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Install",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Install",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pkg.description,
                            color = DiscordTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        if (progress != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = DiscordBlurple,
                                trackColor = DiscordDarker,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }

                        // Terminal commands provided
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            pkg.commands.forEach { cmd ->
                                Surface(
                                    color = DiscordDarker,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cmd,
                                        color = DiscordTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
