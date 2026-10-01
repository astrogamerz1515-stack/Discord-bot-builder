# 🤖 BotStudio - Discord Bot IDE, Visual Builder & Simulator for Android

[![Build Android APK](https://github.com/your-username/botstudio/actions/workflows/build-apk.yml/badge.svg)](https://github.com/your-username/botstudio/actions/workflows/build-apk.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/krEBKByEYu)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

**BotStudio** is a full-featured, mobile IDE, Scratch-style Visual Builder, and real-time simulator built specifically for Discord bot developers on Android. Write, visually assemble, test, optimize, and deploy production Discord bots directly from your phone or tablet with **0% downtime background execution**.

---

## 🚀 Features Overview

### 1. ⚡ Discord Bot Runtime & Gateway v10 Engine
- **Live Discord Gateway v10 WebSocket**: Connect your real Discord bot with your Bot Token directly to Discord's official Gateway.
- **Heartbeat & Zombie Detection**: Automatic heartbeat ACK monitoring loop (`heartbeat_interval`), auto-reconnecting on dropped connections.
- **Session Resuming**: Seamlessly resumes active sessions using `resume_gateway_url` and sequence numbers (`s`) without losing gateway state.
- **Close Code Routing Matrix**: Intelligent recovery for 1000/1001 clean closures, 4000-series resumable disconnects, and fatal auth warnings.
- **Automatic Sharding Calculator**: Queries `/gateway/bot` to dynamically compute recommended shards (`ceil(guilds / 2500)`).
- **Full 15-Bitmask Intent Support**: Configure `GUILDS`, `GUILD_MEMBERS`, `MESSAGE_CONTENT`, `GUILD_VOICE_STATES`, `GUILD_PRESENCES`, etc., with 2026 Privileged Intent Verification guidance.
- **Voice Gateway & DAVE E2EE Protocol**: Built-in protocol negotiation for Discord's mandatory March 2026 End-to-End Encrypted voice standard.
- **Hot Reload**: Instantly push script changes into the running bot process without restarting the gateway connection.

---

### 2. 📱 Android Background Service & 24/7 Hosting (Even When App/Phone is Closed)
- **Decoupled Application-Level Runtime**: The bot runtime runs inside a persistent `BotRuntimeManager.applicationScope` rather than an ephemeral `viewModelScope`. Closing the app or finishing activities does **not** terminate your bot's WebSocket or heartbeat loops!
- **Foreground Service Daemon**: When you tap **Run**, the bot automatically launches as an Android Foreground Service (`BotBackgroundService`) returning `START_STICKY`.
- **Survives App Closure & Swiping from Recents**: Implements `onTaskRemoved()`. When you swipe away BotStudio from Recents or close the app, the foreground service keeps the Discord Gateway connection streaming with zero downtime.
- **Survives Screen Sleep & Phone Lock**: Employs a low-power CPU `WakeLock` (`PARTIAL_WAKE_LOCK`) combined with a high-performance `WifiLock` (`WIFI_MODE_FULL_HIGH_PERF`), preventing Android from sleeping the CPU or throttling Wi-Fi radios when your phone screen turns off.
- **Auto-Restart on Phone Reboot**: Built-in `BootCompletedReceiver` automatically detects if a bot was active and re-launches the bot service immediately when your device reboots.
- **Persistent Notification with Live Latency**: Shows a sleek ongoing notification displaying your bot's name, online state, live Gateway ping, and a one-tap **Stop Bot** action.
- **Unrestricted Battery Settings Shortcut**: Quick one-tap setting helper to exempt BotStudio from Android Doze restrictions.

---

### 3. ⚡ Slow Command Response Optimizer & Latency Diagnostics
Solve the dreaded 3-second *"The application did not respond"* Discord timeout with enterprise performance tools:

- **Triple Latency Diagnostics**:
  - **Gateway Latency Meter**: Real-time heartbeat round-trip ping in milliseconds.
  - **REST Latency Meter**: Instantaneous ping against `GET /users/@me`.
  - **Interaction Latency Badge**: Measures elapsed time from interaction arrival to initial response; flags commands taking `>500ms` with visual warning alerts.
- **The #1 Fix (Immediate deferReply / deferUpdate)**:
  - Explains and provides one-click templates to immediately call `deferReply()` or `deferUpdate()`, granting a 15-minute execution window before heavy operations.
  - Code inspection detects if database or external API calls are made before deferring.
- **Hosting Location Selector**:
  - Region proximity selector prioritizing **US East (Ashburn / us-ashburn-1)** (<50ms target) and warning against high-latency regions like India (>240ms) or Singapore (>210ms).
- **Event Loop Lag Monitor**:
  - Continuous event loop lag tracking. Alerts with red warnings if lag exceeds `50ms`, reminding developers to avoid synchronous calls like `readFileSync`.
- **Cold Start Keep-Alive Server & Detector**:
  - Built-in HTTP health check endpoint (`/`) with 5-minute ping simulation for free-tier hosting (Render, Replit).
- **Command Sync Mode Toggle**:
  - Switch between **Per-Guild Mode** (instant 0s propagation for rapid development) and **Global Mode** (production with up to 1-hour delay).
- **In-Memory REST Cache Layer**:
  - Configurable TTL cache (minimum 1 minute) using `client.users.cache.get()` instead of redundant `.fetch()` calls.
- **Gateway vs. Webhook Interaction Routing**:
  - Select between persistent Gateway WebSocket connections (lowest latency) and HTTP Webhooks.
- **6-Point Diagnostic Checklist**:
  - Interactive verification checklist covering deferral, hosting region, cold starts, event loop lag, command registration, and REST caching.

---

### 4. 🧩 Scratch-Style Visual Builder
A no-code, block-based coding canvas specifically engineered for Discord bot logic:

- **Authentic Scratch Puzzle Snapping**:
  - **Hat Blocks**: Rounded dome headers for Event trigger blocks.
  - **Interlocking Tabs**: Protruding bump tabs and cutout sockets on command blocks.
  - **C-Blocks**: Nested containers for conditional logic (`If ... then`, `Repeat`) with visual indent lines and closing caps.
- **Interactive Parameter Capsules**: Tap directly on embedded bubble chips (e.g., `[ Send [ "Hello!" ] to [ #general ] ]`) to edit parameters inline.
- **Complete Discord Block Library**:
  - **Events (⚡ Electric Blue)**: Message Received, Slash Command (`/help`), Member Join, Member Leave, Reaction Added, Button Click, Voice State Update.
  - **Messages (💬 Neon Green)**: Send Message, Reply with Ping, Send Rich Embed, Send DM, Add Emoji Reaction, Delete Message.
  - **Actions (⚙️ Purple)**: Add Role, Remove Role, Timeout/Mute User, Kick User, Ban User, Create Channel, Change Nickname.
  - **Logic (🔀 Amber)**: If Condition, Asynchronous Delay/Wait, Repeat Loop, Permission Check (`Administrator`), Random Chance %, Stop Script.
  - **Variables (📦 Cyan)**: Set Variable, Change Variable, Save to Persistent Database, Read from Database.
  - **Destructive (⚠️ Red)**: Purge Messages (1–100), Lockdown Channel, Unlock Channel, Ban & Purge 7d.
- **Starter Recipes**: One-tap pre-built stacks for **Ping-Pong Bot**, **Auto-Moderator**, **Welcome & Auto-Role**, and **Slash /help Command**.
- **Multi-Language Generator**: Dual-tab code export for modern **Python (`discord.py`)** and **JavaScript (`discord.js v14`)**.
- **Interactive Discord Simulator**: Live test view replicating Discord's dark UI with user input and live block execution glowing.
- **Undo / Redo Canvas History**: 25-step history stack for effortless editing.

---

### 5. 💻 Real-Time Multi-Language Code Editor
- **Multi-Language Support**: Full IDE syntax styling for JavaScript (Node.js), Python (discord.py), TypeScript, Java, and Kotlin.
- **IntelliSense Autocomplete Bar**: Context-aware completion suggestions for Discord.js & Discord.py methods, events, and classes.
- **Signature Help Tooltips**: Real-time parameter hints for methods and constructors.
- **Monospace Code Canvas**: High-performance text editor with line numbers, bracket pairing, and smooth debounced linting.
- **Snippet Library**: Ready-to-use boilerplate templates for ping commands, embed senders, modal handlers, and event listeners.

---

### 6. 💬 Discord Simulator (Interactive In-App Sandbox)
- **Authentic Discord Dark Theme**: Test without needing a separate Discord client.
- **Rich Message Component Rendering**: Interactive buttons (Primary, Secondary, Success, Danger) and String Select Menus.
- **Rich Embed Visualizer**: Renders titles, descriptions, color strips, inline fields, and footers.
- **Reaction System**: Add emoji reactions with dynamic counters.
- **Slash Command Autocomplete**: Type `/` in the simulator to browse registered slash commands.

---

### 7. 🎨 Visual Embed Designer
- **WYSIWYG Embed Builder**: Design custom rich embed cards visually.
- **Color Customization**: Quick Discord palette presets (Blurple, Green, Yellow, Red) or custom hex inputs.
- **Dynamic Fields**: Add, edit, remove, and reorder title/value field rows with inline toggles.
- **Export to Code**: Instantly converts your visual embed into clean Discord.js (`EmbedBuilder`) or Discord.py (`discord.Embed`) code.

---

### 8. 🗄️ Advanced Storage & Datastore Manager
- **Built-in Key-Value Store**: Built on Room SQLite for persistent bot data (user balances, warnings, custom prefixes).
- **Live Data Browser**: View, add, edit, search, and delete database keys.
- **JSON Import / Export**: Backup and restore bot database states across projects.

---

### 9. 🤖 AI Studio Assistant & Models Pool
- **Gemini-Powered Code Assistant**: Chat with an AI assistant to generate bot commands, debug syntax errors, or explain Discord gateway events.
- **API Key Pool**: Securely store and manage Google AI Studio and Discord developer credentials without hardcoding secrets.

---

### 10. 🛠️ Interactive Terminal & Console
- **Color-Coded Streams**: Dedicated styling for `SYSTEM`, `STDOUT`, `STDERR`, `GATEWAY`, and `REST` output.
- **Terminal Command History**: Navigate previous commands with history recall.
- **Built-in Diagnostics**: Run `ping`, `status`, `shards`, and `clear` commands.

---

## 📦 Building the APK

### join discord - [GAMERZ Realme](https://discord.gg/krEBKByEYu)
for the apk

### 1. Using GitHub Actions (Automated CI/CD)
This repository includes a ready-to-run GitHub Actions workflow (`.github/workflows/build-apk.yml`).

1. Push your repository to GitHub.
2. In your GitHub repository, navigate to the **Actions** tab.
3. Select **Build Android APK** and click **Run workflow** (or simply push to `main` / `master`).
4. Once completed, download the generated APK (`BotStudio-debug-apk.zip`) from the **Artifacts** section at the bottom of the run summary.

---

### 2. Building Locally with Gradle

#### Prerequisites:
- JDK 21 (Eclipse Temurin recommended)
- Android SDK (API 34 / 36)

#### Steps:
```bash
# Ensure gradlew has execution permissions
chmod +x gradlew

# (Optional) Restore debug.keystore if starting fresh
if [ -f debug.keystore.base64 ] && [ ! -f debug.keystore ]; then
  base64 -d debug.keystore.base64 > debug.keystore
fi

# Run tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 💬 Community & Support

Join our Discord community for bot development help, sharing visual recipes, and reporting issues:

👉 **[Join Discord Server](https://discord.gg/krEBKByEYu)**
