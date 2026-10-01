package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that triggers when the Android phone powers on or restarts
 * (BOOT_COMPLETED, MY_PACKAGE_REPLACED, QUICKBOOT_POWERON).
 * If a Discord bot was running prior to shutdown/restart, it immediately restarts
 * the BotBackgroundService to maintain 24/7 bot availability.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = BotBackgroundService.getPrefs(context)
            val wasRunning = prefs.getBoolean(BotBackgroundService.PREF_IS_RUNNING, false)
            val projectId = prefs.getLong(BotBackgroundService.PREF_PROJECT_ID, 0L)
            val botName = prefs.getString(BotBackgroundService.PREF_BOT_NAME, "Discord Bot") ?: "Discord Bot"

            if (wasRunning && projectId > 0) {
                BotBackgroundService.start(context, projectId, botName)
            }
        }
    }
}
