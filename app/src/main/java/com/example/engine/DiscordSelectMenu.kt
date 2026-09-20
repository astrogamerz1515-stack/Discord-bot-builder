package com.example.engine

data class DiscordSelectMenuOption(
    val label: String,
    val value: String,
    val description: String = "",
    val emoji: String = ""
)

data class DiscordSimulatorSelectMenu(
    val customId: String,
    val placeholder: String = "Select an option...",
    val options: List<DiscordSelectMenuOption> = emptyList(),
    val disabled: Boolean = false
)
