# 🤖 BotStudio - Discord Bot IDE, Visual Builder & 24/7 Background Runtime for Android

[![Build Android APK](https://github.com/your-username/botstudio/actions/workflows/build-apk.yml/badge.svg)](https://github.com/your-username/botstudio/actions/workflows/build-apk.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/krEBKByEYu)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

**BotStudio** is a full-featured, mobile IDE, Scratch-style Visual Builder, and real-time simulator built specifically for Discord bot developers on Android. Write, visually assemble, test, optimize, and deploy production Discord bots directly from your phone or tablet with **0% downtime background execution**.

---

## 📑 Master Feature Directory

1. [📱 24/7 Background Execution Engine (Even When App/Phone Is Closed)](#1--247-background-execution-engine)
2. [⚡ Slow Command Response Optimizer & Latency Diagnostics](#2--slow-command-response-optimizer--latency-diagnostics)
3. [🧩 Scratch-Style Visual Builder (Master List)](#3--scratch-style-visual-builder)
4. [💻 Real-Time Multi-Language Code Editor & IntelliSense](#4--real-time-multi-language-code-editor)
5. [💬 Interactive Discord Simulator & Sandbox](#5--interactive-discord-simulator)
6. [🎨 Visual Rich Embed Designer](#6--visual-rich-embed-designer)
7. [🗄️ Local Room Database & Key-Value Datastore](#7--local-room-database--key-value-datastore)
8. [🤖 AI Studio Gemini Assistant & Tools](#8--ai-studio-gemini-assistant)
9. [🛠️ Interactive Terminal & Gateway Console](#9--interactive-terminal--gateway-console)
10. [📦 Building the APK & CI/CD Workflow](#10--building-the-apk)

---

## 1. 📱 24/7 Background Execution Engine
### *The bot and your code run continuously even when the app is closed, swiped away from Recents, or the phone screen is turned off.*

* **Decoupled Application-Level Runtime**: The bot runtime runs inside a persistent `BotRuntimeManager.applicationScope` rather than an ephemeral `viewModelScope`. Closing the app or finishing activities does **not** terminate your bot's WebSocket or heartbeat loops.
* **Enterprise Android Foreground Service (`BotBackgroundService`)**:
  * Tapping **Run** starts a dedicated foreground service returning `START_STICKY`.
  * **Survives App Closure & Swiping from Recents**: Implements `onTaskRemoved()`. When the user swipes away the application from the Android app switcher, the foreground service and Gateway WebSocket remain connected with zero downtime.
  * **Survives Screen Sleep & Phone Lock**: Employs a low-power CPU `WakeLock` (`PARTIAL_WAKE_LOCK`) combined with a high-performance `WifiLock` (`WIFI_MODE_FULL_HIGH_PERF`), preventing Android from sleeping the CPU or throttling Wi-Fi radios when the phone screen turns off.
  * **Auto-Restart on Phone Reboot**: Built-in `BootCompletedReceiver` automatically detects if a bot was active and re-launches the bot service immediately when your device finishes booting.
  * **Ongoing Notification with Live Latency**: Displays bot status, live heartbeat Gateway ping in milliseconds, and a one-tap **Stop Bot** notification action.
  * **Battery Optimization Helper**: Direct shortcut to exempt BotStudio from Android Doze restrictions.

---

## 2. ⚡ Slow Command Response Optimizer & Latency Diagnostics
### *Eliminating the dreaded 3-second "The application did not respond" Discord timeout.*

### 🔍 Diagnosis Suite
* **Gateway Latency Monitor**: Real-time roundtrip ping from heartbeat dispatch to `HEARTBEAT_ACK`.
* **REST Latency Monitor**: Instantaneous HTTP latency benchmark against Discord's `GET /users/@me`.
* **Interaction Latency Badge**: Measures elapsed time from interaction arrival to initial response; flags commands taking `>500ms` with visual warning alerts.

### 🛡️ The #1 Fix: Immediate `deferReply()` / `deferUpdate()`
* **Auto-Defer Wrapper**: Injects `await interaction.deferReply()` (or `await interaction.response.defer()`) immediately on every command.
* **Defer for Components**: Uses `deferUpdate()` for buttons and string select menus.
* **Execution Flow Rule**: Do expensive database, REST API, or computing work **after** deferring, then reply via `editReply()` or `followUp()`.
* **Static Inspection**: Checks user code for DB/API calls executed before the first reply and emits inline warnings.

### 🌍 Hosting Location Optimizer
* **Region Latency Benchmark**:
  * **US East (Ashburn / us-ashburn-1)**: Under 50ms to Discord Gateway (Recommended Default).
  * High-latency warnings for far regions like India (>240ms) or Singapore (>210ms).
* **Region Proximity Selector**: Preset picker targeting Discord's core Ashburn data centers.

### 🔄 Event Loop & Concurrency Optimizations
* **Async I/O Enforcement**: Replaces synchronous `readFileSync` calls with `fs.promises.readFile`.
* **Worker Threads**: Offloads heavy CPU calculations to worker threads.
* **In-Memory Parsed Cache**: Caches parsed JSON objects in memory to avoid repeated parsing overhead.
* **Event Loop Lag Monitor**: Tracks event loop lag and alerts with red warnings if lag exceeds `50ms`.

### ❄️ Cold Starts Keep-Alive
* **Keep-Alive Server**: Integrated HTTP server with `/` health check endpoint.
* **Cold Start Detector**: 5-minute ping simulation preventing free-tier hosting (Render, Replit) from sleeping.

### 📡 Command Sync & REST Optimizations
* **Command Sync Mode Toggle**:
  * **Per-Guild Mode**: Instant 0-second command registration during development.
  * **Global Mode**: Production mode with 1-hour propagation caching.
  * **Publish Button**: One-tap toggle to promote guild commands to global production.
* **REST API Cache Layer**:
  * In-memory cache layer for users, guilds, and channels (minimum 1-minute TTL).
  * Auto-replaces `.fetch()` with `client.users.cache.get()` to eliminate redundant REST calls.
* **Gateway vs. Webhook Routing**:
  * Gateway WebSocket interactions (persistent, lowest latency) vs. HTTP Webhooks (stateless with extra roundtrip).

### 📋 6-Point Diagnostic Checklist
1. ☐ Is `deferReply()` being called immediately?
2. ☐ Where is the bot hosted? (Targeting US East Ashburn)
3. ☐ Is the bot sleeping? (Cold start detector & ping active)
4. ☐ Is the event loop blocked? (Lag under 50ms)
5. ☐ Are commands registered per-guild during development?
6. ☐ Are redundant REST API calls eliminated via cache?

---

## 3. 🧩 Scratch-Style Visual Builder
### *Scratch-style block-based visual programming for Discord bots.*

### 1. UX & Canvas Interactions
* **Pinch-to-Zoom & Two-Finger Pan**: Smooth canvas scaling from 0.5f to 2.5f.
* **Interactive Canvas Minimap**: Floating HUD in the top-right corner with mini colored block strips and real-time execution glow.
* **Canvas Zoom Controls**: Floating zoom controls (+, -, 100% reset) for fast one-tap navigation.
* **25-Step Undo / Redo Stack**: Command pattern history stack with snackbar feedback.
* **Trash Drop-Zone**: Pulsing red drop zone that dynamically appears when dragging blocks to easily discard or delete them.
* **Block Search Bar**: Real-time fuzzy filtering across all block titles and descriptions in the palette.
* **Category Pill Navigation**: Quick horizontal scrolling strip with category counts and signatures.
* **Pre-Built Starter Recipes**:
  * 🏓 **Ping-Pong Bot**: Basic message trigger and latency response.
  * 🛡️ **Auto-Moderator**: Invite-link filtering, message deletion, and user timeout.
  * 👋 **Welcome & Auto-Role**: Greets newcomers with rich embed and assigns Member role.
  * ⌨️ **Slash /help Command**: Modern slash command with interactive embed manual.
  * 🎫 **Interactive Ticket Bot**: Sends interactive button and creates private ticket channel.
  * 🌐 **REST API Quote Bot**: Fetches live quotes via HTTP GET and displays in embed.
* **Dual-Language Code Generator**:
  * **Python (`discord.py >= 2.3.0`)**: Clean asynchronous code with tree commands and tasks.
  * **JavaScript (`discord.js v14`)**: Modern ES modules with EmbedBuilder and ActionRowBuilder.
  * **Auto-Injected #1 Fix**: Automatically includes immediate `deferReply()` in all exported slash commands.
* **Interactive Discord Simulator**: Real-time preview with message input, simulated bot replies, interactive buttons, and step-by-step trace logs.

### 2. Complete Block Palette (7 Categories & 70+ Block Types)

#### ⚡ EVENTS (Blue `#3D7EFF`)
1. **When message received**: Triggers on any server message.
2. **When slash command run**: Modern slash command listener with custom name and description.
3. **When button clicked**: Listens for button component click matching `custom_id`.
4. **When select menu used**: Triggers when user selects an option in a dropdown menu.
5. **When modal submitted**: Triggers on modal form submission.
6. **When member joins**: Triggers when a new member joins the server.
7. **When member leaves**: Triggers when a member departs or is kicked.
8. **When reaction added**: Triggers on emoji reaction to a message.
9. **When reaction removed**: Triggers when an emoji reaction is removed.
10. **On bot ready / startup**: Runs once when the gateway connection opens.
11. **On scheduled time (cron-style)**: Recurring timer running on hourly/daily schedules.
12. **When voice state changes**: Triggers on voice channel join, leave, mute, or deafen.

#### 💬 MESSAGES (Green `#00E676`)
13. **Send message**: Dispatches plain text message to target channel.
14. **Reply to message**: Sends direct reply referencing author message.
15. **Send rich embed**: Full rich embed card with title, description, color, and fields.
16. **Send with buttons**: Action Row child with primary, success, or danger buttons.
17. **Send with select menu**: Dropdown select menu with configurable placeholder and options.
18. **Send DM to user**: Sends private direct message to the author.
19. **Edit message**: Updates content of previously sent message.
20. **Delete message**: Removes triggering or target message.
21. **Pin message**: Pins important message to channel header.
22. **Add reaction**: Adds emoji reaction to message.
23. **Send typing indicator**: Shows bot is typing before slow operations.

#### ⚙️ ACTIONS (Purple `#A78BFA`)
24. **Add role**: Assigns role to target user.
25. **Remove role**: Revokes role from target user.
26. **Timeout user**: Temporarily mutes user for specified minutes.
27. **Kick user**: Kicks user from guild with audit log reason.
28. **Ban user**: Permanently bans user from server.
29. **Unban user**: Revokes user ban by ID.
30. **Create channel**: Creates new text or voice channel.
31. **Delete channel**: Deletes target channel.
32. **Create thread**: Starts public or private discussion thread.
33. **Lock channel**: Disables `Send Messages` permission for `@everyone`.
34. **Unlock channel**: Restores typing permissions.
35. **Move member to voice**: Moves target member to a voice channel.
36. **Change nickname**: Sets server nickname for target user.

#### 🔀 LOGIC (Amber `#F0B429`)
37. **If ... then**: Full conditional container block (`==`, `!=`, `contains`, `startswith`).
38. **Wait ... seconds**: Asynchronous sleep delay without blocking the event loop.
39. **Repeat ... times**: Loop container executing child blocks multiple times.
40. **For each in list**: Iterates over members, roles, or items list.
41. **While is true**: Loop container while condition evaluates to true.
42. **Try / Catch**: Error handling container for catching API exceptions.
43. **Check permission**: Verifies author has permissions (Administrator, Manage Messages, etc.).
44. **Cooldown check**: Per-user rate limiting interval.
45. **With random chance**: Executes child branch with % probability.
46. **Stop script**: Halts block execution early.

#### 📦 VARIABLES & DATA (Cyan `#4DD0E1`)
47. **Set var**: Stores value in runtime memory.
48. **Change var**: Increments or decrements variable value.
49. **List operation**: Adds or removes items from a list.
50. **Map operation**: Sets key-value pairs in a dictionary.
51. **Random number picker**: Generates random integer in range.
52. **Math operation**: Add, subtract, multiply, divide, round.
53. **String operation**: Join, split, uppercase, lowercase.
54. **Save to Database**: Permanently persists variable into SQLite database.
55. **Get from Database**: Retrieves stored value from SQLite database.

#### 👑 DISCORD OBJECTS (Pink `#EB459E`)
56. **User object**: Extracts avatar URL, mention, ID, or roles.
57. **Channel object**: Extracts channel mention, ID, name, or topic.
58. **Server object**: Extracts guild member count, boost level, or owner ID.
59. **Message object**: Extracts message content, author, or attachments.
60. **Permission check block**: Validates granular Discord permission bits.
61. **Role hierarchy check**: Ensures bot or author role is higher than target role.

#### 🌐 API / NETWORK (Teal `#00B4D8`)
62. **HTTP GET request**: Fetches REST API endpoints asynchronously.
63. **HTTP POST request**: Dispatches JSON payloads to external web services.
64. **Parse JSON response**: Converts raw response into accessible object.
65. **Extract JSON path**: Dot-notation extractor (e.g. `data.quote.text`).
66. **Webhook send**: Dispatches message via Discord Webhook URL.

#### ⚠️ DESTRUCTIVE (Red `#FF5555`)
67. **Purge messages**: Bulk deletes 1 to 100 messages with optional author filter.
68. **Lockdown channel**: Immediate emergency chat lockdown.
69. **Unlock channel**: Restores channel access.
70. **Ban & purge 7d**: Bans malicious user and purges 7 days of message history.

---

## 4. 💻 Real-Time Multi-Language Code Editor
* **Languages**: Full syntax support for JavaScript (`Node.js`), Python (`discord.py`), TypeScript, Java, and Kotlin.
* **IntelliSense Autocomplete**: Context-aware autocompletions for Discord.js & Discord.py methods, events, and classes.
* **Signature Help Tooltips**: Real-time parameter hints for methods and constructors.
* **Monospace Code Canvas**: High-performance text editor with line numbers, bracket pairing, and smooth debounced linting.
* **Snippet Library**: Ready-to-use boilerplate templates for ping commands, embed senders, modal handlers, and event listeners.

---

## 5. 💬 Interactive Discord Simulator
* **Authentic Discord Dark Theme**: Test without needing a separate Discord client.
* **Rich Message Component Rendering**: Interactive buttons (Primary, Secondary, Success, Danger) and String Select Menus.
* **Rich Embed Visualizer**: Renders titles, descriptions, color strips, inline fields, and footers.
* **Reaction System**: Add emoji reactions with dynamic counters.
* **Slash Command Autocomplete**: Type `/` in the simulator to browse registered slash commands.

---

## 6. 🎨 Visual Rich Embed Designer
* **WYSIWYG Embed Builder**: Design custom rich embed cards visually.
* **Color Customization**: Quick Discord palette presets (Blurple, Green, Yellow, Red) or custom hex inputs.
* **Dynamic Fields**: Add, edit, remove, and reorder title/value field rows with inline toggles.
* **Export to Code**: Instantly converts visual embed into clean Discord.js (`EmbedBuilder`) or Discord.py (`discord.Embed`) code.

---

## 7. 🗄️ Local Room Database & Key-Value Datastore
* **Built-in Key-Value Store**: Built on Room SQLite for persistent bot data (user balances, warnings, custom prefixes).
* **Live Data Browser**: View, add, edit, search, and delete database keys.
* **JSON Import / Export**: Backup and restore bot database states across projects.

---

## 8. 🤖 AI Studio Gemini Assistant
* **Gemini-Powered Code Assistant**: Chat with an AI assistant to generate bot commands, debug syntax errors, or explain Discord gateway events.
* **API Key Pool**: Securely store and manage Google AI Studio and Discord developer credentials without hardcoding secrets.

---

## 9. 🛠️ Interactive Terminal & Gateway Console
* **Color-Coded Streams**: Dedicated styling for `SYSTEM`, `STDOUT`, `STDERR`, `GATEWAY`, and `REST` output.
* **Terminal Command History**: Navigate previous commands with history recall.
* **Built-in Diagnostics**: Run `ping`, `status`, `shards`, and `clear` commands.

---

## 10. 📦 Building the APK

### Join Discord: [GAMERZ Realme](https://discord.gg/krEBKByEYu)

### 1. Using GitHub Actions (Automated CI/CD)
This repository includes a ready-to-run GitHub Actions workflow (`.github/workflows/build-apk.yml`).

1. Push your repository to GitHub.
2. In your GitHub repository, navigate to the **Actions** tab.
3. Select **Build Android APK** and click **Run workflow** (or simply push to `main` / `master`).
4. Once completed, download the generated APK (`BotStudio-debug-apk.zip`) from the **Artifacts** section at the bottom of the run summary.

### 2. Building Locally with Gradle
```bash
# Ensure gradlew has execution permissions
chmod +x gradlew

# Run tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```
The compiled APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 💬 Community & Support
Join our Discord community for bot development help, sharing visual recipes, and reporting issues:

👉 **[Join Discord Server](https://discord.gg/krEBKByEYu)**
