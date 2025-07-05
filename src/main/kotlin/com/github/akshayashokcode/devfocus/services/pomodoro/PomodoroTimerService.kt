package com.github.akshayashokcode.devfocus.services.pomodoro

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class PomodoroTimerService {
    companion object {
        private const val DEFAULT_MINUTES = 25
        private const val ONE_SECOND = 1000L
    }

    enum class TimerState { IDLE, RUNNING, PAUSED }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var remainingTimeMs: Long = TimeUnit.MINUTES.toMillis(DEFAULT_MINUTES.toLong())
    private var job: Job? = null

    private val _timeLeft = MutableStateFlow(formatTime(remainingTimeMs))
    val timeLeft: StateFlow<String> = _timeLeft

    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state

    fun start() {
        if (_state.value == TimerState.RUNNING) return

        _state.value = TimerState.RUNNING
        job = coroutineScope.launch {
            while (remainingTimeMs > 0 && isActive) {
                delay(ONE_SECOND)
                remainingTimeMs -= ONE_SECOND
                _timeLeft.value = formatTime(remainingTimeMs)
            }
            if (remainingTimeMs <= 0) {
                _state.value = TimerState.IDLE
            }
        }
    }

    fun pause() {
        if (_state.value == TimerState.RUNNING) {
            job?.cancel()
            _state.value = TimerState.PAUSED
        }
    }

    fun reset() {
        job?.cancel()
        remainingTimeMs = TimeUnit.MINUTES.toMillis(DEFAULT_MINUTES.toLong())
        _timeLeft.value = formatTime(remainingTimeMs)
        _state.value = TimerState.IDLE
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}