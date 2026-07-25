package com.astran.russianspy.model

import androidx.compose.ui.graphics.Color

/**
 * Metadate de AFISARE pentru fiecare tip de task de spion (nume, descriere,
 * durata, culoare). Durata TREBUIE sa ramana identica cu
 * server/models.py::SPY_TASK_DURATIONS_SECONDS - serverul valideaza oricum
 * independent (complete_spy_task nu verifica timpul exact, se bazeaza pe
 * faptul ca UI-ul clientului nu permite completarea mai devreme), dar daca
 * valorile difera intre client si server experienta devine inconsistenta.
 */
data class SpyTaskMeta(
    val title: String,
    val description: String,
    val durationSeconds: Float,
    val accentColor: Color,
    /** Doar PLANT_LISTENING_DEVICE si HACK_SURVEILLANCE_CAMERA pot fi gasite si dezactivate de un agent FBI. */
    val canBeDisabledByFbi: Boolean
)

object SpyTaskCatalog {
    private val meta = mapOf(
        "PHOTOGRAPH_DOCUMENTS" to SpyTaskMeta(
            title = "Fotografiaza documente",
            description = "Fa poze discret documentelor din aceasta camera.",
            durationSeconds = 3f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "STEAL_KEYS" to SpyTaskMeta(
            title = "Fura chei",
            description = "Ia cheile fara sa fii observat.",
            durationSeconds = 2f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "PLANT_LISTENING_DEVICE" to SpyTaskMeta(
            title = "Plaseaza dispozitiv de ascultare",
            description = "Ascunde un microfon in aceasta camera.",
            durationSeconds = 6f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = true
        ),
        "HACK_SURVEILLANCE_CAMERA" to SpyTaskMeta(
            title = "Instaleaza sistem de spionat",
            description = "Compromite camera de supraveghere ca Moscova sa vada prin ea.",
            durationSeconds = 10f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = true
        ),
        "SEND_ENCRYPTED_MESSAGE" to SpyTaskMeta(
            title = "Trimite mesaj cifrat",
            description = "Transmite informatiile adunate catre Moscova.",
            durationSeconds = 5f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "ERASE_FORENSIC_EVIDENCE" to SpyTaskMeta(
            title = "Sterge probe criminalistice",
            description = "Elimina orice urma care te-ar putea demasca.",
            durationSeconds = 8f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
    )

    fun get(taskType: String): SpyTaskMeta = meta[taskType] ?: SpyTaskMeta(
        title = "Task necunoscut",
        description = "",
        durationSeconds = 4f,
        accentColor = Color(0xFFB3261E),
        canBeDisabledByFbi = false
    )
}