package com.astran.russianspy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.State
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.GamePhase
import com.astran.russianspy.model.GameState
import com.astran.russianspy.model.Player
import com.astran.russianspy.model.Role
import com.astran.russianspy.model.SurveillanceEvent
import com.astran.russianspy.model.SurveillanceEventType
import com.astran.russianspy.network.AccountSocketManager
import com.astran.russianspy.network.LobbyPlayerInfo
import com.astran.russianspy.network.NetworkClient
import com.astran.russianspy.network.PlayerPositionInfo
import com.astran.russianspy.network.ServerEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LivePosition(val roomId: String, val x: Float, val y: Float)

/** Pozitia fixa (camera + punct exact X/Y in interiorul ei) a unei camere de supraveghere, pentru runda curenta. */
data class SurveillanceCameraSpot(val roomId: String, val x: Float, val y: Float)

/** O invitatie live primita de la un prieten, catre camera lui de joc. */
data class FriendRoomInviteInfo(val fromDisplayName: String, val fromFriendCode: String, val roomCode: String)

/** De ce am fost scosi din camera curenta de catre host. */
enum class RemovalReason { KICKED, BANNED }

class GameViewModel : ViewModel() {

    private val _gameState = mutableStateOf<GameState?>(null)
    val gameState: State<GameState?> = _gameState

    // Devine true cand camera curenta a fost stearsa (de host, din LOBBY) - ecranele
    // (WaitingRoomScreen, etc) observa asta si navigheaza inapoi la meniul principal.
    private val _roomWasDeleted = mutableStateOf(false)
    val roomWasDeleted: State<Boolean> = _roomWasDeleted

    /** Apelat de ecranul care a navigat inapoi dupa un room_deleted, ca sa reseteze flag-ul. */
    fun acknowledgeRoomDeleted() {
        _roomWasDeleted.value = false
    }

    // Ultima invitatie primita de la un prieten (cod camera + numele lui) - un ecran
    // (ex. MainMenuScreen) o poate observa si oferi "Alatura-te" cat timp e activa.
    private val _incomingFriendInvite = mutableStateOf<FriendRoomInviteInfo?>(null)
    val incomingFriendInvite: State<FriendRoomInviteInfo?> = _incomingFriendInvite

    fun dismissFriendInvite() {
        _incomingFriendInvite.value = null
    }

    /**
     * Porneste conexiunea GLOBALA de prezenta pentru invitatii de prieteni
     * (AccountSocketManager) - trebuie apelata o singura data, la pornirea
     * aplicatiei (din MainActivity), cat timp exista un accountId salvat.
     * Independenta total de conexiunea de joc: ramane activa in meniuri,
     * ecranul de prieteni, lobby si in meci, ca invitatiile sa poata fi
     * primite oriunde, nu doar cand esti deja intr-o camera.
     */
    fun startAccountPresence(accountId: String) {
        AccountSocketManager.start(accountId) { fromDisplayName, fromFriendCode, roomCode ->
            _incomingFriendInvite.value = FriendRoomInviteInfo(
                fromDisplayName = fromDisplayName,
                fromFriendCode = fromFriendCode,
                roomCode = roomCode
            )
        }
    }

    fun deleteRoom() {
        networkClient?.sendDeleteRoom()
    }

    /** Doar host-ul poate da kick (verificat si pe server) - jucatorul poate reintra oricand cu acelasi cod. */
    fun kickPlayer(targetPlayerId: String) {
        networkClient?.sendKickPlayer(targetPlayerId)
    }

    /** Doar host-ul poate da ban (verificat si pe server) - jucatorul NU se mai poate intoarce in aceasta camera. */
    fun banPlayer(targetPlayerId: String) {
        networkClient?.sendBanPlayer(targetPlayerId)
    }

    // Devine "KICKED" sau "BANNED" cand serverul ne anunta ca hostul ne-a scos din
    // camera curenta - WaitingRoomScreen observa asta si navigheaza inapoi la meniu,
    // afisand mesajul potrivit. null inseamna "nimic de aratat".
    private val _removalReason = mutableStateOf<RemovalReason?>(null)
    val removalReason: State<RemovalReason?> = _removalReason

    fun acknowledgeRemoval() {
        _removalReason.value = null
    }

