package com.example.ui.install

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allPackages by PackageManager.allPackages.collectAsState()
    val installedSet by PackageManager.installedPackages.collectAsState()
    val downloadingProgress by PackageManager.downloadingProgress.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PackageCategory?>(null) }
    val savedSizeMb = remember(installedSet, allPackages) { PackageManager.calculateSavedApkSizeMb() }

    // Custom package input states
    var customPkgName by remember { mutableStateOf("") }
    var selectedManager by remember { mutableStateOf("npm") }
    var isInstallingCustom by remember { mutableStateOf(false) }

    val filteredPackages = remember(allPackages, selectedCategory, searchQuery) {
        allPackages.filter { pkg ->
            val matchesCategory = selectedCategory == null || pkg.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    pkg.name.contains(searchQuery, ignoreCase = true) ||
                    pkg.description.contains(searchQuery, ignoreCase = true) ||
                    pkg.commands.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .padding(14.dp)
    ) {
        // Top Header
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
                        text = "Packages & Runtime Hub",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Fast package installer with 1-click import into code editor",
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

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Quick Installer Card
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
                        text = "⚡ Install Any Package",
                        color = DiscordTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Manager switcher chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("npm", "pip", "bun").forEach { mgr ->
                            val isSelected = selectedManager == mgr
                            Surface(
                                color = if (isSelected) DiscordBlurple else DiscordElevated,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable { selectedManager = mgr }
                            ) {
                                Text(
                                    text = mgr,
                                    color = if (isSelected) Color.White else DiscordTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customPkgName,
                        onValueChange = { customPkgName = it },
                        placeholder = {
                            Text(
                                if (selectedManager == "pip") "e.g. discord.py, requests..." else "e.g. chalk, express, moment...",
                                color = DiscordTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = TextStyle(
                            color = DiscordTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (customPkgName.isNotBlank() && !isInstallingCustom) {
                                isInstallingCustom = true
                                coroutineScope.launch {
                                    val installed = PackageManager.installCustomPackage(
                                        customPkgName,
                                        selectedManager
                                    )
                                    viewModel.executeQuickTerminalCommand("$selectedManager install ${installed.name}")
                                    Toast.makeText(context, "Installed ${installed.name} successfully!", Toast.LENGTH_SHORT).show()
                                    customPkgName = ""
                                    isInstallingCustom = false
                                }
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DiscordHover,
                            focusedContainerColor = DiscordBackground,
                            unfocusedContainerColor = DiscordBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("input_custom_package")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (customPkgName.isNotBlank() && !isInstallingCustom) {
                                isInstallingCustom = true
                                coroutineScope.launch {
                                    val installed = PackageManager.installCustomPackage(
                                        customPkgName,
                                        selectedManager
                                    )
                                    viewModel.executeQuickTerminalCommand("$selectedManager install ${installed.name}")
                                    Toast.makeText(context, "Installed ${installed.name} successfully!", Toast.LENGTH_SHORT).show()
                                    customPkgName = ""
                                    isInstallingCustom = false
                                }
                            }
                        },
                        enabled = customPkgName.isNotBlank() && !isInstallingCustom,
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_install_custom_pkg")
                    ) {
                        if (isInstallingCustom) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search packages, SDKs, database drivers...", color = DiscordTextMuted, fontSize = 12.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = DiscordTextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = DiscordTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(color = DiscordTextPrimary, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DiscordBlurple,
                unfocusedBorderColor = DiscordElevated,
                focusedContainerColor = DiscordDarker,
                unfocusedContainerColor = DiscordDarker
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("search_package_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

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
                    text = "All (${allPackages.size})",
                    color = if (allSelected) Color.White else DiscordTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Package Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPackages, key = { it.id }) { pkg ->
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
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (pkg.category) {
                                                PackageCategory.LANGUAGE -> Icons.Default.Terminal
                                                PackageCategory.DISCORD_SDK -> Icons.Default.Code
                                                PackageCategory.UTILITY -> Icons.Default.AutoFixHigh
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
                                        text = "${pkg.category.label} • %.1f MB • ${pkg.author}".format(pkg.sizeMb),
                                        color = DiscordTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Action Button (Install / Uninstall / Installed badge)
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
                                            onClick = {
                                                PackageManager.uninstallPackage(pkg)
                                                viewModel.executeQuickTerminalCommand("npm uninstall ${pkg.name}")
                                            },
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
                                            PackageManager.installPackage(pkg) { _, status ->
                                                // Live feedback
                                            }
                                            viewModel.executeQuickTerminalCommand("npm install ${pkg.name}")
                                            Toast.makeText(context, "Installed ${pkg.name} successfully!", Toast.LENGTH_SHORT).show()
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

                        // Terminal commands & Insert Code button
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
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

                            // 1-Click Code Injection button for installed packages with import snippets
                            if (isInstalled && pkg.importSnippet.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = DiscordBlurple.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.insertPackageImport(pkg.importSnippet)
                                            Toast.makeText(context, "Added import to active editor file!", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("btn_insert_import_${pkg.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = "Insert import",
                                            tint = DiscordBlurple,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Insert Import",
                                            color = DiscordBlurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
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
}
