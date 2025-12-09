package com.example.museomovil

import android.content.Context // NUEVO: Necesario para el servicio de audio
import android.content.Intent
import android.media.AudioManager // NUEVO: Importamos el gestor de audio
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MenuActivity : AppCompatActivity() {

    // --- NUEVO: Variables para controlar el sonido también aquí ---
    private var isMuted = false
    private var previousVolume = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // --- NUEVO: Preparamos el Gestor de Audio ---
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // --- Navegación de las Tarjetas (Esto ya lo tenías) ---
        val card3D = findViewById<CardView>(R.id.cardVista3D)
        card3D.setOnClickListener {
            val intent = Intent(this, Vista3DActivity::class.java)
            startActivity(intent)
        }

        // --- CONFIGURACIÓN BOTÓN IDIOMA (Simple) ---
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdiomaMenu)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }


        // -----------------------------------------------------------
        // NUEVO: CONFIGURACIÓN BOTÓN SONIDO INTERACTIVO (Menú)
        // -----------------------------------------------------------
        // Nota: Asegúrate de que el ID aquí coincida con el de tu XML activity_menu.xml (ej: btnSonidoMenu)
        val btnSonido = findViewById<ImageButton>(R.id.btnSonidoMenu)

        // 1. Comprobar estado inicial al abrir el menú
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume == 0) {
            isMuted = true
            btnSonido.setImageResource(R.drawable.ic_volume_off)
        } else {
            isMuted = false
            btnSonido.setImageResource(R.drawable.ic_volume_on)
        }

        // 2. Lógica del Clic (Idéntica a la de MainActivity)
        btnSonido.setOnClickListener {
            if (isMuted) {
                // ACTIVAR SONIDO
                val targetVolume = if (previousVolume == 0)
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
                else previousVolume

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                btnSonido.setImageResource(R.drawable.ic_volume_on)
                isMuted = false
                Toast.makeText(this, "Sonido Activado", Toast.LENGTH_SHORT).show()
            } else {
                // SILENCIAR
                previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                btnSonido.setImageResource(R.drawable.ic_volume_off)
                isMuted = true
                Toast.makeText(this, "Silenciado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}