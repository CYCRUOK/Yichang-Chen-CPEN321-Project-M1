package com.example.cpen321application.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.timer.IssPosition
import com.example.cpen321application.timer.IssTracker
import com.example.cpen321application.timer.WaitFacts
import com.example.cpen321application.timer.WhileYouWaited
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TimerUiState {
    data object Idle : TimerUiState
    data class Running(val remainingMs: Long, val totalMs: Long) : TimerUiState

    /** The timer went off. [facts] is null while the ISS lookup is still in flight. */
    data class Fired(val elapsedSeconds: Long, val facts: WaitFacts?) : TimerUiState
}

/**
 * Countdown that survives rotation and does not drift: the remaining time is
 * always derived from a fixed deadline, not from counting ticks, so it stays
 * correct even if the process was paused in the background for a while.
 */
class TimerViewModel(
    private val issTracker: IssTracker,
    private val clock: () -> Long = System::currentTimeMillis,
    private val tickMs: Long = 250L,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimerUiState>(TimerUiState.Idle)
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    /** Emits once each time the timer goes off; the UI vibrates / notifies on it. */
    private val _firedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val firedEvents: SharedFlow<Unit> = _firedEvents.asSharedFlow()

    private var countdown: Job? = null

    fun start(minutes: Int, seconds: Int) {
        val totalMs = (minutes.coerceAtLeast(0) * 60L + seconds.coerceAtLeast(0)) * 1000L
        if (totalMs <= 0L || _uiState.value is TimerUiState.Running) return

        countdown?.cancel()
        countdown = viewModelScope.launch {
            val startedAt = clock()
            val deadline = startedAt + totalMs
            // Take the "before" ISS fix in parallel with the countdown.
            val issStart: Deferred<IssPosition?> = async { issTracker.currentPosition() }

            while (true) {
                val remaining = deadline - clock()
                if (remaining <= 0L) break
                _uiState.value = TimerUiState.Running(remainingMs = remaining, totalMs = totalMs)
                delay(minOf(tickMs, remaining))
            }

            val elapsedSeconds = (clock() - startedAt + 500L) / 1000L
            _uiState.value = TimerUiState.Fired(elapsedSeconds, facts = null)
            _firedEvents.tryEmit(Unit)

            val issEnd = issTracker.currentPosition()
            _uiState.value = TimerUiState.Fired(
                elapsedSeconds,
                facts = WhileYouWaited.compute(elapsedSeconds, issStart.await(), issEnd),
            )
        }
    }

    fun cancel() {
        countdown?.cancel()
        countdown = null
        _uiState.value = TimerUiState.Idle
    }

    /** Back to the input form after the surprise has been shown. */
    fun reset() = cancel()
}
