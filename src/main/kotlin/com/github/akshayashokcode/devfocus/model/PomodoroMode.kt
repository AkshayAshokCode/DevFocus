package com.github.akshayashokcode.devfocus.model

enum class PomodoroMode(
    val displayName: String,
    val emoji: String,
    val sessionMinutes: Int,
    val breakMinutes: Int,
    val sessionsPerRound: Int
) {
    CLASSIC(
        displayName = "Classic Pomodoro",
        emoji = "🍅",
        sessionMinutes = 25,
        breakMinutes = 5,
        sessionsPerRound = 4
    ),
    DEEP_WORK(
        displayName = "Deep Work",
        emoji = "⚡",
        sessionMinutes = 52,
        breakMinutes = 17,
        sessionsPerRound = 3
    ),
    CUSTOM(
        displayName = "Custom",
        emoji = "⚙️",
        sessionMinutes = 25,
        breakMinutes = 5,
        sessionsPerRound = 4
    );

    fun toSettings(): PomodoroSettings {
        return PomodoroSettings(
            mode = this,
            sessionMinutes = sessionMinutes,
            breakMinutes = breakMinutes,
            sessionsPerRound = sessionsPerRound
        )
    }

    fun getDescription(): String {
        return "$sessionMinutes min work • $breakMinutes min break"
    }

    override fun toString(): String {
        return "$emoji $displayName"
    }
}