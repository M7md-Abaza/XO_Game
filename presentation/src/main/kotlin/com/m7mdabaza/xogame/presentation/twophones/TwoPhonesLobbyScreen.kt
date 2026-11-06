package com.m7mdabaza.xogame.presentation.twophones

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.Starter
import com.m7mdabaza.xogame.domain.multiplayer.JoinRequest
import com.m7mdabaza.xogame.domain.multiplayer.LobbyError
import com.m7mdabaza.xogame.domain.multiplayer.LobbyState
import com.m7mdabaza.xogame.domain.multiplayer.NearbyHost
import com.m7mdabaza.xogame.domain.multiplayer.TwoPhoneSetup
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar
import com.m7mdabaza.xogame.presentation.components.TextChoices
import kotlinx.coroutines.launch

/** What stands between the player and the lobby: a permission, Bluetooth or Location. */
enum class LobbyGate { NEEDS_PERMISSION, PERMISSION_BLOCKED, BLUETOOTH_OFF, LOCATION_OFF, READY }

/** Stateful entry point used by the navigation graph. */
@Composable
fun TwoPhonesLobbyRoute(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    viewModel: TwoPhonesLobbyViewModel = hiltViewModel(),
) {
    val lobbyState by viewModel.lobbyState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A refusal Android won't ask about again (or "don't ask again") sends the player to the app's settings.
    var blocked by rememberSaveable { mutableStateOf(false) }
    fun currentGate(): LobbyGate = when {
        !hasNearbyPermissions(context) -> if (blocked) LobbyGate.PERMISSION_BLOCKED else LobbyGate.NEEDS_PERMISSION
        !isBluetoothOn(context) -> LobbyGate.BLUETOOTH_OFF
        isLocationNeededButOff(context) -> LobbyGate.LOCATION_OFF
        else -> LobbyGate.READY
    }
    var gate by remember { mutableStateOf(currentGate()) }
    // The player may change any of these outside the app.
    LifecycleResumeEffect(Unit) {
        gate = currentGate()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        blocked = activity != null && results.any { (permission, granted) ->
            !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        gate = currentGate()
    }
    val bluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { gate = currentGate() }

    LaunchedEffect(viewModel) {
        viewModel.errors.collect { error ->
            if (error == LobbyError.RADIO_OFF || error == LobbyError.LOCATION_OFF) gate = currentGate()
            scope.launch { snackbar.showSnackbar(resources.getString(error.textRes())) }
        }
    }
    LaunchedEffect(lobbyState) {
        if (lobbyState is LobbyState.Connected) onConnected()
    }

    TwoPhonesLobbyScreen(
        lobbyState = lobbyState,
        form = form,
        gate = gate,
        snackbar = snackbar,
        actions = LobbyActions(
            onBack = onBack,
            onNameChange = viewModel::onNameChange,
            onHostChosen = viewModel::onHostChosen,
            onHostFormClosed = viewModel::onHostFormClosed,
            onBoardSizeChange = viewModel::onBoardSizeChange,
            onMatchLengthChange = viewModel::onMatchLengthChange,
            onStarterChange = viewModel::onStarterChange,
            onStartHosting = viewModel::onStartHosting,
            onSearch = viewModel::onSearch,
            onJoin = viewModel::onJoin,
            onAnswer = viewModel::onAnswer,
            onStop = viewModel::onStop,
            onGateAction = {
                when (gate) {
                    LobbyGate.NEEDS_PERMISSION -> permissionLauncher.launch(nearbyPermissions().toTypedArray())
                    LobbyGate.PERMISSION_BLOCKED -> openAppSettings(context)
                    // The permissions above include BLUETOOTH_CONNECT, which this needs on Android 12+.
                    LobbyGate.BLUETOOTH_OFF -> bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    LobbyGate.LOCATION_OFF -> openLocationSettings(context)
                    LobbyGate.READY -> Unit
                }
            },
        ),
    )
}

@StringRes
private fun LobbyError.textRes(): Int = when (this) {
    LobbyError.NOT_AVAILABLE -> R.string.twophones_error_not_available
    LobbyError.RADIO_OFF -> R.string.twophones_error_radio_off
    LobbyError.LOCATION_OFF -> R.string.twophones_location_off
    LobbyError.DECLINED -> R.string.twophones_error_declined
    LobbyError.HOST_GONE -> R.string.twophones_error_host_gone
    LobbyError.FAILED -> R.string.twophones_error_failed
}

/** Everything the lobby screen can ask for. */
data class LobbyActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onHostChosen: () -> Unit = {},
    val onHostFormClosed: () -> Unit = {},
    val onBoardSizeChange: (BoardSize) -> Unit = {},
    val onMatchLengthChange: (MatchLength) -> Unit = {},
    val onStarterChange: (Starter) -> Unit = {},
    val onStartHosting: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onJoin: (String) -> Unit = {},
    val onAnswer: (Boolean) -> Unit = {},
    val onStop: () -> Unit = {},
    val onGateAction: () -> Unit = {},
)

