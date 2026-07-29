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
    data class LobbyUpdate(val players: List<LobbyPlayerInfo>, val hostId: String) : ServerEvent()
    object RoomDeleted : ServerEvent()
    object YouWereKicked : ServerEvent()
    object YouWereBanned : ServerEvent()
    data class FriendRoomInvite(val fromDisplayName: String, val fromFriendCode: String, val roomCode: String) : ServerEvent()
    data class SpyTasksAssigned(val tasks: List<SpyTaskInfo>) : ServerEvent()
    data class SpyTaskCountChanged(val count: Int) : ServerEvent()
    data class SpyTaskUpdated(val taskId: String, val isCompleted: Boolean) : ServerEvent()
    data class SpyTaskWitnessed(val taskId: String) : ServerEvent()
    data class GameOver(val winner: String) : ServerEvent()
    data class CorpseFound(val corpse: CorpseInfo) : ServerEvent()
    object YouWereKilled : ServerEvent()
    data class MeetingCalled(
        val reason: String,
        val reporterId: String,
        val reporterName: String,
        val durationSeconds: Float
    ) : ServerEvent()
    data class VoteCast(val voterId: String) : ServerEvent()
    data class MeetingResolved(
        val ejectedPlayerId: String?,
        val ejectedPlayerName: String?,
        val wasSpy: Boolean
    ) : ServerEvent()
    data class Error(val message: String) : ServerEvent()

    // --- Morga si ADN ---
    /** Arhiva de ADN populata automat la inceputul rundei - o mostra de referinta per jucator. */
    data class DnaArchiveReady(val samples: List<DnaSampleInfo>) : ServerEvent()
    /** Confirmare privata catre spion ca a stricat proba de pe un corp (nu se afla restul camerei). */
    data class CorpseDnaTampered(val corpseId: String) : ServerEvent()
    /** ADN-ul unui corp din morga a fost extras - corpul + noua mostra recoltata. */
    data class CorpseDnaExtracted(val corpse: CorpseInfo, val sample: DnaSampleInfo?) : ServerEvent()
    /** O mostra (recoltata sau de referinta) a fost mutata intr-o alta camera (de regula spre laborator). */
    data class DnaSampleMoved(val sample: DnaSampleInfo?) : ServerEvent()
    /** Starea celor doua sloturi ale masinii de comparare din laborator s-a schimbat. */
    data class LabMachineUpdated(val harvestedSampleId: String?, val referenceSampleId: String?) : ServerEvent()
    /** Rezultatul compararii ADN - trimis STRICT catre cel care a facut comparatia. */
    data class DnaComparisonResultEvent(val result: DnaComparisonResultInfo?) : ServerEvent()
    /** Mostrele folosite intr-o comparare au fost sterse de pe server - clientul
     * trebuie sa le elimine din lista locala si din sloturile masinii. */
    data class DnaSamplesConsumed(val harvestedSampleId: String?, val referenceSampleId: String?) : ServerEvent()
    /** Trimis STRICT ucigasului dupa un omor reusit, cu cooldown-ul REAL al
     * camerei (nu o valoare fixa presupusa de client). */
    data class KillCooldownStarted(val cooldownSeconds: Float) : ServerEvent()
}

/** Un task alocat spionului: tip, camera + punct exact x/y unde trebuie facut, si daca e completat acum. */
data class SpyTaskInfo(
    val id: String,
    val taskType: String,
    val roomId: String,
    val x: Float,
    val y: Float,
    val isCompleted: Boolean
)

/** Un corp gasit pe harta (agent FBI omorat de spion). killerId nu e trimis de
 * server decat dupa raport/analiza ADN, deci ramane mereu null pentru clienti
 * inainte de acel moment. dnaCompleteness ramane null pana la extractie
 * (in_morgue + dnaExtracted marcheaza progresul prin morga). */
data class CorpseInfo(
    val id: String,
    val victimId: String,
    val roomId: String,
    val x: Float,
    val y: Float,
    val dnaRecovered: Boolean,
    val reported: Boolean,
    val inMorgue: Boolean = false,
    val dnaExtracted: Boolean = false,
    val dnaCompleteness: Int? = null
)

/** isAlive e ESENTIAL pentru ecranul de vot din meeting - fara el, jucatorii
 * morti apareau in numaratoarea "X din Y au votat" desi nu pot vota niciodata,
 * facand votul sa para mereu "incomplet" pentru cei vii. `color` identifica
 * vizual jucatorul (stil Among Us) - folosit in special la mostrele din
 * Arhiva ADN, unde numele/rolul raman ascunse. */
