package com.example.ui.visualbuilder

import androidx.compose.ui.graphics.Color

/**
 * Categories of Scratch-style blocks for Discord bot development.
 * Maps each category to its signature color, darker accent strip color, and visual icon.
 */
enum class BlockCategory(
    val title: String,
    val description: String,
    val color: Color,
    val darkStripColor: Color,
    val emoji: String
) {
    EVENT(
        title = "Events",
        description = "Triggers & Listeners",
        color = Color(0xFF3D7EFF), // Electric Blue
        darkStripColor = Color(0xFF2052C2),
        emoji = "⚡"
    ),
    MESSAGE(
        title = "Messages",
        description = "Channel & DM outputs",
        color = Color(0xFF00E676), // Neon Green
        darkStripColor = Color(0xFF00B85C),
        emoji = "💬"
    ),
    ACTION(
        title = "Actions",
        description = "Roles, Moderation & Members",
        color = Color(0xFFA78BFA), // Purple
        darkStripColor = Color(0xFF7E55E0),
        emoji = "⚙️"
    ),
    LOGIC(
        title = "Logic",
        description = "Branching, Loops & Checks",
        color = Color(0xFFF0B429), // Amber
        darkStripColor = Color(0xFFBD860F),
        emoji = "🔀"
    ),
    VARIABLE(
        title = "Variables",
        description = "Memory & Storage",
        color = Color(0xFF4DD0E1), // Cyan
        darkStripColor = Color(0xFF269BAC),
        emoji = "📦"
    ),
    DESTRUCTIVE(
        title = "Destructive",
        description = "Purge & Severe Actions",
        color = Color(0xFFFF5555), // Red
        darkStripColor = Color(0xFFD32F2F),
        emoji = "⚠️"
    ),
    DISCORD_OBJECT(
        title = "Discord",
        description = "User, Server & Channel Data",
        color = Color(0xFFEB459E), // Pink
        darkStripColor = Color(0xFFB82574),
        emoji = "👑"
    ),
    NETWORK(
        title = "API / Web",
        description = "HTTP, JSON & Webhooks",
        color = Color(0xFF00B4D8), // Teal
        darkStripColor = Color(0xFF007799),
        emoji = "🌐"
    )
}

/**
 * Standard dark IDE theme colors used across the Visual Builder.
 */
object VisualBuilderThemeColors {
    val Background       = Color(0xFF0D0F14)
    val Surface          = Color(0xFF14171F)
    val SurfaceVariant   = Color(0xFF1C2029)
    val Border           = Color(0xFF252A35)
    val OnBackground     = Color(0xFFE6EDF3)
    val OnSurfaceVariant = Color(0xFF8B949E)
    val TextInsideBlock  = Color(0xFF0D0F14)
    val Primary          = Color(0xFF00E676)
    val PrimaryDark      = Color(0xFF00B85C)
    val Accent           = Color(0xFF3D7EFF)
    val DiscordBlurple   = Color(0xFF5865F2)
    val Warning          = Color(0xFFF0B429)
    val Error            = Color(0xFFFF5555)
}
