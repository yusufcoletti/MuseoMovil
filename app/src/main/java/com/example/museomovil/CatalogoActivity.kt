package com.example.museomovil

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

// Implementamos SensorEventListener para escuchar al acelerómetro
class CatalogoActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var seekBar: SeekBar
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null

    // Variables para controlar la inclinación (Debounce)
    private var listoParaGirar = true // Bandera para no pasar páginas a lo loco
    private val UMBRAL_INCLINACION = 2.6 // Aprox 15 grados (9.8 m/s * sin(15))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo)

        // ---------------------------------------------------------------
        // 1. CONFIGURACIÓN DE SENSORES
        // ---------------------------------------------------------------
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (acelerometro == null) {
            Toast.makeText(this, "Tu móvil no tiene acelerómetro", Toast.LENGTH_SHORT).show()
        }

        // ---------------------------------------------------------------
// 2. DATOS DEL CATÁLOGO
// ---------------------------------------------------------------
        val catalogoCompleto = listOf(
            // --- SALA 1: PLANEADORES Y PIONEROS ---
            Avion(
                nombre = "Dumont Demoiselle",
                descripcion = "SALA 1. El precursor de la aviación ultraligera. Un diseño elegante y pequeño creado por Santos-Dumont en 1907.",
                descripcionDetallada = "Una obra maestra de la ingeniería ligera creada por Santos-Dumont. Construido con bambú y seda, fue el primer avión \"deportivo\" del mundo y precursor de la aviación ultraligera moderna.",
                imagenResId = R.drawable.planeador_demoiselle,
                isDesbloqueado = true // ¡Este te lo regalo desbloqueado para probar!
            ),
            Avion(
                nombre = "Globo Aerostático",
                descripcion = "SALA 1. El primer paso del hombre hacia el cielo. Funciona por el principio de Arquímedes con aire caliente.",
                descripcionDetallada = "El primer vehículo en llevar humanos al cielo. Utiliza el principio de Arquímedes, donde el aire caliente en el interior es menos denso que el aire frío exterior, generando elevación.",
                imagenResId = R.drawable.globo_aerostatico,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Tornillo Aéreo (Da Vinci)",
                descripcion = "SALA 1. El 'bisabuelo' del helicóptero. Un diseño visionario de Leonardo da Vinci que imaginaba la elevación vertical.",
                descripcionDetallada = "Un diseño visionario del siglo XV considerado el antepasado del helicóptero. Da Vinci imaginó una estructura helicoidal de lino almidonado que, al girar rápido, se \"atornillaría\" en el aire.",
                imagenResId = R.drawable.tornillo_davinci,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Planeador Da Vinci",
                descripcion = "SALA 1. Ornitóptero basado en la anatomía de los pájaros y murciélagos. Ingeniería renacentista pura.",
                descripcionDetallada = "Basado en la anatomía de los pájaros y los murciélagos. Este ornitóptero cuenta con alas articuladas que el piloto debía mover usando su propia fuerza muscular mediante poleas.",
                imagenResId = R.drawable.planeador_davinci,
                isDesbloqueado = false
            ),

            // --- SALA 2: AVIONES DE GUERRA ---
            Avion(
                nombre = "Messerschmitt Bf 109",
                descripcion = "SALA 2. La columna vertebral de la Luftwaffe alemana. Un caza rápido y versátil de la Segunda Guerra Mundial.",
                descripcionDetallada = "El caza más famoso de la Luftwaffe alemana durante la Segunda Guerra Mundial. Conocido por su inyección de combustible y su capacidad para realizar maniobras de picado extremas sin que el motor se detuviera.",
                imagenResId = R.drawable.guerra_messerschmitt,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Boeing B-17",
                descripcion = "SALA 2. La 'Fortaleza Volante'. Bombardero pesado famoso por su resistencia y capacidad defensiva.",
                descripcionDetallada = "Un bombardero pesado cuatrimotor legendario por su resistencia. Estaba erizado de ametralladoras para defenderse desde cualquier ángulo, de ahí su apodo de \"Fortaleza Volante\".",
                imagenResId = R.drawable.guerra_b17,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Sopwith Camel",
                descripcion = "SALA 2. El caza británico más exitoso de la Primera Guerra Mundial, famoso por su maniobrabilidad.",
                descripcionDetallada = "El caza británico más icónico de la Primera Guerra Mundial. Corto, ágil y muy difícil de pilotar debido al torque de su motor rotativo, pero letal en manos expertas.",
                imagenResId = R.drawable.guerra_sopwith,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Red Baron (Fokker Dr.I)",
                descripcion = "SALA 2. El icónico triplano rojo pilotado por Manfred von Richthofen, el as de ases alemán.",
                descripcionDetallada = "El famoso triplano asociado al as de la aviación Manfred von Richthofen. Sus tres alas le daban una capacidad de ascenso y maniobra superior a la de sus rivales biplanos.",
                imagenResId = R.drawable.guerra_redbaron,
                isDesbloqueado = false
            ),

            // --- SALA 3: AVIONES COMERCIALES ---
            Avion(
                nombre = "Douglas DC-3",
                descripcion = "SALA 3. El avión que revolucionó el transporte aéreo en los años 30 y 40. Fiabilidad legendaria.",
                descripcionDetallada = "El avión que hizo rentable el transporte aéreo de pasajeros en los años 30. Robusto, fiable y capaz de operar en pistas cortas de tierra o hierba.",
                imagenResId = R.drawable.comercial_dc3,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Boeing 747",
                descripcion = "SALA 3. 'La Reina de los Cielos'. El primer avión de fuselaje ancho que democratizó los viajes internacionales.",
                descripcionDetallada = "Conocido como la \"Reina de los Cielos\". Fue el primer avión de fuselaje ancho del mundo, reconocible instantáneamente por su joroba superior que alberga la cabina y una sala VIP.",
                imagenResId = R.drawable.guerra_747,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Airbus A320",
                descripcion = "SALA 3. Pionero en tecnología fly-by-wire digital. El caballo de batalla de las aerolíneas modernas.",
                descripcionDetallada = "El caballo de batalla de las aerolíneas modernas. Fue pionero en introducir la tecnología \"Fly-by-wire\" digital en aviones civiles, reemplazando los cables manuales por señales electrónicas.",
                imagenResId = R.drawable.comercial_a320,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Boeing 787 Dreamliner",
                descripcion = "SALA 3. Eficiencia y tecnología. Construido con materiales compuestos para un vuelo más suave y ecológico.",
                descripcionDetallada = "Un avión revolucionario construido principalmente con materiales compuestos (fibra de carbono) en lugar de aluminio, lo que lo hace más ligero, eficiente y cómodo por la mayor presión en cabina.",
                imagenResId = R.drawable.comercial_787,
                isDesbloqueado = false
            ),

            // --- SALA 4: HELICÓPTEROS ---
            Avion(
                nombre = "AH-64 Apache",
                descripcion = "SALA 4. Helicóptero de ataque avanzado. Diseñado para sobrevivir en combate y atacar desde la distancia.",
                descripcionDetallada = "El helicóptero de ataque más avanzado del mundo. Diseñado para sobrevivir en primera línea, cuenta con blindaje pesado y sistemas de visión nocturna integrados para cazar tanques.",
                imagenResId = R.drawable.heli_apache,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "Bell 206 JetRanger",
                descripcion = "SALA 4. Uno de los helicópteros más populares del mundo, usado para noticias, policía y transporte VIP.",
                descripcionDetallada = "El helicóptero civil más reconocible y versátil. Su diseño de dos palas es simple y seguro, utilizado por policías, noticieros y transporte ejecutivo en todo el mundo.",
                imagenResId = R.drawable.heli_bell206,
                isDesbloqueado = false
            ),
            Avion(
                nombre = "UH-1Y Venom",
                descripcion = "SALA 4. La evolución moderna del famoso 'Huey'. Un helicóptero utilitario versátil y potente.",
                descripcionDetallada = "La versión moderna y letal del clásico \"Huey\" de Vietnam, utilizada por los Marines. Ha sido mejorado con cuatro palas, nuevos motores y aviónica digital de última generación.",
                imagenResId = R.drawable.heli_uh1y,
                isDesbloqueado = false
            )
        )

        // ---------------------------------------------------------------
        // 3. CONFIGURACIÓN VISUAL (ViewPager + SeekBar)
        // ---------------------------------------------------------------
        viewPager = findViewById(R.id.viewPagerCatalogo)
        seekBar = findViewById(R.id.seekBarCatalogo)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        val adapter = AvionAdapter(catalogoCompleto, this)
        viewPager.adapter = adapter

        // Animación elegante
        viewPager.setPageTransformer { page, position ->
            val absPos = Math.abs(position)
            page.alpha = 1f - (absPos * 0.3f)
            page.scaleY = 0.85f + (0.15f * (1f - absPos))
        }

        // --- SINCRONIZACIÓN BARRA -> CARRUSEL ---
        seekBar.max = catalogoCompleto.size - 1 // La barra tiene tantos pasos como aviones

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // Solo si lo mueve el dedo, no el código
                    viewPager.setCurrentItem(progress, true)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- SINCRONIZACIÓN CARRUSEL -> BARRA ---
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                seekBar.progress = position // La barra se mueve sola al deslizar
            }
        })

        // Botón Volver
        btnVolver.setOnClickListener { finish() }
    }

    // ---------------------------------------------------------------
    // 4. LÓGICA DEL SENSOR (Inclinación)
    // ---------------------------------------------------------------
    override fun onResume() {
        super.onResume()
        // Encendemos el sensor al entrar (ahorra batería)
        acelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Apagamos el sensor al salir o bloquear pantalla
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // El acelerómetro devuelve [x, y, z] en m/s²
        // Eje X: Positivo si inclinas a Izquierda, Negativo si inclinas a Derecha
        val x = event.values[0]

        if (listoParaGirar) {
            if (x < -UMBRAL_INCLINACION) {
                // --- INCLINADO A LA DERECHA (> 15º aprox) ---
                val siguiente = viewPager.currentItem + 1
                if (siguiente < viewPager.adapter!!.itemCount) {
                    viewPager.setCurrentItem(siguiente, true)
                    listoParaGirar = false // Bloqueamos hasta que se enderece
                }
            } else if (x > UMBRAL_INCLINACION) {
                // --- INCLINADO A LA IZQUIERDA (> 15º aprox) ---
                val anterior = viewPager.currentItem - 1
                if (anterior >= 0) {
                    viewPager.setCurrentItem(anterior, true)
                    listoParaGirar = false // Bloqueamos hasta que se enderece
                }
            }
        } else {
            // Si ya hemos girado, esperamos a que el móvil vuelva a estar plano (cerca de 0)
            // Ponemos un margen de 1.0 para que no sea tan estricto
            if (Math.abs(x) < 1.0) {
                listoParaGirar = true // ¡Listo para el siguiente giro!
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario para esta práctica
    }
}