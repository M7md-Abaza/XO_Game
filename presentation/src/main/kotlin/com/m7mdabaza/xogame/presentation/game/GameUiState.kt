package com.m7mdabaza.xogame.presentation.game

import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.MatchState
import com.m7mdabaza.xogame.domain.model.Player
import kotlin.random.Random

/**
 * Who moves first. Player one is the human against the computer, or X in
 * two-player mode.
 */
enum class Starter { PLAYER_ONE, PLAYER_TWO, RANDOM }

/** Everything chosen in the setup dialog before a match. */
data class MatchSettings(
    /** The human's mark against the computer (two-player mode always uses X for player one). */
    val humanSymbol: Player = Player.X,
    val starter: Starter = Starter.PLAYER_ONE,
    val length: MatchLength = MatchLength.SINGLE,
    /** Two-player names; blank means the default "Player X" / "Player O". */
    val xName: String = "",
    val oName: String = "",
) {
    fun playerOne(mode: GameMode): Player = if (mode == GameMode.VS_COMPUTER) humanSymbol else Player.X

    /** The concrete first player for [starter], rolling the dice for [Starter.RANDOM]. */
    fun resolveStarter(mode: GameMode, random: Random): Player = when (starter) {
        Starter.PLAYER_ONE -> playerOne(mode)
        Starter.PLAYER_TWO -> playerOne(mode).opponent
        Starter.RANDOM -> if (random.nextBoolean()) Player.X else Player.O
    }
}

enum class GamePhase {
    /** The setup dialog is open. */
    SETUP,
    PLAYING,

    /** A round ended; the match goes on. */
    ROUND_OVER,

    /** A best-of match ended. */
    MATCH_OVER,
}

data class GameUiState(
    val mode: GameMode,
    val level: Level,
    val settings: MatchSettings,
    /** The current round (an empty preview board before the first match starts). */
    val game: GameState,
    val match: MatchState,
    val showSetup: Boolean,
    /** False until the first match starts; closing the setup then leaves the screen. */
    val hasStarted: Boolean,
    val computerThinking: Boolean = false,
    val hintLoading: Boolean = false,
    /** The suggested cell, shown until the next move. */
    val hint: Cell? = null,
    val canUndo: Boolean = false,
    val bannerAdUnitId: String,
) {
    val phase: GamePhase
        get() = when {
            showSetup -> GamePhase.SETUP
            !game.isOver -> GamePhase.PLAYING
            match.isOver -> GamePhase.MATCH_OVER
            else -> GamePhase.ROUND_OVER
        }

    /** A human may tap a cell now. */
    val boardEnabled: Boolean
        get() = phase == GamePhase.PLAYING && game.isHumanTurn && !computerThinking

    val canHint: Boolean
        get() = boardEnabled && !hintLoading && hint == null
}
