package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {

    val rooms = listOf(
        Room("hub_central", "Hol Central", RoomFunction.HUB, x = 2000f, y = 2000f, width = 500f, height = 400f),

        Room("entrance", "Intrare", RoomFunction.ENTRANCE, x = 2150f, y = 2850f, width = 200f, height = 200f),
        Room("hall_entrance", "", RoomFunction.HALLWAY, x = 2200f, y = 2400f, width = 100f, height = 450f),

        Room("meeting_room", "Sala de Intalniri", RoomFunction.MEETING_ROOM, x = 2100f, y = 1350f, width = 300f, height = 300f),
        Room("hall_meeting", "", RoomFunction.HALLWAY, x = 2200f, y = 1650f, width = 100f, height = 350f),

        Room("office1", "Birouri", RoomFunction.OFFICE, x = 1250f, y = 2050f, width = 350f, height = 250f),
        Room("hall_office1", "", RoomFunction.HALLWAY, x = 1600f, y = 2125f, width = 400f, height = 100f),

        Room("office2", "Birouri 2", RoomFunction.OFFICE, x = 650f, y = 2050f, width = 300f, height = 250f),
        Room("hall_office2", "", RoomFunction.HALLWAY, x = 950f, y = 2125f, width = 300f, height = 100f),

        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, x = 400f, y = 1100f, width = 300f, height = 250f),
        Room("hall_surv", "", RoomFunction.HALLWAY, x = 625f, y = 1350f, width = 100f, height = 700f),

        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, x = 400f, y = 500f, width = 350f, height = 250f),
        Room("hall_forensics", "", RoomFunction.HALLWAY, x = 500f, y = 750f, width = 100f, height = 350f),

        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, x = 2900f, y = 1100f, width = 300f, height = 250f),
        Room("hall_server", "", RoomFunction.HALLWAY, x = 3000f, y = 1350f, width = 100f, height = 700f),

        Room("armory", "Armurerie", RoomFunction.ARMORY, x = 2900f, y = 500f, width = 300f, height = 250f),
        Room("hall_armory", "", RoomFunction.HALLWAY, x = 3000f, y = 750f, width = 100f, height = 350f),

        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, x = 2900f, y = 2050f, width = 350f, height = 250f),
        Room("hall_break", "", RoomFunction.HALLWAY, x = 2500f, y = 2125f, width = 400f, height = 100f),

        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, x = 3400f, y = 2050f, width = 300f, height = 250f),
        Room("hall_comms", "", RoomFunction.HALLWAY, x = 3250f, y = 2125f, width = 150f, height = 100f)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }

    fun getRoomAtPoint(px: Float, py: Float): Room? = rooms.find { it.containsPoint(px, py) }

    const val MAP_WIDTH = 3800f
    const val MAP_HEIGHT = 3150f

    const val START_X = 2250f
    const val START_Y = 2950f
}