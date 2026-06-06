package com.github.akshayashokcode.devfocus.toolWindow

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings
import com.github.akshayashokcode.devfocus.model.SavedMode
import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.github.akshayashokcode.devfocus.services.settings.DevFocusSettingsState
import com.github.akshayashokcode.devfocus.ui.components.CircularTimerPanel
import com.github.akshayashokcode.devfocus.ui.components.SessionIndicatorPanel
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsDialog
import com.github.akshayashokcode.devfocus.ui.settings.PomodoroSettingsPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.IconUtil
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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*

class PomodoroToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()), Disposable {

    private val timerService = project.getService(PomodoroTimerService::class.java)
        ?: error("PomodoroTimerService not available")

    private val appSettings: DevFocusSettingsState
        get() = ApplicationManager.getApplication().getService(DevFocusSettingsState::class.java)

    private enum class LayoutMode { COMPACT, VERTICAL, HORIZONTAL }
    private var currentLayout = LayoutMode.VERTICAL

    // Mode selector — holds PomodoroMode entries plus any SavedMode items
    private val modeComboBox = JComboBox<Any>().apply {
        PomodoroMode.entries.forEach { addItem(it) }
        selectedItem = PomodoroMode.CLASSIC
        setRenderer { _, value, _, _, _ ->
            JLabel(when (value) {
                is PomodoroMode -> value.toString()
                is SavedMode  -> "📌 ${value.name}"
                else            -> value?.toString() ?: ""
            })
        }
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
        font = font.deriveFont(Font.PLAIN, 12f)
    }

