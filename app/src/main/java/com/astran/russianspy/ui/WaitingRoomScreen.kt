package com.astran.russianspy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.astran.russianspy.data.LobbyRoomLayout
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.network.AccountApi
import com.astran.russianspy.network.LobbyChatMessageInfo
import com.astran.russianspy.network.LobbyPlayerInfo
import com.astran.russianspy.ui.theme.TacticalColors
import com.astran.russianspy.viewmodel.GameViewModel
import com.astran.russianspy.viewmodel.RemovalReason
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_PLAYERS = 1
private const val MAX_PLAYERS = 15
private const val LOBBY_TILE_SCALE = 1.6f
private const val LOBBY_JOYSTICK_BASE_RADIUS = 90f
private const val LOBBY_JOYSTICK_KNOB_RADIUS = 36f
private const val LOBBY_PLAYER_RADIUS = 12f
private const val LOBBY_INTERACT_RADIUS = LobbyRoomLayout.INTERACT_RADIUS

private fun isLobbyWalkable(px: Float, py: Float, radius: Float): Boolean {
    val samplePoints = 8
    for (i in 0 until samplePoints) {
        val angle = (2.0 * Math.PI * i / samplePoints)
        val sampleX = px + radius * cos(angle).toFloat()
        val sampleY = py + radius * sin(angle).toFloat()
        if (LobbyRoomLayout.rooms.none { it.containsPoint(sampleX, sampleY) }) {
            return false
        }
    }
    return LobbyRoomLayout.rooms.any { it.containsPoint(px, py) }
}

/**
 * Cladirea MICA de asteptare (lobby), pe tot ecranul, in acelasi stil vizual
 * ca jocul propriu-zis (camere desenate vectorial, fog-of-war, joystick).
 * 3 camere mici legate prin 2 holuri scurte - vezi LobbyRoomLayout pentru
 * geometria exacta. Butonul de Start (doar host) ramane fix pe ecran, cu
 * fundal usor (nu opac), ca sa nu blocheze vederea camerei din spate.
 */
