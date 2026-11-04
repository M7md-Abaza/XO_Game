package com.m7mdabaza.xogame.domain.model

import com.m7mdabaza.xogame.domain.testing.cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchStateTest {

    private val xWin = GameResult.Win(Player.X, listOf(cell(0, 0), cell(0, 1), cell(0, 2)))
    private val oWin = GameResult.Win(Player.O, listOf(cell(1, 0), cell(1, 1), cell(1, 2)))

    private fun MatchState.after(vararg results: GameResult) = results.fold(this) { m, r -> m.record(r) }

    @Test
    fun `records wins and draws per player`() {
        val m = MatchState(MatchLength.BEST_OF_5).after(xWin, GameResult.Draw, oWin, xWin)
        assertEquals(2, m.wins(Player.X))
        assertEquals(1, m.wins(Player.O))
        assertEquals(1, m.draws)
        assertEquals(4, m.roundsPlayed)
        assertEquals(5, m.currentRound)
    }

    @Test
    fun `single never ends and keeps a running score`() {
        val m = MatchState(MatchLength.SINGLE).after(xWin, xWin, oWin, GameResult.Draw, xWin)
        assertFalse(m.isOver)
        assertNull(m.winner)
        assertEquals(3, m.xWins)
    }

    @Test
    fun `best of 3 ends after two wins`() {
        val m = MatchState(MatchLength.BEST_OF_3).after(oWin)
        assertFalse(m.isOver)
        val done = m.record(oWin)
        assertTrue(done.isOver)
        assertEquals(Player.O, done.winner)
    }

    @Test
    fun `best of 3 goes to the third round at one all`() {
        val m = MatchState(MatchLength.BEST_OF_3).after(xWin, oWin)
        assertFalse(m.isOver)
        assertEquals(Player.X, m.record(xWin).winner)
    }

    @Test
    fun `draws count as rounds, so a best of 3 always ends`() {
        val drawn = MatchState(MatchLength.BEST_OF_3).after(GameResult.Draw, GameResult.Draw, GameResult.Draw)
        assertTrue(drawn.isOver)
        assertNull(drawn.winner)

        // 1-0 with one round left after a draw: the leader can still be caught.
        val close = MatchState(MatchLength.BEST_OF_3).after(xWin, GameResult.Draw)
        assertFalse(close.isOver)
        assertEquals(Player.X, close.record(GameResult.Draw).winner)
        assertNull(close.record(oWin).winner) // 1-1 after three rounds: drawn
        assertTrue(close.record(oWin).isOver)
    }

    @Test
    fun `best of 5 ends early when the leader cannot be caught`() {
        val m = MatchState(MatchLength.BEST_OF_5).after(xWin, xWin, xWin)
        assertTrue(m.isOver)
        assertEquals(Player.X, m.winner)

        val notYet = MatchState(MatchLength.BEST_OF_5).after(xWin, xWin, oWin)
        assertFalse(notYet.isOver)
    }

    @Test
    fun `cannot record after the match ended or an unfinished round`() {
        val over = MatchState(MatchLength.BEST_OF_3).after(xWin, xWin)
        assertThrows(IllegalStateException::class.java) { over.record(oWin) }
        assertThrows(IllegalArgumentException::class.java) {
            MatchState(MatchLength.BEST_OF_3).record(GameResult.InProgress)
        }
    }
}
