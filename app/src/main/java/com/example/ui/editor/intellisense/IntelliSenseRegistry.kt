package com.example.ui.editor.intellisense

object IntelliSenseRegistry {

    val DISCORD_JS_ITEMS = listOf(
        // Interaction Methods & Properties
        CompletionItem(
            label = "interaction.reply",
            insertText = "await interaction.reply({ content: '\$1', ephemeral: true });",
            kind = CompletionKind.METHOD,
            detail = "(options: InteractionReplyOptions) => Promise<Message>",
            documentation = "Sends an initial response to the slash command or component interaction. Can be set as ephemeral (only visible to user).",
            example = "await interaction.reply({ content: 'Done!', ephemeral: true });",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.deferReply",
            insertText = "await interaction.deferReply({ ephemeral: \$1 });",
            kind = CompletionKind.METHOD,
            detail = "(options?: { ephemeral?: boolean }) => Promise<void>",
            documentation = "Acknowledges the interaction and displays a thinking state ('Bot is thinking...'). Gives your bot up to 15 minutes to respond via editReply.",
            example = "await interaction.deferReply();\n// do slow operation...\nawait interaction.editReply('Finished computing!');",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.editReply",
            insertText = "await interaction.editReply({ content: '\$1' });",
            kind = CompletionKind.METHOD,
            detail = "(options: InteractionEditReplyOptions) => Promise<Message>",
            documentation = "Edits the initial reply or deferred reply sent to this interaction.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.followUp",
            insertText = "await interaction.followUp({ content: '\$1', ephemeral: false });",
            kind = CompletionKind.METHOD,
            detail = "(options: InteractionReplyOptions) => Promise<Message>",
            documentation = "Sends a follow-up message to the interaction after the initial response.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.deleteReply",
            insertText = "await interaction.deleteReply();",
            kind = CompletionKind.METHOD,
            detail = "() => Promise<void>",
            documentation = "Deletes the initial reply sent to this interaction.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.showModal",
            insertText = "await interaction.showModal(modal);",
            kind = CompletionKind.METHOD,
            detail = "(modal: ModalBuilder) => Promise<void>",
            documentation = "Pops up a modal dialog form on the user's Discord client screen.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.options.getString",
            insertText = "interaction.options.getString('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(name: string, required?: boolean) => string | null",
            documentation = "Retrieves the string value provided by the user for the slash command option.",
            category = "Interaction",
            triggerPrefix = "interaction.options."
        ),
        CompletionItem(
            label = "interaction.options.getInteger",
            insertText = "interaction.options.getInteger('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(name: string, required?: boolean) => number | null",
            documentation = "Retrieves the integer value provided by the user for the slash command option.",
            category = "Interaction",
            triggerPrefix = "interaction.options."
        ),
        CompletionItem(
            label = "interaction.options.getUser",
            insertText = "interaction.options.getUser('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(name: string, required?: boolean) => User | null",
            documentation = "Retrieves the Discord User object mentioned in the command argument.",
            category = "Interaction",
            triggerPrefix = "interaction.options."
        ),
        CompletionItem(
            label = "interaction.options.getMember",
            insertText = "interaction.options.getMember('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(name: string, required?: boolean) => GuildMember | null",
            documentation = "Retrieves the GuildMember object in the server context.",
            category = "Interaction",
            triggerPrefix = "interaction.options."
        ),
        CompletionItem(
            label = "interaction.options.getChannel",
            insertText = "interaction.options.getChannel('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(name: string, required?: boolean) => Channel | null",
            documentation = "Retrieves the Channel object passed by the user.",
            category = "Interaction",
            triggerPrefix = "interaction.options."
        ),
        CompletionItem(
            label = "interaction.options.getBoolean",
            insertText = "interaction.options.getBoolean('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(name: string, required?: boolean) => boolean | null",
            documentation = "Retrieves the boolean flag from slash command options.",
            category = "Interaction",
            triggerPrefix = "interaction.options."
        ),
        CompletionItem(
            label = "interaction.isChatInputCommand",
            insertText = "interaction.isChatInputCommand()",
            kind = CompletionKind.METHOD,
            detail = "() => boolean",
            documentation = "Returns true if this interaction was invoked as a slash command.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.isButton",
            insertText = "interaction.isButton()",
            kind = CompletionKind.METHOD,
            detail = "() => boolean",
            documentation = "Returns true if this interaction was triggered by a button component click.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.isStringSelectMenu",
            insertText = "interaction.isStringSelectMenu()",
            kind = CompletionKind.METHOD,
            detail = "() => boolean",
            documentation = "Returns true if this interaction is a string select menu dropdown selection.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.isModalSubmit",
            insertText = "interaction.isModalSubmit()",
            kind = CompletionKind.METHOD,
            detail = "() => boolean",
            documentation = "Returns true if this interaction was a modal form submission.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.user",
            insertText = "interaction.user",
            kind = CompletionKind.PROPERTY,
            detail = "User",
            documentation = "The Discord user who triggered this interaction.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.guild",
            insertText = "interaction.guild",
            kind = CompletionKind.PROPERTY,
            detail = "Guild | null",
            documentation = "The Discord server (guild) in which this interaction occurred.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.channel",
            insertText = "interaction.channel",
            kind = CompletionKind.PROPERTY,
            detail = "TextBasedChannel | null",
            documentation = "The text channel in which this interaction was created.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.commandName",
            insertText = "interaction.commandName",
            kind = CompletionKind.PROPERTY,
            detail = "string",
            documentation = "The name of the slash command executed by the user.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),
        CompletionItem(
            label = "interaction.customId",
            insertText = "interaction.customId",
            kind = CompletionKind.PROPERTY,
            detail = "string",
            documentation = "The developer-defined identifier for buttons, select menus, or modals.",
            category = "Interaction",
            triggerPrefix = "interaction."
        ),

        // EmbedBuilder Methods
        CompletionItem(
            label = "EmbedBuilder",
            insertText = "new EmbedBuilder()\n  .setTitle('\$1')\n  .setDescription('\$2')\n  .setColor(0x5865F2)",
            kind = CompletionKind.CLASS,
            detail = "class EmbedBuilder",
            documentation = "Constructs a rich Discord embed card with title, descriptions, colors, and fields.",
            category = "Embed",
            triggerPrefix = "new "
        ),
        CompletionItem(
            label = "embed.setTitle",
            insertText = "setTitle('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(title: string) => this",
            documentation = "Sets the top title of the embed card (up to 256 characters).",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setDescription",
            insertText = "setDescription('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(description: string) => this",
            documentation = "Sets the main body text of the embed card (up to 4096 characters). Markdown supported.",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setColor",
            insertText = "setColor(0x5865F2)",
            kind = CompletionKind.METHOD,
            detail = "(color: ColorResolvable) => this",
            documentation = "Sets the left accent border color of the embed (hex integer or Discord color preset).",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.addFields",
            insertText = "addFields(\n  { name: '\$1', value: '\$2', inline: true }\n)",
            kind = CompletionKind.METHOD,
            detail = "(...fields: RestOrArray<APIEmbedField>) => this",
            documentation = "Adds one or more grid fields to the embed card (up to 25 total fields).",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setFooter",
            insertText = "setFooter({ text: '\$1', iconURL: '\$2' })",
            kind = CompletionKind.METHOD,
            detail = "(footer: EmbedFooterData) => this",
            documentation = "Sets the small footer note and optional icon at the bottom of the embed.",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setThumbnail",
            insertText = "setThumbnail('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(url: string) => this",
            documentation = "Displays a small image thumbnail in the top-right corner of the embed.",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setImage",
            insertText = "setImage('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(url: string) => this",
            documentation = "Sets a large featured media image banner at the bottom of the embed.",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setTimestamp",
            insertText = "setTimestamp()",
            kind = CompletionKind.METHOD,
            detail = "(timestamp?: number | Date | null) => this",
            documentation = "Renders an accurate localized timestamp in the footer of the embed.",
            category = "Embed",
            triggerPrefix = "embed."
        ),
        CompletionItem(
            label = "embed.setAuthor",
            insertText = "setAuthor({ name: '\$1', iconURL: '\$2', url: '\$3' })",
            kind = CompletionKind.METHOD,
            detail = "(author: EmbedAuthorData) => this",
            documentation = "Sets the small author header displayed above the title.",
            category = "Embed",
            triggerPrefix = "embed."
        ),

        // ActionRow, Buttons, Menus
        CompletionItem(
            label = "ActionRowBuilder",
            insertText = "new ActionRowBuilder().addComponents(\$1)",
            kind = CompletionKind.CLASS,
            detail = "class ActionRowBuilder<AnyComponentBuilder>",
            documentation = "A container row that holds up to 5 buttons or 1 select menu or 1 text input.",
            category = "Components",
            triggerPrefix = "new "
        ),
        CompletionItem(
            label = "ButtonBuilder",
            insertText = "new ButtonBuilder()\n  .setCustomId('\$1')\n  .setLabel('\$2')\n  .setStyle(ButtonStyle.Primary)",
            kind = CompletionKind.CLASS,
            detail = "class ButtonBuilder",
            documentation = "Creates an interactive clickable button component.",
            category = "Components",
            triggerPrefix = "new "
        ),
        CompletionItem(
            label = "ButtonStyle.Primary",
            insertText = "ButtonStyle.Primary",
            kind = CompletionKind.CONSTANT,
            detail = "ButtonStyle.Primary = 1 (Blurple)",
            documentation = "Blurple Discord button for standard primary actions.",
            category = "Components"
        ),
        CompletionItem(
            label = "ButtonStyle.Secondary",
            insertText = "ButtonStyle.Secondary",
            kind = CompletionKind.CONSTANT,
            detail = "ButtonStyle.Secondary = 2 (Grey)",
            documentation = "Neutral grey button for secondary options.",
            category = "Components"
        ),
        CompletionItem(
            label = "ButtonStyle.Success",
            insertText = "ButtonStyle.Success",
            kind = CompletionKind.CONSTANT,
            detail = "ButtonStyle.Success = 3 (Green)",
            documentation = "Green button indicating positive or confirmation actions.",
            category = "Components"
        ),
        CompletionItem(
            label = "ButtonStyle.Danger",
            insertText = "ButtonStyle.Danger",
            kind = CompletionKind.CONSTANT,
            detail = "ButtonStyle.Danger = 4 (Red)",
            documentation = "Red button for destructive actions like kicking, banning, or deleting.",
            category = "Components"
        ),
        CompletionItem(
            label = "ButtonStyle.Link",
            insertText = "ButtonStyle.Link",
            kind = CompletionKind.CONSTANT,
            detail = "ButtonStyle.Link = 5 (URL Link)",
            documentation = "Grey button with an external link arrow that opens a URL in browser.",
            category = "Components"
        ),
        CompletionItem(
            label = "StringSelectMenuBuilder",
            insertText = "new StringSelectMenuBuilder()\n  .setCustomId('\$1')\n  .setPlaceholder('Choose an option...')\n  .addOptions(\n    { label: 'Option 1', value: 'opt_1', description: 'First option' }\n  )",
            kind = CompletionKind.CLASS,
            detail = "class StringSelectMenuBuilder",
            documentation = "Creates a drop-down menu allowing users to select one or multiple text options.",
            category = "Components",
            triggerPrefix = "new "
        ),
        CompletionItem(
            label = "ModalBuilder",
            insertText = "new ModalBuilder()\n  .setCustomId('\$1')\n  .setTitle('\$2')\n  .addComponents(\$3)",
            kind = CompletionKind.CLASS,
            detail = "class ModalBuilder",
            documentation = "Builds an interactive popup modal form with text input fields.",
            category = "Components",
            triggerPrefix = "new "
        ),
        CompletionItem(
            label = "TextInputBuilder",
            insertText = "new TextInputBuilder()\n  .setCustomId('\$1')\n  .setLabel('\$2')\n  .setStyle(TextInputStyle.Paragraph)",
            kind = CompletionKind.CLASS,
            detail = "class TextInputBuilder",
            documentation = "A single-line or multi-line text input field placed inside a modal.",
            category = "Components",
            triggerPrefix = "new "
        ),

        // Client & Events
        CompletionItem(
            label = "client.on",
            insertText = "client.on('\$1', async (\$2) => {\n  \$3\n});",
            kind = CompletionKind.METHOD,
            detail = "(event: Events, listener: Function) => Client",
            documentation = "Listens for Discord gateway events like 'ready', 'interactionCreate', or 'messageCreate'.",
            category = "Client",
            triggerPrefix = "client."
        ),
        CompletionItem(
            label = "client.login",
            insertText = "client.login(process.env.DISCORD_TOKEN);",
            kind = CompletionKind.METHOD,
            detail = "(token?: string) => Promise<string>",
            documentation = "Authenticates the Discord bot with the Discord gateway via WebSocket.",
            category = "Client",
            triggerPrefix = "client."
        ),
        CompletionItem(
            label = "client.user.setActivity",
            insertText = "client.user.setActivity('\$1', { type: ActivityType.Custom });",
            kind = CompletionKind.METHOD,
            detail = "(name: string, options?: ActivityOptions) => Presence",
            documentation = "Sets the bot's display status activity banner (Playing, Streaming, Listening, Watching, Custom).",
            category = "Client",
            triggerPrefix = "client.user."
        ),
        CompletionItem(
            label = "client.user.setPresence",
            insertText = "client.user.setPresence({ status: 'online', activities: [{ name: '\$1' }] });",
            kind = CompletionKind.METHOD,
            detail = "(data: PresenceData) => Presence",
            documentation = "Sets the full bot presence status ('online', 'idle', 'dnd', 'invisible').",
            category = "Client",
            triggerPrefix = "client.user."
        ),
        CompletionItem(
            label = "client.guilds.cache.get",
            insertText = "client.guilds.cache.get('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(id: Snowflake) => Guild | undefined",
            documentation = "Retrieves a cached server/guild by its unique snowflake ID.",
            category = "Client",
            triggerPrefix = "client.guilds."
        ),
        CompletionItem(
            label = "client.channels.cache.get",
            insertText = "client.channels.cache.get('\$1')",
            kind = CompletionKind.METHOD,
            detail = "(id: Snowflake) => Channel | undefined",
            documentation = "Retrieves a cached channel by its snowflake ID.",
            category = "Client",
            triggerPrefix = "client.channels."
        ),

        // Gateway Intents
        CompletionItem(
            label = "GatewayIntentBits.Guilds",
            insertText = "GatewayIntentBits.Guilds",
            kind = CompletionKind.CONSTANT,
            detail = "GatewayIntentBits.Guilds = 1 << 0",
            documentation = "Enables receiving guild-related events such as channel creates, role updates, and guild cache.",
            category = "Gateway"
        ),
        CompletionItem(
            label = "GatewayIntentBits.GuildMessages",
            insertText = "GatewayIntentBits.GuildMessages",
            kind = CompletionKind.CONSTANT,
            detail = "GatewayIntentBits.GuildMessages = 1 << 9",
            documentation = "Enables receiving message events inside server text channels.",
            category = "Gateway"
        ),
        CompletionItem(
            label = "GatewayIntentBits.MessageContent",
            insertText = "GatewayIntentBits.MessageContent",
            kind = CompletionKind.CONSTANT,
            detail = "GatewayIntentBits.MessageContent = 1 << 15 (Privileged)",
            documentation = "Privileged intent required to read message text/content for prefix commands.",
            category = "Gateway"
        ),
        CompletionItem(
            label = "GatewayIntentBits.GuildMembers",
            insertText = "GatewayIntentBits.GuildMembers",
            kind = CompletionKind.CONSTANT,
            detail = "GatewayIntentBits.GuildMembers = 1 << 1 (Privileged)",
            documentation = "Privileged intent required for guild member join/leave events and member cache.",
            category = "Gateway"
        ),

        // Message Methods & Properties
        CompletionItem(
            label = "message.reply",
            insertText = "await message.reply('\$1');",
            kind = CompletionKind.METHOD,
            detail = "(options: string | MessagePayload) => Promise<Message>",
            documentation = "Replies directly to the user's message with an inline quote reference.",
            category = "Message",
            triggerPrefix = "message."
        ),
        CompletionItem(
            label = "message.channel.send",
            insertText = "await message.channel.send('\$1');",
            kind = CompletionKind.METHOD,
            detail = "(options: string | MessagePayload) => Promise<Message>",
            documentation = "Sends a message into the channel where the message was received.",
            category = "Message",
            triggerPrefix = "message.channel."
        ),
        CompletionItem(
            label = "message.delete",
            insertText = "await message.delete();",
            kind = CompletionKind.METHOD,
            detail = "() => Promise<Message>",
            documentation = "Deletes this message from the channel (requires Manage Messages permission).",
            category = "Message",
            triggerPrefix = "message."
        ),
        CompletionItem(
            label = "message.react",
            insertText = "await message.react('👍');",
            kind = CompletionKind.METHOD,
            detail = "(emoji: EmojiIdentifierResolvable) => Promise<MessageReaction>",
            documentation = "Adds an emoji reaction to the message.",
            category = "Message",
            triggerPrefix = "message."
        ),
        CompletionItem(
            label = "message.author",
            insertText = "message.author",
            kind = CompletionKind.PROPERTY,
            detail = "User",
            documentation = "The User who authored the message.",
            category = "Message",
            triggerPrefix = "message."
        ),
        CompletionItem(
            label = "message.content",
            insertText = "message.content",
            kind = CompletionKind.PROPERTY,
            detail = "string",
            documentation = "The raw string content of the message.",
            category = "Message",
            triggerPrefix = "message."
        ),

        // General JS/TS Boilerplates & Snippets
        CompletionItem(
            label = "try...catch",
            insertText = "try {\n  \$1\n} catch (error) {\n  console.error('[ERROR]', error);\n}",
            kind = CompletionKind.SNIPPET,
            detail = "try / catch error handling block",
            documentation = "Safely wraps synchronous or asynchronous logic to catch unexpected runtime errors.",
            category = "Snippets"
        ),
        CompletionItem(
            label = "console.log",
            insertText = "console.log('\$1');",
            kind = CompletionKind.METHOD,
            detail = "(...data: any[]) => void",
            documentation = "Outputs formatted logs to the BotStudio interactive terminal console.",
            category = "Built-in"
        ),
        CompletionItem(
            label = "process.env",
            insertText = "process.env.\$1",
            kind = CompletionKind.PROPERTY,
            detail = "process.env: NodeJS.ProcessEnv",
            documentation = "Accesses environment variables such as DISCORD_TOKEN, CLIENT_ID, or GUILD_ID.",
            category = "Built-in"
        ),
        CompletionItem(
            label = "JSON.stringify",
            insertText = "JSON.stringify(\$1, null, 2)",
            kind = CompletionKind.METHOD,
            detail = "(value: any, replacer?: any, space?: number) => string",
            documentation = "Converts a JavaScript object or array into a pretty-printed JSON string.",
            category = "Built-in"
        )
    )

    val DISCORD_PY_ITEMS = listOf(
        CompletionItem(
            label = "bot.tree.command",
            insertText = "@bot.tree.command(name=\"\$1\", description=\"\$2\")\nasync def \$1(interaction: discord.Interaction):\n    await interaction.response.send_message(\"Hello!\")",
            kind = CompletionKind.METHOD,
            detail = "@bot.tree.command(name: str, description: str)",
            documentation = "Registers a modern application slash command in Discord.py 2.x.",
            category = "Discord.py",
            triggerPrefix = "@bot."
        ),
        CompletionItem(
            label = "interaction.response.send_message",
            insertText = "await interaction.response.send_message(\"\$1\", ephemeral=True)",
            kind = CompletionKind.METHOD,
            detail = "(content: str, ephemeral: bool = False)",
            documentation = "Sends an initial response to an interaction in Python.",
            category = "Discord.py",
            triggerPrefix = "interaction.response."
        ),
        CompletionItem(
            label = "discord.Embed",
            insertText = "embed = discord.Embed(\n    title=\"\$1\",\n    description=\"\$2\",\n    color=discord.Color.blurple()\n)\nembed.add_field(name=\"Field\", value=\"Value\", inline=True)\nawait interaction.response.send_message(embed=embed)",
            kind = CompletionKind.CLASS,
            detail = "discord.Embed(title, description, color)",
            documentation = "Creates a rich embed in Discord.py.",
            category = "Discord.py"
        ),
        CompletionItem(
            label = "discord.ui.View",
            insertText = "class MyView(discord.ui.View):\n    @discord.ui.button(label=\"Click\", style=discord.ButtonStyle.primary)\n    async def button_callback(self, interaction: discord.Interaction, button: discord.ui.Button):\n        await interaction.response.send_message(\"Clicked!\")",
            kind = CompletionKind.CLASS,
            detail = "class View(*items, timeout=180.0)",
            documentation = "A container for interactive components (buttons and selects) in Discord.py.",
            category = "Discord.py"
        )
    )

    val SIGNATURE_HELPS = mapOf(
        "reply" to SignatureHelp(
            functionName = "interaction.reply",
            signature = "interaction.reply({ content, embeds, components, ephemeral, files })",
            parameters = listOf(
                ParameterInfo("content", "string", "The text message content to send.", isOptional = true),
                ParameterInfo("embeds", "EmbedBuilder[]", "Array of rich embed cards to display.", isOptional = true),
                ParameterInfo("components", "ActionRowBuilder[]", "Array of interactive button or select rows.", isOptional = true),
                ParameterInfo("ephemeral", "boolean", "If true, only the invoking user sees the response.", isOptional = true)
            ),
            activeParameterIndex = 0,
            documentation = "Sends an initial response to the slash command."
        ),
        "deferReply" to SignatureHelp(
            functionName = "interaction.deferReply",
            signature = "interaction.deferReply({ ephemeral })",
            parameters = listOf(
                ParameterInfo("ephemeral", "boolean", "Whether the deferred thinking response is ephemeral.", isOptional = true)
            ),
            activeParameterIndex = 0,
            documentation = "Acknowledges interaction with 'Bot is thinking...'. Extends timeout to 15m."
        ),
        "setTitle" to SignatureHelp(
            functionName = "embed.setTitle",
            signature = "embed.setTitle(title)",
            parameters = listOf(
                ParameterInfo("title", "string", "Title text (max 256 characters).", isOptional = false)
            ),
            activeParameterIndex = 0,
            documentation = "Sets the title of the embed."
        ),
        "setDescription" to SignatureHelp(
            functionName = "embed.setDescription",
            signature = "embed.setDescription(description)",
            parameters = listOf(
                ParameterInfo("description", "string", "Main text body of the embed (max 4096 chars).", isOptional = false)
            ),
            activeParameterIndex = 0,
            documentation = "Sets the description of the embed."
        ),
        "addFields" to SignatureHelp(
            functionName = "embed.addFields",
            signature = "embed.addFields(...fields: { name, value, inline }[])",
            parameters = listOf(
                ParameterInfo("fields", "APIEmbedField[]", "Field items with name and value strings.", isOptional = false)
            ),
            activeParameterIndex = 0,
            documentation = "Adds grid fields to the embed (up to 25 total)."
        ),
        "getString" to SignatureHelp(
            functionName = "interaction.options.getString",
            signature = "interaction.options.getString(name, required)",
            parameters = listOf(
                ParameterInfo("name", "string", "Name of the option parameter defined in command.", isOptional = false),
                ParameterInfo("required", "boolean", "Throws error if not supplied by user.", isOptional = true)
            ),
            activeParameterIndex = 0,
            documentation = "Retrieves a string option value."
        ),
        "setLabel" to SignatureHelp(
            functionName = "button.setLabel",
            signature = "button.setLabel(label)",
            parameters = listOf(
                ParameterInfo("label", "string", "Text displayed on the button (max 80 chars).", isOptional = false)
            ),
            activeParameterIndex = 0,
            documentation = "Sets the button's visible label."
        ),
        "setStyle" to SignatureHelp(
            functionName = "button.setStyle",
            signature = "button.setStyle(style: ButtonStyle)",
            parameters = listOf(
                ParameterInfo("style", "ButtonStyle", "Primary (1), Secondary (2), Success (3), Danger (4), Link (5)", isOptional = false)
            ),
            activeParameterIndex = 0,
            documentation = "Sets the color and style variant of the button."
        )
    )
}
