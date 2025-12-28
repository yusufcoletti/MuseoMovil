package com.example.museomovil

import android.content.Context

class DataManager(context: Context) {
    private val prefs = context.getSharedPreferences("MuseoPrefs", Context.MODE_PRIVATE)

    // Guarda el ID del avión como desbloqueado (true)
    fun desbloquearAvion(id: String) {
        prefs.edit().putBoolean(id, true).apply()
    }

    // Comprueba si un avión ya ha sido escaneado antes
    fun estaDesbloqueado(id: String): Boolean {
        return prefs.getBoolean(id, false)
    }
}