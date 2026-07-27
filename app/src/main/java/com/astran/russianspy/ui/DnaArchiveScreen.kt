package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.viewmodel.GameViewModel

/**
 * Ecranul Arhivei de ADN: contine automat o mostra de referinta per jucator
 * din partida curenta, la 100% completeness, NEDISTRUCTIBILA (arhiva e
 * sterila). Fiecare mostra e afisata DOAR cu culoarea jucatorului asociat -
 * numele/rolul raman ascunse pana la o comparare in laborator. Oricine poate
 * trimite o mostra de referinta spre Laboratorul Criminalistic (mostra ramane
 * disponibila si aici, nu se "consuma").
 */
@Composable
fun DnaArchiveScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val referenceSamples = viewModel.dnaSamples.filter { it.isReference && it.roomId == "dna_archive" }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF06120F)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("🧬 Arhiva ADN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Text("←", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF082922),
                    titleContentColor = Color.White
                )
            )

            Text(
                "Fiecare mostra reprezinta ADN-ul de referinta al unui jucator din partida - identificat doar prin culoare.",
                color = Color(0xFF9FCFC2),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (referenceSamples.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Arhiva se incarca...", color = Color(0xFF888888), fontSize = 15.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(referenceSamples) { sample ->
                        DnaSlotCard(
                            colorHex = sample.playerColor,
                            onSendToLab = { viewModel.moveDnaSampleToLab(sample.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnaSlotCard(colorHex: String, onSendToLab: () -> Unit) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF102420))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("100% integru", color = Color(0xFF9FCFC2), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSendToLab,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("La laborator", fontSize = 11.sp)
        }
    }
}