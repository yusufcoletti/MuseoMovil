package com.example.museomovil

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class MainActivity : AppCompatActivity() {

    // Variables para controlar el sonido
    private var isMuted = false // ¿Está silenciado ahora mismo?
    private var previousVolume = 0 // Para recordar el volumen que tenía el usuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Preparamos el Gestor de Audio del sistema
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // -----------------------------------------------------------
        // CONFIGURACIÓN BOTÓN INICIAR
        // -----------------------------------------------------------
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)
        btnIniciar.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        // -----------------------------------------------------------
        // CONFIGURACIÓN BOTÓN SONIDO (INTERACTIVO)
        // -----------------------------------------------------------
        val btnSonido = findViewById<ImageButton>(R.id.btnSonido)

        // Primero: Verificamos cómo está el volumen al abrir la app para poner el icono correcto
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume == 0) {
            isMuted = true
            btnSonido.setImageResource(R.drawable.ic_volume_off) // Icono tachado
        } else {
            isMuted = false
            btnSonido.setImageResource(R.drawable.ic_volume_on) // Icono con ondas
        }

        // Segundo: Qué pasa al hacer CLIC
        btnSonido.setOnClickListener {
            if (isMuted) {
                // --- CASO 1: QUIERO ACTIVAR SONIDO ---
                // Recuperamos el volumen anterior (o ponemos un nivel medio si era 0)
                val targetVolume = if (previousVolume == 0)
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
                else previousVolume

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)

                // Cambiamos icono y bandera
                btnSonido.setImageResource(R.drawable.ic_volume_on)
                isMuted = false
                Toast.makeText(this, "Sonido Activado", Toast.LENGTH_SHORT).show()

            } else {
                // --- CASO 2: QUIERO SILENCIAR ---
                // Guardamos el volumen actual antes de borrarlo
                previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                // Ponemos el volumen a 0
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)

                // Cambiamos icono y bandera
                btnSonido.setImageResource(R.drawable.ic_volume_off)
                isMuted = true
                Toast.makeText(this, "Silenciado", Toast.LENGTH_SHORT).show()
            }
        }

        // -----------------------------------------------------------
        // CONFIGURACIÓN BOTÓN IDIOMA
        // -----------------------------------------------------------
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdioma)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }
    }
}