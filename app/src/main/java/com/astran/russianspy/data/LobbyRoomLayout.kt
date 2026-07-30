package com.astran.russianspy.data

/**
 * Camera FIZICA de asteptare (lobby) - complet separata de harta jocului
 * propriu-zis (vezi BuildingLayout.kt). Un mic "hol" cu doua obiecte
 * interactive: un monitor (setari camera, doar host) si un dulap (alegere
 * propria culoare, oricine).
 *
 * ATENTIE: aceste valori TREBUIE sa ramana identice cu constantele
 * LOBBY_* din server/game_manager.py - daca modifici layout-ul intr-o
 * parte, modifica-l si in cealalta.
 */
object LobbyRoomLayout {
    const val ROOM_WIDTH = 800f
    const val ROOM_HEIGHT = 600f
    const val MONITOR_X = 120f
    const val MONITOR_Y = 120f
    const val WARDROBE_X = 680f
    const val WARDROBE_Y = 120f
    const val INTERACT_RADIUS = 90f
    const val SPAWN_X = ROOM_WIDTH / 2
    const val SPAWN_Y = ROOM_HEIGHT / 2

    // Paleta oficiala de culori - TREBUIE sa ramana identica cu PLAYER_COLORS
    // din server/game_manager.py (aceeasi ordine, aceleasi valori hex).
    val PLAYER_COLORS = listOf(
        "#C51111", "#132ED1", "#117F2D", "#ED54BA", "#EF7D0D",
        "#F5F557", "#3F474E", "#D6E0F0", "#6B2FBB", "#71491E",
        "#38FEDC", "#50EF39", "#83A9EF", "#E7A9F0", "#5A445A",
    )
}
