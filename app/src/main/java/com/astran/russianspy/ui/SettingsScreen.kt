package com.astran.russianspy.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

    TacticalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SectionLabel(text = "Profil agent")

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "SETARI CONT",
                color = TacticalColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Numele tau") },
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
                text = "SALVEAZA",
                onClick = {
                    PlayerPrefs.setPlayerName(context, nameInput.trim())
                    onDone()
                },
                enabled = nameInput.isNotBlank(),
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}