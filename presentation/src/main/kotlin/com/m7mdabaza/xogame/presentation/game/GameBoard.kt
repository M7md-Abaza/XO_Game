package com.m7mdabaza.xogame.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.m7mdabaza.xogame.domain.model.Board
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.drawO
import com.m7mdabaza.xogame.presentation.components.drawX
import com.m7mdabaza.xogame.presentation.theme.BrandBlack
import com.m7mdabaza.xogame.presentation.theme.BrandYellow
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/**
 * The square game board: grid lines, animated marks, the hint pulse and the
 * winning line. Always laid out left to right (rows and columns are game
 * coordinates, not reading order), also in Arabic.
 *
 * Every square is its own clickable node with a description, so screen
 * readers can play too.
 */
@Composable
fun GameBoard(
    board: Board,
    winningLine: List<Cell>?,
    winner: Player?,
    hint: Cell?,
    /** Whose move the hint is for; it is drawn as a faded, pulsing preview of their mark. */
    hintPlayer: Player,
    lastMove: Cell?,
    enabled: Boolean,
    onCellClick: (Cell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = board.size
    val gridColor = MaterialTheme.colorScheme.primary
    val winningCells = winningLine.orEmpty().toSet()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.aspectRatio(1f)) {
            // Inner grid lines only, like a hand-drawn board.
            Canvas(Modifier.fillMaxSize()) {
                val step = this.size.width / size
                val stroke = (step * 0.05f).coerceIn(2.dp.toPx(), 6.dp.toPx())
                for (i in 1 until size) {
                    val p = step * i
                    drawLine(gridColor, Offset(p, stroke), Offset(p, this.size.height - stroke), stroke, StrokeCap.Round)
                    drawLine(gridColor, Offset(stroke, p), Offset(this.size.width - stroke, p), stroke, StrokeCap.Round)
                }
            }
            Column(Modifier.fillMaxSize()) {
                repeat(size) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(size) { col ->
                            val cell = Cell(row, col)
                            BoardCell(
                                row = row,
                                col = col,
                                player = board[cell],
                                isWinning = cell in winningCells,
                                hintPlayer = if (cell == hint) hintPlayer else null,
                                animate = cell == lastMove,
                                enabled = enabled,
                                onClick = { onCellClick(cell) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
            if (winningLine != null && winner != null && winningLine.size >= 2) {
                WinLine(winningLine, winner, size)
            }
        }
    }
}

@Composable
private fun BoardCell(
    row: Int,
    col: Int,
    player: Player?,
    isWinning: Boolean,
    hintPlayer: Player?,
    animate: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = when {
        player != null -> player.name
        hintPlayer != null -> stringResource(R.string.game_cell_hint)
        else -> stringResource(R.string.game_cell_empty)
    }
    val description = stringResource(R.string.game_cell_description, row + 1, col + 1, content)
    val shape = RoundedCornerShape(percent = 22)

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(shape)
            .then(if (isWinning) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
            .clickable(enabled = enabled && player == null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (player != null) Mark(player, animate)
        if (hintPlayer != null && player == null) HintPreview(hintPlayer)
    }
}

/** A mark that draws itself in when it is the newest move. */
@Composable
private fun Mark(player: Player, animate: Boolean) {
    val progress = remember(player) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(player) {
        progress.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
    }
    Canvas(Modifier.fillMaxSize(0.62f)) {
        if (player == Player.X) drawX(BrandYellow, progress.value) else drawO(BrandBlack, progress.value)
    }
}

/** The suggested move: a pulsing tint behind a faded preview of [player]'s mark. */
@Composable
private fun HintPreview(player: Player) {
    val transition = rememberInfiniteTransition(label = "hint")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hintPulse",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f + 0.6f * pulse)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize(0.62f)) {
            val alpha = 0.25f + 0.35f * pulse
            if (player == Player.X) drawX(BrandYellow.copy(alpha = alpha)) else drawO(BrandBlack.copy(alpha = alpha))
        }
    }
}

/** Draws a line through the winning run, growing from its first to its last cell. */
@Composable
private fun WinLine(line: List<Cell>, winner: Player, boardSize: Int) {
    val progress = remember(line) { Animatable(0f) }
    LaunchedEffect(line) {
        progress.animateTo(1f, tween(durationMillis = 450, delayMillis = 150, easing = FastOutSlowInEasing))
    }
    // Contrast with the winner's marks: dark over yellow X, yellow over black O.
    val color = if (winner == Player.X) BrandBlack.copy(alpha = 0.8f) else BrandYellow
    Canvas(Modifier.fillMaxSize()) {
        val step = size.width / boardSize
        fun center(cell: Cell) = Offset((cell.col + 0.5f) * step, (cell.row + 0.5f) * step)
        val start = center(line.first())
        val end = center(line.last())
        val current = Offset(start.x + (end.x - start.x) * progress.value, start.y + (end.y - start.y) * progress.value)
        drawLine(color, start, current, strokeWidth = (step * 0.12f).coerceAtLeast(4.dp.toPx()), cap = StrokeCap.Round)
    }
}

@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun GameBoardPreview() {
    var board = Board.empty(3)
    board = board.with(Cell(0, 0), Player.X).with(Cell(1, 1), Player.X).with(Cell(2, 2), Player.X)
        .with(Cell(0, 2), Player.O).with(Cell(2, 0), Player.O)
    XoTheme {
        GameBoard(
            board = board,
            winningLine = listOf(Cell(0, 0), Cell(1, 1), Cell(2, 2)),
            winner = Player.X,
            hint = null,
            hintPlayer = Player.O,
            lastMove = null,
            enabled = false,
            onCellClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
