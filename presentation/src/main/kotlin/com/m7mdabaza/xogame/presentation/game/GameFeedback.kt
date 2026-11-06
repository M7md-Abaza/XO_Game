package com.m7mdabaza.xogame.presentation.game

import com.m7mdabaza.xogame.domain.model.Player

/** How a finished round sounds from the point of view of the person holding the phone. */
enum class RoundSound { WIN, LOSE, DRAW }

/**
 * Sounds and vibration for game events. An interface so the ViewModel can be
 * tested without Android; the real implementation is `AndroidGameFeedback`.
 * Milestone 5 gates both on the sound and vibration settings.
 */
interface GameFeedback {
    /**
     * A mark was placed. [byComputer] moves make a sound but don't vibrate; so
     * do the friend's moves in a two-phone game.
     */
    fun move(player: Player, byComputer: Boolean)

    fun roundEnded(sound: RoundSound)

    fun newRound()
}