@Composable
fun WaitingRoomScreen(
    viewModel: GameViewModel,
    onGameStarted: () -> Unit,
    onLeaveLobby: () -> Unit,
    onRemovedFromRoom: (RemovalReason) -> Unit
) {
    val isHost by viewModel.isHost
    val lobbyPlayers = viewModel.lobbyPlayers
    val gameStarted by viewModel.gameStarted
    val errorMessage by viewModel.errorMessage
    val removalReason by viewModel.removalReason
    val gameState by viewModel.gameState

    var showSettings by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var showWardrobe by remember { mutableStateOf(false) }
    var playerForModeration by remember { mutableStateOf<LobbyPlayerInfo?>(null) }

    var playerX by remember { mutableStateOf(LobbyRoomLayout.SPAWN_X) }
    var playerY by remember { mutableStateOf(LobbyRoomLayout.SPAWN_Y) }
    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }
    var joystickOrigin by remember { mutableStateOf<Offset?>(null) }
    var joystickKnob by remember { mutableStateOf<Offset?>(null) }

    val wallSegments = remember { buildWallSegmentsFromMergedRooms(LobbyRoomLayout.rooms) }

    LaunchedEffect(gameStarted) {
        if (gameStarted) onGameStarted()
    }

    LaunchedEffect(removalReason) {
        val reason = removalReason
        if (reason != null) {
            viewModel.acknowledgeRemoval()
            viewModel.leaveLobby()
            onRemovedFromRoom(reason)
        }
    }

    LaunchedEffect(Unit) {
        var frameCounter = 0
        while (true) {
            withFrameNanos { }
            if (joystickDirX != 0f || joystickDirY != 0f) {
                val speed = 5f
                val newX = playerX + joystickDirX * speed
                val newY = playerY + joystickDirY * speed
                if (isLobbyWalkable(newX, playerY, LOBBY_PLAYER_RADIUS)) playerX = newX
                if (isLobbyWalkable(playerX, newY, LOBBY_PLAYER_RADIUS)) playerY = newY

                frameCounter++
                if (frameCounter % 3 == 0) {
                    viewModel.sendLobbyPosition(playerX, playerY)
                }
            }
        }
    }

    val roomCode = gameState?.roomCode ?: ""
    val connectedPlayerCount = lobbyPlayers.count { it.connected }
    val canStart = isHost && connectedPlayerCount >= MIN_PLAYERS

    val isNearMonitor = kotlin.math.hypot(
        (playerX - LobbyRoomLayout.MONITOR_X).toDouble(),
        (playerY - LobbyRoomLayout.MONITOR_Y).toDouble()
    ) <= LOBBY_INTERACT_RADIUS

    val isNearWardrobe = kotlin.math.hypot(
        (playerX - LobbyRoomLayout.WARDROBE_X).toDouble(),
        (playerY - LobbyRoomLayout.WARDROBE_Y).toDouble()
    ) <= LOBBY_INTERACT_RADIUS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                        val clamped = if (distance > LOBBY_JOYSTICK_BASE_RADIUS) {
                            Offset(
                                rawOffset.x / distance * LOBBY_JOYSTICK_BASE_RADIUS,
                                rawOffset.y / distance * LOBBY_JOYSTICK_BASE_RADIUS
                            )
                        } else rawOffset
                        joystickKnob = origin + clamped
                        joystickDirX = clamped.x / LOBBY_JOYSTICK_BASE_RADIUS
                        joystickDirY = clamped.y / LOBBY_JOYSTICK_BASE_RADIUS
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
                val dx = (wx - playerX) * LOBBY_TILE_SCALE
                val dy = (wy - playerY) * LOBBY_TILE_SCALE
                return Offset(screenCenterX + dx, screenCenterY + dy)
            }

            val visibilityPolygonWorld = computeVisibilityPolygon(
                originX = playerX,
                originY = playerY,
                segments = wallSegments,
                viewRadius = 900f
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

            clipPath(visibilityPathScreen) {
                LobbyRoomLayout.rooms.forEach { room ->
                    val topLeft = worldToScreen(room.x, room.y)
                    val sizePx = Size(room.width * LOBBY_TILE_SCALE, room.height * LOBBY_TILE_SCALE)

                    if (room.function == RoomFunction.HALLWAY) {
                        translate(left = topLeft.x, top = topLeft.y) {
                            clipRect(left = 0f, top = 0f, right = sizePx.width, bottom = sizePx.height) {
                                drawHallwayDetailed(sizePx.width, sizePx.height)
                            }
                        }
                    } else {
                        // Camera centrala si cele 2 camere laterale - desen simplu
                        // dedicat (nu RoomArt, care are layout-uri fixe pentru
                        // camerele jocului propriu-zis), dar in aceeasi paleta
                        // "FBI misterios".
                        val baseColor = when (room.id) {
                            "lobby_central" -> Color(0xFF1A1D22)
                            "lobby_settings" -> Color(0xFF141A24)
                            "lobby_wardrobe" -> Color(0xFF241A14)
                            else -> Color(0xFF1A1D22)
                        }
                        drawRect(color = baseColor, topLeft = topLeft, size = sizePx)
                        drawRect(
                            color = Color(0xFF05070A),
                            topLeft = topLeft,
                            size = sizePx,
                            style = Stroke(width = 2f)
                        )

                        when (room.id) {
                            "lobby_settings" -> {
                                val monitorPos = worldToScreen(LobbyRoomLayout.MONITOR_X, LobbyRoomLayout.MONITOR_Y)
                                drawRect(
                                    color = Color(0xFF1976D2),
                                    topLeft = Offset(monitorPos.x - 30f, monitorPos.y - 22f),
                                    size = Size(60f, 44f)
                                )
                                drawRect(
                                    color = Color(0xFF05070A),
                                    topLeft = Offset(monitorPos.x - 30f, monitorPos.y - 22f),
                                    size = Size(60f, 44f),
                                    style = Stroke(width = 2.5f)
                                )
                                drawRect(
                                    color = Color(0xFF3DDC5A).copy(alpha = 0.5f),
                                    topLeft = Offset(monitorPos.x - 22f, monitorPos.y - 15f),
                                    size = Size(44f, 26f)
                                )
                            }
                            "lobby_wardrobe" -> {
                                val wardrobePos = worldToScreen(LobbyRoomLayout.WARDROBE_X, LobbyRoomLayout.WARDROBE_Y)
                                drawRect(
                                    color = Color(0xFF6D4C41),
                                    topLeft = Offset(wardrobePos.x - 26f, wardrobePos.y - 34f),
                                    size = Size(52f, 68f)
                                )
                                drawRect(
                                    color = Color(0xFF05070A),
                                    topLeft = Offset(wardrobePos.x - 26f, wardrobePos.y - 34f),
                                    size = Size(52f, 68f),
                                    style = Stroke(width = 2.5f)
                                )
                                drawLine(
                                    color = Color(0xFF05070A),
                                    start = Offset(wardrobePos.x, wardrobePos.y - 34f),
                                    end = Offset(wardrobePos.x, wardrobePos.y + 34f),
                                    strokeWidth = 2.5f
                                )
                                // 2 manere mici
                                drawCircle(Color(0xFFB58A3D), radius = 3f, center = Offset(wardrobePos.x - 8f, wardrobePos.y))
                                drawCircle(Color(0xFFB58A3D), radius = 3f, center = Offset(wardrobePos.x + 8f, wardrobePos.y))
                            }
                        }
                    }
                }
            }

            clipPath(visibilityPathScreen) {
                wallSegments.forEach { seg ->
                    val p1 = worldToScreen(seg.x1, seg.y1)
                    val p2 = worldToScreen(seg.x2, seg.y2)
                    drawLine(color = Color.Black, start = p1, end = p2, strokeWidth = 3f)
                }
            }

            // Ceilalti jucatori din lobby, cu culoarea lor reala
            clipPath(visibilityPathScreen) {
                viewModel.lobbyPlayerPositions.entries.forEach { (pid, pos) ->
                    if (pid == viewModel.localPlayerId.value) return@forEach
                    val isVisible = isPointVisibleFromPoint(pos.x, pos.y, playerX, playerY, wallSegments, 900f)
                    if (!isVisible) return@forEach

                    val info = viewModel.lobbyPlayers.firstOrNull { it.id == pid }
                    val hex = info?.color ?: "#9E9E9E"
                    val color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color(0xFF9E9E9E)
                    }
                    val screenPos = worldToScreen(pos.x, pos.y)
                    drawCircle(color = color, radius = LOBBY_PLAYER_RADIUS * LOBBY_TILE_SCALE, center = screenPos)
                    drawCircle(
                        color = Color.Black,
                        radius = LOBBY_PLAYER_RADIUS * LOBBY_TILE_SCALE,
                        center = screenPos,
                        style = Stroke(width = 2f)
                    )
                    val name = info?.name
                    if (!name.isNullOrBlank()) {
                        drawContext.canvas.nativeCanvas.drawText(
                            name,
                            screenPos.x,
                            screenPos.y - LOBBY_PLAYER_RADIUS * LOBBY_TILE_SCALE - 10f,
                            android.graphics.Paint().apply {
                                this.color = android.graphics.Color.WHITE
                                textSize = 26f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }

            // Jucatorul local, cu propria culoare reala
            val myHex = viewModel.lobbyPlayers
                .firstOrNull { it.id == viewModel.localPlayerId.value }?.color ?: "#FFD700"
            val myColor = try {
                Color(android.graphics.Color.parseColor(myHex))
            } catch (e: Exception) {
                Color(0xFFFFD700)
            }
            drawCircle(color = myColor, radius = LOBBY_PLAYER_RADIUS * LOBBY_TILE_SCALE, center = Offset(screenCenterX, screenCenterY))
            drawCircle(
                color = Color.Black,
                radius = LOBBY_PLAYER_RADIUS * LOBBY_TILE_SCALE,
                center = Offset(screenCenterX, screenCenterY),
                style = Stroke(width = 2f)
            )

            val origin = joystickOrigin
            val knob = joystickKnob
            if (origin != null && knob != null) {
                drawCircle(color = Color(0x55FFFFFF), radius = LOBBY_JOYSTICK_BASE_RADIUS, center = origin)
                drawCircle(color = Color(0xCCFFFFFF), radius = LOBBY_JOYSTICK_KNOB_RADIUS, center = knob)
            }
        }

        // Header: cod camera + iesire + invita prieteni - text simplu peste
        // canvas, fara fundal opac (asa cum ai cerut sa nu blocheze vederea).
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp)) {
            IconButton(
                onClick = {
                    viewModel.leaveLobby()
                    onLeaveLobby()
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("⬅", fontSize = 20.sp, color = Color.White)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(roomCode, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text(
                    "JUCATORI (${connectedPlayerCount}/$MAX_PLAYERS)",
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp
                )
            }

            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showInviteDialog = true }) {
                    Text("👥", color = Color.White)
                }
                IconButton(onClick = { showChat = true }) {
                    Text("💬", fontSize = 18.sp)
                }
            }
        }

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = TacticalColors.Danger,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (isNearMonitor && isHost) {
            Button(
                onClick = { showSettings = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            ) {
                Text("⚙ Setari camera")
            }
        } else if (isNearMonitor && !isHost) {
            Text(
                "Doar gazda poate schimba setarile",
                color = Color(0xFFCCCCCC),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
        }

        if (isNearWardrobe) {
            Button(
                onClick = { showWardrobe = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D4C41)),
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
            ) {
                Text("🎨 Alege culoarea")
            }
        }

        // Butonul de Start (doar host) - fundal usor semi-transparent, NU un
        // strat opac care blocheaza vederea camerei din spatele lui.
        if (isHost) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33000000))
                    .clickable(enabled = canStart) { viewModel.startGame() }
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = if (canStart) "START" else "MINIM $MIN_PLAYERS JUCATORI",
                    color = if (canStart) Color.White else Color(0xFF888888),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }

    if (showSettings) {
        LobbySettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }

    if (showWardrobe) {
        WardrobeDialog(viewModel = viewModel, onDismiss = { showWardrobe = false })
    }

    if (showChat) {
        LobbyChatDialog(
            viewModel = viewModel,
            isHost = isHost,
            onDismiss = { showChat = false },
            onModerate = { player -> playerForModeration = player }
        )
    }

    if (showInviteDialog) {
        InviteFriendDialog(roomCode = roomCode, onDismiss = { showInviteDialog = false })
    }

    playerForModeration?.let { target ->
        ModeratePlayerDialog(
            playerName = target.name,
            onDismiss = { playerForModeration = null },
            onKick = {
                viewModel.kickPlayer(target.id)
                playerForModeration = null
            },
            onBan = {
                viewModel.banPlayer(target.id)
                playerForModeration = null
            }
        )
    }
}

