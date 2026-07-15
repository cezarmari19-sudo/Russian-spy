package com.astran.russianspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.viewmodel.GameViewModel
import com.astran.russianspy.viewmodel.LivePosition

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
    val playerLivePositions = viewModel.playerLivePositions

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
                        playerLivePositions = playerLivePositions,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[1].first,
                        playerLivePositions = playerLivePositions,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[2].first,
                        playerLivePositions = playerLivePositions,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[3].first,
                        playerLivePositions = playerLivePositions,
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
    playerLivePositions: Map<String, LivePosition>,
    modifier: Modifier = Modifier
) {
    val room = remember(roomId) { BuildingLayout.getRoomById(roomId) }

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .border(width = 2.dp, color = Color(0xFF2A2A2A))
    ) {
        if (room == null) return@Box

        val centerX = room.centerX()
        val centerY = room.centerY()

        // Doar jucatorii aflati EFECTIV in camera supravegheata, cu pozitia lor exacta X/Y.
        val playersHere = playerLivePositions.values.filter { it.roomId == roomId }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Camera monitorizata umple TOT ecranul monitorului. Nu se mai deseneaza
            // nicio camera/hol vecin - stil Among Us: fiecare monitor = un singur cadru curat.
            val scale = minOf(size.width / room.width, size.height / room.height)

            fun worldToScreen(wx: Float, wy: Float): Offset {
                val dx = (wx - centerX) * scale
                val dy = (wy - centerY) * scale
                return Offset(size.width / 2f + dx, size.height / 2f + dy)
            }

            // Podeaua camerei, umplind tot cadrul.
            drawRect(
                color = monitorRoomColor(room, true),
                topLeft = Offset.Zero,
                size = size
            )

            // Pozitia EXACTA a fiecarui jucator, live.
            playersHere.forEach { pos ->
                val screenPos = worldToScreen(pos.x, pos.y)
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 8f,
                    center = screenPos
                )
                drawCircle(
                    color = Color.Black,
                    radius = 8f,
                    center = screenPos,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x0A00FF00))
        )
    }
}

private fun monitorRoomColor(room: Room, isMonitoredRoom: Boolean): Color {
    val base = when (room.function) {
        RoomFunction.HALLWAY -> Color(0xFF1A1A1A)
        RoomFunction.HUB -> Color(0xFF242424)
        RoomFunction.ENTRANCE -> Color(0xFF2C3A3F)
        RoomFunction.SURVEILLANCE -> Color(0xFF2E0E4F)
        RoomFunction.FORENSICS_LAB -> Color(0xFF00304A)
        RoomFunction.ARMORY -> Color(0xFF4A0A2A)
        RoomFunction.SERVER_ROOM -> Color(0xFF0F3A12)
        RoomFunction.OFFICE -> Color(0xFF20292D)
        RoomFunction.BREAK_ROOM -> Color(0xFF3A2620)
        RoomFunction.COMMS_MONITOR -> Color(0xFF4A420F)
        RoomFunction.MEETING_ROOM -> Color(0xFF5A0F0F)
    }
    return if (isMonitoredRoom) base else base.copy(alpha = 0.55f)
}