    /**
     * Iesire manuala din LOBBY (camera de asteptare, inainte sa inceapa jocul) -
     * apelata din butonul de iesire din WaitingRoomScreen. Anunta serverul ca a
     * plecat (deconectand WebSocket-ul, la fel ca la o inchidere normala a
     * conexiunii) si reseteaza starea locala, ca revenirea in meniul principal
     * sa nu ramana cu resturi din lobby-ul anterior (jucatori, cod camera etc.).
     */
    fun leaveLobby() {
        networkClient?.disconnect()
        networkClient = null

        _gameState.value = null
        _isHost.value = false
        _errorMessage.value = null
        _currentRoomId.value = "entrance"
        _removalReason.value = null

        lobbyPlayers.clear()
        playerNames.clear()
    }

    /**
     * Iesire manuala din meci/camera curenta (apelata din meniul de Setari in joc,
     * nu din onCleared() - aici jucatorul alege activ sa plece). Deconecteaza
     * WebSocket-ul si reseteaza starea locala, ca la revenirea in meniul principal
     * sa nu ramana resturi din partida anterioara (pozitii, nume, rol, camere).
     */
    fun leaveGame() {
        networkClient?.disconnect()
        networkClient = null

        _gameState.value = null
        _gameStarted.value = false
        _myRole.value = null
        _isHost.value = false
        _errorMessage.value = null
        _activeSurveillanceEvent.value = null
        _currentRoomId.value = "entrance"
        _localPlayerX.value = BuildingLayout.START_X
        _localPlayerY.value = BuildingLayout.START_Y

        lobbyPlayers.clear()
        playerLivePositions.clear()
        playerNames.clear()
        surveillanceCameraSpots.clear()
    }

    private val _localPlayerId = mutableStateOf("")
    val localPlayerId: State<String> = _localPlayerId

    private val _localPlayerName = mutableStateOf("")
    val localPlayerName: State<String> = _localPlayerName

    private val _isHost = mutableStateOf(false)
    val isHost: State<Boolean> = _isHost

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _activeSurveillanceEvent = mutableStateOf<SurveillanceEvent?>(null)
    val activeSurveillanceEvent: State<SurveillanceEvent?> = _activeSurveillanceEvent

    private val _currentRoomId = mutableStateOf("entrance")
    val currentRoomId: State<String> = _currentRoomId

    // Pozitia LOCALA a jucatorului curent (X/Y in coordonate "world"), tinuta aici
    // (nu in GameCanvasScreen cu "remember") ca sa SUPRAVIETUIASCA navigarii catre alte
    // ecrane (ex: camerele de supraveghere). Altfel Compose distruge starea ecranului
    // la navigare si jucatorul reapare la pozitia de start cand se intoarce pe harta.
    private val _localPlayerX = mutableStateOf(BuildingLayout.START_X)
    val localPlayerX: State<Float> = _localPlayerX

    private val _localPlayerY = mutableStateOf(BuildingLayout.START_Y)
    val localPlayerY: State<Float> = _localPlayerY

    fun setLocalPlayerPosition(x: Float, y: Float) {
        _localPlayerX.value = x
        _localPlayerY.value = y
    }

    val lobbyPlayers = mutableStateListOf<LobbyPlayerInfo>()

    // Pozitiile FIXE (camera + X + Y exacte) ale celor 4 camere de supraveghere pentru
    // RUNDA CURENTA. Ideal vin de la server (acelasi pentru toti jucatorii, generat o
    // singura data de host/server la inceputul rundei). Daca serverul nu trimite inca
    // acest eveniment, se genereaza local ca fallback, ca ecranul de camere sa functioneze.
    val surveillanceCameraSpots = mutableStateListOf<SurveillanceCameraSpot>()

