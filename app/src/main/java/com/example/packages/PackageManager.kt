package com.example.packages

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PackageCategory(val label: String) {
    LANGUAGE("Languages & Runtimes"),
    DISCORD_SDK("Discord SDKs"),
    EXTENSION("IDE & Editor Extensions"),
    DATABASE("Database Drivers")
}

data class InstallablePackage(
    val id: String,
    val name: String,
    val version: String,
    val sizeMb: Double,
    val category: PackageCategory,
    val description: String,
    val iconName: String,
    val commands: List<String>,
    val author: String = "BotStudio Official",
    val isCore: Boolean = false
)

object PackageManager {

    val AVAILABLE_PACKAGES = listOf(
        // Core installed
        InstallablePackage(
            id = "runtime_nodejs",
            name = "Node.js v20.12 LTS",
            version = "20.12.2",
            sizeMb = 4.2,
            category = PackageCategory.LANGUAGE,
            description = "Standard JavaScript & TypeScript V8 execution runtime with npm.",
            iconName = "javascript",
            commands = listOf("node", "npm", "npx"),
            isCore = true
        ),
        InstallablePackage(
            id = "sdk_discordjs",
            name = "discord.js v14",
            version = "14.15.3",
            sizeMb = 2.1,
            category = PackageCategory.DISCORD_SDK,
            description = "Official modern Discord API wrapper for Node.js featuring Slash commands & EmbedBuilder.",
            iconName = "code",
            commands = listOf("const { Client } = require('discord.js')"),
            isCore = true
        ),

        // Optional Installable Runtimes (Reduces Base APK)
        InstallablePackage(
            id = "runtime_python",
            name = "Python 3.11 Runtime",
            version = "3.11.8",
            sizeMb = 5.8,
            category = PackageCategory.LANGUAGE,
            description = "Lightweight Python CPython interpreter with asyncio support for discord.py bots.",
            iconName = "terminal",
            commands = listOf("python3", "pip3", "python")
        ),
        InstallablePackage(
            id = "sdk_discordpy",
            name = "discord.py 2.3",
            version = "2.3.2",
            sizeMb = 1.8,
            category = PackageCategory.DISCORD_SDK,
            description = "Modern, easy to use, feature-rich async Discord API client for Python.",
            iconName = "code",
            commands = listOf("import discord", "@bot.tree.command")
        ),
        InstallablePackage(
            id = "runtime_bun",
            name = "Bun Fast JavaScript Runtime",
            version = "1.1.0",
            sizeMb = 6.4,
            category = PackageCategory.LANGUAGE,
            description = "Ultra-fast all-in-one JavaScript runtime, bundler, and package manager.",
            iconName = "speed",
            commands = listOf("bun run", "bun install", "bun test")
        ),
        InstallablePackage(
            id = "runtime_rust",
            name = "Rust Cargo toolchain",
            version = "1.77.1",
            sizeMb = 9.2,
            category = PackageCategory.LANGUAGE,
            description = "Rust compiler and cargo build system for blazing-fast serenity bots.",
            iconName = "memory",
            commands = listOf("cargo build", "cargo run")
        ),
        InstallablePackage(
            id = "runtime_go",
            name = "Go discordgo compiler",
            version = "1.22.1",
            sizeMb = 7.5,
            category = PackageCategory.LANGUAGE,
            description = "Concurrent Discord bot development with Go goroutines.",
            iconName = "code",
            commands = listOf("go run", "go build")
        ),

        // Extensions
        InstallablePackage(
            id = "ext_prettier",
            name = "Prettier Code Formatter",
            version = "3.2.5",
            sizeMb = 1.2,
            category = PackageCategory.EXTENSION,
            description = "Opinionated automatic code beautifier and indentation formatter.",
            iconName = "auto_fix_high",
            commands = listOf("format", "prettier")
        ),
        InstallablePackage(
            id = "ext_jest",
            name = "Jest & Pytest Runner",
            version = "29.7.0",
            sizeMb = 2.4,
            category = PackageCategory.EXTENSION,
            description = "Automated unit test suite runner for checking bot command logic.",
            iconName = "check_circle",
            commands = listOf("npm test", "pytest")
        ),
        InstallablePackage(
            id = "ext_database_drivers",
            name = "SQLite & Key-Value DB Engine",
            version = "3.45.0",
            sizeMb = 1.6,
            category = PackageCategory.DATABASE,
            description = "Embedded local database engine with persistent KV storage.",
            iconName = "storage",
            commands = listOf("sqlite3", "db.get()", "db.set()")
        )
    )

    // Set of installed package IDs
    private val _installedPackages = MutableStateFlow<Set<String>>(
        setOf("runtime_nodejs", "sdk_discordjs", "ext_jest", "ext_database_drivers")
    )
    val installedPackages: StateFlow<Set<String>> = _installedPackages.asStateFlow()

    // Map of packageId -> progress (0.0 to 1.0)
    private val _downloadingProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingProgress: StateFlow<Map<String, Float>> = _downloadingProgress.asStateFlow()

    fun isInstalled(packageId: String): Boolean = _installedPackages.value.contains(packageId)

    fun isLanguageInstalled(languageName: String): Boolean {
        return when (languageName.uppercase()) {
            "JAVASCRIPT" -> isInstalled("runtime_nodejs")
            "TYPESCRIPT" -> isInstalled("runtime_nodejs")
            "PYTHON" -> isInstalled("runtime_python")
            "RUST" -> isInstalled("runtime_rust")
            "GO" -> isInstalled("runtime_go")
            else -> true
        }
    }

    suspend fun installPackage(pkg: InstallablePackage, onProgress: (Float) -> Unit = {}) {
        val currentProgress = _downloadingProgress.value.toMutableMap()
        currentProgress[pkg.id] = 0.05f
        _downloadingProgress.value = currentProgress

        // Smooth simulated stream download
        for (step in 1..10) {
            kotlinx.coroutines.delay(120)
            val p = step / 10f
            currentProgress[pkg.id] = p
            _downloadingProgress.value = currentProgress.toMap()
            onProgress(p)
        }

        currentProgress.remove(pkg.id)
        _downloadingProgress.value = currentProgress

        val set = _installedPackages.value.toMutableSet()
        set.add(pkg.id)
        _installedPackages.value = set
    }

    fun uninstallPackage(pkg: InstallablePackage) {
        if (pkg.isCore) return
        val set = _installedPackages.value.toMutableSet()
        set.remove(pkg.id)
        _installedPackages.value = set
    }

    fun calculateSavedApkSizeMb(): Double {
        val uninstalled = AVAILABLE_PACKAGES.filter { !_installedPackages.value.contains(it.id) }
        return uninstalled.sumOf { it.sizeMb }
    }
}
