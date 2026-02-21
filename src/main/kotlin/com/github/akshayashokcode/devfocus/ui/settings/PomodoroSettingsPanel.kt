package com.github.akshayashokcode.devfocus.ui.settings

import com.github.akshayashokcode.devfocus.util.SettingsValidationResult
import com.github.akshayashokcode.devfocus.util.validateSettings
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.LineBorder

class PomodoroSettingsPanel(
    private val applySettingsCallback: (Int, Int, Int) -> Unit
) : JPanel(GridLayout(4, 2, 8, 5)) {

    private val sessionField = JTextField("25").apply {
        preferredSize = Dimension(60, 28)
    }
    private val breakField = JTextField("5").apply {
        preferredSize = Dimension(60, 28)
    }
    private val sessionsField = JTextField("4").apply {
        preferredSize = Dimension(60, 28)
    }
    private val applyButton = JButton("Apply").apply {
        preferredSize = Dimension(100, 32)
    }

    init {
        border = BorderFactory.createEmptyBorder(10, 15, 10, 15)

        add(JLabel("Session Duration (min):"))
        add(sessionField)
        add(JLabel("Break Duration (min):"))
        add(breakField)
        add(JLabel("Sessions per Round:"))
        add(sessionsField)
        add(JLabel()) // spacer
        add(applyButton)

        clearOnType(sessionField)
        clearOnType(breakField)
        clearOnType(sessionsField)

        applyButton.addActionListener {
            // Reset all fields to default border
            val defaultBorder = UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border")
            sessionField.border = defaultBorder
            breakField.border = defaultBorder
            sessionsField.border = defaultBorder

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
                    val errorBorder = LineBorder(Color.RED, 2)

                    // Highlight the appropriate field
                    when (result.field) {
                        "session" -> sessionField.border = errorBorder
                        "break" -> breakField.border = errorBorder
                        "sessions" -> sessionsField.border = errorBorder
                    }

                    // Show popup error as well
                    JOptionPane.showMessageDialog(
                        this,
                        result.errorMessage,
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }
    private fun clearOnType(field: JTextField) {
        val defaultBorder = UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border")
        field.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = clear()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = clear()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = clear()
            private fun clear() {
                field.border = defaultBorder
            }
        })
    }
}
