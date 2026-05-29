package com.github.akshayashokcode.devfocus.ui.settings

import com.github.akshayashokcode.devfocus.services.settings.DevFocusSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.*

class PomodoroSettingsDialog(project: Project) : DialogWrapper(project) {

    private val settings = ApplicationManager.getApplication()
        .getService(DevFocusSettingsState::class.java)

    private val soundCheckbox = JCheckBox("Enable notification sounds", settings.soundEnabled)

    private val autoStartCheckbox = JCheckBox(
        "Auto-start next work session after break",
        settings.autoStartNextSession
    )

    init {
        title = "DevFocus Settings"
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
            }
            add(panel, BorderLayout.NORTH)
        }
    }

    override fun doOKAction() {
        settings.soundEnabled = soundCheckbox.isSelected
        settings.autoStartNextSession = autoStartCheckbox.isSelected
        super.doOKAction()
    }
}
