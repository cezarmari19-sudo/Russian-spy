package com.astran.russianspy.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Stocare locala simpla (SharedPreferences) pentru datele contului jucatorului -
 * numele si accountId-ul, ca sa nu mai fie nevoie sa retastezi numele si ca sa
 * pastreze aceeasi identitate de cont intre sesiuni.
 *
 * NOTA: accountId e generat o singura data si salvat local (NU e acelasi lucru
 * cu player_id-ul folosit in timpul unei partide, care e efemer si se genereaza
 * din nou la fiecare joc nou). Cat timp nu exista autentificare reala prin email
 * (task viitor), accountId-ul e legat de telefon: daca dezinstalezi aplicatia
 * sau schimbi telefonul, pierzi contul si codul de prieteni.
 */
object PlayerPrefs {
    private const val PREFS_NAME = "russian_spy_player_prefs"
    private const val KEY_PLAYER_NAME = "player_name"
    private const val KEY_ACCOUNT_ID = "account_id"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPlayerName(context: Context): String {
        return prefs(context).getString(KEY_PLAYER_NAME, "") ?: ""
    }

    fun setPlayerName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    fun hasPlayerName(context: Context): Boolean = getPlayerName(context).isNotBlank()

    /** ID stabil de cont, generat o singura data si pastrat pe telefon. */
    fun getAccountId(context: Context): String {
        val existing = prefs(context).getString(KEY_ACCOUNT_ID, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs(context).edit().putString(KEY_ACCOUNT_ID, newId).apply()
        return newId
    }
}