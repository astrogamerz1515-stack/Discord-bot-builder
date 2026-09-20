package com.example.ui.editor

data class Snippet(
    val title: String,
    val description: String,
    val codeForJs: String,
    val codeForPy: String
)

object CodeSnippets {
    val ALL_SNIPPETS = listOf(
        Snippet(
            title = "Slash Command",
            description = "Register and reply to a slash command",
            codeForJs = """
// Slash Command: /userinfo
if (interaction.commandName === 'userinfo') {
  const user = interaction.options.getUser('target') || interaction.user;
  await interaction.reply({
    content: `User: **${'$'}{user.tag}** (ID: ${'$'}{user.id})`,
    ephemeral: true
  });
}
""",
            codeForPy = """
@bot.tree.command(name="userinfo", description="Get user account information")
async def userinfo(interaction: discord.Interaction, member: discord.Member = None):
    target = member or interaction.user
    await interaction.response.send_message(
        f"User: **{target.name}** (ID: {target.id})",
        ephemeral=True
    )
"""
        ),
        Snippet(
            title = "Rich Embed",
            description = "Discord Embed with color, fields & footer",
            codeForJs = """
const embed = new EmbedBuilder()
  .setTitle('🛡️ Security Report')
  .setDescription('Automated moderation status for current guild.')
  .setColor(0x5865F2)
  .addFields(
    { name: 'Protected Channels', value: '14', inline: true },
    { name: 'Raid Shield', value: 'Active', inline: true }
  )
  .setFooter({ text: 'BotStudio Security' })
  .setTimestamp();

await interaction.reply({ embeds: [embed] });
""",
            codeForPy = """
embed = discord.Embed(
    title="🛡️ Security Report",
    description="Automated moderation status for current guild.",
    color=discord.Color.blurple()
)
embed.add_field(name="Protected Channels", value="14", inline=True)
embed.add_field(name="Raid Shield", value="Active", inline=True)
embed.set_footer(text="BotStudio Security")
await interaction.response.send_message(embed=embed)
"""
        ),
        Snippet(
            title = "Action Row Buttons",
            description = "Row of interactive Discord buttons",
            codeForJs = """
const row = new ActionRowBuilder().addComponents(
  new ButtonBuilder()
    .setCustomId('btn_confirm')
    .setLabel('Confirm')
    .setStyle(ButtonStyle.Success)
    .setEmoji('✅'),
  new ButtonBuilder()
    .setCustomId('btn_cancel')
    .setLabel('Cancel')
    .setStyle(ButtonStyle.Danger)
    .setEmoji('✖️')
);

await interaction.reply({ content: 'Please confirm your action:', components: [row] });
""",
            codeForPy = """
class ConfirmView(discord.ui.View):
    def __init__(self):
        super().__init__(timeout=30)

    @discord.ui.button(label="Confirm", style=discord.ButtonStyle.success, emoji="✅")
    async def confirm(self, interaction: discord.Interaction, button: discord.ui.Button):
        await interaction.response.send_message("Confirmed!", ephemeral=True)

    @discord.ui.button(label="Cancel", style=discord.ButtonStyle.danger, emoji="✖️")
    async def cancel(self, interaction: discord.Interaction, button: discord.ui.Button):
        await interaction.response.send_message("Cancelled.", ephemeral=True)

await interaction.response.send_message("Please confirm:", view=ConfirmView())
"""
        ),
        Snippet(
            title = "Modal Form",
            description = "Interactive Discord pop-up modal dialog",
            codeForJs = """
const modal = new ModalBuilder()
  .setCustomId('feedback_modal')
  .setTitle('Submit Server Feedback');

const feedbackInput = new TextInputBuilder()
  .setCustomId('feedback_text')
  .setLabel('Your suggestions:')
  .setStyle(TextInputStyle.Paragraph);

modal.addComponents(new ActionRowBuilder().addComponents(feedbackInput));
await interaction.showModal(modal);
""",
            codeForPy = """
class FeedbackModal(discord.ui.Modal, title="Submit Server Feedback"):
    feedback = discord.ui.TextInput(
        label="Your suggestions:",
        style=discord.TextStyle.paragraph
    )

    async def on_submit(self, interaction: discord.Interaction):
        await interaction.response.send_message(f"Thank you: {self.feedback.value}", ephemeral=True)

await interaction.response.send_modal(FeedbackModal())
"""
        ),
        Snippet(
            title = "Event Listener",
            description = "Handle guild member join event",
            codeForJs = """
client.on('guildMemberAdd', async (member) => {
  const welcomeChannel = member.guild.channels.cache.find(c => c.name === 'welcome');
  if (welcomeChannel) {
    welcomeChannel.send(`Welcome to the server, ${'$'}{member}! 🎉`);
  }
});
""",
            codeForPy = """
@bot.event
async def on_member_join(member):
    channel = discord.utils.get(member.guild.text_channels, name="welcome")
    if channel:
        await channel.send(f"Welcome to the server, {member.mention}! 🎉")
"""
        ),
        Snippet(
            title = "Gemini AI Slash Command",
            description = "Ask Gemini 3.5 Flash inside Discord with streaming reply",
            codeForJs = """
// Slash Command: /ask <prompt>
const { GoogleGenerativeAI } = require('@google/generative-ai');
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: 'gemini-3.5-flash' });

if (interaction.commandName === 'ask') {
  await interaction.deferReply();
  const prompt = interaction.options.getString('prompt');
  const result = await model.generateContent(prompt);
  await interaction.editReply(result.response.text().slice(0, 2000));
}
""",
            codeForPy = """
import google.generativeai as genai
genai.configure(api_key=os.environ.get("GEMINI_API_KEY"))
model = genai.GenerativeModel("gemini-3.5-flash")

@bot.tree.command(name="ask", description="Ask Gemini AI a question")
async def ask(interaction: discord.Interaction, prompt: str):
    await interaction.response.defer()
    response = model.generate_content(prompt)
    await interaction.followup.send(response.text[:2000])
"""
        ),
        Snippet(
            title = "Automated Unit Test Suite",
            description = "Jest / Pytest mock interaction test suite",
            codeForJs = """
describe('Bot Slash Command Suite', () => {
  test('should successfully execute ping command', async () => {
    const mockReply = jest.fn();
    const interaction = {
      commandName: 'ping',
      reply: mockReply
    };
    await handleCommand(interaction);
    expect(mockReply).toHaveBeenCalled();
  });
});
""",
            codeForPy = """
import pytest
from unittest.mock import AsyncMock, MagicMock

@pytest.mark.asyncio
async def test_ping_command():
    interaction = MagicMock()
    interaction.response.send_message = AsyncMock()
    await ping(interaction)
    interaction.response.send_message.assert_called_once()
"""
        )
    )
}
