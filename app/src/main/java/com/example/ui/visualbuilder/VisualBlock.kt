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
 * Can represent events, messages, actions, logic branches, variables, Discord objects, APIs, and moderation.
 */
data class VisualBlock(
    val id: String = UUID.randomUUID().toString(),
    val category: BlockCategory,
    val title: String,
    val subtitle: String = "",
    val icon: String,
    val indentLevel: Int = 0,
    val parentId: String? = null,
    val isContainer: Boolean = false, // True for C-Blocks like If / Loop / Try-Catch
    val isContainerEnd: Boolean = false,
    val slots: List<VisualBlock> = emptyList(),

    // Parameters: Messages & Replies
    val messageContent: String = "Hello!",
    val targetChannel: String = "#general",
    val targetUser: String = "Author", // "Author", "Mentioned", "Owner"
    val reactionEmoji: String = "⭐",
    val isEphemeral: Boolean = false,

    // Parameters: Embeds & Components
    val embedTitle: String = "Server Notification",
    val embedDescription: String = "Welcome to the server! Read the rules in #rules.",
    val embedColorHex: String = "#3D7EFF",
    val embedFieldName: String = "Important Notice",
    val embedFieldValue: String = "Be respectful to everyone.",
    val buttonLabel: String = "Click Me",
    val buttonCustomId: String = "btn_action_1",
    val buttonStyle: String = "Primary", // Primary, Secondary, Success, Danger
    val selectPlaceholder: String = "Select an option...",
    val selectCustomId: String = "menu_select_1",
    val modalTitle: String = "Submit Form",
    val modalCustomId: String = "modal_form_1",

    // Parameters: Logic & Conditions
    val conditionField: String = "message.content", // "message.content", "author.bot", "channel.name"
    val conditionOperator: String = "==", // "==", "!=", "contains", "startswith", "endswith"
    val conditionValue: String = "!ping",
    val waitSeconds: Int = 2,
    val repeatCount: Int = 3,
    val chancePercent: Int = 50,
    val requiredPermission: String = "Administrator", // "Administrator", "Manage Messages", "Kick Members", "Ban Members"
    val cooldownSeconds: Int = 10,
    val loopItemName: String = "item",
    val loopListName: String = "members_list",

    // Parameters: Roles & Actions
    val roleName: String = "Member",
    val timeoutMinutes: Int = 10,
    val actionReason: String = "Rules violation",
    val newChannelName: String = "ticket-01",
    val newNickname: String = "Bot Member",
    val slowmodeSeconds: Int = 5,

    // Parameters: Variables, Lists & Math
    val variableName: String = "user_points",
    val variableOperation: String = "=", // "=", "+=", "-=", "*=", "/="
    val variableValue: String = "10",
    val dbKey: String = "user_balance",
    val mathOperation: String = "+", // "+", "-", "*", "/", "round"
    val stringOperation: String = "uppercase", // "join", "split", "replace", "uppercase", "lowercase"

    // Parameters: Slash Commands & Scheduler
    val slashCommandName: String = "ping",
    val slashCommandDesc: String = "Responds with bot latency",
    val cronSchedule: String = "0 * * * *", // Hourly

    // Parameters: Discord Objects
    val discordObjectType: String = "User", // "User", "Channel", "Server", "Message"
    val objectProperty: String = "tag", // "avatar", "mention", "id", "roles", "member_count", "content"

    // Parameters: Network & APIs
    val httpMethod: String = "GET", // "GET", "POST", "PUT", "DELETE"
    val httpUrl: String = "https://api.github.com/zen",
    val httpBody: String = "{}",
    val jsonPath: String = "data.message",
    val webhookUrl: String = "https://discord.com/api/webhooks/...",

    // Parameters: Moderation & Destructive
    val purgeCount: Int = 10,
    val commentNote: String = "Write a developer note or docstring..."
) {
    /**
     * Returns a formatted descriptive subtitle based on custom configuration.
     */
    val displaySubtitle: String
        get() = when {
            subtitle.isNotEmpty() -> subtitle
            category == BlockCategory.EVENT && title.contains("slash", ignoreCase = true) ->
                "/$slashCommandName - $slashCommandDesc"
            category == BlockCategory.EVENT && title.contains("button", ignoreCase = true) ->
                "on button id: \"$buttonCustomId\""
            category == BlockCategory.EVENT && title.contains("select", ignoreCase = true) ->
                "on select menu id: \"$selectCustomId\""
            category == BlockCategory.EVENT && title.contains("modal", ignoreCase = true) ->
                "on modal submit: \"$modalCustomId\""
            category == BlockCategory.EVENT && title.contains("schedule", ignoreCase = true) ->
                "cron schedule \"$cronSchedule\""
            category == BlockCategory.MESSAGE && title.contains("Send message", ignoreCase = true) ->
                "\"$messageContent\" in $targetChannel"
            category == BlockCategory.MESSAGE && title.contains("Reply", ignoreCase = true) ->
                "\"$messageContent\""
            category == BlockCategory.MESSAGE && title.contains("embed", ignoreCase = true) ->
                "embed \"$embedTitle\" in $targetChannel"
            category == BlockCategory.MESSAGE && title.contains("button", ignoreCase = true) ->
                "action row: \"$buttonLabel\" [$buttonStyle]"
            category == BlockCategory.MESSAGE && title.contains("select menu", ignoreCase = true) ->
                "menu: \"$selectPlaceholder\""
            category == BlockCategory.MESSAGE && title.contains("reaction", ignoreCase = true) ->
                "add $reactionEmoji to message"
            category == BlockCategory.MESSAGE && title.contains("typing", ignoreCase = true) ->
                "typing in $targetChannel"
            category == BlockCategory.MESSAGE && title.contains("DM", ignoreCase = true) ->
                "DM \"$messageContent\" to $targetUser"
            category == BlockCategory.LOGIC && title.contains("If", ignoreCase = true) ->
                "if $conditionField $conditionOperator \"$conditionValue\""
            category == BlockCategory.LOGIC && title.contains("Wait", ignoreCase = true) ->
                "sleep for ${waitSeconds}s"
            category == BlockCategory.LOGIC && title.contains("Repeat", ignoreCase = true) ->
                "loop $repeatCount times"
            category == BlockCategory.LOGIC && title.contains("For each", ignoreCase = true) ->
                "for $loopItemName in $loopListName"
            category == BlockCategory.LOGIC && title.contains("cooldown", ignoreCase = true) ->
                "${cooldownSeconds}s per-user rate limit"
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
            category == BlockCategory.DISCORD_OBJECT && title.contains("User", ignoreCase = true) ->
                "user.$objectProperty of $targetUser"
            category == BlockCategory.DISCORD_OBJECT && title.contains("Channel", ignoreCase = true) ->
                "channel.$objectProperty of $targetChannel"
            category == BlockCategory.DISCORD_OBJECT && title.contains("Server", ignoreCase = true) ->
                "guild.$objectProperty"
            category == BlockCategory.NETWORK && title.contains("HTTP", ignoreCase = true) ->
                "$httpMethod $httpUrl"
            category == BlockCategory.NETWORK && title.contains("JSON", ignoreCase = true) ->
                "extract \"$jsonPath\""
            category == BlockCategory.NETWORK && title.contains("Webhook", ignoreCase = true) ->
                "send payload to webhook"
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
            category == BlockCategory.EVENT && title.contains("button", ignoreCase = true) -> listOf(
                InlineToken.Text("When button"),
                InlineToken.Chip(buttonCustomId, "buttonCustomId"),
                InlineToken.Text("clicked")
            )
            category == BlockCategory.EVENT && title.contains("select", ignoreCase = true) -> listOf(
                InlineToken.Text("When select menu"),
                InlineToken.Chip(selectCustomId, "selectCustomId"),
                InlineToken.Text("selected")
            )
            category == BlockCategory.EVENT && title.contains("schedule", ignoreCase = true) -> listOf(
                InlineToken.Text("At cron"),
                InlineToken.Chip(cronSchedule, "cronSchedule")
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
            category == BlockCategory.MESSAGE && title.contains("button", ignoreCase = true) -> listOf(
                InlineToken.Text("Action Row button"),
                InlineToken.Chip("\"$buttonLabel\"", "buttonLabel"),
                InlineToken.Text("id:"),
                InlineToken.Chip(buttonCustomId, "buttonCustomId")
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
            category == BlockCategory.MESSAGE && title.contains("typing", ignoreCase = true) -> listOf(
                InlineToken.Text("Send typing to"),
                InlineToken.Chip(targetChannel, "targetChannel")
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
            category == BlockCategory.LOGIC && title.contains("For each", ignoreCase = true) -> listOf(
                InlineToken.Text("For each"),
                InlineToken.Chip(loopItemName, "loopItemName"),
                InlineToken.Text("in"),
                InlineToken.Chip(loopListName, "loopListName")
            )
            category == BlockCategory.LOGIC && title.contains("cooldown", ignoreCase = true) -> listOf(
                InlineToken.Text("Rate limit"),
                InlineToken.Chip("${cooldownSeconds}s", "cooldownSeconds")
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
            category == BlockCategory.DISCORD_OBJECT -> listOf(
                InlineToken.Text("Get"),
                InlineToken.Chip(objectProperty, "objectProperty"),
                InlineToken.Text("of"),
                InlineToken.Chip(discordObjectType, "discordObjectType")
            )
            category == BlockCategory.NETWORK && title.contains("HTTP", ignoreCase = true) -> listOf(
                InlineToken.Text("HTTP"),
                InlineToken.Chip(httpMethod, "httpMethod"),
                InlineToken.Chip(httpUrl, "httpUrl")
            )
            category == BlockCategory.NETWORK && title.contains("JSON", ignoreCase = true) -> listOf(
                InlineToken.Text("Extract JSON"),
                InlineToken.Chip(jsonPath, "jsonPath")
            )
            category == BlockCategory.NETWORK && title.contains("Webhook", ignoreCase = true) -> listOf(
                InlineToken.Text("Webhook send"),
                InlineToken.Chip("\"$messageContent\"", "messageContent")
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
