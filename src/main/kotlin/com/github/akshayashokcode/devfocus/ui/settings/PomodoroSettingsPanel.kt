package com.github.akshayashokcode.devfocus.ui.settings

import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*

class PomodoroSettingsPanel(
    private val applySettingsCallback: (Int, Int, Int) -> Unit,
    private val savePresetCallback: (name: String, session: Int, breakTime: Int, sessions: Int) -> Unit
) : JPanel(GridLayout(5, 2, 8, 5)) {

    private val sessionSpinner = JSpinner(SpinnerNumberModel(25, 5, 120, 5)).apply {
        preferredSize = Dimension(80, 28)
    }
    private val breakSpinner = JSpinner(SpinnerNumberModel(5, 1, 60, 1)).apply {
        preferredSize = Dimension(80, 28)
    }
    private val sessionsSpinner = JSpinner(SpinnerNumberModel(4, 1, 10, 1)).apply {
        preferredSize = Dimension(80, 28)
    }
    private val applyButton = JButton("Apply").apply {
        preferredSize = Dimension(100, 32)
    }
    private val savePresetButton = JButton("Save as Preset").apply {
        preferredSize = Dimension(100, 32)
    }

    init {
        border = BorderFactory.createEmptyBorder(10, 15, 10, 15)

        add(JLabel("Session Duration (min):"))
        add(sessionSpinner)
        add(JLabel("Break Duration (min):"))
        add(breakSpinner)
        add(JLabel("Sessions per Round:"))
        add(sessionsSpinner)
        add(JLabel())
        add(applyButton)
        add(JLabel())
        add(savePresetButton)

        applyButton.addActionListener {
            applySettingsCallback(
                sessionSpinner.value as Int,
                breakSpinner.value as Int,
                sessionsSpinner.value as Int
            )
        }

        savePresetButton.addActionListener {
            val name = JOptionPane.showInputDialog(
                this,
                "Enter a name for this preset:",
                "Save Preset",
                JOptionPane.PLAIN_MESSAGE
            )
            if (!name.isNullOrBlank()) {
                savePresetCallback(
                    name.trim(),
                    sessionSpinner.value as Int,
                    breakSpinner.value as Int,
                    sessionsSpinner.value as Int
                )
            }
        }
    }

    fun loadValues(sessionMin: Int, breakMin: Int, sessionsPerRound: Int) {
        sessionSpinner.value = sessionMin.coerceIn(5, 120)
        breakSpinner.value = breakMin.coerceIn(1, 60)
        sessionsSpinner.value = sessionsPerRound.coerceIn(1, 10)
    }
}
