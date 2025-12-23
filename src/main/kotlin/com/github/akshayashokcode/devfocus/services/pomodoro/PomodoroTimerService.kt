package com.github.akshayashokcode.devfocus.services.pomodoro

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
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
        private const val ONE_SECOND = 1000L
    }

    enum class TimerState { IDLE, RUNNING, PAUSED }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private var settings = PomodoroMode.CLASSIC.toSettings()
    private var remainingTimeMs: Long = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())

    private val _timeLeft = MutableStateFlow(formatTime(remainingTimeMs))
    val timeLeft: StateFlow<String> = _timeLeft

    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state

    private val _currentSession = MutableStateFlow(1)
    val currentSession: StateFlow<Int> = _currentSession

    private val _settings = MutableStateFlow(settings)
    val settingsFlow: StateFlow<PomodoroSettings> = _settings

    fun start() {
        if (_state.value == TimerState.RUNNING) return

        // Cancel any existing job to ensure only one timer is running
        job?.cancel()

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
        // Cancel any running job
        job?.cancel()
        job = null

        // Reset to initial state
        remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
        _timeLeft.value = formatTime(remainingTimeMs)
        _currentSession.value = 1
        _state.value = TimerState.IDLE
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun applySettings(newSettings: PomodoroSettings) {
        // Cancel any running job when settings change
        job?.cancel()
        job = null

        settings = newSettings
        _settings.value = newSettings
        remainingTimeMs = TimeUnit.MINUTES.toMillis(newSettings.sessionMinutes.toLong())
        _timeLeft.value = formatTime(remainingTimeMs)
        _currentSession.value = 1
        _state.value = TimerState.IDLE
    }

    fun applyMode(mode: PomodoroMode) {
        applySettings(mode.toSettings())
    }

    fun getSettings(): PomodoroSettings = settings

    fun getProgress(): Float {
        val totalMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
        return if (totalMs > 0) remainingTimeMs.toFloat() / totalMs.toFloat() else 0f
    }
}