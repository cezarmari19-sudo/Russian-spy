package com.astran.russianspy.model

enum class RoomFunction {
    SURVEILLANCE,
    COMMS_MONITOR,
    FORENSICS_LAB,
    ARMORY,
    SERVER_ROOM,
    OFFICE,
    BREAK_ROOM,
    ENTRANCE,
    MEETING_ROOM,
    HALLWAY,       // coridor, fara task, doar trecere
    HUB            // hol central mare, fara task
}

data class Room(
    val id: String,
    val name: String,
    val function: RoomFunction,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun containsPoint(px: Float, py: Float): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }

    fun centerX(): Float = x + width / 2f
    fun centerY(): Float = y + height / 2f

    fun hasTask(): Boolean = function != RoomFunction.HALLWAY && function != RoomFunction.HUB
}