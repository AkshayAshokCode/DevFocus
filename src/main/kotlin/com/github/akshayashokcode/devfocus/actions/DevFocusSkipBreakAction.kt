package com.github.akshayashokcode.devfocus.actions

import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/** Skip the current break and move straight to the next work session. */
class DevFocusSkipBreakAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.getService(PomodoroTimerService::class.java)?.skipBreak()
    }

    override fun update(e: AnActionEvent) {
        val service = e.project?.getService(PomodoroTimerService::class.java)
        e.presentation.isEnabledAndVisible =
            service != null && service.currentPhase.value.isBreak
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
