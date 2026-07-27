package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.astran.russianspy.network.DnaSampleInfo
import com.astran.russianspy.viewmodel.GameViewModel

/**
 * Ecranul Laboratorului Criminalistic: aici se face compararea propriu-zisa.
 * Jucatorul aduce mai intai mostre (recoltate din Morga sau de referinta din
 * Arhiva ADN) folosind butonul "La laborator" din acele ecrane - ele apar apoi
 * in lista "Mostre disponibile" de aici, gata sa fie puse in cele doua
 * sloturi ale masinii (recoltat vs. referinta). Rezultatul comparatiei e
 * vizibil STRICT pentru cel care apasa butonul de comparare.
 */
@Composable
fun ForensicsLabScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val samplesInLab = viewModel.dnaSamples.filter { it.roomId == "forensics" }
    val harvestedAvailable = samplesInLab.filter { !it.isReference && !it.placedInLabSlot }
    val referenceAvailable = samplesInLab.filter { it.isReference && !it.placedInLabSlot }

    val harvestedSlotId = viewModel.labMachineHarvestedSampleId.value
    val referenceSlotId = viewModel.labMachineReferenceSampleId.value
    val harvestedSlotSample = viewModel.dnaSamples.firstOrNull { it.id == harvestedSlotId }
    val referenceSlotSample = viewModel.dnaSamples.firstOrNull { it.id == referenceSlotId }

    val result = viewModel.dnaComparisonResult.value

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF061019)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("🔬 Laborator Criminalistic", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Text("←", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F2636),
                    titleContentColor = Color.White
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Masina de comparare ADN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LabSlot(
                        label = "Mostra recoltata",
                        sample = harvestedSlotSample,
                        modifier = Modifier.weight(1f)
                    )
                    LabSlot(
                        label = "Mostra de referinta",
                        sample = referenceSlotSample,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = { viewModel.compareDnaSamples() },
                    enabled = harvestedSlotId != null && referenceSlotId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01579B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚗️ Compara probele")
                }

                HorizontalDivider(color = Color(0xFF1E3A4C))

                Text("Mostre recoltate disponibile", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                if (harvestedAvailable.isEmpty()) {
                    Text("Niciuna adusa in laborator inca.", color = Color(0xFF666666), fontSize = 12.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(harvestedAvailable) { sample ->
                            AvailableSampleChip(sample) {
                                viewModel.placeSampleInLabMachine(sample.id)
                            }
                        }
                    }
                }

                Text("Mostre de referinta disponibile", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                if (referenceAvailable.isEmpty()) {
                    Text("Niciuna adusa in laborator inca.", color = Color(0xFF666666), fontSize = 12.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(referenceAvailable) { sample ->
                            AvailableSampleChip(sample) {
                                viewModel.placeSampleInLabMachine(sample.id)
                            }
                        }
                    }
                }
            }
        }

        if (result != null) {
            DnaComparisonResultDialog(
                result = result,
                onDismiss = { viewModel.acknowledgeDnaComparisonResult() }
            )
        }
    }
}

@Composable
private fun LabSlot(label: String, sample: DnaSampleInfo?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF102636))
            .border(1.dp, Color(0xFF1E4A63), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF7FB3D5), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (sample == null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2A34))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Gol", color = Color(0xFF555555), fontSize = 11.sp)
        } else {
            val color = try {
                Color(android.graphics.Color.parseColor(sample.playerColor))
            } catch (e: Exception) {
                Color(0xFF9E9E9E)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (sample.isReference) color else Color(0xFFB0BEC5))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (sample.isReference) "Referinta" else "${sample.completeness}% integru",
                color = Color(0xFFCCCCCC),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AvailableSampleChip(sample: DnaSampleInfo, onClick: () -> Unit) {
    val color = try {
        Color(android.graphics.Color.parseColor(sample.playerColor))
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF102636))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (sample.isReference) color else Color(0xFFB0BEC5))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (sample.isReference) "Referinta" else "${sample.completeness}%",
            color = Color(0xFFAAAAAA),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01579B)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Pune in masina", fontSize = 10.sp)
        }
    }
}

@Composable
private fun DnaComparisonResultDialog(
    result: com.astran.russianspy.network.DnaComparisonResultInfo,
    onDismiss: () -> Unit
) {
    val refColor = try {
        Color(android.graphics.Color.parseColor(result.referencePlayerColor))
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rezultatul compararii") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(refColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mostra de referinta comparata")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Similaritate: ${result.similarityPercent}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (result.isMatch) "Rezultat: potrivire fiabila." else "Rezultat: potrivire nesigura sau proba compromisa.",
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Am inteles") }
        }
    )
}