package com.astran.russianspy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.GameState
import com.astran.russianspy.model.Player
import com.astran.russianspy.model.SurveillanceEvent
import com.astran.russianspy.model.SurveillanceEventType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _gameState = mutableStateOf<GameState?>(null)
    val gameState: State<GameState?> = _gameState

    private val _localPlayerId = mutableStateOf("")
    val localPlayerId: State<String> = _localPlayerId

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _activeSurveillanceEvent = mutableStateOf<SurveillanceEvent?>(null)
    val activeSurveillanceEvent: State<SurveillanceEvent?> = _activeSurveillanceEvent

    private val _currentRoomId = mutableStateOf("entrance")
    val currentRoomId: State<String> = _currentRoomId

    fun createRoom(playerName: String) {
        val roomCode = generateRoomCode()
        val playerId = generatePlayerId()

        val hostPlayer = Player(id = playerId, name = playerName, currentRoomId = "entrance")

        _gameState.value = GameState(
            roomCode = roomCode,
            players = mutableListOf(hostPlayer),
            rooms = BuildingLayout.rooms.toMutableList()
        )
        _localPlayerId.value = playerId
        _currentRoomId.value = "entrance"
        _errorMessage.value = null
    }

    fun joinRoom(playerName: String, roomCode: String) {
        _errorMessage.value = "Functia de intrare in camera existenta necesita server (nu e conectat inca)"
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun moveToRoom(roomId: String) {
        _currentRoomId.value = roomId
        val state = _gameState.value ?: return
        val player = state.players.find { it.id == _localPlayerId.value } ?: return
        player.currentRoomId = roomId
    }

    fun spySendsIntel(fromRoomId: String) {
        val event = SurveillanceEvent(
            id = "evt_${Random.nextLong()}",
            type = SurveillanceEventType.SPY_SENDING_INTEL,
            timestampMillis = System.currentTimeMillis(),
            relatedRoomId = fromRoomId
        )
        _activeSurveillanceEvent.value = event

        viewModelScope.launch {
            delay(4000L)
            if (_activeSurveillanceEvent.value?.id == event.id) {
                _activeSurveillanceEvent.value = null
            }
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun generatePlayerId(): String {
        return "player_${Random.nextLong()}"
    }
}
