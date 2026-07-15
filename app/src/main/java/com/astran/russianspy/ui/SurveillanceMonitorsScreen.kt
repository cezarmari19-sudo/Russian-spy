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

/** Cele 4 camere supravegheate. Schimba aici daca vrei alte camere pe monitoare. */
private val MONITORED_ROOMS = listOf(
    "entrance" to "Intrare",
    "hub_central" to "Hol Central",
    "break_room" to "Camera Pauza",
    "server_room" to "Camera Servere"
)

/** Cat de mult din harta (in unitati world) se vede in jurul camerei supravegheate. */
private const val MONITOR_VIEW_SIZE = 900f

@Composable
fun SurveillanceMonitorsScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val playerPositions = viewModel.playerPositions

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
                        playerPositions = playerPositions,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[1].first,
                        playerPositions = playerPositions,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[2].first,
                        playerPositions = playerPositions,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        roomId = MONITORED_ROOMS[3].first,
                        playerPositions = playerPositions,
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
    playerPositions: Map<String, String>,
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

        // Punctele jucatorilor aflati in camera supravegheata (nu tot personajul, doar prezenta)
        val playersInRoom = playerPositions.filter { it.value == roomId }.keys.toList()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = minOf(size.width, size.height) / MONITOR_VIEW_SIZE

            fun worldToScreen(wx: Float, wy: Float): Offset {
                val dx = (wx - centerX) * scale
                val dy = (wy - centerY) * scale
                return Offset(size.width / 2f + dx, size.height / 2f + dy)
            }

            // Desenam toate camerele/holurile care intra in fereastra vizibila a acestui monitor.
            BuildingLayout.rooms.forEach { r ->
                val rCenterDist = kotlin.math.hypot(r.centerX() - centerX, r.centerY() - centerY)
                if (rCenterDist > MONITOR_VIEW_SIZE) return@forEach

                val topLeft = worldToScreen(r.x, r.y)
                val sizePx = Size(r.width * scale, r.height * scale)

                val isMonitoredRoom = r.id == roomId
                drawRect(
                    color = monitorRoomColor(r, isMonitoredRoom),
                    topLeft = topLeft,
                    size = sizePx
                )
                drawRect(
                    color = Color(0xFF000000),
                    topLeft = topLeft,
                    size = sizePx,
                    style = Stroke(width = 2f)
                )
            }

            // Punctele jucatorilor, desenate deasupra hartii.
            playersInRoom.forEach { _ ->
                val px = centerX + (Math.random().toFloat() - 0.5f) * (room.width * 0.5f)
                val py = centerY + (Math.random().toFloat() - 0.5f) * (room.height * 0.5f)
                val screenPos = worldToScreen(px, py)
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

        // Scanlines cosmetice, stil CCTV.
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
    // Camera efectiv supravegheata iese usor in evidenta fata de restul hartii vizibile.
    return if (isMonitoredRoom) base else base.copy(alpha = 0.55f)
}