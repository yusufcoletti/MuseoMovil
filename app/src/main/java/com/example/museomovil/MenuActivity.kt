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
import android.view.MotionEvent // Importante para el multitáctil
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlin.math.abs

class MenuActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    // --- VARIABLES UI ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnSonido: ImageButton

    // --- VARIABLES SENSORES ---
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var lastUpdate: Long = 0

    // --- VARIABLES AUDIO ---
    private lateinit var audioManager: AudioManager
    private var isMuted = false
    private var previousVolume = 0

    // --- VARIABLES GESTO MULTITÁCTIL ---
    private var startY1 = 0f
    private var startY2 = 0f
    private val SWIPE_THRESHOLD = 300 // Distancia para considerar que has deslizado

    // --- VARIABLES NAVEGACIÓN MENÚ ---
    private var selectedIndex = 0
    private var isDrawerOpen = false
    private val menuIds = listOf(R.id.nav_que_ver, R.id.nav_mapa, R.id.nav_arte)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 1. CONFIGURAR TOOLBAR Y DRAWER
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 2. CONFIGURAR AUDIO (Botón Izquierdo)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        btnSonido = findViewById(R.id.btnSonidoPanel)
        actualizarIconoSonido() // Pone el icono correcto al iniciar

        btnSonido.setOnClickListener {
            toggleSonido()
        }

        // 3. CONFIGURAR NFC (Botón Central Grande)
        val btnNFC = findViewById<CardView>(R.id.btnNFCPanel)
        btnNFC.setOnClickListener {
            lanzarEscanerNFC()
        }

        // 4. CONFIGURAR IDIOMA (Botón Derecho)
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdiomaPanel)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }

        // 5. CONFIGURAR CHATBOT (Tarjeta en el dashboard)
        val cardChatbot = findViewById<CardView>(R.id.cardChatbot)
        cardChatbot.setOnClickListener {
            Toast.makeText(this, "Iniciando Asistente VoiceFlow...", Toast.LENGTH_SHORT).show()
            // Aquí conectarás tu chatbot en el futuro
        }

        // 6. INICIALIZAR SENSORES DE INCLINACIÓN
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Listener para saber si el menú lateral está abierto
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) { isDrawerOpen = true; updateMenuSelection() }
            override fun onDrawerClosed(drawerView: View) { isDrawerOpen = false }
        })
    }

    // --- LÓGICA GESTO MULTITÁCTIL (2 DEDOS ABAJO) ---
    // Usamos dispatchTouchEvent para "espiar" los toques antes de que lleguen a los botones
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.dispatchTouchEvent(event)

        // Solo nos interesa si hay 2 dedos
        if (event.pointerCount == 2) {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Guardamos la posición inicial de los dos dedos
                    startY1 = event.getY(0)
                    startY2 = event.getY(1)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculamos cuánto se han movido
                    val endY1 = event.getY(0)
                    val endY2 = event.getY(1)
                    val dy1 = endY1 - startY1
                    val dy2 = endY2 - startY2

                    // Si ambos han bajado más del umbral...
                    if (dy1 > SWIPE_THRESHOLD && dy2 > SWIPE_THRESHOLD) {
                        lanzarEscanerNFC()
                        // Reseteamos para no lanzar 20 veces seguidas
                        startY1 = endY1 + 1000
                        startY2 = endY2 + 1000
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun lanzarEscanerNFC() {
        Toast.makeText(this, "¡Escaneo NFC Activado por Gesto!", Toast.LENGTH_SHORT).show()
        // Aquí iría el código real del NFC
    }

    // --- LÓGICA DE SONIDO ---
    private fun actualizarIconoSonido() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume == 0) {
            isMuted = true
            btnSonido.setImageResource(R.drawable.ic_volume_off)
        } else {
            isMuted = false
            btnSonido.setImageResource(R.drawable.ic_volume_on)
        }
    }

    private fun toggleSonido() {
        if (isMuted) {
            val targetVolume = if (previousVolume == 0)
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
            else previousVolume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            Toast.makeText(this, "Sonido Activado", Toast.LENGTH_SHORT).show()
        } else {
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            Toast.makeText(this, "Silenciado", Toast.LENGTH_SHORT).show()
        }
        actualizarIconoSonido()
    }

    // --- LÓGICA SENSORES (MENÚ LATERAL) ---
    override fun onResume() {
        super.onResume()
        acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isDrawerOpen) return
        val curTime = System.currentTimeMillis()
        if ((curTime - lastUpdate) > 400) {
            val x = event.values[0]
            val z = event.values[2]

            // Navegación (Inclinación Lateral)
            if (x < -3.5) {
                selectedIndex = (selectedIndex + 1) % menuIds.size
                updateMenuSelection()
                lastUpdate = curTime
            } else if (x > 3.5) {
                selectedIndex = if (selectedIndex - 1 < 0) menuIds.size - 1 else selectedIndex - 1
                updateMenuSelection()
                lastUpdate = curTime
            }
            // Selección (Golpe hacia ti)
            if (z > 8.0 && abs(x) < 2) {
                val item = navigationView.menu.findItem(menuIds[selectedIndex])
                onNavigationItemSelected(item)
                lastUpdate = curTime + 1000
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateMenuSelection() {
        navigationView.setCheckedItem(menuIds[selectedIndex])
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_que_ver -> startActivity(Intent(this, CatalogoActivity::class.java))
            R.id.nav_mapa -> Toast.makeText(this, "Abriendo Mapa...", Toast.LENGTH_SHORT).show()
            R.id.nav_arte -> Toast.makeText(this, "Arte Digital...", Toast.LENGTH_SHORT).show()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}