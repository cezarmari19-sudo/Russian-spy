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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
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
                "DUST_FOR_PRINTS" -> DustForPrintsMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "INTERVIEW_WITNESS" -> InterviewWitnessMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "LOG_EVIDENCE_CHAIN" -> LogEvidenceChainMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "PATROL_CAMERA_FEED" -> PatrolCameraFeedMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "RUN_BACKGROUND_CHECK" -> RunBackgroundCheckMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SWEEP_FOR_BUGS" -> SweepForBugsMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "VERIFY_ID_DOCUMENTS" -> VerifyIdDocumentsMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "RESTOCK_AMMO" -> RestockAmmoMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SIGN_OUT_WEAPON" -> SignOutWeaponMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "CATALOG_DNA_SAMPLE" -> CatalogDnaSampleMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "CROSS_REFERENCE_RECORDS" -> CrossReferenceRecordsMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "BRIEF_THE_TEAM" -> BriefTheTeamMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "SECURE_PERIMETER" -> SecurePerimeterMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "IDENTIFY_REMAINS" -> IdentifyRemainsMinigame(durationSeconds, accentColor, wrappedOnComplete)
                "REFILL_COFFEE_MACHINE" -> RefillCoffeeMachineMinigame(durationSeconds, accentColor, wrappedOnComplete)
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
    val sealedFlags = remember {
        var flags = List(boxCount) { Random.nextBoolean() }
        val goodOnes = flags.count { it }
        val badOnes = flags.count { !it }
        if (goodOnes < 2 || badOnes < 2) {
            flags = (0 until boxCount).map { it % 2 == 0 }.shuffled()
        }
        flags
    }
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
    val totalSteps = 5
    val targetStep = remember { Random.nextInt(0, totalSteps) }
    var currentStep by remember { mutableStateOf(totalSteps / 2) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(currentStep) {
        if (currentStep == targetStep) {
            delay(400)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Regleaza frecventa pana semnalul e stabil",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (step in 0 until totalSteps) {
                val isCurrent = step == currentStep
                val isGood = step == targetStep && isCurrent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGood) NEON_GREEN.copy(alpha = 0.4f)
                            else if (isCurrent) accentColor.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${step + 1}", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, accentColor, RoundedCornerShape(27.dp))
                    .clickable(enabled = currentStep > 0) { currentStep -= 1 },
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, accentColor, RoundedCornerShape(27.dp))
                    .clickable(enabled = currentStep < totalSteps - 1) { currentStep += 1 },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (wrongFlash) "Semnal instabil" else "Gaseste frecventa corecta",
            color = if (wrongFlash) NEON_RED else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
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

    LaunchedEffect(isPressed, progress, caught) {
        if (isPressed && !caught) {
            delay(50)
            val raw = sweep.value % 1f
            val sweepFrac = if (raw < 0.5f) raw * 2f else (1f - raw) * 2f
            if (abs(sweepFrac - 0.5f) < 0.08f) {
                caught = true
                progress = 0f
            } else {
                val next = progress + 0.05f / needed
                if (next >= 1f) {
                    progress = 1f
                    onComplete()
                } else {
                    progress = next
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
                        onDragCancel = { progress = 0f },
                        onDrag = { change, _ ->
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
                    )
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

    LaunchedEffect(isPressed, progress, lookingAway) {
        if (isPressed && !caught) {
            delay(50)
            if (!lookingAway) {
                caught = true
                progress = 0f
            } else {
                val next = progress + 0.05f / needed
                if (next >= 1f) {
                    progress = 1f
                    onComplete()
                } else {
                    progress = next
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

    LaunchedEffect(isPressed, progress) {
        if (isPressed) {
            delay(50)
            val next = progress + 0.05f / needed
            if (next >= 1f) {
                progress = 1f
                onComplete()
            } else {
                progress = next
            }
        } else if (progress > 0f) {
            delay(50)
            progress = (progress - 0.075f / needed).coerceAtLeast(0f)
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
// 25) DISPOSE_BODY_EVIDENCE (~6s) - "Choose the right spot": apasa fiecare
//     obiect suspect, apoi apasa zona corecta unde trebuie mutat.
// ---------------------------------------------------------------------------
@Composable
private fun DisposeBodyEvidenceMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val itemCount = 4
    val zoneNames = listOf("Colet A", "Colet B")
    val correctZones = remember { (0 until itemCount).map { Random.nextInt(0, 2) } }
    var placedCount by remember { mutableStateOf(0) }
    var placed by remember { mutableStateOf(List(itemCount) { false }) }
    var selectedItem by remember { mutableStateOf<Int?>(null) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(placedCount) {
        if (placedCount >= itemCount) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Alege obiectul, apoi apasa zona corecta",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0 until itemCount).forEach { idx ->
                if (!placed[idx]) {
                    val isSelected = selectedItem == idx
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { selectedItem = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📦", fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Zona de destinatie:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            zoneNames.forEachIndexed { zoneIdx, zoneName ->
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 70.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, NEON_GREEN.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            val sel = selectedItem
                            if (sel != null) {
                                if (correctZones[sel] == zoneIdx) {
                                    placed = placed.toMutableList().also { it[sel] = true }
                                    placedCount += 1
                                    selectedItem = null
                                } else {
                                    wrongFlash = true
                                    selectedItem = null
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = zoneName, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (wrongFlash) "Nu e locul potrivit" else "$placedCount/$itemCount obiecte plasate",
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
// 26) DUST_FOR_PRINTS (~5s) - "Brush gently": tine apasat, dar nu prea tare,
//     ca sa nu strici amprenta. Prea tare = reset.
// ---------------------------------------------------------------------------
@Composable
private fun DustForPrintsMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var pressure by remember { mutableStateOf(0f) }
    var smudged by remember { mutableStateOf(false) }
    val needed = durationSeconds

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Perie usor amprenta - nu apasa prea tare",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, if (smudged) NEON_RED else accentColor, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pressure = 0.3f },
                        onDragEnd = { pressure = 0f },
                        onDragCancel = { pressure = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        val speed = abs(dragAmount.y) + abs(dragAmount.x)
                        pressure = (speed / 8f).coerceIn(0f, 1f)
                        if (pressure > 0.65f) {
                            smudged = true
                            progress = 0f
                        } else {
                            progress += 0.03f / needed
                            if (progress >= 1f) onComplete()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (smudged) "STRICATA!" else "🖐",
                fontSize = 26.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = if (smudged) NEON_RED else NEON_GREEN,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }

    LaunchedEffect(smudged) {
        if (smudged) {
            delay(500)
            smudged = false
        }
    }
}

// ---------------------------------------------------------------------------
// 27) INTERVIEW_WITNESS (~4s) - "Ask the right question": alege intrebarea
//     potrivita, de mai multe ori la rand.
// ---------------------------------------------------------------------------
@Composable
private fun InterviewWitnessMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val prompts = listOf(
        Triple("Martorul pare nervos.", "Il linistesti si continui calm", "Il presezi mai tare"),
        Triple("Raspunsul e vag.", "Ceri detalii concrete", "Schimbi subiectul"),
        Triple("Martorul ezita.", "Ii dai timp sa se gandeasca", "Il grabesti")
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
            text = "Interviu martor (runda ${round + 1}/${prompts.size})",
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
                            if (isGood) round += 1 else wrongFlash = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(text = label, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Martorul se inchide - incearca alta abordare" else " ",
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
// 28) LOG_EVIDENCE_CHAIN (~5s) - "Fill in order": bifeaza rubricile lantului
//     de custodie in ordine, la fel ca la raportul de incident.
// ---------------------------------------------------------------------------
@Composable
private fun LogEvidenceChainMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val fields = listOf("Colectat de", "Ora colectarii", "Transferat catre", "Locatie depozitare", "Semnatura")
    var filledCount by remember { mutableStateOf(0) }

    LaunchedEffect(filledCount) {
        if (filledCount >= fields.size) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Completeaza lantul de custodie, in ordine",
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
                                if (isDone) NEON_GREEN.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
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
// 29) PATROL_CAMERA_FEED (~4s) - "Spot the glitch": gaseste feed-ul cu semnal
//     anormal printre altele, de cateva ori.
// ---------------------------------------------------------------------------
@Composable
private fun PatrolCameraFeedMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val totalRounds = if (durationSeconds <= 3f) 2 else 3
    var round by remember { mutableStateOf(1) }
    var wrongFlash by remember { mutableStateOf(false) }
    val glitchIndex = remember(round) { Random.nextInt(6) }

    LaunchedEffect(round) {
        if (round > totalRounds) onComplete()
    }
    if (round > totalRounds) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste camera cu semnal defect (runda $round/$totalRounds)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        val isGlitch = idx == glitchIndex
                        Box(
                            modifier = Modifier
                                .size(width = 76.dp, height = 56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    if (isGlitch) NEON_RED.copy(alpha = 0.6f) else accentColor.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (isGlitch) round += 1 else wrongFlash = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isGlitch) "📵" else "📹",
                                fontSize = 18.sp,
                                color = if (isGlitch) NEON_RED else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Semnal normal - mai cauta" else "Cauta camera cu semnal intrerupt",
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
// 30) RUN_BACKGROUND_CHECK (~5s) - "Enter the code": memoreaza si reproduce
//     un cod de 4 cifre.
// ---------------------------------------------------------------------------
@Composable
private fun RunBackgroundCheckMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val code = remember { (0 until 4).map { Random.nextInt(0, 10) } }
    var revealing by remember { mutableStateOf(true) }
    var inputIndex by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1800)
        revealing = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (revealing) "Memoreaza codul de acces" else "Introdu codul",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            code.forEachIndexed { i, digit ->
                val isDone = !revealing && i < inputIndex
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDone) NEON_GREEN.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (revealing || isDone) digit.toString() else "_",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!revealing) {
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..9).chunked(5).forEach { rowDigits ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowDigits.forEach { d ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (inputIndex < code.size) {
                                            if (d == code[inputIndex]) {
                                                inputIndex += 1
                                                if (inputIndex >= code.size) onComplete()
                                            } else {
                                                wrongFlash = true
                                                inputIndex = 0
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = d.toString(), color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (wrongFlash) "Cod gresit" else " ",
                color = NEON_RED,
                fontSize = 12.sp
            )
        }
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(400)
            wrongFlash = false
        }
    }
}

// ---------------------------------------------------------------------------
// 31) SWEEP_FOR_BUGS (~5s) - "Find the peak": plimba senzorul (slider) pana
//     gasesti varful de semnal, la fel ca BUG_PHONE_LINE dar cu prag mai ingust.
// ---------------------------------------------------------------------------
@Composable
private fun SweepForBugsMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    var sliderPos by remember { mutableStateOf(0f) }
    var heldMs by remember { mutableStateOf(0f) }
    val targetCenter = remember { 0.2f + Random.nextFloat() * 0.6f }
    val targetHalfWidth = 0.045f
    val neededMs = 900f
    val inZone = abs(sliderPos - targetCenter) < targetHalfWidth

    LaunchedEffect(inZone, heldMs) {
        if (inZone) {
            delay(50)
            val next = heldMs + 50f
            if (next >= neededMs) {
                heldMs = neededMs
                onComplete()
            } else {
                heldMs = next
            }
        } else {
            heldMs = 0f
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste semnalul dispozitivului ascuns",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth(0.85f).height(50.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(color = Color.White.copy(alpha = 0.08f), cornerRadius = CornerRadius(8.dp.toPx()))
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
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
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
// 32) VERIFY_ID_DOCUMENTS (~4s) - "Spot the difference": gaseste diferenta
//     dintre 2 documente aproape identice.
// ---------------------------------------------------------------------------
@Composable
private fun VerifyIdDocumentsMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val fields = listOf("Nume", "Data nasterii", "Nr. document", "Adresa", "Data expirarii")
    var round by remember { mutableStateOf(1) }
    val totalRounds = if (durationSeconds <= 3f) 2 else 3
    var wrongFlash by remember { mutableStateOf(false) }
    val diffIndex = remember(round) { Random.nextInt(fields.size) }

    LaunchedEffect(round) {
        if (round > totalRounds) onComplete()
    }
    if (round > totalRounds) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste campul care difera (runda $round/$totalRounds)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Original", "Copie").forEach { label ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    fields.forEachIndexed { i, f ->
                        val isCopyDiff = label == "Copie" && i == diffIndex
                        Box(
                            modifier = Modifier
                                .clickable(enabled = label == "Copie") {
                                    if (isCopyDiff) round += 1 else wrongFlash = true
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "$f: ${if (isCopyDiff) "***" else "OK"}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Acesta e identic - mai cauta" else "Apasa campul diferit din Copie",
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
// 33) RESTOCK_AMMO (~3s) - "Count the crates": apasa numarul corect de cutii
//     ca sa confirmi cantitatea din inventar.
// ---------------------------------------------------------------------------
@Composable
private fun RestockAmmoMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val correctCount = remember { Random.nextInt(3, 8) }
    var tapped by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Numara si confirma $correctCount cutii de munitie",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(correctCount + 2) { idx ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (idx < tapped) NEON_GREEN.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable(enabled = idx == tapped) {
                            tapped += 1
                            if (tapped == correctCount) {
                                onComplete()
                            } else if (tapped > correctCount) {
                                wrongFlash = true
                                tapped = 0
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📦", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (wrongFlash) "Prea multe - reincepe numaratoarea" else "$tapped/$correctCount confirmate",
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
// 34) SIGN_OUT_WEAPON (~4s) - "Checklist": bifeaza pasii formularului de
//     predare arma, in ordine (la fel ca LOG_EVIDENCE_CHAIN dar mai scurt).
// ---------------------------------------------------------------------------
@Composable
private fun SignOutWeaponMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val fields = listOf("Verifica seria armei", "Confirma incarcatorul gol", "Semneaza registrul")
    var filledCount by remember { mutableStateOf(0) }

    LaunchedEffect(filledCount) {
        if (filledCount >= fields.size) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Bifeaza pasii de predare a armei, in ordine",
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
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isDone) NEON_GREEN.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
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
                        fontSize = 13.sp,
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
// 35) CATALOG_DNA_SAMPLE (~5s) - "File it right": trage proba in sertarul cu
//     codul corect din arhiva.
// ---------------------------------------------------------------------------
@Composable
private fun CatalogDnaSampleMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val drawerCodes = remember { (0 until 4).map { "D-${('A' + it)}${Random.nextInt(1, 9)}" } }
    val targetDrawer = remember { drawerCodes.random() }
    var wrongFlash by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Cataloghezi proba - alege sertarul corect",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Codul probei: $targetDrawer",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            drawerCodes.chunked(2).forEach { rowCodes ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowCodes.forEach { code ->
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (code == targetDrawer) {
                                        onComplete()
                                    } else {
                                        wrongFlash = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = code, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Sertar gresit" else " ",
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
// 36) CROSS_REFERENCE_RECORDS (~5s) - "Find the match": gaseste cele doua
//     dosare cu acelasi numar din grila.
// ---------------------------------------------------------------------------
@Composable
private fun CrossReferenceRecordsMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val pairCount = 3
    val values = remember {
        val nums = (0 until pairCount).map { Random.nextInt(100, 999) }
        (nums + nums).shuffled()
    }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var matchedIndices by remember { mutableStateOf(setOf<Int>()) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(matchedIndices) {
        if (matchedIndices.size >= values.size) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste perechile de dosare cu acelasi numar",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.indices.chunked(3).forEach { rowIndices ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowIndices.forEach { idx ->
                        val v = values[idx]
                        val isMatched = idx in matchedIndices
                        val isSelected = selectedIndex == idx
                        Box(
                            modifier = Modifier
                                .size(width = 66.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isMatched -> NEON_GREEN.copy(alpha = 0.3f)
                                        isSelected -> accentColor.copy(alpha = 0.5f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    }
                                )
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable(enabled = !isMatched) {
                                    val sel = selectedIndex
                                    if (sel == null) {
                                        selectedIndex = idx
                                    } else if (sel == idx) {
                                        selectedIndex = null
                                    } else if (values[sel] == values[idx]) {
                                        matchedIndices = matchedIndices + sel + idx
                                        selectedIndex = null
                                    } else {
                                        wrongFlash = true
                                        selectedIndex = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$v",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Nu se potrivesc" else "${matchedIndices.size / 2}/$pairCount perechi",
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
// 37) BRIEF_THE_TEAM (~4s) - "Order the slides": apasa diapozitivele in
//     ordinea numerica corecta.
// ---------------------------------------------------------------------------
@Composable
private fun BriefTheTeamMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val slideCount = 5
    val shuffledOrder = remember { (1..slideCount).shuffled() }
    var nextExpected by remember { mutableStateOf(1) }
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(nextExpected) {
        if (nextExpected > slideCount) {
            delay(150)
            onComplete()
        }
    }
    if (nextExpected > slideCount) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Apasa diapozitivele in ordine crescatoare",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shuffledOrder.forEach { num ->
                val isDone = num < nextExpected
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDone) NEON_GREEN.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isDone) {
                            if (num == nextExpected) {
                                nextExpected += 1
                            } else {
                                wrongFlash = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = num.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Nu e urmatorul - verifica ordinea" else "Urmatorul: $nextExpected",
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
// 38) SECURE_PERIMETER (~5s) - "Check each post": apasa punctele de control
//     in ordine, fara sa ratezi vreunul.
// ---------------------------------------------------------------------------
@Composable
private fun SecurePerimeterMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val postCount = 5
    var checkedCount by remember { mutableStateOf(0) }
    var wrongFlash by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Verifica punctele de control, de la 1 la $postCount",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(120.dp)
        ) {
            val positions = remember {
                (0 until postCount).map {
                    Offset(
                        50f + Random.nextFloat() * 200f,
                        20f + Random.nextFloat() * 80f
                    )
                }
            }
            positions.forEachIndexed { idx, pos ->
                val isDone = idx < checkedCount
                val isNext = idx == checkedCount
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.toInt(), pos.y.toInt()) }
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                isDone -> NEON_GREEN.copy(alpha = 0.4f)
                                isNext -> accentColor.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.08f)
                            }
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable {
                            if (isNext) {
                                checkedCount += 1
                                if (checkedCount >= postCount) onComplete()
                            } else if (!isDone) {
                                wrongFlash = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${idx + 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Verifica in ordine" else "$checkedCount/$postCount verificate",
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
// 39) IDENTIFY_REMAINS (~6s) - "Match the profile": potriveste trasaturile
//     cu profilul corect, de doua ori la rand.
// ---------------------------------------------------------------------------
@Composable
private fun IdentifyRemainsMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val totalRounds = 2
    var round by remember { mutableStateOf(1) }
    var wrongFlash by remember { mutableStateOf(false) }

    val traits = listOf("Inaltime", "Varsta estimata", "Grup sangvin")
    val profiles = remember(round) {
        (0 until 3).map { idx ->
            traits.associateWith {
                when (it) {
                    "Inaltime" -> "${Random.nextInt(160, 195)} cm"
                    "Varsta estimata" -> "${Random.nextInt(25, 60)} ani"
                    else -> listOf("A+", "B+", "O+", "AB+").random()
                }
            }
        }
    }
    val targetIdx = remember(round) { Random.nextInt(profiles.size) }
    val targetProfile = profiles[targetIdx]

    LaunchedEffect(round) {
        if (round > totalRounds) onComplete()
    }
    if (round > totalRounds) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Gaseste profilul corect (runda $round/$totalRounds)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column {
                Text("Fisa dentara", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                traits.forEach { t ->
                    Text("$t: ${targetProfile[t] ?: ""}", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Profile disponibile:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            profiles.forEachIndexed { i, profile ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable {
                            if (i == targetIdx) round += 1 else wrongFlash = true
                        }
                        .padding(8.dp)
                ) {
                    Column {
                        traits.forEach { t ->
                            Text("${profile[t] ?: ""}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (wrongFlash) "Nu se potriveste" else " ",
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
// 40) REFILL_COFFEE_MACHINE (~2s) - "Press in order": apasa butoanele in
//     ordinea aratata, task scurt si simplu.
// ---------------------------------------------------------------------------
@Composable
private fun RefillCoffeeMachineMinigame(
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit
) {
    val buttons = listOf("Apa", "Boabe", "Filtru")
    var pressedCount by remember { mutableStateOf(0) }

    LaunchedEffect(pressedCount) {
        if (pressedCount >= buttons.size) {
            delay(150)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Apasa butoanele in ordine, ca sa umpli automatul",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            buttons.forEachIndexed { i, label ->
                val isDone = i < pressedCount
                val isNext = i == pressedCount
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDone) NEON_GREEN.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isNext) accentColor else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = isNext) { pressedCount += 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "$pressedCount/${buttons.size}",
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