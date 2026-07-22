package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.network.AccountApi
import com.astran.russianspy.ui.theme.SectionLabel
import com.astran.russianspy.ui.theme.TacticalBackground
import com.astran.russianspy.ui.theme.TacticalButton
import com.astran.russianspy.ui.theme.TacticalCard
import com.astran.russianspy.ui.theme.TacticalColors
import com.astran.russianspy.viewmodel.GameViewModel

private const val MIN_PLAYERS = 1
private const val MAX_PLAYERS = 15

@Composable
fun WaitingRoomScreen(
    viewModel: GameViewModel,
    onGameStarted: () -> Unit,
    onLeaveLobby: () -> Unit
) {
    val gameState by viewModel.gameState
    val isHost by viewModel.isHost
    val lobbyPlayers = viewModel.lobbyPlayers
    val gameStarted by viewModel.gameStarted
    val errorMessage by viewModel.errorMessage

    var showSettings by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(gameStarted) {
        if (gameStarted) {
            onGameStarted()
        }
    }

    val roomCode = gameState?.roomCode ?: ""
    val canStart = isHost && lobbyPlayers.size >= MIN_PLAYERS

    TacticalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header cu buton de iesire + codul camerei
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {
                        viewModel.leaveLobby()
                        onLeaveLobby()
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("⬅", fontSize = 20.sp, color = TacticalColors.TextPrimary)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionLabel(text = "Cod camera")
                    Text(
                        text = roomCode,
                        color = TacticalColors.TextPrimary,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    )
                    Text(
                        text = "Spune-le celorlalti acest cod ca sa se alature",
                        fontSize = 13.sp,
                        color = TacticalColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contor jucatori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JUCATORI (${lobbyPlayers.size}/$MAX_PLAYERS)",
                    color = TacticalColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row {
                    IconButton(onClick = { showInviteDialog = true }) {
                        Text("👥")
                    }
                    if (isHost) {
                        IconButton(onClick = { showSettings = true }) {
                            Text("⚙")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de jucatori
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lobbyPlayers) { player ->
                    TacticalCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentLeft = player.id == viewModel.localPlayerId.value
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar simplu: cerc cu initiala, culoare derivata din nume -
                            // da personalitate fara sa adauge un accent nou de culoare
                            // in restul paletei (fiecare jucator are propria "insigna").
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorFor(player.name)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = player.name,
                                color = if (player.connected) TacticalColors.TextPrimary else TacticalColors.TextMuted,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (player.id == viewModel.localPlayerId.value) {
                                Text(
                                    text = "TU",
                                    fontSize = 11.sp,
                                    color = TacticalColors.Accent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (!player.connected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "deconectat",
                                    fontSize = 11.sp,
                                    color = TacticalColors.TextMuted
                                )
                            }
                        }
                    }
                }

                if (lobbyPlayers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Se conecteaza...",
                                color = TacticalColors.TextMuted
                            )
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = TacticalColors.Danger,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buton Start (doar host) sau mesaj de asteptare (jucatori normali)
            if (isHost) {
                TacticalButton(
                    text = if (canStart) "START" else "MINIM $MIN_PLAYERS JUCATORI NECESARI",
                    onClick = { viewModel.startGame() },
                    enabled = canStart,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Se asteapta ca gazda sa inceapa jocul...",
                        color = TacticalColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSettings) {
        LobbySettingsDialog(onDismiss = { showSettings = false })
    }

    if (showInviteDialog) {
        InviteFriendDialog(
            roomCode = roomCode,
            onDismiss = { showInviteDialog = false }
        )
    }
}

/** Culoare de avatar derivata deterministic din nume - fiecare jucator arata mereu la fel. */
private fun avatarColorFor(name: String): Color {
    val palette = listOf(
        Color(0xFF5C6BC0), Color(0xFF00897B), Color(0xFF8D6E63),
        Color(0xFF3949AB), Color(0xFF43A047), Color(0xFF546E7A),
        Color(0xFF6D4C41), Color(0xFF00ACC1)
    )
    val index = if (name.isEmpty()) 0 else name.sumOf { it.code } % palette.size
    return palette[index]
}

@Composable
private fun InviteFriendDialog(roomCode: String, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val accountId = remember { PlayerPrefs.getAccountId(context) }

    var friends by remember { mutableStateOf<List<com.astran.russianspy.network.AccountInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AccountApi.fetchFriendsData(accountId) { data, error ->
            isLoading = false
            if (data != null) {
                friends = data.friends
            } else {
                statusMessage = error ?: "Nu am putut incarca lista de prieteni"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Invita un prieten", color = TacticalColors.TextPrimary) },
        text = {
            Column {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TacticalColors.Accent)
                    }
                } else if (friends.isEmpty()) {
                    Text("Nu ai niciun prieten adaugat inca.", fontSize = 13.sp, color = TacticalColors.TextSecondary)
                } else {
                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(friend.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TacticalColors.TextPrimary)
                            TextButton(onClick = {
                                AccountApi.inviteToRoom(accountId, friend.accountId, roomCode) { success, error ->
                                    statusMessage = if (success) {
                                        "Invitatie trimisa catre ${friend.displayName}."
                                    } else {
                                        error ?: "Nu am putut trimite invitatia"
                                    }
                                }
                            }) {
                                Text("Invita", color = TacticalColors.Accent)
                            }
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, fontSize = 12.sp, color = TacticalColors.Success)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide", color = TacticalColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun LobbySettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Setari camera", color = TacticalColors.TextPrimary) },
        text = {
            Text(
                "Setarile de joc (numar spioni, durata task-uri etc.) vor fi adaugate aici in curand.",
                color = TacticalColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide", color = TacticalColors.Accent)
            }
        }
    )
}