    // Daily session count — shown below info label
    private val dailyCountLabel = JLabel("").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.PLAIN, 11f)
        isVisible = false
    }

    private val sessionTextLabel = JLabel("Session 1 of 4").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 13f)
    }

    // Phase label: "Focus" or "Break" or "Long Break"
    private val phaseLabel = JLabel("Focus").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.BOLD, 16f)
        foreground = Color(74, 144, 226)
    }

    // transparent foreground = text invisible but still occupies layout space
    private val sessionEndTimeLabel = JLabel("Focusing until 00:00 am").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.PLAIN, 12f)
        foreground = Color(0, 0, 0, 0)
    }

    private val roundEndTimeLabel = JLabel("All done by 00:00 am").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.PLAIN, 12f)
        foreground = Color(0, 0, 0, 0)
    }

    private val circularTimer = CircularTimerPanel()
    private val sessionIndicator = SessionIndicatorPanel()

    // Single play/pause toggle — icon swaps based on timer state
    private val playPauseButton = JButton(IconUtil.scale(AllIcons.Actions.Execute, null, 1.5f)).apply {
        toolTipText = "Start"
        preferredSize = Dimension(40, 32)
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
    }
    private val resetButton = JButton(IconUtil.scale(AllIcons.Actions.Restart, null, 1.5f)).apply {
        toolTipText = "Reset"
        preferredSize = Dimension(40, 32)
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
    }

    // Skip break — visible only during break/long-break phases
    private val skipBreakButton = JButton("Skip Break").apply {
        preferredSize = Dimension(110, 28)
        isVisible = false
    }

    // Custom settings panel (Custom mode only)
    private val settingsPanel = PomodoroSettingsPanel(
        applySettingsCallback = { session, breakTime, sessions ->
            timerService.applySettings(PomodoroSettings(PomodoroMode.CUSTOM, session, breakTime, sessions))
            updateInfoLabel(session, breakTime)
            updateProgressBar(sessions)
        },
        saveModeCallback = { name, session, breakTime, sessions ->
            val mode = SavedMode(name, session, breakTime, sessions)
            appSettings.addSavedMode(mode)
            modeComboBox.addItem(mode)
            modeComboBox.selectedItem = mode
        }
    )

    private val scope = CoroutineScope(Dispatchers.Default)
    private var stateJob: Job? = null
    private var timeJob: Job? = null
    private var sessionJob: Job? = null
    private var phaseJob: Job? = null
    private var dailyJob: Job? = null

    init {
        appSettings.getSavedModes().forEach { modeComboBox.addItem(it) }
        buildUI()
        applyStoredColors()
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
        val timerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(6, 6, 4, 6)
            add(circularTimer, BorderLayout.CENTER)
        }
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 4, 2)).apply {
            add(playPauseButton)
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

        // Timer group: circle + phase label + end time — centered as a unit
        val timerGroup = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(8, 10, 4, 10)
                isOpaque = false
                add(circularTimer, BorderLayout.CENTER)
            })
            add(Box.createVerticalStrut(4))
            add(centeredRow(phaseLabel))
            add(centeredRow(sessionEndTimeLabel))
        }

        val skipPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply { add(skipBreakButton) }

        // Bottom group: session stats + dots + buttons — pinned to bottom
        val bottomGroup = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
            add(centeredRow(sessionTextLabel))
            add(centeredRow(roundEndTimeLabel))
            add(progressRow())
            add(buttonRow(8))
            add(skipPanel)
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(infoPanel, BorderLayout.NORTH)
            add(JPanel(GridBagLayout()).apply { add(timerGroup) }, BorderLayout.CENTER)
            add(bottomGroup, BorderLayout.SOUTH)
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
        val topPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 4, 10)
            add(modeComboBox, BorderLayout.CENTER)
            add(settingsButton, BorderLayout.EAST)
        }

        // Timer group: circle + phase label + end time — centered in left column
        val timerGroup = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(8, 12, 4, 6)
                isOpaque = false
                add(circularTimer, BorderLayout.CENTER)
            })
            add(Box.createVerticalStrut(4))
            add(centeredRow(phaseLabel))
            add(centeredRow(sessionEndTimeLabel))
        }

        val skipPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply { add(skipBreakButton) }

        val rightPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(4, 4, 4, 12)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(centeredRow(infoLabel))
                add(centeredRow(dailyCountLabel))
            }, BorderLayout.NORTH)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
                add(centeredRow(sessionTextLabel))
                add(centeredRow(roundEndTimeLabel))
                add(progressRow())
                add(buttonRow(6))
                add(skipPanel)
            }, BorderLayout.SOUTH)
        }

        val splitPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply { fill = GridBagConstraints.BOTH; weighty = 1.0 }
            gbc.weightx = 0.55; gbc.gridx = 0; add(JPanel(GridBagLayout()).apply { add(timerGroup) }, gbc)
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
        add(playPauseButton); add(resetButton)
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
        playPauseButton.addActionListener {
            if (timerService.state.value == PomodoroTimerService.TimerState.RUNNING)
                timerService.pause() else timerService.start()
        }
        resetButton.addActionListener { timerService.reset() }
        skipBreakButton.addActionListener { timerService.skipBreak() }

        modeComboBox.addActionListener {
            when (val selected = modeComboBox.selectedItem) {
                is PomodoroMode -> when (selected) {
                    PomodoroMode.CUSTOM -> {
                        val s = timerService.getSettings()
                        settingsPanel.loadValues(s.sessionMinutes, s.breakMinutes, s.sessionsPerRound)
                    }
                    else -> {
                        timerService.applyMode(selected)
                        updateInfoLabel(selected.sessionMinutes, selected.breakMinutes)
                        updateProgressBar(selected.sessionsPerRound)
                    }
                }
                is SavedMode -> {
                    timerService.applySettings(
                        PomodoroSettings(PomodoroMode.CUSTOM, selected.sessionMinutes, selected.breakMinutes, selected.sessionsPerRound)
                    )
                    updateInfoLabel(selected.sessionMinutes, selected.breakMinutes)
                    updateProgressBar(selected.sessionsPerRound)
                }
            }
            updateSettingsPanelVisibility()
        }

        settingsButton.addActionListener {
            PomodoroSettingsDialog(project).show()
            applyStoredColors()
            refreshModeComboBox()
        }

        modeComboBox.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { if (e.isPopupTrigger) showModeContextMenu(e) }
            override fun mouseReleased(e: MouseEvent) { if (e.isPopupTrigger) showModeContextMenu(e) }
        })
    }

    private fun refreshModeComboBox() {
        val previousSelection = modeComboBox.selectedItem
        val toRemove = (0 until modeComboBox.itemCount).map { modeComboBox.getItemAt(it) }.filterIsInstance<SavedMode>()
        toRemove.forEach { modeComboBox.removeItem(it) }
        val current = appSettings.getSavedModes()
        current.forEach { modeComboBox.addItem(it) }
        if (previousSelection is SavedMode && current.any { it.name == previousSelection.name }) {
            modeComboBox.selectedItem = current.first { it.name == previousSelection.name }
        } else if (previousSelection is SavedMode) {
            modeComboBox.selectedItem = PomodoroMode.CLASSIC
        }
    }

    private fun showModeContextMenu(e: MouseEvent) {
        val selected = modeComboBox.selectedItem as? SavedMode ?: return
        val menu = JPopupMenu()
        val deleteItem = JMenuItem("Delete \"${selected.name}\"")
        deleteItem.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete mode \"${selected.name}\"?",
                "Delete Mode",
                JOptionPane.YES_NO_OPTION
            )
            if (confirm == JOptionPane.YES_OPTION) {
                appSettings.deleteSavedMode(selected)
                modeComboBox.removeItem(selected)
                if (modeComboBox.selectedItem !is SavedMode && modeComboBox.selectedItem !is PomodoroMode) {
                    modeComboBox.selectedItem = PomodoroMode.CLASSIC
                }
            }
        }
        menu.add(deleteItem)
        menu.show(e.component, e.x, e.y)
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

    private fun applyStoredColors() {
        circularTimer.focusColor = runCatching { Color.decode(appSettings.focusColorHex) }.getOrDefault(Color(74, 144, 226))
        circularTimer.breakColor = runCatching { Color.decode(appSettings.breakColorHex) }.getOrDefault(Color(243, 156, 18))
        circularTimer.repaint()
    }

    private val wallTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    private fun formatWallTime(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(wallTimeFormatter)

    private fun updateEndTimeLabels() {
        val now = System.currentTimeMillis()
        val phasePrefix = when (timerService.currentPhase.value) {
            PomodoroTimerService.TimerPhase.WORK       -> "Focusing"
            PomodoroTimerService.TimerPhase.BREAK      -> "Break"
            PomodoroTimerService.TimerPhase.LONG_BREAK -> "Long break"
        }
        sessionEndTimeLabel.text = "$phasePrefix until ${formatWallTime(now + timerService.getRemainingSessionMs())}"
        sessionEndTimeLabel.foreground = null
        roundEndTimeLabel.text = "All done by ${formatWallTime(now + timerService.getRemainingRoundMs())}"
        roundEndTimeLabel.foreground = null
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
                    if (timerService.state.value == PomodoroTimerService.TimerState.RUNNING) {
                        updateEndTimeLabels()
                    }
                }
            }
        }

        stateJob = scope.launch {
            timerService.state.collectLatest { state ->
                SwingUtilities.invokeLater {
                    resetButton.isEnabled = state != PomodoroTimerService.TimerState.IDLE

                    when (state) {
                        PomodoroTimerService.TimerState.IDLE -> {
                            playPauseButton.icon = IconUtil.scale(AllIcons.Actions.Execute, null, 1.5f)
                            playPauseButton.toolTipText = "Start"
                            playPauseButton.requestFocusInWindow()
                            sessionEndTimeLabel.foreground = Color(0, 0, 0, 0)
                            roundEndTimeLabel.foreground = Color(0, 0, 0, 0)
                        }
                        PomodoroTimerService.TimerState.PAUSED -> {
                            playPauseButton.icon = IconUtil.scale(AllIcons.Actions.Execute, null, 1.5f)
                            playPauseButton.toolTipText = "Resume"
                            playPauseButton.requestFocusInWindow()
                            sessionEndTimeLabel.foreground = Color(0, 0, 0, 0)
                            roundEndTimeLabel.foreground = Color(0, 0, 0, 0)
                        }
                        PomodoroTimerService.TimerState.RUNNING -> {
                            playPauseButton.icon = IconUtil.scale(AllIcons.Actions.Pause, null, 1.5f)
                            playPauseButton.toolTipText = "Pause"
                            playPauseButton.requestFocusInWindow()
                            updateEndTimeLabels()
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
