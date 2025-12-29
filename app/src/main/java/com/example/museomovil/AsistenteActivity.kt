package com.example.museomovil

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AsistenteActivity : AppCompatActivity(), SensorEventListener {

    // DATOS DE LAS SALAS
    private val salasInfo = mapOf(
        "Sala 1: Planeadores" to "El arte de volar sin motor. Descubre cómo las corrientes térmicas y la aerodinámica pura permitieron a los primeros pioneros, como Lilienthal, dominar los cielos en absoluto silencio.",
        "Sala 2: Aviación Militar" to "Potencia y estrategia en el aire. Desde los cazas legendarios de la Segunda Guerra Mundial hasta la tecnología furtiva actual. Conoce las máquinas diseñadas para la defensa y el combate.",
        "Sala 3: Aviación Comercial" to "Conectando el mundo. Explora la evolución del transporte de pasajeros, desde los primeros vuelos de lujo de la Pan Am hasta los gigantescos jets que cruzan océanos diariamente.",
        "Sala 4: Helicópteros" to "El dominio del vuelo vertical. Descubre la compleja ingeniería de los rotores y cómo estas aeronaves son vitales para rescates, transporte urbano y misiones imposibles para un avión."
    )
    private val listaOpciones = salasInfo.keys.toList()

    // UI
    private lateinit var chatContainer: LinearLayout
    private lateinit var opcionesContainer: LinearLayout
    private lateinit var scrollChat: ScrollView

    // VARIABLES DE CONTROL
    private var opcionSeleccionadaIndex = 0
    private var isSelecting = true // Si false, estamos leyendo info y esperando "volver"

    // SENSORES
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var lastUpdate: Long = 0
    private val UMBRAL_TIEMPO = 400 // Milisegundos entre movimientos (para que no vaya muy rápido)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistente)

        chatContainer = findViewById(R.id.chatContainer)
        opcionesContainer = findViewById(R.id.opcionesContainer)
        scrollChat = findViewById(R.id.scrollChat)

        findViewById<View>(R.id.btnBackAsistente).setOnClickListener { finish() }

        // Inicializar Sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // MENSAJE DE BIENVENIDA DEL BOT
        agregarMensajeBot("¡Hola piloto! Soy tu asistente de vuelo.")
        agregarMensajeBot("Inclina el dispositivo adelante/atrás para moverte, a la derecha para seleccionar y a la izquierda para volver.")

        // MOSTRAR OPCIONES INICIALES
        renderizarOpciones()
    }

    // --- LÓGICA DE SENSORES ---
    override fun onResume() {
        super.onResume()
        acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val curTime = System.currentTimeMillis()
        // Solo actuamos si ha pasado el tiempo de "cooldown"
        if ((curTime - lastUpdate) > UMBRAL_TIEMPO) {
            val x = event.values[0] // Eje X: Izquierda/Derecha
            val y = event.values[1] // Eje Y: Arriba/Abajo (Inclinación)

            // 1. DETECTAR INCLINACIÓN ARRIBA/ABAJO (Navegar lista)
            // Y > 6: Móvil vertical (hacia ti) -> BAJAR
            // Y < 3: Móvil tumbado (hacia adelante) -> SUBIR
            if (isSelecting) {
                if (y > 6.5) {
                    moverSeleccion(1) // Bajar
                    lastUpdate = curTime
                } else if (y < 3.0) {
                    moverSeleccion(-1) // Subir
                    lastUpdate = curTime
                }
            }

            // 2. DETECTAR GIRO DERECHA (Aceptar) -> X < -3 (depende de rotación, probamos negativo)
            // Si inclinas a la derecha, la gravedad tira del lado derecho, X se vuelve negativa o positiva según dispositivo.
            // Generalmente: Inclinar derecha = X negativo. Inclinar izquierda = X positivo.
            if (x < -4.0) {
                // GESTO DERECHA: SELECCIONAR
                if (isSelecting) {
                    seleccionarOpcion()
                    lastUpdate = curTime + 500 // Un poco más de tiempo para evitar doble click
                }
            }

            // 3. DETECTAR GIRO IZQUIERDA (Atrás) -> X > 4.0
            if (x > 4.0) {
                // GESTO IZQUIERDA: VOLVER / SALIR
                if (!isSelecting) {
                    volverALista()
                    lastUpdate = curTime + 500
                } else {
                    finish() // Si ya estamos en la lista, salimos de la activity
                }
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- LÓGICA DE NAVEGACIÓN ---

    private fun moverSeleccion(direccion: Int) {
        val anterior = opcionSeleccionadaIndex
        opcionSeleccionadaIndex += direccion

        // Controlar límites (no salirnos de la lista)
        if (opcionSeleccionadaIndex < 0) opcionSeleccionadaIndex = 0
        if (opcionSeleccionadaIndex >= listaOpciones.size) opcionSeleccionadaIndex = listaOpciones.size - 1

        if (anterior != opcionSeleccionadaIndex) {
            vibrarPequeño() // Feedback táctil
            renderizarOpciones() // Redibujar para ver el cambio de color
        }
    }

    private fun seleccionarOpcion() {
        vibrarPequeño()
        val salaElegida = listaOpciones[opcionSeleccionadaIndex]

        // 1. Usuario "habla"
        agregarMensajeUsuario("Ver información de $salaElegida")

        // 2. Ocultamos panel opciones
        opcionesContainer.visibility = View.GONE
        isSelecting = false

        // 3. Bot responde
        val info = salasInfo[salaElegida] ?: "Información no disponible."
        // Simulamos un pequeño retraso de "pensando"
        chatContainer.postDelayed({
            agregarMensajeBot(info)
            agregarMensajeBot("Inclina a la IZQUIERDA para volver al menú.")
        }, 600)
    }

    private fun volverALista() {
        vibrarPequeño()
        agregarMensajeUsuario("Volver al menú")

        chatContainer.postDelayed({
            agregarMensajeBot("¿Qué más deseas consultar?")
            opcionesContainer.visibility = View.VISIBLE
            isSelecting = true
            renderizarOpciones() // Asegurar que se ve bien
        }, 500)
    }

    // --- DIBUJADO DE LA UI ---

    private fun renderizarOpciones() {
        opcionesContainer.removeAllViews()

        for ((index, opcion) in listaOpciones.withIndex()) {
            val tv = TextView(this)
            tv.text = opcion
            tv.textSize = 18f
            tv.setPadding(30, 20, 30, 20)

            // ESTILO SI ESTÁ SELECCIONADA
            if (index == opcionSeleccionadaIndex) {
                tv.setTextColor(Color.WHITE)
                tv.setTypeface(null, Typeface.BOLD)
                tv.setBackgroundResource(R.drawable.bg_opcion_seleccionada)
                tv.setBackgroundColor(ContextCompat.getColor(this, R.color.aviation_navy)) // Fondo Azul
                // Añadimos un icono o flecha
                tv.text = ">  $opcion"
            } else {
                tv.setTextColor(Color.DKGRAY)
                tv.setTypeface(null, Typeface.NORMAL)
                tv.setBackgroundColor(Color.TRANSPARENT)
            }

            opcionesContainer.addView(tv)
        }
    }

    private fun agregarMensajeBot(texto: String) {
        val tv = TextView(this)
        tv.text = texto
        tv.setTextColor(Color.BLACK)
        tv.setBackgroundResource(R.drawable.bg_chat_bot) // Fondo gris claro redondeado
        tv.setPadding(30, 20, 30, 20)
        tv.textSize = 16f

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.START // Izquierda
        params.setMargins(0, 10, 80, 10) // Margen derecho grande para que no ocupe todo
        tv.layoutParams = params

        chatContainer.addView(tv)
        scrollAlFondo()
    }

    private fun agregarMensajeUsuario(texto: String) {
        val tv = TextView(this)
        tv.text = texto
        tv.setTextColor(Color.WHITE)
        tv.setBackgroundResource(R.drawable.bg_chat_user) // Fondo azul redondeado
        tv.setPadding(30, 20, 30, 20)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.END // Derecha
        params.setMargins(80, 10, 0, 10)
        tv.layoutParams = params

        chatContainer.addView(tv)
        scrollAlFondo()
    }

    private fun scrollAlFondo() {
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun vibrarPequeño() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(50)
        }
    }
}