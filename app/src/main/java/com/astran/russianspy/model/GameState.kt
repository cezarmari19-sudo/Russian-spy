package com.astran.russianspy.model

enum class GamePhase {
    LOBBY,
    IN_PROGRESS,
    SPY_WON,
    FBI_WON
}

data class IntelMessage(
    val id: String,
    val senderId: String,
    val sentAtMillis: Long,
    var isDelivered: Boolean = false,
    var wasInterceptedByDeath: Boolean = false,
    val requiresDelay: Boolean = true   // false daca spionul e in intalnire
)

data class GameState(
    val roomCode: String,
    var phase: GamePhase = GamePhase.LOBBY,
    val players: MutableList<Player> = mutableListOf(),
    val rooms: MutableList<Room> = mutableListOf(),
    val dnaSamples: MutableList<DnaSample> = mutableListOf(),
    val intelMessages: MutableList<IntelMessage> = mutableListOf(),
    var bombPlanted: Boolean = false,
    var bombArmedAtMillis: Long = 0L
) {
    fun aliveFbiAgents(): List<Player> =
        players.filter { it.isAlive && it.role == Role.FBI_AGENT }

    fun spy(): Player? =
        players.find { it.role == Role.RUSSIAN_SPY }
}
