package com.example.ui.visualbuilder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Definition of standard block templates available in the palette.
 */
data class BlockTemplate(
    val category: BlockCategory,
    val title: String,
    val subtitle: String,
    val icon: String,
    val isContainer: Boolean = false,
    val defaultMessage: String = "Hello!",
    val defaultChannel: String = "#general",
    val defaultUser: String = "Author",
    val defaultConditionField: String = "message.content",
    val defaultConditionOp: String = "==",
    val defaultConditionVal: String = "!help",
    val defaultWait: Int = 2,
    val defaultRole: String = "Member",
    val defaultPurge: Int = 10,
    val defaultEmoji: String = "⭐",
    val defaultEmbedTitle: String = "Server Notification",
    val defaultSlashName: String = "ping",
    val defaultSlashDesc: String = "Responds with bot latency",
    val defaultPermission: String = "Administrator",
    val defaultVarName: String = "user_points",
    val defaultVarVal: String = "10",
    val defaultButtonLabel: String = "Click Me",
    val defaultButtonCustomId: String = "btn_action_1",
    val defaultSelectCustomId: String = "menu_select_1",
    val defaultModalCustomId: String = "modal_form_1",
    val defaultCronSchedule: String = "0 * * * *",
    val defaultDiscordObjectType: String = "User",
    val defaultObjectProperty: String = "tag",
    val defaultHttpMethod: String = "GET",
    val defaultHttpUrl: String = "https://api.github.com/zen",
    val defaultJsonPath: String = "data.message"
)

/**
 * Pre-made Bot Recipe / Starter Template
 */
data class BotRecipe(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val blocks: List<VisualBlock>
)

