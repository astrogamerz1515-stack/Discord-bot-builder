package com.example.ui.simulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DiscordSimulatorButton
import com.example.engine.DiscordSimulatorEmbed
import com.example.engine.DiscordSimulatorMessage
import com.example.ui.BotStudioViewModel
import com.example.ui.theme.DiscordBackground
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordDarker
import com.example.ui.theme.DiscordElevated
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordHover
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordYellow

@Composable
fun DiscordSimulatorScreen(viewModel: BotStudioViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val messages by viewModel.runtimeEngine.simulatorMessages.collectAsState()
    val isTyping by viewModel.runtimeEngine.isTyping.collectAsState()
    val isRunning by viewModel.runtimeEngine.isRunning.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
    ) {
        // Channel Header Bar
        Surface(
            color = DiscordElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#",
                        color = DiscordTextMuted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "bot-testing",
                            color = DiscordTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRunning) "🟢 ${project?.name ?: "Bot"} is Online" else "🔴 Bot is Offline",
                            color = if (isRunning) DiscordGreen else DiscordRed,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.runtimeEngine.clearSimulator() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Chat",
                        tint = DiscordTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Discord Chat Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                DiscordMessageItem(
                    message = msg,
                    onButtonClick = { buttonId ->
                        project?.let { viewModel.runtimeEngine.handleButtonClick(buttonId, it) }
                    },
                    onSelectMenuChange = { menuId, value ->
                        project?.let { viewModel.runtimeEngine.handleSelectMenuChange(menuId, value, it) }
                    }
                )
            }
        }

        // Typing Indicator
        AnimatedVisibility(visible = isTyping) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 ${project?.name ?: "Bot"} is typing...",
                    color = DiscordTextMuted,
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // Quick Command Suggestions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DiscordDarker)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf("/ping", "/embed", "/userinfo", "/help", "/roll", "${project?.prefix ?: "!"}echo Hello World")
            suggestions.forEach { cmd ->
                Surface(
                    color = DiscordHover,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        project?.let { viewModel.runtimeEngine.handleSimulatorUserMessage(cmd, it) }
                    }
                ) {
                    Text(
                        text = cmd,
                        color = DiscordBlurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Chat Input Bar
        Surface(
            color = DiscordElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = {
                        Text(
                            "Message #bot-testing (Try /ping, /embed)...",
                            color = DiscordTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = DiscordTextPrimary,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputMessage.isNotBlank()) {
                                project?.let { viewModel.runtimeEngine.handleSimulatorUserMessage(inputMessage, it) }
                                inputMessage = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordHover,
                        focusedContainerColor = DiscordBackground,
                        unfocusedContainerColor = DiscordBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("input_simulator_message")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            project?.let { viewModel.runtimeEngine.handleSimulatorUserMessage(inputMessage, it) }
                            inputMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DiscordBlurple, RoundedCornerShape(8.dp))
                        .testTag("btn_send_simulator_message")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DiscordMessageItem(
    message: DiscordSimulatorMessage,
    onButtonClick: (String) -> Unit,
    onSelectMenuChange: (String, String) -> Unit = { _, _ -> }
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var selectedOptionLabel by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (message.isBot) DiscordBlurple else DiscordGreen,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.authorName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Author header & Bot Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.authorName,
                    color = if (message.isBot) DiscordBlurple else DiscordTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (message.isBot) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = DiscordBlurple,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "BOT",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.timestamp,
                    color = DiscordTextMuted,
                    fontSize = 11.sp
                )
            }

            // Message text content
            if (message.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message.content,
                    color = DiscordTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Rich Discord Embed
            message.embed?.let { embed ->
                Spacer(modifier = Modifier.height(8.dp))
                DiscordEmbedCard(embed = embed)
            }

            // Interactive Action Row Buttons
            if (message.buttons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    message.buttons.forEach { btn ->
                        val btnBgColor = when (btn.style) {
                            "SUCCESS" -> DiscordGreen
                            "DANGER" -> DiscordRed
                            "SECONDARY" -> DiscordHover
                            else -> DiscordBlurple
                        }
                        val btnTextColor = if (btn.style == "SUCCESS") Color.Black else Color.White

                        Button(
                            onClick = { onButtonClick(btn.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = btnBgColor),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (btn.emoji.isNotBlank()) {
                                Text(btn.emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = btn.label,
                                color = btnTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Interactive Action Row Select Menu (Drop-down)
            message.selectMenu?.let { menu ->
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    Surface(
                        color = DiscordDarker,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DiscordHover),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMenuExpanded = !isMenuExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedOptionLabel ?: menu.placeholder,
                                color = if (selectedOptionLabel != null) DiscordTextPrimary else DiscordTextMuted,
                                fontSize = 13.sp
                            )
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = DiscordTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(DiscordDarker)
                    ) {
                        menu.options.forEach { opt ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (opt.emoji.isNotBlank()) {
                                                Text(opt.emoji, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = opt.label,
                                                color = DiscordTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (opt.description.isNotBlank()) {
                                            Text(
                                                text = opt.description,
                                                color = DiscordTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    selectedOptionLabel = opt.label
                                    onSelectMenuChange(menu.customId, opt.value)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscordEmbedCard(embed: DiscordSimulatorEmbed) {
    val embedColor = try {
        Color(android.graphics.Color.parseColor(embed.colorHex))
    } catch (e: Exception) {
        DiscordBlurple
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiscordDarker, RoundedCornerShape(4.dp))
    ) {
        // Vertical colored bar on left
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (embed.fields.isNotEmpty()) 140.dp else 90.dp)
                .background(embedColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
        )

        Column(
            modifier = Modifier
                .padding(12.dp)
                .weight(1f)
        ) {
            // Author
            if (embed.authorName.isNotBlank()) {
                Text(
                    text = embed.authorName,
                    color = DiscordTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Title
            if (embed.title.isNotBlank()) {
                Text(
                    text = embed.title,
                    color = DiscordTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Description
            if (embed.description.isNotBlank()) {
                Text(
                    text = embed.description,
                    color = DiscordTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fields
            if (embed.fields.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    embed.fields.forEach { (name, value) ->
                        Column {
                            Text(
                                text = name,
                                color = DiscordTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = value,
                                color = DiscordTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Footer
            if (embed.footerText.isNotBlank()) {
                Text(
                    text = embed.footerText,
                    color = DiscordTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
