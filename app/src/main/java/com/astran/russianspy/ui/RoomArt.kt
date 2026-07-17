package com.astran.russianspy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.astran.russianspy.model.Room

// ============================================================================
// TEMA VIZUALA COMUNA - "FBI misterios"
// Toate camerele desenate in detaliu (RoomArt) folosesc aceasta paleta si aceste
// principii, ca jocul sa aiba un aspect unitar si serios, nu un colaj de stiluri:
//
// - Fundal de podea aproape negru, cu o nuanta usor colorata dupa tipul camerei
//   (albastru-inchis pentru tehnic, mov-inchis pentru supraveghere, etc), NICIODATA
//   culori saturate/vesele.
// - Un singur "punct de interes" luminos pe cameră (monitor, lampă, ecran), restul
//   camerei rămâne în semi-întuneric - creează senzația de birou oficial, secret.
// - Contur subțire aproape negru pe orice obiect, ca să se distingă de podea fără
//   să pară desen de copii (fără contururi groase, colorate).
// - Grid de podea abia vizibil (dale), niciodată dominant.
// - Vinietă ușoară pe margini, ca centrul camerei să rămână punctul de atenție.
// ============================================================================

object RoomTheme {
    val floorGridLine = Color.White.copy(alpha = 0.035f)
    val wallShadow = Color.Black.copy(alpha = 0.5f)
    val objectOutline = Color(0xFF05070A)
    val screenGlowGreen = Color(0xFF3DDC5A)
    val screenGlowGreenDim = Color(0xFF1F5C2E)
    val metalLight = Color(0xFF4A4F58)
    val metalDark = Color(0xFF23262C)
    val metalDarker = Color(0xFF16181C)
    val accentWarm = Color(0xFFB58A3D) // lumini calde punctuale (lampi, lemn)
    val fabricDark = Color(0xFF2A2E36) // scaune, mobilier textil
}

/** Deseneaza un grid subtil de dale de podea peste zona (0,0)-(w,h) din DrawScope curent. */
private fun DrawScope.drawFloorGrid(w: Float, h: Float, cell: Float) {
    var gx = 0f
    while (gx < w) {
        drawLine(RoomTheme.floorGridLine, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
        gx += cell
    }
    var gy = 0f
    while (gy < h) {
        drawLine(RoomTheme.floorGridLine, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        gy += cell
    }
}

/** Vinieta radiala standard, aplicata la finalul desenului fiecarei camere detaliate. */
private fun DrawScope.drawRoomVignette(w: Float, h: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.72f
        ),
        topLeft = Offset.Zero,
        size = Size(w, h)
    )
}

/** Un dreptunghi cu contur, helper folosit peste tot pentru obiecte "in bloc" (birouri, dulapuri). */
private fun DrawScope.solidRect(topLeft: Offset, size: Size, fill: Color, outline: Color = RoomTheme.objectOutline, outlineWidth: Float = 2f) {
    drawRect(color = fill, topLeft = topLeft, size = size)
    drawRect(color = outline, topLeft = topLeft, size = size, style = Stroke(width = outlineWidth))
}

// ============================================================================
// CAMERA DE SUPRAVEGHERE - etalonul stilistic
// ============================================================================

/**
 * Deseneaza camera de Supraveghere in detaliu, in coordonate LOCALE camerei
 * (0,0 = coltul stanga-sus al camerei, w/h = latimea/inaltimea ei in pixeli pe ecran).
 * Apelantul (GameCanvasScreen) e responsabil sa translateze/scaleze corect inainte
 * de a apela aceasta functie (de ex. cu `translate(topLeft.x, topLeft.y)`).
 *
 * Layout gandit dupa geometria reala a camerei "surveillance" din BuildingLayout.kt:
 * - Acces din hol pe SUS la ~33%-66% din latime (stanga-centru) -> zona ramane libera.
 * - Acces din hol pe JOS la ~75%-100% din latime (dreapta) -> zona ramane libera.
 * - Biroul cu monitoare e lipit de peretele de SUS, in partea DREAPTA (>66% latime),
 *   ca sa nu blocheze niciunul din cele 2 accese.
 */
