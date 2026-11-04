package com.m7mdabaza.xogame.presentation.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.R
import kotlinx.coroutines.launch

@StringRes
fun Level.titleRes(): Int = when (this) {
    Level.EASY -> R.string.level_easy
    Level.MEDIUM -> R.string.level_medium
    Level.HARD -> R.string.level_hard
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelPickerSheet(
    mode: GameMode,
    onDismiss: () -> Unit,
    onLevelSelected: (Level) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.level_picker_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(
                    when (mode) {
                        GameMode.VS_COMPUTER -> R.string.level_picker_vs_computer
                        GameMode.TWO_PLAYERS -> R.string.level_picker_two_players
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Level.entries.forEach { level ->
                LevelCard(
                    level = level,
                    onClick = {
                        // Let the sheet slide away before leaving the screen.
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onLevelSelected(level)
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LevelCard(level: Level, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniBoard(
                size = level.boardSize,
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(level.titleRes()),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.level_rules, level.boardSize, level.winLength),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
