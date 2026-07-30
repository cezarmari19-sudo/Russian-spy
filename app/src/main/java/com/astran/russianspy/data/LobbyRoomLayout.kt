package com.astran.russianspy.data

import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction

/**
 * Cladirea MICA de asteptare (lobby) - complet separata de harta jocului
 * propriu-zis (vezi BuildingLayout.kt). 3 camere mici legate prin 2 holuri
 * scurte, gandita ca traversarea sa dureze doar cateva secunde:
 *
 *   [Setari] --hol stanga-- [Centrala/spawn] --hol dreapta-- [Dulap]
 *
 * Foloseste ACELASI mecanism de desen/miscare/vizibilitate ca jocul propriu-zis
 * (Room, buildWallSegmentsFromMergedRooms, computeVisibilityPolygon din
 * Visibility.kt) - doar cu acest layout mic in loc de BuildingLayout.rooms.
 *
 * ATENTIE: aceste valori TREBUIE sa ramana identice cu constantele LOBBY_*
 * din server/game_manager.py - daca modifici layout-ul intr-o parte,
 * modifica-l si in cealalta.
 */
object LobbyRoomLayout {
    const val CENTRAL_X = 400f
    const val CENTRAL_Y = 300f
    const val CENTRAL_W = 300f
    const val CENTRAL_H = 250f

    const val HALL_LEFT_X = 250f
    const val HALL_LEFT_Y = 390f
    const val HALL_LEFT_W = 150f
    const val HALL_LEFT_H = 70f

    const val SETTINGS_ROOM_X = 50f
    const val SETTINGS_ROOM_Y = 290f
    const val SETTINGS_ROOM_W = 200f
    const val SETTINGS_ROOM_H = 270f
    const val MONITOR_X = 150f
    const val MONITOR_Y = 390f

    const val HALL_RIGHT_X = 700f
    const val HALL_RIGHT_Y = 390f
    const val HALL_RIGHT_W = 150f
    const val HALL_RIGHT_H = 70f

    const val WARDROBE_ROOM_X = 850f
    const val WARDROBE_ROOM_Y = 290f
    const val WARDROBE_ROOM_W = 200f
    const val WARDROBE_ROOM_H = 270f
    const val WARDROBE_X = 950f
    const val WARDROBE_Y = 390f

    const val ROOM_WIDTH = 1050f
    const val ROOM_HEIGHT = 560f
    const val INTERACT_RADIUS = 80f
    const val SPAWN_X = CENTRAL_X + CENTRAL_W / 2
    const val SPAWN_Y = CENTRAL_Y + CENTRAL_H / 2

    /** Cele 5 "camere" (3 reale + 2 holuri), in acelasi format Room folosit
     * de jocul propriu-zis - permite reutilizarea directa a functiilor de
     * desen de pereti/vizibilitate din GameCanvasScreen/Visibility.kt. */
    val rooms = listOf(
        Room("lobby_settings", "Setari", RoomFunction.OFFICE, SETTINGS_ROOM_X, SETTINGS_ROOM_Y, SETTINGS_ROOM_W, SETTINGS_ROOM_H),
        Room("lobby_hall_left", "", RoomFunction.HALLWAY, HALL_LEFT_X, HALL_LEFT_Y, HALL_LEFT_W, HALL_LEFT_H),
        Room("lobby_central", "Zona de asteptare", RoomFunction.HUB, CENTRAL_X, CENTRAL_Y, CENTRAL_W, CENTRAL_H),
        Room("lobby_hall_right", "", RoomFunction.HALLWAY, HALL_RIGHT_X, HALL_RIGHT_Y, HALL_RIGHT_W, HALL_RIGHT_H),
        Room("lobby_wardrobe", "Vestiar", RoomFunction.OFFICE, WARDROBE_ROOM_X, WARDROBE_ROOM_Y, WARDROBE_ROOM_W, WARDROBE_ROOM_H),
    )

    // Paleta oficiala de culori - TREBUIE sa ramana identica cu PLAYER_COLORS
    // din server/game_manager.py (aceeasi ordine, aceleasi valori hex).
    val PLAYER_COLORS = listOf(
        "#C51111", "#132ED1", "#117F2D", "#ED54BA", "#EF7D0D",
        "#F5F557", "#3F474E", "#D6E0F0", "#6B2FBB", "#71491E",
        "#38FEDC", "#50EF39", "#83A9EF", "#E7A9F0", "#5A445A",
    )
}
