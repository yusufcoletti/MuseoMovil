package com.example.museomovil

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner

// Definición de datos de la sala
data class SalaMuseo(
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val audioRes: Int,
    val imgRes: Int,
    val pista: String
)

class TourActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI Elements
    private lateinit var txtEstado: TextView
    private lateinit var txtInstruccion: TextView
    private lateinit var txtDistanciaRestante: TextView
    private lateinit var imgLugar: ImageView
    private lateinit var spinnerDestinos: Spinner
    private lateinit var btnAtras: ImageButton

    private var listaSalas: List<SalaMuseo> = emptyList()

    // Variables de lógica
    private var mediaPlayer: MediaPlayer? = null
    private var salaActual: SalaMuseo? = null
    private var salaDestino: SalaMuseo? = null // La sala que el usuario elige ir
    private var salasInicializadas = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tour)

        // Vincular Vistas
        txtEstado = findViewById(R.id.txtEstado)
        txtInstruccion = findViewById(R.id.txtInstruccion)
        txtDistanciaRestante = findViewById(R.id.txtDistanciaRestante)
        imgLugar = findViewById(R.id.imgLugar)
        spinnerDestinos = findViewById(R.id.spinnerDestinos)
        btnAtras = findViewById(R.id.btnAtras)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. CONFIGURAR BOTÓN ATRÁS
        btnAtras.setOnClickListener {
            mediaPlayer?.stop() // Parar audio si suena
            finish() // Cierra esta actividad y vuelve al menú anterior
        }

        // Configurar salas del museo
        generarSalasDePrueba(null)

        // 2. CONFIGURAR SPINNER (SELECTOR)
        configurarSelectorDestino()

        // 3. INICIAR GPS
        checkPermissionsAndStart()
    }

    private fun configurarSelectorDestino() {
        // Extraemos solo los nombres para el Spinner
        val nombresSalas = listaSalas.map { it.nombre }
        val adapter = ArrayAdapter(this, R.layout.item_spinner_avion, nombresSalas)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDestinos.adapter = adapter

        spinnerDestinos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                salaDestino = listaSalas[position]
                txtInstruccion.text = "Calculando..."

                // Ocultamos la foto si cambian de destino
                val cardMultimedia = findViewById<androidx.cardview.widget.CardView>(R.id.cardMultimedia)
                cardMultimedia.visibility = View.GONE
                cardMultimedia.visibility = View.GONE

                obtenerUbicacion()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        obtenerUbicacion()
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // En app real usar requestLocationUpdates, aquí lastLocation para ejemplo
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    actualizarNavegacion(location)
                } else {
                    txtInstruccion.text = "Sin señal GPS"
                }
            }
        }
    }

    private fun actualizarNavegacion(ubicacionActual: Location) {

        // A) LÓGICA DE DESTINO (¿Hacia dónde voy?)
        if (salaDestino != null) {
            val locDestino = Location("Destino")
            locDestino.latitude = salaDestino!!.latitud
            locDestino.longitude = salaDestino!!.longitud

            val distanciaMetros = ubicacionActual.distanceTo(locDestino)
            txtDistanciaRestante.text = "${"%.1f".format(distanciaMetros)} m"

            if (distanciaMetros < 10.0) {
                txtInstruccion.text = "¡Has llegado!"
                // Opcional: Cambiar color a dorado al llegar
                txtInstruccion.setTextColor(resources.getColor(R.color.accent_gold))
            } else {
                // AQUÍ ESTÁ EL CAMBIO: Usamos la pista humana
                txtInstruccion.text = salaDestino!!.pista
                // Aseguramos que sea blanco (ya que el XML lo tiene, pero por si acaso)
                txtInstruccion.setTextColor(resources.getColor(R.color.white))
            }
        }

        // B) LÓGICA DE SALA ACTUAL (¿Dónde estoy ahora mismo?)
        verificarSalaActual(ubicacionActual)
    }

    // Convierte grados (-180 a 180) en texto (Norte, Sur, Este...)
    private fun obtenerDireccionTexto(grados: Float): String {
        // Normalizar a 0-360
        val deg = if (grados < 0) grados + 360 else grados

        return when {
            deg >= 337.5 || deg < 22.5 -> "NORTE ⬆️"
            deg >= 22.5 && deg < 67.5 -> "NORESTE ↗️"
            deg >= 67.5 && deg < 112.5 -> "ESTE ➡️"
            deg >= 112.5 && deg < 157.5 -> "SURESTE ↘️"
            deg >= 157.5 && deg < 202.5 -> "SUR ⬇️"
            deg >= 202.5 && deg < 247.5 -> "SUROESTE ↙️"
            deg >= 247.5 && deg < 292.5 -> "OESTE ⬅️"
            deg >= 292.5 && deg < 337.5 -> "NOROESTE ↖️"
            else -> "frente"
        }
    }

    private fun verificarSalaActual(location: Location) {
        // Recorre todas las salas para ver si estamos dentro de alguna (independiente del destino)
        for (sala in listaSalas) {
            val locSala = Location("").apply {
                latitude = sala.latitud
                longitude = sala.longitud
            }
            // Si estamos a menos de 15 metros, consideramos que estamos DENTRO
            if (location.distanceTo(locSala) < 15.0) {
                if (salaActual != sala) { // Solo si cambiamos de sala
                    salaActual = sala
                    activarExperienciaSala(sala)
                }
                return
            }
        }
        // Si no estamos cerca de ninguna
        txtEstado.text = "Estás en tránsito (Pasillos)"
    }

    private fun activarExperienciaSala(sala: SalaMuseo) {
        val cardMultimedia = findViewById<androidx.cardview.widget.CardView>(R.id.cardMultimedia)

        txtInstruccion.text = "¡LLEGADA!"
        txtInstruccion.setTextColor(resources.getColor(R.color.accent_gold))

        txtEstado.text = sala.nombre
        cardMultimedia.visibility = View.VISIBLE // Hacemos visible toda la tarjeta

        txtEstado.text = "Estás en: ${sala.nombre}"
        Toast.makeText(this, "Bienvenido a ${sala.nombre}", Toast.LENGTH_SHORT).show()

        // Mostrar imagen y audio (Descomentar cuando tengas archivos)
        // imgLugar.visibility = View.VISIBLE
        // imgLugar.setImageResource(sala.imgRes)

        // if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
        // mediaPlayer = MediaPlayer.create(this, sala.audioRes)
        // mediaPlayer?.start()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // TUS 5 SALAS (Pon coordenadas ligeramente distintas para probar la navegación)
    private fun generarSalasDePrueba(miUbicacion: Location?) {

        // Coordenadas ETSIIT Granada (Calle Periodista Daniel Saucedo Aranda)
        // Centro aproximado: 37.197150, -3.624500

        listaSalas = listOf(
            SalaMuseo(
                "Lobby Principal",
                37.197150, -3.624500, // Entrada Principal / Conserjería
                R.raw.guia_audio,
                R.mipmap.ic_launcher,
                "Ve a la entrada principal, frente a la conserjería."
            ),
            SalaMuseo(
                "Planeadores",
                37.197350, -3.624700, // Hacia la Cafetería
                R.raw.guia_audio,
                R.mipmap.ic_launcher,
                "Baja las escaleras y dirígete hacia la entrada de la cafetería."
            ),
            SalaMuseo(
                "Aviones Guerra",
                37.196900, -3.624500, // Hacia la Biblioteca / Sur
                R.raw.guia_audio,
                R.mipmap.ic_launcher,
                "Camina por el pasillo principal hacia la zona de la Biblioteca."
            ),
            SalaMuseo(
                "Aviones Comerciales",
                37.197200, -3.624100, // Aularios / Este
                R.raw.guia_audio,
                R.mipmap.ic_launcher,
                "Sube a la primera planta, hacia la zona de los despachos y aulas."
            ),
            SalaMuseo(
                "Helicópteros",
                37.197100, -3.625000, // Parking / Exterior Oeste
                R.raw.guia_audio,
                R.mipmap.ic_launcher,
                "Sal al exterior por la puerta lateral, hacia el aparcamiento."
            )
        )

        // Refrescamos el Spinner
        configurarSelectorDestino()
    }
}