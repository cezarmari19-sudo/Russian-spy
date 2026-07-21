package com.astran.russianspy.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ServerConfig {
    const val HTTP_BASE = "https://russian-spy-32q1.onrender.com"
    const val WS_BASE = "wss://russian-spy-32q1.onrender.com"
}

sealed class ServerEvent {
    data class PlayerMoved(val playerId: String, val targetRoomId: String) : ServerEvent()
    data class PositionUpdate(val playerId: String, val x: Float, val y: Float) : ServerEvent()
    data class PositionsSnapshot(val positions: List<PlayerPositionInfo>) : ServerEvent()
    data class GameStarted(val yourRole: String) : ServerEvent()
    data class SurveillanceEvent(val eventType: String, val fromRoomId: String) : ServerEvent()
    data class SurveillanceCamerasAssigned(val spots: List<CameraSpotInfo>) : ServerEvent()
    data class PlayerDisconnected(val playerId: String) : ServerEvent()
    data class LobbyUpdate(val players: List<LobbyPlayerInfo>) : ServerEvent()
    object RoomDeleted : ServerEvent()
    data class FriendRoomInvite(val fromDisplayName: String, val fromFriendCode: String, val roomCode: String) : ServerEvent()
    data class Error(val message: String) : ServerEvent()
}

data class LobbyPlayerInfo(
    val id: String,
    val name: String,
    val connected: Boolean
)

data class PlayerPositionInfo(
    val playerId: String,
    val roomId: String,
    val x: Float?,
    val y: Float?
)

/** Pozitia fixa (roomId + x + y exacte) a unei camere de supraveghere pentru runda curenta. */
data class CameraSpotInfo(
    val roomId: String,
    val x: Float,
    val y: Float
)

/** O intrare din lista de lobby-uri publice disponibile (ecranul stil Among Us). */
data class PublicRoomInfo(
    val roomCode: String,
    val playerCount: Int,
    val maxPlayers: Int
)

