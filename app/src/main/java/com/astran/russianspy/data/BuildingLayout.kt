package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {

    val rooms = listOf(
        Room("hub_central", "Hol Central", RoomFunction.HUB, x = 1600f, y = 1600f, width = 500f, height = 400f),

        Room("entrance", "Intrare", RoomFunction.ENTRANCE, x = 1750f, y = 2400f, width = 200f, height = 200f),
        Room("hall_entrance", "", RoomFunction.HALLWAY, x = 1800f, y = 1960f, width = 100f, height = 480f),

        Room("meeting_room", "Sala de Intalniri", RoomFunction.MEETING_ROOM, x = 1700f, y = 1100f, width = 300f, height = 300f),
        Room("hall_meeting", "", RoomFunction.HALLWAY, x = 1800f, y = 1360f, width = 100f, height = 280f),

        Room("office1", "Birouri", RoomFunction.OFFICE, x = 900f, y = 1650f, width = 350f, height = 250f),
        Room("hall_office", "", RoomFunction.HALLWAY, x = 1210f, y = 1700f, width = 430f, height = 100f),

        Room("office2", "Birouri 2", RoomFunction.OFFICE, x = 350f, y = 1680f, width = 300f, height = 200f),
        Room("hall_office2", "", RoomFunction.HALLWAY, x = 610f, y = 1720f, width = 330f, height = 100f),

        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, x = 950f, y = 750f, width = 300f, height = 250f),
        Room("hall_surv", "", RoomFunction.HALLWAY, x = 1050f, y = 960f, width = 100f, height = 680f),

        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, x = 950f, y = 150f, width = 350f, height = 250f),
        Room("hall_forensics", "", RoomFunction.HALLWAY, x = 1050f, y = 360f, width = 100f, height = 430f),

        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, x = 2450f, y = 750f, width = 300f, height = 250f),
        Room("hall_server", "", RoomFunction.HALLWAY, x = 2550f, y = 960f, width = 100f, height = 680f),

        Room("armory", "Armurerie", RoomFunction.ARMORY, x = 2450f, y = 150f, width = 300f, height = 250f),
        Room("hall_armory", "", RoomFunction.HALLWAY, x = 2550f, y = 360f, width = 100f, height = 430f),

        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, x = 2450f, y = 1700f, width = 350f, height = 250f),
        Room("hall_break", "", RoomFunction.HALLWAY, x = 2060f, y = 1750f, width = 430f, height = 100f),

        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, x = 2900f, y = 1550f, width = 300f, height = 250f),
        Room("hall_comms", "", RoomFunction.HALLWAY, x = 2760f, y = 1600f, width = 180f, height = 100f)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }

    fun getRoomAtPoint(px: Float, py: Float): Room? = rooms.find { it.containsPoint(px, py) }

    const val MAP_WIDTH = 3300f
    const val MAP_HEIGHT = 2700f

    const val START_X = 1850f
    const val START_Y = 2500f
}