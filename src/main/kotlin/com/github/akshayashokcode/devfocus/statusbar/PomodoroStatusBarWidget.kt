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


    init {
        observeTimer()
    }
    override fun ID(): @NonNls String = ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        timeJob?.cancel()
        stateJob?.cancel()
        sessionJob?.cancel()
        phaseJob?.cancel()
        scope.cancel()
    }
    override fun getText(): String = currentText

    override fun getAlignment(): Float = 0.5f

    override fun getTooltipText(): String {
        val state = timerService.state.value
        val phase = timerService.currentPhase.value
        val session = timerService.currentSession.value
        val settings = timerService.getSettings()

        return when (state) {
            PomodoroTimerService.TimerState.IDLE -> "DevFocus - Click to open"
            PomodoroTimerService.TimerState.RUNNING -> {
                if (phase == PomodoroTimerService.TimerPhase.WORK) {
                    "Work Session $session/${settings.sessionsPerRound} - Running"
                } else {
                    "Break Time - Running"
                }
            }
            PomodoroTimerService.TimerState.PAUSED -> {
                if (phase == PomodoroTimerService.TimerPhase.WORK) {
                    "Work Session $session/${settings.sessionsPerRound} - Paused"
                } else {
                    "Break Time - Paused"
                }
            }
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { event ->
            if (event.button == MouseEvent.BUTTON1) {
                // Left click - open focus tool window
                SwingUtilities.invokeLater {
                    val toolWindowManager = ToolWindowManager.getInstance(project)
                    val toolWindow = toolWindowManager.getToolWindow("DevFocus")
                    toolWindow?.show()
                }
            }
        }
    }

    private fun observeTimer() {
        timeJob = scope.launch {
            timerService.timeLeft.collectLatest {
                updateText()
            }
        }

        stateJob = scope.launch {
            timerService.state.collectLatest {
                updateText()
            }
        }

        sessionJob = scope.launch {
            timerService.currentSession.collectLatest {
                updateText()
            }
        }

        phaseJob = scope.launch {
            timerService.currentPhase.collectLatest {
                updateText()
            }
        }
    }

    private fun updateText() {
        val state = timerService.state.value
        val phase = timerService.currentPhase.value
        val time = timerService.timeLeft.value
        val session = timerService.currentSession.value
        val settings = timerService.getSettings()

        // Only show text when timer is active (running or paused)
        val isActive = state == PomodoroTimerService.TimerState.RUNNING ||
                state == PomodoroTimerService.TimerState.PAUSED

        currentText = if (isActive) {
            // Use stopwatch for work, coffee for break
            val prefix = if (phase == PomodoroTimerService.TimerPhase.WORK) "⏱\uFE0F" else "☕"
            val sessionInfo = if (phase == PomodoroTimerService.TimerPhase.WORK) {
                " | Session $session/${settings.sessionsPerRound}"
            } else {
                " | Break"
            }
            "$prefix $time$sessionInfo"
        } else {
            "" // Empty string when idle - Widget still exists but shows nothing
        }

        SwingUtilities.invokeLater {
            statusBar?.updateWidget(ID)
        }

    }
}