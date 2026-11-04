package com.m7mdabaza.xogame.domain.model

/** A mark on the board. */
enum class Player {
    X,
    O;

    /** The other player. */
    val opponent: Player
        get() = if (this == X) O else X
}