class NetworkClient(
    private val onEvent: (ServerEvent) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun createRoom(playerId: String, playerName: String, onResult: (roomCode: String?, error: String?) -> Unit) {
        val url = "${ServerConfig.HTTP_BASE}/create_room?player_id=$playerId&player_name=${playerName}"
        val request = Request.Builder().url(url).post(RequestBody.create(null, ByteArray(0))).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                onResult(null, "Nu ma pot conecta la server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    onResult(null, "Raspuns gol de la server")
                    return
                }
                val json = JSONObject(body)
                if (json.has("error")) {
                    onResult(null, json.getString("error"))
                } else {
                    onResult(json.getString("roomCode"), null)
                }
            }
        })
    }

    fun joinRoom(playerId: String, playerName: String, roomCode: String, onResult: (success: Boolean, error: String?) -> Unit) {
        val url = "${ServerConfig.HTTP_BASE}/join_room?room_code=$roomCode&player_id=$playerId&player_name=${playerName}"
        val request = Request.Builder().url(url).post(RequestBody.create(null, ByteArray(0))).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                onResult(false, "Nu ma pot conecta la server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    onResult(false, "Raspuns gol de la server")
                    return
                }
                val json = JSONObject(body)
                if (json.has("error")) {
                    onResult(false, json.getString("error"))
                } else {
                    onResult(true, null)
                }
            }
        })
    }

    /** Lista de lobby-uri publice disponibile, apelata doar la refresh MANUAL (nu automat). */
    fun fetchPublicRooms(onResult: (rooms: List<PublicRoomInfo>?, error: String?) -> Unit) {
        val url = "${ServerConfig.HTTP_BASE}/public_rooms"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                onResult(null, "Nu ma pot conecta la server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    onResult(null, "Raspuns gol de la server")
                    return
                }
                val json = JSONObject(body)
                val arr = json.getJSONArray("rooms")
                val list = (0 until arr.length()).map { i ->
                    val entry = arr.getJSONObject(i)
                    PublicRoomInfo(
                        roomCode = entry.getString("roomCode"),
                        playerCount = entry.getInt("playerCount"),
                        maxPlayers = entry.getInt("maxPlayers")
                    )
                }
                onResult(list, null)
            }
        })
    }

    /** Comuta o camera intre public/privat. Doar host-ul poate reusi (verificat si pe server). */
    fun setRoomPrivacy(roomCode: String, playerId: String, isPrivate: Boolean, onResult: (success: Boolean, error: String?) -> Unit) {
        val url = "${ServerConfig.HTTP_BASE}/set_room_privacy?room_code=$roomCode&player_id=$playerId&is_private=$isPrivate"
        val request = Request.Builder().url(url).post(RequestBody.create(null, ByteArray(0))).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                onResult(false, "Nu ma pot conecta la server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    onResult(false, "Raspuns gol de la server")
                    return
                }
                val json = JSONObject(body)
                if (json.has("error")) {
                    onResult(false, json.getString("error"))
                } else {
                    onResult(true, null)
                }
            }
        })
    }

    fun connectWebSocket(roomCode: String, playerId: String, accountId: String? = null) {
        val url = if (accountId != null) {
            "${ServerConfig.WS_BASE}/ws/$roomCode/$playerId?account_id=$accountId"
        } else {
            "${ServerConfig.WS_BASE}/ws/$roomCode/$playerId"
        }
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onEvent(ServerEvent.Error("Conexiune pierduta: ${t.message}"))
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        val json = JSONObject(text)
        when (json.optString("type")) {
            "player_moved" -> onEvent(
                ServerEvent.PlayerMoved(
                    playerId = json.getString("playerId"),
                    targetRoomId = json.getString("targetRoomId")
                )
            )
            "position_update" -> onEvent(
                ServerEvent.PositionUpdate(
                    playerId = json.getString("playerId"),
                    x = json.getDouble("x").toFloat(),
                    y = json.getDouble("y").toFloat()
                )
            )
            "positions_snapshot" -> {
                val arr = json.getJSONArray("positions")
                val list = (0 until arr.length()).map { i ->
                    val entry = arr.getJSONObject(i)
                    PlayerPositionInfo(
                        playerId = entry.getString("playerId"),
                        roomId = entry.optString("roomId", ""),
                        x = if (entry.isNull("x")) null else entry.getDouble("x").toFloat(),
                        y = if (entry.isNull("y")) null else entry.getDouble("y").toFloat()
                    )
                }
                onEvent(ServerEvent.PositionsSnapshot(list))
            }
            "game_started" -> onEvent(
                ServerEvent.GameStarted(yourRole = json.getString("yourRole"))
            )
            "surveillance_event" -> onEvent(
                ServerEvent.SurveillanceEvent(
                    eventType = json.getString("eventType"),
                    fromRoomId = json.optString("fromRoomId", "")
                )
            )
            "surveillance_cameras_assigned" -> {
                val arr = json.getJSONArray("spots")
                val list = (0 until arr.length()).map { i ->
                    val entry = arr.getJSONObject(i)
                    CameraSpotInfo(
                        roomId = entry.getString("roomId"),
                        x = entry.getDouble("x").toFloat(),
                        y = entry.getDouble("y").toFloat()
                    )
                }
                onEvent(ServerEvent.SurveillanceCamerasAssigned(list))
            }
            "player_disconnected" -> onEvent(
                ServerEvent.PlayerDisconnected(playerId = json.getString("playerId"))
            )
            "lobby_update" -> {
                val playersArray = json.getJSONArray("players")
                val players = (0 until playersArray.length()).map { i ->
                    val p = playersArray.getJSONObject(i)
                    LobbyPlayerInfo(
                        id = p.getString("id"),
                        name = p.getString("name"),
                        connected = p.optBoolean("connected", true)
                    )
                }
                onEvent(ServerEvent.LobbyUpdate(players))
            }
            "error" -> onEvent(ServerEvent.Error(json.optString("message", "Eroare necunoscuta")))
            "room_deleted" -> onEvent(ServerEvent.RoomDeleted)
            "friend_room_invite" -> onEvent(
                ServerEvent.FriendRoomInvite(
                    fromDisplayName = json.getString("fromDisplayName"),
                    fromFriendCode = json.getString("fromFriendCode"),
                    roomCode = json.getString("roomCode")
                )
            )
        }
    }

    fun sendMove(targetRoomId: String) {
        send(JSONObject().apply {
            put("action", "move")
            put("targetRoomId", targetRoomId)
        })
    }

    fun sendPositionUpdate(x: Float, y: Float) {
        send(JSONObject().apply {
            put("action", "position_update")
            put("x", x)
            put("y", y)
        })
    }

    fun sendStartGame() {
        send(JSONObject().apply {
            put("action", "start_game")
        })
    }

    fun sendSpyIntel() {
        send(JSONObject().apply {
            put("action", "spy_send_intel")
        })
    }

    fun sendDeleteRoom() {
        send(JSONObject().apply {
            put("action", "delete_room")
        })
    }

    private fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}