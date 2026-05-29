package com.github.akshayashokcode.devfocus.services.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "DevFocusSettings", storages = [Storage("DevFocusSettings.xml")])
class DevFocusSettingsState : PersistentStateComponent<DevFocusSettingsState.SettingsState> {

    data class SettingsState(
        // User preferences
        var soundEnabled: Boolean = true,
        var autoStartNextSession: Boolean = true,
        // Timer state — persisted across IDE restarts
        var savedRemainingTimeMs: Long = 0L,
        var savedCurrentSession: Int = 1,
        var savedPhase: String = "WORK",
        var savedTimerWasRunning: Boolean = false,
        var savedSessionMinutes: Int = 25,
        var savedBreakMinutes: Int = 5,
        var savedSessionsPerRound: Int = 4,
        var savedLongBreakMinutes: Int = 15,
        var savedLongBreakAfter: Int = 4,
        var savedMode: String = "CLASSIC",
        // Daily session counter
        var completedSessionsToday: Int = 0,
        var lastSessionDate: String = ""
    )

    private var state = SettingsState()

    override fun getState(): SettingsState = state
    override fun loadState(state: SettingsState) { this.state = state }

    // User preferences
    var soundEnabled: Boolean
        get() = state.soundEnabled
        set(value) { state.soundEnabled = value }

    var autoStartNextSession: Boolean
        get() = state.autoStartNextSession
        set(value) { state.autoStartNextSession = value }

    // Timer persistence
    var savedRemainingTimeMs: Long
        get() = state.savedRemainingTimeMs
        set(value) { state.savedRemainingTimeMs = value }

    var savedCurrentSession: Int
        get() = state.savedCurrentSession
        set(value) { state.savedCurrentSession = value }

    var savedPhase: String
        get() = state.savedPhase
        set(value) { state.savedPhase = value }

    var savedTimerWasRunning: Boolean
        get() = state.savedTimerWasRunning
        set(value) { state.savedTimerWasRunning = value }

    var savedSessionMinutes: Int
        get() = state.savedSessionMinutes
        set(value) { state.savedSessionMinutes = value }

    var savedBreakMinutes: Int
        get() = state.savedBreakMinutes
        set(value) { state.savedBreakMinutes = value }

    var savedSessionsPerRound: Int
        get() = state.savedSessionsPerRound
        set(value) { state.savedSessionsPerRound = value }

    var savedLongBreakMinutes: Int
        get() = state.savedLongBreakMinutes
        set(value) { state.savedLongBreakMinutes = value }

    var savedLongBreakAfter: Int
        get() = state.savedLongBreakAfter
        set(value) { state.savedLongBreakAfter = value }

    var savedMode: String
        get() = state.savedMode
        set(value) { state.savedMode = value }

    // Daily counter
    var completedSessionsToday: Int
        get() = state.completedSessionsToday
        set(value) { state.completedSessionsToday = value }

    var lastSessionDate: String
        get() = state.lastSessionDate
        set(value) { state.lastSessionDate = value }
}
