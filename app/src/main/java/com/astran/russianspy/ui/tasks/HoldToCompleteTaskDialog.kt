package com.astran.russianspy.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Mini-joc "hold to complete", stil Among Us: jucatorul tine apasat pe un cerc
 * pana se umple un inel de progres, in `durationSeconds`. Daca ridica degetul
 * inainte sa termine, progresul se reseteaza la 0 (nu ramane partial). La
 * completare, apeleaza onComplete() o singura data.
 *
 * Folosit atat pentru task-urile spionului (fotografiat, plasare dispozitiv,
 * etc) cat si pentru dezactivarea unui dispozitiv de catre un agent FBI -
 * mecanica identica, doar textul si culoarea de accent difera.
 */
@Composable
fun HoldToCompleteTaskDialog(
    title: String,
    description: String,
    durationSeconds: Float,
    accentColor: Color,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
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

            Spacer(modifier = Modifier.height(40.dp))

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
                                        animationSpec = tween(
                                            (durationSeconds * 1000).toInt(),
                                            easing = LinearEasing
                                        )
                                    )
                                    if (progress.value >= 0.999f) {
                                        isDone = true
                                        onComplete()
                                    }
                                }
                                val released = tryAwaitRelease()
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
                        size = androidx.compose.ui.geometry.Size(
                            size.width - 16.dp.toPx(),
                            size.height - 16.dp.toPx()
                        )
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

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onCancel) {
                Text("Anuleaza", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}