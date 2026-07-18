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

// ============================================================================
// ARMURERIE - dulapuri de arme, zona restrictionata
// ============================================================================

/**
 * Geometrie reala (BuildingLayout.kt): camera "armory" e x:2900-3200, y:500-750 (300x250).
 * Accese:
 * - hall_lab_armory intra prin peretele STANG, pe y:550-700 (local y: 50-200, adica
 *   aproape tot peretele stang, centrat vertical) -> peretele stang ramane liber.
 * - hall_armory intra prin peretele de JOS, pe x:3000-3100 (local x: 100-200,
 *   centru-dreapta) -> acea portiune din jos ramane libera.
 * Mobilierul (dulapurile de arme) e asezat lipit de peretele de SUS si de DREAPTA,
 * zone fara niciun acces.
 */
fun DrawScope.drawArmoryRoomDetailed(w: Float, h: Float) {
    // Podeaua: gri-albastrui foarte inchis, aproape negru - metalic, rece, oficial.
    drawRect(color = Color(0xFF0E1114), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 9f)

    // --- Dulapuri de arme, lipite de peretele de SUS (3 vitrine identice) ---
    val cabinetCount = 3
    val cabinetGap = w * 0.02f
    val cabinetsAreaLeft = w * 0.30f  // incepe dupa zona de acces din stanga
    val cabinetsAreaWidth = w * 0.66f
    val cabinetWidth = (cabinetsAreaWidth - cabinetGap * (cabinetCount - 1)) / cabinetCount
    val cabinetHeight = h * 0.34f
    val cabinetTop = h * 0.03f

    for (i in 0 until cabinetCount) {
        val cx = cabinetsAreaLeft + i * (cabinetWidth + cabinetGap)

        // Umbra sub dulap.
        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(cx - 2f, cabinetTop + cabinetHeight),
            size = Size(cabinetWidth + 4f, h * 0.015f)
        )

        // Corpul dulapului (rama metalica).
        solidRect(Offset(cx, cabinetTop), Size(cabinetWidth, cabinetHeight), RoomTheme.metalDark)

        // Interiorul vitrinei - sticla usor iluminata, cu o tenta rosiatica discreta
        // (lumina de securitate), ca sa para o zona restrictionata / alarma.
        val glassPad = cabinetWidth * 0.1f
        val glassColor = Color(0xFF3A1616)
        drawRect(
            color = glassColor,
            topLeft = Offset(cx + glassPad, cabinetTop + glassPad),
            size = Size(cabinetWidth - glassPad * 2f, cabinetHeight - glassPad * 2f)
        )
        // Glow rosu discret in jurul vitrinei - sursa de lumina a camerei.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB33A3A).copy(alpha = 0.16f), Color.Transparent),
                center = Offset(cx + cabinetWidth / 2f, cabinetTop + cabinetHeight / 2f),
                radius = cabinetWidth * 1.3f
            ),
            topLeft = Offset(cx - cabinetWidth * 0.3f, cabinetTop - cabinetHeight * 0.3f),
            size = Size(cabinetWidth * 1.6f, cabinetHeight * 1.6f)
        )

        // Siluete simple de arme in vitrina (2 dreptunghiuri lungi, verticale, sugerand pusti).
        val gunW = cabinetWidth * 0.10f
        val gunH = cabinetHeight * 0.58f
        val gunY = cabinetTop + glassPad + (cabinetHeight - glassPad * 2f - gunH) / 2f
        drawRect(
            color = RoomTheme.metalLight.copy(alpha = 0.85f),
            topLeft = Offset(cx + cabinetWidth * 0.35f, gunY),
            size = Size(gunW, gunH)
        )
        drawRect(
            color = RoomTheme.metalLight.copy(alpha = 0.85f),
            topLeft = Offset(cx + cabinetWidth * 0.55f, gunY),
            size = Size(gunW, gunH)
        )

        // Mic LED de status pe rama dulapului (rosu = incuiat/restrictionat).
        drawCircle(Color(0xFFB33A3A), radius = 2.5f, center = Offset(cx + cabinetWidth * 0.9f, cabinetTop + cabinetHeight * 0.08f))
    }

    // --- Rack metalic vertical, lipit de peretele din DREAPTA (zona fara acces) ---
    val rackWidth = w * 0.10f
    val rackHeight = h * 0.52f
    val rackLeft = w * 0.86f
    val rackTop = h * 0.42f
    solidRect(Offset(rackLeft, rackTop), Size(rackWidth, rackHeight), RoomTheme.metalDarker)
    for (i in 1..4) {
        val ly = rackTop + rackHeight * (i / 5f)
        drawLine(
            RoomTheme.objectOutline.copy(alpha = 0.7f),
            Offset(rackLeft, ly),
            Offset(rackLeft + rackWidth, ly),
            strokeWidth = 1.5f
        )
    }
    // Un LED galben pe rack (status neutru, spre deosebire de rosu de la vitrine).
    drawCircle(Color(0xFFC9A227), radius = 2.5f, center = Offset(rackLeft + rackWidth * 0.5f, rackTop + rackHeight * 0.06f))

    // --- Masa de intretinere/curatat arme, jos-stanga-centru (zona libera de accese) ---
    val tableW = w * 0.20f
    val tableH = h * 0.11f
    val tableLeft = w * 0.30f
    val tableTop = h * 0.75f
    drawRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(tableLeft - 2f, tableTop + tableH),
        size = Size(tableW + 4f, h * 0.015f)
    )
    solidRect(Offset(tableLeft, tableTop), Size(tableW, tableH), RoomTheme.metalDark)
    drawRect(
        color = RoomTheme.metalLight.copy(alpha = 0.3f),
        topLeft = Offset(tableLeft, tableTop),
        size = Size(tableW, tableH * 0.2f)
    )

    drawRoomVignette(w, h)
}

