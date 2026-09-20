package com.example.data.templates

import com.example.data.database.AppDatabase
import com.example.data.model.BotFile
import com.example.data.model.BotLanguage
import com.example.data.model.BotProject
import com.example.data.model.SavedEmbed
import com.example.data.model.TerminalLog

object BotTemplates {

    suspend fun seedDefaultProjects(database: AppDatabase) {
        val projectDao = database.botProjectDao()
        val fileDao = database.botFileDao()
        val logDao = database.terminalLogDao()
        val embedDao = database.savedEmbedDao()

        // 1. Default Primary: Node.js Discord.js v14 Bot
        val jsProjectId = projectDao.insertProject(
            BotProject(
                name = "Aegis Moderation & Utility",
                description = "Production-grade Discord bot in Node.js (discord.js v14) with slash commands, button interactions, and rich embeds.",
                language = BotLanguage.JAVASCRIPT.name,
                prefix = "!",
                botToken = "MTE4OTIzNDU2Nzg5MDEyMzQ1Ng.G-DiscordSecretBotTokenHere",
                clientId = "118923456789012345",
                status = "Online",
                activityType = "WATCHING",
                activityText = "over #general | /help",
                intentMessageContent = true,
                intentGuildMembers = true,
                intentPresences = true
            )
        )

        fileDao.insertFiles(
            listOf(
                BotFile(
                    projectId = jsProjectId,
                    filePath = "index.js",
                    isEntrypoint = true,
                    content = """// Aegis Discord Bot - discord.js v14
const { 
  Client, 
  GatewayIntentBits, 
  Partials, 
  Collection, 
  EmbedBuilder, 
  ActionRowBuilder, 
  ButtonBuilder, 
  ButtonStyle 
} = require('discord.js');
require('dotenv').config();

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
    GatewayIntentBits.GuildMembers,
    GatewayIntentBits.GuildPresences
  ],
  partials: [Partials.Channel, Partials.Message]
});

// Event: Bot Ready
client.once('ready', () => {
  console.log('-------------------------------------------');
  console.log(`[GATEWAY] Connected as: ${'$'}{client.user.tag}`);
  console.log(`[SERVERS] Serving ${'$'}{client.guilds.cache.size} servers`);
  console.log(`[SHARDS] Shard 0 heartbeat latency: ${'$'}{client.ws.ping}ms`);
  console.log('-------------------------------------------');
  
  client.user.setPresence({
    activities: [{ name: '/help | v2.4', type: 3 }],
    status: 'online'
  });
});

// Event: Interaction (Slash Commands & Buttons)
client.on('interactionCreate', async (interaction) => {
  if (interaction.isChatInputCommand()) {
    const { commandName } = interaction;

    if (commandName === 'ping') {
      const pingEmbed = new EmbedBuilder()
        .setTitle('🏓 Pong!')
        .setDescription(`API Gateway Latency: **${'$'}{client.ws.ping}ms**`)
        .setColor(0x5865F2)
        .addFields(
          { name: 'Uptime', value: '99.98%', inline: true },
          { name: 'Memory', value: '42.8 MB', inline: true }
        )
        .setFooter({ text: 'Aegis Sentinel System', iconURL: client.user.displayAvatarURL() })
        .setTimestamp();

      const row = new ActionRowBuilder().addComponents(
        new ButtonBuilder()
          .setCustomId('btn_refresh_ping')
          .setLabel('Refresh')
          .setStyle(ButtonStyle.Primary)
          .setEmoji('🔄'),
        new ButtonBuilder()
          .setCustomId('btn_docs')
          .setLabel('Bot Docs')
          .setStyle(ButtonStyle.Link)
          .setURL('https://discord.js.org')
      );

      await interaction.reply({ embeds: [pingEmbed], components: [row] });
    }

    if (commandName === 'embed') {
      const embed = new EmbedBuilder()
        .setTitle('✨ Discord Bot Studio Embed')
        .setDescription('This is a rich embed generated directly from the visual embed designer!')
        .setColor(0x57F287)
        .addFields(
          { name: 'Language', value: 'JavaScript (Node.js)', inline: true },
          { name: 'Framework', value: 'discord.js v14.15', inline: true }
        )
        .setThumbnail('https://cdn.discordapp.com/embed/avatars/0.png');

      await interaction.reply({ embeds: [embed] });
    }
  }

  // Button Click Handling
  if (interaction.isButton()) {
    if (interaction.customId === 'btn_refresh_ping') {
      await interaction.update({
        content: `⚡ Ping refreshed! Current WS latency: **${'$'}{client.ws.ping}ms**`,
        components: []
      });
    }
  }
});

// Event: Message Prefix Handler
client.on('messageCreate', async (message) => {
  if (message.author.bot) return;

  const prefix = process.env.PREFIX || '!';
  if (!message.content.startsWith(prefix)) return;

  const args = message.content.slice(prefix.length).trim().split(/ +/);
  const command = args.shift().toLowerCase();

  if (command === 'help') {
    message.reply({
      content: '🛡️ **Aegis Bot Commands:**\n`!help` - Show this menu\n`!ping` - Check latency\n`!server` - Server info'
    });
  }
});

client.login(process.env.DISCORD_TOKEN);
"""
                ),
                BotFile(
                    projectId = jsProjectId,
                    filePath = "package.json",
                    content = """{
  "name": "aegis-discord-bot",
  "version": "2.4.0",
  "description": "Discord Bot powered by BotStudio",
  "main": "index.js",
  "scripts": {
    "start": "node index.js",
    "dev": "nodemon index.js"
  },
  "dependencies": {
    "discord.js": "^14.15.3",
    "dotenv": "^16.4.5"
  }
}"""
                ),
                BotFile(
                    projectId = jsProjectId,
                    filePath = ".env",
                    content = """DISCORD_TOKEN=MTE4OTIzNDU2Nzg5MDEyMzQ1Ng.G-DiscordSecretBotTokenHere
CLIENT_ID=118923456789012345
GUILD_ID=987654321098765432
PREFIX=!
NODE_ENV=development"""
                ),
                BotFile(
                    projectId = jsProjectId,
                    filePath = "commands/ping.js",
                    content = """const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('ping')
    .setDescription('Replies with WebSocket latency and heartbeat'),
  async execute(interaction) {
    const ping = interaction.client.ws.ping;
    await interaction.reply({
      content: `🏓 Pong! Gateway Latency: **${'$'}{ping}ms**`,
      ephemeral: true
    });
  }
};"""
                ),
                BotFile(
                    projectId = jsProjectId,
                    filePath = "README.md",
                    content = """# Aegis Discord Bot
A modular, high performance Discord bot created in BotStudio.

## Quick Start
Run in BotStudio terminal:
```bash
npm install
node index.js
```
"""
                )
            )
        )

        // Seed initial terminal logs
        logDao.insertLog(
            TerminalLog(
                projectId = jsProjectId,
                text = "Welcome to Discord Bot Studio Terminal v2.4",
                type = "SYSTEM"
            )
        )
        logDao.insertLog(
            TerminalLog(
                projectId = jsProjectId,
                text = "Node.js v20.12.2 environment initialized. Project: aegis-discord-bot",
                type = "SYSTEM"
            )
        )
        logDao.insertLog(
            TerminalLog(
                projectId = jsProjectId,
                text = "$ node index.js",
                type = "INPUT"
            )
        )
        logDao.insertLog(
            TerminalLog(
                projectId = jsProjectId,
                text = "[GATEWAY] Identifying shard 0/1 with Discord Gateway API v10...",
                type = "STDOUT"
            )
        )
        logDao.insertLog(
            TerminalLog(
                projectId = jsProjectId,
                text = "[GATEWAY] Connected as: Aegis#1337 (ID: 118923456789012345)",
                type = "SUCCESS"
            )
        )
        logDao.insertLog(
            TerminalLog(
                projectId = jsProjectId,
                text = "[READY] Serving 3 servers | Heartbeat latency: 19ms",
                type = "SUCCESS"
            )
        )

        // Seed initial saved embed
        embedDao.insertEmbed(
            SavedEmbed(
                projectId = jsProjectId,
                title = "Welcome to Our Server!",
                description = "Make sure to read the rules and verify your account in #rules.",
                colorHex = "#5865F2",
                authorName = "Server Announcements",
                footerText = "Discord Bot Studio • Today at 12:00 PM"
            )
        )

        // 2. Second Template: Python discord.py 2.x
        val pyProjectId = projectDao.insertProject(
            BotProject(
                name = "Kitsune Python Bot",
                description = "Modern Python Discord bot using discord.py 2.3 with cogs, slash commands (app_commands), and interactive UI views.",
                language = BotLanguage.PYTHON.name,
                prefix = "?",
                botToken = "OTg3NjU0MzIxMDk4NzY1NDMyMQ.G-PythonBotTokenExample",
                clientId = "987654321098765432",
                status = "Online",
                activityType = "PLAYING",
                activityText = "?help | discord.py v2.3",
                intentMessageContent = true,
                intentGuildMembers = true,
                intentPresences = false
            )
        )

        fileDao.insertFiles(
            listOf(
                BotFile(
                    projectId = pyProjectId,
                    filePath = "bot.py",
                    isEntrypoint = true,
                    content = """import os
import discord
from discord import app_commands
from discord.ext import commands
from dotenv import load_dotenv

load_dotenv()

# Discord Intents configuration
intents = discord.Intents.default()
intents.message_content = True
intents.members = True

bot = commands.Bot(command_prefix="?", intents=intents)

# Interactive UI View with Buttons
class ActionView(discord.ui.View):
    def __init__(self):
        super().__init__(timeout=60)

    @discord.ui.button(label="Click Me!", style=discord.ButtonStyle.primary, emoji="🦊")
    async def button_callback(self, interaction: discord.Interaction, button: discord.ui.Button):
        button.disabled = True
        button.label = "Claimed!"
        await interaction.response.edit_message(content="🎉 You pressed the button!", view=self)

@bot.event
async def on_ready():
    print(f"Logged in as {bot.user.name} (ID: {bot.user.id})")
    print(f"Loaded discord.py version: {discord.__version__}")
    try:
        synced = await bot.tree.sync()
        print(f"Synced {len(synced)} slash commands globally")
    except Exception as e:
        print(f"Slash command sync error: {e}")

@bot.tree.command(name="ping", description="Check bot latency")
async def ping(interaction: discord.Interaction):
    latency = round(bot.latency * 1000)
    embed = discord.Embed(
        title="🏓 Pong!",
        description=f"Gateway WebSocket Latency: **{latency}ms**",
        color=discord.Color.brand_green()
    )
    embed.add_field(name="Language", value="Python 3.11", inline=True)
    embed.add_field(name="Library", value="discord.py", inline=True)
    await interaction.response.send_message(embed=embed, view=ActionView())

@bot.command()
async def hello(ctx):
    await ctx.reply(f"Hello {ctx.author.mention}! 🦊 I am running discord.py!")

if __name__ == "__main__":
    token = os.getenv("DISCORD_TOKEN")
    bot.run(token)
"""
                ),
                BotFile(
                    projectId = pyProjectId,
                    filePath = "requirements.txt",
                    content = """discord.py>=2.3.2
python-dotenv>=1.0.1
aiohttp>=3.9.0"""
                ),
                BotFile(
                    projectId = pyProjectId,
                    filePath = ".env",
                    content = """DISCORD_TOKEN=OTg3NjU0MzIxMDk4NzY1NDMyMQ.G-PythonBotTokenExample
CLIENT_ID=987654321098765432
PREFIX=?"""
                )
            )
        )
    }