object PaletteCatalog {
    val catalog: Map<BlockCategory, List<BlockTemplate>> = mapOf(
        BlockCategory.EVENT to listOf(
            BlockTemplate(BlockCategory.EVENT, "When message received", "triggers on any server message", "⚡"),
            BlockTemplate(BlockCategory.EVENT, "When slash command run", "/slash command trigger", "⌨️", defaultSlashName = "ping", defaultSlashDesc = "Check bot status"),
            BlockTemplate(BlockCategory.EVENT, "When button clicked", "triggers on component click", "🔘", defaultButtonCustomId = "btn_action_1"),
            BlockTemplate(BlockCategory.EVENT, "When select menu used", "triggers on option selection", "📋", defaultSelectCustomId = "menu_select_1"),
            BlockTemplate(BlockCategory.EVENT, "When modal submitted", "triggers on modal submit", "📝", defaultModalCustomId = "modal_form_1"),
            BlockTemplate(BlockCategory.EVENT, "When member joins", "triggers on new user join", "👋"),
            BlockTemplate(BlockCategory.EVENT, "When member leaves", "triggers on member leave", "🚪"),
            BlockTemplate(BlockCategory.EVENT, "When reaction added", "triggers on emoji reaction", "⭐"),
            BlockTemplate(BlockCategory.EVENT, "When reaction removed", "triggers when reaction removed", "❌"),
            BlockTemplate(BlockCategory.EVENT, "On bot ready", "runs once when bot starts up", "🚀"),
            BlockTemplate(BlockCategory.EVENT, "On scheduled time", "cron-style recurring timer", "⏰", defaultCronSchedule = "0 * * * *"),
            BlockTemplate(BlockCategory.EVENT, "When voice state changes", "joins/leaves voice channel", "🎙️")
        ),
        BlockCategory.MESSAGE to listOf(
            BlockTemplate(BlockCategory.MESSAGE, "Send message", "send text to a channel", "💬", defaultMessage = "Hello from Bot!", defaultChannel = "#general"),
            BlockTemplate(BlockCategory.MESSAGE, "Reply to message", "reply directly to author", "↩️", defaultMessage = "Pong! 🏓"),
            BlockTemplate(BlockCategory.MESSAGE, "Send rich embed", "rich embed card with color & fields", "📋", defaultEmbedTitle = "Server Announcement", defaultChannel = "#announcements"),
            BlockTemplate(BlockCategory.MESSAGE, "Send with buttons", "action row with interactive buttons", "🔘", defaultButtonLabel = "Click Here", defaultButtonCustomId = "btn_1"),
            BlockTemplate(BlockCategory.MESSAGE, "Send with select menu", "dropdown menu with choices", "📑", defaultSelectCustomId = "select_roles"),
            BlockTemplate(BlockCategory.MESSAGE, "Send DM to user", "private direct message", "📬", defaultMessage = "Welcome to our server!", defaultUser = "Author"),
            BlockTemplate(BlockCategory.MESSAGE, "Edit message", "update previously sent message", "✏️", defaultMessage = "Updated content"),
            BlockTemplate(BlockCategory.MESSAGE, "Delete message", "delete the triggering message", "🗑️"),
            BlockTemplate(BlockCategory.MESSAGE, "Pin message", "pin message to channel header", "📌"),
            BlockTemplate(BlockCategory.MESSAGE, "Add reaction", "react with emoji to message", "✨", defaultEmoji = "✅"),
            BlockTemplate(BlockCategory.MESSAGE, "Send typing indicator", "show bot is typing in channel", "⏳", defaultChannel = "#general")
        ),
        BlockCategory.ACTION to listOf(
            BlockTemplate(BlockCategory.ACTION, "Add role", "assign role to user", "🏷️", defaultRole = "Member", defaultUser = "Author"),
            BlockTemplate(BlockCategory.ACTION, "Remove role", "remove role from user", "🚫", defaultRole = "Muted", defaultUser = "Author"),
            BlockTemplate(BlockCategory.ACTION, "Timeout user", "mute user temporarily", "⏱️", defaultUser = "Author", defaultWait = 10),
            BlockTemplate(BlockCategory.ACTION, "Kick user", "kick user from server", "👢", defaultUser = "Author"),
            BlockTemplate(BlockCategory.ACTION, "Ban user", "ban user from server", "🔨", defaultUser = "Author"),
            BlockTemplate(BlockCategory.ACTION, "Unban user", "remove ban by user ID", "🕊️", defaultUser = "Author"),
            BlockTemplate(BlockCategory.ACTION, "Create channel", "create new channel", "📁", defaultChannel = "ticket-01"),
            BlockTemplate(BlockCategory.ACTION, "Delete channel", "remove target channel", "🗑️", defaultChannel = "temp-channel"),
            BlockTemplate(BlockCategory.ACTION, "Create thread", "start public or private thread", "🧵", defaultChannel = "discussion"),
            BlockTemplate(BlockCategory.ACTION, "Lock channel", "prevent members from typing", "🔒", defaultChannel = "#general"),
            BlockTemplate(BlockCategory.ACTION, "Unlock channel", "restore chat permissions", "🔓", defaultChannel = "#general"),
            BlockTemplate(BlockCategory.ACTION, "Move member to voice", "drag member to voice channel", "🔊", defaultUser = "Author"),
            BlockTemplate(BlockCategory.ACTION, "Change nickname", "rename user in server", "✏️", defaultUser = "Author")
        ),
        BlockCategory.LOGIC to listOf(
            BlockTemplate(BlockCategory.LOGIC, "If ... then", "conditional branch", "🔀", isContainer = true, defaultConditionField = "message.content", defaultConditionOp = "==", defaultConditionVal = "!ping"),
            BlockTemplate(BlockCategory.LOGIC, "Wait ... seconds", "pause execution asynchronously", "⏱️", defaultWait = 2),
            BlockTemplate(BlockCategory.LOGIC, "Repeat ... times", "run loop block", "🔁", isContainer = true),
            BlockTemplate(BlockCategory.LOGIC, "For each in list", "iterate through elements", "🔄", isContainer = true),
            BlockTemplate(BlockCategory.LOGIC, "While is true", "loop while condition matches", "⏳", isContainer = true),
            BlockTemplate(BlockCategory.LOGIC, "Try / Catch", "catch and handle runtime errors", "🛡️", isContainer = true),
            BlockTemplate(BlockCategory.LOGIC, "Check permission", "verify author permissions", "🛡️", isContainer = true, defaultPermission = "Administrator"),
            BlockTemplate(BlockCategory.LOGIC, "Cooldown check", "per-user rate limit interval", "⏳", defaultWait = 5),
            BlockTemplate(BlockCategory.LOGIC, "With random chance", "execute branch with % probability", "🎲", isContainer = true),
            BlockTemplate(BlockCategory.LOGIC, "Stop script", "stop execution early", "🛑")
        ),
        BlockCategory.VARIABLE to listOf(
            BlockTemplate(BlockCategory.VARIABLE, "Set var", "store value in memory", "📦", defaultVarName = "user_points", defaultVarVal = "10"),
            BlockTemplate(BlockCategory.VARIABLE, "Change var", "increment/decrement value", "➕", defaultVarName = "user_points", defaultVarVal = "1"),
            BlockTemplate(BlockCategory.VARIABLE, "List operation", "add or remove item from list", "📜", defaultVarName = "items_list", defaultVarVal = "apple"),
            BlockTemplate(BlockCategory.VARIABLE, "Map operation", "set key-value in dictionary", "🗺️", defaultVarName = "user_data", defaultVarVal = "gold"),
            BlockTemplate(BlockCategory.VARIABLE, "Random number picker", "generate random int in range", "🎲", defaultVarName = "rolled_num", defaultVarVal = "100"),
            BlockTemplate(BlockCategory.VARIABLE, "Math operation", "add, multiply, divide, round", "🔢", defaultVarName = "total_score", defaultVarVal = "5"),
            BlockTemplate(BlockCategory.VARIABLE, "String operation", "join, split, or uppercase", "🔤", defaultVarName = "clean_text", defaultVarVal = "HELLO"),
            BlockTemplate(BlockCategory.VARIABLE, "Save to Database", "persist variable permanently", "💾", defaultVarName = "user_points"),
            BlockTemplate(BlockCategory.VARIABLE, "Get from Database", "retrieve stored value", "🔍", defaultVarName = "user_points")
        ),
        BlockCategory.DISCORD_OBJECT to listOf(
            BlockTemplate(BlockCategory.DISCORD_OBJECT, "User object", "get avatar, mention, id, or roles", "👤", defaultDiscordObjectType = "User", defaultObjectProperty = "tag"),
            BlockTemplate(BlockCategory.DISCORD_OBJECT, "Channel object", "get channel mention, id, or topic", "💬", defaultDiscordObjectType = "Channel", defaultObjectProperty = "name"),
            BlockTemplate(BlockCategory.DISCORD_OBJECT, "Server object", "get guild member count or boost level", "🏰", defaultDiscordObjectType = "Server", defaultObjectProperty = "member_count"),
            BlockTemplate(BlockCategory.DISCORD_OBJECT, "Message object", "get content, author, or attachments", "📨", defaultDiscordObjectType = "Message", defaultObjectProperty = "content"),
            BlockTemplate(BlockCategory.DISCORD_OBJECT, "Permission check", "check user permission flags", "🛡️", isContainer = true, defaultPermission = "Manage Messages"),
            BlockTemplate(BlockCategory.DISCORD_OBJECT, "Role hierarchy check", "verify author role is higher than target", "⚖️", isContainer = true)
        ),
        BlockCategory.NETWORK to listOf(
            BlockTemplate(BlockCategory.NETWORK, "HTTP GET request", "fetch REST API response", "🌐", defaultHttpMethod = "GET", defaultHttpUrl = "https://api.github.com/zen"),
            BlockTemplate(BlockCategory.NETWORK, "HTTP POST request", "send JSON payload to API", "📤", defaultHttpMethod = "POST", defaultHttpUrl = "https://httpbin.org/post"),
            BlockTemplate(BlockCategory.NETWORK, "Parse JSON response", "parse raw API body as object", "🧩", defaultJsonPath = "data.message"),
            BlockTemplate(BlockCategory.NETWORK, "Extract JSON path", "read dot-notation field from response", "🎯", defaultJsonPath = "data.quote"),
            BlockTemplate(BlockCategory.NETWORK, "Webhook send", "dispatch message via Discord Webhook", "🚀", defaultMessage = "Alert from BotStudio!")
        ),
        BlockCategory.DESTRUCTIVE to listOf(
            BlockTemplate(BlockCategory.DESTRUCTIVE, "Purge messages", "bulk delete recent messages", "🔥", defaultPurge = 10, defaultChannel = "#general"),
            BlockTemplate(BlockCategory.DESTRUCTIVE, "Lockdown channel", "disable send messages", "🔒", defaultChannel = "#general"),
            BlockTemplate(BlockCategory.DESTRUCTIVE, "Unlock channel", "re-enable send messages", "🔓", defaultChannel = "#general"),
            BlockTemplate(BlockCategory.DESTRUCTIVE, "Ban & purge 7d", "ban user and clear 7 days msgs", "💥", defaultUser = "Author")
        )
    )

