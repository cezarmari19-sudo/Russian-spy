package com.astran.russianspy.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/**
 * Inlocuieste vechiul HoldToCompleteTaskDialog (tinut apasat 2-10s). Pastreaza
 * exact aceeasi "carcasa" (fundal, titlu, descriere, buton Anuleaza) dar
 * randeaza un minigame DIFERIT in functie de taskType, in stil Among Us.
 * Fiecare minigame e calibrat sa dureze aproximativ cat `durationSeconds`
 * (acelasi numar care inainte controla progresul barei de "hold").
 *
 * Semnatura publica identica cu vechiul dialog, ca sa poata fi apelat la fel
 * din GameCanvasScreen.kt (doar numele fisierului/functiei difera).
 */
@Composable
fun HoldToCompleteTaskDialog(
    title: String,
    description: String,
    durationSeconds: Float,
    accentColor: Color,
    taskType: String = "",
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var isDone by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xE6000000)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            val wrappedOnComplete: () -> Unit = {
                if (!isDone) {
                    isDone = true
                    onComplete()
                }
            }

            // Ruteaza catre minigame-ul potrivit dupa taskType. Necunoscut sau
            // gol -> ramane mecanica originala de "hold to complete" (folosita
            // si pentru dezactivarea unui dispozitiv de catre un agent FBI).
            when (taskType) {
                "PHOTOGRAPH_DOCUMENTS" -> PhotographDocumentsMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "STEAL_KEYS" -> StealKeysMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "PLANT_LISTENING_DEVICE" -> PlantListeningDeviceMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "HACK_SURVEILLANCE_CAMERA" -> HackSurveillanceCameraMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SEND_ENCRYPTED_MESSAGE" -> SendEncryptedMessageMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "ERASE_FORENSIC_EVIDENCE" -> EraseForensicEvidenceMinigame(durationSeconds, accentColor, wrappedOnComplete)
                else -> HoldToCompleteCore(durationSeconds, accentColor, wrappedOnComplete)
            }

            Spacer(modifier = Modifier.height(28.dp))

            TextButton(onClick = onCancel) {
                Text("Anuleaza", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

private val NEON_GREEN = Color(0xFF4CD964)
private val NEON_RED = Color(0xFFB3261E)
private val WIRE_COLORS = listOf(
    Color(0xFFE53935), // rosu
    Color(0xFFFDD835), // galben
    Color(0xFF42A5F5), // albastru
    Color(0xFF66BB6A), // verde
)

// ---------------------------------------------------------------------------
// 1) PHOTOGRAPH_DOCUMENTS (~3s) - "Snap the frame": un cerc care se
//    micsoreaza spre un inel fix; apasa exact cand se suprapun.
// ---------------------------------------------------------------------------
@Composable
private fun PhotographDocumentsMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val totalShots = if (durationSeconds <= 2.5f) 2 else 3
    var shotsDone by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val roundMs = ((durationSeconds * 1000) / totalShots).toInt().coerceAtLeast(700)
    val targetRadius = 0.42f

    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(shotsDone) {
        if (shotsDone >= totalShots) {
            onComplete()
            return@LaunchedEffect
        }
        progress.snapTo(1f)
        progress.animateTo(0.06f, animationSpec = tween(roundMs, easing = LinearEasing))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Poza ${shotsDone.coerceAtMost(totalShots)}/$totalShots",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .clickable {
                    if (shotsDone >= totalShots) return@clickable
                    val diff = abs(progress.value - targetRadius)
                    if (diff < 0.09f) {
                        message = "Click!"
                        scope.launch { shotsDone += 1 }
                    } else {
                        message = "Prea devreme/tarziu"
                        scope.launch {
                            progress.snapTo(1f)
                            progress.animateTo(0.06f, animationSpec = tween(roundMs, easing = LinearEasing))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val maxR = size.minDimension / 2f - 6.dp.toPx()
                drawCircle(
                    color = accentColor.copy(alpha = 0.9f),
                    radius = maxR * targetRadius,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = maxR * progress.value,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            Text("📷", fontSize = 30.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (message.isEmpty()) "Apasa cand cercul atinge inelul" else message,
            color = if (message == "Click!") NEON_GREEN else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }

    LaunchedEffect(message) {
        if (message == "Click!") {
            delay(120)
            message = ""
        }
    }
}

// ---------------------------------------------------------------------------
// 2) STEAL_KEYS (~2s) - "Slide the key": tragi cheia de-a lungul unei sine;
//    o deviatie prea mare pe verticala te scoate de pe sina si o iei de la capat.
// ---------------------------------------------------------------------------
@Composable
private fun StealKeysMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var keyProgress by remember { mutableStateOf(0f) }
    var offTrack by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableStateOf(1f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Trage cheia pana la capatul sinei",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    trackWidthPx = size.width.toFloat()
                    detectDragGestures(
                        onDragEnd = {
                            if (keyProgress < 0.98f) {
                                keyProgress = 0f
                                offTrack = false
                            }
                        },
                        onDragCancel = {
                            keyProgress = 0f
                            offTrack = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val deltaFrac = dragAmount.x / trackWidthPx
                        keyProgress = (keyProgress + deltaFrac).coerceIn(0f, 1f)

                        val verticalDrift = abs(dragAmount.y)
                        if (verticalDrift > 30f) {
                            offTrack = true
                        }

                        if (keyProgress >= 0.98f && !offTrack) {
                            onComplete()
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            val fraction = keyProgress.coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth(1f)) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (fraction * (trackWidthPx - 96f)).toInt().coerceAtLeast(0),
                                0
                            )
                        }
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (offTrack) NEON_RED else accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔑", fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (offTrack) "Ai iesit de pe sina - ia-o de la capat" else "${(keyProgress * 100).toInt()}%",
            color = if (offTrack) NEON_RED else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------------------------
// 3) PLANT_LISTENING_DEVICE (~6s) - "Wires": conecteaza firele de aceeasi
//    culoare intre stanga si dreapta, tragand cu degetul.
// ---------------------------------------------------------------------------
@Composable
private fun PlantListeningDeviceMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val wireCount = if (durationSeconds <= 4f) 3 else 4
    val colors = remember { WIRE_COLORS.take(wireCount) }
    val rightOrder = remember { colors.shuffled(Random(System.nanoTime())) }

    var connections by remember { mutableStateOf(List(wireCount) { -1 }) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var boxSize by remember { mutableStateOf(Offset(1f, 1f)) }
    val leftSlotYs = remember(wireCount) { List(wireCount) { (it + 1f) / (wireCount + 1f) } }
    val rightSlotYs = leftSlotYs

    val allConnected = connections.all { it >= 0 } &&
        connections.withIndex().all { (li, ri) -> ri >= 0 && colors[li] == rightOrder[ri] }

    LaunchedEffect(allConnected) {
        if (allConnected) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Conecteaza firele de aceeasi culoare",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .pointerInput(wireCount) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val h = boxSize.y
                            var bestIdx: Int? = null
                            var bestDist = Float.MAX_VALUE
                            leftSlotYs.forEachIndexed { idx, frac ->
                                val slotPos = Offset(24.dp.toPx(), frac * h)
                                val d = (slotPos - start).getDistanceSquared()
                                if (d < bestDist && d < (60.dp.toPx() * 60.dp.toPx())) {
                                    bestDist = d
                                    bestIdx = idx
                                }
                            }
                            dragStartIndex = bestIdx
                            dragPos = start
                        },
                        onDragEnd = {
                            val startIdx = dragStartIndex
                            val pos = dragPos
                            if (startIdx != null && pos != null) {
                                val h = boxSize.y
                                var bestRight: Int? = null
                                var bestDist = Float.MAX_VALUE
                                rightSlotYs.forEachIndexed { idx, frac ->
                                    val slotPos = Offset(boxSize.x - 24.dp.toPx(), frac * h)
                                    val d = (slotPos - pos).getDistanceSquared()
                                    if (d < bestDist && d < (70.dp.toPx() * 70.dp.toPx())) {
                                        bestDist = d
                                        bestRight = idx
                                    }
                                }
                                connections = connections.toMutableList().also {
                                    it[startIdx] = bestRight ?: -1
                                }
                            }
                            dragStartIndex = null
                            dragPos = null
                        },
                        onDragCancel = {
                            dragStartIndex = null
                            dragPos = null
                        }
                    ) { change, _ ->
                        change.consume()
                        dragPos = change.position
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                boxSize = Offset(size.width, size.height)

                connections.forEachIndexed { li, ri ->
                    if (ri >= 0) {
                        val correct = colors[li] == rightOrder[ri]
                        val start = Offset(24.dp.toPx(), leftSlotYs[li] * size.height)
                        val end = Offset(size.width - 24.dp.toPx(), rightSlotYs[ri] * size.height)
                        drawLine(
                            color = if (correct) colors[li] else NEON_RED,
                            start = start,
                            end = end,
                            strokeWidth = 6.dp.toPx()
                        )
                    }
                }

                val sIdx = dragStartIndex
                val pos = dragPos
                if (sIdx != null && pos != null) {
                    val start = Offset(24.dp.toPx(), leftSlotYs[sIdx] * size.height)
                    drawLine(
                        color = colors[sIdx].copy(alpha = 0.8f),
                        start = start,
                        end = pos,
                        strokeWidth = 6.dp.toPx()
                    )
                }

                leftSlotYs.forEachIndexed { idx, frac ->
                    drawCircle(
                        color = colors[idx],
                        radius = 12.dp.toPx(),
                        center = Offset(24.dp.toPx(), frac * size.height)
                    )
                }
                rightSlotYs.forEachIndexed { idx, frac ->
                    drawCircle(
                        color = rightOrder[idx],
                        radius = 12.dp.toPx(),
                        center = Offset(size.width - 24.dp.toPx(), frac * size.height)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${connections.count { it >= 0 }}/$wireCount conectate",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------------------------
// 4) HACK_SURVEILLANCE_CAMERA (~10s) - "Simon Says": memoreaza si repeta o
//    secventie care creste cu 1 la fiecare runda reusita.
// ---------------------------------------------------------------------------
private data class SimonPad(val symbol: String, val color: Color)

@Composable
private fun HackSurveillanceCameraMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val pads = remember {
        listOf(
            SimonPad("▲", Color(0xFF42A5F5)),
            SimonPad("●", Color(0xFFE53935)),
            SimonPad("■", Color(0xFFFDD835)),
            SimonPad("◆", Color(0xFF66BB6A)),
        )
    }
    val targetRounds = if (durationSeconds <= 6f) 3 else 4

    var sequence by remember { mutableStateOf(listOf(Random.nextInt(4))) }
    var playerInputSize by remember { mutableStateOf(0) }
    var showingSequence by remember { mutableStateOf(true) }
    var highlightIndex by remember { mutableStateOf(-1) }
    var failed by remember { mutableStateOf(false) }
    var round by remember { mutableStateOf(1) }

    LaunchedEffect(sequence, failed) {
        if (failed) return@LaunchedEffect
        showingSequence = true
        playerInputSize = 0
        delay(400)
        for (idx in sequence) {
            highlightIndex = idx
            delay(420)
            highlightIndex = -1
            delay(180)
        }
        showingSequence = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (failed) "Gresit! Reincepe secventa" else "Runda $round/$targetRounds - ${if (showingSequence) "Urmareste" else "Repeta secventa"}",
            color = if (failed) NEON_RED else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0..1) {
                        val idx = row * 2 + col
                        val pad = pads[idx]
                        val isLit = highlightIndex == idx
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isLit) pad.color else pad.color.copy(alpha = 0.25f))
                                .clickable(enabled = !showingSequence && !failed) {
                                    val posInSeq = playerInputSize
                                    if (sequence[posInSeq] != idx) {
                                        failed = true
                                        return@clickable
                                    }
                                    val newSize = playerInputSize + 1
                                    playerInputSize = newSize
                                    if (newSize == sequence.size) {
                                        if (round >= targetRounds) {
                                            onComplete()
                                        } else {
                                            round += 1
                                            sequence = sequence + Random.nextInt(4)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pad.symbol, fontSize = 26.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        if (failed) {
            Text("Reincepe in scurt timp...", color = NEON_RED, fontSize = 12.sp)
            LaunchedEffect(Unit) {
                delay(900)
                round = 1
                sequence = listOf(Random.nextInt(4))
                failed = false
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5) SEND_ENCRYPTED_MESSAGE (~5s) - "Tap the code": apasa cifrele afisate,
//    in ordine, pe o grila numerica amestecata.
// ---------------------------------------------------------------------------
@Composable
private fun SendEncryptedMessageMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val codeLength = if (durationSeconds <= 4f) 4 else 5
    val code = remember { List(codeLength) { Random.nextInt(1, 10) } }
    val pad = remember { (1..9).toList().shuffled(Random(System.nanoTime())) }
    var enteredCount by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(enteredCount) {
        if (enteredCount >= code.size) {
            delay(120)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Introdu codul cifrat in ordine",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            code.forEachIndexed { i, digit ->
                val isDone = i < enteredCount
                val isNext = i == enteredCount
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isDone -> NEON_GREEN.copy(alpha = 0.25f)
                                isNext -> accentColor.copy(alpha = 0.3f)
                                else -> Color.White.copy(alpha = 0.06f)
                            }
                        )
                        .border(
                            1.dp,
                            if (isNext) accentColor else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDone || isNext) digit.toString() else "•",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..2) {
                        val n = pad[row * 3 + col]
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    if (enteredCount < code.size && n == code[enteredCount]) {
                                        enteredCount += 1
                                    } else {
                                        wrongFlash = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(n.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Cifra gresita" else "Apasa cifra evidentiata mai sus, in ordine",
            color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(400)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 6) ERASE_FORENSIC_EVIDENCE (~8s) - "Wipe the stains": tragi degetul peste
//    petele desenate pana le acoperi pe toate.
// ---------------------------------------------------------------------------
@Composable
private fun EraseForensicEvidenceMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val stainCount = if (durationSeconds <= 6f) 5 else 7
    val stains = remember {
        List(stainCount) {
            Offset(Random.nextFloat() * 0.8f + 0.1f, Random.nextFloat() * 0.8f + 0.1f)
        }.toMutableStateList()
    }
    var boxSizePx by remember { mutableStateOf(Offset(1f, 1f)) }
    var cleanedCount by remember { mutableStateOf(0) }
    val wipeRadius = 42.dp

    LaunchedEffect(cleanedCount) {
        if (cleanedCount >= stainCount) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Sterge toate petele (trage degetul peste ele)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .pointerInput(stainCount) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val pos = change.position
                        val wipeRPx = wipeRadius.toPx()
                        val iterator = stains.listIterator()
                        while (iterator.hasNext()) {
                            val s = iterator.next()
                            val stainPx = Offset(s.x * boxSizePx.x, s.y * boxSizePx.y)
                            if ((stainPx - pos).getDistance() < wipeRPx) {
                                iterator.remove()
                                cleanedCount += 1
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                boxSizePx = Offset(size.width, size.height)
                stains.forEach { s ->
                    drawCircle(
                        color = accentColor.copy(alpha = 0.55f),
                        radius = 16.dp.toPx(),
                        center = Offset(s.x * size.width, s.y * size.height)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "$cleanedCount/$stainCount curatate",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Mecanica originala "hold to complete" - pastrata ca fallback pentru orice
// taskType necunoscut si pentru dezactivarea unui dispozitiv de catre agentul
// FBI (are sens fizic: agentul smulge dispozitivul, nu rezolva un puzzle).
// ---------------------------------------------------------------------------
@Composable
private fun HoldToCompleteCore(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(160.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isDone) return@detectTapGestures
                        isHolding = true
                        val job = scope.launch {
                            progress.animateTo(
                                1f,
                                animationSpec = tween((durationSeconds * 1000).toInt(), easing = LinearEasing)
                            )
                            if (progress.value >= 0.999f) {
                                isDone = true
                                onComplete()
                            }
                        }
                        tryAwaitRelease()
                        isHolding = false
                        if (!isDone) {
                            job.cancel()
                            scope.launch { progress.snapTo(0f) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.minDimension / 2 - 8.dp.toPx(),
                style = Stroke(width = 8.dp.toPx())
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx()),
                topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx())
            )
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(if (isHolding) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDone) "✓" else "${(progress.value * 100).toInt()}%",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Tine apasat pana se umple",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 13.sp
    )
}