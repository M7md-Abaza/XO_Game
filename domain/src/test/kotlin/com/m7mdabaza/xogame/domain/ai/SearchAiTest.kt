package com.m7mdabaza.xogame.domain.ai

import com.m7mdabaza.xogame.domain.model.BoardSize
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

    /** The computer at every level, the hint helper, and a computer that only plays random candidates. */
    private val difficulties: List<Pair<String, (BoardSize) -> Difficulty>> =
        Level.entries.map { level -> "computer $level" to { size: BoardSize -> DifficultyPolicy.computer(size, level) } } +
            listOf(
                "hint" to DifficultyPolicy::hint,
                "always random" to { size -> DifficultyPolicy.computer(size, Level.HARD).copy(mistakeChance = 1.0) },
            )

    /** Plays [moves] as X, O, X, ... on an empty [boardSize] board. */
    private fun position(boardSize: BoardSize, vararg moves: Cell): GameState = newGame(config(boardSize = boardSize), *moves)

    /**
     * Asserts every difficulty, over many random seeds, plays [expected] in
     * [state]. With [isBlock], difficulties allowed to overlook a threat
     * (4×4 Easy and Medium) are left out; see the missed-block tests.
     */
    private fun assertAlwaysPlays(expected: Set<Cell>, state: GameState, isBlock: Boolean = false) {
        for ((name, policy) in difficulties) {
            val difficulty = policy(state.config.boardSize)
            if (isBlock && difficulty.missedBlockChance > 0) continue
            repeat(30) { seed ->
                val move = SearchAi(Random(seed)).chooseMove(state, difficulty)
                assertTrue("$name (seed $seed) played $move on ${state.board}, expected one of $expected", move in expected)
            }
        }
    }

    // --- Win and block in one, on every size --------------------------------

    @Test
    fun `takes a win in one on 3x3`() {
        // X: (0,0) (0,1); O: (1,1) (2,2). X to move wins at (0,2).
        assertAlwaysPlays(setOf(cell(0, 2)), position(BoardSize.THREE, cell(0, 0), cell(1, 1), cell(0, 1), cell(2, 2)))
    }

    @Test
    fun `blocks a loss in one on 3x3`() {
        // X: (0,0) (0,1); O: (1,1). O to move must block (0,2).
        assertAlwaysPlays(setOf(cell(0, 2)), position(BoardSize.THREE, cell(0, 0), cell(1, 1), cell(0, 1)), isBlock = true)
    }

    @Test
    fun `prefers winning to blocking`() {
        // X: (0,0) (0,1) (2,0); O: (1,0) (1,1). O to move: win at (1,2) instead of blocking (0,2).
        val state = position(BoardSize.THREE, cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1), cell(2, 0))
        assertAlwaysPlays(setOf(cell(1, 2)), state)
    }

    @Test
    fun `takes a win in one on 4x4`() {
        // X: row 0 cols 0-2; O: row 3 cols 0-2 except it is X's turn.
        val state = position(BoardSize.FOUR, cell(0, 0), cell(3, 0), cell(0, 1), cell(3, 1), cell(0, 2), cell(2, 3))
        assertAlwaysPlays(setOf(cell(0, 3)), state)
    }

    @Test
    fun `prefers winning to blocking on 4x4, even at the levels that miss blocks`() {
        // X: row 0 cols 0-2; O: row 3 cols 0-2. X to move wins at (0,3) instead of blocking (3,3).
        val state = position(BoardSize.FOUR, cell(0, 0), cell(3, 0), cell(0, 1), cell(3, 1), cell(0, 2), cell(3, 2))
        assertAlwaysPlays(setOf(cell(0, 3)), state)
    }

    @Test
    fun `blocks a loss in one on 4x4`() {
        // X threatens the anti-diagonal (0,3) (1,2) (2,1) -> (3,0).
        val state = position(BoardSize.FOUR, cell(0, 3), cell(0, 0), cell(1, 2), cell(1, 0), cell(2, 1))
        assertAlwaysPlays(setOf(cell(3, 0)), state, isBlock = true)
    }

    @Test
    fun `takes a win in one on 8x8`() {
        // X: (4,1)..(4,4); O scattered. X to move: (4,0) or (4,5).
        val state = position(
            BoardSize.EIGHT,
            cell(4, 1), cell(0, 0), cell(4, 2), cell(0, 7), cell(4, 3), cell(7, 7), cell(4, 4), cell(7, 0),
        )
        assertAlwaysPlays(setOf(cell(4, 0), cell(4, 5)), state)
    }

    @Test
    fun `blocks a loss in one on 8x8`() {
        // X: diagonal (1,1)..(4,4), (0,0) taken by O, so X wins only at (5,5).
        val state = position(
            BoardSize.EIGHT,
            cell(1, 1), cell(0, 0), cell(2, 2), cell(7, 0), cell(3, 3), cell(0, 7), cell(4, 4),
        )
        assertAlwaysPlays(setOf(cell(5, 5)), state, isBlock = true)
    }

    @Test
    fun `8x8 blocks an open four's only remaining end`() {
        // X: (2,2)..(2,5) is a four; (2,1) is already O so (2,6) is the only block.
        val state = position(
            BoardSize.EIGHT,
            cell(2, 2), cell(2, 1), cell(2, 3), cell(7, 7), cell(2, 4), cell(7, 0), cell(2, 5),
        )
        assertAlwaysPlays(setOf(cell(2, 6)), state, isBlock = true)
    }

    // --- Missed blocks (4×4 Easy and Medium only) -----------------------------

    @Test
    fun `only 4x4 Easy and Medium may miss a block, and hints never do`() {
        for (size in BoardSize.entries) {
            assertEquals(0.0, DifficultyPolicy.hint(size).missedBlockChance, 0.0)
            for (level in Level.entries) {
                val misses = DifficultyPolicy.computer(size, level).missedBlockChance > 0
                assertEquals("$size $level", size == BoardSize.FOUR && level != Level.HARD, misses)
            }
        }
    }

    @Test
    fun `4x4 Easy misses blocks more often than Medium`() {
        val state = position(BoardSize.FOUR, cell(0, 3), cell(0, 0), cell(1, 2), cell(1, 0), cell(2, 1))
        fun missRate(level: Level): Double {
            val difficulty = DifficultyPolicy.computer(BoardSize.FOUR, level)
            val misses = (0 until 1000).count { seed -> SearchAi(Random(seed)).chooseMove(state, difficulty) != cell(3, 0) }
            return misses / 1000.0
        }
        val easy = missRate(Level.EASY)
        val medium = missRate(Level.MEDIUM)
        val expectedEasy = DifficultyPolicy.computer(BoardSize.FOUR, Level.EASY).missedBlockChance
        val expectedMedium = DifficultyPolicy.computer(BoardSize.FOUR, Level.MEDIUM).missedBlockChance
        assertEquals("Easy miss rate", expectedEasy, easy, 0.05)
        assertEquals("Medium miss rate", expectedMedium, medium, 0.05)
        assertTrue(easy > medium)
    }

    // --- 3×3 full strength is perfect ----------------------------------------

    /** Every 3×3 game the AI at [difficulty] can reach, against every possible opponent: none may be lost. */
    private fun assertNeverLoses3x3(difficulty: Difficulty) {
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
                        SearchAi(Random(0)).bestMoves(state, difficulty).forEach { explore(state.playing(it)) }
                    else -> state.board.emptyCells.forEach { explore(state.playing(it)) }
                }
            }
            explore(start(config(firstPlayer = Player.X)))
            assertEquals("AI as $aiPlayer lost in: $losses", emptyList<String>(), losses)
        }
    }

    @Test
    fun `3x3 hints never lose, as X or as O, against every possible opponent`() {
        assertNeverLoses3x3(DifficultyPolicy.hint(BoardSize.THREE))
    }

    @Test
    fun `3x3 Hard is unbeatable`() {
        val hard = DifficultyPolicy.computer(BoardSize.THREE, Level.HARD)
        // No random moves, so it always plays one of the moves checked below.
        assertEquals(0.0, hard.mistakeChance, 0.0)
        assertEquals(0.0, hard.missedBlockChance, 0.0)
        assertNeverLoses3x3(hard)
    }

    @Test
    fun `3x3 full strength plays a fork when it can`() {
        // X: (0,0) (2,0); O: (0,1) (1,0). X to move. (1,1) and (2,2) both make two
        // threats at once, so X wins by force.
        val state = position(BoardSize.THREE, cell(0, 0), cell(0, 1), cell(2, 0), cell(1, 0))
        val best = SearchAi(Random(0)).bestMoves(state, DifficultyPolicy.hint(BoardSize.THREE))
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
        return { ai.chooseMove(it, DifficultyPolicy.hint(it.config.boardSize)) }
    }

    private fun computerAi(level: Level, seed: Int = 0): (GameState) -> Cell {
        val ai = SearchAi(Random(seed))
        return { ai.chooseMove(it, DifficultyPolicy.computer(it.config.boardSize, level)) }
    }

    private fun player(difficulty: Difficulty, seed: Int): (GameState) -> Cell {
        val ai = SearchAi(Random(seed))
        return { ai.chooseMove(it, difficulty) }
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

    private data class Record(val wins: Int, val draws: Int, val losses: Int) {
        val games get() = wins + draws + losses
        val lossRate get() = losses.toDouble() / games
    }

    /** Wins / draws / losses for [ai] over [games], alternating who starts. */
    private fun record(boardSize: BoardSize, games: Int, ai: (Int) -> (GameState) -> Cell, opponent: (Int) -> (GameState) -> Cell): Record {
        var wins = 0
        var draws = 0
        var losses = 0
        repeat(games) { g ->
            val aiPlays = if (g % 2 == 0) Player.X else Player.O
            val me = ai(g)
            val them = opponent(g + 1000)
            val end = playOut(
                start(config(GameMode.TWO_PLAYERS, boardSize, firstPlayer = Player.X)),
                if (aiPlays == Player.X) me else them,
                if (aiPlays == Player.O) me else them,
            )
            when (val r = end.result) {
                is GameResult.Win -> if (r.player == aiPlays) wins++ else losses++
                else -> draws++
            }
        }
        return Record(wins, draws, losses)
    }

    @Test
    fun `4x4 full strength never loses to random play`() {
        val result = record(BoardSize.FOUR, 20, ::hintAi, ::randomMover)
        assertEquals(0, result.losses)
    }

    @Test
    fun `8x8 Hard computer and hint beat random play`() {
        val computer = record(BoardSize.EIGHT, 10, { computerAi(Level.HARD, it) }, ::randomMover)
        val hint = record(BoardSize.EIGHT, 10, ::hintAi, ::randomMover)
        assertEquals("computer $computer", 10, computer.wins)
        assertEquals("hint $hint", 10, hint.wins)
    }

    @Test
    fun `3x3 Easy and Medium are beatable by perfect play, Medium less often`() {
        val easy = record(BoardSize.THREE, 1000, { computerAi(Level.EASY, it) }, ::hintAi)
        val medium = record(BoardSize.THREE, 1000, { computerAi(Level.MEDIUM, it) }, ::hintAi)
        assertTrue("Easy $easy", easy.lossRate in 0.12..0.25)
        assertTrue("Medium $medium", medium.lossRate in 0.04..0.12)
        assertTrue("Medium $medium should lose less than Easy $easy", medium.losses * 3 < easy.losses * 2)
    }

    @Test
    fun `4x4 levels - a good player beats Easy often, Medium sometimes, Hard almost never`() {
        // A strong but human-like opponent: full depth, with a random move now and then.
        val good = { seed: Int -> player(Difficulty(searchDepth = 6, mistakeChance = 0.10), seed) }
        val easy = record(BoardSize.FOUR, 300, { computerAi(Level.EASY, it) }, good)
        val medium = record(BoardSize.FOUR, 300, { computerAi(Level.MEDIUM, it) }, good)
        val hard = record(BoardSize.FOUR, 300, { computerAi(Level.HARD, it) }, good)
        assertTrue("Easy $easy", easy.lossRate in 0.18..0.35)
        assertTrue("Medium $medium", medium.lossRate in 0.04..0.14)
        assertTrue("Hard $hard", hard.lossRate <= 0.03)
        assertTrue("Medium $medium should lose much less than Easy $easy", medium.losses * 2 < easy.losses)
    }

    @Test
    fun `8x8 levels - each beats the one below`() {
        val mediumVsEasy = record(BoardSize.EIGHT, 30, { computerAi(Level.MEDIUM, it) }, { computerAi(Level.EASY, it) })
        val hardVsMedium = record(BoardSize.EIGHT, 30, { computerAi(Level.HARD, it) }, { computerAi(Level.MEDIUM, it) })
        assertTrue("Medium vs Easy $mediumVsEasy", mediumVsEasy.wins > 2 * mediumVsEasy.losses)
        assertTrue("Hard vs Medium $hardVsMedium", hardVsMedium.wins > 2 * hardVsMedium.losses)
    }

    @Test
    fun `strength check - computer vs full strength hint AI (printed)`() {
        for (size in BoardSize.entries) for (level in Level.entries) {
            val games = if (size == BoardSize.THREE) 200 else 20
            val started = System.nanoTime()
            val r = record(size, games, { computerAi(level, it) }, ::hintAi)
            val ms = (System.nanoTime() - started) / 1_000_000
            println("$size $level computer vs hint AI over $games games: W${r.wins} D${r.draws} L${r.losses} ($ms ms)")
        }
    }
}