// ============================================================================
// CAMERA DE PAUZA - unicul spatiu cu tenta calda din cladire
// ============================================================================

/**
 * Geometrie reala (BuildingLayout.kt): camera "break_room" e x:2900-3250, y:2050-2300
 * (350x250). Singurul acces: hall_break intra prin peretele STANG, pe y:2125-2225
 * (local y: 75-175, adica centrul vertical al peretelui stang) -> acea zona ramane libera.
 * Mobilierul e distribuit sus (chiuveta/dulap), dreapta (automatul de cafea) si
 * jos-centru (masa cu scaune), evitand toate centrul-stanga.
 */
fun DrawScope.drawBreakRoomDetailed(w: Float, h: Float) {
    // Podeaua: maro-caramiziu foarte inchis, singura camera cu o tenta calda (nu rece/metalica),
    // dar tot suficient de intunecata cat sa se incadreze in atmosfera generala.
    drawRect(color = Color(0xFF1C130E), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 10f)

    // --- Chiuveta/dulap de bucatarie, lipit de peretele de SUS (zona libera de acces) ---
    val counterWidth = w * 0.5f
    val counterHeight = h * 0.10f
    val counterLeft = w * 0.28f
    val counterTop = h * 0.03f

    drawRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(counterLeft - 2f, counterTop + counterHeight),
        size = Size(counterWidth + 4f, h * 0.015f)
    )
    solidRect(Offset(counterLeft, counterTop), Size(counterWidth, counterHeight), RoomTheme.metalDark)
    // Blatul de lucru, usor mai deschis.
    drawRect(
        color = RoomTheme.metalLight.copy(alpha = 0.4f),
        topLeft = Offset(counterLeft, counterTop),
        size = Size(counterWidth, counterHeight * 0.22f)
    )
    // Chiuveta (patrat mai inchis, centrat pe blat).
    val sinkSize = counterHeight * 0.5f
    drawRect(
        color = RoomTheme.metalDarker,
        topLeft = Offset(counterLeft + counterWidth * 0.5f - sinkSize / 2f, counterTop + counterHeight * 0.3f),
        size = Size(sinkSize, sinkSize * 0.7f)
    )
    // Cateva dulapuri sub blat (linii verticale de separare).
    for (i in 1..4) {
        val lx = counterLeft + counterWidth * (i / 5f)
        drawLine(
            RoomTheme.objectOutline.copy(alpha = 0.5f),
            Offset(lx, counterTop + counterHeight * 0.4f),
            Offset(lx, counterTop + counterHeight),
            strokeWidth = 1.5f
        )
    }

    // --- Automat de cafea/racoritoare, lipit de peretele din DREAPTA (zona libera) ---
    val vendingWidth = w * 0.13f
    val vendingHeight = h * 0.40f
    val vendingLeft = w * 0.83f
    val vendingTop = h * 0.28f

    drawRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(vendingLeft - 2f, vendingTop + vendingHeight),
        size = Size(vendingWidth + 4f, h * 0.015f)
    )
    solidRect(Offset(vendingLeft, vendingTop), Size(vendingWidth, vendingHeight), RoomTheme.metalDark)
    // Panoul frontal, cu o lumina calda (galben-portocalie) - sursa principala de lumina a camerei.
    val panelPad = vendingWidth * 0.14f
    val warmColor = RoomTheme.accentWarm
    drawRect(
        color = warmColor.copy(alpha = 0.55f),
        topLeft = Offset(vendingLeft + panelPad, vendingTop + panelPad),
        size = Size(vendingWidth - panelPad * 2f, vendingHeight * 0.45f)
    )
    // Glow cald in jurul automatului.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(warmColor.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(vendingLeft + vendingWidth / 2f, vendingTop + vendingHeight * 0.3f),
            radius = vendingWidth * 2f
        ),
        topLeft = Offset(vendingLeft - vendingWidth * 0.6f, vendingTop - vendingHeight * 0.4f),
        size = Size(vendingWidth * 2.2f, vendingHeight * 1.6f)
    )
    // Butoane mici (2 randuri de puncte).
    for (row in 0..1) {
        for (col in 0..1) {
            drawCircle(
                RoomTheme.metalLight,
                radius = 2f,
                center = Offset(
                    vendingLeft + panelPad + col * (vendingWidth - panelPad * 2f),
                    vendingTop + vendingHeight * 0.65f + row * (vendingHeight * 0.12f)
                )
            )
        }
    }

    // --- Masa rotunda cu scaune, jos-centru (zona clar libera de orice acces) ---
    val tableCenterX = w * 0.56f
    val tableCenterY = h * 0.72f
    val tableRadius = w * 0.12f

    // Umbra mesei.
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = tableRadius * 1.05f,
        center = Offset(tableCenterX, tableCenterY + tableRadius * 0.12f)
    )
    // Blatul mesei.
    drawCircle(color = RoomTheme.metalDark, radius = tableRadius, center = Offset(tableCenterX, tableCenterY))
    drawCircle(
        color = RoomTheme.objectOutline,
        radius = tableRadius,
        center = Offset(tableCenterX, tableCenterY),
        style = Stroke(width = 2f)
    )
    // Reflexie calda discreta pe masa (lumina automatului ajunge pana aici).
    drawCircle(
        color = warmColor.copy(alpha = 0.08f),
        radius = tableRadius * 0.7f,
        center = Offset(tableCenterX, tableCenterY)
    )

    // 4 scaune in jurul mesei (cercuri mici, in cruce).
    val chairDist = tableRadius * 1.55f
    val chairRadius = tableRadius * 0.32f
    val chairOffsets = listOf(
        Offset(0f, -chairDist), Offset(0f, chairDist),
        Offset(-chairDist, 0f), Offset(chairDist, 0f)
    )
    chairOffsets.forEach { offset ->
        val cx = tableCenterX + offset.x
        val cy = tableCenterY + offset.y
        drawCircle(color = RoomTheme.fabricDark, radius = chairRadius, center = Offset(cx, cy))
        drawCircle(
            color = RoomTheme.objectOutline,
            radius = chairRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
    }

    drawRoomVignette(w, h)
}
// ============================================================================
// BIROURI - ambient birocratic, mai neutru, un singur punct cald de lumina
// ============================================================================

