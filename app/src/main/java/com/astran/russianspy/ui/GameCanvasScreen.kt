package com.astran.russianspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import kotlin.math.sqrt

// cati pixeli pe ecran reprezinta 1 unitate din lumea jocului
// numar mai mare = zoom mai apropiat = harta pare mai mare
private const val TILE_SCALE = 0.6f

@Composable
fun GameCanvasScreen(
    onEnterTask: (Room) -> Unit
) {
    var playerX by remember { mutableStateOf(BuildingLayout.START_X) }
    var playerY by remember { mutableStateOf(BuildingLayout.START_Y) }

    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }

    val playerSpeed = 5f
    val playerRadius = 18f

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
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
            // camera centrata pe jucator: jucatorul e mereu in mijlocul ecranului,
            // iar harta se deseneaza relativ la pozitia lui
            val screenCenterX = size.width / 2f
            val screenCenterY = size.height / 2f

            fun worldToScreen(wx: Float, wy: Float): Offset {
                val dx = (wx - playerX) * TILE_SCALE
                val dy = (wy - playerY) * TILE_SCALE
                return Offset(screenCenterX + dx, screenCenterY + dy)
            }

            // deseneaza doar camerele care ar putea fi vizibile pe ecran (optimizare simpla)
            val viewRangeWorld = maxOf(size.width, size.height) / TILE_SCALE
            BuildingLayout.rooms.forEach { room ->
                val roomCenterDist = kotlin.math.hypot(room.centerX() - playerX, room.centerY() - playerY)
                if (roomCenterDist > viewRangeWorld) return@forEach

                val topLeft = worldToScreen(room.x, room.y)
                val sizePx = Size(room.width * TILE_SCALE, room.height * TILE_SCALE)

                drawRect(color = roomColor(room), topLeft = topLeft, size = sizePx)
                drawRect(color = Color(0xFF000000), topLeft = topLeft, size = sizePx, style = Stroke(width = 3f))
            }

            // jucatorul e mereu desenat exact in centrul ecranului
            drawCircle(
                color = Color(0xFFFFD700),
                radius = playerRadius * TILE_SCALE,
                center = Offset(screenCenterX, screenCenterY)
            )
            drawCircle(
                color = Color(0xFF000000),
                radius = playerRadius * TILE_SCALE,
                center = Offset(screenCenterX, screenCenterY),
                style = Stroke(width = 2f)
            )
        }

        Text(
            text = currentRoomName(playerX, playerY),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

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
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = Color(0xCCFFFFFF),
                radius = 35f,
                center = Offset(
                    size.width / 2f + knobOffset.x,
                    size.height / 2f + knobOffset.y
                )
            )
        }
    }
}