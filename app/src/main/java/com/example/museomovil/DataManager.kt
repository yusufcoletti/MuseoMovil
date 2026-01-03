package com.example.museomovil

import android.content.Context

class DataManager(context: Context) {
    // Usamos un nombre de archivo consistente
    private val PREFS_NAME = "MuseoPrefs"
    private val KEY_DESBLOQUEADOS = "aviones_desbloqueados_ids"

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 1. Guardar avión (Añade el ID a la lista existente)
    fun desbloquearAvion(idAvion: String) {
        val setActual = prefs.getStringSet(KEY_DESBLOQUEADOS, mutableSetOf()) ?: mutableSetOf()
        val nuevoSet = setActual.toMutableSet() // Copia editable
        nuevoSet.add(idAvion) // Añadimos el nuevo
        prefs.edit().putStringSet(KEY_DESBLOQUEADOS, nuevoSet).apply() // Guardamos
    }

    // 2. Comprobar si está desbloqueado (Busca en la lista)
    fun estaDesbloqueado(idAvion: String): Boolean {
        val setIds = prefs.getStringSet(KEY_DESBLOQUEADOS, emptySet())
        return setIds?.contains(idAvion) == true
    }

    // 3. Contar cuántos hay (ESTA ES LA QUE TE FALTA Y DA ERROR EN ROJO)
    fun getAvionesDesbloqueadosCount(): Int {
        val setIds = prefs.getStringSet(KEY_DESBLOQUEADOS, emptySet())
        return setIds?.size ?: 0
    }

    // Extra: Borrar todo (útil para pruebas)
    fun resetearProgreso() {
        prefs.edit().clear().apply()
    }
}