package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.viewmodel.GameViewModel

/** Cele 4 camere supravegheate. Schimba aici daca vrei alte camere pe monitoare. */
private val MONITORED_ROOMS = listOf(
    "entrance" to "Intrare",
    "hub_central" to "Hol Central",
    "break_room" to "Camera Pauza",
    "server_room" to "Camera Servere"
)

@Composable
fun SurveillanceMonitorsScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val playerPositions = viewModel.playerPositions
    val playerNames = viewModel.playerNames

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
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
                Text(
                    text = "CAMERE DE SUPRAVEGHERE",
                    color = Color(0xFF4CAF50),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onExit) {
                    Text("Iesire", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grila 2x2, ca in Among Us
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[0].first,
                        roomLabel = MONITORED_ROOMS[0].second,
                        playerPositions = playerPositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[1].first,
                        roomLabel = MONITORED_ROOMS[1].second,
                        playerPositions = playerPositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[2].first,
                        roomLabel = MONITORED_ROOMS[2].second,
                        playerPositions = playerPositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[3].first,
                        roomLabel = MONITORED_ROOMS[3].second,
                        playerPositions = playerPositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorPanel(
    roomId: String,
    roomLabel: String,
    playerPositions: Map<String, String>,
    playerNames: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val playersHere = playerPositions.filter { it.value == roomId }.keys.toList()

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .border(width = 2.dp, color = Color(0xFF2A2A2A))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = roomLabel,
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // "scanline" simplu, static, doar cosmetic pentru aspect de camera CCTV
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF050505))
            ) {
                if (playersHere.isEmpty()) {
                    Text(
                        text = "Nimeni in camera",
                        color = Color(0xFF3A3A3A),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        playersHere.forEach { pid ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFFFD700))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = playerNames[pid] ?: "Jucator",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}