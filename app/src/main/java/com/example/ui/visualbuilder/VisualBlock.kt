package com.example.ui.visualbuilder

import java.util.UUID

/**
 * Inline tokens for rendering interactive Scratch-style parameter capsules inside a block.
 */
sealed class InlineToken {
    data class Text(val text: String) : InlineToken()
    data class Chip(val value: String, val fieldKey: String) : InlineToken()
}

/**
 * Data model representing a block on the Visual Builder canvas.
 * Can represent events, messages, actions, logic branches, variables, and moderation.
 */
data class VisualBlock(
    val id: String = UUID.randomUUID().toString(),
    val category: BlockCategory,
    val title: String,
    val subtitle: String = "",
    val icon: String,
    val indentLevel: Int = 0,
    val parentId: String? = null,
    val isContainer: Boolean = false, // True for C-Blocks like If / Loop
    val isContainerEnd: Boolean = false,
    val slots: List<VisualBlock> = emptyList(),

    // Parameters: Messages & Replies
    val messageContent: String = "Hello!",
    val targetChannel: String = "#general",
    val targetUser: String = "Author", // "Author", "Mentioned", "Owner"
    val reactionEmoji: String = "⭐",

    // Parameters: Embeds
    val embedTitle: String = "Server Notification",
    val embedDescription: String = "Welcome to the server! Read the rules in #rules.",
    val embedColorHex: String = "#3D7EFF",
    val embedFieldName: String = "Important Notice",
    val embedFieldValue: String = "Be respectful to everyone.",

    // Parameters: Logic & Conditions
    val conditionField: String = "message.content", // "message.content", "author.bot", "channel.name"
    val conditionOperator: String = "==", // "==", "!=", "contains", "startswith", "endswith"
    val conditionValue: String = "!ping",
    val waitSeconds: Int = 2,
    val repeatCount: Int = 3,
    val chancePercent: Int = 50,
    val requiredPermission: String = "Administrator", // "Administrator", "Manage Messages", "Kick Members", "Ban Members"

    // Parameters: Roles & Actions
    val roleName: String = "Member",
    val timeoutMinutes: Int = 10,
    val actionReason: String = "Rules violation",
    val newChannelName: String = "ticket-01",
    val newNickname: String = "Bot Member",

    // Parameters: Variables & Storage
    val variableName: String = "user_points",
    val variableOperation: String = "=", // "=", "+=", "-="
    val variableValue: String = "10",
    val dbKey: String = "user_balance",

    // Parameters: Slash Commands
    val slashCommandName: String = "ping",
    val slashCommandDesc: String = "Responds with bot latency",

    // Parameters: Destructive / Moderation
    val purgeCount: Int = 10
) {
    /**
     * Returns a formatted descriptive subtitle based on custom configuration.
     */
    val displaySubtitle: String
        get() = when {
            subtitle.isNotEmpty() -> subtitle
            category == BlockCategory.EVENT && title.contains("slash", ignoreCase = true) ->
                "/$slashCommandName - $slashCommandDesc"
            category == BlockCategory.MESSAGE && title.contains("Send message", ignoreCase = true) ->
                "\"$messageContent\" in $targetChannel"
            category == BlockCategory.MESSAGE && title.contains("Reply", ignoreCase = true) ->
                "\"$messageContent\""
            category == BlockCategory.MESSAGE && title.contains("embed", ignoreCase = true) ->
                "embed \"$embedTitle\" in $targetChannel"
            category == BlockCategory.MESSAGE && title.contains("reaction", ignoreCase = true) ->
                "add $reactionEmoji to message"
            category == BlockCategory.MESSAGE && title.contains("DM", ignoreCase = true) ->
                "DM \"$messageContent\" to $targetUser"
            category == BlockCategory.LOGIC && title.contains("If", ignoreCase = true) ->
                "if $conditionField $conditionOperator \"$conditionValue\""
            category == BlockCategory.LOGIC && title.contains("Wait", ignoreCase = true) ->
                "sleep for ${waitSeconds}s"
            category == BlockCategory.LOGIC && title.contains("Repeat", ignoreCase = true) ->
                "loop $repeatCount times"
            category == BlockCategory.LOGIC && title.contains("chance", ignoreCase = true) ->
                "${chancePercent}% probability"
            category == BlockCategory.LOGIC && title.contains("permission", ignoreCase = true) ->
                "requires $requiredPermission"
            category == BlockCategory.VARIABLE && title.contains("Set var", ignoreCase = true) ->
                "$variableName = $variableValue"
            category == BlockCategory.VARIABLE && title.contains("Change var", ignoreCase = true) ->
                "$variableName += $variableValue"
            category == BlockCategory.VARIABLE && title.contains("Save to Database", ignoreCase = true) ->
                "DB.set(\"$dbKey\", $variableName)"
            category == BlockCategory.VARIABLE && title.contains("Get from Database", ignoreCase = true) ->
                "$variableName = DB.get(\"$dbKey\")"
            category == BlockCategory.ACTION && title.contains("role", ignoreCase = true) ->
                "${if (title.contains("Remove", ignoreCase = true)) "remove" else "add"} \"$roleName\" for $targetUser"
            category == BlockCategory.ACTION && title.contains("Timeout", ignoreCase = true) ->
                "mute $targetUser for ${timeoutMinutes}m"
            category == BlockCategory.ACTION && (title.contains("Kick", ignoreCase = true) || title.contains("Ban", ignoreCase = true)) ->
                "$targetUser (reason: $actionReason)"
            category == BlockCategory.ACTION && title.contains("channel", ignoreCase = true) ->
                "create #$newChannelName"
            category == BlockCategory.DESTRUCTIVE && title.contains("Purge", ignoreCase = true) ->
                "delete $purgeCount msgs in $targetChannel"
            category == BlockCategory.DESTRUCTIVE && title.contains("Lockdown", ignoreCase = true) ->
                "lock $targetChannel"
            else -> ""
        }

    /**
     * Builds interactive parameter tokens for inline bubble chips in Scratch blocks.
     */
    val inlineTokens: List<InlineToken>
        get() = when {
            category == BlockCategory.EVENT && title.contains("slash", ignoreCase = true) -> listOf(
                InlineToken.Text("When command"),
                InlineToken.Chip("/$slashCommandName", "slashCommandName"),
                InlineToken.Text("run")
            )
            category == BlockCategory.MESSAGE && title.contains("Send message", ignoreCase = true) -> listOf(
                InlineToken.Text("Send"),
                InlineToken.Chip("\"$messageContent\"", "messageContent"),
                InlineToken.Text("to"),
                InlineToken.Chip(targetChannel, "targetChannel")
            )
            category == BlockCategory.MESSAGE && title.contains("Reply", ignoreCase = true) -> listOf(
                InlineToken.Text("Reply with"),
                InlineToken.Chip("\"$messageContent\"", "messageContent")
            )
            category == BlockCategory.MESSAGE && title.contains("embed", ignoreCase = true) -> listOf(
                InlineToken.Text("Send embed"),
                InlineToken.Chip("\"$embedTitle\"", "embedTitle"),
                InlineToken.Text("to"),
                InlineToken.Chip(targetChannel, "targetChannel")
            )
            category == BlockCategory.MESSAGE && title.contains("DM", ignoreCase = true) -> listOf(
                InlineToken.Text("DM"),
                InlineToken.Chip("\"$messageContent\"", "messageContent"),
                InlineToken.Text("to"),
                InlineToken.Chip(targetUser, "targetUser")
            )
            category == BlockCategory.MESSAGE && title.contains("reaction", ignoreCase = true) -> listOf(
                InlineToken.Text("Add reaction"),
                InlineToken.Chip(reactionEmoji, "reactionEmoji")
            )
            category == BlockCategory.ACTION && title.contains("Add role", ignoreCase = true) -> listOf(
                InlineToken.Text("Add role"),
                InlineToken.Chip(roleName, "roleName"),
                InlineToken.Text("to"),
                InlineToken.Chip(targetUser, "targetUser")
            )
            category == BlockCategory.ACTION && title.contains("Remove role", ignoreCase = true) -> listOf(
                InlineToken.Text("Remove role"),
                InlineToken.Chip(roleName, "roleName"),
                InlineToken.Text("from"),
                InlineToken.Chip(targetUser, "targetUser")
            )
            category == BlockCategory.ACTION && title.contains("Timeout", ignoreCase = true) -> listOf(
                InlineToken.Text("Timeout"),
                InlineToken.Chip(targetUser, "targetUser"),
                InlineToken.Text("for"),
                InlineToken.Chip("${timeoutMinutes}m", "timeoutMinutes")
            )
            category == BlockCategory.ACTION && title.contains("Kick", ignoreCase = true) -> listOf(
                InlineToken.Text("Kick"),
                InlineToken.Chip(targetUser, "targetUser")
            )
            category == BlockCategory.ACTION && title.contains("Ban", ignoreCase = true) -> listOf(
                InlineToken.Text("Ban"),
                InlineToken.Chip(targetUser, "targetUser")
            )
            category == BlockCategory.LOGIC && title.contains("If", ignoreCase = true) -> listOf(
                InlineToken.Text("If"),
                InlineToken.Chip(conditionField, "conditionField"),
                InlineToken.Chip(conditionOperator, "conditionOperator"),
                InlineToken.Chip("\"$conditionValue\"", "conditionValue")
            )
            category == BlockCategory.LOGIC && title.contains("Wait", ignoreCase = true) -> listOf(
                InlineToken.Text("Wait"),
                InlineToken.Chip("${waitSeconds}s", "waitSeconds")
            )
            category == BlockCategory.LOGIC && title.contains("Repeat", ignoreCase = true) -> listOf(
                InlineToken.Text("Repeat"),
                InlineToken.Chip("$repeatCount times", "repeatCount")
            )
            category == BlockCategory.LOGIC && title.contains("chance", ignoreCase = true) -> listOf(
                InlineToken.Text("With chance"),
                InlineToken.Chip("${chancePercent}%", "chancePercent")
            )
            category == BlockCategory.LOGIC && title.contains("permission", ignoreCase = true) -> listOf(
                InlineToken.Text("If author has"),
                InlineToken.Chip(requiredPermission, "requiredPermission")
            )
            category == BlockCategory.VARIABLE && title.contains("Set var", ignoreCase = true) -> listOf(
                InlineToken.Text("Set"),
                InlineToken.Chip(variableName, "variableName"),
                InlineToken.Text("="),
                InlineToken.Chip(variableValue, "variableValue")
            )
            category == BlockCategory.VARIABLE && title.contains("Change var", ignoreCase = true) -> listOf(
                InlineToken.Text("Change"),
                InlineToken.Chip(variableName, "variableName"),
                InlineToken.Text("by"),
                InlineToken.Chip("+$variableValue", "variableValue")
            )
            category == BlockCategory.VARIABLE && title.contains("Save to Database", ignoreCase = true) -> listOf(
                InlineToken.Text("Save"),
                InlineToken.Chip(variableName, "variableName"),
                InlineToken.Text("as DB key"),
                InlineToken.Chip("\"$dbKey\"", "dbKey")
            )
            category == BlockCategory.DESTRUCTIVE && title.contains("Purge", ignoreCase = true) -> listOf(
                InlineToken.Text("Purge"),
                InlineToken.Chip("$purgeCount messages", "purgeCount"),
                InlineToken.Text("in"),
                InlineToken.Chip(targetChannel, "targetChannel")
            )
            else -> listOf(
                InlineToken.Text(title)
            )
        }
}