    fun getTemplateForLanguage(language: BotLanguage, projectName: String): List<BotFile> {
        return when (language) {
            BotLanguage.JAVASCRIPT -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "index.js",
                    isEntrypoint = true,
                    content = """const { Client, GatewayIntentBits, EmbedBuilder } = require('discord.js');
require('dotenv').config();

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent
  ]
});

client.once('ready', () => {
  console.log(`[READY] Logged in as ${'$'}{client.user.tag}`);
});

client.on('messageCreate', (message) => {
  if (message.author.bot) return;
  if (message.content === '!ping') {
    message.reply(`🏓 Pong! Latency: ${'$'}{client.ws.ping}ms`);
  }
});

client.login(process.env.DISCORD_TOKEN);"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "package.json",
                    content = """{
  "name": "${projectName.lowercase().replace(" ", "-")}",
  "version": "1.0.0",
  "main": "index.js",
  "dependencies": {
    "discord.js": "^14.15.3",
    "dotenv": "^16.4.5"
  }
}"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = ".env",
                    content = "DISCORD_TOKEN=YOUR_BOT_TOKEN_HERE\nPREFIX=!"
                )
            )

            BotLanguage.PYTHON -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "bot.py",
                    isEntrypoint = true,
                    content = """import os
import discord
from discord.ext import commands
from dotenv import load_dotenv

load_dotenv()

intents = discord.Intents.default()
intents.message_content = True

bot = commands.Bot(command_prefix="!", intents=intents)

@bot.event
async def on_ready():
    print(f"Logged in as {bot.user} (ID: {bot.user.id})")

@bot.command()
async def ping(ctx):
    await ctx.send(f"🏓 Pong! Latency: {round(bot.latency * 1000)}ms")

bot.run(os.getenv("DISCORD_TOKEN"))"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "requirements.txt",
                    content = "discord.py>=2.3.2\npython-dotenv>=1.0.1"
                ),
                BotFile(
                    projectId = 0,
                    filePath = ".env",
                    content = "DISCORD_TOKEN=YOUR_BOT_TOKEN_HERE\nPREFIX=!"
                )
            )

            BotLanguage.TYPESCRIPT -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "src/index.ts",
                    isEntrypoint = true,
                    content = """import { Client, GatewayIntentBits, Message } from 'discord.js';
import * as dotenv from 'dotenv';
dotenv.config();

const client: Client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
  ]
});

