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
        "DUST_FOR_PRINTS" to FbiTaskMeta(
            title = "Cauta amprente",
            description = "Presara praf si sufla usor, fara sa strici urma.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "INTERVIEW_WITNESS" to FbiTaskMeta(
            title = "Ia interviu unui martor",
            description = "Pune intrebarile potrivite ca sa afli adevarul.",
            durationSeconds = 4f,
            accentColor = Color(0xFF1976D2)
        ),
        "LOG_EVIDENCE_CHAIN" to FbiTaskMeta(
            title = "Completeaza lantul de custodie",
            description = "Noteaza in ordine fiecare transfer al probei.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "PATROL_CAMERA_FEED" to FbiTaskMeta(
            title = "Patruleaza feed-urile video",
            description = "Gaseste camera cu semnal anormal.",
            durationSeconds = 4f,
            accentColor = Color(0xFF1976D2)
        ),
        "RUN_BACKGROUND_CHECK" to FbiTaskMeta(
            title = "Ruleaza verificare de fond",
            description = "Introdu codul de acces corect in sistem.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "SWEEP_FOR_BUGS" to FbiTaskMeta(
            title = "Cauta dispozitive de ascultare",
            description = "Plimba senzorul pana gasesti varful de semnal.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "VERIFY_ID_DOCUMENTS" to FbiTaskMeta(
            title = "Verifica actele de identitate",
            description = "Compara cele doua documente si gaseste diferenta.",
            durationSeconds = 4f,
            accentColor = Color(0xFF1976D2)
        ),
        "RESTOCK_AMMO" to FbiTaskMeta(
            title = "Reaprovizioneaza munitia",
            description = "Confirma cantitatea corecta din depozit.",
            durationSeconds = 3f,
            accentColor = Color(0xFF1976D2)
        ),
        "SIGN_OUT_WEAPON" to FbiTaskMeta(
            title = "Semneaza predarea armei",
            description = "Bifeaza pasii formularului de predare, in ordine.",
            durationSeconds = 4f,
            accentColor = Color(0xFF1976D2)
        ),
        "CATALOG_DNA_SAMPLE" to FbiTaskMeta(
            title = "Cataloghezi o proba ADN",
            description = "Aseaza proba la locul potrivit din arhiva.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "CROSS_REFERENCE_RECORDS" to FbiTaskMeta(
            title = "Incruciseaza dosarele",
            description = "Gaseste cele doua dosare care se potrivesc.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "BRIEF_THE_TEAM" to FbiTaskMeta(
            title = "Pregateste briefing-ul echipei",
            description = "Pune diapozitivele in ordinea corecta.",
            durationSeconds = 4f,
            accentColor = Color(0xFF1976D2)
        ),
        "SECURE_PERIMETER" to FbiTaskMeta(
            title = "Asigura perimetrul",
            description = "Verifica fiecare punct de control, in ordine.",
            durationSeconds = 5f,
            accentColor = Color(0xFF1976D2)
        ),
        "IDENTIFY_REMAINS" to FbiTaskMeta(
            title = "Identifica ramasitele",
            description = "Potriveste fisa cu profilul corect.",
            durationSeconds = 6f,
            accentColor = Color(0xFF1976D2)
        ),
        "REFILL_COFFEE_MACHINE" to FbiTaskMeta(
            title = "Umple automatul de cafea",
            description = "Apasa butoanele in ordinea corecta.",
            durationSeconds = 2f,
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
