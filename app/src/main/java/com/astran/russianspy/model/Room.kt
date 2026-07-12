package com.astran.russianspy.model

enum class RoomFunction {
    SURVEILLANCE,
    COMMS_MONITOR,
    FORENSICS_LAB,
    ARMORY,
    SERVER_ROOM,
    OFFICE,
    BREAK_ROOM,
    ENTRANCE
}

data class Room(
    val id: String,
    val name: String,
    val function: RoomFunction,
    val gridRow: Int,
    val gridCol: Int,
    val connectedRoomIds: List<String> = emptyList()
)
