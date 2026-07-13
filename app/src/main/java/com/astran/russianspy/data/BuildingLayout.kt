package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {

    val rooms = listOf(
        // Intrare - jos, centru
        Room("entrance", "Intrare", RoomFunction.ENTRANCE, x = 1750f, y = 2500f, width = 300f, height = 220f),
        Room("hall_entrance", "", RoomFunction.HALLWAY, x = 1850f, y = 2150f, width = 100f, height = 350f),

        // Hol Central - hub mare
        Room("hub_central", "Hol Central", RoomFunction.HUB, x = 1500f, y = 1700f, width = 600f, height = 450f),

        // Birouri - stanga hub-ului, coridor aliniat pe centrul vertical al ambelor camere
        Room("office1", "Birouri", RoomFunction.OFFICE, x = 850f, y = 1820f, width = 400f, height = 250f),
        Room("hall_office", "", RoomFunction.HALLWAY, x = 1250f, y = 1890f, width = 250f, height = 110f),

        // Birouri 2 - fundatura din Birouri
        Room("office2", "Birouri 2", RoomFunction.OFFICE, x = 300f, y = 1850f, width = 300f, height = 200f),
        Room("hall_office2", "", RoomFunction.HALLWAY, x = 600f, y = 1900f, width = 250f, height = 100f),

        // Supraveghere - sus-stanga hub-ului
        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, x = 900f, y = 900f, width = 350f, height = 250f),
        Room("hall_surv", "", RoomFunction.HALLWAY, x = 1050f, y = 1150f, width = 100f, height = 550f),

        // Camera Servere - sus-dreapta hub-ului
        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, x = 2350f, y = 900f, width = 350f, height = 250f),
        Room("hall_server", "", RoomFunction.HALLWAY, x = 2350f, y = 1150f, width = 100f, height = 550f),

        // Laborator Criminalistic - zona izolata, coridor lung din Supraveghere
        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, x = 900f, y = 250f, width = 400f, height = 280f),
        Room("hall_forensics_long", "", RoomFunction.HALLWAY, x = 1050f, y = 530f, width = 100f, height = 370f),

        // Armurerie - zona izolata, coridor lung din Camera Servere
        Room("armory", "Armurerie", RoomFunction.ARMORY, x = 2350f, y = 250f, width = 350f, height = 280f),
        Room("hall_armory_long", "", RoomFunction.HALLWAY, x = 2450f, y = 530f, width = 100f, height = 370f),

        // Camera Pauza - din hub, spre dreapta-jos
        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, x = 2350f, y = 1820f, width = 400f, height = 260f),
        Room("hall_break", "", RoomFunction.HALLWAY, x = 2100f, y = 1900f, width = 250f, height = 100f),

        // Monitorizare Comunicatii - coridor lung din Camera Pauza, colt opus
        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, x = 3050f, y = 1700f, width = 350f, height = 280f),
        Room("hall_comms_long", "", RoomFunction.HALLWAY, x = 2750f, y = 1900f, width = 350f, height = 100f),

        // Camera de intalnire - langa hub, sus
        Room("meeting_room", "Sala de Intalniri", RoomFunction.MEETING_ROOM, x = 1650f, y = 1300f, width = 400f, height = 300f),
        Room("hall_meeting", "", RoomFunction.HALLWAY, x = 1750f, y = 1600f, width = 100f, height = 100f)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }

    fun getRoomAtPoint(px: Float, py: Float): Room? = rooms.find { it.containsPoint(px, py) }

    const val MAP_WIDTH = 3500f
    const val MAP_HEIGHT = 2800f

    // pozitia de start a jucatorului (centrul Intrarii)
    const val START_X = 1900f
    const val START_Y = 2610f
}