package com.astran.russianspy.ui

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.astran.russianspy.ui.theme.SectionLabel
import com.astran.russianspy.ui.theme.TacticalBackground
import com.astran.russianspy.ui.theme.TacticalButton
import com.astran.russianspy.ui.theme.TacticalColors
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

    TacticalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SectionLabel(text = "Alaturare camera existenta")

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "GASESTE LOBBY",
                color = TacticalColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it.uppercase() },
                label = { Text("Cod camera") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalColors.Accent,
                    unfocusedBorderColor = TacticalColors.Border,
                    focusedTextColor = TacticalColors.TextPrimary,
                    unfocusedTextColor = TacticalColors.TextPrimary,
                    cursorColor = TacticalColors.Accent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            TacticalButton(
                text = "INTRA IN CAMERA",
                onClick = {
                    val playerName = PlayerPrefs.getPlayerName(context)
                    val accountId = PlayerPrefs.getAccountId(context)
                    viewModel.joinRoom(playerName, roomCode, accountId)
                },
                enabled = roomCode.isNotBlank(),
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = msg, color = TacticalColors.Danger)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onBack) {
                Text("Inapoi", color = TacticalColors.TextSecondary)
            }
        }
    }
}