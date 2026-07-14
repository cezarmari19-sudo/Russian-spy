package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {

    val rooms = listOf(
        Room("entrance", "Intrare", RoomFunction.ENTRANCE, x = 1750f, y = 2500f, width = 300f, height = 220f),
        Room("hall_entrance", "", RoomFunction.HALLWAY, x = 1850f, y = 2110f, width = 100f, height = 430f),

        Room("hub_central", "Hol Central", RoomFunction.HUB, x = 1500f, y = 1700f, width = 600f, height = 450f),

        Room("office1", "Birouri", RoomFunction.OFFICE, x = 850f, y = 1820f, width = 400f, height = 250f),
        Room("hall_office", "", RoomFunction.HALLWAY, x = 1210f, y = 1895f, width = 330f, height = 100f),

        Room("office2", "Birouri 2", RoomFunction.OFFICE, x = 300f, y = 1850f, width = 300f, height = 200f),
        Room("hall_office2", "", RoomFunction.HALLWAY, x = 560f, y = 1900f, width = 330f, height = 100f),

        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, x = 900f, y = 900f, width = 350f, height = 250f),
        Room("hall_surv", "", RoomFunction.HALLWAY, x = 1025f, y = 1110f, width = 100f, height = 630f),

        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, x = 2350f, y = 900f, width = 350f, height = 250f),
        Room("hall_server", "", RoomFunction.HALLWAY, x = 2475f, y = 1110f, width = 100f, height = 630f),

        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, x = 900f, y = 250f, width = 400f, height = 280f),
        Room("hall_forensics_long", "", RoomFunction.HALLWAY, x = 1025f, y = 490f, width = 100f, height = 450f),

        Room("armory", "Armurerie", RoomFunction.ARMORY, x = 2350f, y = 250f, width = 350f, height = 280f),
        Room("hall_armory_long", "", RoomFunction.HALLWAY, x = 2475f, y = 490f, width = 100f, height = 450f),

        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, x = 2350f, y = 1820f, width = 400f, height = 260f),
        Room("hall_break", "", RoomFunction.HALLWAY, x = 2060f, y = 1895f, width = 330f, height = 100f),

        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, x = 3050f, y = 1700f, width = 350f, height = 280f),
        Room("hall_comms_long", "", RoomFunction.HALLWAY, x = 2710f, y = 1895f, width = 380f, height = 100f),

        Room("meeting_room", "Sala de Intalniri", RoomFunction.MEETING_ROOM, x = 1650f, y = 1300f, width = 400f, height = 300f),
        Room("hall_meeting", "", RoomFunction.HALLWAY, x = 1800f, y = 1560f, width = 100f, height = 180f)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }

    fun getRoomAtPoint(px: Float, py: Float): Room? = rooms.find { it.containsPoint(px, py) }

    const val MAP_WIDTH = 3500f
    const val MAP_HEIGHT = 2800f

    const val START_X = 1900f
    const val START_Y = 2610f
}