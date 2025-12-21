package com.github.akshayashokcode.devfocus.model

data class PomodoroSettings(
    val mode: PomodoroMode = PomodoroMode.CLASSIC,
    val sessionMinutes: Int,
    val breakMinutes: Int,
    val sessionsPerRound: Int
)
