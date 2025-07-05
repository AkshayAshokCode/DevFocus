package com.github.akshayashokcode.devfocus.toolWindow

import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsPanel
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class PomodoroToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()) {

    private val timerService = project.getService(PomodoroTimerService::class.java) ?: error("PomodoroTimerService not available")

    private val timeLabel = JLabel("25:00").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(32f)
    }

    private val startButton = JButton("Start")
    private val pauseButton = JButton("Pause")
    private val resetButton = JButton("Reset")

    private val settingsPanel = PomodoroSettingsPanel { session, breakTime, sessions ->
        timerService.applySettings(PomodoroSettings(session, breakTime, sessions))
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var stateJob: Job? = null
    private var timeJob: Job? = null

    init {
        val buttonPanel = JPanel(FlowLayout()).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(timeLabel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }

        add(centerPanel, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.SOUTH)

        setupListeners()
        observeTimer()
    }

    private fun setupListeners() {
        startButton.addActionListener { timerService.start() }
        pauseButton.addActionListener { timerService.pause() }
        resetButton.addActionListener { timerService.reset() }
    }

    private fun observeTimer() {
        timeJob = scope.launch {
            timerService.timeLeft.collectLatest {
                SwingUtilities.invokeLater {
                    timeLabel.text = it
                }
            }
        }

        stateJob = scope.launch {
            timerService.state.collectLatest {
                SwingUtilities.invokeLater {
                    startButton.isEnabled = it != PomodoroTimerService.TimerState.RUNNING
                    pauseButton.isEnabled = it == PomodoroTimerService.TimerState.RUNNING
                    resetButton.isEnabled = it != PomodoroTimerService.TimerState.IDLE
                }
            }
        }
    }

    fun dispose() {
        stateJob?.cancel()
        timeJob?.cancel()
        scope.cancel()
    }
}