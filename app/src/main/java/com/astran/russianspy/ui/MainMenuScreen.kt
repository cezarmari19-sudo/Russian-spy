package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.ui.theme.SectionLabel
import com.astran.russianspy.ui.theme.TacticalBackground
import com.astran.russianspy.ui.theme.TacticalButton
import com.astran.russianspy.ui.theme.TacticalColors

/**
 * Ecranul principal, primul care apare la pornirea aplicatiei (stil Among Us):
 * o bara subtire pe partea STANGA cu iconite pentru SETARI, PRIETENI si
 * LOBBY-URI, iar restul ecranului (dreapta) contine titlul jocului si cele 2
 * actiuni principale: START (-> gaseste lobby, introduci un cod) si CREEAZA
 * LOBBY (-> creezi o camera noua, primesti un cod).
 */
@Composable
fun MainMenuScreen(
    onStartClick: () -> Unit,
    onCreateLobbyClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onLobbiesClick: () -> Unit
) {
    TacticalBackground {
        Row(modifier = Modifier.fillMaxSize()) {

            // --- Bara subtire din stanga: Setari + Prieteni + Lobby-uri ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(76.dp)
                    .background(TacticalColors.BackgroundLight)
                    .border(
                        width = 1.dp,
                        color = TacticalColors.Border
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                SidebarIconButton(label = "Setari", icon = "⚙", onClick = onSettingsClick)
                Spacer(modifier = Modifier.height(14.dp))
                SidebarIconButton(label = "Prieteni", icon = "👥", onClick = onFriendsClick)
                Spacer(modifier = Modifier.height(14.dp))
                SidebarIconButton(label = "Lobby-uri", icon = "📋", onClick = onLobbiesClick)
            }

            // --- Zona principala: titlu + actiuni ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SectionLabel(text = "Operatiune activa")

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RUSSIAN SPY",
                    color = TacticalColors.TextPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // Linie subtire de accent sub titlu - un singur detaliu de culoare,
                // nu un fundal colorat sau un gradient pe tot titlul.
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .width(64.dp)
                        .height(3.dp)
                        .background(TacticalColors.Accent)
                )

                Spacer(modifier = Modifier.height(56.dp))

                TacticalButton(
                    text = "START",
                    onClick = onStartClick,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                TacticalButton(
                    text = "CREEAZA LOBBY",
                    onClick = onCreateLobbyClick,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
            }
        }
    }
}

@Composable
private fun SidebarIconButton(label: String, icon: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TacticalColors.Surface)
            .border(width = 1.dp, color = TacticalColors.Border, shape = RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, fontSize = 9.sp, color = TacticalColors.TextSecondary)
            }
        }
    }
}