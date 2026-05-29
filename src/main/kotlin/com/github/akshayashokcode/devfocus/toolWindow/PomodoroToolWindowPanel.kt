package com.github.akshayashokcode.devfocus.toolWindow

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.github.akshayashokcode.devfocus.ui.components.CircularTimerPanel
import com.github.akshayashokcode.devfocus.ui.components.SessionIndicatorPanel
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsDialog
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

class PomodoroToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()), Disposable {

    private val timerService = project.getService(PomodoroTimerService::class.java)
        ?: error("PomodoroTimerService not available")

    private enum class LayoutMode { COMPACT, VERTICAL, HORIZONTAL }
    private var currentLayout = LayoutMode.VERTICAL

    // Mode selector
    private val modeComboBox = JComboBox(PomodoroMode.entries.toTypedArray()).apply {
        selectedItem = PomodoroMode.CLASSIC
    }

    // Settings button
    private val settingsButton = JButton(AllIcons.General.Settings).apply {
        toolTipText = "Settings"
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
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

    // Phase label: shows "Focus" or "Break" clearly beneath the timer
    private val phaseLabel = JLabel("Focus").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 13f)
        foreground = Color(74, 144, 226)
    }

    private val circularTimer = CircularTimerPanel()
    private val sessionIndicator = SessionIndicatorPanel()

    // Control buttons
    private val startButton = JButton("Start").apply {
        preferredSize = Dimension(80, 32)
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

    // ---------------------------------------------------------------------------
    // Layout routing
    // ---------------------------------------------------------------------------

    private fun buildUI() {
        when (currentLayout) {
            LayoutMode.COMPACT    -> buildCompactLayout()
            LayoutMode.VERTICAL   -> buildVerticalLayout()
            LayoutMode.HORIZONTAL -> buildHorizontalLayout()
        }
    }

    /**
     * Compact: either dimension < 160px.
     * Just the circular timer + a row of buttons. Everything else hidden.
     */
    private fun buildCompactLayout() {
        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(6, 6, 4, 6)
            add(circularTimer, BorderLayout.CENTER)
        }
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 4, 2)).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }
        add(timerPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    /**
     * Vertical: height >= width.
     * Mode selector at top. Timer fills all remaining vertical space via
     * BorderLayout.CENTER so it grows/shrinks naturally. Controls pinned at bottom.
     *
     * Scenarios handled:
     *   - Tall + narrow  → small circle (min(width, timerHeight) drives diameter)
     *   - Tall + wide    → large circle (width becomes the constraint)
     */
    private fun buildVerticalLayout() {
        val topPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 4, 10)
            add(modeComboBox, BorderLayout.CENTER)
            add(settingsButton, BorderLayout.EAST)
        }

        val infoPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 4)).apply {
            add(infoLabel)
        }

        // Timer lives in CENTER — it stretches to fill whatever height is left
        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
            add(circularTimer, BorderLayout.CENTER)
        }

        val phaseLabelPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply {
            add(phaseLabel)
        }
        val sessionPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply {
            add(sessionTextLabel)
        }
        val progressPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(4, 20, 4, 20)
            add(sessionIndicator, BorderLayout.CENTER)
        }
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 8, 5)).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }

        // Fixed-height controls below the timer
        val controlsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(phaseLabelPanel)
            add(sessionPanel)
            add(progressPanel)
            add(buttonPanel)
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(infoPanel, BorderLayout.NORTH)
            add(timerPanel, BorderLayout.CENTER)   // ← grows with panel
            add(controlsPanel, BorderLayout.SOUTH)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.SOUTH)
    }

    /**
     * Horizontal: width > height.
     * Mode selector spans the top. Timer takes left 55%, controls take right 45%.
     *
     * Scenarios handled:
     *   - Wide + tall  → large circle (height drives diameter), ample control space
     *   - Wide + short → smaller circle, controls stack compactly on the right
     */
    private fun buildHorizontalLayout() {
        val topPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 4, 10)
            add(modeComboBox, BorderLayout.CENTER)
            add(settingsButton, BorderLayout.EAST)
        }

        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 12, 8, 6)
            add(circularTimer, BorderLayout.CENTER)
        }

        val phaseLabelPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply { add(phaseLabel) }
        val sessionPanel    = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply { add(sessionTextLabel) }
        val progressPanel   = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            add(sessionIndicator, BorderLayout.CENTER)
        }
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 6, 4)).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }

        // Controls centered vertically on the right side
        val rightPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 12)
            add(Box.createVerticalGlue())
            add(phaseLabelPanel)
            add(sessionPanel)
            add(progressPanel)
            add(buttonPanel)
            add(Box.createVerticalGlue())
        }

        // Split: timer 55% | controls 45%
        val splitPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.BOTH
                weighty = 1.0
            }
            gbc.weightx = 0.55; gbc.gridx = 0; add(timerPanel, gbc)
            gbc.weightx = 0.45; gbc.gridx = 1; add(rightPanel, gbc)
        }

        add(topPanel, BorderLayout.NORTH)
        add(splitPanel, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.SOUTH)
    }

    // ---------------------------------------------------------------------------
    // Responsive layout detection
    // ---------------------------------------------------------------------------

    private fun setupLayoutListener() {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                checkAndUpdateLayout()
            }
        })
    }

    private fun checkAndUpdateLayout() {
        val newLayout = when {
            width < 160 || height < 160 -> LayoutMode.COMPACT
            width > height              -> LayoutMode.HORIZONTAL
            else                        -> LayoutMode.VERTICAL
        }
        if (newLayout != currentLayout) {
            currentLayout = newLayout
            rebuildLayout()
        }
    }

    private fun rebuildLayout() {
        removeAll()
        buildUI()
        updateSettingsPanelVisibility()
        revalidate()
        repaint()
    }

    // ---------------------------------------------------------------------------
    // Listeners & helpers
    // ---------------------------------------------------------------------------

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

        settingsButton.addActionListener {
            PomodoroSettingsDialog(project).show()
        }
    }

    private fun updateSettingsPanelVisibility() {
        val isCustom = modeComboBox.selectedItem == PomodoroMode.CUSTOM
        // Never show the custom settings panel in compact mode — no room for it
        settingsPanel.isVisible = isCustom && currentLayout != LayoutMode.COMPACT
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

    // ---------------------------------------------------------------------------
    // Timer observation
    // ---------------------------------------------------------------------------

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

                    startButton.text = when (it) {
                        PomodoroTimerService.TimerState.IDLE -> "Start"
                        else -> "Resume"
                    }

                    startButton.putClientProperty("JButton.buttonType", null)
                    pauseButton.putClientProperty("JButton.buttonType", null)
                    resetButton.putClientProperty("JButton.buttonType", null)

                    when (it) {
                        PomodoroTimerService.TimerState.IDLE -> {
                            startButton.putClientProperty("JButton.buttonType", "default")
                            startButton.requestFocusInWindow()
                        }
                        PomodoroTimerService.TimerState.RUNNING -> {
                            pauseButton.putClientProperty("JButton.buttonType", "default")
                            pauseButton.requestFocusInWindow()
                        }
                        PomodoroTimerService.TimerState.PAUSED -> {
                            startButton.putClientProperty("JButton.buttonType", "default")
                            startButton.requestFocusInWindow()
                        }
                    }

                    val currentSession = timerService.currentSession.value
                    val currentPhase = timerService.currentPhase.value
                    val isTrulyIdle = it == PomodoroTimerService.TimerState.IDLE &&
                            currentSession == 1 &&
                            currentPhase == PomodoroTimerService.TimerPhase.WORK

                    modeComboBox.isEnabled = isTrulyIdle

                    if (!isTrulyIdle && modeComboBox.selectedItem == PomodoroMode.CUSTOM) {
                        settingsPanel.isVisible = false
                        revalidate()
                        repaint()
                    } else if (isTrulyIdle) {
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
                    if (isBreak) {
                        phaseLabel.text = "Break"
                        phaseLabel.foreground = Color(243, 156, 18)
                    } else {
                        phaseLabel.text = "Focus"
                        phaseLabel.foreground = Color(74, 144, 226)
                    }
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
