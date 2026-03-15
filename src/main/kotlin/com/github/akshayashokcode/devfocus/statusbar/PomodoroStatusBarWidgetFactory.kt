package com.github.akshayashokcode.devfocus.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import org.jetbrains.annotations.NonNls

class PomodoroStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): @NonNls String = PomodoroStatusBarWidget.ID

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "DevFocus Timer"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return PomodoroStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun isEnabledByDefault(): Boolean = true
}