fun DrawScope.drawSurveillanceRoomDetailed(w: Float, h: Float) {
    // Podeaua: mov foarte inchis, aproape negru, cu o usoara tenta violet - "camera secreta".
    drawRect(color = Color(0xFF120C1A), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 9f)

    // --- Biroul cu monitoare (dreapta-sus, lipit de perete) ---
    val deskWidth = w * 0.42f
    val deskHeight = h * 0.09f
    val deskLeft = w * 0.55f
    val deskTop = h * 0.04f

    // Umbra usoara sub birou, ca sa "stea" pe podea.
    drawRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(deskLeft - 4f, deskTop + deskHeight),
        size = Size(deskWidth + 8f, h * 0.02f)
    )

    // Consola/biroul propriu-zis.
    solidRect(Offset(deskLeft, deskTop), Size(deskWidth, deskHeight), RoomTheme.metalDark)
    // Muchia de sus a biroului, mai deschisa (efect de lumina de sus).
    drawRect(
        color = RoomTheme.metalLight.copy(alpha = 0.5f),
        topLeft = Offset(deskLeft, deskTop),
        size = Size(deskWidth, deskHeight * 0.18f)
    )

    // 3 monitoare CRT pe birou, cu "static" verde - sursa principala de lumina a camerei.
    val monitorCount = 3
    val monitorGap = deskWidth * 0.03f
    val monitorWidth = (deskWidth - monitorGap * (monitorCount + 1)) / monitorCount
    val monitorHeight = deskHeight * 0.62f
    for (i in 0 until monitorCount) {
        val mx = deskLeft + monitorGap + i * (monitorWidth + monitorGap)
        val my = deskTop - monitorHeight * 0.55f
        // Rama monitorului.
        solidRect(Offset(mx, my), Size(monitorWidth, monitorHeight), RoomTheme.metalDarker)
        // Ecranul - lumina verde, cu o varianta usor diferita de verde per monitor.
        val screenPad = monitorWidth * 0.08f
        val screenColor = if (i == 1) RoomTheme.screenGlowGreenDim else RoomTheme.screenGlowGreen.copy(alpha = 0.85f)
        drawRect(
            color = screenColor,
            topLeft = Offset(mx + screenPad, my + screenPad),
            size = Size(monitorWidth - screenPad * 2f, monitorHeight - screenPad * 2f)
        )
        // Un mic glow in jurul ecranului, ca sursa de lumina reala in camera intunecata.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(screenColor.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(mx + monitorWidth / 2f, my + monitorHeight / 2f),
                radius = monitorWidth * 1.4f
            ),
            topLeft = Offset(mx - monitorWidth * 0.4f, my - monitorHeight * 0.4f),
            size = Size(monitorWidth * 1.8f, monitorHeight * 1.8f)
        )
    }

    // Scaunul de birou, sub consola, catre centrul camerei.
    val chairCenterX = deskLeft + deskWidth * 0.5f
    val chairCenterY = deskTop + deskHeight + h * 0.09f
    drawCircle(color = RoomTheme.fabricDark, radius = w * 0.045f, center = Offset(chairCenterX, chairCenterY))
    drawCircle(
        color = RoomTheme.objectOutline,
        radius = w * 0.045f,
        center = Offset(chairCenterX, chairCenterY),
        style = Stroke(width = 2f)
    )
    // Baza scaunului (picioare), o cruce simpla.
    drawLine(
        RoomTheme.metalDark,
        Offset(chairCenterX - w * 0.03f, chairCenterY + h * 0.05f),
        Offset(chairCenterX + w * 0.03f, chairCenterY + h * 0.05f),
        strokeWidth = 3f
    )

    // --- Dulap/rack lateral, stanga-jos (zona libera de accese, decor) ---
    val cabinetW = w * 0.14f
    val cabinetH = h * 0.22f
    solidRect(Offset(w * 0.04f, h * 0.7f), Size(cabinetW, cabinetH), RoomTheme.metalDark)
    // Cateva "sertare" - linii orizontale.
    for (i in 1..3) {
        val ly = h * 0.7f + cabinetH * (i / 4f)
        drawLine(
            RoomTheme.objectOutline.copy(alpha = 0.6f),
            Offset(w * 0.04f, ly),
            Offset(w * 0.04f + cabinetW, ly),
            strokeWidth = 1.5f
        )
    }
    // Un mic LED indicator pe dulap (rosu, discret).
    drawCircle(Color(0xFFB33A3A), radius = 3f, center = Offset(w * 0.04f + cabinetW * 0.85f, h * 0.72f))

    // --- Sigla FBI discreta pe podea, in centru, aproape invizibila (atmosfera) ---
    drawCircle(
        color = Color.White.copy(alpha = 0.025f),
        radius = w * 0.16f,
        center = Offset(w * 0.42f, h * 0.62f),
        style = Stroke(width = 3f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.018f),
        radius = w * 0.13f,
        center = Offset(w * 0.42f, h * 0.62f),
        style = Stroke(width = 1.5f)
    )

    drawRoomVignette(w, h)
}