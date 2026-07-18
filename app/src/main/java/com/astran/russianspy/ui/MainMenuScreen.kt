package com.astran.russianspy.ui

import androidx.compose.foundation.background
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

/**
 * Ecranul principal, primul care apare la pornirea aplicatiei (stil Among Us):
 * o bara subtire pe partea STANGA (nu un sfert din latime cum ar suna literal -
 * un sfert ar fi prea lat si ar concura vizual cu butoanele mari; aici e o bara
 * ingusta, fixa, cu iconite/text vertical) continand butoanele de SETARI si
 * PRIETENI, iar restul ecranului (dreapta) contine titlul jocului si cele 2
 * actiuni principale: START (-> gaseste lobby, introduci un cod) si CREEAZA LOBBY
 * (-> creezi o camera noua, primesti un cod).
 */
@Composable
fun MainMenuScreen(
    onStartClick: () -> Unit,
    onCreateLobbyClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFriendsClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D0F12)) {
        Row(modifier = Modifier.fillMaxSize()) {

            // --- Bara subtire din stanga: Setari + Prieteni ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(72.dp)
                    .background(Color(0xFF15181C)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                SidebarIconButton(
                    label = "Setari",
                    icon = "⚙",
                    onClick = onSettingsClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                SidebarIconButton(
                    label = "Prieteni",
                    icon = "👥",
                    onClick = onFriendsClick
                )
            }

            // --- Zona principala: titlu + actiuni ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "RUSSIAN SPY",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(64.dp))

                Button(
                    onClick = onStartClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                ) {
                    Text("START", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCreateLobbyClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E36)),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                ) {
                    Text("CREEAZA LOBBY", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SidebarIconButton(label: String, icon: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1F232A))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, fontSize = 9.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}