package com.m7mdabaza.xogame.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Who moves first in a match. Player one is the human against the computer,
 * or X in two-player mode.
 */
enum class Starter { PLAYER_ONE, PLAYER_TWO, RANDOM }

/**
 * App settings. The language is not here on purpose: Android stores the
 * per-app language itself (AppCompat / system settings), so keeping a second
 * copy could only get out of sync.
 *
 * The match-setup properties (match length to names) are the defaults of the
 * next setup, remembered from the last match the user started. [boardSize] and
 * [level] are the last choice in the "against the phone" picker.
 */
data class Settings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val matchLength: MatchLength = MatchLength.SINGLE,
    val humanSymbol: Player = Player.X,
    val starter: Starter = Starter.PLAYER_ONE,
    val xName: String = "",
    val oName: String = "",
    val boardSize: BoardSize = BoardSize.THREE,
    val level: Level = Level.EASY,
)
