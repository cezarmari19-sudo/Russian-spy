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
import com.astran.russianspy.network.LobbyPlayerInfo
import com.astran.russianspy.network.NetworkClient
import com.astran.russianspy.network.ServerEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _gameState = mutableStateOf<GameState?>(null)
    val gameState: State<GameState?> = _gameState

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

    // Lista de jucatori din lobby-ul de asteptare (inainte sa inceapa jocul)
    val lobbyPlayers = mutableStateListOf<LobbyPlayerInfo>()

    // Pozitiile LIVE ale tuturor jucatorilor: playerId -> roomId. Folosit de monitoarele de supraveghere.
    val playerPositions = mutableStateMapOf<String, String>()

    // Numele jucatorilor cunoscute (playerId -> name), pentru afisare pe monitoare
    val playerNames = mutableStateMapOf<String, String>()

    private val _gameStarted = mutableStateOf(false)
    val gameStarted: State<Boolean> = _gameStarted

    private val _myRole = mutableStateOf<Role?>(null)
    val myRole: State<Role?> = _myRole

    private var networkClient: NetworkClient? = null

    fun createRoom(playerName: String) {
        val playerId = generatePlayerId()
        _localPlayerId.value = playerId
        _localPlayerName.value = playerName
        _errorMessage.value = null

        val client = NetworkClient(onEvent = ::handleServerEvent)
        networkClient = client

        client.createRoom(playerId, playerName) { roomCode, error ->
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
            playerPositions[playerId] = "entrance"
            playerNames[playerId] = playerName
            client.connectWebSocket(roomCode, playerId)
        }
    }

    fun joinRoom(playerName: String, roomCode: String) {
        val playerId = generatePlayerId()
        _localPlayerId.value = playerId
        _localPlayerName.value = playerName
        _errorMessage.value = null

        val client = NetworkClient(onEvent = ::handleServerEvent)
        networkClient = client

        client.joinRoom(playerId, playerName, roomCode) { success, error ->
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
            playerPositions[playerId] = "entrance"
            playerNames[playerId] = playerName
            client.connectWebSocket(roomCode, playerId)
        }
    }

    /** Apelat de host cand apasa butonul Start din lobby-ul de asteptare. */
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
        playerPositions[_localPlayerId.value] = roomId
        networkClient?.sendMove(roomId)
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
            is ServerEvent.LobbyUpdate -> {
                lobbyPlayers.clear()
                lobbyPlayers.addAll(event.players)
                event.players.forEach { playerNames[it.id] = it.name }
            }
            is ServerEvent.PositionsSnapshot -> {
                playerPositions.putAll(event.positions)
            }
            is ServerEvent.GameStarted -> {
                _myRole.value = if (event.yourRole == "RUSSIAN_SPY") Role.RUSSIAN_SPY else Role.FBI_AGENT
                _gameState.value?.phase = GamePhase.IN_PROGRESS
                _gameStarted.value = true
            }
            is ServerEvent.PlayerMoved -> {
                val state = _gameState.value ?: return
                val player = state.players.find { it.id == event.playerId }
                player?.currentRoomId = event.targetRoomId
                playerPositions[event.playerId] = event.targetRoomId
            }
            is ServerEvent.PlayerDisconnected -> {
                playerPositions.remove(event.playerId)
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