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
        "BUG_PHONE_LINE" to SpyTaskMeta(
            title = "Intercepteaza linia telefonica",
            description = "Potriveste frecventa exacta ca sa asculti convorbirile.",
            durationSeconds = 5f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "COPY_KEYCARD" to SpyTaskMeta(
            title = "Cloneaza cartela de acces",
            description = "Copiaza datele cartelei inainte sa fii vazut.",
            durationSeconds = 3f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "BRIBE_GUARD" to SpyTaskMeta(
            title = "Mituieste garda",
            description = "Alege replicile potrivite ca sa convingi garda.",
            durationSeconds = 4f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "SABOTAGE_ALARM" to SpyTaskMeta(
            title = "Sabotarea alarmei",
            description = "Taie firul potrivit ca sa dezactivezi alarma.",
            durationSeconds = 6f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "SMUGGLE_WEAPON" to SpyTaskMeta(
            title = "Introduce o arma",
            description = "Ascunde arma printre obiecte, fara sa fii prins.",
            durationSeconds = 9f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "DECODE_INTERCEPT" to SpyTaskMeta(
            title = "Decodeaza mesajul interceptat",
            description = "Descifreaza mesajul litera cu litera.",
            durationSeconds = 6f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "FORGE_SIGNATURE" to SpyTaskMeta(
            title = "Falsifica o semnatura",
            description = "Traseaza semnatura urmand modelul original.",
            durationSeconds = 3f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "SEARCH_FILES" to SpyTaskMeta(
            title = "Cauta documentul potrivit",
            description = "Gaseste dosarul cautat printre celelalte.",
            durationSeconds = 5f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "TAMPER_DNA_SAMPLE" to SpyTaskMeta(
            title = "Altereaza proba ADN",
            description = "Amesteca reactivii in ordinea corecta ca sa strici proba.",
            durationSeconds = 8f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "SWAP_DNA_LABEL" to SpyTaskMeta(
            title = "Schimba etichetele probelor",
            description = "Potriveste etichetele ca sa incurci probele ADN.",
            durationSeconds = 5f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "POISON_COFFEE" to SpyTaskMeta(
            title = "Otraveste cafeaua",
            description = "Picura otrava in ceasca tinta, fara sa fii prins.",
            durationSeconds = 3f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "EAVESDROP_CONVERSATION" to SpyTaskMeta(
            title = "Asculta o conversatie",
            description = "Prinde momentele cheie din conversatia din camera alaturata.",
            durationSeconds = 5f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "UPLOAD_VIRUS" to SpyTaskMeta(
            title = "Incarca un virus",
            description = "Injecteaza codul in server fara sa activezi protectia.",
            durationSeconds = 9f,
            accentColor = Color(0xFFB3261E),
            canBeDisabledByFbi = false
        ),
        "DISPOSE_BODY_EVIDENCE" to SpyTaskMeta(
            title = "Elimina probe compromitatoare",
            description = "Muta obiectele la locul potrivit ca sa nu ridice suspiciuni.",
            durationSeconds = 6f,
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