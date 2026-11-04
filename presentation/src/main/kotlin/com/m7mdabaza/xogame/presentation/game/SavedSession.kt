package com.m7mdabaza.xogame.presentation.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The game screen's state as stored in `SavedStateHandle`, so a match
 * survives process death. A round is saved as its first player plus the
 * moves (cell indexes `row * size + col`) and rebuilt by replaying them.
 * Enums are stored by name.
 */
@Serializable
internal data class SavedSession(
    val humanSymbol: String,
    val starter: String,
    val length: String,
    val xName: String,
    val oName: String,
    val showSetup: Boolean,
    val hasStarted: Boolean,
    /** The resolved first player of round 1 of the match (rounds alternate from it). */
    val matchFirstPlayer: String,
    val roundFirstPlayer: String,
    val moves: List<Int>,
    val xWins: Int,
    val oWins: Int,
    val draws: Int,
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Null when there is nothing saved or it can't be read (e.g. after an app update). */
        fun decode(text: String?): SavedSession? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
