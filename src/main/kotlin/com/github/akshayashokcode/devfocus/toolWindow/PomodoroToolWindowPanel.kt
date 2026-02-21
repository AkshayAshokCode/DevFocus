package com.github.akshayashokcode.devfocus.toolWindow

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.github.akshayashokcode.devfocus.ui.components.CircularTimerPanel
import com.github.akshayashokcode.devfocus.ui.components.SessionIndicatorPanel
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

class PomodoroToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()), Disposable {

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
        font = font.deriveFont(Font.BOLD, 12f)
    }

    private val sessionTextLabel = JLabel("Session 1 of 4").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 14f)
    }

    // Circular timer display
    private val circularTimer = CircularTimerPanel()

    // Session indicator with tomato icons
    private val sessionIndicator = SessionIndicatorPanel()

    // Control buttons
    private val startButton = JButton("Start").apply {
        preferredSize = Dimension(80, 32)
        // Make it a prominent primary button
        putClientProperty("JButton.buttonType", "default")
        font = font.deriveFont(Font.BOLD)
    }
    private val pauseButton = JButton("Pause").apply {
        preferredSize = Dimension(80, 32)
    }
    private val resetButton = JButton("Reset").apply {
        preferredSize = Dimension(80, 32)
    }

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
    private var phaseJob: Job? = null

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
        val infoPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 5)).apply {
            add(infoLabel)
        }

        // Timer panel
        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(15, 10, 10, 10)
            add(circularTimer, BorderLayout.CENTER)
        }

        // Session text label panel
        val sessionPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 5)).apply {
            add(sessionTextLabel)
        }

        // Progress panel
        val progressPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(5, 20, 10, 20)
            add(sessionIndicator, BorderLayout.CENTER)
        }

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 8, 5)).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }

        // Center content
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(infoPanel)
            add(timerPanel)
            add(sessionPanel)
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
        val currentSession = timerService.currentSession.value
        sessionIndicator.updateSessions(currentSession, totalSessions)
        sessionTextLabel.text = "Session $currentSession of $totalSessions"
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
                    val isBreak = timerService.currentPhase.value == PomodoroTimerService.TimerPhase.BREAK
                    circularTimer.updateTimer(time, progress, isBreak)
                }
            }
        }

        stateJob = scope.launch {
            timerService.state.collectLatest {
                SwingUtilities.invokeLater {
                    startButton.isEnabled = it != PomodoroTimerService.TimerState.RUNNING
                    pauseButton.isEnabled = it == PomodoroTimerService.TimerState.RUNNING
                    resetButton.isEnabled = it != PomodoroTimerService.TimerState.IDLE

                    // Check if we're truly idle (session and work phase) or just transitioning
                    val currentSession = timerService.currentSession.value
                    val currentPhase = timerService.currentPhase.value
                    val isTrulyIdle = it == PomodoroTimerService.TimerState.IDLE &&
                            currentSession == 1 &&
                            currentPhase == PomodoroTimerService.TimerPhase.WORK

                    modeComboBox.isEnabled = isTrulyIdle

                    // Hide custom settings panel when timer is active
                    if (!isTrulyIdle && modeComboBox.selectedItem == PomodoroMode.CUSTOM) {
                        settingsPanel.isVisible = false
                        revalidate()
                        repaint()
                    } else if (isTrulyIdle){
                        updateSettingsPanelVisibility()
                    }
                }
            }
        }

        sessionJob = scope.launch {
            timerService.currentSession.collectLatest { session ->
                SwingUtilities.invokeLater {
                    val settings = timerService.getSettings()
                    val isBreak = timerService.currentPhase.value == PomodoroTimerService.TimerPhase.BREAK
                    sessionIndicator.updateSessions(session, settings.sessionsPerRound, isBreak)
                    sessionTextLabel.text = "Session $session of ${settings.sessionsPerRound}"
                }
            }
        }

        phaseJob = scope.launch {
            timerService.currentPhase.collectLatest { phase ->
                SwingUtilities.invokeLater {
                    val settings = timerService.getSettings()
                    val session = timerService.currentSession.value
                    val isBreak = phase == PomodoroTimerService.TimerPhase.BREAK
                    sessionIndicator.updateSessions(session, settings.sessionsPerRound, isBreak)
                }
            }
        }
    }

    override fun dispose() {
        stateJob?.cancel()
        timeJob?.cancel()
        sessionJob?.cancel()
        phaseJob?.cancel()
        scope.cancel()
    }
}