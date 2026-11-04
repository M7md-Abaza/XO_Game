package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.testing.cell
import com.m7mdabaza.xogame.domain.testing.config
import com.m7mdabaza.xogame.domain.testing.newGame
import com.m7mdabaza.xogame.domain.testing.playing
import com.m7mdabaza.xogame.domain.testing.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SearchAiTest {

    private val difficulties: List<Pair<String, (Level) -> Difficulty>> = listOf(
        "computer" to DifficultyPolicy::computer,
        "hint" to DifficultyPolicy::hint,
        "always random" to { level -> DifficultyPolicy.computer(level).copy(mistakeChance = 1.0) },
    )

    /** Plays [moves] as X, O, X, ... on an empty board of [level]. */
    private fun position(level: Level, vararg moves: Cell): GameState = newGame(config(level = level), *moves)

    /** Asserts every difficulty, over many random seeds, plays [expected] in [state]. */
    private fun assertAlwaysPlays(expected: Set<Cell>, state: GameState) {
        for ((name, policy) in difficulties) {
            repeat(30) { seed ->
                val move = SearchAi(Random(seed)).chooseMove(state, policy(state.config.level))
                assertTrue("$name (seed $seed) played $move on ${state.board}, expected one of $expected", move in expected)
            }
        }
    }

    // --- Win and block in one, on every size --------------------------------

    @Test
    fun `takes a win in one on 3x3`() {
        // X: (0,0) (0,1); O: (1,1) (2,2). X to move wins at (0,2).
        assertAlwaysPlays(setOf(cell(0, 2)), position(Level.EASY, cell(0, 0), cell(1, 1), cell(0, 1), cell(2, 2)))
    }

    @Test
    fun `blocks a loss in one on 3x3`() {
        // X: (0,0) (0,1); O: (1,1). O to move must block (0,2).
        assertAlwaysPlays(setOf(cell(0, 2)), position(Level.EASY, cell(0, 0), cell(1, 1), cell(0, 1)))
    }

    @Test
    fun `prefers winning to blocking`() {
        // X: (0,0) (0,1) (2,0); O: (1,0) (1,1). O to move: win at (1,2) instead of blocking (0,2).
        val state = position(Level.EASY, cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(2, 0))
        assertAlwaysPlays(setOf(cell(1, 2)), state)
    }

    @Test
    fun `takes a win in one on 4x4`() {
        // X: row 0 cols 0-2; O: row 3 cols 0-2 except it is X's turn.
        val state = position(Level.MEDIUM, cell(0, 0), cell(3, 0), cell(0, 1), cell(3, 1), cell(0, 2), cell(2, 3))
        assertAlwaysPlays(setOf(cell(0, 3)), state)
    }

    @Test
    fun `blocks a loss in one on 4x4`() {
        // X threatens the anti-diagonal (0,3) (1,2) (2,1) -> (3,0).
        val state = position(Level.MEDIUM, cell(0, 3), cell(0, 0), cell(1, 2), cell(1, 0), cell(2, 1))
        assertAlwaysPlays(setOf(cell(3, 0)), state)
    }

    @Test
    fun `takes a win in one on 8x8`() {
        // X: (4,1)..(4,4); O scattered. X to move: (4,0) or (4,5).
        val state = position(
            Level.HARD,
            cell(4, 1), cell(0, 0), cell(4, 2), cell(0, 7), cell(4, 3), cell(7, 7), cell(4, 4), cell(7, 0),
        )
        assertAlwaysPlays(setOf(cell(4, 0), cell(4, 5)), state)
    }

    @Test
    fun `blocks a loss in one on 8x8`() {
        // X: diagonal (1,1)..(4,4), (0,0) taken by O, so X wins only at (5,5).
        val state = position(
            Level.HARD,
            cell(1, 1), cell(0, 0), cell(2, 2), cell(7, 0), cell(3, 3), cell(0, 7), cell(4, 4),
        )
        assertAlwaysPlays(setOf(cell(5, 5)), state)
    }

    @Test
    fun `8x8 blocks an open four's only remaining end`() {
        // X: (2,2)..(2,5) is a four; (2,1) is already O so (2,6) is the only block.
        val state = position(
            Level.HARD,
            cell(2, 2), cell(2, 1), cell(2, 3), cell(7, 7), cell(2, 4), cell(7, 0), cell(2, 5),
        )
        assertAlwaysPlays(setOf(cell(2, 6)), state)
    }

    // --- 3×3 full strength is perfect ----------------------------------------

    @Test
    fun `3x3 full strength never loses, as X or as O, against every possible opponent`() {
        for (aiPlayer in Player.entries) {
            val seen = HashSet<String>()
            val losses = mutableListOf<String>()
            fun explore(state: GameState) {
                if (!seen.add(state.board.toString())) return
                when {
                    state.result is GameResult.Win -> {
                        if (state.result.player != aiPlayer) losses += state.board.toString()
                    }
                    state.isOver -> Unit
                    state.turn == aiPlayer ->
                        // Every move the AI rates best, not just one random pick.
                        SearchAi(Random(0)).bestMoves(state, DifficultyPolicy.hint(Level.EASY))
                            .forEach { explore(state.playing(it)) }
                    else -> state.board.emptyCells.forEach { explore(state.playing(it)) }
                }
            }
            explore(start(config(level = Level.EASY, firstPlayer = Player.X)))
            assertEquals("AI as $aiPlayer lost in: $losses", emptyList<String>(), losses)
        }
    }

    @Test
    fun `3x3 full strength plays a fork when it can`() {
        // X: (0,0) (2,0); O: (0,1) (1,0). X to move. (1,1) and (2,2) both make two
        // threats at once, so X wins by force.
        val state = position(Level.EASY, cell(0, 0), cell(0, 1), cell(2, 0), cell(1, 0))
        val best = SearchAi(Random(0)).bestMoves(state, DifficultyPolicy.hint(Level.EASY))
        assertEquals(setOf(cell(1, 1), cell(2, 2)), best.toSet())
        // And it converts: perfect play from here still ends in an X win.
        for (move in best) {
            val end = playOut(state.playing(move), hintAi(), hintAi())
            assertEquals(Player.X, (end.result as GameResult.Win).player)
        }
    }

    // --- Strength checks ------------------------------------------------------

    private fun hintAi(seed: Int = 0): (GameState) -> Cell {
        val ai = SearchAi(Random(seed))
        return { ai.chooseMove(it, DifficultyPolicy.hint(it.config.level)) }
    }

    private fun computerAi(seed: Int = 0): (GameState) -> Cell {
        val ai = SearchAi(Random(seed))
        return { ai.chooseMove(it, DifficultyPolicy.computer(it.config.level)) }
    }

    private fun randomMover(seed: Int): (GameState) -> Cell {
        val random = Random(seed)
        return { it.board.emptyCells.random(random) }
    }

    private fun playOut(state: GameState, x: (GameState) -> Cell, o: (GameState) -> Cell): GameState {
        var s = state
        while (!s.isOver) s = s.playing(if (s.turn == Player.X) x(s) else o(s))
        return s
    }

    /** Wins / draws / losses for [ai] over [games], alternating who starts. */
    private fun record(level: Level, games: Int, ai: (Int) -> (GameState) -> Cell, opponent: (Int) -> (GameState) -> Cell): Triple<Int, Int, Int> {
        var wins = 0
        var draws = 0
        var losses = 0
        repeat(games) { g ->
            val aiPlays = if (g % 2 == 0) Player.X else Player.O
            val me = ai(g)
            val them = opponent(g + 1000)
            val end = playOut(
                start(config(GameMode.TWO_PLAYERS, level, firstPlayer = Player.X)),
                if (aiPlays == Player.X) me else them,
                if (aiPlays == Player.O) me else them,
            )
            when (val r = end.result) {
                is GameResult.Win -> if (r.player == aiPlays) wins++ else losses++
                else -> draws++
            }
        }
        return Triple(wins, draws, losses)
    }

    @Test
    fun `4x4 full strength never loses to random play`() {
        val (_, _, losses) = record(Level.MEDIUM, 20, ::hintAi, ::randomMover)
        assertEquals(0, losses)
    }

    @Test
    fun `8x8 computer and hint beat random play`() {
        val computer = record(Level.HARD, 10, ::computerAi, ::randomMover)
        val hint = record(Level.HARD, 10, ::hintAi, ::randomMover)
        assertEquals("computer W/D/L $computer", 10, computer.first)
        assertEquals("hint W/D/L $hint", 10, hint.first)
    }

    @Test
    fun `easy computer is beatable by perfect play`() {
        val (_, _, lossesToPerfect) = record(Level.EASY, 100, ::computerAi, ::hintAi)
        assertTrue("Easy should lose sometimes to perfect play", lossesToPerfect > 0)
    }

    @Test
    fun `strength check - computer vs full strength hint AI (printed)`() {
        for ((level, games) in listOf(Level.EASY to 100, Level.MEDIUM to 20, Level.HARD to 20)) {
            val started = System.nanoTime()
            val (w, d, l) = record(level, games, ::computerAi, ::hintAi)
            val ms = (System.nanoTime() - started) / 1_000_000
            println("$level computer vs hint AI over $games games: W$w D$d L$l ($ms ms)")
        }
    }
}
