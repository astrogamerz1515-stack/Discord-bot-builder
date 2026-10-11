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

    val TYPESCRIPT_ITEMS = listOf(
        CompletionItem(
            label = "ChatInputCommandInteraction",
            insertText = "interaction: ChatInputCommandInteraction",
            kind = CompletionKind.INTERFACE,
            detail = "import { ChatInputCommandInteraction } from 'discord.js'",
            documentation = "TypeScript type for slash command interaction callback parameter.",
            category = "TypeScript"
        ),
        CompletionItem(
            label = "ButtonInteraction",
            insertText = "interaction: ButtonInteraction",
            kind = CompletionKind.INTERFACE,
            detail = "import { ButtonInteraction } from 'discord.js'",
            documentation = "TypeScript type for button component interaction callback parameter.",
            category = "TypeScript"
        ),
        CompletionItem(
            label = "interface BotCommand",
            insertText = "export interface BotCommand {\n  data: SlashCommandBuilder;\n  execute(interaction: ChatInputCommandInteraction): Promise<void>;\n}",
            kind = CompletionKind.INTERFACE,
            detail = "Standard Discord bot command structure interface",
            documentation = "TypeScript contract for modular slash command files.",
            category = "TypeScript"
        ),
        CompletionItem(
            label = "type SlashCommandExecute",
            insertText = "type SlashCommandExecute = (interaction: ChatInputCommandInteraction) => Promise<void>;",
            kind = CompletionKind.INTERFACE,
            detail = "Command execution function type alias",
            documentation = "Defines signature for slash command executor functions.",
            category = "TypeScript"
        ),
        CompletionItem(
            label = "Client<true>",
            insertText = "client: Client<true>",
            kind = CompletionKind.CLASS,
            detail = "Logged-in ready Discord client instance type",
            documentation = "Ensures client.user is non-null after 'ready' event.",
            category = "TypeScript"
        )
    )

    val JSON_ITEMS = listOf(
        CompletionItem(
            label = "\"scripts\": { ... }",
            insertText = "\"scripts\": {\n  \"start\": \"node index.js\",\n  \"dev\": \"nodemon index.js\"\n}",
            kind = CompletionKind.SNIPPET,
            detail = "package.json npm script commands",
            documentation = "Defines startup and developer execution commands.",
            category = "JSON"
        ),
        CompletionItem(
            label = "\"dependencies\": { ... }",
            insertText = "\"dependencies\": {\n  \"discord.js\": \"^14.16.0\",\n  \"dotenv\": \"^16.4.5\"\n}",
            kind = CompletionKind.SNIPPET,
            detail = "package.json production dependencies block",
            documentation = "Declares discord.js and dotenv packages.",
            category = "JSON"
        ),
        CompletionItem(
            label = "\"token\": \"...\"",
            insertText = "\"token\": \"YOUR_DISCORD_BOT_TOKEN\",",
            kind = CompletionKind.PROPERTY,
            detail = "Bot configuration token property",
            documentation = "Discord bot secret gateway authorization token.",
            category = "Config"
        ),
        CompletionItem(
            label = "\"clientId\": \"...\"",
            insertText = "\"clientId\": \"YOUR_APPLICATION_ID\",",
            kind = CompletionKind.PROPERTY,
            detail = "Discord Application / Client ID",
            documentation = "18-19 digit Discord application identifier.",
            category = "Config"
        ),
        CompletionItem(
            label = "\"prefix\": \"!\"",
            insertText = "\"prefix\": \"!\",",
            kind = CompletionKind.PROPERTY,
            detail = "Bot text command prefix",
            documentation = "Default prefix character for chat-based bot commands.",
            category = "Config"
        )
    )

    val MARKDOWN_ITEMS = listOf(
        CompletionItem(
            label = "# Project Title",
            insertText = "# 🤖 Discord Bot\n\nA modern, feature-rich bot built with BotStudio.\n\n## 🚀 Features\n- Slash commands\n- Embed builder\n\n## 🛠 Setup\n```bash\nnpm install\nnpm start\n```",
            kind = CompletionKind.SNIPPET,
            detail = "Complete README.md starter template",
            documentation = "Professional README template with setup and command docs.",
            category = "Markdown"
        ),
        CompletionItem(
            label = "```js Code Block",
            insertText = "```javascript\n$1\n```",
            kind = CompletionKind.SNIPPET,
            detail = "Fenced JavaScript code block",
            documentation = "Renders formatted syntax-highlighted code in Discord or GitHub.",
            category = "Markdown"
        ),
        CompletionItem(
            label = "| Table Template |",
            insertText = "| Command | Description | Permission |\n| :--- | :--- | :--- |\n| `/ping` | Check latency | Everyone |\n| `/clear` | Purge chat | Admin |",
            kind = CompletionKind.SNIPPET,
            detail = "Markdown documentation table",
            documentation = "Structured table for bot commands and arguments.",
            category = "Markdown"
        )
    )

    val HTML_CSS_ITEMS = listOf(
        CompletionItem(
            label = "<!DOCTYPE html>",
            insertText = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Bot Status Dashboard</title>\n  <style>\n    body { font-family: sans-serif; background: #2B2D31; color: white; margin: 0; padding: 20px; }\n  </style>\n</head>\n<body>\n  <h1>🤖 Bot Status: Online</h1>\n</body>\n</html>",
            kind = CompletionKind.SNIPPET,
            detail = "HTML5 Bot Dashboard Starter",
            documentation = "Complete dark-mode web status dashboard page.",
            category = "HTML"
        ),
        CompletionItem(
            label = "display: flex;",
            insertText = "display: flex;\njustify-content: center;\nalign-items: center;",
            kind = CompletionKind.PROPERTY,
            detail = "CSS Flexbox centering layout",
            documentation = "Standard responsive flexbox layout container.",
            category = "CSS"
        ),
        CompletionItem(
            label = "background: #5865F2",
            insertText = "background-color: #5865F2;\ncolor: #FFFFFF;\nborder-radius: 6px;",
            kind = CompletionKind.PROPERTY,
            detail = "Discord Blurple theme styling",
            documentation = "Authentic Discord branding colors for buttons and cards.",
            category = "CSS"
        )
    )

    val SHELL_ITEMS = listOf(
        CompletionItem(
            label = "npm install discord.js",
            insertText = "npm install discord.js dotenv",
            kind = CompletionKind.SNIPPET,
            detail = "Install discord.js v14 via npm",
            documentation = "Installs Discord API SDK and dotenv environment manager.",
            category = "Shell"
        ),
        CompletionItem(
            label = "pip install discord.py",
            insertText = "pip install discord.py python-dotenv aiohttp",
            kind = CompletionKind.SNIPPET,
            detail = "Install discord.py via pip",
            documentation = "Installs discord.py async engine and dependencies.",
            category = "Shell"
        ),
        CompletionItem(
            label = "export DISCORD_TOKEN=",
            insertText = "export DISCORD_TOKEN=\"\$1\"",
            kind = CompletionKind.SNIPPET,
            detail = "Export environment variable in terminal",
            documentation = "Sets bot token in current shell session.",
            category = "Shell"
        )
    )

    val SQL_ITEMS = listOf(
        CompletionItem(
            label = "CREATE TABLE IF NOT EXISTS",
            insertText = "CREATE TABLE IF NOT EXISTS users (\n  user_id VARCHAR(32) PRIMARY KEY,\n  points INT DEFAULT 0,\n  level INT DEFAULT 1,\n  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n);",
            kind = CompletionKind.SNIPPET,
            detail = "Create bot economy / profile table",
            documentation = "Standard schema for per-user Discord database records.",
            category = "SQL"
        ),
        CompletionItem(
            label = "SELECT * FROM users WHERE",
            insertText = "SELECT * FROM users WHERE user_id = $1 LIMIT 1;",
            kind = CompletionKind.SNIPPET,
            detail = "Query single user record",
            documentation = "Fetches saved user points and inventory data.",
            category = "SQL"
        ),
        CompletionItem(
            label = "INSERT INTO users VALUES",
            insertText = "INSERT INTO users (user_id, points) VALUES ($1, $2)\nON CONFLICT(user_id) DO UPDATE SET points = points + excluded.points;",
            kind = CompletionKind.SNIPPET,
            detail = "Upsert user balance or points",
            documentation = "Atomic insert or update for bot points.",
            category = "SQL"
        )
    )

    val KOTLIN_ITEMS = listOf(
        CompletionItem(
            label = "fun main()",
            insertText = "fun main() {\n    println(\"🤖 Bot started\")\n}",
            kind = CompletionKind.SNIPPET,
            detail = "Standard Kotlin main entry point",
            documentation = "Kotlin application main entry point.",
            category = "Kotlin"
        ),
        CompletionItem(
            label = "data class BotConfig",
            insertText = "data class BotConfig(\n    val token: String,\n    val prefix: String = \"!\",\n    val ownerId: Long = 0L\n)",
            kind = CompletionKind.CLASS,
            detail = "Data class for bot configuration",
            documentation = "Immutable configuration holder data class.",
            category = "Kotlin"
        ),
        CompletionItem(
            label = "suspend fun handleCommand",
            insertText = "suspend fun handleCommand(command: String, args: List<String>) {\n    when (command) {\n        \"ping\" -> println(\"Pong!\")\n        else -> println(\"Unknown command\")\n    }\n}",
            kind = CompletionKind.METHOD,
            detail = "Coroutine command handler function",
            documentation = "Asynchronous suspend function for routing bot commands.",
            category = "Kotlin"
        ),
        CompletionItem(
            label = "val kord = Kord(token)",
            insertText = "val kord = Kord(System.getenv(\"DISCORD_TOKEN\") ?: \"\")",
            kind = CompletionKind.SNIPPET,
            detail = "Initialize Kord Discord client",
            documentation = "Initializes idiomatic Kotlin Discord client (Kord).",
            category = "Kotlin"
        ),
        CompletionItem(
            label = "kord.on<MessageCreateEvent>",
            insertText = "kord.on<MessageCreateEvent> {\n    if (message.author?.isBot == true) return@on\n    if (message.content == \"!ping\") {\n        message.respond(\"🏓 Pong!\")\n    }\n}",
            kind = CompletionKind.EVENT,
            detail = "Kord event listener for messages",
            documentation = "Listens for message creation events in Kord.",
            category = "Kotlin"
        )
    )

    val LUA_ITEMS = listOf(
        CompletionItem(
            label = "local discordia = require('discordia')",
            insertText = "local discordia = require('discordia')\nlocal client = discordia.Client()",
            kind = CompletionKind.SNIPPET,
            detail = "Discordia Discord Bot Setup (Lua)",
            documentation = "Initializes the Discordia client in Lua.",
            category = "Lua"
        ),
        CompletionItem(
            label = "client:on('ready', function() ... end)",
            insertText = "client:on('ready', function()\n    print('🤖 Bot is ready as ' .. client.user.username)\nend)",
            kind = CompletionKind.EVENT,
            detail = "Discordia ready event handler",
            documentation = "Executes when the Lua bot successfully authenticates.",
            category = "Lua"
        ),
        CompletionItem(
            label = "client:on('messageCreate', function(message) ... end)",
            insertText = "client:on('messageCreate', function(message)\n    if message.author.bot then return end\n    if message.content == '!ping' then\n        message:reply('🏓 Pong!')\n    end\nend)",
            kind = CompletionKind.EVENT,
            detail = "Discordia message event handler",
            documentation = "Listens for incoming messages in Lua.",
            category = "Lua"
        ),
        CompletionItem(
            label = "message:reply('...')",
            insertText = "message:reply('$1')",
            kind = CompletionKind.METHOD,
            detail = "message:reply(content: string)",
            documentation = "Sends a reply to a message in Discordia.",
            category = "Lua"
        ),
        CompletionItem(
            label = "client:run('Bot ...')",
            insertText = "client:run('Bot ' .. (os.getenv('DISCORD_TOKEN') or ''))",
            kind = CompletionKind.METHOD,
            detail = "client:run(token: string)",
            documentation = "Connects the Discordia client to Discord gateway.",
            category = "Lua"
        )
    )

    val ENV_ITEMS = listOf(
        CompletionItem(
            label = "DISCORD_TOKEN=",
            insertText = "DISCORD_TOKEN=your_bot_token_here",
            kind = CompletionKind.PROPERTY,
            detail = "Discord Bot Authentication Token",
            documentation = "Secret bot token from Discord Developer Portal.",
            category = "Env"
        ),
        CompletionItem(
            label = "CLIENT_ID=",
            insertText = "CLIENT_ID=123456789012345678",
            kind = CompletionKind.PROPERTY,
            detail = "Discord Application ID",
            documentation = "Application / client ID used for slash command registration.",
            category = "Env"
        ),
        CompletionItem(
            label = "GUILD_ID=",
            insertText = "GUILD_ID=123456789012345678",
            kind = CompletionKind.PROPERTY,
            detail = "Development Server / Guild ID",
            documentation = "Server ID for instant testing of guild slash commands.",
            category = "Env"
        ),
        CompletionItem(
            label = "PREFIX=",
            insertText = "PREFIX=!",
            kind = CompletionKind.PROPERTY,
            detail = "Message command prefix",
            documentation = "Default prefix character for message commands.",
            category = "Env"
        )
    )

    /**
     * Resolves completion catalog matching the file type and language.
     */
    fun getItemsForFile(filePath: String): List<CompletionItem> {
        val lower = filePath.lowercase()
        return when {
            lower.endsWith(".py") -> DISCORD_PY_ITEMS
            lower.endsWith(".ts") || lower.endsWith(".tsx") -> TYPESCRIPT_ITEMS + DISCORD_JS_ITEMS
            lower.endsWith(".json") -> JSON_ITEMS
            lower.endsWith(".kt") || lower.endsWith(".kts") -> KOTLIN_ITEMS
            lower.endsWith(".lua") -> LUA_ITEMS
            lower.endsWith(".env") || lower.endsWith(".properties") -> ENV_ITEMS
            lower.endsWith(".md") || lower.endsWith(".markdown") -> MARKDOWN_ITEMS
            lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".css") -> HTML_CSS_ITEMS
            lower.endsWith(".sh") || lower.endsWith(".bash") -> SHELL_ITEMS
            lower.endsWith(".sql") -> SQL_ITEMS
            else -> DISCORD_JS_ITEMS
        }
    }

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
