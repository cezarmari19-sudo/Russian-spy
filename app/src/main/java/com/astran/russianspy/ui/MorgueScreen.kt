package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.astran.russianspy.model.Role
import com.astran.russianspy.network.CorpseInfo
import com.astran.russianspy.viewmodel.GameViewModel

/**
 * Ecranul Morgii: aici sunt mutate automat corpurile dupa ce sunt raportate,
 * si raman vizibile pana li se extrage ADN-ul. Oricine (spion sau agent FBI)
 * poate extrage ADN-ul unui corp o singura data; SPIONUL, in plus, poate
 * strica proba (reduce completeness la 0-30%) inainte ca cineva sa apuce sa o
 * extraga - actiune discreta, fara indiciu vizual pentru ceilalti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorgueScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    // Corpurile inca "de lucru" in morga - odata ce ADN-ul le-a fost extras,
    // dispar din aceasta lista (nu mai e nimic de facut pe ele aici; mostra
    // recoltata rezultata se gaseste/gestioneaza din Laborator).
    val corpses = viewModel.corpses.filter { it.inMorgue && !it.dnaExtracted }
    val myRole = viewModel.myRole.value

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0D0F)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("⚰️ Morga", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Text("←", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF12161A),
                    titleContentColor = Color.White
                )
            )

            if (corpses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nu sunt corpuri in morga momentan.",
                        color = Color(0xFF888888),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(corpses) { corpse ->
                        CorpseCard(
                            corpse = corpse,
                            canTamper = myRole == Role.RUSSIAN_SPY && !corpse.dnaExtracted,
                            onTamper = { viewModel.tamperCorpseDna(corpse.id) },
                            onExtract = { viewModel.extractCorpseDna(corpse.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CorpseCard(
    corpse: CorpseInfo,
    canTamper: Boolean,
    onTamper: () -> Unit,
    onExtract: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2024))
            .padding(16.dp)
    ) {
        Text(
            "Corp neidentificat",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (corpse.dnaExtracted) {
            Text(
                "ADN extras - completitudine ${corpse.dnaCompleteness ?: 0}%",
                color = dnaQualityColor(corpse.dnaCompleteness ?: 0),
                fontSize = 13.sp
            )
        } else {
            Text(
                "ADN inca prezent pe corp - poate fi recoltat.",
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!corpse.dnaExtracted) {
                Button(
                    onClick = onExtract,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) {
                    Text("🧪 Extrage ADN")
                }
            }
            // Butonul de stricare a probei - vizibil STRICT spionului si doar
            // cat timp ADN-ul inca nu a fost extras (dupa extractie, mostra
            // devine independenta si nu mai poate fi alterata).
            if (canTamper) {
                Button(
                    onClick = onTamper,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A0000))
                ) {
                    Text("🩸 Strica proba")
                }
            }
        }
    }
}

private fun dnaQualityColor(completeness: Int): Color = when {
    completeness >= 70 -> Color(0xFF4CAF50)
    completeness >= 31 -> Color(0xFFFFA726)
    else -> Color(0xFFE53935)
}
