package com.github.akshayashokcode.devfocus.toolWindow

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.github.akshayashokcode.devfocus.ui.components.CircularTimerPanel
import com.github.akshayashokcode.devfocus.ui.components.SessionIndicatorPanel
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsPanel
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

class PomodoroToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()) {

    private val timerService = project.getService(PomodoroTimerService::class.java) ?: error("PomodoroTimerService not available")

    // Layout orientation tracking
    private var isHorizontalLayout = false

    // Mode selector
    private val modeComboBox = JComboBox(PomodoroMode.entries.toTypedArray()).apply {
        selectedItem = PomodoroMode.CLASSIC
    }

    // Info label showing current mode settings
    private val infoLabel = JLabel("📊 25 min work • 5 min break").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.PLAIN, 12f)
    }

    // Circular timer display
    private val circularTimer = CircularTimerPanel()

    // Session indicator with tomato icons
    private val sessionIndicator = SessionIndicatorPanel()

    // Control buttons
    private val startButton = JButton("Start")
    private val pauseButton = JButton("Pause")
    private val resetButton = JButton("Reset")

    // Custom settings panel (only visible when Custom mode selected)
    private val settingsPanel = PomodoroSettingsPanel { session, breakTime, sessions ->
        timerService.applySettings(PomodoroSettings(PomodoroMode.CUSTOM, session, breakTime, sessions))
        updateInfoLabel(session, breakTime)
        updateProgressBar(sessions)
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var stateJob: Job? = null
    private var timeJob: Job? = null
    private var sessionJob: Job? = null

    init {
        buildUI()
        setupListeners()
        observeTimer()
        updateSettingsPanelVisibility()
        setupLayoutListener()
    }

    private fun buildUI() {
        if(isHorizontalLayout) {
            buildHorizontalLayout()
        } else {
            buildVerticalLayout()
        }
    }

    private fun buildVerticalLayout() {
        // Top panel with mode selector
        val topPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 5, 10)
            add(modeComboBox, BorderLayout.CENTER)
        }

        // Info panel
        val infoPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            add(infoLabel)
        }

        // Timer panel
        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(20, 10, 20, 10)
            add(circularTimer, BorderLayout.CENTER)
        }

        // Progress panel
        val progressPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(0, 20, 10, 20)
            add(sessionIndicator, BorderLayout.CENTER)
        }

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 5)).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }

        // Center content
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(infoPanel)
            add(timerPanel)
            add(progressPanel)
            add(buttonPanel)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.SOUTH)
    }

    private fun buildHorizontalLayout() {
        buildVerticalLayout()
    }

    private fun setupListeners() {
        startButton.addActionListener { timerService.start() }
        pauseButton.addActionListener { timerService.pause() }
        resetButton.addActionListener { timerService.reset() }

        modeComboBox.addActionListener {
            val selectedMode = modeComboBox.selectedItem as PomodoroMode
            if (selectedMode != PomodoroMode.CUSTOM) {
                timerService.applyMode(selectedMode)
                updateInfoLabel(selectedMode.sessionMinutes, selectedMode.breakMinutes)
                updateProgressBar(selectedMode.sessionsPerRound)
            }
            updateSettingsPanelVisibility()
        }
    }

    private fun updateSettingsPanelVisibility() {
        val isCustom = modeComboBox.selectedItem == PomodoroMode.CUSTOM
        settingsPanel.isVisible = isCustom
        revalidate()
        repaint()
    }

    private fun updateInfoLabel(sessionMin: Int, breakMin: Int) {
        infoLabel.text = "📊 $sessionMin min work • $breakMin min break"
    }

    private fun updateProgressBar(totalSessions: Int) {
        sessionIndicator.updateSessions(timerService.currentSession.value, totalSessions)
    }

    private fun setupLayoutListener() {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                checkAndUpdateLayout()
            }
        })
    }

    private fun checkAndUpdateLayout() {
        val width = width
        val height = height

        // Determine if we should use horizontal layout (width > height * 1.5)
        val shouldBeHorizontal = width > height * 1.5

        // Only rebuild if layout orientation changed
        if (shouldBeHorizontal != isHorizontalLayout) {
            isHorizontalLayout = shouldBeHorizontal
            rebuildLayout()
        }
    }

    private fun rebuildLayout() {
        // Remove all components
        removeAll()

        // Rebuild UI with new layout
        buildUI()

        // Reconnect listeners (buttons are recreated, need new listeners)
        setupListeners()

        // Update setting panel visibility
        updateSettingsPanelVisibility()

        // Refresh the panel
        revalidate()
        repaint()
    }

    private fun observeTimer() {
        timeJob = scope.launch {
            timerService.timeLeft.collectLatest { time ->
                SwingUtilities.invokeLater {
                    val progress = timerService.getProgress()
                    circularTimer.updateTimer(time, progress, false)
                }
            }
        }

        stateJob = scope.launch {
            timerService.state.collectLatest {
                SwingUtilities.invokeLater {
                    startButton.isEnabled = it != PomodoroTimerService.TimerState.RUNNING
                    pauseButton.isEnabled = it == PomodoroTimerService.TimerState.RUNNING
                    resetButton.isEnabled = it != PomodoroTimerService.TimerState.IDLE
                    // Disable mode selector when timer is active (running or paused)
                    modeComboBox.isEnabled = it == PomodoroTimerService.TimerState.IDLE
                }
            }
        }

        sessionJob = scope.launch {
            timerService.currentSession.collectLatest { session ->
                SwingUtilities.invokeLater {
                    val settings = timerService.getSettings()
                    sessionIndicator.updateSessions(session, settings.sessionsPerRound)
                }
            }
        }
    }

    fun dispose() {
        stateJob?.cancel()
        timeJob?.cancel()
        sessionJob?.cancel()
        scope.cancel()
    }
}