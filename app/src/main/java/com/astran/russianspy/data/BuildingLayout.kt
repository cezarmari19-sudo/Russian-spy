package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

object BuildingLayout {

    val rooms = listOf(
        // Intrare - jos, centru. Centrul pe X = 1900
        Room("entrance", "Intrare", RoomFunction.ENTRANCE, x = 1750f, y = 2500f, width = 300f, height = 220f),
        // coridor intrare->hub: aliniat pe centrul X al intrarii (1900), incepe la marginea de sus a intrarii
        Room("hall_entrance", "", RoomFunction.HALLWAY, x = 1850f, y = 2150f, width = 100f, height = 350f),

        // Hol Central - hub mare. Ocupa x: 1500-2100, y: 1700-2150
        Room("hub_central", "Hol Central", RoomFunction.HUB, x = 1500f, y = 1700f, width = 600f, height = 450f),

        // Birouri - stanga hub-ului. Centrul Y al biroului = 1820+125 = 1945
        Room("office1", "Birouri", RoomFunction.OFFICE, x = 850f, y = 1820f, width = 400f, height = 250f),
        // coridor office1->hub: Y aliniat pe centrul biroului (1945), intre marginea dreapta a biroului (1250) si marginea stanga a hub-ului (1500)
        Room("hall_office", "", RoomFunction.HALLWAY, x = 1250f, y = 1895f, width = 250f, height = 100f),

        // Birouri 2 - fundatura din Birouri. Centrul Y = 1850+100 = 1950
        Room("office2", "Birouri 2", RoomFunction.OFFICE, x = 300f, y = 1850f, width = 300f, height = 200f),
        // coridor office2->office1: Y aliniat la 1950 (centrul ambelor camere e apropiat), intre marginea dreapta office2 (600) si marginea stanga office1 (850)
        Room("hall_office2", "", RoomFunction.HALLWAY, x = 600f, y = 1900f, width = 250f, height = 100f),

        // Supraveghere - sus-stanga hub-ului. Centrul X = 900+175 = 1075
        Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, x = 900f, y = 900f, width = 350f, height = 250f),
        // coridor surv->hub: X aliniat pe centrul supravegherii (1075), intre marginea de jos a supravegherii (1150) si marginea de sus a hub-ului (1700)
        Room("hall_surv", "", RoomFunction.HALLWAY, x = 1025f, y = 1150f, width = 100f, height = 550f),

        // Camera Servere - sus-dreapta hub-ului. Centrul X = 2350+175 = 2525
        Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, x = 2350f, y = 900f, width = 350f, height = 250f),
        // coridor server->hub: X aliniat pe centrul camerei servere (2525)
        Room("hall_server", "", RoomFunction.HALLWAY, x = 2475f, y = 1150f, width = 100f, height = 550f),

        // Laborator Criminalistic - zona izolata. Centrul X = 900+200 = 1100
        Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, x = 900f, y = 250f, width = 400f, height = 280f),
        // coridor forensics->surveillance: X aliniat intre centrul forensics (1100) si centrul supravegherii (1075) - facem coridorul suficient de lat ca sa acopere ambele
        Room("hall_forensics_long", "", RoomFunction.HALLWAY, x = 1025f, y = 530f, width = 100f, height = 370f),

        // Armurerie - zona izolata. Centrul X = 2350+175 = 2525
        Room("armory", "Armurerie", RoomFunction.ARMORY, x = 2350f, y = 250f, width = 350f, height = 280f),
        // coridor armory->server: X aliniat pe centrul comun (2525)
        Room("hall_armory_long", "", RoomFunction.HALLWAY, x = 2475f, y = 530f, width = 100f, height = 370f),

        // Camera Pauza - din hub, spre dreapta-jos. Centrul Y = 1820+130 = 1950
        Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, x = 2350f, y = 1820f, width = 400f, height = 260f),
        // coridor hub->break_room: Y aliniat pe centrul comun (~1945), intre marginea dreapta hub (2100) si marginea stanga break_room (2350)
        Room("hall_break", "", RoomFunction.HALLWAY, x = 2100f, y = 1895f, width = 250f, height = 100f),

        // Monitorizare Comunicatii - coridor lung din Camera Pauza. Centrul X = 3050+175=3225, dar break_room centru X=2550
        Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, x = 3050f, y = 1700f, width = 350f, height = 280f),
        // coridor break_room->comms: Y aliniat intre marginea dreapta break_room (2750) si marginea stanga comms (3050)
        Room("hall_comms_long", "", RoomFunction.HALLWAY, x = 2750f, y = 1895f, width = 300f, height = 100f),

        // Camera de intalnire - langa hub, sus. Centrul X = 1650+200=1850
        Room("meeting_room", "Sala de Intalniri", RoomFunction.MEETING_ROOM, x = 1650f, y = 1300f, width = 400f, height = 300f),
        // coridor meeting->hub: X aliniat pe centrul comun (1850), intre marginea de jos meeting (1600) si marginea de sus hub (1700)
        Room("hall_meeting", "", RoomFunction.HALLWAY, x = 1800f, y = 1600f, width = 100f, height = 100f)
    )

    fun getRoomById(id: String): Room? = rooms.find { it.id == id }

    fun getRoomAtPoint(px: Float, py: Float): Room? = rooms.find { it.containsPoint(px, py) }

    const val MAP_WIDTH = 3500f
    const val MAP_HEIGHT = 2800f

    const val START_X = 1900f
    const val START_Y = 2610f
}