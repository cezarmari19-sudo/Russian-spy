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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.viewmodel.GameViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TILE_SCALE = 2.2f
private const val JOYSTICK_BASE_RADIUS = 100f
private const val JOYSTICK_KNOB_RADIUS = 40f
private const val VIEW_RADIUS = 420f // raza de vizibilitate in unitati "world", stil Among Us

@Composable
fun GameCanvasScreen(
    viewModel: GameViewModel,
    onEnterTask: (Room) -> Unit,
    onOpenSurveillanceMonitors: () -> Unit
) {
    var playerX by remember { mutableStateOf(BuildingLayout.START_X) }
    var playerY by remember { mutableStateOf(BuildingLayout.START_Y) }

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

                    drawRect(color = roomColor(room), topLeft = topLeft, size = sizePx)
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

        // Buton "Camere", vizibil doar cand jucatorul e in camera de supraveghere.
        if (currentRoomIdLocal == "surveillance") {
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

// ---------------------------------------------------------------------------------
// Sistem de vizibilitate (raycasting), stil Among Us: raza fixa, blocata de pereti.
//
// Abordare noua, mai robusta decat inainte:
// 1. Camerele sunt unite intr-o singura zona continua folosind un grid boolean
//    (union geometrica pe celule), asa ca nu mai depindem de compararea exacta
//    a coordonatelor de tip Float intre camere adiacente ("hall_x == hub_y").
// 2. Peretii reali sunt extrasi ca marginile exterioare ale zonei unite -
//    orice granita dintre doua camere care se ating/suprapun dispare automat,
//    fara nicio lista separata de "usi".
// 3. Raycasting-ul foloseste un epsilon relativ (nu +/-0.0001 fix) si sorteaza
//    punctele dupa unghi cu un tie-break pe distanta, ca sa evite crestaturile
//    ascutite cand doua raze au unghiuri aproape identice dar lovesc segmente
//    diferite.
// ---------------------------------------------------------------------------------

data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
private data class WorldPoint(val x: Float, val y: Float)

private const val GRID_CELL = 25f // rezolutia gridului de union, in unitati world

/**
 * Uneste toate camerele intr-o zona continua folosind un grid boolean, apoi
 * extrage marginile exterioare ale zonei unite ca segmente de perete.
 * Asta elimina complet nevoia de a detecta "usi" prin compararea coordonatelor
 * exacte intre camere - orice doua camere care se ating sau se suprapun devin
 * automat o singura zona fara perete intre ele, indiferent de erori mici de
 * aliniere in date.
 */
fun buildWallSegmentsFromMergedRooms(rooms: List<Room>): List<WallSegment> {
    if (rooms.isEmpty()) return emptyList()

    val minX = rooms.minOf { it.x }
    val minY = rooms.minOf { it.y }
    val maxX = rooms.maxOf { it.x + it.width }
    val maxY = rooms.maxOf { it.y + it.height }

    val cols = kotlin.math.ceil((maxX - minX) / GRID_CELL).toInt() + 1
    val rowsCount = kotlin.math.ceil((maxY - minY) / GRID_CELL).toInt() + 1

    // occupied[row][col] = true daca celula respectiva e acoperita de vreo camera.
    val occupied = Array(rowsCount) { BooleanArray(cols) }

    fun cellCenterWorldX(col: Int) = minX + (col + 0.5f) * GRID_CELL
    fun cellCenterWorldY(row: Int) = minY + (row + 0.5f) * GRID_CELL

    for (row in 0 until rowsCount) {
        for (col in 0 until cols) {
            val wx = cellCenterWorldX(col)
            val wy = cellCenterWorldY(row)
            if (rooms.any { it.containsPoint(wx, wy) }) {
                occupied[row][col] = true
            }
        }
    }

    fun isOccupied(row: Int, col: Int): Boolean {
        if (row < 0 || row >= rowsCount || col < 0 || col >= cols) return false
        return occupied[row][col]
    }

    // Pentru fiecare celula ocupata, verificam cele 4 muchii; daca vecinul de pe
    // acea directie nu e ocupat, muchia respectiva e perete exterior real.
    val rawEdges = mutableListOf<WallSegment>()
    for (row in 0 until rowsCount) {
        for (col in 0 until cols) {
            if (!isOccupied(row, col)) continue

            val x1 = minX + col * GRID_CELL
            val y1 = minY + row * GRID_CELL
            val x2 = x1 + GRID_CELL
            val y2 = y1 + GRID_CELL

            if (!isOccupied(row - 1, col)) rawEdges.add(WallSegment(x1, y1, x2, y1)) // sus
            if (!isOccupied(row + 1, col)) rawEdges.add(WallSegment(x1, y2, x2, y2)) // jos
            if (!isOccupied(row, col - 1)) rawEdges.add(WallSegment(x1, y1, x1, y2)) // stanga
            if (!isOccupied(row, col + 1)) rawEdges.add(WallSegment(x2, y1, x2, y2)) // dreapta
        }
    }

    return mergeCollinearSegments(rawEdges)
}

/**
 * Uneste segmentele mici de grid care sunt coliniare si adiacente intr-un singur
 * segment lung, ca sa reducem numarul de segmente folosite in raycasting
 * (performanta) si sa evitam colturi false intre bucati de perete care de fapt
 * formeaza o linie dreapta continua.
 */
private fun mergeCollinearSegments(segments: List<WallSegment>): List<WallSegment> {
    val horizontal = segments.filter { it.y1 == it.y2 }
        .groupBy { it.y1 }
    val vertical = segments.filter { it.x1 == it.x2 }
        .groupBy { it.x1 }

    val result = mutableListOf<WallSegment>()

    horizontal.forEach { (y, segs) ->
        val intervals = segs.map { minOf(it.x1, it.x2) to maxOf(it.x1, it.x2) }.sortedBy { it.first }
        var curStart = intervals.first().first
        var curEnd = intervals.first().second
        for (i in 1 until intervals.size) {
            val (s, e) = intervals[i]
            if (s <= curEnd + 0.01f) {
                curEnd = maxOf(curEnd, e)
            } else {
                result.add(WallSegment(curStart, y, curEnd, y))
                curStart = s
                curEnd = e
            }
        }
        result.add(WallSegment(curStart, y, curEnd, y))
    }

    vertical.forEach { (x, segs) ->
        val intervals = segs.map { minOf(it.y1, it.y2) to maxOf(it.y1, it.y2) }.sortedBy { it.first }
        var curStart = intervals.first().first
        var curEnd = intervals.first().second
        for (i in 1 until intervals.size) {
            val (s, e) = intervals[i]
            if (s <= curEnd + 0.01f) {
                curEnd = maxOf(curEnd, e)
            } else {
                result.add(WallSegment(x, curStart, x, curEnd))
                curStart = s
                curEnd = e
            }
        }
        result.add(WallSegment(x, curStart, x, curEnd))
    }

    return result
}

/**
 * Calculeaza poligonul de vizibilitate din punctul (originX, originY), limitat la viewRadius,
 * blocat de segmentele de perete date. Trage o raza catre fiecare capat de perete relevant
 * (plus doua raze usor deviate cu un epsilon unghiular mic si constant) si intersecteaza
 * fiecare raza cu cel mai apropiat perete, pastrand cea mai mica distanta gasita.
 */
private fun computeVisibilityPolygon(
    originX: Float,
    originY: Float,
    segments: List<WallSegment>,
    viewRadius: Float
): List<WorldPoint> {
    val relevantSegments = segments.filter { seg ->
        val distToSeg = distancePointToSegment(originX, originY, seg)
        distToSeg < viewRadius * 1.5f
    }

    val angleEpsilon = 0.00005
    val anglesSet = LinkedHashSet<Double>()

    relevantSegments.forEach { seg ->
        listOf(seg.x1 to seg.y1, seg.x2 to seg.y2).forEach { (px, py) ->
            val baseAngle = atan2((py - originY).toDouble(), (px - originX).toDouble())
            anglesSet.add(baseAngle)
            anglesSet.add(baseAngle - angleEpsilon)
            anglesSet.add(baseAngle + angleEpsilon)
        }
    }
    // Adaugam si unghiuri uniforme ca fallback pentru zone fara pereti apropiati (camere deschise).
    val fallbackSteps = 90
    for (i in 0 until fallbackSteps) {
        anglesSet.add(2.0 * Math.PI * i / fallbackSteps)
    }

    val points = mutableListOf<Pair<Double, WorldPoint>>()
    anglesSet.forEach { angle ->
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        var closestDist = viewRadius
        var hitX = originX + dx * viewRadius
        var hitY = originY + dy * viewRadius

        relevantSegments.forEach { seg ->
            val t = raySegmentIntersection(originX, originY, dx, dy, seg)
            if (t != null && t < closestDist) {
                closestDist = t
                hitX = originX + dx * t
                hitY = originY + dy * t
            }
        }
        points.add(angle to WorldPoint(hitX, hitY))
    }

    return points.sortedBy { it.first }.map { it.second }
}

private fun distancePointToSegment(px: Float, py: Float, seg: WallSegment): Float {
    val dx = seg.x2 - seg.x1
    val dy = seg.y2 - seg.y1
    val lenSq = dx * dx + dy * dy
    if (lenSq < 1e-6f) return kotlin.math.hypot(px - seg.x1, py - seg.y1)
    var t = ((px - seg.x1) * dx + (py - seg.y1) * dy) / lenSq
    t = t.coerceIn(0f, 1f)
    val closestX = seg.x1 + t * dx
    val closestY = seg.y1 + t * dy
    return kotlin.math.hypot(px - closestX, py - closestY)
}

/**
 * Intersecteaza raza (origin + t*dir) cu segmentul dat. Returneaza distanta t (>=0) daca exista
 * intersectie in fata razei si in interiorul segmentului (cu o mica toleranta la capete ca sa
 * nu piarda intersectii chiar in colturi din cauza erorilor de rotunjire), altfel null.
 */
private fun raySegmentIntersection(
    originX: Float,
    originY: Float,
    dirX: Float,
    dirY: Float,
    seg: WallSegment
): Float? {
    val x3 = seg.x1
    val y3 = seg.y1
    val x4 = seg.x2
    val y4 = seg.y2

    val denom = dirX * (y4 - y3) - dirY * (x4 - x3)
    if (kotlin.math.abs(denom) < 1e-6f) return null

    val t2 = ((x3 - originX) * dirY - (y3 - originY) * dirX) / denom
    val edgeTolerance = 0.001f
    if (t2 < -edgeTolerance || t2 > 1f + edgeTolerance) return null
    val t2Clamped = t2.coerceIn(0f, 1f)

    val t1 = if (kotlin.math.abs(dirX) > kotlin.math.abs(dirY)) {
        (x3 + t2Clamped * (x4 - x3) - originX) / dirX
    } else {
        (y3 + t2Clamped * (y4 - y3) - originY) / dirY
    }
    if (t1 < 0f) return null

    return t1
}