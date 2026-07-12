package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {
    val rooms = listOf(
        Room("entrance", "Intrare", RoomFunction.ENTRANCE, gridRow = 0, gridCol = 1),
        Room("office1", "Birouri", RoomFunction.OFFICE, gridRow = 1, gridCol = 0),
        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, gridRow = 1, gridCol = 1),
        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, gridRow = 1, gridCol = 2),
        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, gridRow = 2, gridCol = 0),
        Room("armory", "Armurerie", RoomFunction.ARMORY, gridRow = 2, gridCol = 1),
        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, gridRow = 2, gridCol = 2),
        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, gridRow = 3, gridCol = 0),
        Room("office2", "Birouri 2", RoomFunction.OFFICE, gridRow = 3, gridCol = 2)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }
}
