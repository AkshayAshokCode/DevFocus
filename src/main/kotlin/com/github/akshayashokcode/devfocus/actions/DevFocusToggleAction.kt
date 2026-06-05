package com.github.akshayashokcode.devfocus.actions

import com.github.akshayashokcode.devfocus.services.pomodoro.PomodoroTimerService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/** Start the timer if idle/paused; pause it if running. */
class DevFocusToggleAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val service = e.project?.getService(PomodoroTimerService::class.java) ?: return
        if (service.state.value == PomodoroTimerService.TimerState.RUNNING) {
            service.pause()
        } else {
            service.start()
        }
    }

    override fun update(e: AnActionEvent) {
        val service = e.project?.getService(PomodoroTimerService::class.java)
        e.presentation.isEnabledAndVisible = service != null
        if (service != null) {
            e.presentation.text =
                if (service.state.value == PomodoroTimerService.TimerState.RUNNING) "Pause Timer"
                else "Start / Resume Timer"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