/**
 * Geometrie reala (BuildingLayout.kt): camera "office1" e x:1250-1600, y:2050-2300
 * (350x250). Singurul acces: hall_office1 intra prin peretele DREAPT, pe y:2125-2225
 * (local y: 75-175, centrul vertical al peretelui drept) -> acea zona ramane libera.
 * Mobilierul (2 birouri, dulap de dosare, lampa) e distribuit stanga si sus/jos,
 * evitand centrul-dreapta.
 */
fun DrawScope.drawOfficeRoomDetailed(w: Float, h: Float) {
    // Podeaua: gri-albastrui inchis, neutru - birou oficial, fara accent puternic de culoare.
    drawRect(color = Color(0xFF14171C), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 9f)

    // --- Biroul principal, lipit de peretele din STANGA (zona libera de acces) ---
    val desk1Width = w * 0.09f
    val desk1Height = h * 0.40f
    val desk1Left = w * 0.05f
    val desk1Top = h * 0.14f

    drawRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(desk1Left - 2f, desk1Top + desk1Height),
        size = Size(desk1Width + 4f, h * 0.02f)
    )
    solidRect(Offset(desk1Left, desk1Top), Size(desk1Width, desk1Height), RoomTheme.metalDark)
    // Blatul, usor mai deschis.
    drawRect(
        color = RoomTheme.metalLight.copy(alpha = 0.35f),
        topLeft = Offset(desk1Left, desk1Top),
        size = Size(desk1Width, desk1Height * 0.1f)
    )
    // Un teanc de dosare pe birou (cateva dreptunghiuri suprapuse usor decalate).
    val fileColors = listOf(Color(0xFF8A6A3A), Color(0xFF6E5530), Color(0xFF9C7B45))
    for (i in fileColors.indices) {
        drawRect(
            color = fileColors[i],
            topLeft = Offset(desk1Left + desk1Width * 0.15f, desk1Top + desk1Height * 0.2f - i * 3f),
            size = Size(desk1Width * 0.5f, desk1Height * 0.07f)
        )
    }
    // Lampa de birou - singura sursa calda de lumina din camera.
    val lampX = desk1Left + desk1Width * 0.78f
    val lampY = desk1Top + desk1Height * 0.35f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(RoomTheme.accentWarm.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(lampX, lampY),
            radius = w * 0.14f
        ),
        topLeft = Offset(lampX - w * 0.14f, lampY - w * 0.14f),
        size = Size(w * 0.28f, w * 0.28f)
    )
    drawCircle(RoomTheme.accentWarm, radius = 3.5f, center = Offset(lampX, lampY))
    drawCircle(RoomTheme.objectOutline, radius = 3.5f, center = Offset(lampX, lampY), style = Stroke(width = 1f))

    // Scaunul biroului principal.
    val chair1X = desk1Left + desk1Width * 0.5f
    val chair1Y = desk1Top + desk1Height + h * 0.06f
    drawCircle(RoomTheme.fabricDark, radius = w * 0.035f, center = Offset(chair1X, chair1Y))
    drawCircle(RoomTheme.objectOutline, radius = w * 0.035f, center = Offset(chair1X, chair1Y), style = Stroke(width = 1.5f))

    // --- Al doilea birou, lipit de peretele de SUS, spre centru-stanga (zona libera) ---
    val desk2Width = w * 0.30f
    val desk2Height = h * 0.09f
    val desk2Left = w * 0.24f
    val desk2Top = h * 0.03f

    drawRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(desk2Left - 2f, desk2Top + desk2Height),
        size = Size(desk2Width + 4f, h * 0.015f)
    )
    solidRect(Offset(desk2Left, desk2Top), Size(desk2Width, desk2Height), RoomTheme.metalDark)
    drawRect(
        color = RoomTheme.metalLight.copy(alpha = 0.3f),
        topLeft = Offset(desk2Left, desk2Top),
        size = Size(desk2Width, desk2Height * 0.2f)
    )
    // Un monitor mic, stins (computer de birou obisnuit, nu sursa de lumina).
    val monW = desk2Width * 0.18f
    val monH = desk2Height * 0.55f
    solidRect(
        Offset(desk2Left + desk2Width * 0.6f, desk2Top - monH * 0.5f),
        Size(monW, monH),
        RoomTheme.metalDarker
    )
    drawRect(
        color = Color(0xFF1A2226),
        topLeft = Offset(desk2Left + desk2Width * 0.6f + monW * 0.12f, desk2Top - monH * 0.5f + monH * 0.12f),
        size = Size(monW * 0.76f, monH * 0.76f)
    )

    val chair2X = desk2Left + desk2Width * 0.5f
    val chair2Y = desk2Top + desk2Height + h * 0.08f
    drawCircle(RoomTheme.fabricDark, radius = w * 0.035f, center = Offset(chair2X, chair2Y))
    drawCircle(RoomTheme.objectOutline, radius = w * 0.035f, center = Offset(chair2X, chair2Y), style = Stroke(width = 1.5f))

    // --- Dulap de dosare, jos-stanga (zona libera de accese) ---
    val cabinetW = w * 0.13f
    val cabinetH = h * 0.30f
    val cabinetLeft = w * 0.06f
    val cabinetTop = h * 0.66f
    solidRect(Offset(cabinetLeft, cabinetTop), Size(cabinetW, cabinetH), RoomTheme.metalDark)
    for (i in 1..3) {
        val ly = cabinetTop + cabinetH * (i / 4f)
        drawLine(
            RoomTheme.objectOutline.copy(alpha = 0.6f),
            Offset(cabinetLeft, ly),
            Offset(cabinetLeft + cabinetW, ly),
            strokeWidth = 1.5f
        )
        // Manere mici pe fiecare sertar.
        drawCircle(
            RoomTheme.metalLight,
            radius = 1.5f,
            center = Offset(cabinetLeft + cabinetW * 0.5f, ly - cabinetH * 0.06f)
        )
    }

    drawRoomVignette(w, h)
}
// ============================================================================
// BIROURI 2 - varianta a doua, layout diferit fata de office1 (evita repetitia)
// ============================================================================

