package com.astran.russianspy.ui

import com.astran.russianspy.model.Room
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------------
// Sistem de vizibilitate (raycasting), stil Among Us: raza fixa, blocata de pereti.
// Folosit atat de harta jocului (GameCanvasScreen) cat si de camerele de
// supraveghere (SurveillanceMonitorsScreen), ca ambele sa "vada" identic -
// o camera de supraveghere e tratata exact ca un jucator care nu se misca.
//
// Abordare:
// 1. Camerele sunt unite intr-o singura zona continua folosind un grid boolean
//    (union geometrica pe celule), asa ca nu mai depindem de compararea exacta
//    a coordonatelor de tip Float intre camere adiacente.
// 2. Peretii reali sunt extrasi ca marginile exterioare ale zonei unite -
//    orice granita dintre doua camere care se ating/suprapun dispare automat.
// 3. Raycasting-ul foloseste un epsilon relativ si sorteaza punctele dupa unghi,
//    ca sa evite crestaturile ascutite intre raze cu unghiuri aproape identice.
// ---------------------------------------------------------------------------------

const val VIEW_RADIUS = 420f // raza de vizibilitate in unitati "world", stil Among Us

data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
data class WorldPoint(val x: Float, val y: Float)

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
 *
 * Folosita si pentru jucator (originea se misca) si pentru camerele de supraveghere
 * (originea e fixa, camera "e" un jucator care nu se misca niciodata).
 */
fun computeVisibilityPolygon(
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

/**
 * Verifica daca punctul (px, py) e vizibil din (originX, originY) - adica in raza
 * de vizibilitate SI fara niciun perete intre ele. Functie PUBLICA, comuna intre
 * GameCanvasScreen (jucatorul vede alti jucatori pe harta) si
 * SurveillanceMonitorsScreen (camera de supraveghere vede jucatorii din cadru).
 */
fun isPointVisibleFromPoint(
    px: Float,
    py: Float,
    originX: Float,
    originY: Float,
    segments: List<WallSegment>,
    viewRadius: Float
): Boolean {
    val dx = px - originX
    val dy = py - originY
    val dist = kotlin.math.hypot(dx, dy)
    if (dist > viewRadius) return false
    if (dist < 0.001f) return true

    val dirX = dx / dist
    val dirY = dy / dist

    segments.forEach { seg ->
        val t = raySegmentIntersection(originX, originY, dirX, dirY, seg)
        // Daca exista un perete intersectat MAI APROAPE decat punctul tinta, e ascuns.
        if (t != null && t < dist - 0.5f) return false
    }
    return true
}