    /** Genereaza local 4 camere random: camera hartii aleasa random + punct random in interiorul ei. */
    fun generateRandomCameraSpotsLocally() {
        if (surveillanceCameraSpots.isNotEmpty()) return // deja generate pentru runda asta
        // Excludem holurile (HALLWAY) - o camera de supraveghere pusa pe un hol nu are sens,
        // vrem camere reale (birouri, armurerie, servere, etc), inclusiv HUB si ENTRANCE.
        val candidateRooms = BuildingLayout.rooms.filter { it.function.name != "HALLWAY" }
        val chosen = candidateRooms.shuffled().take(4)
        surveillanceCameraSpots.clear()
        chosen.forEach { room ->
            // Margine mica fata de pereti (10% din dimensiune), ca punctul sa nu cada
            // chiar pe/langa un perete si sa aiba o vedere ingusta artificial.
            val marginX = room.width * 0.1f
            val marginY = room.height * 0.1f
            val spotX = room.x + marginX + Random.nextFloat() * (room.width - marginX * 2f)
            val spotY = room.y + marginY + Random.nextFloat() * (room.height - marginY * 2f)
            surveillanceCameraSpots.add(SurveillanceCameraSpot(room.id, spotX, spotY))
        }
    }

    // Pozitiile LIVE (roomId + x + y exacte) ale tuturor jucatorilor. Folosit de monitoarele de supraveghere.
    val playerLivePositions = mutableStateMapOf<String, LivePosition>()

    val playerNames = mutableStateMapOf<String, String>()

    private val _gameStarted = mutableStateOf(false)
    val gameStarted: State<Boolean> = _gameStarted

    private val _myRole = mutableStateOf<Role?>(null)
    val myRole: State<Role?> = _myRole

    private var networkClient: NetworkClient? = null

    fun createRoom(playerName: String, accountId: String? = null) {
        val playerId = generatePlayerId()
        _localPlayerId.value = playerId
        _localPlayerName.value = playerName
        _errorMessage.value = null

        val client = NetworkClient(onEvent = ::handleServerEvent)
        networkClient = client

        client.createRoom(playerId, playerName, accountId) { roomCode, error ->
            if (error != null || roomCode == null) {
                _errorMessage.value = error ?: "Eroare necunoscuta la crearea camerei"
                return@createRoom
            }
            _isHost.value = true
            _gameState.value = GameState(
                roomCode = roomCode,
                players = mutableListOf(Player(id = playerId, name = playerName, currentRoomId = "entrance")),
                rooms = BuildingLayout.rooms.toMutableList()
            )
            _currentRoomId.value = "entrance"
            playerNames[playerId] = playerName
            client.connectWebSocket(roomCode, playerId)
        }
    }

    fun joinRoom(playerName: String, roomCode: String, accountId: String? = null) {
        val playerId = generatePlayerId()
        _localPlayerId.value = playerId
        _localPlayerName.value = playerName
        _errorMessage.value = null

        val client = NetworkClient(onEvent = ::handleServerEvent)
        networkClient = client

        client.joinRoom(playerId, playerName, roomCode, accountId) { success, error ->
            if (!success) {
                _errorMessage.value = error ?: "Nu m-am putut alatura camerei"
                return@joinRoom
            }
            _isHost.value = false
            _gameState.value = GameState(
                roomCode = roomCode,
                players = mutableListOf(Player(id = playerId, name = playerName, currentRoomId = "entrance")),
                rooms = BuildingLayout.rooms.toMutableList()
            )
            _currentRoomId.value = "entrance"
            playerNames[playerId] = playerName
            client.connectWebSocket(roomCode, playerId)
        }
    }

