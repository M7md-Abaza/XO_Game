package com.m7mdabaza.xogame.presentation.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.TextChoices
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import kotlinx.coroutines.launch

@StringRes
fun Level.titleRes(): Int = when (this) {
    Level.EASY -> R.string.level_easy
    Level.MEDIUM -> R.string.level_medium
    Level.HARD -> R.string.level_hard
}

/** How the phone plays at this level on [boardSize] (Hard on 3×3 never loses). */
@StringRes
fun Level.descriptionRes(boardSize: BoardSize): Int = when (this) {
    Level.EASY -> R.string.level_easy_desc
    Level.MEDIUM -> R.string.level_medium_desc
    Level.HARD -> if (boardSize == BoardSize.THREE) R.string.level_hard_unbeatable_desc else R.string.level_hard_desc
}

/** The board as a short label, e.g. "3×3". */
@Composable
fun BoardSize.label(): String = stringResource(R.string.board_size, size)

/**
 * Starts a new game. Against the phone the player picks a board and a level
 * (the last choice is preselected) and taps Play; in two-player mode tapping
 * a board starts at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePickerSheet(
    mode: GameMode,
    initialBoardSize: BoardSize,
    initialLevel: Level,
    onDismiss: () -> Unit,
    onStart: (BoardSize, Level?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var boardSize by rememberSaveable { mutableStateOf(initialBoardSize) }
    var level by rememberSaveable { mutableStateOf(initialLevel) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        GamePickerContent(
            mode = mode,
            boardSize = boardSize,
            level = level,
            onBoardSizeSelected = { boardSize = it },
            onLevelSelected = { level = it },
            onStart = { board, chosenLevel ->
                // Let the sheet slide away before leaving the screen.
                scope.launch { sheetState.hide() }.invokeOnCompletion { onStart(board, chosenLevel) }
            },
        )
    }
}

/** The sheet's content, without the sheet (also used by the UI tests). */
@Composable
fun GamePickerContent(
    mode: GameMode,
    boardSize: BoardSize,
    level: Level,
    onBoardSizeSelected: (BoardSize) -> Unit,
    onLevelSelected: (Level) -> Unit,
    onStart: (BoardSize, Level?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vsComputer = mode == GameMode.VS_COMPUTER
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.picker_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(if (vsComputer) R.string.picker_vs_computer else R.string.picker_two_players),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (vsComputer) {
            SectionTitle(stringResource(R.string.picker_board))
            Column(Modifier.selectableGroup()) {
                BoardSize.entries.forEach { board ->
                    BoardCard(board, selected = board == boardSize, onClick = { onBoardSizeSelected(board) })
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.picker_level))
            TextChoices(Level.entries, level, onLevelSelected) { stringResource(it.titleRes()) }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(level.descriptionRes(boardSize)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onStart(boardSize, level) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.picker_play), style = MaterialTheme.typography.titleLarge)
            }
        } else {
            BoardSize.entries.forEach { board ->
                BoardCard(board, selected = null, onClick = { onStart(board, null) })
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .semantics { heading() },
    )
}

/**
 * One board: a mini grid, its size and the win rule. With [selected] it is a
 * radio choice; without (null) a plain button.
 */
@Composable
private fun BoardCard(board: BoardSize, selected: Boolean?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val content = @Composable { rowModifier: Modifier ->
        Row(
            modifier = rowModifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniBoard(
                size = board.size,
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = board.label(), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.board_win_rule, board.winLength),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected != null) {
                // The whole card is the radio button, so this one only shows the state.
                RadioButton(selected = selected, onClick = null)
            }
        }
    }
    if (selected == null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) { content(Modifier) }
    } else {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            content(Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick))
        }
    }
}

/** A tiny empty grid that shows how big the board is. */
@Composable
private fun MiniBoard(size: Int, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = (this.size.minDimension / 24f).coerceAtLeast(1.5f)
        val step = this.size.width / size
        for (i in 1 until size) {
            val p = step * i
            drawLine(lineColor, Offset(p, 0f), Offset(p, this.size.height), stroke, StrokeCap.Round)
            drawLine(lineColor, Offset(0f, p), Offset(this.size.width, p), stroke, StrokeCap.Round)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GamePickerPreview() {
    XoTheme {
        Surface {
            GamePickerContent(GameMode.VS_COMPUTER, BoardSize.FOUR, Level.MEDIUM, {}, {}, { _, _ -> })
        }
    }
}
