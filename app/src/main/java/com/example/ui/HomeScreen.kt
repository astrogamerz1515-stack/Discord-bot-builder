package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotProject
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════
// COLOR PALETTE (exact hex tokens)
// ═══════════════════════════════════════════
object DiscordBotColors {
    val Background       = Color(0xFF0D0F14)
    val Surface          = Color(0xFF14171F)
    val SurfaceVariant   = Color(0xFF1C2029)
    val Border           = Color(0xFF252A35)
    val Primary          = Color(0xFF00E676)
    val PrimaryDark      = Color(0xFF00B85C)
    val Accent           = Color(0xFF3D7EFF)
    val AccentSoft       = Color(0xFF5B8DEF)
    val Purple           = Color(0xFFA78BFA)
    val Cyan             = Color(0xFF4DD0E1)
    val OnBackground     = Color(0xFFE6EDF3)
    val OnSurfaceVariant = Color(0xFF8B949E)
    val Warning          = Color(0xFFF0B429)
    val Error            = Color(0xFFFF5555)
}

val DiscordBotColorScheme = darkColorScheme(
    primary            = DiscordBotColors.Primary,
    onPrimary          = DiscordBotColors.Background,
    primaryContainer   = DiscordBotColors.PrimaryDark,
    secondary          = DiscordBotColors.Accent,
    onSecondary        = Color.White,
    tertiary           = DiscordBotColors.Purple,
    background         = DiscordBotColors.Background,
    onBackground       = DiscordBotColors.OnBackground,
    surface            = DiscordBotColors.Surface,
    onSurface          = DiscordBotColors.OnBackground,
    surfaceVariant     = DiscordBotColors.SurfaceVariant,
    onSurfaceVariant   = DiscordBotColors.OnSurfaceVariant,
    outline            = DiscordBotColors.Border,
    error              = DiscordBotColors.Error
)

// ═══════════════════════════════════════════
// DATA MODELS FOR UI
// ═══════════════════════════════════════════
data class BotUiItem(
    val id: Long,
    val name: String,
    val commandsCount: Int,
    val serversCount: Int,
    val emoji: String,
    val isOnline: Boolean,
    val originalProject: BotProject? = null
)

private val DefaultSampleBots = listOf(
    BotUiItem(
        id = 1L,
        name = "Aegis Moderation",
        commandsCount = 12,
        serversCount = 3,
        emoji = "🛡️",
        isOnline = true
    ),
    BotUiItem(
        id = 2L,
        name = "MusicMaster DJ",
        commandsCount = 18,
        serversCount = 7,
        emoji = "🎵",
        isOnline = true
    ),
    BotUiItem(
        id = 3L,
        name = "Echo Assistant",
        commandsCount = 6,
        serversCount = 1,
        emoji = "🤖",
        isOnline = false
    )
)

