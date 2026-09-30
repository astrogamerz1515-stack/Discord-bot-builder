package com.example.engine

/**
 * Complete Discord Gateway Intents (Unprivileged and Privileged)
 * Bitmask representations according to Discord Developer Documentation.
 */
enum class GatewayIntent(
    val bit: Int,
    val flagName: String,
    val description: String,
    val isPrivileged: Boolean = false
) {
    GUILDS(1 shl 0, "GUILDS", "Server create/update/delete, role updates, channel updates"),
    GUILD_MEMBERS(1 shl 1, "GUILD_MEMBERS", "Member join/leave/update, role changes (Privileged)", isPrivileged = true),
    GUILD_MODERATION(1 shl 2, "GUILD_MODERATION", "Guild audit log, bans, timeouts, kicks"),
    GUILD_EMOJIS_AND_STICKERS(1 shl 3, "GUILD_EMOJIS_AND_STICKERS", "Custom emojis and stickers updates"),
    GUILD_INTEGRATIONS(1 shl 4, "GUILD_INTEGRATIONS", "Third-party bot integrations and webhooks"),
    GUILD_WEBHOOKS(1 shl 5, "GUILD_WEBHOOKS", "Webhook create, update, and delete"),
    GUILD_INVITES(1 shl 6, "GUILD_INVITES", "Server invite creation and deletion"),
    GUILD_VOICE_STATES(1 shl 7, "GUILD_VOICE_STATES", "Voice channel join/leave, mute, deafen, DAVE"),
    GUILD_PRESENCES(1 shl 8, "GUILD_PRESENCES", "User activity, status, game presence (Privileged)", isPrivileged = true),
    GUILD_MESSAGES(1 shl 9, "GUILD_MESSAGES", "Guild message creation, editing, deletion"),
    GUILD_MESSAGE_REACTIONS(1 shl 10, "GUILD_MESSAGE_REACTIONS", "Message emoji reactions and reaction removal"),
    GUILD_MESSAGE_TYPING(1 shl 11, "GUILD_MESSAGE_TYPING", "User typing start notifications in guilds"),
    DIRECT_MESSAGES(1 shl 12, "DIRECT_MESSAGES", "Direct message send, edit, delete in DMs"),
    DIRECT_MESSAGE_REACTIONS(1 shl 13, "DIRECT_MESSAGE_REACTIONS", "Reactions on direct messages"),
    DIRECT_MESSAGE_TYPING(1 shl 14, "DIRECT_MESSAGE_TYPING", "User typing start notifications in DMs"),
    MESSAGE_CONTENT(1 shl 15, "MESSAGE_CONTENT", "Access to message.content, embeds, attachments (Privileged)", isPrivileged = true),
    GUILD_SCHEDULED_EVENTS(1 shl 16, "GUILD_SCHEDULED_EVENTS", "Scheduled events creation, updates, RSVPs"),
    AUTO_MODERATION_CONFIGURATION(1 shl 20, "AUTO_MODERATION_CONFIGURATION", "AutoMod rules create/update/delete"),
    AUTO_MODERATION_EXECUTION(1 shl 21, "AUTO_MODERATION_EXECUTION", "AutoMod rule execution events");

    companion object {
        /**
         * Calculates the cumulative integer bitmask for a collection of selected intents.
         */
        fun calculateBitmask(intents: Collection<GatewayIntent>): Int {
            return intents.fold(0) { acc, intent -> acc or intent.bit }
        }

        /**
         * Default safe intent set for standard developer bots.
         */
        val defaultIntents: Set<GatewayIntent> = setOf(
            GUILDS,
            GUILD_MESSAGES,
            DIRECT_MESSAGES,
            MESSAGE_CONTENT,
            GUILD_MEMBERS
        )
    }
}

/**
 * Standard Discord Gateway Opcodes.
 */
object GatewayOpcodes {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val VOICE_STATE_UPDATE = 4
    const val RESUME = 6
    const val RECONNECT = 7
    const val REQUEST_GUILD_MEMBERS = 8
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
    const val REQUEST_SOUNDBOARD_SOUNDS = 31
    const val REQUEST_CHANNEL_INFO = 43
}

/**
 * Voice Gateway Opcodes including DAVE E2EE protocol negotiation.
 */
object VoiceGatewayOpcodes {
    const val IDENTIFY = 0
    const val SELECT_PROTOCOL = 1
    const val READY = 2
    const val HEARTBEAT = 3
    const val SESSION_DESCRIPTION = 4
    const val SPEAKING = 5
    const val HEARTBEAT_ACK = 6
    const val RESUME = 7
    const val HELLO = 8
    const val RESUMED = 9
    const val DAVE_PREPARE_TRANSITION = 21
    const val DAVE_EXECUTE_TRANSITION = 22
    const val DAVE_TRANSITION_READY = 23
    const val DAVE_PREPARE_EPOCH = 24
}

/**
 * Interaction Response Types.
 */
object InteractionResponseType {
    const val PONG = 1
    const val CHANNEL_MESSAGE_WITH_SOURCE = 4
    const val DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE = 5
    const val DEFERRED_UPDATE_MESSAGE = 6
    const val UPDATE_MESSAGE = 7
    const val APPLICATION_COMMAND_AUTOCOMPLETE_RESULT = 8
    const val MODAL = 9
    const val LAUNCH_ACTIVITY = 12
}

/**
 * Status snapshot of the Discord bot Gateway connection.
 */
data class GatewayStatus(
    val isConnected: Boolean = false,
    val sessionId: String? = null,
    val resumeGatewayUrl: String? = null,
    val lastSequence: Int? = null,
    val shardId: Int = 0,
    val shardCount: Int = 1,
    val pingMs: Int = 0,
    val heartbeatIntervalMs: Long = 41250L,
    val lastHeartbeatSentAt: Long = 0L,
    val lastHeartbeatAckAt: Long = 0L,
    val isZombieDetected: Boolean = false,
    val activeGuildsCount: Int = 0,
    val botUsername: String = "",
    val botUserId: String = "",
    val currentIntentsMask: Int = 0,
    val daveE2eeActive: Boolean = false,
    val cloudflareWarningCount: Int = 0
)
