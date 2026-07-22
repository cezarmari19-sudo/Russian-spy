package com.astran.russianspy.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.network.NetworkClient
import com.astran.russianspy.network.PublicRoomInfo
import com.astran.russianspy.ui.theme.SectionLabel
import com.astran.russianspy.ui.theme.TacticalBackground
import com.astran.russianspy.ui.theme.TacticalButton
import com.astran.russianspy.ui.theme.TacticalCard
import com.astran.russianspy.ui.theme.TacticalColors
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

    TacticalBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel(text = "Camere active")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LOBBY-URI PUBLICE",
                        color = TacticalColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onBack) {
                    Text("Inapoi", color = TacticalColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TacticalButton(
                text = if (isLoading) "SE ACTUALIZEAZA..." else "REFRESH",
                onClick = { refresh() },
                enabled = !isLoading,
                isPrimary = false,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { msg ->
                Text(text = msg, color = TacticalColors.Danger)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isLoading && rooms.isEmpty() && errorMessage == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Niciun lobby disponibil acum.\nApasa REFRESH sau creeaza unul nou.",
                        color = TacticalColors.TextMuted,
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
    TacticalCard(modifier = Modifier.fillMaxWidth(), accentLeft = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CAMERA ${room.roomCode}",
                    color = TacticalColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${room.playerCount}/${room.maxPlayers} jucatori",
                    color = TacticalColors.TextSecondary,
                    fontSize = 13.sp
                )
            }

            TacticalButton(
                text = if (isJoining) "..." else "INTRA",
                onClick = onJoin,
                enabled = !isJoining,
                isPrimary = true,
                height = 40.dp,
                modifier = Modifier.width(96.dp)
            )
        }
    }
}