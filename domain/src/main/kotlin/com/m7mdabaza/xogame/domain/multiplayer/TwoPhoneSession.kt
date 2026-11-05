package com.m7mdabaza.xogame.domain.multiplayer

import com.m7mdabaza.xogame.domain.model.Cell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Runs [TwoPhoneRules] over an open [PeerChannel] for as long as [scope]
 * lives (the two-phone game screen). The friend's messages and this
 * player's actions are applied one at a time, in the order they come; the
 * resulting messages are sent in order; the channel closes when the game ends.
 *
 * @param setup the match the host chose; null on the guest.
 */
class TwoPhoneSession(
    private val channel: PeerChannel,
    private val rules: TwoPhoneRules,
    scope: CoroutineScope,
    role: PeerRole,
    myName: String,
    setup: TwoPhoneSetup?,
) {
    private val first = rules.start(role, myName, setup)
    private val _state = MutableStateFlow(first.state)

    /** This phone's view of the match. */
    val state: StateFlow<TwoPhoneState> = _state.asStateFlow()

    private val _events = Channel<TwoPhoneEvent>(Channel.UNLIMITED)

    /** Sounds, stats and messages to show, each once. Collected by one collector. */
    val events: Flow<TwoPhoneEvent> = _events.receiveAsFlow()

    /** Everything that changes the state, in arrival order, applied by one coroutine. */
    private val inputs = Channel<(TwoPhoneState) -> TwoPhoneStep>(Channel.UNLIMITED)

    private var closed = false

    init {
        scope.launch {
            apply(first)
            for (input in inputs) apply(input(_state.value))
        }
        scope.launch {
            channel.events.collect { event ->
                inputs.send { state ->
                    when (event) {
                        is PeerEvent.Received -> rules.receive(state, event.message)
                        PeerEvent.Disconnected -> rules.connectionLost(state)
                    }
                }
            }
        }
    }

    fun play(cell: Cell) = input { rules.play(it, cell) }

    /** "Next round" / "Rematch". */
    fun continueAfterRound() = input { rules.continueAfterRound(it) }

    fun request(kind: RequestKind) = input { rules.request(it, kind) }

    fun answer(accept: Boolean) = input { rules.answer(it, accept) }

    /** Says goodbye to the friend and closes the connection. */
    fun leave() = input { rules.leave(it) }

    /** Closes the connection without a goodbye (the screen is gone); the friend sees the connection lost. */
    fun close() = channel.close()

    private fun input(transition: (TwoPhoneState) -> TwoPhoneStep) {
        inputs.trySend(transition)
    }

    private suspend fun apply(step: TwoPhoneStep) {
        _state.value = step.state
        step.events.forEach { _events.send(it) }
        step.send.forEach { channel.send(it) }
        if (step.state.isEnded && !closed) {
            closed = true
            channel.close()
        }
    }
}
