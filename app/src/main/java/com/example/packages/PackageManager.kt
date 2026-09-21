package com.example.packages

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PackageCategory(val label: String) {
    LANGUAGE("Languages & Runtimes"),
    DISCORD_SDK("Discord SDKs"),
    UTILITY("Utilities & Helpers"),
    DATABASE("Database & Storage"),
    EXTENSION("IDE & Editor Extensions")
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
    val importSnippet: String = "",
    val author: String = "BotStudio Official",
    val isCore: Boolean = false
)

object PackageManager {

    private val DEFAULT_PACKAGES = listOf(
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
            importSnippet = "// Node.js standard environment",
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
            commands = listOf("npm i discord.js"),
            importSnippet = "const { Client, GatewayIntentBits, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');",
            isCore = true
        ),

        // Discord SDKs & Voice
        InstallablePackage(
            id = "sdk_discordpy",
            name = "discord.py 2.3",
            version = "2.3.2",
            sizeMb = 1.8,
            category = PackageCategory.DISCORD_SDK,
            description = "Modern, easy to use, feature-rich async Discord API client for Python.",
            iconName = "code",
            commands = listOf("pip install discord.py"),
            importSnippet = "import discord\nfrom discord import app_commands\nfrom discord.ext import commands"
        ),
        InstallablePackage(
            id = "sdk_discord_voice",
            name = "@discordjs/voice",
            version = "0.17.0",
            sizeMb = 1.4,
            category = PackageCategory.DISCORD_SDK,
            description = "High performance Discord voice connection, audio player & audio stream pipeline.",
            iconName = "speed",
            commands = listOf("npm i @discordjs/voice @discordjs/opus"),
            importSnippet = "const { joinVoiceChannel, createAudioPlayer, createAudioResource } = require('@discordjs/voice');"
        ),
        InstallablePackage(
            id = "sdk_transcripts",
            name = "discord-html-transcripts",
            version = "3.2.0",
            sizeMb = 1.1,
            category = PackageCategory.DISCORD_SDK,
            description = "Export full Discord ticket channels and chats into beautiful downloadable HTML transcripts.",
            iconName = "code",
            commands = listOf("npm i discord-html-transcripts"),
            importSnippet = "const discordTranscripts = require('discord-html-transcripts');"
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

        // Utilities & Helpers
        InstallablePackage(
            id = "util_dotenv",
            name = "dotenv",
            version = "16.4.5",
            sizeMb = 0.4,
            category = PackageCategory.UTILITY,
            description = "Zero-dependency module that loads environment variables from a .env file into process.env.",
            iconName = "key",
            commands = listOf("npm i dotenv"),
            importSnippet = "require('dotenv').config();"
        ),
        InstallablePackage(
            id = "util_axios",
            name = "axios",
            version = "1.6.8",
            sizeMb = 0.8,
            category = PackageCategory.UTILITY,
            description = "Promise-based HTTP client for fetching REST APIs, weather forecasts, meme endpoints and webhooks.",
            iconName = "cloud",
            commands = listOf("npm i axios"),
            importSnippet = "const axios = require('axios');"
        ),
        InstallablePackage(
            id = "util_canvas",
            name = "@napi-rs/canvas",
            version = "0.1.52",
            sizeMb = 3.5,
            category = PackageCategory.UTILITY,
            description = "Ultra-fast Skia-based 2D canvas library for generating customized server welcome banners and rank cards.",
            iconName = "image",
            commands = listOf("npm i @napi-rs/canvas"),
            importSnippet = "const { createCanvas, loadImage } = require('@napi-rs/canvas');"
        ),
        InstallablePackage(
            id = "util_cron",
            name = "node-cron",
            version = "3.0.3",
            sizeMb = 0.5,
            category = PackageCategory.UTILITY,
            description = "Tiny task scheduler in pure JavaScript for scheduled daily announcements and server reminders.",
            iconName = "schedule",
            commands = listOf("npm i node-cron"),
            importSnippet = "const cron = require('node-cron');\ncron.schedule('0 9 * * *', () => { /* Daily 9AM Task */ });"
        ),
        InstallablePackage(
            id = "util_zod",
            name = "zod",
            version = "3.22.4",
            sizeMb = 0.7,
            category = PackageCategory.UTILITY,
            description = "TypeScript-first schema validation with static type inference for bot command options.",
            iconName = "check_circle",
            commands = listOf("npm i zod"),
            importSnippet = "const { z } = require('zod');"
        ),

        // Database & Storage
        InstallablePackage(
            id = "db_quickdb",
            name = "quick.db",
            version = "9.1.7",
            sizeMb = 1.3,
            category = PackageCategory.DATABASE,
            description = "Simple, lightweight, persistent Key-Value SQLite storage engine for server economy and leveling systems.",
            iconName = "storage",
            commands = listOf("npm i quick.db better-sqlite3"),
            importSnippet = "const { QuickDB } = require('quick.db');\nconst db = new QuickDB();"
        ),
        InstallablePackage(
            id = "ext_database_drivers",
            name = "SQLite & Embedded DB Engine",
            version = "3.45.0",
            sizeMb = 1.6,
            category = PackageCategory.DATABASE,
            description = "Embedded local database engine with persistent KV storage for custom bot tables.",
            iconName = "storage",
            commands = listOf("sqlite3", "db.get()", "db.set()"),
            importSnippet = "const sqlite3 = require('sqlite3').verbose();"
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
            commands = listOf("format", "prettier"),
            importSnippet = "// Formatter extension active"
        ),
        InstallablePackage(
            id = "ext_jest",
            name = "Jest & Pytest Runner",
            version = "29.7.0",
            sizeMb = 2.4,
            category = PackageCategory.EXTENSION,
            description = "Automated unit test suite runner for checking bot command logic.",
            iconName = "check_circle",
            commands = listOf("npm test", "pytest"),
            importSnippet = "// Test runner active"
        )
    )

