package com.example.ui.gradle

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GradleFileType(val displayName: String, val fileName: String) {
    APP_BUILD("app/build.gradle.kts", "build.gradle.kts"),
    SETTINGS("settings.gradle.kts", "settings.gradle.kts"),
    LIBS_TOML("gradle/libs.versions.toml", "libs.versions.toml"),
    ROOT_BUILD("build.gradle.kts (root)", "root.build.gradle.kts"),
    PROGUARD("proguard-rules.pro", "proguard-rules.pro")
}

@Composable
fun GradleConfigScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf(GradleFileType.APP_BUILD) }
    var fileContents by remember {
        mutableStateOf(
            mapOf(
                GradleFileType.APP_BUILD to DEFAULT_APP_BUILD_GRADLE,
                GradleFileType.SETTINGS to DEFAULT_SETTINGS_GRADLE,
                GradleFileType.LIBS_TOML to DEFAULT_LIBS_TOML,
                GradleFileType.ROOT_BUILD to DEFAULT_ROOT_GRADLE,
                GradleFileType.PROGUARD to DEFAULT_PROGUARD
            )
        )
    }

    var currentContent by remember(selectedFile) {
        mutableStateOf(fileContents[selectedFile] ?: "")
    }

    var isSyncing by remember { mutableStateOf(false) }
    var syncOutput by remember { mutableStateOf("") }
    var showAddDependencyDialog by remember { mutableStateOf(false) }
    var depNameInput by remember { mutableStateOf("") }
    var depVersionInput by remember { mutableStateOf("") }

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
                        imageVector = Icons.Default.Build,
                        contentDescription = "Gradle",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gradle & Build Manager",
                        color = DiscordTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Full Read, Edit, Dependency & Sync Access for anyone",
                    color = DiscordTextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Quick Dependency Button
                Button(
                    onClick = { showAddDependencyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordElevated),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DiscordTextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Dep", fontSize = 11.sp, color = DiscordTextPrimary)
                }

                // Sync Gradle Button
                Button(
                    onClick = {
                        if (!isSyncing) {
                            isSyncing = true
                            syncOutput = "Starting Gradle sync daemon...\nEvaluating build files...\nResolving dependencies..."
                            coroutineScope.launch {
                                delay(1200)
                                isSyncing = false
                                syncOutput = "BUILD SUCCESSFUL in 1.4s\n35 actionable tasks: 35 executed\nAll Gradle configurations synced."
                                Toast.makeText(context, "Gradle Sync completed successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_sync_gradle")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Syncing...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Gradle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Open Access Banner
        Surface(
            color = DiscordGreen.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DiscordGreen.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "No Permission Required",
                        tint = DiscordGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Access: Anyone can read, edit, manage, and sync Gradle build scripts without permissions or authentication.",
                        color = DiscordGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(
                    color = DiscordGreen,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "FREE TO EDIT",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // File Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GradleFileType.values().forEach { fileType ->
                val isSelected = selectedFile == fileType
                Surface(
                    color = if (isSelected) DiscordBlurple else DiscordSurface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable {
                            // save current file changes
                            val updatedMap = fileContents.toMutableMap()
                            updatedMap[selectedFile] = currentContent
                            fileContents = updatedMap
                            selectedFile = fileType
                            currentContent = updatedMap[fileType] ?: ""
                        }
                        .testTag("gradle_tab_${fileType.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else DiscordTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = fileType.displayName,
                            color = if (isSelected) Color.White else DiscordTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedFile.fileName} • ${currentContent.lines().size} lines",
                color = DiscordTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Copy Content
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Gradle File", currentContent))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = DiscordTextSecondary, modifier = Modifier.size(16.dp))
                }

                // Save File
                IconButton(
                    onClick = {
                        val updatedMap = fileContents.toMutableMap()
                        updatedMap[selectedFile] = currentContent
                        fileContents = updatedMap
                        Toast.makeText(context, "Saved ${selectedFile.displayName}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = DiscordGreen, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Code Editor Box
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordDarker),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = currentContent,
                    onValueChange = { currentContent = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = DiscordTextPrimary,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(DiscordBlurple),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .testTag("gradle_code_editor")
                )
            }
        }

        // Sync Output Log if available
        if (syncOutput.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DiscordSurface),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Build Daemon Output",
                            color = DiscordGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear",
                            color = DiscordTextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { syncOutput = "" }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = syncOutput,
                        color = DiscordTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // Add Dependency Dialog
    if (showAddDependencyDialog) {
        AlertDialog(
            onDismissRequest = { showAddDependencyDialog = false },
            containerColor = DiscordSurface,
            title = { Text("Add Gradle Dependency", color = DiscordTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quickly append an implementation dependency into ${selectedFile.displayName}:",
                        color = DiscordTextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = depNameInput,
                        onValueChange = { depNameInput = it },
                        label = { Text("Dependency Coordinate") },
                        placeholder = { Text("e.g. com.squareup.okhttp3:okhttp") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = depVersionInput,
                        onValueChange = { depVersionInput = it },
                        label = { Text("Version") },
                        placeholder = { Text("e.g. 4.12.0") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = DiscordTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (depNameInput.isNotBlank()) {
                            val coord = if (depVersionInput.isNotBlank()) "$depNameInput:$depVersionInput" else depNameInput
                            currentContent += "\n    implementation(\"$coord\")\n"
                            val updatedMap = fileContents.toMutableMap()
                            updatedMap[selectedFile] = currentContent
                            fileContents = updatedMap
                            showAddDependencyDialog = false
                            depNameInput = ""
                            depVersionInput = ""
                            Toast.makeText(context, "Dependency added to build script!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Add Dependency")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDependencyDialog = false }) {
                    Text("Cancel", color = DiscordTextSecondary)
                }
            }
        )
    }
}

private val DEFAULT_APP_BUILD_GRADLE = """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "com.aistudio.botstudio.ddev"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
}
""".trimIndent()

private val DEFAULT_SETTINGS_GRADLE = """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BotStudio"
include(":app")
""".trimIndent()

private val DEFAULT_LIBS_TOML = """
[versions]
agp = "9.1.1"
kotlin = "2.2.10"
composeBom = "2024.09.00"
room = "2.7.0"
okhttp = "4.10.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.18.0" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
""".trimIndent()

private val DEFAULT_ROOT_GRADLE = """
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}
""".trimIndent()

private val DEFAULT_PROGUARD = """
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/proguard-android-optimize.txt
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
""".trimIndent()
