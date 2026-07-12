package com.astran.russianspy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.viewmodel.GameViewModel

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onRoomReady: () -> Unit
) {
    var playerName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }

    val gameState by viewModel.gameState
    val errorMessage by viewModel.errorMessage

    LaunchedEffect(gameState) {
        if (gameState != null) {
            onRoomReady()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "RUSSIAN SPY",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Numele tau") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.createRoom(playerName) },
                enabled = playerName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Creeaza camera noua")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("SAU")

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it.uppercase() },
                label = { Text("Cod camera") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.joinRoom(playerName, roomCode) },
                enabled = playerName.isNotBlank() && roomCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Intra in camera")
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