@Composable
fun TwoPhonesLobbyScreen(
    lobbyState: LobbyState,
    form: LobbyForm,
    gate: LobbyGate,
    actions: LobbyActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Back steps out of hosting, searching or the host form before leaving the screen.
    val busy = lobbyState is LobbyState.Hosting || lobbyState is LobbyState.Searching || lobbyState is LobbyState.Joining
    BackHandler {
        when {
            busy -> actions.onStop()
            form.choosingHostSetup -> actions.onHostFormClosed()
            else -> actions.onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(stringResource(R.string.twophones_title), onBack = actions.onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                if (gate != LobbyGate.READY) {
                    GateCard(gate, actions.onGateAction)
                } else {
                    when (lobbyState) {
                        LobbyState.Idle -> if (form.choosingHostSetup) HostForm(form, actions) else Choices(form, actions)
                        is LobbyState.Hosting -> Waiting(lobbyState.setup, actions.onStop)
                        is LobbyState.Searching -> Searching(lobbyState.hosts, actions)
                        is LobbyState.Joining -> Joining(lobbyState.host, lobbyState.code, actions.onStop)
                        is LobbyState.Connected -> Busy(stringResource(R.string.twophones_connecting))
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    val request = (lobbyState as? LobbyState.Hosting)?.request
    if (request != null) JoinRequestDialog(request, actions.onAnswer)
}

@Composable
private fun Choices(form: LobbyForm, actions: LobbyActions) {
    Text(
        stringResource(R.string.twophones_intro),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = form.name,
        onValueChange = actions.onNameChange,
        label = { Text(stringResource(R.string.twophones_your_name)) },
        supportingText = { Text(stringResource(R.string.twophones_name_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    OptionCard(R.drawable.ic_bluetooth, R.string.twophones_host, R.string.twophones_host_summary, actions.onHostChosen)
    Spacer(Modifier.height(12.dp))
    OptionCard(R.drawable.ic_group, R.string.twophones_join, R.string.twophones_join_summary, actions.onSearch)
}

@Composable
private fun OptionCard(@DrawableRes icon: Int, @StringRes title: Int, @StringRes summary: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.heightIn(min = 72.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(summary), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ColumnScope.HostForm(form: LobbyForm, actions: LobbyActions) {
    Text(
        stringResource(R.string.twophones_host),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    Label(stringResource(R.string.picker_board))
    TextChoices(BoardSize.entries, form.boardSize, actions.onBoardSizeChange) { stringResource(R.string.board_size, it.size) }
    Label(stringResource(R.string.setup_match))
    TextChoices(MatchLength.entries, form.matchLength, actions.onMatchLengthChange) { matchLabel(it) }
    Label(stringResource(R.string.setup_who_starts))
    TextChoices(Starter.entries, form.starter, actions.onStarterChange) { starter ->
        stringResource(
            when (starter) {
                Starter.PLAYER_ONE -> R.string.twophones_starter_me
                Starter.PLAYER_TWO -> R.string.twophones_starter_friend
                Starter.RANDOM -> R.string.setup_starter_random
            },
        )
    }
    Text(
        stringResource(R.string.setup_take_turns),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = actions.onStartHosting, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(stringResource(R.string.twophones_start_hosting), style = MaterialTheme.typography.titleMedium)
    }
    TextButton(
        onClick = actions.onHostFormClosed,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) { Text(stringResource(R.string.setup_cancel)) }
}

@Composable
private fun ColumnScope.Waiting(setup: TwoPhoneSetup, onStop: () -> Unit) {
    Busy(stringResource(R.string.twophones_waiting))
    Text(
        gameSummary(setup.boardSize, setup.matchLength),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Hint(stringResource(R.string.twophones_waiting_hint))
    StopButton(onStop)
}

@Composable
private fun ColumnScope.Searching(hosts: List<NearbyHost>, actions: LobbyActions) {
    Busy(stringResource(R.string.twophones_searching))
    Hint(stringResource(R.string.twophones_searching_hint))
    Spacer(Modifier.height(16.dp))
    hosts.forEach { host ->
        Card(
            onClick = { actions.onJoin(host.id) },
            enabled = host.compatible,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Column(Modifier.heightIn(min = 64.dp).padding(16.dp), verticalArrangement = Arrangement.Center) {
                Text(host.name.ifBlank { stringResource(R.string.twophones_unnamed) }, style = MaterialTheme.typography.titleLarge)
                val boardSize = host.boardSize
                val matchLength = host.matchLength
                Text(
                    if (boardSize != null && matchLength != null) gameSummary(boardSize, matchLength) else stringResource(R.string.twophones_host_other_version),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    StopButton(actions.onStop)
}

@Composable
private fun ColumnScope.Joining(host: NearbyHost, code: String?, onStop: () -> Unit) {
    Busy(stringResource(R.string.twophones_joining, host.name.ifBlank { stringResource(R.string.twophones_unnamed) }))
    if (code != null) {
        Text(
            stringResource(R.string.twophones_code_label),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Code(code, Modifier.align(Alignment.CenterHorizontally))
    }
    StopButton(onStop)
}

@Composable
private fun JoinRequestDialog(request: JoinRequest, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        // Back declines; a tap outside does nothing, so nobody declines by accident.
        onDismissRequest = { if (!request.accepted) onAnswer(false) },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.twophones_request_title, request.name.ifBlank { stringResource(R.string.twophones_unnamed) })) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.twophones_request_code))
                Code(request.code, Modifier.align(Alignment.CenterHorizontally))
                if (request.accepted) {
                    Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.twophones_connecting))
                    }
                }
            }
        },
        confirmButton = {
            if (!request.accepted) Button(onClick = { onAnswer(true) }) { Text(stringResource(R.string.twophones_accept)) }
        },
        dismissButton = {
            if (!request.accepted) {
                TextButton(
                    onClick = { onAnswer(false) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) { Text(stringResource(R.string.twophones_decline)) }
            }
        },
    )
}

@Composable
private fun GateCard(gate: LobbyGate, onAction: () -> Unit) {
    val (text, button) = when (gate) {
        LobbyGate.NEEDS_PERMISSION ->
            (if (nearbyPermissionIsLocation()) R.string.twophones_permission_text_location else R.string.twophones_permission_text) to
                R.string.twophones_permission_allow
        LobbyGate.PERMISSION_BLOCKED -> R.string.twophones_permission_denied to R.string.twophones_open_settings
        LobbyGate.BLUETOOTH_OFF -> R.string.twophones_bluetooth_off to R.string.twophones_turn_on
        LobbyGate.LOCATION_OFF -> R.string.twophones_location_off to R.string.twophones_turn_on
        LobbyGate.READY -> return
    }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.ic_bluetooth), contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.twophones_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(button), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A spinner with what the lobby is doing, announced to screen readers. */
@Composable
private fun Busy(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

@Composable
private fun ColumnScope.StopButton(onStop: () -> Unit) {
    OutlinedButton(onClick = onStop, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp).heightIn(min = 48.dp)) {
        Text(stringResource(R.string.twophones_stop))
    }
}

/** The connection code both phones show, big enough to compare at a glance. */
@Composable
private fun Code(code: String, modifier: Modifier = Modifier) {
    Text(code, style = MaterialTheme.typography.displayMedium, modifier = modifier.padding(vertical = 8.dp))
}

@Composable
internal fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

@Composable
internal fun matchLabel(length: MatchLength): String =
    if (length == MatchLength.SINGLE) stringResource(R.string.setup_match_single) else stringResource(R.string.setup_match_best_of, length.rounds)

@Composable
internal fun gameSummary(boardSize: BoardSize, matchLength: MatchLength): String =
    stringResource(R.string.twophones_game_summary, stringResource(R.string.board_size, boardSize.size), matchLabel(matchLength))
