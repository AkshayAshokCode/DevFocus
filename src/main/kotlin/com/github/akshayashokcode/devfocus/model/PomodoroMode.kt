package com.github.akshayashokcode.devfocus.model

enum class PomodoroMode(
    val displayName: String,
    val emoji: String,
    val sessionMinutes: Int,
    val breakMinutes: Int,
    val sessionsPerRound: Int,
    val longBreakMinutes: Int,
    val longBreakAfter: Int
) {
    CLASSIC(
        displayName = "Classic Pomodoro",
        emoji = "\uD83C\uDFAF",
        sessionMinutes = 25,
        breakMinutes = 5,
        sessionsPerRound = 4,
        longBreakMinutes = 15,
        longBreakAfter = 4
    ),
    DEEP_WORK(
        displayName = "Deep Work",
        emoji = "⚡",
        sessionMinutes = 50,
        breakMinutes = 10,
        sessionsPerRound = 2,
        longBreakMinutes = 30,
        longBreakAfter = 2
    ),
    CUSTOM(
        displayName = "Custom",
        emoji = "⚙️",
        sessionMinutes = 25,
        breakMinutes = 5,
        sessionsPerRound = 4,
        longBreakMinutes = 15,
        longBreakAfter = 4
    );

    fun toSettings(): PomodoroSettings {
        return PomodoroSettings(
            mode = this,
            sessionMinutes = sessionMinutes,
            breakMinutes = breakMinutes,
            sessionsPerRound = sessionsPerRound,
            longBreakMinutes = longBreakMinutes,
            longBreakAfter = longBreakAfter
        )
    }

    fun getDescription(): String {
        return "$sessionMinutes min work • $breakMinutes min break"
    }

    override fun toString(): String {
        return "$emoji $displayName"
    }
}