/**
 * Geometrie reala (BuildingLayout.kt): camera "office2" e x:650-950, y:2050-2300
 * (300x250). Singurul acces: hall_office2 intra prin peretele DREAPT, pe y:2125-2225
 * (local y: 75-175, centrul vertical al peretelui drept) -> acea zona ramane libera.
 * Spre deosebire de office1 (2 birouri mici + dulap), aici e UN birou mare lipit de
 * peretele din STANGA plus un colt de asteptare (scaune) jos, pentru variatie vizuala.
 */
fun DrawScope.drawOffice2RoomDetailed(w: Float, h: Float) {
    // Aceeasi paleta neutra ca office1, ca ambele sa se simta din aceeasi cladire.
    drawRect(color = Color(0xFF14171C), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 8f)

    // --- Biroul mare, lipit de peretele din STANGA, orientat pe verticala ---
    val deskWidth = w * 0.11f
    val deskHeight = h * 0.55f
    val deskLeft = w * 0.06f
    val deskTop = h * 0.10f

    drawRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(deskLeft - 2f, deskTop + deskHeight),
        size = Size(deskWidth + 4f, h * 0.02f)
    )
    solidRect(Offset(deskLeft, deskTop), Size(deskWidth, deskHeight), RoomTheme.metalDark)
    drawRect(
        color = RoomTheme.metalLight.copy(alpha = 0.35f),
        topLeft = Offset(deskLeft, deskTop),
        size = Size(deskWidth, deskHeight * 0.06f)
    )

    // Monitor pe birou (stins, computer obisnuit).
    val monW = deskWidth * 0.6f
    val monH = deskHeight * 0.12f
    val monLeft = deskLeft + deskWidth * 0.2f
    val monTop = deskTop + deskHeight * 0.12f
    solidRect(Offset(monLeft, monTop), Size(monW, monH), RoomTheme.metalDarker)
    drawRect(
        color = Color(0xFF1A2226),
        topLeft = Offset(monLeft + monW * 0.12f, monTop + monH * 0.12f),
        size = Size(monW * 0.76f, monH * 0.76f)
    )

    // Teanc de dosare pe partea de jos a biroului.
    val fileColors = listOf(Color(0xFF8A6A3A), Color(0xFF6E5530))
    for (i in fileColors.indices) {
        drawRect(
            color = fileColors[i],
            topLeft = Offset(deskLeft + deskWidth * 0.15f, deskTop + deskHeight * 0.55f - i * 3f),
            size = Size(deskWidth * 0.7f, deskHeight * 0.08f)
        )
    }

    // Lampa de birou, sursa calda de lumina.
    val lampX = deskLeft + deskWidth * 0.5f
    val lampY = deskTop + deskHeight * 0.85f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(RoomTheme.accentWarm.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(lampX, lampY),
            radius = w * 0.15f
        ),
        topLeft = Offset(lampX - w * 0.15f, lampY - w * 0.15f),
        size = Size(w * 0.3f, w * 0.3f)
    )
    drawCircle(RoomTheme.accentWarm, radius = 3.5f, center = Offset(lampX, lampY))
    drawCircle(RoomTheme.objectOutline, radius = 3.5f, center = Offset(lampX, lampY), style = Stroke(width = 1f))

    // Scaunul biroului (langa el, spre centrul camerei).
    val chairX = deskLeft + deskWidth + w * 0.06f
    val chairY = deskTop + deskHeight * 0.5f
    drawCircle(RoomTheme.fabricDark, radius = w * 0.04f, center = Offset(chairX, chairY))
    drawCircle(RoomTheme.objectOutline, radius = w * 0.04f, center = Offset(chairX, chairY), style = Stroke(width = 1.5f))

    // --- Colt de asteptare, jos-centru/dreapta (zona libera de acces, sub zona de hol) ---
    // 2 scaune simple, distincte de scaunul de birou (fara birou langa ele).
    val waitChairRadius = w * 0.032f
    val waitY = h * 0.85f
    val waitPositions = listOf(w * 0.38f, w * 0.50f)
    waitPositions.forEach { wx ->
        drawCircle(RoomTheme.fabricDark, radius = waitChairRadius, center = Offset(wx, waitY))
        drawCircle(RoomTheme.objectOutline, radius = waitChairRadius, center = Offset(wx, waitY), style = Stroke(width = 1.5f))
    }
    // O masuta joasa langa scaunele de asteptare.
    val lowTableW = w * 0.08f
    val lowTableH = h * 0.05f
    solidRect(
        Offset(w * 0.44f - lowTableW / 2f, waitY - h * 0.11f),
        Size(lowTableW, lowTableH),
        RoomTheme.metalDark
    )

    // --- Un dulap ingust de dosare, sus-centru (zona libera) ---
    val cabinetW = w * 0.10f
    val cabinetH = h * 0.20f
    val cabinetLeft = w * 0.40f
    val cabinetTop = h * 0.04f
    solidRect(Offset(cabinetLeft, cabinetTop), Size(cabinetW, cabinetH), RoomTheme.metalDark)
    for (i in 1..2) {
        val ly = cabinetTop + cabinetH * (i / 3f)
        drawLine(
            RoomTheme.objectOutline.copy(alpha = 0.6f),
            Offset(cabinetLeft, ly),
            Offset(cabinetLeft + cabinetW, ly),
            strokeWidth = 1.5f
        )
    }

    drawRoomVignette(w, h)
}
// ============================================================================
// HOL CENTRAL - hub de trecere, fara task, doar atmosfera
// ============================================================================

