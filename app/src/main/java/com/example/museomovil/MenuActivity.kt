package com.example.museomovil

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MenuActivity : AppCompatActivity() {

    private var isMuted = false
    private var previousVolume = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // -------------------------------------------------
        // 1. BOTÓN CATÁLOGO (¡EL QUE FALTABA!)
        // -------------------------------------------------
        val cardCatalogo = findViewById<CardView>(R.id.cardCatalogo)
        cardCatalogo.setOnClickListener {
            val intent = Intent(this, CatalogoActivity::class.java)
            startActivity(intent)
        }

        // -------------------------------------------------
        // 2. BOTÓN VISTAS 3D / AR
        // -------------------------------------------------
        // Nota: Asegúrate de que en el XML el ID sea 'cardVista3D' o 'cardVistaAR', usa el que tengas puesto.
        // Si tu ID en el XML es cardVista3D, cambia lo de abajo.
        val card3D = findViewById<CardView>(R.id.cardVistaAR)
        card3D.setOnClickListener {
            val intent = Intent(this, ArActivity::class.java) // O ArActivity si es lo que usas
            startActivity(intent)
        }

        // -------------------------------------------------
        // 3. BOTÓN MAPA (Opcional, para que no falle si lo pulsas)
        // -------------------------------------------------
        /* val cardMapa = findViewById<CardView>(R.id.cardMapa) // Asegúrate del ID en el XML
        cardMapa.setOnClickListener {
             Toast.makeText(this, "Mapa próximamente", Toast.LENGTH_SHORT).show()
        }
        */

        // -------------------------------------------------
        // 4. BOTÓN NFC (Opcional)
        // -------------------------------------------------
        /*
        val cardNFC = findViewById<CardView>(R.id.cardNFC) // Asegúrate del ID en el XML
        cardNFC.setOnClickListener {
             Toast.makeText(this, "Escaneando...", Toast.LENGTH_SHORT).show()
        }
        */

        // -------------------------------------------------
        // CONFIGURACIÓN IDIOMA
        // -------------------------------------------------
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdiomaMenu)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }

        // -------------------------------------------------
        // CONFIGURACIÓN SONIDO
        // -------------------------------------------------
        val btnSonido = findViewById<ImageButton>(R.id.btnSonidoMenu)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (currentVolume == 0) {
            isMuted = true
            btnSonido.setImageResource(R.drawable.ic_volume_off)
        } else {
            isMuted = false
            btnSonido.setImageResource(R.drawable.ic_volume_on)
        }

        btnSonido.setOnClickListener {
            if (isMuted) {
                val targetVolume = if (previousVolume == 0)
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
                else previousVolume
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                btnSonido.setImageResource(R.drawable.ic_volume_on)
                isMuted = false
                Toast.makeText(this, "Sonido Activado", Toast.LENGTH_SHORT).show()
            } else {
                previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                btnSonido.setImageResource(R.drawable.ic_volume_off)
                isMuted = true
                Toast.makeText(this, "Silenciado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}