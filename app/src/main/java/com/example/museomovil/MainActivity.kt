package com.example.museomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Botón INICIAR VISITA
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)
        btnIniciar.setOnClickListener {
            // En Kotlin, pasar de una pantalla a otra es así de simple:
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        // 2. Botón SONIDO
        val btnSonido = findViewById<FloatingActionButton>(R.id.btnSonido)
        btnSonido.setOnClickListener {
            Toast.makeText(this, "Sonido desactivado", Toast.LENGTH_SHORT).show()
        }

        // 3. Botón IDIOMA
        val btnIdioma = findViewById<FloatingActionButton>(R.id.btnIdioma)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }
    }
}