package com.astran.russianspy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.model.Role
import kotlinx.coroutines.delay

/**
 * Ecran de dezvaluire a rolului, stil Among Us: fundal plin, colorat dupa rol
 * (rosu intens = Spion Rus, albastru = Agent FBI), cu o scurta animatie de
 * aparitie (scale + fade), text mare cu rolul, apoi navigheaza automat mai
 * departe catre harta dupa cateva secunde. Nu exista buton "skip" - la fel ca
 * in Among Us, e menit sa fie un moment de suspans scurt, nu o intrerupere.
 */
@Composable
fun RoleRevealScreen(
    role: Role,
    onContinue: () -> Unit
) {
    val isSpy = role == Role.RUSSIAN_SPY

    val backgroundColor = if (isSpy) Color(0xFF7A1B16) else Color(0xFF16407A)
    val backgroundColorDark = if (isSpy) Color(0xFF3A0C09) else Color(0xFF0B1F3A)
    val accentColor = if (isSpy) Color(0xFFE53935) else Color(0xFF4FC3F7)

    val scaleAnim = remember { Animatable(0.4f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(350, easing = LinearOutSlowInEasing))
        scaleAnim.animateTo(1f, animationSpec = tween(500, easing = LinearOutSlowInEasing))
        delay(2600)
        onContinue()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(backgroundColor, backgroundColorDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
            ) {
                Text(
                    text = if (isSpy) "TU ESTI" else "ROLUL TAU",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isSpy) "SPION RUS" else "AGENT FBI",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(3.dp)
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isSpy) {
                        "Ascunde-te printre agenti si trimite informatii\ncatre Moscova fara sa fii prins."
                    } else {
                        "Gaseste-l pe spionul infiltrat inainte\nsa fie prea tarziu."
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}