package com.astran.russianspy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ecran Prieteni - PLACEHOLDER. Sistemul complet de prieteni (adaugare dupa cod
 * de utilizator, lista de prieteni, invitatii) e un task separat, mai mare, care
 * necesita si suport pe server (identificatori de cont persistenti, nu doar
 * playerId-uri generate random per sesiune ca acum). Deocamdata doar ecranul
 * exista, ca navigarea din meniul principal sa functioneze.
 */
@Composable
fun FriendsScreen(
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "PRIETENI", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "In curand.")
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onBack) {
                Text("Inapoi")
            }
        }
    }
}