client.once('ready', () => {
  console.log(`[TS-READY] Online as ${'$'}{client.user?.tag}`);
});

client.on('messageCreate', (msg: Message) => {
  if (msg.author.bot) return;
  if (msg.content === '!ping') {
    msg.reply(`🏓 Pong! ${'$'}{client.ws.ping}ms`);
  }
});

client.login(process.env.DISCORD_TOKEN);"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "package.json",
                    content = """{
  "name": "${projectName.lowercase().replace(" ", "-")}",
  "version": "1.0.0",
  "scripts": {
    "build": "tsc",
    "start": "ts-node src/index.ts"
  },
  "dependencies": {
    "discord.js": "^14.15.3",
    "dotenv": "^16.4.5"
  },
  "devDependencies": {
    "typescript": "^5.4.5",
    "ts-node": "^10.9.2",
    "@types/node": "^20.12.7"
  }
}"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = ".env",
                    content = "DISCORD_TOKEN=YOUR_BOT_TOKEN_HERE\nPREFIX=!"
                )
            )

            BotLanguage.RUST -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "src/main.rs",
                    isEntrypoint = true,
                    content = """use poise::serenity_prelude as serenity;

struct Data {} // User data, which is stored and accessible in all command invocations
type Error = Box<dyn std::error::Error + Send + Sync>;
type Context<'a> = poise::Context<'a, Data, Error>;

