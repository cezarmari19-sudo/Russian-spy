package com.astran.russianspy.model

enum class Role {
    FBI_AGENT,
    RUSSIAN_SPY
}

data class Player(
    val id: String,
    val name: String,
    var role: Role = Role.FBI_AGENT,
    var isAlive: Boolean = true,
    var currentRoomId: String = "",
    var isWearingGloves: Boolean = false
)