    fun startGame() {
        networkClient?.sendStartGame()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun moveToRoom(roomId: String) {
        _currentRoomId.value = roomId
        val state = _gameState.value ?: return
        val player = state.players.find { it.id == _localPlayerId.value } ?: return
        player.currentRoomId = roomId
        networkClient?.sendMove(roomId)
    }

    /** Apelat la fiecare cadru de miscare, ca monitoarele sa vada pozitia exacta X/Y a jucatorului local. */
    fun updateLocalPosition(x: Float, y: Float, roomId: String) {
        playerLivePositions[_localPlayerId.value] = LivePosition(roomId, x, y)
        networkClient?.sendPositionUpdate(x, y)
    }

    fun spySendsIntel(fromRoomId: String) {
        val event = SurveillanceEvent(
            id = "evt_${Random.nextLong()}",
            type = SurveillanceEventType.SPY_SENDING_INTEL,
            timestampMillis = System.currentTimeMillis(),
            relatedRoomId = fromRoomId
        )
        _activeSurveillanceEvent.value = event
        networkClient?.sendSpyIntel()

        viewModelScope.launch {
            delay(4000L)
            if (_activeSurveillanceEvent.value?.id == event.id) {
                _activeSurveillanceEvent.value = null
            }
        }
    }

    private fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.RoomDeleted -> {
                // Camera a fost stearsa de host - resetam starea locala a jocului,
                // ca ecranul curent sa poata naviga inapoi la meniul principal.
                _gameState.value = null
                _roomWasDeleted.value = true
            }
            is ServerEvent.FriendRoomInvite -> {
                // Nu mai poate ajunge pe acest canal (serverul trimite invitatii doar
                // pe conexiunea globala /ws/account/{account_id}, gestionata de
                // AccountSocketManager) - ramas doar ca ramura goala, ca "when" sa
                // fie exhaustiv fata de sealed class-ul ServerEvent.
            }
            is ServerEvent.LobbyUpdate -> {
                lobbyPlayers.clear()
                lobbyPlayers.addAll(event.players)
                event.players.forEach { playerNames[it.id] = it.name }
                // Hostul se poate schimba automat pe server (daca hostul anterior a
                // plecat din LOBBY) - recalculam mereu local din hostId primit, in loc
                // sa tinem minte doar starea initiala setata la create/joinRoom.
                if (event.hostId.isNotEmpty()) {
                    _isHost.value = (event.hostId == _localPlayerId.value)
                }
            }
            is ServerEvent.YouWereKicked -> {
                _removalReason.value = RemovalReason.KICKED
            }
            is ServerEvent.YouWereBanned -> {
                _removalReason.value = RemovalReason.BANNED
            }
            is ServerEvent.PositionsSnapshot -> {
                event.positions.forEach { info ->
                    if (info.x != null && info.y != null) {
                        playerLivePositions[info.playerId] = LivePosition(info.roomId, info.x, info.y)
                    }
                }
            }
            is ServerEvent.PositionUpdate -> {
                val existing = playerLivePositions[event.playerId]
                val roomId = existing?.roomId ?: ""
                playerLivePositions[event.playerId] = LivePosition(roomId, event.x, event.y)
            }
            is ServerEvent.GameStarted -> {
                _myRole.value = if (event.yourRole == "RUSSIAN_SPY") Role.RUSSIAN_SPY else Role.FBI_AGENT
                _gameState.value?.phase = GamePhase.IN_PROGRESS
                _gameStarted.value = true
                // Camerele se regenereaza pentru fiecare runda noua. Daca serverul trimite
                // propriul eveniment "surveillance_cameras_assigned" imediat dupa asta,
                // acela va suprascrie fallback-ul local mai jos cu pozitiile reale, comune
                // tuturor jucatorilor.
                surveillanceCameraSpots.clear()
                generateRandomCameraSpotsLocally()
            }
            is ServerEvent.SurveillanceCamerasAssigned -> {
                // Pozitii oficiale de la server - inlocuiesc orice fallback local generat,
                // ca toti jucatorii sa vada exact aceleasi 4 camere in runda asta.
                surveillanceCameraSpots.clear()
                event.spots.forEach { spot ->
                    surveillanceCameraSpots.add(SurveillanceCameraSpot(spot.roomId, spot.x, spot.y))
                }
            }
            is ServerEvent.PlayerMoved -> {
                val state = _gameState.value ?: return
                val player = state.players.find { it.id == event.playerId }
                player?.currentRoomId = event.targetRoomId
                val existing = playerLivePositions[event.playerId]
                if (existing != null) {
                    playerLivePositions[event.playerId] = existing.copy(roomId = event.targetRoomId)
                }
            }
            is ServerEvent.PlayerDisconnected -> {
                playerLivePositions.remove(event.playerId)
            }
            is ServerEvent.SurveillanceEvent -> {
                val evt = SurveillanceEvent(
                    id = "evt_remote_${Random.nextLong()}",
                    type = SurveillanceEventType.SPY_SENDING_INTEL,
                    timestampMillis = System.currentTimeMillis(),
                    relatedRoomId = event.fromRoomId
                )
                _activeSurveillanceEvent.value = evt
                viewModelScope.launch {
                    delay(4000L)
                    if (_activeSurveillanceEvent.value?.id == evt.id) {
                        _activeSurveillanceEvent.value = null
                    }
                }
            }
            is ServerEvent.Error -> {
                _errorMessage.value = event.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkClient?.disconnect()
    }

    private fun generatePlayerId(): String {
        return "player_${Random.nextLong()}"
    }
}