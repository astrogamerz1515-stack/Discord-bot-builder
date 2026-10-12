package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.repository.BotRepository
import com.example.engine.BotRuntimeEngine
import com.example.engine.BotRuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Enterprise Android Foreground Service to keep Discord Bot WebSocket connections,
 * heartbeat intervals, and background runtime running 24/7 even when:
 * 1. The phone screen is turned off / locked (WakeLock + WifiLock).
 * 2. The user switches to Discord or another application.
 * 3. The app is completely closed or swiped away from Recents (onTaskRemoved + START_STICKY + Watchdog Alarm).
 * 4. The device reboots (BootCompletedReceiver + auto-resume).
 * 5. OS tries to terminate the process (auto-alives with AlarmManager watchdog heartbeat).
 */
class BotBackgroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var notificationUpdaterJob: Job? = null
    private var internalWatchdogJob: Job? = null

    private var activeProjectId: Long = 0L
    private var activeBotName: String = "Discord Bot"

    companion object {
        const val CHANNEL_ID = "discord_bot_background_channel"
        const val NOTIFICATION_ID = 4040
        const val WATCHDOG_ALARM_ID = 9999
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_BOT_NAME = "extra_bot_name"

        const val PREFS_NAME = "botstudio_background_prefs"
        const val PREF_IS_RUNNING = "is_bot_running_background"
        const val PREF_PROJECT_ID = "active_bot_project_id"
        const val PREF_BOT_NAME = "active_bot_name"

        // Static listener for UI updates when stopped from notification
        var onServiceStopListener: (() -> Unit)? = null

        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun isBotMarkedRunning(context: Context): Boolean {
            return getPrefs(context).getBoolean(PREF_IS_RUNNING, false)
        }

        fun start(context: Context, projectId: Long, botName: String) {
            getPrefs(context).edit()
                .putBoolean(PREF_IS_RUNNING, true)
                .putLong(PREF_PROJECT_ID, projectId)
                .putString(PREF_BOT_NAME, botName)
                .apply()

            val intent = Intent(context, BotBackgroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_BOT_NAME, botName)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // If direct start is restricted by background execution limits, schedule via AlarmManager
                scheduleWatchdogAlarmStatic(context, projectId, botName, 500L)
            }
        }

        fun stop(context: Context) {
            getPrefs(context).edit()
                .putBoolean(PREF_IS_RUNNING, false)
                .apply()

            cancelWatchdogAlarmStatic(context)

            val intent = Intent(context, BotBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun scheduleWatchdogAlarmStatic(context: Context, projectId: Long, botName: String, delayMs: Long) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, BootCompletedReceiver::class.java).apply {
                    action = BootCompletedReceiver.ACTION_WATCHDOG_KEEP_ALIVE
                    putExtra(EXTRA_PROJECT_ID, projectId)
                    putExtra(EXTRA_BOT_NAME, botName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    WATCHDOG_ALARM_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerAt = SystemClock.elapsedRealtime() + delayMs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {}
        }

        fun cancelWatchdogAlarmStatic(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, BootCompletedReceiver::class.java).apply {
                    action = BootCompletedReceiver.ACTION_WATCHDOG_KEEP_ALIVE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    WATCHDOG_ALARM_ID,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireSystemLocks()
        registerNetworkWatcher()
        startInternalWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdownBotAndService()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                val prefs = getPrefs(this)
                val wasMarkedRunning = prefs.getBoolean(PREF_IS_RUNNING, false)

                // If restarted by OS without explicit intent, ensure it was actually supposed to be running
                if (intent == null && !wasMarkedRunning) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }

                val rawId = intent?.getLongExtra(EXTRA_PROJECT_ID, 0L) ?: 0L
                activeProjectId = if (rawId > 0) rawId else prefs.getLong(PREF_PROJECT_ID, 0L)
                val rawName = intent?.getStringExtra(EXTRA_BOT_NAME)
                activeBotName = if (!rawName.isNullOrBlank()) rawName else (prefs.getString(PREF_BOT_NAME, "Discord Bot") ?: "Discord Bot")

                // Save persistent state
                prefs.edit()
                    .putBoolean(PREF_IS_RUNNING, true)
                    .putLong(PREF_PROJECT_ID, activeProjectId)
                    .putString(PREF_BOT_NAME, activeBotName)
                    .apply()

                acquireSystemLocks()

                val notification = buildNotification(activeBotName, "🟢 24/7 Active • Background Online", false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                ensureBotEngineRunning(activeProjectId)
                startNotificationObserving()
                scheduleWatchdogAlarmStatic(this, activeProjectId, activeBotName, 60_000L) // 1 minute safety pulse
            }
        }
        return START_STICKY
    }

    /**
     * CRITICAL: Called when user swipes app from Recents.
     * Keep foreground service running, ensure engine alive, and schedule immediate resurrection alarm!
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        acquireSystemLocks()

        if (activeProjectId > 0) {
            ensureBotEngineRunning(activeProjectId)
        }

        // Schedule auto-aliving resurrection alarm in case system tries to terminate the process
        scheduleWatchdogAlarmStatic(this, activeProjectId, activeBotName, 1500L)

        val notification = buildNotification(
            activeBotName,
            "🟢 Online • 24/7 Background (App Closed) • Tap to view",
            isAppClosed = true
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isBotMarkedRunning(this)) {
            // Killed by OS while marked running -> Immediately resurrect!
            scheduleWatchdogAlarmStatic(this, activeProjectId, activeBotName, 1500L)
        }
        unregisterNetworkWatcher()
        internalWatchdogJob?.cancel()
        notificationUpdaterJob?.cancel()
        serviceScope.cancel()
        releaseSystemLocks()
        super.onDestroy()
    }

    /**
     * Continuous 24/7 Watchdog: Periodically verifies engine health and auto-alives if stopped.
     */
    private fun startInternalWatchdog() {
        internalWatchdogJob?.cancel()
        internalWatchdogJob = serviceScope.launch {
            while (isActive) {
                delay(12_000L) // Check every 12 seconds
                if (isBotMarkedRunning(this@BotBackgroundService)) {
                    acquireSystemLocks()
                    if (activeProjectId > 0) {
                        ensureBotEngineRunning(activeProjectId)
                    }
                    // Keep refreshing OS-level watchdog alarm every 2 minutes
                    scheduleWatchdogAlarmStatic(this@BotBackgroundService, activeProjectId, activeBotName, 120_000L)
                }
            }
        }
    }

    private fun registerNetworkWatcher() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (networkCallback == null && cm != null) {
                val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        super.onAvailable(network)
                        val engine = BotRuntimeManager.currentEngine()
                        if (engine != null && isBotMarkedRunning(this@BotBackgroundService)) {
                            if (!engine.isRealDiscordConnected.value && activeProjectId > 0) {
                                ensureBotEngineRunning(activeProjectId)
                            }
                        }
                    }
                }
                cm.registerDefaultNetworkCallback(callback)
                networkCallback = callback
            }
        } catch (_: Exception) {}
    }

    private fun unregisterNetworkWatcher() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        networkCallback = null
    }

    private fun ensureBotEngineRunning(projectId: Long) {
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = BotRepository(
                    database.botProjectDao(),
                    database.botFileDao(),
                    database.terminalLogDao(),
                    database.savedEmbedDao(),
                    database.botKeyValueDao()
                )
                val engine = BotRuntimeManager.getEngine(repository)

                var targetId = projectId
                if (targetId <= 0) {
                    val allProjs = repository.getAllProjectsSync()
                    targetId = allProjs.firstOrNull()?.id ?: 0L
                }

                if (targetId > 0) {
                    activeProjectId = targetId
                    getPrefs(this@BotBackgroundService).edit()
                        .putLong(PREF_PROJECT_ID, targetId)
                        .apply()

                    val project = repository.getProjectSync(targetId)
                    if (project != null) {
                        activeBotName = project.name
                        val hasToken = BotRuntimeEngine.isRealToken(project.botToken)
                        if (!engine.isRunning.value || (hasToken && !engine.isRealDiscordConnected.value)) {
                            engine.startBot(project)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun startNotificationObserving() {
        notificationUpdaterJob?.cancel()
        notificationUpdaterJob = serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = BotRepository(
                database.botProjectDao(),
                database.botFileDao(),
                database.terminalLogDao(),
                database.savedEmbedDao(),
                database.botKeyValueDao()
            )
            val engine = BotRuntimeManager.getEngine(repository)

            // Live updates of latency and status to ongoing notification
            engine.gatewayPingMs.collectLatest { ping ->
                if (engine.isRunning.value) {
                    val statusText = if (engine.isRealDiscordConnected.value) {
                        "🟢 Online • Gateway: ${ping}ms • 24/7 Active"
                    } else {
                        "🟢 Running (${engine.botStatusText.value}) • 24/7 Active"
                    }
                    val notification = buildNotification(activeBotName, statusText, false)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun shutdownBotAndService() {
        getPrefs(this).edit()
            .putBoolean(PREF_IS_RUNNING, false)
            .apply()

        cancelWatchdogAlarmStatic(this)
        onServiceStopListener?.invoke()

        val engine = BotRuntimeManager.currentEngine()
        if (engine != null && activeProjectId > 0) {
            engine.stopBot(activeProjectId)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireSystemLocks() {
        // 1. Acquire CPU WakeLock (Prevents CPU sleep when screen is off)
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "BotStudio::BackgroundRunLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire(24 * 60 * 60 * 1000L) // 24 hours safeguard
                }
            } else if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            // Ignore system restriction exceptions
        }

        // 2. Acquire High-Performance WifiLock (Prevents Wi-Fi radio sleep when screen is off)
        try {
            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                } else {
                    WifiManager.WIFI_MODE_FULL
                }
                wifiLock = wifiManager?.createWifiLock(mode, "BotStudio::WifiKeepAlive")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            } else if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            // Ignore Wi-Fi lock exceptions
        }
    }

    private fun releaseSystemLocks() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) { }
        wakeLock = null

        try {
            wifiLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) { }
        wifiLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Discord Bot 24/7 Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your Discord Bot Gateway WebSocket connection online in the background even if the app or screen is closed"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(botName: String, subStatus: String, isAppClosed: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BotBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("🤖 $botName")
            .setContentText(subStatus)
            .setSubText(if (isAppClosed) "Background (App Closed)" else "BotStudio Active")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Bot", stopPendingIntent)
            .build()
    }
}
