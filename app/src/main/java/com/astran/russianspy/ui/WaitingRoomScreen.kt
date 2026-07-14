package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.astran.russianspy.viewmodel.GameViewModel

private const val MIN_PLAYERS = 4
private const val MAX_PLAYERS = 15

@Composable
fun WaitingRoomScreen(
    viewModel: GameViewModel,
    onGameStarted: () -> Unit
) {
    val gameState by viewModel.gameState
    val isHost by viewModel.isHost
    val lobbyPlayers = viewModel.lobbyPlayers
    val gameStarted by viewModel.gameStarted
    val errorMessage by viewModel.errorMessage

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(gameStarted) {
        if (gameStarted) {
            onGameStarted()
        }
    }

    val roomCode = gameState?.roomCode ?: ""
    val canStart = isHost && lobbyPlayers.size >= MIN_PLAYERS

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header cu codul camerei
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cod camera",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = roomCode,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                )
                Text(
                    text = "Spune-le celorlalti acest cod ca sa se alature",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contor jucatori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jucatori (${lobbyPlayers.size}/$MAX_PLAYERS)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isHost) {
                    IconButton(onClick = { showSettings = true }) {
                        Text("⚙")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de jucatori
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(lobbyPlayers) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = if (player.connected) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (player.id == viewModel.localPlayerId.value) {
                            Text(
                                text = "TU",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!player.connected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "deconectat",
                                fontSize = 11.sp,
                                color = Color(0xFF9E9E9E)
                            )
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Buton Start (doar host) sau mesaj de asteptare (jucatori normali)
            if (isHost) {
                Button(
                    onClick = { viewModel.startGame() },
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (canStart) "START" else "Minim $MIN_PLAYERS jucatori necesari",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Se asteapta ca gazda sa inceapa jocul...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSettings) {
        LobbySettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
private fun LobbySettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setari camera") },
        text = {
            Text("Setarile de joc (numar spioni, durata task-uri etc.) vor fi adaugate aici in curand.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide")
            }
        }
    )
}