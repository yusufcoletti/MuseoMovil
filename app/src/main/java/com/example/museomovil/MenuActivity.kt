package com.example.museomovil

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlin.math.abs
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.app.Dialog
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
// Importante para la vibración
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.widget.TextView

class MenuActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    // --- VARIABLES UI ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnSonido: ImageButton

    // --- VARIABLES SENSORES ---
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var lastUpdate: Long = 0
    private val UMBRAL_TIEMPO = 500 // Tiempo de espera entre movimientos para que no vaya muy rápido

    // --- VARIABLES AUDIO ---
    private lateinit var audioManager: AudioManager
    private var isMuted = false
    private var previousVolume = 0

    // --- VARIABLES GESTO MULTITÁCTIL ---
    private var startY1 = 0f
    private var startY2 = 0f
    private val SWIPE_THRESHOLD = 300

    // --- VARIABLES NAVEGACIÓN MENÚ ---
    private var selectedIndex = 0
    private var isDrawerOpen = false
    private val menuIds = listOf(R.id.nav_que_ver, R.id.nav_mapa, R.id.nav_arte)

    // --- VARIABLES NFC ---
    private var nfcAdapter: NfcAdapter? = null
    private var nfcDialog: Dialog? = null

    // --- BARRA DE PROGRESO ---
    private lateinit var txtProgresoCuenta: TextView
    private lateinit var imgAvionProgreso: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 1. CONFIGURAR MENÚ LATERAL
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Marcar visualmente la primera opción al iniciar
        navigationView.setCheckedItem(menuIds[0])

        // Configurar el botón de menú (Hamburguesa)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenuHamburguesa)
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Listener para saber si el menú está abierto
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                isDrawerOpen = true
                // Sincronizar selección visual al abrir
                navigationView.setCheckedItem(menuIds[selectedIndex])
            }
            override fun onDrawerClosed(drawerView: View) {
                isDrawerOpen = false
            }
        })

        // 2. CONFIGURAR AUDIO
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        btnSonido = findViewById(R.id.btnSonidoPanel)
        actualizarIconoSonido()
        btnSonido.setOnClickListener { toggleSonido() }

        // 3. CONFIGURAR NFC (Botón Central - FloatingActionButton)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val btnNFC = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnNFCPanel)
        btnNFC.setOnClickListener { lanzarEscanerNFC() }

        // 4. CONFIGURAR IDIOMA
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdiomaPanel)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }

        // 5. CONFIGURAR ASISTENTE (Banner Hero)
        val cardHero = findViewById<androidx.cardview.widget.CardView>(R.id.cardHero)

        // Opción A: Click normal (Tocar)
        cardHero.setOnClickListener {
            abrirAsistente()
        }

        // Opción B: Mantener pulsado (Long Click)
        cardHero.setOnLongClickListener {
            vibrar() // Pequeña vibración para confirmar
            abrirAsistente()
            true // 'true' significa que hemos consumido el evento
        }

        // 6. INICIALIZAR SENSORES
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 7. CONFIGURAR BOTÓN DE AYUDA (?)
        val btnAyuda = findViewById<ImageButton>(R.id.btnAyuda)
        btnAyuda.setOnClickListener {
            mostrarDialogoAyuda()
        }

        //
        txtProgresoCuenta = findViewById(R.id.txtProgresoCuenta)
        imgAvionProgreso = findViewById(R.id.imgAvionProgreso)
    }

    // --- CICLO DE VIDA ---
    override fun onResume() {
        super.onResume()
        acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

        // ACTUALIZAR BARRA DE PROGRESO
        actualizarBarraProgreso()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        detenerEscaneoNFC()
    }

    // --- LÓGICA DE SENSORES (CONTROL DEL MENÚ) ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val curTime = System.currentTimeMillis()

        // Control de velocidad (Cooldown) para no saltar opciones muy rápido
        if ((curTime - lastUpdate) > UMBRAL_TIEMPO) {
            val x = event.values[0] // Eje X: Izquierda (+) / Derecha (-)
            val y = event.values[1] // Eje Y: Inclinación Arriba/Abajo

            if (isDrawerOpen) {
                // --- MODO MENÚ ABIERTO: NAVEGACIÓN ---

                // 1. ARRIBA / ABAJO (Eje Y)
                if (y > 6.5) {
                    // Inclinar hacia TI -> BAJAR OPCIÓN
                    moveMenuSelection(1)
                    lastUpdate = curTime
                } else if (y < 3.0) {
                    // Inclinar hacia ADELANTE -> SUBIR OPCIÓN
                    moveMenuSelection(-1)
                    lastUpdate = curTime
                }

                // 2. DERECHA (Seleccionar) -> Eje X < -4
                if (x < -4.0) {
                    vibrar()
                    // Ejecutar la opción seleccionada
                    val item = navigationView.menu.findItem(menuIds[selectedIndex])
                    onNavigationItemSelected(item)
                    lastUpdate = curTime + 500 // Pausa extra tras seleccionar
                }

                // 3. IZQUIERDA (Cerrar menú) -> Eje X > 4
                if (x > 4.0) {
                    vibrar()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    lastUpdate = curTime
                }

            } else {
                // --- MODO MENÚ CERRADO: ABRIR ---

                // Inclinar a la IZQUIERDA fuerte -> ABRIR MENÚ
                if (x > 5.0) {
                    vibrar()
                    drawerLayout.openDrawer(GravityCompat.START)
                    lastUpdate = curTime + 500 // Pausa para que no empiece a navegar solo
                }
            }
        }
    }

    private fun moveMenuSelection(direction: Int) {
        val nuevoIndex = selectedIndex + direction

        // Comprobar límites para no salirnos de la lista
        if (nuevoIndex in 0 until menuIds.size) {
            selectedIndex = nuevoIndex
            // Feedback Visual: Iluminamos la opción
            navigationView.setCheckedItem(menuIds[selectedIndex])
            // Feedback Táctil
            vibrar()
        }
    }

    // Método auxiliar para vibrar
    private fun vibrar() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(50)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- NAVEGACIÓN ---
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        // Pequeño delay para que se vea la animación de cierre antes de cambiar
        drawerLayout.postDelayed({
            when (item.itemId) {
                R.id.nav_que_ver -> irACatalogo("")
                R.id.nav_mapa -> {
                    val intent = Intent(this, TourActivity::class.java)
                    startActivity(intent)}
                R.id.nav_arte -> Toast.makeText(this, "Arte Digital...", Toast.LENGTH_SHORT).show()
            }
        }, 250)
        return true
    }

    // --- GESTO MULTITÁCTIL (2 DEDOS) ---
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.dispatchTouchEvent(event)
        if (event.pointerCount == 2) {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    startY1 = event.getY(0); startY2 = event.getY(1)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy1 = event.getY(0) - startY1
                    val dy2 = event.getY(1) - startY2
                    if (dy1 > SWIPE_THRESHOLD && dy2 > SWIPE_THRESHOLD) {
                        lanzarEscanerNFC()
                        startY1 = event.getY(0) + 1000; startY2 = event.getY(1) + 1000
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // --- LÓGICA NFC ---
    private fun lanzarEscanerNFC() {
        if (nfcAdapter != null && !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, activa el NFC", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            return
        }
        nfcDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = layoutInflater.inflate(R.layout.nfc_dialog, null)
        nfcDialog?.setContentView(view)

        val imagenNfc = view.findViewById<ImageView>(R.id.imgNfcIcon)
        val pulso = AlphaAnimation(0.4f, 1.0f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        imagenNfc.startAnimation(pulso)

        view.findViewById<Button>(R.id.btnCancelarNFC).setOnClickListener {
            detenerEscaneoNFC()
            nfcDialog?.dismiss()
        }
        nfcDialog?.show()
        activarModoLector()
    }

    private fun activarModoLector() {
        val options = Bundle()
        // IMPORTANTE: Usamos estos flags para permitir leer cualquier etiqueta y asegurar que Android chequee NDEF
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V

        nfcAdapter?.enableReaderMode(this, { tag ->
            val idLeido = leerIdDesdeTag(tag)

            runOnUiThread {
                if (idLeido != null && idLeido.isNotEmpty()) {
                    // Éxito: Cerramos todo y navegamos
                    nfcDialog?.dismiss()
                    detenerEscaneoNFC()
                    Toast.makeText(this, "Avión detectado: $idLeido", Toast.LENGTH_LONG).show()
                    irACatalogo(idLeido)
                } else {
                    // Fallo de lectura (etiqueta vacía o error)
                    Toast.makeText(this, "Error al leer. Asegúrate de que la etiqueta tiene texto grabado.", Toast.LENGTH_SHORT).show()
                }
            }
        }, flags, options)
    }

    private fun leerIdDesdeTag(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null

        return try {
            ndef.connect()
            val record = ndef.ndefMessage.records[0]
            val payload = record.payload

            // Lógica para detectar encoding y longitud del idioma
            val statusByte = payload[0].toInt()
            val languageCodeLength = statusByte and 0x3F
            val textEncoding = if ((statusByte and 128) == 0) "UTF-8" else "UTF-16"

            // Extraer texto saltando la cabecera
            val texto = String(
                payload,
                1 + languageCodeLength,
                payload.size - 1 - languageCodeLength,
                java.nio.charset.Charset.forName(textEncoding)
            )

            // IMPORTANTE: .trim() elimina espacios fantasma que meten los iPhone
            return texto.trim()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { ndef.close() } catch (e: Exception) {}
        }
    }

    private fun detenerEscaneoNFC() {
        nfcAdapter?.disableReaderMode(this)
    }

    private fun irACatalogo(idAvion: String) {
        val intent = Intent(this, CatalogoActivity::class.java)
        if (idAvion.isNotEmpty()) {
            val dataManager = DataManager(this)
            dataManager.desbloquearAvion(idAvion)
            intent.putExtra("ID_RECIEN_ESCANEDO", idAvion)
        }
        startActivity(intent)
    }

    // --- AUDIO ---
    private fun actualizarIconoSonido() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        btnSonido.setImageResource(if (currentVolume == 0) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
        isMuted = currentVolume == 0
    }

    private fun toggleSonido() {
        if (isMuted) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = if (previousVolume == 0) maxVol / 2 else previousVolume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            Toast.makeText(this, "Sonido Activado", Toast.LENGTH_SHORT).show()
        } else {
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            Toast.makeText(this, "Silenciado", Toast.LENGTH_SHORT).show()
        }
        actualizarIconoSonido()
    }

    // --- CONTROL POR BOTONES FÍSICOS (HARDWARE) ---
    // --- CONTROL DE HARDWARE (BOTONES FÍSICOS) ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            // Caso 1: Botón SUBIR VOLUMEN -> Abre el Asistente
            KeyEvent.KEYCODE_VOLUME_UP -> {
                vibrar()
                val intent = Intent(this, AsistenteActivity::class.java)
                startActivity(intent)
                return true // 'true' evita que suba el volumen real
            }

            // Caso 2: Botón BAJAR VOLUMEN -> Lanza el Escáner NFC
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                vibrar()
                lanzarEscanerNFC() // Llamamos a tu función existente
                return true // 'true' evita que baje el volumen real
            }
        }

        // Si es otro botón (atrás, encendido...), dejamos que Android lo maneje
        return super.onKeyDown(keyCode, event)
    }

    // --- MÉTODO AUXILIAR PARA ABRIR ASISTENTE ---
    // Creamos esto para no repetir código en el click, long click y botón físico
    private fun abrirAsistente() {
        val intent = Intent(this, AsistenteActivity::class.java)
        startActivity(intent)
        // Opcional: Transición suave
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // --- DIÁLOGO DE AYUDA ---
    private fun mostrarDialogoAyuda() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_help)

        // Hacemos el fondo del diálogo transparente para que se vean las esquinas redondeadas del CardView
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Configurar botón cerrar
        val btnCerrar = dialog.findViewById<Button>(R.id.btnCerrarAyuda)
        btnCerrar.setOnClickListener {
            vibrar() // Pequeño feedback al cerrar
            dialog.dismiss()
        }

        dialog.show()
    }

    // Función auxiliar para calcular matemáticas de la barra
    private fun actualizarBarraProgreso() {
        // Verificamos que las vistas existen
        if (!::txtProgresoCuenta.isInitialized || !::imgAvionProgreso.isInitialized) return

        val dataManager = DataManager(this)
        val desbloqueados = dataManager.getAvionesDesbloqueadosCount()
        val total = 15

        // 1. Calcular Porcentaje (0.0 a 1.0)
        val porcentaje = if (total > 0) desbloqueados.toFloat() / total.toFloat() else 0f

        // 2. TEXTO CUENTA: "4 / 15"
        txtProgresoCuenta.text = "$desbloqueados / $total"

        // 3. MOVER EL AVIÓN (Horizontal Bias)
        val params = imgAvionProgreso.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params?.let {
            it.horizontalBias = porcentaje.coerceIn(0.0f, 1.0f)
            imgAvionProgreso.layoutParams = it
        }

        // 4. LÓGICA DE RANGOS
        // Aspirante (<4), Cadete (4-6), Segundo Oficial (7-11), Primer Oficial (12-14), Capitán (15)
        val rangoTitulo = when {
            desbloqueados >= 15 -> "Capitán"
            desbloqueados >= 12 -> "Primer Oficial"
            desbloqueados >= 7  -> "Segundo Oficial"
            desbloqueados >= 4  -> "Cadete"
            else -> "Aspirante"
        }

        // --- ACTUALIZAR UI EN TARJETA PRINCIPAL ---
        // Buscamos el TextView del nivel (asegúrate de haberlo puesto en el XML paso 2)
        val txtNivelMain = findViewById<TextView>(R.id.txtNivelPiloto)
        txtNivelMain?.text = "Nivel: $rangoTitulo"

        // Cambiamos el color del texto si es Capitán para darle epicidad
        if (desbloqueados == 15) {
            txtNivelMain?.setTextColor(getColor(R.color.accent_gold)) // O Color.YELLOW
        }

        // --- ACTUALIZAR UI EN MENÚ LATERAL (HEADER) ---
        // Accedemos a la cabecera del menú lateral
        val headerView = navigationView.getHeaderView(0)
        val txtRangoHeader = headerView.findViewById<TextView>(R.id.txtRangoHeader)
        txtRangoHeader?.text = "Rango: $rangoTitulo"
    }
}