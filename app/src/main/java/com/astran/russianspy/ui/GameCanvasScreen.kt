package com.astran.russianspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TILE_SCALE = 2.2f
private const val JOYSTICK_BASE_RADIUS = 100f
private const val JOYSTICK_KNOB_RADIUS = 40f
// VIEW_RADIUS e definit in Visibility.kt (folosit si de camerele de supraveghere)

@Composable
fun GameCanvasScreen(
    viewModel: GameViewModel,
    onEnterTask: (Room) -> Unit,
    onOpenSurveillanceMonitors: () -> Unit
) {
    // Pozitia jucatorului e tinuta in GameViewModel (nu in "remember" local), ca sa
    // supravietuiasca navigarii catre alte ecrane (ex: camerele de supraveghere) si sa
    // nu te "teleporteze" inapoi la pozitia de start cand te intorci pe harta.
    var playerX by remember { mutableStateOf(viewModel.localPlayerX.value) }
    var playerY by remember { mutableStateOf(viewModel.localPlayerY.value) }

    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }

    var joystickOrigin by remember { mutableStateOf<Offset?>(null) }
    var joystickKnob by remember { mutableStateOf<Offset?>(null) }

    val playerSpeed = 5f
    val playerRadius = 12f

    // Segmentele tuturor peretilor, calculate o singura data prin unirea camerelor
    // care se ating/suprapun intr-o singura zona continua (evita "usi fantoma"
    // si linii de perete gresite intre camere adiacente).
    val wallSegments = remember { buildWallSegmentsFromMergedRooms(BuildingLayout.rooms) }

    var currentRoomIdLocal by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var frameCounter = 0
        while (true) {
            withFrameNanos { }
            if (joystickDirX != 0f || joystickDirY != 0f) {
                val newX = playerX + joystickDirX * playerSpeed
                val newY = playerY + joystickDirY * playerSpeed
                if (isWalkable(newX, playerY, playerRadius)) playerX = newX
                if (isWalkable(playerX, newY, playerRadius)) playerY = newY

                // Salvam pozitia in ViewModel la fiecare cadru de miscare, ca sa fie
                // mereu actualizata daca jucatorul navigheaza brusc catre alt ecran.
                viewModel.setLocalPlayerPosition(playerX, playerY)

                val room = BuildingLayout.getRoomAtPoint(playerX, playerY)
                val newRoomId = room?.id ?: ""
                if (newRoomId != currentRoomIdLocal) {
                    currentRoomIdLocal = newRoomId
                    if (newRoomId.isNotEmpty()) {
                        viewModel.moveToRoom(newRoomId)
                    }
                }

                // Trimitem pozitia exacta la fiecare al 3-lea cadru (nu la fiecare cadru,
                // ca sa nu supraincarcam serverul cu update-uri prea dese).
                frameCounter++
                if (frameCounter % 3 == 0) {
                    viewModel.updateLocalPosition(playerX, playerY, currentRoomIdLocal)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos ->
                        joystickOrigin = startPos
                        joystickKnob = startPos
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val origin = joystickOrigin ?: return@detectDragGestures
                        val rawOffset = change.position - origin
                        val distance = sqrt(rawOffset.x * rawOffset.x + rawOffset.y * rawOffset.y)
                        val clampedOffset = if (distance > JOYSTICK_BASE_RADIUS) {
                            Offset(
                                rawOffset.x / distance * JOYSTICK_BASE_RADIUS,
                                rawOffset.y / distance * JOYSTICK_BASE_RADIUS
                            )
                        } else {
                            rawOffset
                        }
                        joystickKnob = origin + clampedOffset
                        joystickDirX = clampedOffset.x / JOYSTICK_BASE_RADIUS
                        joystickDirY = clampedOffset.y / JOYSTICK_BASE_RADIUS
                    },
                    onDragEnd = {
                        joystickOrigin = null
                        joystickKnob = null
                        joystickDirX = 0f
                        joystickDirY = 0f
                    },
                    onDragCancel = {
                        joystickOrigin = null
                        joystickKnob = null
                        joystickDirX = 0f
                        joystickDirY = 0f
                    }
                )
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black, topLeft = Offset.Zero, size = size)

            val screenCenterX = size.width / 2f
            val screenCenterY = size.height / 2f

            fun worldToScreen(wx: Float, wy: Float): Offset {
                val dx = (wx - playerX) * TILE_SCALE
                val dy = (wy - playerY) * TILE_SCALE
                return Offset(screenCenterX + dx, screenCenterY + dy)
            }

            // Poligonul de vizibilitate calculat prin raycasting, in coordonate world.
            val visibilityPolygonWorld = computeVisibilityPolygon(
                originX = playerX,
                originY = playerY,
                segments = wallSegments,
                viewRadius = VIEW_RADIUS
            )

            val visibilityPathScreen = Path().apply {
                if (visibilityPolygonWorld.isNotEmpty()) {
                    val first = worldToScreen(visibilityPolygonWorld[0].x, visibilityPolygonWorld[0].y)
                    moveTo(first.x, first.y)
                    for (i in 1 until visibilityPolygonWorld.size) {
                        val p = worldToScreen(visibilityPolygonWorld[i].x, visibilityPolygonWorld[i].y)
                        lineTo(p.x, p.y)
                    }
                    close()
                }
            }

            // Tot ce desenam mai jos e "taiat" la forma poligonului vizibil - restul ramane negru.
            clipPath(visibilityPathScreen) {
                val viewRangeWorld = maxOf(size.width, size.height) / TILE_SCALE
                BuildingLayout.rooms.forEach { room ->
                    val roomCenterDist = kotlin.math.hypot(room.centerX() - playerX, room.centerY() - playerY)
                    if (roomCenterDist > viewRangeWorld) return@forEach

                    val topLeft = worldToScreen(room.x, room.y)
                    val sizePx = Size(room.width * TILE_SCALE, room.height * TILE_SCALE)

                    if (room.function == RoomFunction.HALLWAY) {
                        // Toate holurile folosesc acelasi desen generic (lumini de tavan
                        // repetate + covor central), indiferent de dimensiune/orientare.
                        translate(left = topLeft.x, top = topLeft.y) {
                            clipRect(left = 0f, top = 0f, right = sizePx.width, bottom = sizePx.height) {
                                drawHallwayDetailed(sizePx.width, sizePx.height)
                            }
                        }
                    } else if (room.id == "surveillance" || room.id == "armory" || room.id == "break_room" || room.id == "office1" || room.id == "office2" || room.id == "hub_central" || room.id == "server_room" || room.id == "meeting_room") {
                        // Camerele detaliate se deseneaza complet vectorial - vezi RoomArt.kt
                        // pentru continutul fiecareia, stil "FBI misterios" consistent.
                        // translate() muta originea (0,0) in coltul camerei, ca desenul
                        // din RoomArt sa foloseasca coordonate locale simple (0..w, 0..h).
                        translate(left = topLeft.x, top = topLeft.y) {
                            clipRect(left = 0f, top = 0f, right = sizePx.width, bottom = sizePx.height) {
                                when (room.id) {
                                    "surveillance" -> drawSurveillanceRoomDetailed(sizePx.width, sizePx.height)
                                    "armory" -> drawArmoryRoomDetailed(sizePx.width, sizePx.height)
                                    "break_room" -> drawBreakRoomDetailed(sizePx.width, sizePx.height)
                                    "office1" -> drawOfficeRoomDetailed(sizePx.width, sizePx.height)
                                    "office2" -> drawOffice2RoomDetailed(sizePx.width, sizePx.height)
                                    "hub_central" -> drawHubCentralDetailed(sizePx.width, sizePx.height)
                                    "server_room" -> drawServerRoomDetailed(sizePx.width, sizePx.height)
                                    "meeting_room" -> drawMeetingRoomDetailed(sizePx.width, sizePx.height)
                                }
                            }
                        }
                    } else {
                        drawRect(color = roomColor(room), topLeft = topLeft, size = sizePx)
                    }
                }
            }

            // Peretii reali (dupa unire) se deseneaza o singura data, deasupra camerelor,
            // ca sa nu mai apara linii false in mijlocul zonelor unite.
            clipPath(visibilityPathScreen) {
                wallSegments.forEach { seg ->
                    val p1 = worldToScreen(seg.x1, seg.y1)
                    val p2 = worldToScreen(seg.x2, seg.y2)
                    drawLine(color = Color.Black, start = p1, end = p2, strokeWidth = 3f)
                }
            }

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

            val origin = joystickOrigin
            val knob = joystickKnob
            if (origin != null && knob != null) {
                drawCircle(
                    color = Color(0x55FFFFFF),
                    radius = JOYSTICK_BASE_RADIUS,
                    center = origin
                )
                drawCircle(
                    color = Color(0xCCFFFFFF),
                    radius = JOYSTICK_KNOB_RADIUS,
                    center = knob
                )
            }
        }

        Text(
            text = currentRoomName(playerX, playerY),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // Buton "Camere", vizibil DOAR cand jucatorul e langa monitorul fizic din
        // camera de Supraveghere (nu oriunde in camera - altfel e prea usor/OP).
        val distToMonitor = kotlin.math.hypot(
            playerX - BuildingLayout.SURVEILLANCE_MONITOR_X,
            playerY - BuildingLayout.SURVEILLANCE_MONITOR_Y
        )
        val isNearMonitor = distToMonitor <= BuildingLayout.MONITOR_INTERACT_RADIUS
        if (isNearMonitor) {
            Button(
                onClick = onOpenSurveillanceMonitors,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Text("📹 Camere")
            }
        }
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

    val samplePoints = 8
    for (i in 0 until samplePoints) {
        val angle = (2.0 * Math.PI * i / samplePoints)
        val sampleX = px + radius * cos(angle).toFloat()
        val sampleY = py + radius * sin(angle).toFloat()
        if (BuildingLayout.rooms.none { it.containsPoint(sampleX, sampleY) }) {
            return false
        }
    }
    return BuildingLayout.rooms.any { it.containsPoint(px, py) }
}

// Sistemul de vizibilitate (raycasting: WallSegment, buildWallSegmentsFromMergedRooms,
// computeVisibilityPolygon, VIEW_RADIUS) e definit in Visibility.kt, ca sa fie
// folosit identic si de camerele de supraveghere (SurveillanceMonitorsScreen).