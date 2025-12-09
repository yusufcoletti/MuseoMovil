package com.example.museomovil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Referencia a la tarjeta de Vistas 3D
        val card3D = findViewById<CardView>(R.id.cardVista3D)

        card3D.setOnClickListener {
            // Navegar a la actividad 3D
            val intent = Intent(this, Vista3DActivity::class.java)
            startActivity(intent)
        }

        // Aquí puedes añadir los listeners para las otras tarjetas (Catálogo, etc.)
    }
}