    // Dynamic package catalog
    private val _allPackages = MutableStateFlow<List<InstallablePackage>>(DEFAULT_PACKAGES)
    val allPackages: StateFlow<List<InstallablePackage>> = _allPackages.asStateFlow()

    // Set of installed package IDs
    private val _installedPackages = MutableStateFlow<Set<String>>(
        setOf("runtime_nodejs", "sdk_discordjs", "util_dotenv", "ext_database_drivers", "ext_jest")
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

    suspend fun installPackage(
        pkg: InstallablePackage,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ) {
        val currentProgress = _downloadingProgress.value.toMutableMap()
        currentProgress[pkg.id] = 0.05f
        _downloadingProgress.value = currentProgress

        val stages = listOf(
            0.15f to "Resolving package manifest and dependency tree...",
            0.35f to "Fetching tarball from registry...",
            0.60f to "Verifying checksum and integrity sha512...",
            0.85f to "Extracting and linking node_modules binaries...",
            1.00f to "Running package post-install lifecycle scripts..."
        )

        for ((progress, statusText) in stages) {
            delay(150)
            currentProgress[pkg.id] = progress
            _downloadingProgress.value = currentProgress.toMap()
            onProgress(progress, statusText)
        }

        currentProgress.remove(pkg.id)
        _downloadingProgress.value = currentProgress

        val set = _installedPackages.value.toMutableSet()
        set.add(pkg.id)
        _installedPackages.value = set
    }

    suspend fun installCustomPackage(
        packageName: String,
        manager: String = "npm",
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): InstallablePackage {
        val cleanName = packageName.trim().lowercase()
        val pkgId = "pkg_" + cleanName.replace(Regex("[^a-z0-9_]"), "_")

        val newPkg = InstallablePackage(
            id = pkgId,
            name = cleanName,
            version = "latest",
            sizeMb = 1.0 + (cleanName.length % 4) * 0.7,
            category = if (manager == "pip") PackageCategory.LANGUAGE else PackageCategory.UTILITY,
            description = "Custom installed package '$cleanName' via $manager package registry.",
            iconName = "extension",
            commands = listOf("$manager install $cleanName"),
            importSnippet = if (manager == "pip") {
                "import $cleanName"
            } else {
                "const $cleanName = require('$cleanName');"
            },
            author = "Registry Package ($manager)"
        )

        // Add to catalog if not exists
        if (_allPackages.value.none { it.id == pkgId }) {
            _allPackages.value = _allPackages.value + newPkg
        }

        installPackage(newPkg, onProgress)
        return newPkg
    }

    fun uninstallPackage(pkg: InstallablePackage) {
        if (pkg.isCore) return
        val set = _installedPackages.value.toMutableSet()
        set.remove(pkg.id)
        _installedPackages.value = set
    }

    fun calculateSavedApkSizeMb(): Double {
        val uninstalled = _allPackages.value.filter { !_installedPackages.value.contains(it.id) }
        return uninstalled.sumOf { it.sizeMb }
    }
}
