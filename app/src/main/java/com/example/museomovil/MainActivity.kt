package com.example.museomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // -----------------------------------------------------------
        // 1. CONFIGURACIÓN DEL BOTÓN "INICIAR VISITA"
        // -----------------------------------------------------------
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)

        btnIniciar.setOnClickListener {
            // Navegamos hacia el Menú Principal
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        // -----------------------------------------------------------
        // 2. CONFIGURACIÓN DEL BOTÓN DE SONIDO (Abajo Izquierda)
        // -----------------------------------------------------------
        // Nota: En el diseño nuevo usamos <ImageButton>, asegúrate de que coincida
        val btnSonido = findViewById<ImageButton>(R.id.btnSonido)

        btnSonido.setOnClickListener {
            Toast.makeText(this, "Sonido desactivado", Toast.LENGTH_SHORT).show()
        }

        // -----------------------------------------------------------
        // 3. CONFIGURACIÓN DEL BOTÓN DE IDIOMA (Abajo Derecha)
        // -----------------------------------------------------------
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdioma)

        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }
    }
}