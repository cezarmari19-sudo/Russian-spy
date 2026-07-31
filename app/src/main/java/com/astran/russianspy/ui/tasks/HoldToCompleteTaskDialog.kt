package com.astran.russianspy.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.sin
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
                "CHECK_EVIDENCE_LOCKER" -> CheckEvidenceLockerMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "FILE_INCIDENT_REPORT" -> FileIncidentReportMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "INSPECT_BADGE_SCANNER" -> InspectBadgeScannerMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "CALIBRATE_METAL_DETECTOR" -> CalibrateMetalDetectorMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "REVIEW_PERSONNEL_FILES" -> ReviewPersonnelFilesMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "BUG_PHONE_LINE" -> BugPhoneLineMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "COPY_KEYCARD" -> CopyKeycardMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "BRIBE_GUARD" -> BribeGuardMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SABOTAGE_ALARM" -> SabotageAlarmMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SMUGGLE_WEAPON" -> SmuggleWeaponMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "DECODE_INTERCEPT" -> DecodeInterceptMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "FORGE_SIGNATURE" -> ForgeSignatureMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SEARCH_FILES" -> SearchFilesMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "TAMPER_DNA_SAMPLE" -> TamperDnaSampleMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SWAP_DNA_LABEL" -> SwapDnaLabelMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "POISON_COFFEE" -> PoisonCoffeeMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "EAVESDROP_CONVERSATION" -> EavesdropConversationMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "UPLOAD_VIRUS" -> UploadVirusMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "DISPOSE_BODY_EVIDENCE" -> DisposeBodyEvidenceMinigame(durationSeconds, accentColor, wrappedOnComplete)
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
// 7) CHECK_EVIDENCE_LOCKER (~3s) - "Seal check": apasa doar cutiile cu sigiliu
//    intact (verde), evita-le pe cele rupte (rosu), pana le bifezi pe toate
//    cele bune.
// ---------------------------------------------------------------------------
@Composable
private fun CheckEvidenceLockerMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val boxCount = 6
    val sealedFlags = remember { List(boxCount) { Random.nextBoolean() }.let { flags ->
        // garanteaza cel putin 2 sigilate si 2 rupte, ca sa fie interesant
        if (flags.count { it } < 2 || flags.count { !it } < 2) {
            (0 until boxCount).map { it % 2 == 0 }.shuffled(Random(System.nanoTime()))
        } else flags
    } }
    var checked by remember { mutableStateOf(List(boxCount) { false }) }
    var wrongFlash by remember { mutableStateOf(false) }

    val goodCount = sealedFlags.count { it }
    val goodChecked = checked.indices.count { checked[it] && sealedFlags[it] }

    LaunchedEffect(goodChecked) {
        if (goodChecked >= goodCount) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Bifeaza doar cutiile cu sigiliu intact",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        val isSealed = sealedFlags[idx]
                        val isChecked = checked[idx]
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isChecked && isSealed -> NEON_GREEN.copy(alpha = 0.3f)
                                        isChecked && !isSealed -> NEON_RED.copy(alpha = 0.3f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    }
                                )
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .clickable(enabled = !isChecked) {
                                    checked = checked.toMutableList().also { it[idx] = true }
                                    if (!isSealed) {
                                        wrongFlash = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (!isChecked) "📦" else if (isSealed) "✓" else "✗",
                                fontSize = 22.sp,
                                color = if (isChecked && !isSealed) NEON_RED else Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (wrongFlash) "Sigiliu rupt - alege alta cutie" else "$goodChecked/$goodCount sigilii confirmate",
            color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(500)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 8) FILE_INCIDENT_REPORT (~5s) - "Fill the form": apasa in ordine casutele
//    de bifat ale formularului, de sus in jos.
// ---------------------------------------------------------------------------
@Composable
private fun FileIncidentReportMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val fields = listOf("Data", "Locatie", "Ofiter", "Descriere", "Semnatura")
    var filledCount by remember { mutableStateOf(0) }

    LaunchedEffect(filledCount) {
        if (filledCount >= fields.size) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Completeaza rubricile raportului, in ordine",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            fields.forEachIndexed { i, label ->
                val isDone = i < filledCount
                val isNext = i == filledCount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isNext) { filledCount += 1 }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isDone) NEON_GREEN.copy(alpha = 0.4f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (isNext) accentColor else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) Text("✓", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        color = if (isNext) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "$filledCount/${fields.size} completate",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------------------------
// 9) INSPECT_BADGE_SCANNER (~2s) - "Swipe the badge": trage insigna prin
//    fanta scanerului cu viteza potrivita, nici prea incet, nici prea repede.
// ---------------------------------------------------------------------------
@Composable
private fun InspectBadgeScannerMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var badgeProgress by remember { mutableStateOf(0f) }
    var trackWidthPx by remember { mutableStateOf(1f) }
    var message by remember { mutableStateOf("") }
    var lastDragTime by remember { mutableStateOf(0L) }
    var tooFast by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Trage insigna prin scaner, cu viteza constanta",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    trackWidthPx = size.width.toFloat()
                    detectDragGestures(
                        onDragStart = { lastDragTime = System.currentTimeMillis() },
                        onDragEnd = {
                            if (badgeProgress < 0.98f) {
                                badgeProgress = 0f
                                message = ""
                            }
                        },
                        onDragCancel = {
                            badgeProgress = 0f
                            message = ""
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val now = System.currentTimeMillis()
                        val dt = (now - lastDragTime).coerceAtLeast(1)
                        lastDragTime = now
                        val speed = abs(dragAmount.x) / dt // px per ms
                        tooFast = speed > 3.5f

                        if (!tooFast) {
                            val deltaFrac = dragAmount.x / trackWidthPx
                            badgeProgress = (badgeProgress + deltaFrac).coerceIn(0f, 1f)
                            if (badgeProgress >= 0.98f) {
                                message = "Scanat!"
                                onComplete()
                            }
                        } else {
                            message = "Prea repede"
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(accentColor.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (badgeProgress * (trackWidthPx - 90f)).toInt().coerceAtLeast(0),
                            0
                        )
                    }
                    .size(width = 70.dp, height = 44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (tooFast) NEON_RED else accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text("🪪", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (message.isNotEmpty()) message else "${(badgeProgress * 100).toInt()}%",
            color = if (tooFast) NEON_RED else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }

    LaunchedEffect(tooFast) {
        if (tooFast) {
            delay(400)
            tooFast = false
            message = ""
        }
    }
}

// ---------------------------------------------------------------------------
// 10) CALIBRATE_METAL_DETECTOR (~6s) - "Hit the zone": un ac oscileaza pe un
//     cadran; apasa cand acul e in zona verde, de mai multe ori la rand.
// ---------------------------------------------------------------------------
@Composable
private fun CalibrateMetalDetectorMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val targetHits = if (durationSeconds <= 4f) 3 else 4
    var hits by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val needle = remember { Animatable(0f) }
    val zoneCenter = 0.5f
    val zoneHalfWidth = 0.12f
    val scope = rememberCoroutineScope()

    LaunchedEffect(hits) {
        if (hits >= targetHits) {
            onComplete()
            return@LaunchedEffect
        }
        needle.snapTo(0f)
        needle.animateTo(
            1f,
            animationSpec = tween((1400 - hits * 120).coerceAtLeast(700), easing = LinearEasing)
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Apasa cand acul e in zona verde ($hits/$targetHits)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .clickable {
                    // acul merge 0->1->0 pe durata tween-ului; folosim o functie
                    // triunghi din valoarea animata pentru pozitia vizuala reala
                    val raw = needle.value
                    val pos = if (raw <= 1f) raw else 1f
                    val triangular = if (pos < 0.5f) pos * 2f else (1f - pos) * 2f
                    val inZone = abs(triangular - zoneCenter) < zoneHalfWidth
                    if (inZone) {
                        message = "Bun!"
                        scope.launch { hits += 1 }
                    } else {
                        message = "Rateaza - reincearca"
                        hits = 0
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = size.height
                val w = size.width
                // fundal
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                // zona tinta (verde)
                val zoneStart = (zoneCenter - zoneHalfWidth) * w
                val zoneWidth = (zoneHalfWidth * 2f) * w
                drawRoundRect(
                    color = NEON_GREEN.copy(alpha = 0.35f),
                    topLeft = Offset(zoneStart, 0f),
                    size = Size(zoneWidth, h),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // pozitia acului (functie triunghi: 0 -> w -> 0)
                val raw = needle.value.coerceIn(0f, 1f)
                val triangular = if (raw < 0.5f) raw * 2f else (1f - raw) * 2f
                val needleX = triangular * w
                drawLine(
                    color = accentColor,
                    start = Offset(needleX, 0f),
                    end = Offset(needleX, h),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message.ifEmpty { "Apasa ecranul cand acul trece prin verde" },
            color = if (message == "Bun!") NEON_GREEN else if (message.startsWith("Rateaza")) NEON_RED else Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
    }

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            delay(500)
            message = ""
        }
    }
}

// ---------------------------------------------------------------------------
// 11) REVIEW_PERSONNEL_FILES (~4s) - "Odd one out": gaseste dosarul cu numarul
//     diferit de restul, de cateva ori la rand.
// ---------------------------------------------------------------------------
@Composable
private fun ReviewPersonnelFilesMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val totalRounds = if (durationSeconds <= 3f) 2 else 3
    var round by remember { mutableStateOf(1) }
    var wrongFlash by remember { mutableStateOf(false) }

    // 6 dosare, toate cu acelasi numar de referinta, unul singur diferit
    var oddIndex by remember(round) { mutableStateOf(Random.nextInt(6)) }
    val baseNumber = remember(round) { Random.nextInt(100, 900) }
    val oddNumber = remember(round) { baseNumber + (Random.nextInt(1, 9)) }

    LaunchedEffect(round) {
        if (round > totalRounds) {
            onComplete()
        }
    }

    if (round > totalRounds) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste dosarul cu numarul diferit (runda $round/$totalRounds)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        val isOdd = idx == oddIndex
                        val displayNumber = if (isOdd) oddNumber else baseNumber
                        Box(
                            modifier = Modifier
                                .size(width = 76.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isOdd) {
                                        round += 1
                                    } else {
                                        wrongFlash = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$displayNumber",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (wrongFlash) "Nu e acesta - mai incearca" else "Compara numerele de pe dosare",
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
// 12) BUG_PHONE_LINE (~5s) - "Tune the frequency": misca un slider pana acul
//     intra si ramane in zona verde un timp cumulat suficient.
// ---------------------------------------------------------------------------
@Composable
private fun BugPhoneLineMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var sliderPos by remember { mutableStateOf(0f) } // 0..1
    var heldMs by remember { mutableStateOf(0f) }
    val targetCenter = remember { 0.25f + Random.nextFloat() * 0.5f }
    val targetHalfWidth = 0.06f
    val neededMs = 900f

    val inZone = abs(sliderPos - targetCenter) < targetHalfWidth

    LaunchedEffect(inZone) {
        while (inZone) {
            delay(50)
            heldMs += 50f
            if (heldMs >= neededMs) {
                onComplete()
                break
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Regleaza frecventa pana semnalul e stabil",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(50.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                val zoneStart = (targetCenter - targetHalfWidth) * w
                val zoneWidth = targetHalfWidth * 2f * w
                drawRoundRect(
                    color = NEON_GREEN.copy(alpha = 0.35f),
                    topLeft = Offset(zoneStart, 0f),
                    size = Size(zoneWidth, h),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                val needleX = sliderPos * w
                drawLine(
                    color = if (inZone) NEON_GREEN else accentColor,
                    start = Offset(needleX, 0f),
                    end = Offset(needleX, h),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        Slider(
            value = sliderPos,
            onValueChange = { sliderPos = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (heldMs / neededMs).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = NEON_GREEN,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

// ---------------------------------------------------------------------------
// 13) COPY_KEYCARD (~3s) - "Match the pattern": memoreaza o secventa de 4
//     simboluri si reproduce-o apasand in ordine.
// ---------------------------------------------------------------------------
@Composable
private fun CopyKeycardMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val symbols = listOf("◆", "●", "▲", "■", "★", "✦")
    val sequence = remember { (0 until 4).map { symbols.random() } }
    var showingIndex by remember { mutableStateOf(0) }
    var revealing by remember { mutableStateOf(true) }
    var inputIndex by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (i in sequence.indices) {
            showingIndex = i
            delay(500)
        }
        revealing = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (revealing) "Memoreaza secventa cartelei" else "Reproduce secventa, in ordine",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sequence.forEachIndexed { i, sym ->
                val isLit = revealing && i == showingIndex
                val isDone = !revealing && i < inputIndex
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isLit -> accentColor.copy(alpha = 0.6f)
                                isDone -> NEON_GREEN.copy(alpha = 0.3f)
                                else -> Color.White.copy(alpha = 0.08f)
                            }
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (revealing || isDone) sym else "?",
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
            }
        }

        if (!revealing) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                symbols.forEach { sym ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable {
                                if (inputIndex < sequence.size) {
                                    if (sym == sequence[inputIndex]) {
                                        inputIndex += 1
                                        if (inputIndex >= sequence.size) onComplete()
                                    } else {
                                        wrongFlash = true
                                        inputIndex = 0
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = sym, fontSize = 20.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (wrongFlash) "Gresit - ia-o de la capat" else "$inputIndex/${sequence.size}",
                color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(450)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 14) BRIBE_GUARD (~4s) - "Say the right thing": alege replica potrivita, de
//     3 ori la rand, ca sa convingi garda.
// ---------------------------------------------------------------------------
@Composable
private fun BribeGuardMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val prompts = listOf(
        Triple("Garda intreaba de legitimatie.", "Ii arati un teanc de bani", "Fugi imediat"),
        Triple("Garda pare suspicioasa.", "Vorbesti calm si increzator", "Te bagi in panica"),
        Triple("Garda ezita.", "Ii mai oferi ceva in plus", "Ridici tonul")
    )
    var round by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(round) {
        if (round >= prompts.size) onComplete()
    }
    if (round >= prompts.size) return

    val (situation, goodOption, badOption) = prompts[round]
    val goodFirst = remember(round) { Random.nextBoolean() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Convinge garda (runda ${round + 1}/${prompts.size})",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = situation,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))

        val options = if (goodFirst) listOf(goodOption to true, badOption to false)
        else listOf(badOption to false, goodOption to true)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (label, isGood) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable {
                            if (isGood) {
                                round += 1
                            } else {
                                wrongFlash = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(text = label, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Nu a mers - garda e mai atenta acum" else " ",
            color = NEON_RED,
            fontSize = 12.sp
        )
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(450)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 15) SABOTAGE_ALARM (~6s) - "Cut the right wire": la fel ca la wires clasic,
//     dar cu penalizare mai mare daca gresesti (reset complet).
// ---------------------------------------------------------------------------
@Composable
private fun SabotageAlarmMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val wireCount = 5
    val colors = remember {
        listOf(NEON_RED, NEON_GREEN, Color(0xFF2196F3), Color(0xFFFFEB3B), Color(0xFFFF9800))
            .shuffled()
    }
    val correctPairs = remember { (0 until wireCount).shuffled() }
    var connectedCount by remember { mutableStateOf(0) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var connections by remember { mutableStateOf(mapOf<Int, Int>()) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(connectedCount) {
        if (connectedCount >= wireCount) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Conecteaza firele de aceeasi culoare ca sa taie alarma",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(220.dp)
        ) {
            val leftSlotYs = (0 until wireCount).map { (it + 1f) / (wireCount + 1f) }
            val rightSlotYs = correctPairs.map { (it + 1f) / (wireCount + 1f) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val idx = leftSlotYs.indexOfFirst {
                                    abs(pos.y - it * size.height) < 40f && pos.x < size.width * 0.3f
                                }
                                if (idx >= 0 && !connections.containsKey(idx)) {
                                    dragStartIndex = idx
                                    dragPos = pos
                                }
                            },
                            onDrag = { change, _ -> dragPos = change.position },
                            onDragEnd = {
                                val sIdx = dragStartIndex
                                val pos = dragPos
                                if (sIdx != null && pos != null) {
                                    val rIdx = rightSlotYs.indexOfFirst {
                                        abs(pos.y - it * size.height) < 40f && pos.x > size.width * 0.7f
                                    }
                                    if (rIdx >= 0) {
                                        if (correctPairs[rIdx] == sIdx) {
                                            connections = connections + (sIdx to rIdx)
                                            connectedCount += 1
                                        } else {
                                            wrongFlash = true
                                            connections = emptyMap()
                                            connectedCount = 0
                                        }
                                    }
                                }
                                dragStartIndex = null
                                dragPos = null
                            },
                            onDragCancel = {
                                dragStartIndex = null
                                dragPos = null
                            }
                        )
                    }
            ) {
                for (li in 0 until wireCount) {
                    val start = Offset(24.dp.toPx(), leftSlotYs[li] * size.height)
                    drawCircle(color = colors[li], radius = 12.dp.toPx(), center = start)
                }
                for (ri in 0 until wireCount) {
                    val end = Offset(size.width - 24.dp.toPx(), rightSlotYs[ri] * size.height)
                    drawCircle(color = colors[correctPairs[ri]], radius = 12.dp.toPx(), center = end, style = Stroke(4.dp.toPx()))
                }
                connections.forEach { (li, ri) ->
                    drawLine(
                        color = colors[li],
                        start = Offset(24.dp.toPx(), leftSlotYs[li] * size.height),
                        end = Offset(size.width - 24.dp.toPx(), rightSlotYs[ri] * size.height),
                        strokeWidth = 6.dp.toPx()
                    )
                }
                val sIdx = dragStartIndex
                val pos = dragPos
                if (sIdx != null && pos != null) {
                    drawLine(
                        color = colors[sIdx].copy(alpha = 0.8f),
                        start = Offset(24.dp.toPx(), leftSlotYs[sIdx] * size.height),
                        end = pos,
                        strokeWidth = 6.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Alarma aproape a sunat - reincepe" else "$connectedCount/$wireCount fire taiate",
            color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(500)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 16) SMUGGLE_WEAPON (~9s) - "Avoid the sweep": o raza de gardian matura
//     ecranul; tine degetul pe zona sigura pana se umple bara de progres.
// ---------------------------------------------------------------------------
@Composable
private fun SmuggleWeaponMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var caught by remember { mutableStateOf(false) }
    val sweep = remember { Animatable(0f) }
    val needed = durationSeconds

    LaunchedEffect(Unit) {
        sweep.animateTo(
            1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(isPressed) {
        while (isPressed && !caught) {
            delay(50)
            val raw = sweep.value % 1f
            val sweepFrac = if (raw < 0.5f) raw * 2f else (1f - raw) * 2f
            if (abs(sweepFrac - 0.5f) < 0.08f) {
                caught = true
                progress = 0f
            } else {
                progress += 0.05f / needed
                if (progress >= 1f) {
                    onComplete()
                    break
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Tine apasat, dar fereste-te de raza gardianului",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(2.dp, if (caught) NEON_RED else accentColor, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            caught = false
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val raw = sweep.value % 1f
                val sweepFrac = if (raw < 0.5f) raw * 2f else (1f - raw) * 2f
                drawLine(
                    color = NEON_RED.copy(alpha = 0.7f),
                    start = Offset(sweepFrac * size.width, 0f),
                    end = Offset(sweepFrac * size.width, size.height),
                    strokeWidth = 5.dp.toPx()
                )
            }
            Text(
                text = if (caught) "VAZUT!" else "🔫",
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = if (caught) NEON_RED else NEON_GREEN,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

// ---------------------------------------------------------------------------
// 17) DECODE_INTERCEPT (~6s) - "Break the cipher": alege litera corecta
//     pentru fiecare simbol cifrat, pe rand.
// ---------------------------------------------------------------------------
@Composable
private fun DecodeInterceptMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val cipherMap = remember {
        val letters = ('A'..'F').toList()
        val symbols = listOf("Ж", "Ω", "Ψ", "Δ", "Ξ", "Ϟ").shuffled()
        letters.zip(symbols).toMap()
    }
    val order = remember { cipherMap.keys.shuffled() }
    var currentIndex by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= order.size) onComplete()
    }
    if (currentIndex >= order.size) return

    val targetLetter = order[currentIndex]
    val targetSymbol = cipherMap[targetLetter]
    val optionLetters = remember(currentIndex) {
        (listOf(targetLetter) + cipherMap.keys.filter { it != targetLetter }.shuffled().take(3)).shuffled()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Decodeaza simbolul (${currentIndex + 1}/${order.size})",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = targetSymbol ?: "?", fontSize = 32.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Ce litera reprezinta?", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            optionLetters.forEach { letter ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable {
                            if (letter == targetLetter) {
                                currentIndex += 1
                            } else {
                                wrongFlash = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = letter.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Gresit" else " ",
            color = NEON_RED,
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
// 18) FORGE_SIGNATURE (~3s) - "Trace the line": traseaza cu degetul peste un
//     traseu ghid, ramanand suficient de aproape pe tot parcursul.
// ---------------------------------------------------------------------------
@Composable
private fun ForgeSignatureMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var offTrack by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size(1f, 1f)) }

    fun guidePoint(t: Float, w: Float, h: Float): Offset {
        val x = t * w
        val y = h * 0.5f + sin(t * 3.14159f * 2.5f) * h * 0.3f
        return Offset(x, y)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Traseaza semnatura urmarind linia punctata",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (progress < 0.95f) {
                                progress = 0f
                            }
                        },
                        onDragCancel = { progress = 0f }
                    ) { change, _ ->
                        change.consume()
                        val w = canvasSize.width
                        val h = canvasSize.height
                        if (w > 1f) {
                            val t = (change.position.x / w).coerceIn(0f, 1f)
                            val guide = guidePoint(t, w, h)
                            val dist = (change.position - guide).getDistance()
                            if (dist < 28f) {
                                offTrack = false
                                progress = maxOf(progress, t)
                                if (progress >= 0.97f) onComplete()
                            } else {
                                offTrack = true
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                canvasSize = size
                val path = androidx.compose.ui.graphics.Path()
                var first = true
                var t = 0f
                while (t <= 1f) {
                    val p = guidePoint(t, size.width, size.height)
                    if (first) {
                        path.moveTo(p.x, p.y)
                        first = false
                    } else {
                        path.lineTo(p.x, p.y)
                    }
                    t += 0.02f
                }
                drawPath(
                    path = path,
                    color = if (offTrack) NEON_RED.copy(alpha = 0.5f) else accentColor.copy(alpha = 0.5f),
                    style = Stroke(width = 3.dp.toPx())
                )
                val progressPoint = guidePoint(progress, size.width, size.height)
                drawCircle(color = NEON_GREEN, radius = 8.dp.toPx(), center = progressPoint)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = if (offTrack) NEON_RED else NEON_GREEN,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

// ---------------------------------------------------------------------------
// 19) SEARCH_FILES (~5s) - "Find the file": gaseste dosarul cu numele cerut
//     printre altele similare, de cateva ori.
// ---------------------------------------------------------------------------
@Composable
private fun SearchFilesMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val names = listOf(
        "Volkov, D.", "Volkova, D.", "Volkov, N.", "Volkov, D.M.",
        "Ivanov, S.", "Ivanova, S.", "Petrov, A.", "Petrova, A."
    )
    val totalRounds = if (durationSeconds <= 3f) 2 else 3
    var round by remember { mutableStateOf(1) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(round) {
        if (round > totalRounds) onComplete()
    }
    if (round > totalRounds) return

    val shuffled = remember(round) { names.shuffled().take(6) }
    val target = remember(round) { shuffled.random() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste dosarul (runda $round/$totalRounds)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cauti: \"$target\"",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shuffled.chunked(2).forEach { rowNames ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowNames.forEach { name ->
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (name == target) {
                                        round += 1
                                    } else {
                                        wrongFlash = true
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = name, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Nu e acesta - citeste cu atentie" else "Fii atent la detalii mici",
            color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(450)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 20) TAMPER_DNA_SAMPLE (~8s) - "Mix in order": apasa reactivii in ordinea
//     corecta, aratata la inceput, ca sa strici proba.
// ---------------------------------------------------------------------------
@Composable
private fun TamperDnaSampleMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val reagents = listOf("A", "B", "C", "D", "E")
    val correctOrder = remember { reagents.shuffled() }
    var showingIndex by remember { mutableStateOf(0) }
    var revealing by remember { mutableStateOf(true) }
    var inputIndex by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (i in correctOrder.indices) {
            showingIndex = i
            delay(550)
        }
        revealing = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (revealing) "Memoreaza ordinea reactivilor" else "Adauga reactivii in ordinea corecta",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (revealing) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(35.dp))
                    .background(accentColor.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = correctOrder[showingIndex], fontSize = 26.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                reagents.forEach { r ->
                    val isDone = correctOrder.indexOf(r) < inputIndex
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(if (isDone) NEON_GREEN.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(27.dp))
                            .clickable(enabled = !isDone) {
                                if (r == correctOrder[inputIndex]) {
                                    inputIndex += 1
                                    if (inputIndex >= correctOrder.size) onComplete()
                                } else {
                                    wrongFlash = true
                                    inputIndex = 0
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = r, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (wrongFlash) "Ordine gresita - reia" else "$inputIndex/${correctOrder.size}",
                color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(450)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 21) SWAP_DNA_LABEL (~5s) - "Match the label": potriveste eticheta corecta
//     cu proba corespunzatoare, prin apasare in perechi.
// ---------------------------------------------------------------------------
@Composable
private fun SwapDnaLabelMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val sampleCodes = remember { (1..4).map { "S-${Random.nextInt(100, 999)}" } }
    var selectedSampleIdx by remember { mutableStateOf<Int?>(null) }
    var matchedCount by remember { mutableStateOf(0) }
    var matched by remember { mutableStateOf(List(sampleCodes.size) { false }) }
    var wrongFlash by remember { mutableStateOf(false) }
    val shuffledLabels = remember { sampleCodes.indices.shuffled() }

    LaunchedEffect(matchedCount) {
        if (matchedCount >= sampleCodes.size) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Potriveste fiecare eticheta cu proba corecta",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Probe", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                sampleCodes.forEachIndexed { i, code ->
                    val isMatched = matched[i]
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMatched) NEON_GREEN.copy(alpha = 0.3f)
                                else if (selectedSampleIdx == i) accentColor.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isMatched) { selectedSampleIdx = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = code, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Etichete", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                shuffledLabels.forEach { labelForSampleIdx ->
                    val isMatched = matched[labelForSampleIdx]
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMatched) NEON_GREEN.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isMatched) {
                                val sel = selectedSampleIdx
                                if (sel != null) {
                                    if (sel == labelForSampleIdx) {
                                        matched = matched.toMutableList().also { it[sel] = true }
                                        matchedCount += 1
                                        selectedSampleIdx = null
                                    } else {
                                        wrongFlash = true
                                        selectedSampleIdx = null
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = sampleCodes[labelForSampleIdx], color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Nu se potrivesc" else "$matchedCount/${sampleCodes.size} potrivite",
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
// 22) POISON_COFFEE (~3s) - "Pour without looking": apasa si tine cand
//     indicatorul e in ceasca tinta, evitand momentele cand cineva se uita.
// ---------------------------------------------------------------------------
@Composable
private fun PoisonCoffeeMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var caught by remember { mutableStateOf(false) }
    var lookingAway by remember { mutableStateOf(true) }
    val needed = durationSeconds

    LaunchedEffect(Unit) {
        while (true) {
            delay((600..1400).random().toLong())
            lookingAway = !lookingAway
        }
    }

    LaunchedEffect(isPressed) {
        while (isPressed && !caught) {
            delay(50)
            if (!lookingAway) {
                caught = true
                progress = 0f
            } else {
                progress += 0.05f / needed
                if (progress >= 1f) {
                    onComplete()
                    break
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Toarna otrava doar cat timp nimeni nu se uita",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(70.dp))
                .background(
                    if (caught) NEON_RED.copy(alpha = 0.3f)
                    else if (lookingAway) NEON_GREEN.copy(alpha = 0.15f)
                    else NEON_RED.copy(alpha = 0.15f)
                )
                .border(2.dp, if (lookingAway) NEON_GREEN else NEON_RED, RoundedCornerShape(70.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            caught = false
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    caught -> "OBSERVAT!"
                    lookingAway -> "☕ Sigur"
                    else -> "👀 Se uita"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = if (caught) NEON_RED else NEON_GREEN,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

// ---------------------------------------------------------------------------
// 23) EAVESDROP_CONVERSATION (~5s) - "Catch the beats": apasa exact cand bara
//     verticala trece prin varfurile evidentiate, de cateva ori la rand.
// ---------------------------------------------------------------------------
@Composable
private fun EavesdropConversationMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val targetHits = if (durationSeconds <= 4f) 3 else 4
    var hits by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val sweep = remember { Animatable(0f) }
    val peakPositions = remember { listOf(0.2f, 0.45f, 0.7f, 0.9f) }
    val tolerance = 0.05f

    LaunchedEffect(hits) {
        if (hits >= targetHits) {
            onComplete()
            return@LaunchedEffect
        }
        sweep.snapTo(0f)
        sweep.animateTo(1f, animationSpec = tween(1500, easing = LinearEasing))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Apasa cand bara trece prin varf ($hits/$targetHits)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(90.dp)
                .clickable {
                    val pos = sweep.value.coerceIn(0f, 1f)
                    val hitPeak = peakPositions.any { abs(pos - it) < tolerance }
                    if (hitPeak) {
                        message = "Prins!"
                        hits += 1
                    } else {
                        message = "Ratat"
                        hits = 0
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.06f),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                peakPositions.forEach { p ->
                    drawLine(
                        color = NEON_GREEN.copy(alpha = 0.5f),
                        start = Offset(p * w, h * 0.2f),
                        end = Offset(p * w, h * 0.8f),
                        strokeWidth = 10.dp.toPx()
                    )
                }
                val sweepX = sweep.value.coerceIn(0f, 1f) * w
                drawLine(
                    color = accentColor,
                    start = Offset(sweepX, 0f),
                    end = Offset(sweepX, h),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message.ifEmpty { "Atinge ecranul la momentul potrivit" },
            color = when (message) {
                "Prins!" -> NEON_GREEN
                "Ratat" -> NEON_RED
                else -> Color.White.copy(alpha = 0.5f)
            },
            fontSize = 13.sp
        )
    }

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            delay(450)
            message = ""
        }
    }
}

// ---------------------------------------------------------------------------
// 24) UPLOAD_VIRUS (~9s) - "Push through firewall": tine apasat, dar bara de
//     progres scade daca ridici degetul, la fel ca o incarcare intrerupta.
// ---------------------------------------------------------------------------
@Composable
private fun UploadVirusMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val needed = durationSeconds

    LaunchedEffect(isPressed) {
        while (true) {
            delay(50)
            if (isPressed) {
                progress += 0.05f / needed
                if (progress >= 1f) {
                    onComplete()
                    break
                }
            } else {
                progress = (progress - 0.05f / needed * 1.5f).coerceAtLeast(0f)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Tine apasat ca sa incarci virusul prin firewall",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.15f + progress * 0.3f))
                .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = NEON_GREEN,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
        Text(
            text = if (isPressed) "Se incarca..." else "Firewall-ul respinge - tine apasat",
            color = if (isPressed) NEON_GREEN else NEON_RED,
            fontSize = 12.sp
        )
    }
}

// ---------------------------------------------------------------------------
// 25) DISPOSE_BODY_EVIDENCE (~6s) - "Move it to the right spot": trage fiecare
//     obiect suspect in zona corecta, ca sa nu ridice suspiciuni.
// ---------------------------------------------------------------------------
@Composable
private fun DisposeBodyEvidenceMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val itemCount = 4
    var placedCount by remember { mutableStateOf(0) }
    var placed by remember { mutableStateOf(List(itemCount) { false }) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(placedCount) {
        if (placedCount >= itemCount) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Trage fiecare obiect in zona corecta",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(200.dp)
        ) {
            // Zonele tinta, in colturi
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, NEON_GREEN.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, NEON_GREEN.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            )

            // Obiectele de mutat, initial in centru, imprastiate
            val startOffsets = remember {
                (0 until itemCount).map {
                    Offset(
                        Random.nextFloat() * 120f + 60f,
                        Random.nextFloat() * 60f + 70f
                    )
                }
            }
            (0 until itemCount).forEach { idx ->
                if (!placed[idx]) {
                    val base = startOffsets[idx]
                    val liveOffset = if (draggingIndex == idx) dragOffset else Offset.Zero
                    