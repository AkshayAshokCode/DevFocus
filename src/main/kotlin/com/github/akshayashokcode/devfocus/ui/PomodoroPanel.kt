package com.github.akshayashokcode.devfocus.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class PomodoroPanel : JBPanel<Nothing>() {

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        val sessionLabel = JBLabel("Focus Session").apply {
            alignmentX = Component.CENTER_ALIGNMENT
        }

        val timerLabel = JBLabel("25:00").apply {
            font = Font("Dialog", Font.BOLD, 36)
            alignmentX = CENTER_ALIGNMENT
        }

        val buttonPanel = JPanel(FlowLayout()).apply {
            add(JButton("Start"))
            add(JButton("Pause"))
            add(JButton("Reset"))
        }

        add(Box.createVerticalStrut(20))
        add(sessionLabel)
        add(Box.createVerticalStrut(10))
        add(timerLabel)
        add(Box.createVerticalStrut(20))
        add(buttonPanel)
    }
}