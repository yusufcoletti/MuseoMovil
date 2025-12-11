package com.example.museomovil

data class Avion(
    val nombre: String,
    val descripcion: String,
    val imagenResId: Int,   // El ID de la foto (R.drawable.avion1)
    var isDesbloqueado: Boolean = false // ¿Está desbloqueado? Por defecto NO
)