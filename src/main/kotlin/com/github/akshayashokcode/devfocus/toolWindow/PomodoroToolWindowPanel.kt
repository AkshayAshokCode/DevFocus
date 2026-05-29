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

    // Info label: current mode durations
    private val infoLabel = JLabel("📊 25 min work • 5 min break").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 12f)
    }

    // Daily session count — shown below info label
    private val dailyCountLabel = JLabel("").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.PLAIN, 11f)
        isVisible = false
    }

    private val sessionTextLabel = JLabel("Session 1 of 4").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 14f)
    }

    // Phase label: "Focus" or "Break" or "Long Break"
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
    private val pauseButton = JButton("Pause").apply { preferredSize = Dimension(80, 32) }
    private val resetButton = JButton("Reset").apply { preferredSize = Dimension(80, 32) }

    // Skip break — visible only during break/long-break phases
    private val skipBreakButton = JButton("Skip Break").apply {
        preferredSize = Dimension(110, 28)
        isVisible = false
    }

    // Custom settings panel (Custom mode only)
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
    private var dailyJob: Job? = null

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
     * Compact (<160px either dimension): timer + action buttons only.
     */
    private fun buildCompactLayout() {
        startButton.preferredSize = Dimension(60, 26)
        pauseButton.preferredSize = Dimension(60, 26)
        resetButton.preferredSize = Dimension(60, 26)

        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(6, 6, 4, 6)
            add(circularTimer, BorderLayout.CENTER)
        }
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 4, 2)).apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }
        val skipPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply { add(skipBreakButton) }
        val southPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(buttonPanel)
            add(skipPanel)
        }
        add(timerPanel, BorderLayout.CENTER)
        add(southPanel, BorderLayout.SOUTH)
    }

    /**
     * Vertical (height >= width): mode selector top, timer fills center, controls pinned bottom.
     * Handles both tall+narrow and tall+wide naturally since the circle scales with min(w,h).
     */
    private fun buildVerticalLayout() {
        startButton.preferredSize = Dimension(80, 32)
        pauseButton.preferredSize = Dimension(80, 32)
        resetButton.preferredSize = Dimension(80, 32)

        val topPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 4, 10)
            add(modeComboBox, BorderLayout.CENTER)
            add(settingsButton, BorderLayout.EAST)
        }

        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
            add(centeredRow(infoLabel))
            add(centeredRow(dailyCountLabel))
        }

        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
            add(circularTimer, BorderLayout.CENTER)
        }

        val skipPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply { add(skipBreakButton) }

        val controlsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(centeredRow(phaseLabel))
            add(centeredRow(sessionTextLabel))
            add(progressRow())
            add(buttonRow(8))
            add(skipPanel)
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(infoPanel, BorderLayout.NORTH)
            add(timerPanel, BorderLayout.CENTER)
            add(controlsPanel, BorderLayout.SOUTH)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.SOUTH)
    }

    /**
     * Horizontal (width > height): mode selector top, timer 55% left, controls 45% right.
     * Handles wide+tall (large circle) and wide+short (smaller circle, controls stay centered).
     */
    private fun buildHorizontalLayout() {
        startButton.preferredSize = Dimension(80, 32)
        pauseButton.preferredSize = Dimension(80, 32)
        resetButton.preferredSize = Dimension(80, 32)

        val topPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 4, 10)
            add(modeComboBox, BorderLayout.CENTER)
            add(settingsButton, BorderLayout.EAST)
        }

        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 12, 8, 6)
            add(circularTimer, BorderLayout.CENTER)
        }

        val skipPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply { add(skipBreakButton) }

        val rightPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 12)
            add(Box.createVerticalGlue())
            add(centeredRow(infoLabel))
            add(centeredRow(dailyCountLabel))
            add(centeredRow(phaseLabel))
            add(centeredRow(sessionTextLabel))
            add(progressRow())
            add(buttonRow(6))
            add(skipPanel)
            add(Box.createVerticalGlue())
        }

        val splitPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply { fill = GridBagConstraints.BOTH; weighty = 1.0 }
            gbc.weightx = 0.55; gbc.gridx = 0; add(timerPanel, gbc)
            gbc.weightx = 0.45; gbc.gridx = 1; add(rightPanel, gbc)
        }

        add(topPanel, BorderLayout.NORTH)
        add(splitPanel, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.SOUTH)
    }

    // Small helpers to avoid repetitive panel construction
    private fun centeredRow(component: JComponent) =
        JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).also { it.add(component) }

    private fun progressRow() = JPanel(BorderLayout(5, 5)).apply {
        border = BorderFactory.createEmptyBorder(4, 20, 4, 20)
        add(sessionIndicator, BorderLayout.CENTER)
    }

    private fun buttonRow(gap: Int) = JPanel(FlowLayout(FlowLayout.CENTER, gap, 5)).apply {
        add(startButton); add(pauseButton); add(resetButton)
    }

    // ---------------------------------------------------------------------------
    // Responsive layout detection
    // ---------------------------------------------------------------------------

    private fun setupLayoutListener() {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) = checkAndUpdateLayout()
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
            removeAll()
            buildUI()
            updateSettingsPanelVisibility()
            revalidate()
            repaint()
        }
    }

    // ---------------------------------------------------------------------------
    // Listeners & helpers
    // ---------------------------------------------------------------------------

    private fun setupListeners() {
        startButton.addActionListener { timerService.start() }
        pauseButton.addActionListener { timerService.pause() }
        resetButton.addActionListener { timerService.reset() }
        skipBreakButton.addActionListener { timerService.skipBreak() }

        modeComboBox.addActionListener {
            val selectedMode = modeComboBox.selectedItem as PomodoroMode
            if (selectedMode != PomodoroMode.CUSTOM) {
                timerService.applyMode(selectedMode)
                updateInfoLabel(selectedMode.sessionMinutes, selectedMode.breakMinutes)
                updateProgressBar(selectedMode.sessionsPerRound)
            }
            updateSettingsPanelVisibility()
        }

        settingsButton.addActionListener { PomodoroSettingsDialog(project).show() }
    }

    private fun updateSettingsPanelVisibility() {
        val isCustom = modeComboBox.selectedItem == PomodoroMode.CUSTOM
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
                    val isBreak = timerService.currentPhase.value.isBreak
                    circularTimer.updateTimer(time, progress, isBreak)
                }
            }
        }

        stateJob = scope.launch {
            timerService.state.collectLatest { state ->
                SwingUtilities.invokeLater {
                    startButton.isEnabled = state != PomodoroTimerService.TimerState.RUNNING
                    pauseButton.isEnabled = state == PomodoroTimerService.TimerState.RUNNING
                    resetButton.isEnabled = state != PomodoroTimerService.TimerState.IDLE

                    startButton.text = if (state == PomodoroTimerService.TimerState.IDLE) "Start" else "Resume"

                    startButton.putClientProperty("JButton.buttonType", null)
                    pauseButton.putClientProperty("JButton.buttonType", null)
                    resetButton.putClientProperty("JButton.buttonType", null)

                    when (state) {
                        PomodoroTimerService.TimerState.IDLE, PomodoroTimerService.TimerState.PAUSED -> {
                            startButton.putClientProperty("JButton.buttonType", "default")
                            startButton.requestFocusInWindow()
                        }
                        PomodoroTimerService.TimerState.RUNNING -> {
                            pauseButton.putClientProperty("JButton.buttonType", "default")
                            pauseButton.requestFocusInWindow()
                        }
                    }

                    val currentSession = timerService.currentSession.value
                    val currentPhase = timerService.currentPhase.value
                    val isTrulyIdle = state == PomodoroTimerService.TimerState.IDLE &&
                            currentSession == 1 && !currentPhase.isBreak

                    modeComboBox.isEnabled = isTrulyIdle

                    if (!isTrulyIdle && modeComboBox.selectedItem == PomodoroMode.CUSTOM) {
                        settingsPanel.isVisible = false
                        revalidate(); repaint()
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
                    val isBreak = timerService.currentPhase.value.isBreak
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
                    sessionIndicator.updateSessions(session, settings.sessionsPerRound, phase.isBreak)

                    when (phase) {
                        PomodoroTimerService.TimerPhase.WORK -> {
                            phaseLabel.text = "Focus"
                            phaseLabel.foreground = Color(74, 144, 226)
                        }
                        PomodoroTimerService.TimerPhase.BREAK -> {
                            phaseLabel.text = "Break"
                            phaseLabel.foreground = Color(243, 156, 18)
                        }
                        PomodoroTimerService.TimerPhase.LONG_BREAK -> {
                            phaseLabel.text = "Long Break"
                            phaseLabel.foreground = Color(155, 89, 182) // Purple for long break
                        }
                    }

                    skipBreakButton.isVisible = phase.isBreak
                }
            }
        }

        dailyJob = scope.launch {
            timerService.dailySessionCount.collectLatest { count ->
                SwingUtilities.invokeLater {
                    if (count > 0) {
                        dailyCountLabel.text = "🍅 $count session${if (count == 1) "" else "s"} today"
                        dailyCountLabel.isVisible = true
                    } else {
                        dailyCountLabel.isVisible = false
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
        dailyJob?.cancel()
        scope.cancel()
    }
}