// ═══════════════════════════════════════════
// MAIN HOME SCREEN COMPOSABLE
// ═══════════════════════════════════════════
@Composable
fun HomeScreen(
    viewModel: BotStudioViewModel? = null,
    onBotClick: (BotUiItem) -> Unit = { bot ->
        bot.originalProject?.let { viewModel?.selectProject(it) }
    },
    onCreateBotClick: () -> Unit = {
        viewModel?.showNewProjectDialog?.value = true
    },
    onEditBotClick: (BotUiItem) -> Unit = { bot ->
        bot.originalProject?.let { viewModel?.selectProject(it) }
        viewModel?.setTab(AppTab.EDITOR)
    },
    onRunBotClick: (BotUiItem) -> Unit = { bot ->
        bot.originalProject?.let { viewModel?.selectProject(it) }
        viewModel?.startBotProcess()
    },
    onMoreBotClick: (BotUiItem) -> Unit = { bot ->
        bot.originalProject?.let { viewModel?.selectProject(it) }
        viewModel?.setTab(AppTab.BOT_CONFIG)
    },
    onNavItemSelected: (Int) -> Unit = {},
    onChipSelected: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var selectedChip by remember { mutableStateOf("＋ New Bot") }

    val rawProjects = viewModel?.allProjects?.collectAsState()?.value
    val isRuntimeRunning = viewModel?.runtimeEngine?.isRunning?.collectAsState()?.value ?: false

    val bots = remember(rawProjects, isRuntimeRunning) {
        if (!rawProjects.isNullOrEmpty()) {
            rawProjects.mapIndexed { index, p ->
                val emoji = when (index % 4) {
                    0 -> "🛡️"
                    1 -> "🎵"
                    2 -> "🤖"
                    else -> "⚡"
                }
                BotUiItem(
                    id = p.id,
                    name = p.name,
                    commandsCount = 12 + index * 4,
                    serversCount = 3 + index * 2,
                    emoji = emoji,
                    isOnline = if (index == 0) isRuntimeRunning else (p.status.equals("Online", ignoreCase = true)),
                    originalProject = p
                )
            }
        } else {
            DefaultSampleBots
        }
    }

    MaterialTheme(colorScheme = DiscordBotColorScheme) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_scaffold"),
            containerColor = DiscordBotColors.Background,
            topBar = {
                HomeTopAppBar(
                    title = "My Bots",
                    onMenuClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Menu opened")
                        }
                    },
                    onNotificationClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("No new notifications")
                        }
                    },
                    onProfileClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("User Profile: Active")
                        }
                    }
                )
            },
            bottomBar = {
                HomeBottomNavigationBar(
                    selectedIndex = selectedNavIndex,
                    onItemSelected = { index ->
                        selectedNavIndex = index
                        onNavItemSelected(index)
                        val name = when (index) {
                            0 -> "Home"
                            1 -> "Templates"
                            else -> "Settings"
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Navigated to $name")
                        }
                    }
                )
            },
            floatingActionButton = {
                HomeFloatingActionButton(
                    onClick = {
                        onCreateBotClick()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Opening Bot Creator...")
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp)
                ) { data ->
                    Surface(
                        color = DiscordBotColors.SurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DiscordBotColors.Border),
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = DiscordBotColors.OnBackground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DiscordBotColors.Background)
            ) {
                // 2. Chip Row below App Bar
                HomeChipRow(
                    selectedChip = selectedChip,
                    onChipTapped = { chipLabel ->
                        selectedChip = chipLabel
                        onChipSelected(chipLabel)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Chip selected: $chipLabel")
                        }
                        when (chipLabel) {
                            "＋ New Bot" -> onCreateBotClick()
                            "<> Editor" -> viewModel?.setTab(AppTab.EDITOR)
                            "▶ Simulator" -> viewModel?.setTab(AppTab.SIMULATOR)
                        }
                    }
                )

                // 3. Section Header
                HomeSectionHeader(botCount = bots.size)

                // 4. Bot List (LazyColumn)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("home_bot_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = bots, key = { it.id }) { bot ->
                        BotCard(
                            bot = bot,
                            onClick = { onBotClick(bot) },
                            onEditClick = { onEditBotClick(bot) },
                            onRunClick = {
                                onRunBotClick(bot)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Triggered Run for ${bot.name}")
                                }
                            },
                            onMoreClick = {
                                onMoreBotClick(bot)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Options for ${bot.name}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 1. TOP APP BAR
// ═══════════════════════════════════════════
@Composable
fun HomeTopAppBar(
    title: String,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("home_top_app_bar"),
        color = DiscordBotColors.Surface,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left hamburger icon (min 48dp touch target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(onClick = onMenuClick)
                        .testTag("btn_menu_hamburger"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = DiscordBotColors.OnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title: "My Bots" — 18sp SemiBold, monospace, #E6EDF3
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = DiscordBotColors.OnBackground,
                    modifier = Modifier.weight(1f)
                )

                // Right: Bell + 28dp circle avatar
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(onClick = onNotificationClick)
                        .testTag("btn_notifications"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = DiscordBotColors.OnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(onClick = onProfileClick)
                        .testTag("btn_user_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(DiscordBotColors.Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 1.dp bottom border (#252A35)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DiscordBotColors.Border)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 2. CHIP ROW
// ═══════════════════════════════════════════
private data class ChipSpec(
    val label: String,
    val defaultBg: Color,
    val defaultTextColor: Color,
    val isBold: Boolean,
    val border: BorderStroke?
)

@Composable
fun HomeChipRow(
    selectedChip: String,
    onChipTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        ChipSpec(
            label = "＋ New Bot",
            defaultBg = DiscordBotColors.Primary,
            defaultTextColor = DiscordBotColors.Background,
            isBold = true,
            border = null
        ),
        ChipSpec(
            label = "<> Editor",
            defaultBg = DiscordBotColors.Accent,
            defaultTextColor = Color.White,
            isBold = false,
            border = null
        ),
        ChipSpec(
            label = "▶ Simulator",
            defaultBg = DiscordBotColors.SurfaceVariant,
            defaultTextColor = DiscordBotColors.OnSurfaceVariant,
            isBold = false,
            border = BorderStroke(1.dp, DiscordBotColors.Border)
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("home_chip_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chip ->
            val isSelected = (selectedChip == chip.label)

            // When selected: "＋ New Bot" styling (bg #00E676, text #0D0F14 bold)
            val currentBg = if (isSelected) DiscordBotColors.Primary else chip.defaultBg
            val currentTextColor = if (isSelected) DiscordBotColors.Background else chip.defaultTextColor
            val currentBorder = if (isSelected) null else chip.border
            val currentBold = isSelected || chip.isBold

            Box(
                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .clickable { onChipTapped(chip.label) }
                    .testTag("chip_${chip.label.filter { it.isLetterOrDigit() }}"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = currentBg,
                    shape = RoundedCornerShape(8.dp),
                    border = currentBorder,
                    shadowElevation = 0.dp,
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip.label,
                            color = currentTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (currentBold) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 3. SECTION HEADER
// ═══════════════════════════════════════════
@Composable
fun HomeSectionHeader(
    botCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .testTag("home_section_header"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 6.dp dot in #00E676
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(DiscordBotColors.Primary, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Text: "Your Bots (3)" — 16sp SemiBold monospace #E6EDF3
        Text(
            text = "Your Bots ($botCount)",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = DiscordBotColors.OnBackground
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 1.dp horizontal divider filling remaining width, #252A35
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DiscordBotColors.Border)
        )
    }
}

// ═══════════════════════════════════════════
// 4. BOT CARD & LIST HELPERS
// ═══════════════════════════════════════════
@Composable
fun BotCard(
    bot: BotUiItem,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onRunClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bot_card_${bot.id}"),
        shape = RoundedCornerShape(10.dp),
        color = DiscordBotColors.Surface,
        border = BorderStroke(1.dp, DiscordBotColors.Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: 48.dp circle avatar (bg #1C2029, 1.dp #252A35 outline, centered emoji 22sp)
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = DiscordBotColors.SurfaceVariant,
                border = BorderStroke(1.dp, DiscordBotColors.Border),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bot.emoji,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Middle column (8.dp start padding from avatar)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Bot name: 15sp SemiBold monospace #E6EDF3
                Text(
                    text = bot.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = DiscordBotColors.OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle "12 commands • 3 servers": 12sp monospace #8B949E
                Text(
                    text = "${bot.commandsCount} commands • ${bot.serversCount} servers",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DiscordBotColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action row of 3 pill buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionPill(
                        text = "Edit",
                        backgroundColor = DiscordBotColors.Accent,
                        textColor = Color.White,
                        onClick = onEditClick,
                        testTag = "pill_edit_${bot.id}"
                    )
                    ActionPill(
                        text = "Run",
                        backgroundColor = DiscordBotColors.Primary,
                        textColor = DiscordBotColors.Background,
                        onClick = onRunClick,
                        testTag = "pill_run_${bot.id}"
                    )
                    ActionPill(
                        text = "⋯",
                        backgroundColor = DiscordBotColors.SurfaceVariant,
                        textColor = DiscordBotColors.OnSurfaceVariant,
                        onClick = onMoreClick,
                        testTag = "pill_more_${bot.id}"
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: 10.dp status dot
            StatusDot(
                isOnline = bot.isOnline,
                modifier = Modifier.testTag("status_dot_${bot.id}")
            )
        }
    }
}

// Action row pill button (height 26.dp, radius 6.dp, horizontal padding 10.dp, monospace 11sp)
// Wrapped in min 48.dp touch target box for accessibility
@Composable
fun ActionPill(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 0.dp,
            modifier = Modifier.height(26.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Right status dot:
// Online  → #00E676 with a 4.dp outer ring at 20% alpha
// Offline → #FF5555 (10.dp)
@Composable
fun StatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isOnline) {
            // 4.dp outer ring at 20% alpha
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(DiscordBotColors.Primary.copy(alpha = 0.20f), CircleShape)
            )
            // 10.dp inner dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(DiscordBotColors.Primary, CircleShape)
            )
        } else {
            // 10.dp offline dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(DiscordBotColors.Error, CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════
// 5. BOTTOM NAVIGATION BAR
// ═══════════════════════════════════════════
@Composable
fun HomeBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("home_bottom_nav_bar"),
        color = DiscordBotColors.Surface,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1.dp top border #252A35
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DiscordBotColors.Border)
                    .align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    Triple(Icons.Default.Home, "Home", "Home"),
                    Triple(Icons.Default.MenuBook, "Templates", "Templates"),
                    Triple(Icons.Default.Settings, "Settings", "Settings")
                )

                navItems.forEachIndexed { index, item ->
                    val isSelected = (selectedIndex == index)
                    val tintColor = if (isSelected) DiscordBotColors.Primary else DiscordBotColors.OnSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onItemSelected(index) }
                            .testTag("bottom_nav_item_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 2.dp top accent line in #00E676 for selected
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(2.dp)
                                        .background(DiscordBotColors.Primary, RoundedCornerShape(1.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Icon(
                                imageVector = item.first,
                                contentDescription = item.second,
                                tint = tintColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.third,
                                color = tintColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// 6. FLOATING ACTION BUTTON
// ═══════════════════════════════════════════
@Composable
fun HomeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = DiscordBotColors.Primary,
        contentColor = DiscordBotColors.Background,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        ),
        modifier = modifier
            .size(48.dp)
            .testTag("btn_fab_create_bot")
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create New Bot",
            tint = DiscordBotColors.Background,
            modifier = Modifier.size(24.dp)
        )
    }
}
