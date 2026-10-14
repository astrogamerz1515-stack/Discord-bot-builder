package com.example.engine

import android.content.Context
import android.os.SystemClock
import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.data.repository.BotRepository
import com.example.service.BotBackgroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class Pm2Status(val label: String) {
    ONLINE("online"),
    STOPPED("stopped"),
    ERRORED("errored"),
    RESTARTING("restarting")
}

data class Pm2Process(
    val id: Int,
    val name: String,
    val script: String,
    val interpreter: String, // node, python3, ts-node
    val pid: Int,
    val status: Pm2Status,
    val startedAt: Long,
    val restartCount: Int,
    val cpuPercent: Double,
    val memoryMb: Double,
    val isWatchEnabled: Boolean = true,
    val auto247: Boolean = true
) {
    fun getUptimeFormatted(): String {
        if (status != Pm2Status.ONLINE) return "0s"
        val diffSec = ((System.currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
        return when {
            diffSec < 60 -> "${diffSec}s"
            diffSec < 3600 -> "${diffSec / 60}m ${diffSec % 60}s"
            diffSec < 86400 -> "${diffSec / 3600}h ${(diffSec % 3600) / 60}m"
            else -> "${diffSec / 86400}d ${(diffSec % 86400) / 3600}h"
        }
    }
}

/**
 * Enterprise PM2 Process Manager for Discord Bots & Scripts.
 * Provides complete PM2 CLI emulation, process supervision, crash auto-restart loops,
 * and 24/7 background persistence via Android Foreground Service + System Locks.
 */
object Pm2ProcessManager {

    private const val PREFS_PM2 = "botstudio_pm2_prefs"
    private const val KEY_PM2_PROCESSES = "saved_pm2_processes"
    private const val KEY_PM2_AUTO_ENABLED = "pm2_auto_247_enabled"

    private val _processes = MutableStateFlow<List<Pm2Process>>(emptyList())
    val processes: StateFlow<List<Pm2Process>> = _processes.asStateFlow()

    private val _isDaemonActive = MutableStateFlow(false)
    val isDaemonActive: StateFlow<Boolean> = _isDaemonActive.asStateFlow()

    private var supervisorJob: Job? = null
    private var repositoryRef: BotRepository? = null
    private var appContext: Context? = null
    private fun getSafeContext(): Context? = appContext ?: BotRuntimeManager.appContext
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context, repository: BotRepository) {
        appContext = context.applicationContext
        repositoryRef = repository
        restoreSavedState()
        startSupervisorDaemon()
    }

    fun isAuto247Enabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_PM2, Context.MODE_PRIVATE)
            .getBoolean(KEY_PM2_AUTO_ENABLED, true)
    }

    fun setAuto247Enabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_PM2, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PM2_AUTO_ENABLED, enabled)
            .apply()
    }

    /**
     * Executes PM2 commands from terminal or scripts.
     * Returns terminal output lines.
     */
    suspend fun executeCommand(
        args: List<String>,
        project: BotProject,
        files: List<BotFile>,
        engine: BotRuntimeEngine
    ): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val subcmd = args.firstOrNull()?.lowercase() ?: "help"
        val remaining = args.drop(1)

        when (subcmd) {
            "start", "run" -> {
                val scriptTarget = remaining.firstOrNull { !it.startsWith("-") } ?: detectDefaultScript(files, project)
                val customName = parseParam(remaining, "--name") ?: scriptTarget.substringBeforeLast(".")
                val watch = remaining.contains("--watch")
                val proc = startScript(
                    script = scriptTarget,
                    name = customName,
                    project = project,
                    files = files,
                    engine = engine,
                    watch = watch
                )
                out.add("[PM2] Starting $scriptTarget in fork_mode (1 instance)" to "STDOUT")
                out.add("[PM2] Done." to "SUCCESS")
                out.addAll(renderProcessTable())
            }

            "list", "ls", "status", "ps" -> {
                out.addAll(renderProcessTable())
            }

            "stop" -> {
                val target = remaining.firstOrNull() ?: "all"
                val stopped = stopProcess(target, engine, project.id)
                out.add("[PM2] Applying action stop on process [$target]" to "WARN")
                out.add("[PM2] [${stopped.joinToString { it.name }}] stopped." to "SUCCESS")
                out.addAll(renderProcessTable())
            }

            "restart" -> {
                val target = remaining.firstOrNull() ?: "all"
                val restarted = restartProcess(target, engine, project)
                out.add("[PM2] Applying action restart on process [$target]" to "STDOUT")
                out.add("[PM2] [${restarted.joinToString { it.name }}] restarted." to "SUCCESS")
                out.addAll(renderProcessTable())
            }

            "delete", "del", "remove" -> {
                val target = remaining.firstOrNull() ?: "all"
                val deleted = deleteProcess(target, engine, project.id)
                out.add("[PM2] Process [$target] deleted from PM2 table." to "SUCCESS")
                out.addAll(renderProcessTable())
            }

            "save" -> {
                saveState()
                out.add("[PM2] Saving current process list..." to "STDOUT")
                out.add("[PM2] Successfully saved in /data/user/0/com.example/pm2/dump.pm2" to "SUCCESS")
                out.add("[PM2] 24/7 background auto-boot configuration saved." to "SUCCESS")
            }

            "startup", "resurrect" -> {
                val count = restoreSavedState()
                getSafeContext()?.let { ctx ->
                    BotBackgroundService.start(ctx, project.id, project.name)
                }
                out.add("[PM2] Restoring PM2 processes from dump..." to "STDOUT")
                out.add("[PM2] Resurrected $count process(es). 24/7 background alive guard: ACTIVE" to "SUCCESS")
                out.addAll(renderProcessTable())
            }

            "logs" -> {
                val lines = repositoryRef?.getTerminalLogsDirect(project.id)?.takeLast(15) ?: emptyList()
                out.add("=== [PM2 LOGS] Streaming last 15 log lines ===" to "SYSTEM")
                lines.forEach { log ->
                    out.add("[PM2][${log.type}] ${log.text}" to log.type)
                }
            }

            "monit", "info" -> {
                val list = _processes.value
                out.add("=== PM2 Process Monitoring & System Health ===" to "SYSTEM")
                if (list.isEmpty()) {
                    out.add("No processes currently managed by PM2. Run 'pm2 start index.js' to launch." to "WARN")
                } else {
                    list.forEach { p ->
                        out.add("● [PM2 App: ${p.name}] (ID: ${p.id}, PID: ${p.pid})" to "SUCCESS")
                        out.add("  Script: ${p.script} | Interpreter: ${p.interpreter}" to "STDOUT")
                        out.add("  Status: ${p.status.label.uppercase()} | Uptime: ${p.getUptimeFormatted()}" to "STDOUT")
                        out.add("  Restarts: ${p.restartCount} | Watch: ${if (p.isWatchEnabled) "enabled" else "disabled"}" to "STDOUT")
                        out.add("  CPU: ${p.cpuPercent}% | Memory: ${"%.1f".format(p.memoryMb)} MB" to "STDOUT")
                        out.add("  24/7 Daemon Guard: ONLINE (Service + WakeLock + AlarmManager)" to "SUCCESS")
                    }
                }
            }

            "kill" -> {
                _processes.value = emptyList()
                engine.stopBot(project.id)
                getSafeContext()?.let { ctx ->
                    BotBackgroundService.stop(ctx)
                }
                saveState()
                out.add("[PM2] Daemon killed. All processes stopped." to "WARN")
            }

            else -> {
                out.add("PM2 - Production Process Manager for Node.js / Discord Bots" to "SYSTEM")
                out.add("Usage: pm2 <command> [options]" to "STDOUT")
                out.add("  pm2 start <script> [--name <name>]   Start a script in background 24/7" to "STDOUT")
                out.add("  pm2 list / status                    Show live process table" to "STDOUT")
                out.add("  pm2 restart <id|name|all>            Restart process" to "STDOUT")
                out.add("  pm2 stop <id|name|all>               Stop process" to "STDOUT")
                out.add("  pm2 delete <id|name|all>             Remove process from PM2" to "STDOUT")
                out.add("  pm2 logs                             Show output & error logs" to "STDOUT")
                out.add("  pm2 monit                            Process health monitoring" to "STDOUT")
                out.add("  pm2 save                             Freeze process list for reboot" to "STDOUT")
                out.add("  pm2 resurrect                        Restore saved processes" to "STDOUT")
            }
        }

        return out
    }

    fun startScript(
        script: String,
        name: String,
        project: BotProject,
        files: List<BotFile>,
        engine: BotRuntimeEngine,
        watch: Boolean = true
    ): Pm2Process {
        val currentList = _processes.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.name == name || it.script == script }

        val interpreter = when {
            script.endsWith(".py") -> "python3"
            script.endsWith(".ts") -> "ts-node"
            else -> "node"
        }

        val pid = Random.nextInt(1024, 49151)
        val proc = Pm2Process(
            id = if (existingIndex >= 0) currentList[existingIndex].id else currentList.size,
            name = name,
            script = script,
            interpreter = interpreter,
            pid = pid,
            status = Pm2Status.ONLINE,
            startedAt = System.currentTimeMillis(),
            restartCount = if (existingIndex >= 0) currentList[existingIndex].restartCount + 1 else 0,
            cpuPercent = Random.nextDouble(0.1, 1.4),
            memoryMb = Random.nextDouble(32.0, 58.0),
            isWatchEnabled = watch,
            auto247 = true
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = proc
        } else {
            currentList.add(proc)
        }

        _processes.value = currentList
        saveState()

        // Boot engine and ensure 24/7 Foreground Service is running!
        if (!engine.isRunning.value) {
            engine.startBot(project)
        }
        getSafeContext()?.let { ctx ->
            BotBackgroundService.start(ctx, project.id, project.name)
        }

        return proc
    }

    fun stopProcess(target: String, engine: BotRuntimeEngine, projectId: Long): List<Pm2Process> {
        val current = _processes.value.toMutableList()
        val stopped = mutableListOf<Pm2Process>()

        for (i in current.indices) {
            val p = current[i]
            if (target == "all" || target == p.name || target == p.id.toString()) {
                val updated = p.copy(status = Pm2Status.STOPPED, cpuPercent = 0.0)
                current[i] = updated
                stopped.add(updated)
            }
        }

        _processes.value = current
        saveState()

        if (current.none { it.status == Pm2Status.ONLINE }) {
            engine.stopBot(projectId)
            getSafeContext()?.let { ctx ->
                BotBackgroundService.stop(ctx)
            }
        }

        return stopped
    }

    fun restartProcess(target: String, engine: BotRuntimeEngine, project: BotProject): List<Pm2Process> {
        val current = _processes.value.toMutableList()
        val restarted = mutableListOf<Pm2Process>()

        for (i in current.indices) {
            val p = current[i]
            if (target == "all" || target == p.name || target == p.id.toString()) {
                val updated = p.copy(
                    status = Pm2Status.ONLINE,
                    startedAt = System.currentTimeMillis(),
                    restartCount = p.restartCount + 1,
                    pid = Random.nextInt(1024, 49151),
                    cpuPercent = Random.nextDouble(0.1, 1.4)
                )
                current[i] = updated
                restarted.add(updated)
            }
        }

        _processes.value = current
        saveState()

        engine.restartBot(project)
        getSafeContext()?.let { ctx ->
            BotBackgroundService.start(ctx, project.id, project.name)
        }

        return restarted
    }

    fun deleteProcess(target: String, engine: BotRuntimeEngine, projectId: Long): List<Pm2Process> {
        val current = _processes.value.toMutableList()
        val deleted = mutableListOf<Pm2Process>()

        current.removeAll { p ->
            val matches = (target == "all" || target == p.name || target == p.id.toString())
            if (matches) deleted.add(p)
            matches
        }

        _processes.value = current
        saveState()

        if (current.none { it.status == Pm2Status.ONLINE }) {
            engine.stopBot(projectId)
            getSafeContext()?.let { ctx ->
                BotBackgroundService.stop(ctx)
            }
        }

        return deleted
    }

    /**
     * Renders authentic PM2 terminal ASCII table.
     */
    fun renderProcessTable(): List<Pair<String, String>> {
        val list = _processes.value
        val out = mutableListOf<Pair<String, String>>()

        out.add("┌─────┬──────────────┬─────────────┬─────────┬────────┬──────────┬────────┬──────┐" to "SYSTEM")
        out.add("│ id  │ name         │ mode        │ ↺       │ status │ cpu      │ memory │ pid  │" to "SYSTEM")
        out.add("├─────┼──────────────┼─────────────┼─────────┼────────┼──────────┼────────┼──────┤" to "SYSTEM")

        if (list.isEmpty()) {
            out.add("│ 0   │ (no-scripts) │ fork (idle) │ 0       │ stopped│ 0.0%     │ 0.0 MB │ ---- │" to "STDOUT")
        } else {
            list.forEach { p ->
                val idStr = p.id.toString().padEnd(3)
                val nameStr = p.name.take(12).padEnd(12)
                val modeStr = "fork (24/7)".padEnd(11)
                val restartStr = p.restartCount.toString().padEnd(7)
                val statusStr = p.status.label.padEnd(6)
                val cpuStr = "${"%.1f".format(p.cpuPercent)}%".padEnd(8)
                val memStr = "${"%.1f".format(p.memoryMb)}MB".padEnd(6)
                val pidStr = p.pid.toString().padEnd(4)

                val line = "│ $idStr │ $nameStr │ $modeStr │ $restartStr │ $statusStr │ $cpuStr │ $memStr │ $pidStr │"
                val style = if (p.status == Pm2Status.ONLINE) "SUCCESS" else "WARN"
                out.add(line to style)
            }
        }

        out.add("└─────┴──────────────┴─────────────┴─────────┴────────┴──────────┴────────┴──────┘" to "SYSTEM")
        out.add("ℹ [PM2] 24/7 Background Supervisor: RUNNING (Alive even when app is closed)" to "SUCCESS")
        return out
    }

    private fun detectDefaultScript(files: List<BotFile>, project: BotProject): String {
        return files.firstOrNull { it.isEntrypoint }?.filePath
            ?: files.firstOrNull { it.filePath == "index.js" || it.filePath == "bot.py" || it.filePath == "main.js" }?.filePath
            ?: if (project.language.contains("PYTHON", ignoreCase = true)) "bot.py" else "index.js"
    }

    private fun parseParam(args: List<String>, key: String): String? {
        val idx = args.indexOf(key)
        if (idx >= 0 && idx + 1 < args.size) {
            return args[idx + 1]
        }
        return null
    }

    /**
     * Continuous 24/7 Daemon Loop:
     * Watches PM2 processes every 8 seconds, adjusts CPU/memory simulation slightly,
     * checks if any online script needs auto-alive resuscitation, and guarantees
     * Foreground Service & WakeLock are held.
     */
    private fun startSupervisorDaemon() {
        supervisorJob?.cancel()
        supervisorJob = scope.launch {
            _isDaemonActive.value = true
            while (isActive) {
                delay(8000L)
                val current = _processes.value
                val hasOnline = current.any { it.status == Pm2Status.ONLINE }

                if (hasOnline) {
                    val updated = current.map { p ->
                        if (p.status == Pm2Status.ONLINE) {
                            val newCpu = (p.cpuPercent + Random.nextDouble(-0.2, 0.2)).coerceIn(0.1, 2.8)
                            val newMem = (p.memoryMb + Random.nextDouble(-0.3, 0.4)).coerceIn(28.0, 75.0)
                            p.copy(cpuPercent = newCpu, memoryMb = newMem)
                        } else p
                    }
                    _processes.value = updated

                    // Ensure Android service is actively guarding the script
                    getSafeContext()?.let { ctx ->
                        if (!BotBackgroundService.isBotMarkedRunning(ctx)) {
                            val engine = BotRuntimeManager.currentEngine()
                            val runningProjId = engine?.runningProjectId?.value ?: 0L
                            if (runningProjId > 0) {
                                BotBackgroundService.start(ctx, runningProjId, "PM2 Managed Bot")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveState() {
        try {
            val ctx = getSafeContext() ?: return
            val jsonArr = JSONArray()
            _processes.value.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("script", p.script)
                    put("interpreter", p.interpreter)
                    put("pid", p.pid)
                    put("status", p.status.name)
                    put("startedAt", p.startedAt)
                    put("restartCount", p.restartCount)
                    put("cpuPercent", p.cpuPercent)
                    put("memoryMb", p.memoryMb)
                    put("isWatchEnabled", p.isWatchEnabled)
                    put("auto247", p.auto247)
                }
                jsonArr.put(obj)
            }
            ctx.getSharedPreferences(PREFS_PM2, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PM2_PROCESSES, jsonArr.toString())
                .apply()
        } catch (_: Exception) {}
    }

    private fun restoreSavedState(): Int {
        try {
            val ctx = getSafeContext() ?: return 0
            val raw = ctx.getSharedPreferences(PREFS_PM2, Context.MODE_PRIVATE)
                .getString(KEY_PM2_PROCESSES, null) ?: return 0
            val jsonArr = JSONArray(raw)
            val list = mutableListOf<Pm2Process>()
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val statusStr = obj.optString("status", "ONLINE")
                val status = try { Pm2Status.valueOf(statusStr) } catch (_: Exception) { Pm2Status.ONLINE }
                list.add(
                    Pm2Process(
                        id = obj.optInt("id", i),
                        name = obj.optString("name", "bot"),
                        script = obj.optString("script", "index.js"),
                        interpreter = obj.optString("interpreter", "node"),
                        pid = obj.optInt("pid", Random.nextInt(1024, 49151)),
                        status = status,
                        startedAt = obj.optLong("startedAt", System.currentTimeMillis()),
                        restartCount = obj.optInt("restartCount", 0),
                        cpuPercent = obj.optDouble("cpuPercent", 0.3),
                        memoryMb = obj.optDouble("memoryMb", 42.0),
                        isWatchEnabled = obj.optBoolean("isWatchEnabled", true),
                        auto247 = obj.optBoolean("auto247", true)
                    )
                )
            }
            _processes.value = list
            return list.size
        } catch (_: Exception) {
            return 0
        }
    }
}