/// Responds with pong and gateway ping
#[poise::command(slash_command, prefix_command)]
async fn ping(ctx: Context<'_>) -> Result<(), Error> {
    ctx.say("🏓 Pong from Rust Serenity!").await?;
    Ok(())
}

#[tokio::main]
async fn main() {
    dotenv::dotenv().ok();
    let token = std::env::var("DISCORD_TOKEN").expect("missing DISCORD_TOKEN");
    let intents = serenity::GatewayIntents::non_privileged();

    let framework = poise::Framework::builder()
        .options(poise::FrameworkOptions {
            commands: vec![ping()],
            ..Default::default()
        })
        .setup(|ctx, _ready, framework| {
            Box::pin(async move {
                poise::builtins::register_globally(ctx, &framework.options().commands).await?;
                println!("Rust Discord Bot Connected!");
                Ok(Data {})
            })
        })
        .build();

    let client = serenity::ClientBuilder::new(token, intents)
        .framework(framework)
        .await;
    client.unwrap().start().await.unwrap();
}"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "Cargo.toml",
                    content = """[package]
name = "${projectName.lowercase().replace(" ", "-")}"
version = "0.1.0"
edition = "2021"

[dependencies]
poise = "0.6.1"
tokio = { version = "1.37", features = ["macros", "rt-multi-thread"] }
dotenv = "0.15.0"
serenity = "0.12.1"
"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = ".env",
                    content = "DISCORD_TOKEN=YOUR_BOT_TOKEN_HERE"
                )
            )

            BotLanguage.GO -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "main.go",
                    isEntrypoint = true,
                    content = """package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"

	"github.com/bwmarrin/discordgo"
)

