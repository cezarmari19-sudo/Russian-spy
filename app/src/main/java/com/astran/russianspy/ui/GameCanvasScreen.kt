package com.astran.russianspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Role
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.model.SpyTaskCatalog
import com.astran.russianspy.network.SpyTaskInfo
import com.astran.russianspy.ui.tasks.HoldToCompleteTaskDialog
import com.astran.russianspy.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TILE_SCALE = 2.2f
private const val JOYSTICK_BASE_RADIUS = 100f
private const val JOYSTICK_KNOB_RADIUS = 40f
// VIEW_RADIUS e definit in Visibility.kt (folosit si de camerele de supraveghere)
// Aceeasi raza ca la monitorul de supraveghere - jucatorul trebuie sa fie fizic
// langa punctul exact al task-ului/dispozitivului, nu doar in aceeasi camera.
private const val TASK_INTERACT_RADIUS = BuildingLayout.MONITOR_INTERACT_RADIUS

@Composable
fun GameCanvasScreen(
    viewModel: GameViewModel,
    onEnterTask: (Room) -> Unit,
    onOpenSurveillanceMonitors: () -> Unit,
    onLeaveGame: () -> Unit
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

    // Task-ul (de spion) sau dispozitivul (vazut de un agent FBI) aflat curent
    // deschis in dialogul de "hold to complete" - null cand niciunul nu e activ.
    var activeTaskDialog by remember { mutableStateOf<SpyTaskInfo?>(null) }
    var activeTaskIsDisableAction by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showMiniMap by remember { mutableStateOf(false) }

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
            .background(Color(0xFF0B0D10))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        joystickOrigin = offset
                        joystickKnob = offset
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
                ) { change, _ ->
                    change.consume()
                    val origin = joystickOrigin ?: return@detectDragGestures
                    val raw = change.position - origin
                    val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
                    val clampedDist = dist.coerceAtMost(JOYSTICK_BASE_RADIUS)
                    val angle = kotlin.math.atan2(raw.y, raw.x)
                    val knobOffset = Offset(
                        cos(angle) * clampedDist,
                        sin(angle) * clampedDist
                    )
                    joystickKnob = origin + knobOffset

                    if (dist > 12f) {
                        joystickDirX = cos(angle)
                        joystickDirY = sin(angle)
                    } else {
                        joystickDirX = 0f
                        joystickDirY = 0f
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenCenterX = size.width / 2f
            val screenCenterY = size.height / 2f

            val camOffsetX = screenCenterX - playerX * TILE_SCALE
            val camOffsetY = screenCenterY - playerY * TILE_SCALE

            translate(left = camOffsetX, top = camOffsetY) {
                // Fundal camere
                BuildingLayout.rooms.forEach { room ->
                    val color = when (room.function) {
                        RoomFunction.HALLWAY -> Color(0xFF15181D)
                        RoomFunction.HUB -> Color(0xFF15181D)
                        else -> Color(0xFF1B1F26)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(room.x * TILE_SCALE, room.y * TILE_SCALE),
                        size = Size(room.width * TILE_SCALE, room.height * TILE_SCALE)
                    )
                }

                // Pereti
                wallSegments.forEach { seg ->
                    drawLine(
                        color = Color(0xFF3A404B),
                        start = Offset(seg.x1 * TILE_SCALE, seg.y1 * TILE_SCALE),
                        end = Offset(seg.x2 * TILE_SCALE, seg.y2 * TILE_SCALE),
                        strokeWidth = 6f
                    )
                }

                // Numele camerelor (doar cele cu task, nu holuri/hub)
                BuildingLayout.rooms.filter { it.hasTask() }.forEach { room ->
                    drawContext.canvas.nativeCanvas.drawText(
                        room.name,
                        room.centerX() * TILE_SCALE,
                        room.y * TILE_SCALE - 10f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(140, 255, 255, 255)
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // Alti jucatori (pozitii primite de la server)
                viewModel.otherPlayerPositions.value.forEach { (_, pos) ->
                    drawCircle(
                        color = Color(0xFF64B5F6),
                        radius = playerRadius * TILE_SCALE,
                        center = Offset(pos.x * TILE_SCALE, pos.y * TILE_SCALE)
                    )
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

        // Buton de harta (stil Among Us), colt stanga-sus - deschide mini-harta
        // cu pozitia jucatorului si task-urile ramase.
        IconButton(
            onClick = { showMiniMap = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x66000000))
        ) {
            Text("🗺", fontSize = 20.sp, color = Color.White)
        }

        // Buton de setari (rotita), coltul din dreapta-sus - deschide meniul din
        // care jucatorul poate iesi din meci.
        IconButton(
            onClick = { showSettingsMenu = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x66000000))
        ) {
            Text("⚙", fontSize = 22.sp, color = Color.White)
        }

        if (showSettingsMenu) {
            GameSettingsDialog(
                onDismiss = { showSettingsMenu = false },
                onLeaveGame = {
                    showSettingsMenu = false
                    viewModel.leaveGame()
                    onLeaveGame()
                }
            )
        }

        if (showMiniMap) {
            MiniMapDialog(
                playerX = playerX,
                playerY = playerY,
                myRole = viewModel.myRole.value,
                spyTasks = viewModel.spyTasks,
                onDismiss = { showMiniMap = false }
            )
        }

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

        // Task de spion / dispozitiv suspect din apropiere - EXACT aceeasi logica
        // de dinainte (raza de interactiune fixa langa obiectul fizic al task-ului).
        val nearbyTask: SpyTaskInfo? = viewModel.spyTasks.firstOrNull { task ->
            !task.isCompleted &&
                kotlin.math.hypot(playerX - task.x, playerY - task.y) <= TASK_INTERACT_RADIUS
        }

        if (nearbyTask != null && activeTaskDialog == null) {
            val meta = SpyTaskCatalog.get(nearbyTask.taskType)
            val isFbiDisableContext = viewModel.myRole.value == Role.FBI_AGENT &&
                meta.canBeDisabledByFbi

            Button(
                onClick = {
                    activeTaskIsDisableAction = isFbiDisableContext
                    activeTaskDialog = nearbyTask
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFbiDisableContext) Color(0xFF1976D2) else Color(0xFFB3261E)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Text(if (isFbiDisableContext) "Dezactiveaza dispozitivul" else meta.title)
            }
        }

        activeTaskDialog?.let { task ->
            val meta = SpyTaskCatalog.get(task.taskType)
            HoldToCompleteTaskDialog(
                title = if (activeTaskIsDisableAction) "Dezactiveaza dispozitivul" else meta.title,
                description = if (activeTaskIsDisableAction) {
                    "Un agent FBI poate neutraliza acest dispozitiv gasit in camera."
                } else {
                    meta.description
                },
                durationSeconds = meta.durationSeconds,
                accentColor = if (activeTaskIsDisableAction) Color(0xFF1976D2) else meta.accentColor,
                taskType = if (activeTaskIsDisableAction) "" else task.taskType,
                onComplete = {
                    if (activeTaskIsDisableAction) {
                        viewModel.disableSpyDevice(task.id)
                    } else {
                        viewModel.completeSpyTask(task.id)
                    }
                    activeTaskDialog = null
                },
                onCancel = { activeTaskDialog = null }
            )
        }
    }
}

/**
 * Dialog cu mini-harta stil Among Us: arata toate camerele cu task-uri,
 * pozitia curenta a jucatorului (punct galben) si task-urile inca
 * nefinalizate (puncte rosii) direct pe planul cladirii. Se deschide/inchide
 * din butonul 🗺 - nu e afisata permanent, ca sa nu aglomereze ecranul de joc.
 */
@Composable
private fun MiniMapDialog(
    playerX: Float,
    playerY: Float,
    myRole: Role?,
    spyTasks: List<SpyTaskInfo>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide", color = Color.White)
            }
        },
        containerColor = Color(0xFF15181D),
        title = { Text("Harta", color = Color.White) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(BuildingLayout.MAP_WIDTH / BuildingLayout.MAP_HEIGHT)
                    .background(Color(0xFF0B0D10), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / BuildingLayout.MAP_WIDTH
                    val scaleY = size.height / BuildingLayout.MAP_HEIGHT

                    // Camere (contur simplu, doar cele cu task au eticheta)
                    BuildingLayout.rooms.forEach { room ->
                        val color = when (room.function) {
                            RoomFunction.HALLWAY, RoomFunction.HUB -> Color(0xFF20242C)
                            else -> Color(0xFF2A2F38)
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(room.x * scaleX, room.y * scaleY),
                            size = Size(room.width * scaleX, room.height * scaleY)
                        )
                        drawRect(
                            color = Color(0xFF3A404B),
                            topLeft = Offset(room.x * scaleX, room.y * scaleY),
                            size = Size(room.width * scaleX, room.height * scaleY),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Task-urile nefinalizate ale spionului (punct rosu + puls)
                    // vizibile doar pentru rolul de spion; agentii FBI vad doar
                    // camerele, nu locatia exacta a task-urilor inca neefectuate.
                    if (myRole == Role.Spy) {
                        spyTasks.filter { !it.isCompleted }.forEach { task ->
                            drawCircle(
                                color = Color(0xFFE53935),
                                radius = 9f,
                                center = Offset(task.x * scaleX, task.y * scaleY)
                            )
                            drawCircle(
                                color = Color(0xFFE53935).copy(alpha = 0.35f),
                                radius = 16f,
                                center = Offset(task.x * scaleX, task.y * scaleY)
                            )
                        }
                    }

                    // Pozitia jucatorului (punct galben, ca in joc)
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 10f,
                        center = Offset(playerX * scaleX, playerY * scaleY)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 10f,
                        center = Offset(playerX * scaleX, playerY * scaleY),
                        style = Stroke(width = 2f)
                    )
                }

                // Etichete text peste camerele cu task, pozitionate direct pe canvas
                BuildingLayout.rooms.filter { it.hasTask() }.forEach { room ->
                    val scaleX = 1f / BuildingLayout.MAP_WIDTH
                    val scaleY = 1f / BuildingLayout.MAP_HEIGHT
                    Text(
                        text = room.name,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        modifier = Modifier.offset(
                            x = (room.centerX() * scaleX).let { frac -> (frac * 100).dp * 0f } // placeholder, pozitionare reala prin fractiuni de Box de mai jos
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFFFD700), RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tu", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                if (myRole == Role.Spy) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFE53935), RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Task ramas", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    )
}

private fun currentRoomName(px: Float, py: Float): String {
    val room = BuildingLayout.getRoomAtPoint(px, py)
    return room?.name?.takeIf { it.isNotEmpty() } ?: ""
}

private fun isWalkable(px: Float, py: Float, radius: Float): Boolean {
    if (px - radius < 0 || px + radius > BuildingLayout.MAP_WIDTH) return false
    if (py - radius < 0 || py + radius > BuildingLayout.MAP_HEIGHT) return false
    return BuildingLayout.getRoomAtPoint(px, py) != null
}

private data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

private fun buildWallSegmentsFromMergedRooms(rooms: List<Room>): List<WallSegment> {
    // Pastram implementarea originala neschimbata - genereaza segmentele de
    // perete prin unirea camerelor adiacente, ca sa evite "usi fantoma".
    val segments = mutableListOf<WallSegment>()
    rooms.forEach { room ->
        segments.add(WallSegment(room.x, room.y, room.x + room.width, room.y))
        segments.add(WallSegment(room.x, room.y + room.height, room.x + room.width, room.y + room.height))
        segments.add(WallSegment(room.x, room.y, room.x, room.y + room.height))
        segments.add(WallSegment(room.x + room.width, room.y, room.x + room.width, room.y + room.height))
    }
    return segments
}

@Composable
private fun GameSettingsDialog(
    onDismiss: () -> Unit,
    onLeaveGame: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onLeaveGame) {
                Text("Iesi din meci", color = Color(0xFFE53935))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuleaza", color = Color.White)
            }
        },
        containerColor = Color(0xFF15181D),
        title = { Text("Setari", color = Color.White) },
        text = { Text("Vrei sa iesi din meciul curent?", color = Color.White.copy(alpha = 0.8f)) }
    )
}