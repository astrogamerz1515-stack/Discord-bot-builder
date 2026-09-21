package com.example.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DynamicModuleStatus {
    UNLOADED,
    DOWNLOADING,
    LOADING,
    ACTIVE,
    FAILED
}

data class DynamicModule(
    val id: String,
    val name: String,
    val description: String,
    val sizeKb: Int,
    val memoryFootprintKb: Int,
    val status: DynamicModuleStatus = DynamicModuleStatus.UNLOADED,
    val progress: Float = 0f,
    val autoEvictOnLowMemory: Boolean = true,
    val loadedTimestamp: Long = 0L,
    val exportedApis: List<String> = emptyList()
)

object DynamicLoadingService {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _modules = MutableStateFlow(
        listOf(
            DynamicModule(
                id = "dyn_discord_canvas",
                name = "Canvas & Image Synthesizer",
                description = "On-demand image generation for level cards, banners, and rank displays.",
                sizeKb = 2400,
                memoryFootprintKb = 8500,
                status = DynamicModuleStatus.ACTIVE,
                exportedApis = listOf("createRankCard()", "renderWelcomeBanner()", "blendAvatars()")
            ),
            DynamicModule(
                id = "dyn_voice_audio",
                name = "Discord Voice & Opus Streamer",
                description = "Audio streaming pipeline with prism-media and @discordjs/voice codec buffers.",
                sizeKb = 3200,
                memoryFootprintKb = 14200,
                status = DynamicModuleStatus.UNLOADED,
                exportedApis = listOf("joinVoiceChannel()", "createAudioPlayer()", "playStream()")
            ),
            DynamicModule(
                id = "dyn_json_schema",
                name = "Zod & JSON Schema Validator",
                description = "Strict contract validation for slash commands, webhooks, and options.",
                sizeKb = 850,
                memoryFootprintKb = 2800,
                status = DynamicModuleStatus.ACTIVE,
                exportedApis = listOf("z.object()", "validateInteractionPayload()")
            ),
            DynamicModule(
                id = "dyn_math_eval",
                name = "Expression & Math Parser",
                description = "Safe AST evaluation for custom Discord currency math, dice rolls, and stats.",
                sizeKb = 620,
                memoryFootprintKb = 1800,
                status = DynamicModuleStatus.UNLOADED,
                exportedApis = listOf("evaluateFormula()", "rollDice(d20)")
            ),
            DynamicModule(
                id = "dyn_regex_automod",
                name = "Advanced AutoMod Regex Engine",
                description = "High-throughput token filtering, anti-invite, anti-phishing url detection.",
                sizeKb = 1100,
                memoryFootprintKb = 4200,
                status = DynamicModuleStatus.ACTIVE,
                exportedApis = listOf("scanAutomodFilters()", "detectMaliciousLinks()")
            )
        )
    )
    val modules: StateFlow<List<DynamicModule>> = _modules.asStateFlow()

    private val _isDynamicOptimizerActive = MutableStateFlow(true)
    val isDynamicOptimizerActive: StateFlow<Boolean> = _isDynamicOptimizerActive.asStateFlow()

    fun loadModule(moduleId: String) {
        val currentList = _modules.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == moduleId }
        if (index == -1) return

        val mod = currentList[index]
        if (mod.status == DynamicModuleStatus.ACTIVE || mod.status == DynamicModuleStatus.LOADING) return

        currentList[index] = mod.copy(status = DynamicModuleStatus.DOWNLOADING, progress = 0.1f)
        _modules.value = currentList

        scope.launch {
            for (step in 1..5) {
                delay(100)
                updateModuleProgress(moduleId, DynamicModuleStatus.DOWNLOADING, step * 0.2f)
            }
            updateModuleProgress(moduleId, DynamicModuleStatus.LOADING, 1.0f)
            delay(150)
            val updated = _modules.value.toMutableList()
            val finalIndex = updated.indexOfFirst { it.id == moduleId }
            if (finalIndex != -1) {
                updated[finalIndex] = updated[finalIndex].copy(
                    status = DynamicModuleStatus.ACTIVE,
                    progress = 1f,
                    loadedTimestamp = System.currentTimeMillis()
                )
                _modules.value = updated
            }
            SystemDeviceOptimizer.refreshMemoryStats()
        }
    }

    private fun updateModuleProgress(id: String, status: DynamicModuleStatus, progress: Float) {
        val list = _modules.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(status = status, progress = progress)
            _modules.value = list
        }
    }

    fun unloadModule(moduleId: String) {
        val list = _modules.value.toMutableList()
        val idx = list.indexOfFirst { it.id == moduleId }
        if (idx != -1) {
            list[idx] = list[idx].copy(status = DynamicModuleStatus.UNLOADED, progress = 0f)
            _modules.value = list
        }
        SystemDeviceOptimizer.refreshMemoryStats()
    }

    fun unloadInactiveModulesToFreeRam(): Int {
        var count = 0
        val list = _modules.value.toMutableList()
        for (i in list.indices) {
            val m = list[i]
            if (m.status == DynamicModuleStatus.ACTIVE && m.autoEvictOnLowMemory) {
                list[i] = m.copy(status = DynamicModuleStatus.UNLOADED, progress = 0f)
                count++
            }
        }
        _modules.value = list
        SystemDeviceOptimizer.refreshMemoryStats()
        return count
    }

    fun getTotalAllocatedMemoryKb(): Int {
        return _modules.value
            .filter { it.status == DynamicModuleStatus.ACTIVE }
            .sumOf { it.memoryFootprintKb }
    }
}