    val starterRecipes: List<BotRecipe> = listOf(
        BotRecipe(
            id = "ping_pong",
            name = "Ping-Pong Bot",
            emoji = "🏓",
            description = "Responds to !ping with latency and a pong reply",
            blocks = listOf(
                VisualBlock(category = BlockCategory.EVENT, title = "When message received", subtitle = "triggers on any server message", icon = "⚡"),
                VisualBlock(category = BlockCategory.LOGIC, title = "If ... then", subtitle = "if message.content == \"!ping\"", icon = "🔀", conditionField = "message.content", conditionOperator = "==", conditionValue = "!ping", isContainer = true),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Reply to message", subtitle = "\"Pong! 🏓 Latency: 24ms\"", icon = "↩️", messageContent = "Pong! 🏓 Latency: 24ms", indentLevel = 1),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Add reaction", subtitle = "add 🏓 to message", icon = "✨", reactionEmoji = "🏓", indentLevel = 1)
            )
        ),
        BotRecipe(
            id = "auto_moderator",
            name = "Auto-Moderator",
            emoji = "🛡️",
            description = "Filters banned words, deletes message, and warns the author",
            blocks = listOf(
                VisualBlock(category = BlockCategory.EVENT, title = "When message received", subtitle = "triggers on any server message", icon = "⚡"),
                VisualBlock(category = BlockCategory.LOGIC, title = "If ... then", subtitle = "if message.content contains \"discord.gg\"", icon = "🔀", conditionField = "message.content", conditionOperator = "contains", conditionValue = "discord.gg", isContainer = true),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Delete message", subtitle = "delete advertising message", icon = "🗑️", indentLevel = 1),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Send DM to user", subtitle = "\"Advertising invite links is prohibited!\"", icon = "📬", messageContent = "⚠️ Warning: Advertising invite links is prohibited!", targetUser = "Author", indentLevel = 1),
                VisualBlock(category = BlockCategory.ACTION, title = "Timeout user", subtitle = "mute Author for 10m", icon = "⏱️", targetUser = "Author", timeoutMinutes = 10, indentLevel = 1)
            )
        ),
        BotRecipe(
            id = "welcome_role",
            name = "Welcome & Auto-Role",
            emoji = "👋",
            description = "Welcomes newcomers with rich embed card & assigns Member role",
            blocks = listOf(
                VisualBlock(category = BlockCategory.EVENT, title = "When member joins", subtitle = "triggers on new user join", icon = "👋"),
                VisualBlock(category = BlockCategory.ACTION, title = "Add role", subtitle = "add \"Member\" for Author", icon = "🏷️", roleName = "Member", targetUser = "Author"),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Send rich embed", subtitle = "embed \"Welcome!\" in #welcome", icon = "📋", embedTitle = "🎉 Welcome New Member!", embedDescription = "We are thrilled to have you here! Check out #rules and say hi!", targetChannel = "#welcome"),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Send DM to user", subtitle = "\"Welcome to the community!\"", icon = "📬", messageContent = "Welcome to the server! Need help? Contact staff in #help.", targetUser = "Author")
            )
        ),
        BotRecipe(
            id = "slash_command",
            name = "Slash /help Command",
            emoji = "⌨️",
            description = "Modern slash command with embed menu",
            blocks = listOf(
                VisualBlock(category = BlockCategory.EVENT, title = "When slash command run", subtitle = "/help - Show bot commands", icon = "⌨️", slashCommandName = "help", slashCommandDesc = "Show bot commands list"),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Send rich embed", subtitle = "embed \"Bot Command Manual\"", icon = "📋", embedTitle = "📖 Bot Command Manual", embedDescription = "`/help` - This menu\n`/ping` - Check latency\n`/clear` - Purge chat", targetChannel = "#general")
            )
        ),
        BotRecipe(
            id = "ticket_bot",
            name = "Interactive Ticket Bot",
            emoji = "🎫",
            description = "Sends interactive button and creates dedicated support channel on click",
            blocks = listOf(
                VisualBlock(category = BlockCategory.EVENT, title = "When button clicked", subtitle = "on button id: \"create_ticket\"", icon = "🔘", buttonCustomId = "create_ticket"),
                VisualBlock(category = BlockCategory.ACTION, title = "Create channel", subtitle = "create #ticket-user", icon = "📁", newChannelName = "ticket-support"),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Send message", subtitle = "\"Ticket opened! A moderator will assist you shortly.\"", icon = "💬", messageContent = "Support ticket created! Please describe your issue.", targetChannel = "#ticket-support")
            )
        ),
        BotRecipe(
            id = "api_quote_bot",
            name = "REST API Quote Bot",
            emoji = "🌐",
            description = "Fetches a live quote via HTTP GET and displays it in a rich embed",
            blocks = listOf(
                VisualBlock(category = BlockCategory.EVENT, title = "When slash command run", subtitle = "/quote - Get random quote", icon = "⌨️", slashCommandName = "quote", slashCommandDesc = "Fetches live quote from API"),
                VisualBlock(category = BlockCategory.NETWORK, title = "HTTP GET request", subtitle = "GET https://api.github.com/zen", icon = "🌐", httpMethod = "GET", httpUrl = "https://api.github.com/zen"),
                VisualBlock(category = BlockCategory.MESSAGE, title = "Send rich embed", subtitle = "embed \"Quote of the Day\"", icon = "📋", embedTitle = "💡 Zen Wisdom", embedDescription = "Mind your words, they become actions.", targetChannel = "#general")
            )
        )
    )
}

