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
    private val SWIPE_THRESHOLD = 300

    // --- VARIABLES NAVEGACIÓN MENÚ ---
    private var selectedIndex = 0
    private var isDrawerOpen = false
    private val menuIds = listOf(R.id.nav_que_ver, R.id.nav_mapa, R.id.nav_arte)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 1. CONFIGURAR MENÚ LATERAL (Sin Toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Configurar el nuevo botón de menú (Hamburguesa)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenuHamburguesa)
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Listener para saber si el menú está abierto (para los gestos)
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) { isDrawerOpen = true; updateMenuSelection() }
            override fun onDrawerClosed(drawerView: View) { isDrawerOpen = false }
        })

        // 2. CONFIGURAR AUDIO
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        btnSonido = findViewById(R.id.btnSonidoPanel)
        actualizarIconoSonido()

        btnSonido.setOnClickListener { toggleSonido() }

        // 3. CONFIGURAR NFC (Botón Central)
        val btnNFC = findViewById<CardView>(R.id.btnNFCPanel)
        btnNFC.setOnClickListener { lanzarEscanerNFC() }

        // 4. CONFIGURAR IDIOMA
        val btnIdioma = findViewById<ImageButton>(R.id.btnIdiomaPanel)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
        }

        // 5. CONFIGURAR ASISTENTE (Robot)
        val cardChatbot = findViewById<CardView>(R.id.cardChatbot)
        cardChatbot.setOnClickListener {
            Toast.makeText(this, "Hola, soy tu asistente virtual", Toast.LENGTH_SHORT).show()
        }

        // 6. INICIALIZAR SENSORES
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // --- LÓGICA GESTO MULTITÁCTIL (2 DEDOS) ---
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

    private fun lanzarEscanerNFC() {
        Toast.makeText(this, "Escanear NFC...", Toast.LENGTH_SHORT).show()
    }

    // --- LÓGICA SONIDO ---
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

    // --- LÓGICA SENSORES (MENÚ) ---
    override fun onResume() { super.onResume(); acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) } }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isDrawerOpen) return
        if ((System.currentTimeMillis() - lastUpdate) > 400) {
            val x = event.values[0]; val z = event.values[2]

            if (x < -3.5) { selectedIndex = (selectedIndex + 1) % menuIds.size; updateMenuSelection(); lastUpdate = System.currentTimeMillis() }
            else if (x > 3.5) { selectedIndex = if (selectedIndex - 1 < 0) menuIds.size - 1 else selectedIndex - 1; updateMenuSelection(); lastUpdate = System.currentTimeMillis() }

            if (z > 8.0 && abs(x) < 2) {
                onNavigationItemSelected(navigationView.menu.findItem(menuIds[selectedIndex]))
                lastUpdate = System.currentTimeMillis() + 1000
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateMenuSelection() { navigationView.setCheckedItem(menuIds[selectedIndex]) }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_que_ver -> startActivity(Intent(this, CatalogoActivity::class.java))
            R.id.nav_mapa -> Toast.makeText(this, "Mapa", Toast.LENGTH_SHORT).show()
            R.id.nav_arte -> Toast.makeText(this, "Arte Digital", Toast.LENGTH_SHORT).show()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}