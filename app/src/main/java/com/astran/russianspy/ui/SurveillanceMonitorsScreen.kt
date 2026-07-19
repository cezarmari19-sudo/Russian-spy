package com.astran.russianspy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.viewmodel.GameViewModel
import com.astran.russianspy.viewmodel.LivePosition
import com.astran.russianspy.viewmodel.SurveillanceCameraSpot
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SurveillanceMonitorsScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val playerLivePositions = viewModel.playerLivePositions
    val playerNames = viewModel.playerNames
    val cameraSpots = viewModel.surveillanceCameraSpots

    // Segmentele de perete, calculate o singura data - identice cu cele folosite
    // pe harta jocului, ca fiecare camera de supraveghere sa "vada" exact ca un
    // jucator plasat in acel punct fix (blocat de aceiasi pereti).
    val wallSegments = remember { buildWallSegmentsFromMergedRooms(BuildingLayout.rooms) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050505)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            SurveillanceHeader(onExit = onExit)

            Spacer(modifier = Modifier.height(10.dp))

            if (cameraSpots.size < 4) {
                // Pozitiile camerelor nu au fost inca primite/generate pentru runda asta.
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "SE CONECTEAZA LA CAMERE...",
                        color = Color(0xFF3DDC5A),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonitorPanel(
                        label = "CAM 1",
                        spot = cameraSpots[0],
                        wallSegments = wallSegments,
                        playerLivePositions = playerLivePositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        label = "CAM 2",
                        spot = cameraSpots[1],
                        wallSegments = wallSegments,
                        playerLivePositions = playerLivePositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonitorPanel(
                        label = "CAM 3",
                        spot = cameraSpots[2],
                        wallSegments = wallSegments,
                        playerLivePositions = playerLivePositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MonitorPanel(
                        label = "CAM 4",
                        spot = cameraSpots[3],
                        wallSegments = wallSegments,
                        playerLivePositions = playerLivePositions,
                        playerNames = playerNames,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun SurveillanceHeader(onExit: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF1744).copy(alpha = recAlpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC",
                color = Color(0xFFFF1744).copy(alpha = recAlpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "CAMERE DE SUPRAVEGHERE",
                color = Color(0xFF3DDC5A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        TextButton(onClick = onExit) {
            Text("IESIRE", color = Color(0xFFAAAAAA), fontFamily = FontFamily.Monospace)
        }
    }
}

/**
 * Un singur monitor de supraveghere: bezel (cadru fizic) + ecran CRT cu vederea REALA
 * a camerei din punctul ei fix - exact ca un jucator care sta nemiscat in acel punct:
 * raza limitata (VIEW_RADIUS) si blocata de pereti (nu vede prin ei), nu tot dreptunghiul
 * camerei intregi.
 */
@Composable
private fun MonitorPanel(
    label: String,
    spot: SurveillanceCameraSpot,
    wallSegments: List<WallSegment>,
    playerLivePositions: Map<String, LivePosition>,
    playerNames: Map<String, String>,
    modifier: Modifier = Modifier
) {
    // Poligonul de vizibilitate al camerei, recalculat doar cand se schimba pozitia
    // camerei (o data pe runda), nu la fiecare cadru - camera sta pe loc.
    val visibilityPolygon = remember(spot.x, spot.y) {
        computeVisibilityPolygon(
            originX = spot.x,
            originY = spot.y,
            segments = wallSegments,
            viewRadius = VIEW_RADIUS
        )
    }

    // Zgomot / static animat, ca un feed video real usor instabil.
    val infinite = rememberInfiniteTransition(label = "static_${spot.roomId}")
    val staticPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "static_phase"
    )
    val scanlineOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_offset"
    )

    Box(
        modifier = modifier
            .background(Color(0xFF1B1B1B), RoundedCornerShape(10.dp))
            .padding(7.dp)
    ) {
        // --- Bezel-ul monitorului (rama fizica, cu "suruburi") ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black, RoundedCornerShape(4.dp))
                .padding(3.dp)
        ) {
            // Jucatorii aflati in raza de vizibilitate REALA a camerei (distanta +
            // vizibilitate directa, nu doar "aceeasi camera din harta").
            val visiblePlayers = playerLivePositions.entries.filter { (_, pos) ->
                isPointVisibleFromPoint(pos.x, pos.y, spot.x, spot.y, wallSegments, VIEW_RADIUS)
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCameraFeed(
                    spot = spot,
                    visibilityPolygon = visibilityPolygon,
                    players = visiblePlayers,
                    playerNames = playerNames,
                    staticPhase = staticPhase,
                    scanlineOffset = scanlineOffset,
                    seed = spot.roomId.hashCode()
                )
            }

            // Suruburile bezel-ului, in cele 4 colturi.
            listOf(
                Alignment.TopStart, Alignment.TopEnd,
                Alignment.BottomStart, Alignment.BottomEnd
            ).forEach { corner ->
                Box(
                    modifier = Modifier
                        .align(corner)
                        .padding(4.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF3A3A3A))
                )
            }

            // Eticheta camerei, in coltul din stanga-sus, stil overlay CCTV.
            Text(
                text = "CAM // $label",
                color = Color(0xFF3DDC5A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )

            // Cate persoane sunt vizibile acum in acest cadru, coltul din dreapta-sus.
            Text(
                text = "${visiblePlayers.size}",
                color = Color(0xFF3DDC5A).copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

/** Deseneaza feed-ul complet al unei camere fixe: podea vizibila (raycasting), jucatori, efecte CRT. */
private fun DrawScope.drawCameraFeed(
    spot: SurveillanceCameraSpot,
    visibilityPolygon: List<WorldPoint>,
    players: List<Map.Entry<String, LivePosition>>,
    playerNames: Map<String, String>,
    staticPhase: Float,
    scanlineOffset: Float,
    seed: Int
) {
    // Scala aleasa ca raza de vizibilitate (VIEW_RADIUS) sa umple cadrul monitorului.
    val scale = minOf(size.width, size.height) / (VIEW_RADIUS * 2f)

    fun worldToScreen(wx: Float, wy: Float): Offset {
        val dx = (wx - spot.x) * scale
        val dy = (wy - spot.y) * scale
        return Offset(size.width / 2f + dx, size.height / 2f + dy)
    }

    // Fundal complet negru - orice nu e in poligonul de vizibilitate ramane nevazut,
    // exact ca la jucator pe harta (camera nu vede prin pereti).
    drawRect(color = Color.Black, topLeft = Offset.Zero, size = size)

    val room = BuildingLayout.getRoomAtPoint(spot.x, spot.y)
    val floorColor = if (room != null) roomFloorColor(room) else Color(0xFF14171A)

    val visibilityPathScreen = Path().apply {
        if (visibilityPolygon.isNotEmpty()) {
            val first = worldToScreen(visibilityPolygon[0].x, visibilityPolygon[0].y)
            moveTo(first.x, first.y)
            for (i in 1 until visibilityPolygon.size) {
                val p = worldToScreen(visibilityPolygon[i].x, visibilityPolygon[i].y)
                lineTo(p.x, p.y)
            }
            close()
        }
    }

    // Tot ce desenam mai jos e "taiat" la forma poligonului vizibil - restul ramane negru,
    // exact ca vederea unui jucator.
    clipPath(visibilityPathScreen) {
        // 1) Podeaua zonei vizibile.
        drawRect(color = floorColor, topLeft = Offset.Zero, size = size)

        // 2) Grid de podea, ca sa se vada scara si miscarea, exact stilul Among Us.
        val gridStep = 26f
        val gridColor = Color.White.copy(alpha = 0.045f)
        var gx = 0f
        while (gx < size.width) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
            gx += gridStep
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            gy += gridStep
        }

        // 3) Jucatorii vizibili in cadru - fiecare cu umbra, marker si eticheta cu numele.
        //    NOTE PENTRU DEZVOLTATOR: aici e locul unde se randeaza avatarul fiecarui jucator.
        //    Momentan e un PLACEHOLDER simplu (cerc + contur) - va fi inlocuit cu avatarul
        //    custom facut de tine. Cauta functia drawPlayerPlaceholder() mai jos.
        players.forEach { (playerId, pos) ->
            val screenPos = worldToScreen(pos.x, pos.y)
            val color = colorForPlayer(playerId)
            drawPlayerPlaceholder(center = screenPos, color = color)
        }
    }

    // 4) Conturul poligonului de vizibilitate (marginea dintre vazut/nevazut), subtil.
    drawPath(
        path = visibilityPathScreen,
        color = Color(0xFF3DDC5A).copy(alpha = 0.12f),
        style = Stroke(width = 1.5f)
    )

    // 5) Static / zgomot de semnal - puncte aleatorii care clipesc, schimbate rapid.
    drawSignalStatic(phase = staticPhase, seed = seed, alpha = 0.05f)

    // 6) Scanlines orizontale (linii fine, tipice pentru un ecran CRT).
    drawScanlines(offset = scanlineOffset)

    // 7) Vigneta - marginile mai intunecate, ca lumina sa cada spre centrul cadrului.
    drawVignette()

    // 8) Un foarte usor "flicker" verzui de semnal, ca sa nu para o imagine statica perfecta.
    drawRect(
        color = Color(0xFF00FF66).copy(alpha = 0.02f + 0.015f * sin(staticPhase * 6.283f)),
        topLeft = Offset.Zero,
        size = size
    )
}

/**
 * PLACEHOLDER pentru avatarul jucatorului pe camera. Momentan doar un cerc plin cu
 * un contur - usor de gasit si inlocuit cand vine avatarul custom facut separat
 * (evita orice design/vizor/forma copiat dupa alt joc).
 */
private fun DrawScope.drawPlayerPlaceholder(center: Offset, color: Color) {
    // Umbra la "picioare".
    drawOval(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(center.x - 10f, center.y + 8f),
        size = Size(20f, 8f)
    )

    // --- TODO: AVATAR AICI ---
    // Inlocuieste blocul de mai jos cu desenul avatarului tau custom (functie separata).
    drawCircle(color = color, radius = 11f, center = center)
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 11f,
        center = center,
        style = Stroke(width = 1.5f)
    )
    // --- SFARSIT TODO ---
}

/** Genereaza o culoare stabila si distincta pentru fiecare jucator, pe baza id-ului lui. */
private fun colorForPlayer(playerId: String): Color {
    val palette = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
        Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00),
        Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41),
        Color(0xFFC0CA33)
    )
    val idx = (playerId.hashCode().absoluteValue) % palette.size
    return palette[idx]
}

private fun roomFloorColor(room: Room): Color {
    return when (room.function) {
        RoomFunction.HALLWAY -> Color(0xFF14171A)
        RoomFunction.HUB -> Color(0xFF1B1E22)
        RoomFunction.ENTRANCE -> Color(0xFF20282C)
        RoomFunction.SURVEILLANCE -> Color(0xFF221331)
        RoomFunction.FORENSICS_LAB -> Color(0xFF0F2636)
        RoomFunction.ARMORY -> Color(0xFF331420)
        RoomFunction.SERVER_ROOM -> Color(0xFF0E2A17)
        RoomFunction.OFFICE -> Color(0xFF1A2124)
        RoomFunction.BREAK_ROOM -> Color(0xFF2A1E19)
        RoomFunction.COMMS_MONITOR -> Color(0xFF33300F)
        RoomFunction.MEETING_ROOM -> Color(0xFF361212)
    }
}

/** Puncte de static/zgomot, repozitionate pseudo-aleator la fiecare "faza" pentru senzatia de semnal video. */
private fun DrawScope.drawSignalStatic(phase: Float, seed: Int, alpha: Float) {
    val rnd = Random(seed + (phase * 997).toInt())
    val dotCount = 40
    repeat(dotCount) {
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height
        val a = rnd.nextFloat() * alpha
        drawRect(
            color = Color.White.copy(alpha = a),
            topLeft = Offset(x, y),
            size = Size(1.5f, 1.5f)
        )
    }
}

/** Linii orizontale fine, deplasate lent pe verticala - efectul clasic de scanline CRT. */
private fun DrawScope.drawScanlines(offset: Float) {
    val lineSpacing = 4f
    var y = -lineSpacing + (offset % lineSpacing)
    val color = Color.Black.copy(alpha = 0.16f)
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += lineSpacing
    }
}

/** Vigneta radiala - margini mai intunecate, centru mai luminos, tipic pentru feed CCTV. */
private fun DrawScope.drawVignette() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width.coerceAtLeast(size.height) * 0.75f
        ),
        topLeft = Offset.Zero,
        size = size
    )
}