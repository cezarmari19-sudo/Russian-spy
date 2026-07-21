package com.astran.russianspy.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.viewmodel.GameViewModel

/**
 * Ecranul "Gaseste Lobby": jucatorul introduce codul unei camere existente si
 * intra in ea. Numele jucatorului vine automat din PlayerPrefs (nu se mai cere
 * aici) - daca nu exista inca un nume salvat, redirectioneaza catre Setari.
 */
@Composable
fun FindLobbyScreen(
    viewModel: GameViewModel,
    onRoomReady: () -> Unit,
    onBack: () -> Unit,
    onNeedsName: () -> Unit
) {
    val context: Context = LocalContext.current
    var roomCode by remember { mutableStateOf("") }

    val gameState by viewModel.gameState
    val errorMessage by viewModel.errorMessage

    LaunchedEffect(Unit) {
        if (!PlayerPrefs.hasPlayerName(context)) {
            onNeedsName()
        }
    }

    LaunchedEffect(gameState) {
        if (gameState != null) {
            onRoomReady()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GASESTE LOBBY",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

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
                onClick = {
                    val playerName = PlayerPrefs.getPlayerName(context)
                    val accountId = PlayerPrefs.getAccountId(context)
                    viewModel.joinRoom(playerName, roomCode, accountId)
                },
                enabled = roomCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Intra in camera")
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onBack) {
                Text("Inapoi")
            }
        }
    }
}