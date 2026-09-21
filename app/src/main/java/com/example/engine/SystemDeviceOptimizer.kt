package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DevicePerformanceTier {
    LOW_END,     // Older devices or < 3GB RAM: disables syntax highlighting throttle, limits log buffers, pauses animations
    MID_RANGE,   // 3GB - 6GB RAM: standard smooth performance
    HIGH_END     // 6GB+ RAM: full animations, extensive history, continuous diagnostics
}

data class DeviceOptimizationSettings(
    val tier: DevicePerformanceTier = DevicePerformanceTier.MID_RANGE,
    val isLowEndDevice: Boolean = false,
    val maxLogBufferSize: Int = 100,
    val syntaxHighlightingEnabled: Boolean = true,
    val autoDiagnosticThrottlingMs: Long = 400L,
    val smoothAnimationsEnabled: Boolean = true,
    val powerSaveMode: Boolean = false,
    val dynamicLoadingKeepInMemory: Boolean = false,
    val memoryUsageMb: Long = 0L,
    val totalRamMb: Long = 4096L,
    val availableRamMb: Long = 2048L
)

object SystemDeviceOptimizer {

    private val _settings = MutableStateFlow(DeviceOptimizationSettings())
    val settings: StateFlow<DeviceOptimizationSettings> = _settings.asStateFlow()

    fun initialize(context: Context) {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val isLowRamDevice = actManager?.isLowRamDevice ?: false

        val tier = when {
            isLowRamDevice || totalRamMb < 3000 -> DevicePerformanceTier.LOW_END
            totalRamMb < 6000 -> DevicePerformanceTier.MID_RANGE
            else -> DevicePerformanceTier.HIGH_END
        }

        _settings.value = DeviceOptimizationSettings(
            tier = tier,
            isLowEndDevice = tier == DevicePerformanceTier.LOW_END,
            maxLogBufferSize = when (tier) {
                DevicePerformanceTier.LOW_END -> 50
                DevicePerformanceTier.MID_RANGE -> 150
                DevicePerformanceTier.HIGH_END -> 500
            },
            syntaxHighlightingEnabled = true,
            autoDiagnosticThrottlingMs = when (tier) {
                DevicePerformanceTier.LOW_END -> 800L
                DevicePerformanceTier.MID_RANGE -> 350L
                DevicePerformanceTier.HIGH_END -> 150L
            },
            smoothAnimationsEnabled = tier != DevicePerformanceTier.LOW_END,
            powerSaveMode = false,
            dynamicLoadingKeepInMemory = tier == DevicePerformanceTier.HIGH_END,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            memoryUsageMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
        )
    }

    fun togglePowerSave(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            powerSaveMode = enabled,
            smoothAnimationsEnabled = !enabled && _settings.value.tier != DevicePerformanceTier.LOW_END,
            autoDiagnosticThrottlingMs = if (enabled) 1000L else 350L
        )
    }

    fun setPerformanceTier(tier: DevicePerformanceTier) {
        _settings.value = _settings.value.copy(
            tier = tier,
            isLowEndDevice = tier == DevicePerformanceTier.LOW_END,
            smoothAnimationsEnabled = tier != DevicePerformanceTier.LOW_END && !_settings.value.powerSaveMode,
            maxLogBufferSize = when (tier) {
                DevicePerformanceTier.LOW_END -> 50
                DevicePerformanceTier.MID_RANGE -> 150
                DevicePerformanceTier.HIGH_END -> 500
            }
        )
    }

    fun refreshMemoryStats() {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        _settings.value = _settings.value.copy(memoryUsageMb = usedMb)
    }
}
