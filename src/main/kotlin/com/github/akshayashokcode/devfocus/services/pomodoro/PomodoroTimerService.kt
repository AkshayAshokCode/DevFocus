package com.github.akshayashokcode.devfocus.services.pomodoro

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.github.akshayashokcode.devfocus.services.settings.DevFocusSettingsState
import com.github.akshayashokcode.devfocus.util.SoundPlayer
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class PomodoroTimerService(private val project: Project) {

    companion object {
        private const val ONE_SECOND = 1000L
        private const val NOTIFICATION_GROUP_ID = "DevFocus Notifications"
        private const val SAVE_INTERVAL_TICKS = 30
    }

    enum class TimerState { IDLE, RUNNING, PAUSED }

    enum class TimerPhase {
        WORK, BREAK, LONG_BREAK;
        val isBreak: Boolean get() = this != WORK
    }

    // CoroutineExceptionHandler suppresses the debug-metadata version mismatch crash that
    // kotlinx-coroutines triggers during stack trace recovery when cancelling a coroutine
    // compiled with Kotlin 2.3.x against an older bundled coroutines runtime.
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
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

    private val _dailySessionCount = MutableStateFlow(0)
    val dailySessionCount: StateFlow<Int> = _dailySessionCount

    private val appSettings: DevFocusSettingsState
        get() = ApplicationManager.getApplication().getService(DevFocusSettingsState::class.java)

    init {
        restoreState()
    }

    // ---------------------------------------------------------------------------
    // Timer control
    // ---------------------------------------------------------------------------

    fun start() {
        if (_state.value == TimerState.RUNNING) return
        job?.cancel()
        _state.value = TimerState.RUNNING
        persistState()

        job = coroutineScope.launch {
            var ticks = 0
            while (remainingTimeMs > 0 && isActive) {
                delay(ONE_SECOND)
                remainingTimeMs -= ONE_SECOND
                _timeLeft.value = formatTime(remainingTimeMs)
                if (++ticks % SAVE_INTERVAL_TICKS == 0) persistState()
            }
            if (remainingTimeMs <= 0) {
                // Do NOT set IDLE here — onSessionComplete updates phase/session first,
                // then sets IDLE so the stateJob collector always sees a consistent snapshot.
                onSessionComplete()
            }
        }
    }

    fun pause() {
        if (_state.value != TimerState.RUNNING) return
        job?.cancel()
        _state.value = TimerState.PAUSED
        persistState()
    }

    fun reset() {
        job?.cancel()
        job = null
        internalPhase = TimerPhase.WORK
        _currentPhase.value = TimerPhase.WORK
        remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
        _timeLeft.value = formatTime(remainingTimeMs)
        _currentSession.value = 1
        _state.value = TimerState.IDLE
        persistState()
    }

    fun skipBreak() {
        if (!internalPhase.isBreak) return
        job?.cancel()
        job = null
        _state.value = TimerState.IDLE  // must reset before start() — its guard rejects RUNNING state
        internalPhase = TimerPhase.WORK
        _currentPhase.value = TimerPhase.WORK
        remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
        _timeLeft.value = formatTime(remainingTimeMs)
        persistState()
        if (appSettings.autoStartNextSession) start()
    }

    // ---------------------------------------------------------------------------
    // Session completion & transitions
    // ---------------------------------------------------------------------------

    private fun onSessionComplete() {
        val sessionNum = _currentSession.value
        val totalSessions = settings.sessionsPerRound

        if (internalPhase == TimerPhase.WORK) {
            incrementDailyCounter()

            if (sessionNum >= totalSessions) {
                // Full round done — start long break
                playWorkEndSound()
                val longMin = settings.longBreakMinutes
                notifyWithAction(
                    title = "🎉 Round Complete!",
                    body = "Outstanding! $totalSessions sessions done. Enjoy a $longMin-min long break.",
                    actionText = "Skip Long Break",
                    action = { skipBreak() }
                )
                _currentSession.value = 1
                internalPhase = TimerPhase.LONG_BREAK
                _currentPhase.value = TimerPhase.LONG_BREAK
                remainingTimeMs = TimeUnit.MINUTES.toMillis(longMin.toLong())
                _timeLeft.value = formatTime(remainingTimeMs)
                _state.value = TimerState.IDLE
                persistState()
                start() // long break always auto-starts

            } else {
                // Short break between sessions
                playWorkEndSound()
                _currentSession.value = sessionNum + 1
                notifyWithAction(
                    title = "✅ Session $sessionNum Complete!",
                    body = "Great work! Starting ${settings.breakMinutes}-min break ☕.",
                    actionText = "Skip Break",
                    action = { skipBreak() }
                )
                internalPhase = TimerPhase.BREAK
                _currentPhase.value = TimerPhase.BREAK
                remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.breakMinutes.toLong())
                _timeLeft.value = formatTime(remainingTimeMs)
                _state.value = TimerState.IDLE
                persistState()
                start() // short breaks always auto-start
            }

        } else {
            // BREAK or LONG_BREAK complete
            playBreakEndSound()
            val nextSession = _currentSession.value
            val autoStart = appSettings.autoStartNextSession
            internalPhase = TimerPhase.WORK
            _currentPhase.value = TimerPhase.WORK
            remainingTimeMs = TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
            _timeLeft.value = formatTime(remainingTimeMs)
            _state.value = TimerState.IDLE
            persistState()

            if (autoStart) {
                notify(
                    title = "☕ Break Over!",
                    body = "Starting session $nextSession of $totalSessions."
                )
                start()
            } else {
                notifyWithAction(
                    title = "☕ Break Over!",
                    body = "Session $nextSession of $totalSessions is ready when you are.",
                    actionText = "Start Session",
                    action = { start() }
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------------

    fun applySettings(newSettings: PomodoroSettings) {
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
        persistState()
    }

    fun applyMode(mode: PomodoroMode) = applySettings(mode.toSettings())

    fun getSettings(): PomodoroSettings = settings

    fun getProgress(): Float {
        val totalMs = when (internalPhase) {
            TimerPhase.WORK       -> TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
            TimerPhase.BREAK      -> TimeUnit.MINUTES.toMillis(settings.breakMinutes.toLong())
            TimerPhase.LONG_BREAK -> TimeUnit.MINUTES.toMillis(settings.longBreakMinutes.toLong())
        }
        return if (totalMs > 0) remainingTimeMs.toFloat() / totalMs.toFloat() else 0f
    }

    // ---------------------------------------------------------------------------
    // State persistence
    // ---------------------------------------------------------------------------

    private fun persistState() {
        appSettings.apply {
            savedRemainingTimeMs = remainingTimeMs
            savedCurrentSession = _currentSession.value
            savedPhase = internalPhase.name
            savedTimerWasRunning = _state.value == TimerState.RUNNING
            savedSessionMinutes = settings.sessionMinutes
            savedBreakMinutes = settings.breakMinutes
            savedSessionsPerRound = settings.sessionsPerRound
            savedLongBreakMinutes = settings.longBreakMinutes
            savedLongBreakAfter = settings.longBreakAfter
            savedMode = settings.mode.name
        }
    }

    private fun restoreState() {
        val saved = appSettings
        refreshDailyCount()

        // Restore settings
        val mode = runCatching { PomodoroMode.valueOf(saved.savedMode) }.getOrDefault(PomodoroMode.CLASSIC)
        settings = PomodoroSettings(
            mode = mode,
            sessionMinutes = saved.savedSessionMinutes,
            breakMinutes = saved.savedBreakMinutes,
            sessionsPerRound = saved.savedSessionsPerRound,
            longBreakMinutes = saved.savedLongBreakMinutes,
            longBreakAfter = saved.savedLongBreakAfter
        )
        _settings.value = settings

        // Restore phase & session
        internalPhase = runCatching { TimerPhase.valueOf(saved.savedPhase) }.getOrDefault(TimerPhase.WORK)
        _currentPhase.value = internalPhase
        _currentSession.value = saved.savedCurrentSession.coerceAtLeast(1)

        // Restore the exact saved position — wall-clock time while the IDE was closed is
        // intentionally ignored. The timer only counts down while actively running in the IDE.
        // A crash or close freezes the session at the exact second it stopped.
        val savedMs = saved.savedRemainingTimeMs
        remainingTimeMs = if (savedMs > 0) savedMs
                          else TimeUnit.MINUTES.toMillis(settings.sessionMinutes.toLong())
        _timeLeft.value = formatTime(remainingTimeMs)

        if (saved.savedTimerWasRunning && savedMs > 0) {
            _state.value = TimerState.PAUSED
        }
    }

    // ---------------------------------------------------------------------------
    // Daily counter
    // ---------------------------------------------------------------------------

    private fun refreshDailyCount() {
        val today = LocalDate.now().toString()
        if (appSettings.lastSessionDate != today) {
            appSettings.completedSessionsToday = 0
            appSettings.lastSessionDate = today
        }
        _dailySessionCount.value = appSettings.completedSessionsToday
    }

    private fun incrementDailyCounter() {
        val today = LocalDate.now().toString()
        if (appSettings.lastSessionDate != today) {
            appSettings.completedSessionsToday = 0
            appSettings.lastSessionDate = today
        }
        appSettings.completedSessionsToday++
        appSettings.lastSessionDate = today
        _dailySessionCount.value = appSettings.completedSessionsToday
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun notify(title: String, body: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(title, body, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun notifyWithAction(title: String, body: String, actionText: String, action: () -> Unit) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(title, body, NotificationType.INFORMATION)
            .addAction(object : NotificationAction(actionText) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    action()
                    notification.expire()
                }
            })
            .notify(project)
    }

    private fun playWorkEndSound()  = SoundPlayer.play("work.wav")
    private fun playBreakEndSound() = SoundPlayer.play("break.wav")
}
