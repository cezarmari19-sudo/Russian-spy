package com.astran.russianspy.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Stocare locala simpla (SharedPreferences) pentru datele contului jucatorului -
 * momentan doar numele, ca sa nu mai fie nevoie sa-l retastezi de fiecare data cand
 * deschizi aplicatia. Ramane salvat pe telefon intre sesiuni.
 */
object PlayerPrefs {
    private const val PREFS_NAME = "russian_spy_player_prefs"
    private const val KEY_PLAYER_NAME = "player_name"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPlayerName(context: Context): String {
        return prefs(context).getString(KEY_PLAYER_NAME, "") ?: ""
    }

    fun setPlayerName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    fun hasPlayerName(context: Context): Boolean = getPlayerName(context).isNotBlank()
}