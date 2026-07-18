package com.astran.russianspy.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs

/**
 * Ecran de Setari cont: momentan doar numele jucatorului, salvat local pe telefon
 * (PlayerPrefs / SharedPreferences), ca sa nu mai trebuiasca retastat de fiecare
 * data cand se deschide aplicatia sau se creeaza/se intra intr-o camera.
 */
@Composable
fun SettingsScreen(
    onDone: () -> Unit
) {
    val context: Context = LocalContext.current
    var nameInput by remember { mutableStateOf(PlayerPrefs.getPlayerName(context)) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SETARI CONT",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Numele tau") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    PlayerPrefs.setPlayerName(context, nameInput.trim())
                    onDone()
                },
                enabled = nameInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salveaza")
            }
        }
    }
}