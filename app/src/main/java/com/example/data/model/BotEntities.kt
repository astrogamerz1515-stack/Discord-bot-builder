package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BotLanguage(
    val displayName: String,
    val fileExtension: String,
    val runtimeName: String,
    val iconName: String,
    val defaultEntryFile: String
) {
    JAVASCRIPT("JavaScript (discord.js)", "js", "Node.js v20", "javascript", "index.js"),
    TYPESCRIPT("TypeScript (discord.js)", "ts", "Node.js + ts-node", "typescript", "src/index.ts"),
    PYTHON("Python (discord.py)", "py", "Python 3.11", "python", "bot.py"),
    RUST("Rust (serenity)", "rs", "Rust 1.76", "rust", "src/main.rs"),
    GO("Go (discordgo)", "go", "Go 1.22", "go", "main.go"),
    JAVA("Java (JDA)", "java", "OpenJDK 21", "java", "src/Main.java"),
    CSHARP("C# (Discord.Net)", "cs", ".NET 8.0", "csharp", "Program.cs")
}

@Entity(tableName = "bot_projects")
data class BotProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val language: String = BotLanguage.JAVASCRIPT.name,
    val prefix: String = "!",
    val botToken: String = "MTE4OTIzNDU2Nzg5MDEyMzQ1Ng.G-DiscordSecretBotTokenHere",
    val clientId: String = "118923456789012345",
    val status: String = "Online", // Online, Idle, DND, Invisible
    val activityType: String = "PLAYING", // PLAYING, STREAMING, LISTENING, WATCHING, COMPETING
    val activityText: String = "/help | v2.0",
    val intentMessageContent: Boolean = true,
    val intentGuildMembers: Boolean = true,
    val intentPresences: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bot_files")
data class BotFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val filePath: String, // e.g. "index.js", "commands/ping.js", "package.json"
    val content: String,
    val isEntrypoint: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "terminal_logs")
data class TerminalLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val text: String,
    val type: String = "STDOUT", // STDOUT, STDERR, SYSTEM, INPUT, SUCCESS, WARN
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_embeds")
data class SavedEmbed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String,
    val colorHex: String = "#5865F2",
    val authorName: String = "",
    val authorIconUrl: String = "",
    val footerText: String = "",
    val footerIconUrl: String = "",
    val thumbnailUrl: String = "",
    val imageUrl: String = "",
    val fieldsJson: String = "[]",
    val buttonsJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)
