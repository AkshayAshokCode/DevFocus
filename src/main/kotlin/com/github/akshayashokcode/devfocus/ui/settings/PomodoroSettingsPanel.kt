package com.github.akshayashokcode.devfocus.ui.settings

import com.github.akshayashokcode.devfocus.util.SettingsValidationResult
import com.github.akshayashokcode.devfocus.util.validateSettings
import java.awt.GridLayout
import javax.swing.*

class PomodoroSettingsPanel(
    private val applySettingsCallback: (Int, Int, Int) -> Unit
) : JPanel(GridLayout(4, 2, 5, 5)) {

    private val sessionField = JTextField("25")
    private val breakField = JTextField("5")
    private val sessionsField = JTextField("4")
    private val applyButton = JButton("Apply")

    init {
        add(JLabel("Session Duration (min):"))
        add(sessionField)
        add(JLabel("Break Duration (min):"))
        add(breakField)
        add(JLabel("Sessions per Round:"))
        add(sessionsField)
        add(JLabel()) // spacer
        add(applyButton)

        applyButton.addActionListener {
            val session = sessionField.text.toIntOrNull()
            val breakTime = breakField.text.toIntOrNull()
            val sessions = sessionsField.text.toIntOrNull()

            when (val result = validateSettings(session, breakTime, sessions)) {
                is SettingsValidationResult.Valid -> {
                    applySettingsCallback(
                        result.settings.sessionMinutes,
                        result.settings.breakMinutes,
                        result.settings.sessionsPerRound
                    )
                    JOptionPane.showMessageDialog(this, "Settings applied successfully.")
                }
                is SettingsValidationResult.Invalid -> {
                    JOptionPane.showMessageDialog(this, result.errorMessage, "Validation Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }
}
