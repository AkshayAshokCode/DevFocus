package com.github.akshayashokcode.devfocus.services.settings

import com.github.akshayashokcode.devfocus.model.SavedMode
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
        var savedTimerState: String = "IDLE",
        var savedSessionMinutes: Int = 25,
        var savedBreakMinutes: Int = 5,
        var savedSessionsPerRound: Int = 4,
        var savedLongBreakMinutes: Int = 15,
        var savedLongBreakAfter: Int = 4,
        var savedMode: String = "CLASSIC",
        // Ring accent colors (hex strings, e.g. "#4a90e2")
        var focusColorHex: String = "#4a90e2",
        var breakColorHex: String = "#f39c12",
        // Daily session counter
        var completedSessionsToday: Int = 0,
        var lastSessionDate: String = "",
        // Saved custom modes — parallel lists (IntelliJ XML serializer handles List<String> reliably)
        var savedModeNames: MutableList<String> = mutableListOf(),
        var savedModeSessions: MutableList<String> = mutableListOf(),
        var savedModeBreaks: MutableList<String> = mutableListOf(),
        var savedModeCounts: MutableList<String> = mutableListOf()
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

    var savedTimerState: String
        get() = state.savedTimerState
        set(value) { state.savedTimerState = value }

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

    // Ring colors
    var focusColorHex: String
        get() = state.focusColorHex
        set(value) { state.focusColorHex = value }

    var breakColorHex: String
        get() = state.breakColorHex
        set(value) { state.breakColorHex = value }

    // Daily counter
    var completedSessionsToday: Int
        get() = state.completedSessionsToday
        set(value) { state.completedSessionsToday = value }

    var lastSessionDate: String
        get() = state.lastSessionDate
        set(value) { state.lastSessionDate = value }

    // Saved modes
    fun getSavedModes(): List<SavedMode> =
        state.savedModeNames.indices.mapNotNull { i ->
            SavedMode(
                name = state.savedModeNames[i],
                sessionMinutes = state.savedModeSessions.getOrNull(i)?.toIntOrNull() ?: return@mapNotNull null,
                breakMinutes = state.savedModeBreaks.getOrNull(i)?.toIntOrNull() ?: return@mapNotNull null,
                sessionsPerRound = state.savedModeCounts.getOrNull(i)?.toIntOrNull() ?: return@mapNotNull null
            )
        }

    fun addSavedMode(mode: SavedMode) {
        state.savedModeNames.add(mode.name)
        state.savedModeSessions.add(mode.sessionMinutes.toString())
        state.savedModeBreaks.add(mode.breakMinutes.toString())
        state.savedModeCounts.add(mode.sessionsPerRound.toString())
    }

    fun deleteSavedMode(mode: SavedMode) {
        val index = state.savedModeNames.indexOf(mode.name)
        if (index >= 0) {
            state.savedModeNames.removeAt(index)
            state.savedModeSessions.removeAt(index)
            state.savedModeBreaks.removeAt(index)
            state.savedModeCounts.removeAt(index)
        }
    }
}