/**
 * Geometrie reala (BuildingLayout.kt): camera "hub_central" e x:2000-2500, y:2000-2400
 * (500x400) - cea mai mare camera din harta. Are 4 accese, cate unul pe fiecare perete,
 * toate centrate pe mijlocul peretelui respectiv:
 * - hall_meeting intra SUS, x:2200-2300 (local x:200-300)
 * - hall_entrance intra JOS, x:2200-2300 (local x:200-300)
 * - hall_office1 intra STANGA, y:2125-2225 (local y:125-225)
 * - hall_break intra DREAPTA, y:2125-2225 (local y:125-225)
 * Centrul ramane complet liber (e o zona de trecere, nu are task). Decorul (banci,
 * plante, embleme) se aseaza STRICT in cele 4 colturi, care nu sunt atinse de niciun hol.
 */
fun DrawScope.drawHubCentralDetailed(w: Float, h: Float) {
    // Podeaua: gri neutru, putin mai deschis decat restul cladirii - un hol oficial,
    // nu o camera secreta, dar tot sobru si intunecat fata de un joc obisnuit.
    drawRect(color = Color(0xFF191C20), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 14f)

    // --- Emblema mare, discreta, in centrul podelei (element de atmosfera, fara sa
    // blocheze vizual trecerea prin hol) ---
    drawCircle(
        color = Color.White.copy(alpha = 0.03f),
        radius = w * 0.11f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = 3f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.02f),
        radius = w * 0.09f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = 1.5f)
    )

    // --- Colt STANGA-SUS: banca de asteptare + planta ---
    run {
        val benchW = w * 0.14f
        val benchH = h * 0.06f
        val benchLeft = w * 0.05f
        val benchTop = h * 0.06f
        drawRect(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(benchLeft - 2f, benchTop + benchH),
            size = Size(benchW + 4f, h * 0.012f)
        )
        solidRect(Offset(benchLeft, benchTop), Size(benchW, benchH), RoomTheme.fabricDark)

        // Planta - un ghiveci simplu cu un cerc verde inchis deasupra.
        val plantX = benchLeft + benchW + w * 0.03f
        val plantY = benchTop + benchH * 0.5f
        drawRect(
            color = RoomTheme.metalDark,
            topLeft = Offset(plantX - w * 0.012f, plantY),
            size = Size(w * 0.024f, h * 0.025f)
        )
        drawCircle(Color(0xFF1F3D24), radius = w * 0.022f, center = Offset(plantX, plantY - h * 0.01f))
        drawCircle(Color(0xFF2A5030), radius = w * 0.014f, center = Offset(plantX - w * 0.008f, plantY - h * 0.02f))
    }

    // --- Colt DREAPTA-SUS: panou/tablou pe perete + o lampa de perete discreta ---
    run {
        val panelW = w * 0.10f
        val panelH = h * 0.07f
        val panelLeft = w * 0.85f
        val panelTop = h * 0.05f
        solidRect(Offset(panelLeft, panelTop), Size(panelW, panelH), RoomTheme.metalDarker)
        drawRect(
            color = Color(0xFF2A3038),
            topLeft = Offset(panelLeft + panelW * 0.1f, panelTop + panelH * 0.1f),
            size = Size(panelW * 0.8f, panelH * 0.8f)
        )
        // Lumina de perete discreta deasupra panoului.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(panelLeft + panelW / 2f, panelTop),
                radius = w * 0.12f
            ),
            topLeft = Offset(panelLeft - w * 0.06f, panelTop - w * 0.12f),
            size = Size(panelW + w * 0.12f, w * 0.18f)
        )
    }

    // --- Colt STANGA-JOS: alta banca, orientata diferit ---
    run {
        val benchW = h * 0.06f
        val benchH = w * 0.14f
        val benchLeft = w * 0.05f
        val benchTop = h * 0.80f
        drawRect(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(benchLeft - 2f, benchTop + benchH),
            size = Size(benchW + 4f, h * 0.012f)
        )
        solidRect(Offset(benchLeft, benchTop), Size(benchW, benchH), RoomTheme.fabricDark)
    }

    // --- Colt DREAPTA-JOS: planta mare, decor final ---
    run {
        val plantX = w * 0.91f
        val plantY = h * 0.87f
        drawRect(
            color = RoomTheme.metalDark,
            topLeft = Offset(plantX - w * 0.018f, plantY),
            size = Size(w * 0.036f, h * 0.04f)
        )
        drawCircle(Color(0xFF1F3D24), radius = w * 0.032f, center = Offset(plantX, plantY - h * 0.015f))
        drawCircle(Color(0xFF2A5030), radius = w * 0.020f, center = Offset(plantX - w * 0.012f, plantY - h * 0.032f))
        drawCircle(Color(0xFF2A5030), radius = w * 0.016f, center = Offset(plantX + w * 0.014f, plantY - h * 0.028f))
    }

    drawRoomVignette(w, h)
}
// ============================================================================
// CAMERA SERVERE - rack-uri tehnice, lumina rece albastru-cyan
// ============================================================================

