package com.astran.russianspy.model

import androidx.compose.ui.graphics.Color

/**
 * Metadate de AFISARE pentru fiecare tip de task COSMETIC de agent FBI (nume,
 * descriere, durata, culoare) - fara niciun efect real in joc, doar ocupatie.
 * Durata TREBUIE sa ramana identica cu server/models.py::FBI_TASK_DURATIONS_SECONDS.
 */
data class FbiTaskMeta(
    val title: String,
    val description: String,
    val durationSeconds: Float,
    val accentColor: Color
)

object FbiTaskCatalog {
    private val meta = mapOf(
        "CHECK_EVIDENCE_LOCKER" to FbiTaskMeta(
            title = "Verifica dulapul de probe",
            description = "Confirma ca toate probele sunt sigilate corect.",
            durationSeconds = 3f,
            accentColor = Color(0xFF1976D2)
        ),
        "FILE_INCIDENT_REPORT" to FbiTaskMeta(
            title = "Completeaza raport de incident",
            description = "Scrie raportul de rutina pentru tura curenta.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "INSPECT_BADGE_SCANNER" to FbiTaskMeta(
            title = "Inspecteaza scanerul de insigne",
            description = "Verifica daca scanerul de la intrare functioneaza corect.",
            durationSeconds = 2f,
            accentColor = Color(0xFF1976D2)
        ),
        "CALIBRATE_METAL_DETECTOR" to FbiTaskMeta(
            title = "Calibreaza detectorul de metale",
            description = "Ajusteaza sensibilitatea detectorului din armurerie.",
            durationSeconds = 6f,
            accentColor = Color(0xFF1976D2)
        ),
        "REVIEW_PERSONNEL_FILES" to FbiTaskMeta(
            title = "Revizuieste dosarele de personal",
            description = "Confirma ca toate dosarele sunt la zi.",
            durationSeconds = 4f,
            accentColor = Color(0xFF1976D2)
        ),
    )

    fun get(taskType: String): FbiTaskMeta = meta[taskType] ?: FbiTaskMeta(
        title = "Task necunoscut",
        description = "",
        durationSeconds = 4f,
        accentColor = Color(0xFF1976D2)
    )
}
