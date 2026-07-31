package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.astran.russianspy.viewmodel.GameViewModel

/** Cifrul Morse complet A-Z, folosit doar pentru afisare (foaia de langa
 * task) - jucatorul trebuie sa-l foloseasca pentru a compune manual
 * secventa pentru S-O-S folosind cele 2 butoane cu functie ascunsa. */
private val MORSE_ALPHABET = listOf(
    "A" to ".-", "B" to "-...", "C" to "-.-.", "D" to "-..", "E" to ".",
    "F" to "..-.", "G" to "--.", "H" to "....", "I" to "..", "J" to ".---",
    "K" to "-.-", "L" to ".-..", "M" to "--", "N" to "-.", "O" to "---",
    "P" to ".--.", "Q" to "--.-", "R" to ".-.", "S" to "...", "T" to "-",
    "U" to "..-", "V" to "...-", "W" to ".--", "X" to "-..-", "Y" to "-.--",
    "Z" to "--.."
)

/**
 * Ecranul de Comunicatii (task-ul mare din camera COMMS_MONITOR): jucatorul
 * are alaturi cifrul Morse complet A-Z si trebuie sa compuna manual secventa
 * pentru SOS (... --- ...) folosind 2 butoane fixe (albastru stanga, rosu
 * dreapta) a caror functie (punct/linie) e RANDOMIZATA de server la fiecare
 * runda - clientul NU stie care e care, trebuie ghicit/testat, iar rezultatul
 * (corect/gresit) apare doar dupa ce apasa "Trimite secventa". O incercare
 * gresita reseteaza secventa curenta si se poate incerca din nou.
 */
@Composable
fun CommunicationsScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    var sequence by remember { mutableStateOf(listOf<String>()) }
    val sosResult by viewModel.sosResult

    LaunchedEffect(sosResult) {
        if (sosResult != null) {
            // Dupa afisarea rezultatului, resetam secventa curenta - fie a
            // fost corect (nu mai are rost sa continue), fie gresit (trebuie
            // sa ia de la capat, eventual cu alta ghicire a butoanelor).
            sequence = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit) {
                    Text("⬅", fontSize = 20.sp, color = Color.White)
                }
                Text(
                    "📡 Comunicatii de urgenta",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Compune semnalul SOS folosind alfabetul Morse de mai jos. " +
                    "Nu se stie care buton e PUNCT si care e LINIE - testeaza " +
                    "cu grija inainte sa trimiti.",
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Foaia cu cifrul Morse complet A-Z, mereu vizibila
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1D22))
                    .padding(12.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(MORSE_ALPHABET) { (letter, code) ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF262B33))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(letter, color = Color(0xFF3DDC5A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(code, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secventa curenta compusa, afisata cu simbolurile REALE
            // A/B apasate (nu punct/linie - jucatorul nu stie inca ce
            // reprezinta fiecare buton pana nu trimite si vede rezultatul).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1D22))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (sequence.isEmpty()) "— secventa goala —" else sequence.joinToString(" "),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            sosResult?.let { correct ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (correct) "✅ Semnal trimis corect!" else "❌ Secventa gresita - incearca din nou.",
                    color = if (correct) Color(0xFF3DDC5A) else Color(0xFFE53935),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { sequence = sequence + "A" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Text("Buton A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { sequence = sequence + "B" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Text("Buton B", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { sequence = emptyList() },
                    modifier = Modifier.weight(1f),
                    enabled = sequence.isNotEmpty()
                ) {
                    Text("Sterge tot", color = Color(0xFFB0B0B0))
                }
                Button(
                    onClick = {
                        viewModel.acknowledgeSosResult()
                        viewModel.attemptSendSos(sequence)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC5A)),
                    modifier = Modifier.weight(1f),
                    enabled = sequence.isNotEmpty()
                ) {
                    Text("Trimite secventa", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
