package com.m7mdabaza.xogame.domain.model

/** Who the human plays against. */
enum class GameMode {
    /** One human against the phone's AI. */
    VS_COMPUTER,

    /** Two humans sharing one device. */
    TWO_PLAYERS,

    /**
     * Two humans, each on their own phone, connected nearby (see
     * `domain/multiplayer`). Each phone sees the game from its own player's
     * side: `GameConfig.humanSymbol` is this phone's mark.
     */
    TWO_PHONES,
}
