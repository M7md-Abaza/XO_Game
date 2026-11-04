package com.m7mdabaza.xogame.presentation.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.OMark
import com.m7mdabaza.xogame.presentation.components.SegmentedChoices
import com.m7mdabaza.xogame.presentation.components.TextChoices
import com.m7mdabaza.xogame.presentation.components.XMark
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/**
 * Chooses the human's mark (against the computer) or the players' names (two
 * players), who starts and the match length, before a match.
 */
@Composable
fun MatchSetupDialog(
    mode: GameMode,
    initial: MatchSettings,
    onStart: (MatchSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    // Keyed on [initial]: the remembered defaults may arrive just after the dialog opens.
    var humanSymbol by rememberSaveable(initial) { mutableStateOf(initial.humanSymbol) }
    var starter by rememberSaveable(initial) { mutableStateOf(initial.starter) }
    var length by rememberSaveable(initial) { mutableStateOf(initial.length) }
    var xName by rememberSaveable(initial) { mutableStateOf(initial.xName) }
    var oName by rememberSaveable(initial) { mutableStateOf(initial.oName) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 480.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)

                when (mode) {
                    GameMode.VS_COMPUTER -> {
                        SectionLabel(stringResource(R.string.setup_play_as))
                        SegmentedChoices(
                            options = listOf(Player.X, Player.O),
                            selected = humanSymbol,
                            onSelect = { humanSymbol = it },
                        ) { player ->
                            // The marks are drawings, so give screen readers the letter.
                            val mark = Modifier.size(20.dp).semantics { contentDescription = player.name }
                            if (player == Player.X) XMark(mark) else OMark(mark)
                        }
                    }
                    GameMode.TWO_PLAYERS -> {
                        SectionLabel(stringResource(R.string.setup_names))
                        NameField(Player.X, xName) { xName = it }
                        Spacer(Modifier.height(8.dp))
                        NameField(Player.O, oName) { oName = it }
                    }
                }

                SectionLabel(stringResource(R.string.setup_who_starts))
                TextChoices(
                    options = Starter.entries,
                    selected = starter,
                    onSelect = { starter = it },
                ) { option ->
                    when (option) {
                        // X and O are game symbols, the same in every language.
                        Starter.PLAYER_ONE ->
                            if (mode == GameMode.VS_COMPUTER) stringResource(R.string.game_you) else Player.X.name
                        Starter.PLAYER_TWO ->
                            if (mode == GameMode.VS_COMPUTER) stringResource(R.string.game_phone) else Player.O.name
                        Starter.RANDOM -> stringResource(R.string.setup_starter_random)
                    }
                }

                SectionLabel(stringResource(R.string.setup_match))
                TextChoices(
                    options = MatchLength.entries,
                    selected = length,
                    onSelect = { length = it },
                ) { option ->
                    if (option == MatchLength.SINGLE) {
                        stringResource(R.string.setup_match_single)
                    } else {
                        stringResource(R.string.setup_match_best_of, option.rounds)
                    }
                }
                if (length != MatchLength.SINGLE) {
                    Text(
                        stringResource(R.string.setup_take_turns),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    ) { Text(stringResource(R.string.setup_cancel)) }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            onStart(
                                MatchSettings(
                                    humanSymbol = humanSymbol,
                                    starter = starter,
                                    length = length,
                                    xName = xName,
                                    oName = oName,
                                ),
                            )
                        },
                    ) { Text(stringResource(R.string.setup_start)) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun NameField(player: Player, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(GameViewModel.MAX_NAME_LENGTH)) },
        label = { Text(stringResource(R.string.setup_name_label, player.name)) },
        leadingIcon = {
            if (player == Player.X) XMark(Modifier.size(20.dp)) else OMark(Modifier.size(20.dp))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = if (player == Player.X) ImeAction.Next else ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun MatchSetupDialogPreview() {
    XoTheme {
        MatchSetupDialog(GameMode.TWO_PLAYERS, MatchSettings(length = MatchLength.BEST_OF_3), onStart = {}, onDismiss = {})
    }
}
