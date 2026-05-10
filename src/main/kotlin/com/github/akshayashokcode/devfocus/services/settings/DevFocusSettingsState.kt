package com.github.akshayashokcode.devfocus.services.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "DevFocusSettings",
    storages = [Storage("DevFocusSettings.xml")]
)
class DevFocusSettingsState :
    PersistentStateComponent<DevFocusSettingsState.SettingsState> {

    data class SettingsState(
        var soundEnabled: Boolean = true
    )

    private var state = SettingsState()

    override fun getState(): SettingsState = state

    override fun loadState(state: SettingsState) {
        this.state = state
    }

    var soundEnabled: Boolean
        get() = state.soundEnabled
        set(value) {
            state.soundEnabled = value
        }
}