data class LobbyPlayerInfo(
    val id: String,
    val name: String,
    val connected: Boolean,
    val isAlive: Boolean = true,
    val color: String = "#9E9E9E"
)

/** O mostra de ADN. isReference=true => mostra de referinta din Arhiva ADN
 * (100% completeness, nedistructibila, afisata doar cu playerColor). 
 * isReference=false => mostra RECOLTATA de pe un corp (sourceCorpseId),
 * cu completeness variabil (poate fi redus de spion inainte de extractie). */
data class DnaSampleInfo(
    val id: String,
    val roomId: String,
    val completeness: Int,
    val isAnalyzed: Boolean,
    val isReference: Boolean,
    val playerColor: String,
    val sourceCorpseId: String?,
    val placedInLabSlot: Boolean
)

/** Rezultatul unei comparari la masina din laborator - vizibil STRICT pentru
 * cel care a facut comparatia. similarityPercent scade daca mostra recoltata
 * a fost stricata de spion, chiar daca apartine cu adevarat aceleiasi
 * persoane ca mostra de referinta. */
data class DnaComparisonResultInfo(
    val harvestedSampleId: String,
    val referenceSampleId: String,
    val referencePlayerColor: String,
    val similarityPercent: Int,
    val isMatch: Boolean
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

    fun createRoom(playerId: String, playerName: String, accountId: String? = null, onResult: (roomCode: String?, error: String?) -> Unit) {
        val accountParam = if (accountId != null) "&account_id=$accountId" else ""
        val url = "${ServerConfig.HTTP_BASE}/create_room?player_id=$playerId&player_name=${playerName}$accountParam"
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

    fun joinRoom(playerId: String, playerName: String, roomCode: String, accountId: String? = null, onResult: (success: Boolean, error: String?) -> Unit) {
        val accountParam = if (accountId != null) "&account_id=$accountId" else ""
        val url = "${ServerConfig.HTTP_BASE}/join_room?room_code=$roomCode&player_id=$playerId&player_name=${playerName}$accountParam"
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

    fun connectWebSocket(roomCode: String, playerId: String) {
        val url = "${ServerConfig.WS_BASE}/ws/$roomCode/$playerId"
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
                        connected = p.optBoolean("connected", true),
                        isAlive = p.optBoolean("isAlive", true),
                        color = p.optString("color", "#9E9E9E")
                    )
                }
                onEvent(ServerEvent.LobbyUpdate(players, hostId = json.optString("hostId", "")))
            }
            "error" -> onEvent(ServerEvent.Error(json.optString("message", "Eroare necunoscuta")))
            "room_deleted" -> onEvent(ServerEvent.RoomDeleted)
            "you_were_kicked" -> onEvent(ServerEvent.YouWereKicked)
            "you_were_banned" -> onEvent(ServerEvent.YouWereBanned)
            "friend_room_invite" -> onEvent(
                ServerEvent.FriendRoomInvite(
                    fromDisplayName = json.getString("fromDisplayName"),
                    fromFriendCode = json.getString("fromFriendCode"),
                    roomCode = json.getString("roomCode")
                )
            )
            "spy_tasks_assigned" -> {
                val arr = json.getJSONArray("tasks")
                val list = (0 until arr.length()).map { i ->
                    val entry = arr.getJSONObject(i)
                    SpyTaskInfo(
                        id = entry.getString("id"),
                        taskType = entry.getString("taskType"),
                        roomId = entry.getString("roomId"),
                        x = entry.getDouble("x").toFloat(),
                        y = entry.getDouble("y").toFloat(),
                        isCompleted = entry.optBoolean("isCompleted", false)
                    )
                }
                onEvent(ServerEvent.SpyTasksAssigned(list))
            }
            "spy_task_count_changed" -> onEvent(
                ServerEvent.SpyTaskCountChanged(count = json.getInt("count"))
            )
            "spy_task_updated" -> onEvent(
                ServerEvent.SpyTaskUpdated(
                    taskId = json.getString("taskId"),
                    isCompleted = json.getBoolean("isCompleted")
                )
            )
            "spy_task_witnessed" -> onEvent(
                ServerEvent.SpyTaskWitnessed(taskId = json.getString("taskId"))
            )
            "game_over" -> onEvent(
                ServerEvent.GameOver(winner = json.optString("winner", ""))
            )
            "corpse_found" -> {
                val c = json.getJSONObject("corpse")
                onEvent(ServerEvent.CorpseFound(parseCorpse(c)))
            }
            "you_were_killed" -> onEvent(ServerEvent.YouWereKilled)
            "meeting_called" -> onEvent(
                ServerEvent.MeetingCalled(
                    reason = json.optString("reason", ""),
                    reporterId = json.optString("reporterId", ""),
                    reporterName = json.optString("reporterName", ""),
                    durationSeconds = json.optDouble("durationSeconds", 75.0).toFloat()
                )
            )
            "vote_cast" -> onEvent(
                ServerEvent.VoteCast(voterId = json.getString("voterId"))
            )
            "meeting_resolved" -> onEvent(
                ServerEvent.MeetingResolved(
                    ejectedPlayerId = if (json.isNull("ejectedPlayerId")) null else json.optString("ejectedPlayerId", null),
                    ejectedPlayerName = if (json.isNull("ejectedPlayerName")) null else json.optString("ejectedPlayerName", null),
                    wasSpy = json.optBoolean("wasSpy", false)
                )
            )
            "dna_archive_ready" -> {
                val arr = json.getJSONArray("samples")
                val list = (0 until arr.length()).map { i -> parseDnaSample(arr.getJSONObject(i)) }
                onEvent(ServerEvent.DnaArchiveReady(list))
            }
            "corpse_dna_tampered" -> onEvent(
                ServerEvent.CorpseDnaTampered(corpseId = json.getString("corpseId"))
            )
            "corpse_dna_extracted" -> {
                val c = json.optJSONObject("corpse")
                if (c != null) {
                    val s = json.optJSONObject("sample")
                    onEvent(ServerEvent.CorpseDnaExtracted(parseCorpse(c), s?.let { parseDnaSample(it) }))
                }
            }
            "dna_sample_moved" -> {
                val s = json.optJSONObject("sample")
                onEvent(ServerEvent.DnaSampleMoved(s?.let { parseDnaSample(it) }))
            }
            "lab_machine_updated" -> onEvent(
                ServerEvent.LabMachineUpdated(
                    harvestedSampleId = if (json.isNull("harvestedSampleId")) null else json.optString("harvestedSampleId", null),
                    referenceSampleId = if (json.isNull("referenceSampleId")) null else json.optString("referenceSampleId", null)
                )
            )
            "dna_comparison_result" -> {
                val r = json.optJSONObject("result")
                onEvent(
                    ServerEvent.DnaComparisonResultEvent(
                        r?.let {
                            DnaComparisonResultInfo(
                                harvestedSampleId = it.getString("harvestedSampleId"),
                                referenceSampleId = it.getString("referenceSampleId"),
                                referencePlayerColor = it.optString("referencePlayerColor", "#9E9E9E"),
                                similarityPercent = it.getInt("similarityPercent"),
                                isMatch = it.getBoolean("isMatch")
                            )
                        }
                    )
                )
            }
            "dna_samples_consumed" -> onEvent(
                ServerEvent.DnaSamplesConsumed(
                    harvestedSampleId = if (json.isNull("harvestedSampleId")) null else json.optString("harvestedSampleId", null),
                    referenceSampleId = if (json.isNull("referenceSampleId")) null else json.optString("referenceSampleId", null)
                )
            )
            "kill_cooldown_started" -> onEvent(
                ServerEvent.KillCooldownStarted(cooldownSeconds = json.optDouble("cooldownSeconds", 30.0).toFloat())
            )
        }
    }

    private fun parseCorpse(c: JSONObject): CorpseInfo = CorpseInfo(
        id = c.getString("id"),
        victimId = c.getString("victimId"),
        roomId = c.getString("roomId"),
        x = c.getDouble("x").toFloat(),
        y = c.getDouble("y").toFloat(),
        dnaRecovered = c.optBoolean("dnaRecovered", false),
        reported = c.optBoolean("reported", false),
        inMorgue = c.optBoolean("inMorgue", false),
        dnaExtracted = c.optBoolean("dnaExtracted", false),
        dnaCompleteness = if (c.isNull("dnaCompleteness")) null else c.optInt("dnaCompleteness")
    )

    private fun parseDnaSample(s: JSONObject): DnaSampleInfo = DnaSampleInfo(
        id = s.getString("id"),
        roomId = s.getString("roomId"),
        completeness = s.getInt("completeness"),
        isAnalyzed = s.optBoolean("isAnalyzed", false),
        isReference = s.optBoolean("isReference", false),
        playerColor = s.optString("playerColor", "#9E9E9E"),
        sourceCorpseId = if (s.isNull("sourceCorpseId")) null else s.optString("sourceCorpseId", null),
        placedInLabSlot = s.optBoolean("placedInLabSlot", false)
    )

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

    fun sendKickPlayer(targetPlayerId: String) {
        send(JSONObject().apply {
            put("action", "kick_player")
            put("targetPlayerId", targetPlayerId)
        })
    }

    fun sendBanPlayer(targetPlayerId: String) {
        send(JSONObject().apply {
            put("action", "ban_player")
            put("targetPlayerId", targetPlayerId)
        })
    }

    /** Doar host-ul poate seta acest numar, si doar in LOBBY (verificat pe server). */
    fun sendSetSpyTaskCount(count: Int) {
        send(JSONObject().apply {
            put("action", "set_spy_task_count")
            put("count", count)
        })
    }

    /** Apelat de client dupa ce hold-ul de durata corecta s-a terminat, pentru un task de spion. */
    fun sendCompleteSpyTask(taskId: String) {
        send(JSONObject().apply {
            put("action", "complete_spy_task")
            put("taskId", taskId)
        })
    }

    /** Apelat de un agent FBI care a gasit un dispozitiv plasat de spion si il dezactiveaza. */
    fun sendDisableSpyDevice(taskId: String) {
        send(JSONObject().apply {
            put("action", "disable_spy_device")
            put("taskId", taskId)
        })
    }

    /** Apelat de spion cand incearca sa omoare un agent FBI aflat in aceeasi
     * camera. Serverul valideaza toate conditiile (martori, cooldown, etc) -
     * clientul doar trimite intentia, nu decide singur daca reuseste. */
    fun sendKillPlayer(targetPlayerId: String) {
        send(JSONObject().apply {
            put("action", "kill_player")
            put("targetPlayerId", targetPlayerId)
        })
    }

    /** Apelat de ORICE jucator viu (spion sau agent FBI) aflat langa un corp
     * nereportat, ca sa il raporteze - aduce toata camera intr-un meeting. */
    fun sendReportCorpse(corpseId: String) {
        send(JSONObject().apply {
            put("action", "report_corpse")
            put("corpseId", corpseId)
        })
    }

    /** Doar spionul, doar pe un corp aflat in Morga si inca neextras - strica
     * ADN-ul (completeness scade random intre 0 si 30%). Serverul valideaza
     * totul; clientul doar trimite intentia. */
    fun sendTamperCorpseDna(corpseId: String) {
        send(JSONObject().apply {
            put("action", "tamper_corpse_dna")
            put("corpseId", corpseId)
        })
    }

    /** Apelat de orice jucator viu aflat in Morga langa un corp raportat, ca
     * sa ii extraga ADN-ul o singura data (rezulta o DnaSample recoltata). */
    fun sendExtractCorpseDna(corpseId: String) {
        send(JSONObject().apply {
            put("action", "extract_corpse_dna")
            put("corpseId", corpseId)
        })
    }

    /** Muta o mostra (din morga sau din arhiva ADN) in Laboratorul
     * Criminalistic - jucatorul trebuie sa fie fizic in camera unde se afla
     * mostra in prezent. */
    fun sendMoveDnaSampleToLab(sampleId: String) {
        send(JSONObject().apply {
            put("action", "move_dna_sample_to_lab")
            put("sampleId", sampleId)
        })
    }

    /** Pune o mostra deja adusa in laborator intr-unul din cele doua sloturi
     * ale masinii de comparare (recoltat vs. referinta, decis automat de
     * server dupa tipul mostrei). */
    fun sendPlaceSampleInLabMachine(sampleId: String) {
        send(JSONObject().apply {
            put("action", "place_sample_in_lab_machine")
            put("sampleId", sampleId)
        })
    }

    /** Ruleaza compararea masinii din laborator - necesita ambele sloturi
     * ocupate. Rezultatul vine STRICT catre cel care a cerut comparatia. */
    fun sendCompareDnaSamples() {
        send(JSONObject().apply {
            put("action", "compare_dna_samples")
        })
    }

    /** Trimite votul jucatorului in meeting-ul activ curent. targetPlayerId
     * null inseamna vot explicit de "skip" (abtinere), diferit de a nu vota
     * deloc - serverul reseteaza votul daca e trimis din nou (jucatorul se
     * poate razgandi cat timp meeting-ul e inca activ). */
    fun sendCastVote(targetPlayerId: String?) {
        send(JSONObject().apply {
            put("action", "cast_vote")
            put("targetPlayerId", targetPlayerId ?: JSONObject.NULL)
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