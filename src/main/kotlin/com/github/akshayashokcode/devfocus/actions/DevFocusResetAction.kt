package com.github.akshayashokcode.devfocus.actions

import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/** Reset the timer and session back to the initial state. */
class DevFocusResetAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.getService(PomodoroTimerService::class.java)?.reset()
    }

    override fun update(e: AnActionEvent) {
        val service = e.project?.getService(PomodoroTimerService::class.java)
        e.presentation.isEnabledAndVisible =
            service != null && service.state.value != PomodoroTimerService.TimerState.IDLE
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
