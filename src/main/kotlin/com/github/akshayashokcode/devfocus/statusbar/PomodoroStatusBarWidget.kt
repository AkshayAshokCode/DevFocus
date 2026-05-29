package com.github.akshayashokcode.devfocus.statusbar

import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Consumer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.annotations.NonNls
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

class PomodoroStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    companion object {
        const val ID = "DevFocusStatusBarWidget"
    }

    private val timerService = project.getService(PomodoroTimerService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentText = ""
    private var statusBar: StatusBar? = null

    private var timeJob: Job? = null
    private var stateJob: Job? = null
    private var sessionJob: Job? = null
    private var phaseJob: Job? = null
    private var dailyJob: Job? = null

    init {
        observeTimer()
    }

    override fun ID(): @NonNls String = ID
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    override fun install(statusBar: StatusBar) { this.statusBar = statusBar }

    override fun dispose() {
        timeJob?.cancel()
        stateJob?.cancel()
        sessionJob?.cancel()
        phaseJob?.cancel()
        dailyJob?.cancel()
        scope.cancel()
    }

    override fun getText(): String = currentText
    override fun getAlignment(): Float = 0.5f

    override fun getTooltipText(): String {
        val state = timerService.state.value
        val phase = timerService.currentPhase.value
        val session = timerService.currentSession.value
        val settings = timerService.getSettings()
        val daily = timerService.dailySessionCount.value

        return when (state) {
            PomodoroTimerService.TimerState.IDLE -> {
                if (daily > 0) "DevFocus — $daily session${if (daily == 1) "" else "s"} completed today. Click to open."
                else "DevFocus — Click to open"
            }
            PomodoroTimerService.TimerState.RUNNING, PomodoroTimerService.TimerState.PAUSED -> {
                val stateLabel = if (state == PomodoroTimerService.TimerState.RUNNING) "Running" else "Paused"
                when (phase) {
                    PomodoroTimerService.TimerPhase.WORK ->
                        "Work Session $session/${settings.sessionsPerRound} — $stateLabel"
                    PomodoroTimerService.TimerPhase.BREAK ->
                        "Break — $stateLabel"
                    PomodoroTimerService.TimerPhase.LONG_BREAK ->
                        "Long Break — $stateLabel"
                }
            }
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { event ->
        if (event.button == MouseEvent.BUTTON1) {
            SwingUtilities.invokeLater {
                ToolWindowManager.getInstance(project).getToolWindow("DevFocus")?.show()
            }
        }
    }

    private fun observeTimer() {
        timeJob  = scope.launch { timerService.timeLeft.collectLatest { updateText() } }
        stateJob = scope.launch { timerService.state.collectLatest { updateText() } }
        sessionJob = scope.launch { timerService.currentSession.collectLatest { updateText() } }
        phaseJob = scope.launch { timerService.currentPhase.collectLatest { updateText() } }
        dailyJob = scope.launch { timerService.dailySessionCount.collectLatest { updateText() } }
    }

    private fun updateText() {
        val state = timerService.state.value
        val phase = timerService.currentPhase.value
        val time = timerService.timeLeft.value
        val session = timerService.currentSession.value
        val settings = timerService.getSettings()
        val daily = timerService.dailySessionCount.value

        val isActive = state == PomodoroTimerService.TimerState.RUNNING ||
                state == PomodoroTimerService.TimerState.PAUSED

        currentText = if (isActive) {
            val pauseMarker = if (state == PomodoroTimerService.TimerState.PAUSED) " ⏸" else ""
            when (phase) {
                PomodoroTimerService.TimerPhase.WORK ->
                    "⏱️ $time | Session $session/${settings.sessionsPerRound}$pauseMarker"
                PomodoroTimerService.TimerPhase.BREAK ->
                    "☕ $time | Break$pauseMarker"
                PomodoroTimerService.TimerPhase.LONG_BREAK ->
                    "🌟 $time | Long Break$pauseMarker"
            }
        } else {
            // Idle: show daily count as ambient progress, or just the plugin name
            if (daily > 0) "🍅 $daily today" else "🍅 DevFocus"
        }

        SwingUtilities.invokeLater { statusBar?.updateWidget(ID) }
    }
}
