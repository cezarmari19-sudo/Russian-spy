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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
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
    onEnterTask: (Room) -> Unit
) {
    var playerX by remember { mutableStateOf(BuildingLayout.START_X) }
    var playerY by remember { mutableStateOf(BuildingLayout.START_Y) }

    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }

    var joystickOrigin by remember { mutableStateOf<Offset?>(null) }
    var joystickKnob by remember { mutableStateOf<Offset?>(null) }

    val playerSpeed = 5f
    val playerRadius = 12f

    // Segmentele tuturor peretilor (marginile camerelor), calculate o singura data.
    val wallSegments = remember { buildWallSegments(BuildingLayout.rooms) }

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
                    drawRect(color = Color(0xFF000000), topLeft = topLeft, size = sizePx, style = Stroke(width = 3f))
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
// ---------------------------------------------------------------------------------

private data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
private data class WorldPoint(val x: Float, val y: Float)

/**
 * Construieste segmentele de perete din marginile camerelor.
 * Un segment care e (aproape) identic intre doua camere e o "usa"/trecere si e eliminat,
 * ca vederea sa poata trece liber intre camere adiacente.
 */
private fun buildWallSegments(rooms: List<Room>): List<WallSegment> {
    val rawSegments = mutableListOf<WallSegment>()
    rooms.forEach { room ->
        val x1 = room.x
        val y1 = room.y
        val x2 = room.x + room.width
        val y2 = room.y + room.height
        rawSegments.add(WallSegment(x1, y1, x2, y1)) // sus
        rawSegments.add(WallSegment(x1, y2, x2, y2)) // jos
        rawSegments.add(WallSegment(x1, y1, x1, y2)) // stanga
        rawSegments.add(WallSegment(x2, y1, x2, y2)) // dreapta
    }

    fun overlaps1D(aMin: Float, aMax: Float, bMin: Float, bMax: Float): Float {
        return minOf(aMax, bMax) - maxOf(aMin, bMin)
    }

    val doors = mutableListOf<WallSegment>()
    for (i in rawSegments.indices) {
        for (j in i + 1 until rawSegments.size) {
            val a = rawSegments[i]
            val b = rawSegments[j]
            val aVertical = a.x1 == a.x2
            val bVertical = b.x1 == b.x2
            if (aVertical != bVertical) continue
            if (aVertical) {
                if (a.x1 != b.x1) continue
                val overlap = overlaps1D(minOf(a.y1, a.y2), maxOf(a.y1, a.y2), minOf(b.y1, b.y2), maxOf(b.y1, b.y2))
                if (overlap > 1f) {
                    val yMin = maxOf(minOf(a.y1, a.y2), minOf(b.y1, b.y2))
                    val yMax = minOf(maxOf(a.y1, a.y2), maxOf(b.y1, b.y2))
                    doors.add(WallSegment(a.x1, yMin, a.x1, yMax))
                }
            } else {
                if (a.y1 != b.y1) continue
                val overlap = overlaps1D(minOf(a.x1, a.x2), maxOf(a.x1, a.x2), minOf(b.x1, b.x2), maxOf(b.x1, b.x2))
                if (overlap > 1f) {
                    val xMin = maxOf(minOf(a.x1, a.x2), minOf(b.x1, b.x2))
                    val xMax = minOf(maxOf(a.x1, a.x2), maxOf(b.x1, b.x2))
                    doors.add(WallSegment(xMin, a.y1, xMax, a.y1))
                }
            }
        }
    }

    // Scadem partea de "usa" din fiecare perete original, pastrand restul ca perete solid.
    val result = mutableListOf<WallSegment>()
    rawSegments.forEach { seg ->
        val segIsVertical = seg.x1 == seg.x2
        val overlappingDoors = doors.filter { door ->
            val doorIsVertical = door.x1 == door.x2
            if (doorIsVertical != segIsVertical) return@filter false
            if (segIsVertical) door.x1 == seg.x1 else door.y1 == seg.y1
        }
        if (overlappingDoors.isEmpty()) {
            result.add(seg)
            return@forEach
        }
        if (segIsVertical) {
            val segMin = minOf(seg.y1, seg.y2)
            val segMax = maxOf(seg.y1, seg.y2)
            val covered = overlappingDoors.map { minOf(it.y1, it.y2) to maxOf(it.y1, it.y2) }.sortedBy { it.first }
            var cursor = segMin
            covered.forEach { (dMin, dMax) ->
                if (dMin > cursor) result.add(WallSegment(seg.x1, cursor, seg.x1, dMin))
                cursor = maxOf(cursor, dMax)
            }
            if (cursor < segMax) result.add(WallSegment(seg.x1, cursor, seg.x1, segMax))
        } else {
            val segMin = minOf(seg.x1, seg.x2)
            val segMax = maxOf(seg.x1, seg.x2)
            val covered = overlappingDoors.map { minOf(it.x1, it.x2) to maxOf(it.x1, it.x2) }.sortedBy { it.first }
            var cursor = segMin
            covered.forEach { (dMin, dMax) ->
                if (dMin > cursor) result.add(WallSegment(cursor, seg.y1, dMin, seg.y1))
                cursor = maxOf(cursor, dMax)
            }
            if (cursor < segMax) result.add(WallSegment(cursor, seg.y1, segMax, seg.y1))
        }
    }
    return result
}

/**
 * Calculeaza poligonul de vizibilitate din punctul (originX, originY), limitat la viewRadius,
 * blocat de segmentele de perete date. Foloseste raycasting catre fiecare capat de perete
 * relevant (plus unghiuri usor deviate) si intersecteaza fiecare raza cu cel mai apropiat perete.
 */
private fun computeVisibilityPolygon(
    originX: Float,
    originY: Float,
    segments: List<WallSegment>,
    viewRadius: Float
): List<WorldPoint> {
    val relevantSegments = segments.filter { seg ->
        val midX = (seg.x1 + seg.x2) / 2f
        val midY = (seg.y1 + seg.y2) / 2f
        val dist = kotlin.math.hypot(midX - originX, midY - originY)
        dist < viewRadius * 1.5f
    }

    val angles = mutableListOf<Double>()
    relevantSegments.forEach { seg ->
        listOf(seg.x1 to seg.y1, seg.x2 to seg.y2).forEach { (px, py) ->
            val baseAngle = atan2((py - originY).toDouble(), (px - originX).toDouble())
            angles.add(baseAngle - 0.0001)
            angles.add(baseAngle)
            angles.add(baseAngle + 0.0001)
        }
    }
    // Adaugam si unghiuri uniforme ca fallback pentru zone fara pereti apropiati (camere deschise).
    val fallbackSteps = 60
    for (i in 0 until fallbackSteps) {
        angles.add(2.0 * Math.PI * i / fallbackSteps)
    }

    val points = mutableListOf<Pair<Double, WorldPoint>>()
    angles.forEach { angle ->
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

/**
 * Intersecteaza raza (origin + t*dir) cu segmentul dat. Returneaza distanta t (>=0) daca exista
 * intersectie in fata razei si in interiorul segmentului, altfel null.
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
    if (t2 < 0f || t2 > 1f) return null

    val t1 = if (kotlin.math.abs(dirX) > kotlin.math.abs(dirY)) {
        (x3 + t2 * (x4 - x3) - originX) / dirX
    } else {
        (y3 + t2 * (y4 - y3) - originY) / dirY
    }
    if (t1 < 0f) return null

    return t1
}