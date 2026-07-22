package com.astran.russianspy.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Paleta si componentele vizuale COMUNE ale aplicatiei (meniuri, lobby, prieteni,
 * setari) - stil "dosar de investigatie tactic": griuri/negru inchis + UN SINGUR
 * accent (rosu). Harta de joc (GameCanvasScreen) NU foloseste acest fisier, ramane
 * neschimbata.
 */
object TacticalColors {
    val Background = Color(0xFF0B0D10)
    val BackgroundLight = Color(0xFF15181D)
    val Surface = Color(0xFF191C22)
    val SurfaceRaised = Color(0xFF20242C)
    val Border = Color(0xFF2E333D)
    val Accent = Color(0xFFB3261E)       // rosu tactic - SINGURUL accent de culoare
    val AccentDim = Color(0xFF7A1B16)
    val TextPrimary = Color(0xFFEDEEF0)
    val TextSecondary = Color(0xFF9AA0A6)
    val TextMuted = Color(0xFF5C6067)
    val Success = Color(0xFF4CAF50)
    val Danger = Color(0xFFE53935)
}

/**
 * Fundal comun pentru toate ecranele: gradient vertical subtil (nu negru plat)
 * plus un pattern discret de puncte (stil radar/supraveghere), desenat o
 * singura data cu positii random fixe (seed constant) - nu se re-genereaza la
 * fiecare recompunere.
 */
@Composable
fun TacticalBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dotSeed = remember { Random(42) }
    val dots = remember {
        List(60) {
            Triple(dotSeed.nextFloat(), dotSeed.nextFloat(), dotSeed.nextFloat() * 1.6f + 0.6f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TacticalColors.BackgroundLight,
                        TacticalColors.Background
                    )
                )
            )
            .drawBehind {
                dots.forEach { (fx, fy, radius) ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.035f),
                        radius = radius.dp.toPx(),
                        center = Offset(fx * size.width, fy * size.height)
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Forma cu COLT TAIAT (nu rotunjit) in stanga-sus si dreapta-jos - da un aspect
 * de "placuta/insigna tactica", nu un dreptunghi generic de UI web/AI.
 */
private fun cutCornerShape(cut: Dp) = GenericShape { size, density ->
    val c = with(density) { cut.toPx() }
    moveTo(c, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(0f, size.height)
    lineTo(0f, c)
    close()
}

/** Versiune publica a formei cu colt taiat, pentru folosire in afara acestui fisier (ex. NavGraph). */
fun tacticalCardShapePublic(cut: Dp) = cutCornerShape(cut)

/**
 * Buton principal "tactic": colt taiat, bordura, umbra jos care il face sa para
 * apasat/fizic (nu plat), stare "pressed" cu offset vizual mic. isPrimary
 * foloseste accentul rosu; altfel e monocrom (gri inchis) - UN SINGUR accent
 * de culoare in toata paleta, nu multe culori pe acelasi buton.
 */
@Composable
fun TacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset = if (isPressed) 2.dp else 0.dp

    val baseColor = when {
        !enabled -> TacticalColors.SurfaceRaised
        isPrimary -> TacticalColors.Accent
        else -> TacticalColors.SurfaceRaised
    }
    val shadowColor = if (isPrimary) TacticalColors.AccentDim else Color(0xFF0A0C0F)
    val borderColor = if (isPrimary) Color(0xFFE0463C) else TacticalColors.Border
    val textColor = if (enabled) Color.White else TacticalColors.TextMuted

    Box(
        modifier = modifier
            .height(height + 4.dp)
    ) {
        // "umbra" solida dedesubt - da senzatia de buton apasabil, cu adancime
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(top = 4.dp)
                .clip(cutCornerShape(14.dp))
                .background(shadowColor)
        )
        // fata butonului, care se "lasa" usor in jos cand e apasat
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(top = pressOffset)
                .clip(cutCornerShape(14.dp))
                .background(baseColor)
                .border(width = 1.5.dp, color = borderColor, shape = cutCornerShape(14.dp))
                .clickableTactical(enabled, interactionSource, onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun Modifier.clickableTactical(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
): Modifier = this.then(
    androidx.compose.foundation.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
)

/**
 * Card/rand tactic reutilizabil: colt taiat mic, bordura subtire, fundal usor
 * ridicat fata de background - folosit pentru randuri de jucatori, prieteni,
 * cereri, etc, in locul unui dreptunghi rotunjit plat.
 */
@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    accentLeft: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(cutCornerShape(10.dp))
            .background(TacticalColors.Surface)
            .border(width = 1.dp, color = TacticalColors.Border, shape = cutCornerShape(10.dp))
            .then(
                if (accentLeft) {
                    Modifier.drawBehind {
                        drawRect(
                            color = TacticalColors.Accent,
                            size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
            )
    ) {
        content()
    }
}

/** Eticheta mica de sectiune, stil "dosar" - text cu spatiere larga, subtitrat. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TacticalColors.TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = modifier
    )
}