package com.astran.russianspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun GameCanvasScreen(
    onEnterTask: (Room) -> Unit
) {
    // pozitia jucatorului in coordonate lume (aceeasi scara ca BuildingLayout)
    var playerX by remember { mutableStateOf(1000f) }
    var playerY by remember { mutableStateOf(1200f) }

    // directia curenta a joystick-ului (-1..1 pe fiecare axa)
    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }

    val playerSpeed = 6f
    val playerRadius = 20f

    // bucla de miscare: la fiecare frame, mutam jucatorul in directia joystick-ului
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanosCompat()
            if (joystickDirX != 0f || joystickDirY != 0f) {
                val newX = playerX + joystickDirX * playerSpeed
                val newY = playerY + joystickDirY * playerSpeed
                if (isWalkable(newX, playerY, playerRadius)) playerX = newX
                if (isWalkable(playerX, newY, playerRadius)) playerY = newY
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / BuildingLayout.MAP_WIDTH
            val scaleY = size.height / BuildingLayout.MAP_HEIGHT
            val scale = min(scaleX, scaleY)

            // centram harta in canvas
            val offsetX = (size.width - BuildingLayout.MAP_WIDTH * scale) / 2f
            val offsetY = (size.height - BuildingLayout.MAP_HEIGHT * scale) / 2f

            fun worldToScreen(wx: Float, wy: Float): Offset {
                return Offset(offsetX + wx * scale, offsetY + wy * scale)
            }

            // deseneaza toate camerele
            BuildingLayout.rooms.forEach { room ->
                val topLeft = worldToScreen(room.x, room.y)
                val color = roomColor(room)
                drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(room.width * scale, room.height * scale)
                )
                drawRect(
                    color = Color(0xFF000000),
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(room.width * scale, room.height * scale),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }

            // deseneaza jucatorul (doar daca e vizibil - fara linie de vedere inca, adaugam la pasul urmator)
            val playerScreen = worldToScreen(playerX, playerY)
            drawCircle(
                color = Color(0xFFFFD700),
                radius = playerRadius * scale,
                center = playerScreen
            )
        }

        // numele camerelor, afisate ca text peste canvas (simplu, fara scalare complexa inca)
        Text(
            text = currentRoomName(playerX, playerY),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // Joystick virtual, jos-stanga
        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            onDirectionChanged = { dx, dy ->
                joystickDirX = dx
                joystickDirY = dy
            }
        )
    }
}

private fun roomColor(room: Room): Color {
    return when (room.function) {
        RoomFunction.HALLWAY -> Color(0xFF2A2A2A)
        RoomFunction.HUB -> Color(0xFF3A3A3A)
        RoomFunction.ENTRANCE -> Color(0xFF455A64)
        RoomFunction.SURVEILLANCE -> Color(0xFF4A148C)
        RoomFunction.FORENSICS_LAB -> Color(0xFF01579B)
        RoomFunction.ARMORY -> Color(0xFF880E4F)
        RoomFunction.SERVER_ROOM -> Color(0xFF1B5E20)
        RoomFunction.OFFICE -> Color(0xFF37474F)
        RoomFunction.BREAK_ROOM -> Color(0xFF5D4037)
        RoomFunction.COMMS_MONITOR -> Color(0xFF827717)
        RoomFunction.MEETING_ROOM -> Color(0xFFB71C1C)
    }
}

private fun currentRoomName(px: Float, py: Float): String {
    val room = BuildingLayout.getRoomAtPoint(px, py)
    return room?.name?.takeIf { it.isNotBlank() } ?: ""
}

// verifica daca punctul e in interiorul vreunei camere/coridor (nu poti iesi din cladire)
private fun isWalkable(px: Float, py: Float, radius: Float): Boolean {
    if (px - radius < 0f || px + radius > BuildingLayout.MAP_WIDTH) return false
    if (py - radius < 0f || py + radius > BuildingLayout.MAP_HEIGHT) return false
    return BuildingLayout.rooms.any { it.containsPoint(px, py) }
}

@Composable
private fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onDirectionChanged: (Float, Float) -> Unit
) {
    val baseRadius = 70f
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(140.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = knobOffset + dragAmount
                        val distance = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        knobOffset = if (distance > baseRadius) {
                            Offset(newOffset.x / distance * baseRadius, newOffset.y / distance * baseRadius)
                        } else {
                            newOffset
                        }
                        onDirectionChanged(knobOffset.x / baseRadius, knobOffset.y / baseRadius)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onDirectionChanged(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x55FFFFFF),
                radius = baseRadius,
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = Color(0xCCFFFFFF),
                radius = 35f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width / 2f + knobOffset.x,
                    size.height / 2f + knobOffset.y
                )
            )
        }
    }
}

private suspend fun withFrameNanosCompat() {
    androidx.compose.runtime.withFrameNanos { }
}