func main() {
	token := os.Getenv("DISCORD_TOKEN")
	dg, err := discordgo.New("Bot " + token)
	if err != nil {
		fmt.Println("Error creating Discord session,", err)
		return
	}

	dg.AddHandler(messageCreate)
	dg.Identify.Intents = discordgo.IntentsGuildMessages

	err = dg.Open()
	if err != nil {
		fmt.Println("Error opening connection,", err)
		return
	}

	fmt.Println("Go Discord Bot is now running. Press CTRL-C to exit.")
	sc := make(chan os.Signal, 1)
	signal.Notify(sc, syscall.SIGINT, syscall.SIGTERM, os.Interrupt)
	<-sc

	dg.Close()
}

func messageCreate(s *discordgo.Session, m *discordgo.MessageCreate) {
	if m.Author.ID == s.State.User.ID {
		return
	}
	if m.Content == "!ping" {
		s.ChannelMessageSend(m.ChannelID, "🏓 Pong from DiscordGo!")
	}
}"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "go.mod",
                    content = """module ${projectName.lowercase().replace(" ", "-")}

go 1.22

require github.com/bwmarrin/discordgo v0.28.1
"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = ".env",
                    content = "DISCORD_TOKEN=YOUR_BOT_TOKEN_HERE\nPREFIX=!"
                )
            )

            BotLanguage.JAVA -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "src/Main.java",
                    isEntrypoint = true,
                    content = """import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main extends ListenerAdapter {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .addEventListeners(new Main())
            .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMessage().getContentRaw().equals("!ping")) {
            event.getChannel().sendMessage("🏓 Pong from JDA!").queue();
        }
    }
}"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "pom.xml",
                    content = """<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.discord</groupId>
  <artifactId>${projectName.lowercase().replace(" ", "-")}</artifactId>
  <version>1.0-SNAPSHOT</version>
  <dependencies>
    <dependency>
      <groupId>net.dv8tion</groupId>
      <artifactId>JDA</artifactId>
      <version>5.0.0-beta.24</version>
    </dependency>
  </dependencies>
</project>"""
                )
            )

            BotLanguage.CSHARP -> listOf(
                BotFile(
                    projectId = 0,
                    filePath = "Program.cs",
                    isEntrypoint = true,
                    content = """using Discord;
using Discord.WebSocket;
using System;
using System.Threading.Tasks;

class Program
{
    private DiscordSocketClient _client;

    public static Task Main(string[] args) => new Program().MainAsync();

    public async Task MainAsync()
    {
        _client = new DiscordSocketClient();
        _client.Log += Log;
        _client.MessageReceived += MessageReceivedAsync;

        var token = Environment.GetEnvironmentVariable("DISCORD_TOKEN");
        await _client.LoginAsync(TokenType.Bot, token);
        await _client.StartAsync();

        await Task.Delay(-1);
    }

    private Task Log(LogMessage msg)
    {
        Console.WriteLine(msg.ToString());
        return Task.CompletedTask;
    }

    private async Task MessageReceivedAsync(SocketMessage message)
    {
        if (message.Author.IsBot) return;
        if (message.Content == "!ping")
        {
            await message.Channel.SendMessageAsync("🏓 Pong from Discord.Net!");
        }
    }
}"""
                ),
                BotFile(
                    projectId = 0,
                    filePath = "Bot.csproj",
                    content = """<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net8.0</TargetFramework>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Discord.Net" Version="3.14.1" />
  </ItemGroup>
</Project>"""
                )
            )
        }
    }
}
