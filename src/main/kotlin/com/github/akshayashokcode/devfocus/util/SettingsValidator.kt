package com.github.akshayashokcode.devfocus.util

import com.github.akshayashokcode.devfocus.model.PomodoroMode
import com.github.akshayashokcode.devfocus.model.PomodoroSettings

sealed class SettingsValidationResult {
    data class Valid(val settings: PomodoroSettings) : SettingsValidationResult()
    data class Invalid(val field: String, val errorMessage: String) : SettingsValidationResult()
}

fun validateSettings(
    session: Int?,
    breakTime: Int?,
    sessions: Int?
): SettingsValidationResult {
    if (session == null) return SettingsValidationResult.Invalid("session", "Session duration must be a number.")
    if (session !in 1..120) return SettingsValidationResult.Invalid("session", "Session duration must be between 1 and 120 minutes.")

    if (breakTime == null) return SettingsValidationResult.Invalid("break", "Break duration must be a number.")
    if (breakTime !in 1..60) return SettingsValidationResult.Invalid("break", "Break duration must be between 1 and 60 minutes.")

    if (sessions == null) return SettingsValidationResult.Invalid("sessions", "Sessions per round must be a number.")
    if (sessions !in 1..10) return SettingsValidationResult.Invalid("sessions", "Sessions per round must be between 1 and 10.")

    return SettingsValidationResult.Valid(PomodoroSettings(PomodoroMode.CUSTOM, session, breakTime, sessions))
}