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
 * Ecran "Creeaza Lobby": creeaza automat o camera noua, folosind numele salvat
 * in PlayerPrefs (nu se mai cere numele aici - vine din Setari). Daca nu exista
 * inca un nume salvat, redirectioneaza catre Setari inainte sa continue.
 */
@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onRoomReady: () -> Unit,
    onNeedsName: () -> Unit
) {
    val context: Context = LocalContext.current

    val gameState by viewModel.gameState
    val errorMessage by viewModel.errorMessage

    var hasStartedCreating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!PlayerPrefs.hasPlayerName(context)) {
            onNeedsName()
            return@LaunchedEffect
        }
        if (!hasStartedCreating) {
            hasStartedCreating = true
            val playerName = PlayerPrefs.getPlayerName(context)
            val accountId = PlayerPrefs.getAccountId(context)
            viewModel.createRoom(playerName, accountId)
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
                text = "SE CREEAZA CAMERA...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator()

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}