package com.example.engine

import com.example.data.repository.BotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manages the singleton BotRuntimeEngine lifecycle tied to an application-level CoroutineScope.
 * This guarantees that when the user closes the app, minimizes it, or locks the phone,
 * all Gateway WebSockets, heartbeat loops, and bot listeners remain active in the background.
 */
object BotRuntimeManager {

    // Persistent application-level scope that never terminates when Activities or ViewModels close
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var engineInstance: BotRuntimeEngine? = null

    fun getEngine(repository: BotRepository): BotRuntimeEngine {
        return engineInstance ?: synchronized(this) {
            engineInstance ?: BotRuntimeEngine(repository, applicationScope).also {
                engineInstance = it
            }
        }
    }

    fun hasEngine(): Boolean = engineInstance != null

    fun currentEngine(): BotRuntimeEngine? = engineInstance
}
