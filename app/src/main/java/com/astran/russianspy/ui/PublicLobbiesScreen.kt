package com.astran.russianspy.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.network.NetworkClient
import com.astran.russianspy.network.PublicRoomInfo
import com.astran.russianspy.network.ServerEvent
import com.astran.russianspy.viewmodel.GameViewModel

/**
 * Ecran "Lobby-uri publice", stil Among Us: lista de camere disponibile (cu loc
 * liber, publice), cu buton de REFRESH MANUAL (nu automat/periodic - vezi
 * server/main.py: /public_rooms e apelat doar la cerere explicita, ca sa nu
 * incarce serverul degeaba). Selectia primita de la server e deja random
 * (stil Among Us beta), asa ca aici doar afisam ce vine, fara sortare proprie.
 */
@Composable
fun PublicLobbiesScreen(
    viewModel: GameViewModel,
    onRoomReady: () -> Unit,
    onBack: () -> Unit,
    onNeedsName: () -> Unit
) {
    val context: Context = LocalContext.current

    // NetworkClient temporar, doar pentru apeluri HTTP simple (listare, join) -
    // nu are nevoie de conexiune WebSocket la o camera anume ca sa functioneze.
    val httpClient = remember { NetworkClient(onEvent = {}) }

    var rooms by remember { mutableStateOf<List<PublicRoomInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var joiningRoomCode by remember { mutableStateOf<String?>(null) }

    val gameState by viewModel.gameState

    fun refresh() {
        isLoading = true
        errorMessage = null
        httpClient.fetchPublicRooms { result, error ->
            isLoading = false
            if (result != null) {
                rooms = result
            } else {
                errorMessage = error ?: "Eroare necunoscuta"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!PlayerPrefs.hasPlayerName(context)) {
            onNeedsName()
            return@LaunchedEffect
        }
        refresh()
    }

    LaunchedEffect(gameState) {
        if (gameState != null && joiningRoomCode != null) {
            onRoomReady()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D0F12)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOBBY-URI PUBLICE",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onBack) {
                    Text("Inapoi", color = Color(0xFFAAAAAA))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { refresh() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E36)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("🔄 REFRESH")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            errorMessage?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isLoading && rooms.isEmpty() && errorMessage == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Niciun lobby disponibil acum.\nApasa REFRESH sau creeaza unul nou.",
                        color = Color(0xFF888888),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rooms) { room ->
                    LobbyRow(
                        room = room,
                        isJoining = joiningRoomCode == room.roomCode,
                        onJoin = {
                            joiningRoomCode = room.roomCode
                            val playerName = PlayerPrefs.getPlayerName(context)
                            viewModel.joinRoom(playerName, room.roomCode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LobbyRow(
    room: PublicRoomInfo,
    isJoining: Boolean,
    onJoin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1D22))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Camera ${room.roomCode}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${room.playerCount}/${room.maxPlayers} jucatori",
                color = Color(0xFF9AA0A6),
                fontSize = 13.sp
            )
        }

        Button(
            onClick = onJoin,
            enabled = !isJoining,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            if (isJoining) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Intra")
            }
        }
    }
}