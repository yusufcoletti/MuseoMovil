package com.example.museomovil

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class CatalogoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo)

        // ---------------------------------------------------------------
        // BASE DE DATOS DE AERONAVES (Simulada)
        // ---------------------------------------------------------------
        // Nota: He puesto R.drawable.ic_view_3d como imagen temporal.
        // Cuando tengas las fotos reales, subelas a 'drawable' y cambia el ID aquí.
        // ---------------------------------------------------------------

        val catalogoCompleto = listOf(
            // --- SALA 1: PLANEADORES Y PIONEROS ---
            Avion(
                nombre = "Dumont Demoiselle",
                descripcion = "SALA 1. El precursor de la aviación ultraligera. Un diseño elegante y pequeño creado por Santos-Dumont en 1907.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = true // ¡Este te lo regalo desbloqueado para probar!
            ),
            Avion(
                nombre = "Globo Aerostático",
                descripcion = "SALA 1. El primer paso del hombre hacia el cielo. Funciona por el principio de Arquímedes con aire caliente.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Tornillo Aéreo (Da Vinci)",
                descripcion = "SALA 1. El 'bisabuelo' del helicóptero. Un diseño visionario de Leonardo da Vinci que imaginaba la elevación vertical.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Planeador Da Vinci",
                descripcion = "SALA 1. Ornitóptero basado en la anatomía de los pájaros y murciélagos. Ingeniería renacentista pura.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),

            // --- SALA 2: AVIONES DE GUERRA ---
            Avion(
                nombre = "Messerschmitt Bf 109",
                descripcion = "SALA 2. La columna vertebral de la Luftwaffe alemana. Un caza rápido y versátil de la Segunda Guerra Mundial.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Boeing B-17",
                descripcion = "SALA 2. La 'Fortaleza Volante'. Bombardero pesado famoso por su resistencia y capacidad defensiva.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Sopwith Camel",
                descripcion = "SALA 2. El caza británico más exitoso de la Primera Guerra Mundial, famoso por su maniobrabilidad.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Red Baron (Fokker Dr.I)",
                descripcion = "SALA 2. El icónico triplano rojo pilotado por Manfred von Richthofen, el as de ases alemán.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),

            // --- SALA 3: AVIONES COMERCIALES ---
            Avion(
                nombre = "Douglas DC-3",
                descripcion = "SALA 3. El avión que revolucionó el transporte aéreo en los años 30 y 40. Fiabilidad legendaria.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Boeing 747",
                descripcion = "SALA 3. 'La Reina de los Cielos'. El primer avión de fuselaje ancho que democratizó los viajes internacionales.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Airbus A320",
                descripcion = "SALA 3. Pionero en tecnología fly-by-wire digital. El caballo de batalla de las aerolíneas modernas.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Boeing 787 Dreamliner",
                descripcion = "SALA 3. Eficiencia y tecnología. Construido con materiales compuestos para un vuelo más suave y ecológico.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),

            // --- SALA 4: HELICÓPTEROS ---
            Avion(
                nombre = "AH-64 Apache",
                descripcion = "SALA 4. Helicóptero de ataque avanzado. Diseñado para sobrevivir en combate y atacar desde la distancia.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Bell 206 JetRanger",
                descripcion = "SALA 4. Uno de los helicópteros más populares del mundo, usado para noticias, policía y transporte VIP.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "UH-1Y Venom",
                descripcion = "SALA 4. La evolución moderna del famoso 'Huey'. Un helicóptero utilitario versátil y potente.",
                imagenResId = R.drawable.ic_view_3d,
                isDesbloqueado = false
            )
        )

        // CONFIGURACIÓN BOTÓN VOLVER
        val btnVolver = findViewById<android.widget.ImageButton>(R.id.btnVolver)
        btnVolver.setOnClickListener {
            finish() // Esto cierra la actividad y vuelve al Menú
        }

        // ---------------------------------------------------------------
        // CONFIGURACIÓN VISUAL (Carrusel)
        // ---------------------------------------------------------------
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerCatalogo)
        val adapter = AvionAdapter(catalogoCompleto, this)
        viewPager.adapter = adapter

        // Animación elegante al deslizar
        viewPager.setPageTransformer { page, position ->
            val absPos = Math.abs(position)
            page.alpha = 1f - (absPos * 0.3f)
            page.scaleY = 0.85f + (0.15f * (1f - absPos))
        }
    }
}