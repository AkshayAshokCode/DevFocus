package com.github.akshayashokcode.devfocus.ui.settings

import com.github.akshayashokcode.devfocus.services.settings.DevFocusSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class PomodoroSettingsDialog(
    project: Project
) : DialogWrapper(project) {

    private val settings =
        ApplicationManager.getApplication()
            .getService(DevFocusSettingsState::class.java)

    private val soundCheckbox =
        JCheckBox(
            "Notification sound",
            settings.soundEnabled
        )

    init {
        title = "DevFocus Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {

        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)

            add(soundCheckbox, BorderLayout.NORTH)
        }
    }

    override fun doOKAction() {

        settings.soundEnabled = soundCheckbox.isSelected

        super.doOKAction()
    }
}