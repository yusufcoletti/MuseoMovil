package com.example.museomovil

data class Avion(
    val id: String,
    val nombre: String,
    val descripcion: String, // Info breve (la que sale al principio)
    val descripcionDetallada: String, // Info completa (historia, datos técnicos...)
    val imagenResId: Int,   // El ID de la foto (R.drawable.avion1)
    var isDesbloqueado: Boolean = false, // ¿Está desbloqueado? Por defecto NO
    val modelo3D: String? = null // Ruta del archivo .glb para vista 3D (opcional)
)