package com.github.akshayashokcode.devfocus.services.pomodoro

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
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
class PomodoroTimerService(private val project: Project) {
    companion object {
        private const val ONE_SECOND = 1000L
        private const val NOTIFICATION_GROUP_ID = "DevFocus Notifications"
    }

    enum class TimerState { IDLE, RUNNING, PAUSED }
    enum class TimerPhase { WORK, BREAK }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private var settings = PomodoroMode.CLASSIC.toSettings()
    private var internalPhase = TimerPhase.WORK
    private var remainingTimeMs: Long = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())

    private val _timeLeft = MutableStateFlow(formatTime(remainingTimeMs))
    val timeLeft: StateFlow<String> = _timeLeft

    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state

    private val _currentSession = MutableStateFlow(1)
    val currentSession: StateFlow<Int> = _currentSession

    private val _currentPhase = MutableStateFlow(TimerPhase.WORK)
    val currentPhase: StateFlow<TimerPhase> = _currentPhase

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
                onSessionComplete()
            }
        }
    }

    private fun onSessionComplete() {
        val currentSessionNum = _currentSession.value
        val totalSessions = settings.sessionsPerRound

        if (internalPhase == TimerPhase.WORK) {
            // Work session complete
            if (currentSessionNum >= totalSessions) {
                // Last session complete - all done!
                NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(
                        "\uD83C\uDF89 All Sessions Complete!",
                        "You've completed all $totalSessions sessions. Take a well-deserved break!",
                        NotificationType.INFORMATION
                    )
                    .notify(project)

                // Reset to initial state
                internalPhase = TimerPhase.WORK
                _currentPhase.value = TimerPhase.WORK
                _currentSession.value = 1
                remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
                _timeLeft.value = formatTime(remainingTimeMs)
            } else {
                // Work session complete - start break
                _currentSession.value = currentSessionNum + 1

                NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(
                        "\uD83C\uDF45 Session $currentSessionNum Complete!",
                        "Great work! Starting ${settings.breakMinutes}-minute break ☕.",
                        NotificationType.INFORMATION
                    )
                    .notify(project)

                // Start break timer
                internalPhase = TimerPhase.BREAK
                _currentPhase.value = TimerPhase.BREAK
                remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.breakMinutes.toLong())
                _timeLeft.value = formatTime(remainingTimeMs)
                start()
            }

        } else {
            // Break complete
            // More sessions remaining - start next session
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(
                    "☕ Break Complete!",
                    "Starting session ${currentSessionNum + 1} of $totalSessions.",
                    NotificationType.INFORMATION
                )
                .notify(project)

            // Start next work session
            internalPhase = TimerPhase.WORK
            _currentPhase.value = TimerPhase.WORK
            _currentSession.value = currentSessionNum + 1
            remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
            _timeLeft.value = formatTime(remainingTimeMs)
            start()

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
        internalPhase = TimerPhase.WORK
        _currentPhase.value = TimerPhase.WORK
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
        internalPhase = TimerPhase.WORK
        _currentPhase.value = TimerPhase.WORK
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
        val totalMs = if (internalPhase == TimerPhase.WORK) {
            TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
        } else {
            TimeUnit.MINUTES.toMillis(settings.breakMinutes.toLong())
        }
        return if (totalMs > 0) remainingTimeMs.toFloat() / totalMs.toFloat() else 0f
    }
}