@Composable
private fun WardrobeDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    val usedColors = viewModel.lobbyPlayers
        .filter { it.id != viewModel.localPlayerId.value && it.connected }
        .map { it.color }
        .toSet()
    val myColor = viewModel.lobbyPlayers.firstOrNull { it.id == viewModel.localPlayerId.value }?.color

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Alege-ti culoarea", color = TacticalColors.TextPrimary) },
        text = {
            Column {
                LobbyRoomLayout.PLAYER_COLORS.chunked(5).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowColors.forEach { hex ->
                            val isTaken = hex in usedColors && hex != myColor
                            val isMine = hex == myColor
                            val color = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isMine) 3.dp else 1.dp,
                                        color = if (isMine) Color.White else Color.Black,
                                        shape = CircleShape
                                    )
                                    .then(
                                        if (!isTaken) {
                                            Modifier.clickable {
                                                viewModel.choosePlayerColor(hex)
                                                onDismiss()
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                if (isTaken) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0x99000000))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide", color = TacticalColors.Accent)
            }
        }
    )
}

@Composable
private fun LobbyChatDialog(
    viewModel: GameViewModel,
    isHost: Boolean,
    onDismiss: () -> Unit,
    onModerate: (LobbyPlayerInfo) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var playerToReport by remember { mutableStateOf<LobbyPlayerInfo?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(TacticalColors.Surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chat", color = TacticalColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = TacticalColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(viewModel.lobbyPlayers) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalColors.SurfaceRaised)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val color = try {
                                    Color(android.graphics.Color.parseColor(player.color))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(player.name, color = TacticalColors.TextPrimary, fontSize = 13.sp)
                                if (player.id == viewModel.localPlayerId.value) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(tu)", color = TacticalColors.Accent, fontSize = 11.sp)
                                }
                            }

                            Row {
                                if (player.id != viewModel.localPlayerId.value) {
                                    TextButton(onClick = { playerToReport = player }) {
                                        Text("🚩 Raporteaza", fontSize = 11.sp, color = TacticalColors.Danger)
                                    }
                                    if (isHost) {
                                        TextButton(onClick = { onModerate(player) }) {
                                            Text("⋮", fontSize = 14.sp, color = TacticalColors.TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TacticalColors.Border)
                Spacer(modifier = Modifier.height(8.dp))

                val listState = rememberLazyListState()
                LaunchedEffect(viewModel.lobbyChatMessages.size) {
                    if (viewModel.lobbyChatMessages.isNotEmpty()) {
                        listState.animateScrollToItem(viewModel.lobbyChatMessages.size - 1)
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(viewModel.lobbyChatMessages) { msg: LobbyChatMessageInfo ->
                        val isMine = msg.senderId == viewModel.localPlayerId.value
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .then(if (isMine) Modifier.padding(start = 40.dp) else Modifier)
                        ) {
                            Text(msg.senderName, color = TacticalColors.TextSecondary, fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMine) TacticalColors.AccentDim else TacticalColors.SurfaceRaised)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(msg.text, color = TacticalColors.TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 300) draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Scrie un mesaj...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (draft.isNotBlank()) {
                                    viewModel.sendLobbyChatMessage(draft)
                                    draft = ""
                                }
                            }
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (draft.isNotBlank()) {
                            viewModel.sendLobbyChatMessage(draft)
                            draft = ""
                        }
                    }) {
                        Text("➤", color = TacticalColors.Accent, fontSize = 20.sp)
                    }
                }
            }
        }
    }

    playerToReport?.let { target ->
        ReportPlayerDialog(
            player = target,
            onDismiss = { playerToReport = null },
            onSubmit = { reason ->
                viewModel.reportPlayer(target.id, reason)
                playerToReport = null
            }
        )
    }
}

