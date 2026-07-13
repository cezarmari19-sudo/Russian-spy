package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {

    val rooms = listOf(
        // Intrare - jos, centru
        Room("entrance", "Intrare", RoomFunction.ENTRANCE, x = 900f, y = 1150f, width = 200f, height = 150f),

        // Coridor scurt intrare -> hub central
        Room("hall_entrance", "", RoomFunction.HALLWAY, x = 960f, y = 950f, width = 80f, height = 200f),

        // Hol Central - hub mare, multe iesiri
        Room("hub_central", "Hol Central", RoomFunction.HUB, x = 750f, y = 650f, width = 500f, height = 300f),

        // Birouri - stanga hub-ului
        Room("office1", "Birouri", RoomFunction.OFFICE, x = 350f, y = 700f, width = 300f, height = 200f),
        Room("hall_office", "", RoomFunction.HALLWAY, x = 650f, y = 770f, width = 100f, height = 60f),

        // Birouri 2 - fundatura, accesibila doar din Birouri
        Room("office2", "Birouri 2", RoomFunction.OFFICE, x = 100f, y = 750f, width = 220f, height = 150f),
        Room("hall_office2", "", RoomFunction.HALLWAY, x = 320f, y = 790f, width = 30f, height = 60f),

        // Supraveghere - sus-stanga hub-ului
        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, x = 450f, y = 300f, width = 260f, height = 180f),
        Room("hall_surv", "", RoomFunction.HALLWAY, x = 700f, y = 480f, width = 60f, height = 180f),

        // Camera Servere - sus-dreapta hub-ului
        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, x = 1350f, y = 300f, width = 260f, height = 180f),
        Room("hall_server", "", RoomFunction.HALLWAY, x = 1300f, y = 480f, width = 60f, height = 180f),

        // Laborator Criminalistic - zona izolata, coridor lung din Supraveghere
        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, x = 100f, y = 100f, width = 280f, height = 180f),
        Room("hall_forensics_long", "", RoomFunction.HALLWAY, x = 380f, y = 150f, width = 250f, height = 60f),

        // Armurerie - zona izolata, coridor lung din Camera Servere
        Room("armory", "Armurerie", RoomFunction.ARMORY, x = 1700f, y = 100f, width = 250f, height = 180f),
        Room("hall_armory_long", "", RoomFunction.HALLWAY, x = 1610f, y = 150f, width = 260f, height = 60f),

        // Camera Pauza - din hub, spre dreapta-jos
        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, x = 1350f, y = 750f, width = 260f, height = 200f),
        Room("hall_break", "", RoomFunction.HALLWAY, x = 1250f, y = 800f, width = 100f, height = 60f),

        // Monitorizare Comunicatii - coridor lung din Camera Pauza, colt opus
        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, x = 1700f, y = 1000f, width = 260f, height = 180f),
        Room("hall_comms_long", "", RoomFunction.HALLWAY, x = 1610f, y = 850f, width = 60f, height = 250f),

        // Camera de intalnire - undeva accesibil global, langa hub
        Room("meeting_room", "Sala de Intalniri", RoomFunction.MEETING_ROOM, x = 850f, y = 350f, width = 220f, height = 180f),
        Room("hall_meeting", "", RoomFunction.HALLWAY, x = 930f, y = 530f, width = 60f, height = 120f)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }

    fun getRoomAtPoint(px: Float, py: Float): Room? = rooms.find { it.containsPoint(px, py) }

    // Dimensiunile totale ale hartii, pentru randare/scalare in Canvas
    const val MAP_WIDTH = 2000f
    const val MAP_HEIGHT = 1400f
}