/**
 * Geometrie reala (BuildingLayout.kt): camera "server_room" e x:2900-3200, y:1100-1350
 * (300x250). Singurul acces: hall_server intra prin peretele de JOS, pe x:3000-3100
 * (local x:100-200, centru) -> acea zona ramane libera. Restul camerei (sus, stanga,
 * dreapta) e complet liber pentru rack-uri, deci punem 3 rack-uri mari lipite de
 * peretele de SUS si cate un rack mai mic pe fiecare parte laterala.
 */
fun DrawScope.drawServerRoomDetailed(w: Float, h: Float) {
    // Podeaua: albastru-cyan foarte inchis, aproape negru - camera tehnica, rece.
    drawRect(color = Color(0xFF0A1418), topLeft = Offset.Zero, size = Size(w, h))
    drawFloorGrid(w, h, cell = w / 9f)

    val cyanGlow = Color(0xFF3AC9E8)
    val cyanGlowDim = Color(0xFF1E7A8C)

    // --- 3 rack-uri mari, lipite de peretele de SUS ---
    val rackCount = 3
    val rackGap = w * 0.03f
    val racksAreaWidth = w * 0.86f
    val racksAreaLeft = w * 0.07f
    val rackWidth = (racksAreaWidth - rackGap * (rackCount - 1)) / rackCount
    val rackHeight = h * 0.42f
    val rackTop = h * 0.04f

    for (i in 0 until rackCount) {
        val rx = racksAreaLeft + i * (rackWidth + rackGap)

        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(rx - 2f, rackTop + rackHeight),
            size = Size(rackWidth + 4f, h * 0.015f)
        )
        solidRect(Offset(rx, rackTop), Size(rackWidth, rackHeight), RoomTheme.metalDarker)

        // Randuri de LED-uri pe fiecare rack (simuland unitati de server active).
        val ledRows = 6
        for (row in 0 until ledRows) {
            val ly = rackTop + rackHeight * 0.08f + row * (rackHeight * 0.82f / ledRows)
            // Fiecare rand are 2-3 LED-uri de culori diferite (status simulat).
            val ledColor = when {
                row % 5 == 0 -> Color(0xFFB33A3A) // ocazional un LED rosu (alerta minora)
                row % 2 == 0 -> cyanGlow
                else -> cyanGlowDim
            }
            drawRect(
                color = ledColor,
                topLeft = Offset(rx + rackWidth * 0.12f, ly),
                size = Size(rackWidth * 0.12f, rackHeight * 0.025f)
            )
            drawRect(
                color = RoomTheme.metalLight.copy(alpha = 0.4f),
                topLeft = Offset(rx + rackWidth * 0.3f, ly),
                size = Size(rackWidth * 0.55f, rackHeight * 0.025f)
            )
        }

        // Glow tehnic (cyan) in jurul rack-ului din mijloc - sursa principala de lumina.
        if (i == 1) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(cyanGlow.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(rx + rackWidth / 2f, rackTop + rackHeight / 2f),
                    radius = rackWidth * 1.6f
                ),
                topLeft = Offset(rx - rackWidth * 0.5f, rackTop - rackHeight * 0.3f),
                size = Size(rackWidth * 2f, rackHeight * 1.6f)
            )
        }
    }

    // --- Rack lateral mic, STANGA-JOS (zona libera, langa dar nu peste acces) ---
    val sideRackW = w * 0.10f
    val sideRackH = h * 0.30f
    solidRect(Offset(w * 0.04f, h * 0.62f), Size(sideRackW, sideRackH), RoomTheme.metalDark)
    for (i in 1..3) {
        val ly = h * 0.62f + sideRackH * (i / 4f)
        drawRect(
            color = if (i % 2 == 0) cyanGlow.copy(alpha = 0.7f) else cyanGlowDim,
            topLeft = Offset(w * 0.04f + sideRackW * 0.2f, ly),
            size = Size(sideRackW * 0.6f, sideRackH * 0.04f)
        )
    }

    // --- Rack lateral mic, DREAPTA-JOS (simetric, zona libera) ---
    solidRect(Offset(w * 0.86f, h * 0.62f), Size(sideRackW, sideRackH), RoomTheme.metalDark)
    for (i in 1..3) {
        val ly = h * 0.62f + sideRackH * (i / 4f)
        drawRect(
            color = if (i % 2 == 1) cyanGlow.copy(alpha = 0.7f) else cyanGlowDim,
            topLeft = Offset(w * 0.86f + sideRackW * 0.2f, ly),
            size = Size(sideRackW * 0.6f, sideRackH * 0.04f)
        )
    }

    // --- Cateva cabluri simple pe podea, intre rack-urile de sus si cele laterale ---
    drawLine(
        RoomTheme.objectOutline.copy(alpha = 0.5f),
        Offset(w * 0.09f, rackTop + rackHeight),
        Offset(w * 0.09f, h * 0.62f),
        strokeWidth = 2f
    )
    drawLine(
        RoomTheme.objectOutline.copy(alpha = 0.5f),
        Offset(w * 0.91f, rackTop + rackHeight),
        Offset(w * 0.91f, h * 0.62f),
        strokeWidth = 2f
    )

    drawRoomVignette(w, h)
}