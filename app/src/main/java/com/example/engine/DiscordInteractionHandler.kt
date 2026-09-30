package com.example.engine

import org.json.JSONArray
import org.json.JSONObject

/**
 * Encapsulates response builders for all Discord Interaction Response types.
 */
object DiscordInteractionHandler {

    /**
     * Type 1: PONG (Handshake response to webhook ping).
     */
    fun createPongResponse(): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.PONG)
    }

    /**
     * Type 4: CHANNEL_MESSAGE_WITH_SOURCE (Standard reply with message / embed).
     */
    fun createMessageResponse(
        content: String,
        ephemeral: Boolean = false,
        embed: JSONObject? = null
    ): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE)
        put("data", JSONObject().apply {
            put("content", content)
            if (ephemeral) put("flags", 64) // Ephemeral message
            if (embed != null) put("embeds", JSONArray().put(embed))
        })
    }

    /**
     * Type 5: DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE (Bot is thinking...).
     */
    fun createDeferredMessageResponse(ephemeral: Boolean = false): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE)
        if (ephemeral) {
            put("data", JSONObject().put("flags", 64))
        }
    }

    /**
     * Type 6: DEFERRED_UPDATE_MESSAGE (Acknowledge component click without modifying message immediately).
     */
    fun createDeferredUpdateResponse(): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.DEFERRED_UPDATE_MESSAGE)
    }

    /**
     * Type 7: UPDATE_MESSAGE (Edit the message the component was attached to).
     */
    fun createUpdateMessageResponse(
        content: String,
        embed: JSONObject? = null
    ): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.UPDATE_MESSAGE)
        put("data", JSONObject().apply {
            put("content", content)
            if (embed != null) put("embeds", JSONArray().put(embed))
        })
    }

    /**
     * Type 8: APPLICATION_COMMAND_AUTOCOMPLETE_RESULT (Autocomplete dropdown choices).
     */
    fun createAutocompleteResponse(choices: List<Pair<String, String>>): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT)
        put("data", JSONObject().apply {
            put("choices", JSONArray().apply {
                choices.forEach { (name, value) ->
                    put(JSONObject().apply {
                        put("name", name)
                        put("value", value)
                    })
                }
            })
        })
    }

    /**
     * Type 9: MODAL (Interactive popup form with text inputs).
     */
    fun createModalResponse(
        customId: String,
        title: String,
        inputs: List<Pair<String, String>> // customId to label
    ): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.MODAL)
        put("data", JSONObject().apply {
            put("custom_id", customId)
            put("title", title)
            put("components", JSONArray().apply {
                inputs.forEach { (inpId, inpLabel) ->
                    put(JSONObject().apply {
                        put("type", 1) // Action row
                        put("components", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", 4) // Text input
                                put("custom_id", inpId)
                                put("label", inpLabel)
                                put("style", 1) // Short
                                put("required", true)
                            })
                        })
                    })
                }
            })
        })
    }

    /**
     * Type 12: LAUNCH_ACTIVITY (Launch embedded Discord activity).
     */
    fun createLaunchActivityResponse(): JSONObject = JSONObject().apply {
        put("type", InteractionResponseType.LAUNCH_ACTIVITY)
    }
}
