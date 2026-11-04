package com.m7mdabaza.xogame.domain.testing

import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Board
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.usecase.MoveResult
import com.m7mdabaza.xogame.domain.usecase.PlayMoveUseCase
import com.m7mdabaza.xogame.domain.usecase.StartGameUseCase
import org.junit.Assert.fail

/** Builds a board from rows of `X`, `O` and `.` separated by `/`, e.g. `X.O/.X./..O`. */
fun board(rows: String): Board {
    val lines = rows.split("/")
    var board = Board.empty(lines.size)
    lines.forEachIndexed { r, line ->
        require(line.length == lines.size) { "Row $r of \"$rows\" is not ${lines.size} wide" }
        line.forEachIndexed { c, ch ->
            when (ch) {
                'X' -> board = board.with(Cell(r, c), Player.X)
                'O' -> board = board.with(Cell(r, c), Player.O)
                '.' -> Unit
                else -> error("Unexpected '$ch' in \"$rows\"")
            }
        }
    }
    return board
}

fun cell(row: Int, col: Int) = Cell(row, col)

val play = PlayMoveUseCase(WinChecker())
val start = StartGameUseCase()

fun config(
    mode: GameMode = GameMode.TWO_PLAYERS,
    level: Level = Level.EASY,
    humanSymbol: Player = Player.X,
    firstPlayer: Player = Player.X,
) = GameConfig(mode, level, humanSymbol, firstPlayer)

/** Plays [cells] in order (alternating turns), failing the test if any move is rejected. */
fun GameState.playing(vararg cells: Cell): GameState = cells.fold(this) { state, c ->
    when (val result = play(state, c)) {
        is MoveResult.Played -> result.state
        else -> {
            fail("Move $c was rejected with $result on ${state.board}")
            state
        }
    }
}

fun newGame(config: GameConfig = config(), vararg cells: Cell): GameState = start(config).playing(*cells)