/**
 * Collapsible bottom panel containing the block palette categorized by color and type,
 * with search bar, recipe shortcuts, and tap/drag capabilities.
 */
@Composable
fun BlockPalette(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedCategory: BlockCategory,
    onSelectCategory: (BlockCategory) -> Unit,
    onBlockDragStart: (BlockTemplate, Offset) -> Unit,
    onBlockDrag: (Offset) -> Unit,
    onBlockDragEnd: () -> Unit,
    onBlockClick: (BlockTemplate) -> Unit,
    onOpenRecipes: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 0.8f))
            .alpha(if (isDragging) 0.35f else 1.0f)
            .testTag("block_palette_surface"),
        color = VisualBuilderThemeColors.Surface,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1.dp top border #252A35
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VisualBuilderThemeColors.Border)
            )

            // Header bar: Drag handle + Expand/Collapse toggle + Recipe button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp)
                    .testTag("palette_drag_handle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Palette label with toggle arrow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Toggle palette",
                        tint = VisualBuilderThemeColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BLOCK PALETTE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VisualBuilderThemeColors.OnSurfaceVariant
                    )
                }

                // Center: drag handle bar
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .background(VisualBuilderThemeColors.Border, CircleShape)
                )

                // Right: Starter Recipes button
                Surface(
                    color = VisualBuilderThemeColors.SurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                    modifier = Modifier
                        .clickable { onOpenRecipes() }
                        .testTag("btn_open_recipes")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Recipes",
                            tint = VisualBuilderThemeColors.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recipes",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VisualBuilderThemeColors.OnBackground
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    // Search bar inside palette
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search blocks (e.g. embed, ban, role)...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = VisualBuilderThemeColors.OnSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = VisualBuilderThemeColors.OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = VisualBuilderThemeColors.OnSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VisualBuilderThemeColors.OnBackground,
                                unfocusedTextColor = VisualBuilderThemeColors.OnBackground,
                                focusedBorderColor = VisualBuilderThemeColors.Primary,
                                unfocusedBorderColor = VisualBuilderThemeColors.Border,
                                focusedContainerColor = VisualBuilderThemeColors.Background,
                                unfocusedContainerColor = VisualBuilderThemeColors.Background
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )
                    }

                    // Category Pill Strip (Horizontal Scroll)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BlockCategory.values().forEach { category ->
                            val isSelected = (selectedCategory == category) && searchQuery.isEmpty()
                            val count = PaletteCatalog.catalog[category]?.size ?: 0

                            Surface(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        searchQuery = ""
                                        onSelectCategory(category)
                                    }
                                    .testTag("palette_tab_${category.name.lowercase()}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) category.color else VisualBuilderThemeColors.SurfaceVariant,
                                border = if (isSelected) null else BorderStroke(1.dp, VisualBuilderThemeColors.Border),
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.emoji,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) VisualBuilderThemeColors.TextInsideBlock else VisualBuilderThemeColors.OnBackground
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Count badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) VisualBuilderThemeColors.TextInsideBlock else VisualBuilderThemeColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Blocks List (Horizontal Scroll or filtered list)
                    val displayedTemplates = remember(selectedCategory, searchQuery) {
                        if (searchQuery.isNotBlank()) {
                            PaletteCatalog.catalog.values.flatten().filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.subtitle.contains(searchQuery, ignoreCase = true) ||
                                it.category.title.contains(searchQuery, ignoreCase = true)
                            }
                        } else {
                            PaletteCatalog.catalog[selectedCategory] ?: emptyList()
                        }
                    }

                    if (displayedTemplates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No blocks matching \"$searchQuery\"",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = VisualBuilderThemeColors.OnSurfaceVariant
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("palette_blocks_row"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = displayedTemplates,
                                key = { "${it.category.name}_${it.title}" }
                            ) { template ->
                                PaletteBlockItem(
                                    template = template,
                                    onClick = { onBlockClick(template) },
                                    onDragStart = { offset -> onBlockDragStart(template, offset) },
                                    onDrag = onBlockDrag,
                                    onDragEnd = onBlockDragEnd
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual draggable and clickable block template item in the palette.
 */
@Composable
fun PaletteBlockItem(
    template: BlockTemplate,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(200.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .draggableBlock(
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragEnd
            )
            .testTag("palette_block_${template.title}"),
        shape = RoundedCornerShape(10.dp),
        color = template.category.color,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 4.dp vertical color strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(58.dp)
                        .background(template.category.darkStripColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Icon
                Text(
                    text = template.icon,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Title and Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = template.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = VisualBuilderThemeColors.TextInsideBlock,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = template.subtitle,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = VisualBuilderThemeColors.TextInsideBlock.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Plus badge indicating tap to add
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp)
                        .background(Color.Black.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VisualBuilderThemeColors.TextInsideBlock
                    )
                }
            }

            // Scratch bottom puzzle notch
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 4.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(VisualBuilderThemeColors.Background)
            )
        }
    }
}
