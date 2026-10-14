package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.engine.BotRuntimeManager
import com.example.ui.BotStudioViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: BotStudioViewModel by viewModels()

  private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    // Notification permission granted or denied handled gracefully
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    BotRuntimeManager.appContext = applicationContext

    requestNotificationPermissionIfNeeded()
    requestBatteryOptimizationIgnoreIfNeeded()

    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  private fun requestBatteryOptimizationIgnoreIfNeeded() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
          val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
          }
          if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
          }
        }
      }
    } catch (_: Exception) {}
  }
}

