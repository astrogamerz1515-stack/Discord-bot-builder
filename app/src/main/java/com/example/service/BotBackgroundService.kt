package com.example.service

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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.repository.BotRepository
import com.example.engine.BotRuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Enterprise Android Foreground Service to keep Discord Bot WebSocket connections,
 * heartbeat intervals, and background runtime running 24/7 even when:
 * 1. The phone screen is turned off / locked (WakeLock + WifiLock).
 * 2. The user switches to Discord or another application.
 * 3. The app is completely closed or swiped away from Recents (onTaskRemoved + START_STICKY).
 * 4. The device reboots (BootCompletedReceiver + auto-resume).
 */
class BotBackgroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var notificationUpdaterJob: Job? = null

    private var activeProjectId: Long = 0L
    private var activeBotName: String = "Discord Bot"

    companion object {
        const val CHANNEL_ID = "discord_bot_background_channel"
        const val NOTIFICATION_ID = 4040
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            getPrefs(context).edit()
                .putBoolean(PREF_IS_RUNNING, false)
                .apply()

            val intent = Intent(context, BotBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireSystemLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdownBotAndService()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                val prefs = getPrefs(this)
                activeProjectId = intent?.getLongExtra(EXTRA_PROJECT_ID, 0L) ?: prefs.getLong(PREF_PROJECT_ID, 0L)
                activeBotName = intent?.getStringExtra(EXTRA_BOT_NAME) ?: prefs.getString(PREF_BOT_NAME, "Discord Bot") ?: "Discord Bot"

                // Save state to disk
                prefs.edit()
                    .putBoolean(PREF_IS_RUNNING, true)
                    .putLong(PREF_PROJECT_ID, activeProjectId)
                    .putString(PREF_BOT_NAME, activeBotName)
                    .apply()

                val notification = buildNotification(activeBotName, "Starting Gateway connection...", false)
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
            }
        }
        return START_STICKY
    }

    /**
     * CRITICAL: Called when the user closes or swipes the app away from Recents.
     * We keep the foreground service running and keep the WebSocket connected 24/7!
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Refresh locks to guarantee system will not sleep
        acquireSystemLocks()

        // Update notification indicating app is closed but bot is still active 24/7
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
        notificationUpdaterJob?.cancel()
        serviceScope.cancel()
        releaseSystemLocks()
        super.onDestroy()
    }

    private fun ensureBotEngineRunning(projectId: Long) {
        serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = BotRepository(
                database.botProjectDao(),
                database.botFileDao(),
                database.terminalLogDao(),
                database.savedEmbedDao(),
                database.botKeyValueDao()
            )
            val engine = BotRuntimeManager.getEngine(repository)

            if (!engine.isRunning.value && projectId > 0) {
                val project = repository.getProjectSync(projectId)
                if (project != null) {
                    engine.startBot(project)
                }
            }
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
                        "🟢 Online • Gateway: ${ping}ms • 24/7 Background"
                    } else {
                        "🟡 Running (${engine.botStatusText.value}) • Background"
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
