package com.github.akshayashokcode.devfocus.toolWindow

import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

class PomodoroToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()) {

    private val timerService = project.getService(PomodoroTimerService::class.java) ?: error("PomodoroTimerService not available")

    private val timerLabel = JBLabel("25:00")
    private val startButton = JButton("Start")
    private val pauseButton = JButton("Pause")
    private val resetButton = JButton("Reset")

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        setupUI()
        observeTimer()
    }

    private fun setupUI() {
        val controls = JPanel().apply {
            add(startButton)
            add(pauseButton)
            add(resetButton)
        }

        add(timerLabel, BorderLayout.CENTER)
        add(controls, BorderLayout.SOUTH)

        startButton.addActionListener { timerService.start() }
        pauseButton.addActionListener { timerService.pause() }
        resetButton.addActionListener { timerService.reset() }
    }

    private fun observeTimer() {
        CoroutineScope(Dispatchers.Default).launch {
            timerService.timeLeft.collect {
                SwingUtilities.invokeLater {
                    timerLabel.text = it
                }
            }
        }
    }

    override fun removeNotify() {
        super.removeNotify()
        coroutineScope.cancel() // cleanup coroutine when panel is disposed
    }
}