@Composable
private fun ReportPlayerDialog(
    player: LobbyPlayerInfo,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "hacking" to "🖥 Hack",
        "harassment" to "😠 Hartuire",
        "bad_language" to "🤬 Vorbire proasta",
        "name" to "🔤 Nume"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Raporteaza pe ${player.name}", color = TacticalColors.TextPrimary) },
        text = {
            Column {
                Text("Alege motivul raportului:", color = TacticalColors.TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))

                categories.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (reasonKey, label) ->
                            val isSelected = selectedReason == reasonKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) TacticalColors.AccentDim else TacticalColors.SurfaceRaised)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TacticalColors.Accent else TacticalColors.Border,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedReason = reasonKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = TacticalColors.TextPrimary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selectedReason?.let(onSubmit) }, enabled = selectedReason != null) {
                Text(
                    "Trimite",
                    color = if (selectedReason != null) TacticalColors.Accent else TacticalColors.TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuleaza", color = TacticalColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun InviteFriendDialog(roomCode: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val accountId = remember { PlayerPrefs.getAccountId(context) }

    var friends by remember { mutableStateOf<List<com.astran.russianspy.network.AccountInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AccountApi.fetchFriendsData(accountId) { data, error ->
            isLoading = false
            if (data != null) {
                friends = data.friends
            } else {
                statusMessage = error ?: "Nu am putut incarca lista de prieteni"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Invita un prieten", color = TacticalColors.TextPrimary) },
        text = {
            Column {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TacticalColors.Accent)
                    }
                } else if (friends.isEmpty()) {
                    Text("Nu ai niciun prieten adaugat inca.", fontSize = 13.sp, color = TacticalColors.TextSecondary)
                } else {
                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(friend.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TacticalColors.TextPrimary)
                            TextButton(onClick = {
                                AccountApi.inviteToRoom(accountId, friend.accountId, roomCode) { success, error ->
                                    statusMessage = if (success) {
                                        "Invitatie trimisa catre ${friend.displayName}."
                                    } else {
                                        error ?: "Nu am putut trimite invitatia"
                                    }
                                }
                            }) {
                                Text("Invita", color = TacticalColors.Accent)
                            }
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, fontSize = 12.sp, color = TacticalColors.Success)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide", color = TacticalColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun ModeratePlayerDialog(
    playerName: String,
    onDismiss: () -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text(playerName, color = TacticalColors.TextPrimary) },
        text = {
            Text(
                "Kick: poate reintra oricand cu acelasi cod.\nBan: nu se mai poate intoarce in aceasta camera.",
                color = TacticalColors.TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onBan) {
                Text("BAN", color = TacticalColors.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Anuleaza", color = TacticalColors.TextSecondary)
                }
                TextButton(onClick = onKick) {
                    Text("KICK", color = TacticalColors.Accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun LobbySettingsDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    val isPrivate by viewModel.roomIsPrivate
    val isUpdating by viewModel.roomPrivacyUpdating
    val spyTaskCount by viewModel.spyTaskCount
    val fbiTaskCount by viewModel.fbiTaskCount

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Setari camera", color = TacticalColors.TextPrimary) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPrivate) "Camera privata" else "Camera publica",
                            color = TacticalColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isPrivate) {
                                "Nu apare in lista de lobby-uri publice. Se poate intra doar cu codul."
                            } else {
                                "Apare in lista de lobby-uri publice, oricine o poate gasi."
                            },
                            color = TacticalColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { checked -> viewModel.setRoomPrivacy(checked) },
                        enabled = !isUpdating,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TacticalColors.Accent,
                            checkedTrackColor = TacticalColors.AccentDim
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = TacticalColors.TextSecondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Task-uri spion: $spyTaskCount",
                    color = TacticalColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Cate task-uri alege serverul, random, pentru spion in fiecare runda.",
                    color = TacticalColors.TextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = spyTaskCount.toFloat(),
                    onValueChange = { viewModel.setSpyTaskCount(it.toInt()) },
                    valueRange = 2f..20f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = TacticalColors.Accent,
                        activeTrackColor = TacticalColors.Accent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Task-uri FBI (per agent): $fbiTaskCount",
                    color = TacticalColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Cate task-uri cosmetice primeste FIECARE agent FBI in parte.",
                    color = TacticalColors.TextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = fbiTaskCount.toFloat(),
                    onValueChange = { viewModel.setFbiTaskCount(it.toInt()) },
                    valueRange = 1f..20f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = TacticalColors.Accent,
                        activeTrackColor = TacticalColors.Accent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Inchide", color = TacticalColors.Accent)
            }
        }
    )
}
