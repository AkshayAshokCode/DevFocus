package com.github.akshayashokcode.devfocus.ui.settings

import com.github.akshayashokcode.devfocus.services.settings.DevFocusSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class PomodoroSettingsDialog(project: Project) : DialogWrapper(project) {

    private val settings = ApplicationManager.getApplication()
        .getService(DevFocusSettingsState::class.java)

    private val soundCheckbox = JCheckBox("Enable notification sounds", settings.soundEnabled)
    private val autoStartCheckbox = JCheckBox(
        "Auto-start next work session after break",
        settings.autoStartNextSession
    )

    private var selectedFocusColor = parseColor(settings.focusColorHex, Color(74, 144, 226))
    private var selectedBreakColor = parseColor(settings.breakColorHex, Color(243, 156, 18))

    private val focusSwatchButton = swatchButton(selectedFocusColor)
    private val breakSwatchButton = swatchButton(selectedBreakColor)

    init {
        title = "DevFocus Settings"

        focusSwatchButton.addActionListener {
            val color = JColorChooser.showDialog(focusSwatchButton, "Focus Ring Color", selectedFocusColor)
            if (color != null) {
                selectedFocusColor = color
                focusSwatchButton.background = color
            }
        }
        breakSwatchButton.addActionListener {
            val color = JColorChooser.showDialog(breakSwatchButton, "Break Ring Color", selectedBreakColor)
            if (color != null) {
                selectedBreakColor = color
                breakSwatchButton.background = color
            }
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(16, 16, 8, 16)
            val panel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(soundCheckbox)
                add(Box.createVerticalStrut(10))
                add(autoStartCheckbox)
                add(Box.createVerticalStrut(4))
                add(JLabel("<html><small style='color:gray'>When disabled, a notification with a Start button<br>appears after each break so you choose when to resume.</small></html>").apply {
                    border = BorderFactory.createEmptyBorder(0, 22, 0, 0)
                })
                add(Box.createVerticalStrut(14))
                add(JSeparator())
                add(Box.createVerticalStrut(10))
                add(colorRow("Focus ring color:", focusSwatchButton))
                add(Box.createVerticalStrut(8))
                add(colorRow("Break ring color:", breakSwatchButton))
            }
            add(panel, BorderLayout.NORTH)
        }
    }

    override fun doOKAction() {
        settings.soundEnabled = soundCheckbox.isSelected
        settings.autoStartNextSession = autoStartCheckbox.isSelected
        settings.focusColorHex = toHex(selectedFocusColor)
        settings.breakColorHex = toHex(selectedBreakColor)
        super.doOKAction()
    }

    private fun colorRow(label: String, swatch: JButton) =
        JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(JLabel(label).apply { preferredSize = Dimension(130, 24) })
            add(swatch)
        }

    private fun swatchButton(color: Color) = JButton().apply {
        background = color
        preferredSize = Dimension(44, 22)
        isOpaque = true
        isBorderPainted = true
        isFocusPainted = false
    }

    private fun toHex(color: Color) =
        String.format("#%02x%02x%02x", color.red, color.green, color.blue)

    private fun parseColor(hex: String, fallback: Color) =
        runCatching { Color.decode(hex) }.getOrDefault(fallback)
}
