package com.github.akshayashokcode.devfocus.model

data class SavedPreset(
    val name: String,
    val sessionMinutes: Int,
    val breakMinutes: Int,
    val sessionsPerRound: Int
)
