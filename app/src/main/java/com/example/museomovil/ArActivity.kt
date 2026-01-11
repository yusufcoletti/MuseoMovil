package com.example.museomovil

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.view.MotionEvent
import android.app.AlertDialog
import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Importaciones de SceneView
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.ViewNode
import io.github.sceneview.node.Node
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.div

import com.google.android.filament.RenderableManager
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.collision.Ray
import kotlin.math.atan2
import kotlin.math.sqrt


class ArActivity : AppCompatActivity() {

    lateinit var sceneView: ArSceneView
    // El nodo que controla la POSICIÓN y ROTACIÓN en el mundo (El Padre)
    lateinit var anchorNode: ArModelNode
    // El nodo que tiene el DIBUJO 3D (El Hijo)
    lateinit var visualNode: ModelNode
    // Para rotaciones y translaciones
    lateinit var flightNode: Node
    // HotSpots
    private val misHotspots = mutableMapOf<io.github.sceneview.node.Node, () -> Unit>()
    // Velocidad de rotación
    private val rotationSpeed = 0.15f
    private val translationSpeed = 0.0005f
    private val scaleSpeed = 5.0f

    // VARIABLES PARA EL TOQUE
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isRotationLocked = false
    private var isTranslationLocked = false
    private var lastMultiTouchTime: Long = 0
    private val TOUCH_COOLDOWN_MS = 300 // 0.3 segundos de espera

    // Variables para Pinch-to-Zoom
    private var lastMultiTouchDistance = 0f
    private val minScale = 0.2f  // Tamaño mínimo (20%)
    private val maxScale = 3.0f  // Tamaño máximo (300%)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ar)

        // Configuración de pantalla completa
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sceneView = findViewById(R.id.arSceneView)

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnFijar = findViewById<Button>(R.id.btnAnchor)
        // ESTADO INICIAL: Botón invisible (aún no hay suelo)
        btnFijar.visibility = View.GONE

        btnFijar.setOnClickListener {
            // Verificamos que el nodo del avión exista
            if (::anchorNode.isInitialized) {
                anchorNode.anchor()

                // Ocultar los puntos blancos del suelo para que se vea mejor
                sceneView.planeRenderer.isVisible = false

                // Feedback visual: Ocultamos el botón o cambiamos el texto
                btnFijar.isEnabled = false
                btnFijar.text = "¡FIJADO!"
                // o btnFijar.visibility = View.GONE (si quieres que desaparezca)

                // Permitir rotación
                setupTouchControls()

                Toast.makeText(this, "Avión anclado en posición", Toast.LENGTH_SHORT).show()
            }
        }

        newModelNode("tinker.glb")

        // Comprobar en cada frame si hay suelo
        sceneView.onFrame = { _ ->
            // Verificamos si la sesión de AR está lista
            val session = sceneView.arSession
            if (session != null) {
                // Obtenemos todos los PLANOS que el móvil está rastreando ahora mismo
                val planes = session.getAllTrackables(Plane::class.java)

                // ¿Hay algún plano válido (TRACKING)?
                val haySueloDetectado = planes.any { it.trackingState == TrackingState.TRACKING }

                val tvInstructions = findViewById<TextView>(R.id.tvInstructions)

                // LOGICA DE VISIBILIDAD:
                // si no esta anclado:
                //   si hay suelo: muestra el boton y oculta el texto
                //   si no hay suelo: oculta el boton y muestra el texto
                // si esta anclado:
                //   oculta el texto
                if (haySueloDetectado && !anchorNode.isAnchored) {
                    // Solo cambiamos si es necesario para no parpadear
                    if (btnFijar.visibility != View.VISIBLE) {
                        // Intercambio: Texto FUERA, Botón DENTRO
                        tvInstructions.visibility = View.GONE
                        btnFijar.visibility = View.VISIBLE
                    }
                } else {
                    if (!anchorNode.isAnchored) {
                        if (btnFijar.visibility != View.GONE) {
                            // Intercambio: Botón FUERA, Texto DENTRO
                            btnFijar.visibility = View.GONE
                            tvInstructions.visibility = View.VISIBLE
                        }
                    } else {
                        if (tvInstructions.visibility != View.GONE) {
                            // Si ya está anclado, ocultamos el texto
                            tvInstructions.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun newModelNode(name: String) {
        // 1. CONFIGURACIÓN DEL PADRE (ANCLA)
        anchorNode = ArModelNode(sceneView.engine, PlacementMode.PLANE_HORIZONTAL).apply {
            isPositionEditable = false
            isRotationEditable = false
            isVisible = true
        }

        flightNode = Node(sceneView.engine)

        // 2. CONFIGURACIÓN DEL HIJO (VISUAL)
        visualNode = ModelNode(sceneView.engine).apply {
            loadModelGlbAsync(
                glbFileLocation = name,
                autoAnimate = true,
                scaleToUnits = null,
                centerOrigin = null,
                onLoaded = { modelInstance ->
                    val asset = modelInstance.asset
                    val box = asset.boundingBox

                    // OBTENER DIMENSIONES USANDO 'halfExtent'
                    // halfExtent es un array [x, y, z] con la "mitad" del tamaño.
                    val halfExtent = box.halfExtent

                    // Calculamos el tamaño total (Largo, Alto, Ancho)
                    val sizeX = halfExtent[0] * 2.0f
                    val sizeY = halfExtent[1] * 2.0f
                    val sizeZ = halfExtent[2] * 2.0f

                    // Cogemos el lado más grande
                    val largestDimension = maxOf(sizeX, sizeY, sizeZ)

                    // CALCULAR FACTOR DE ESCALA (Para que mida 0.5 metros)
                    val targetSize = 1.5f
                    val scaleFactor = targetSize / largestDimension

                    // APLICAR ESCALA
                    scale = Scale(scaleFactor)

                    // Arreglar rotacion
                    rotation = Rotation(-90f, 0f, 0f)

                    // CENTRAR A MANO
                    val correctX = -box.center[0] * scaleFactor
                    val correctY = -box.center[1] * scaleFactor
                    val correctZ = -box.center[2] * scaleFactor

                    position = Position(correctX, correctY, correctZ)

                    // Punto 1: En el Morro (Z positivo o negativo según tu modelo)
                    addHotspot(20.0f, -75.0f, 25.0f, scale, "La Cabina", "Aquí van los pilotos controlando el vuelo.")
                    // Punto 2: En el Ala Izquierda (X negativo)
                    addHotspot(60.0f, 15.0f, 20.0f, scale, "Ala Izquierda", "Genera sustentación para elevar el avión.")
                    // Punto 3: En la Cola (Z contrario al morro)
                    addHotspot(20.0f, 75.0f, 25.0f, scale, "Estabilizador", "Mantiene el equilibrio del avión.")
                    // Punto 4: En la turbina derecha
                    addHotspot(0.0f, -15.0f, 15.0f, scale, "Turbina derecha", "Impulsa el avión hacia adelante.")

                    //Toast.makeText(this@ArActivity, "Listo. Toca el suelo.", Toast.LENGTH_SHORT).show()

                    //anchorNode.anchor()
                },
                onError = { e ->
                    Toast.makeText(this@ArActivity, "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }

        flightNode.addChild(visualNode)

        val offsetX = 0.0f
        val offsetY = 1.0f
        val offsetZ = 0.0f
        flightNode.position = Position(offsetX, offsetY, offsetZ)

        anchorNode.addChild(flightNode)
        sceneView.addChild(anchorNode)
        sceneView.selectedNode = anchorNode
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchControls() {
        sceneView.setOnTouchListener { _, event ->
            // Si el avión no está listo, ignoramos todo
            if (!::anchorNode.isInitialized) return@setOnTouchListener false

            // DETECTAR CLICKS (Solo al pulsar)
            if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_DOWN) {
                // Solo permitimos click si ha pasado tiempo desde que soltaste los 2 dedos
                // (Para evitar clicks fantasma al soltar)
                if (System.currentTimeMillis() - lastMultiTouchTime > TOUCH_COOLDOWN_MS) {
                    if (tryClickHotspot(event)) return@setOnTouchListener true
                }
            }

            // Si cambia el número de dedos, reseteamos las coordenadas para evitar saltos
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
                event.actionMasked == MotionEvent.ACTION_POINTER_UP) {

                if (event.pointerCount == 2) {
                    // Preparamos para 2 dedos
                    lastTouchX = (event.getX(0) + event.getX(1)) / 2
                    lastTouchY = (event.getY(0) + event.getY(1)) / 2
                    // Forzamos reinicio de distancia en la función de gesto
                    lastMultiTouchDistance = 0f
                } else {
                    // Preparamos para 1 dedo
                    lastTouchX = event.x
                    lastTouchY = event.y
                    lastMultiTouchDistance = 0f // Reseteamos seguridad

                    // IMPORTANTE: Si levantas un dedo (ACTION_POINTER_UP),
                    // marcamos la hora actual para iniciar el "Tiempo de Gracia"
                    if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                        lastMultiTouchTime = System.currentTimeMillis()
                    }
                }
                return@setOnTouchListener true
            }

            // DECISIÓN: ¿Rotar o Mover?
            if (event.pointerCount == 1) {
                // UN DEDO -> ROTAR
                lastMultiTouchDistance = 0f

                val tiempoDesdeDosDedos = System.currentTimeMillis() - lastMultiTouchTime
                if (tiempoDesdeDosDedos < TOUCH_COOLDOWN_MS) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    return@setOnTouchListener true // Ignoramos el movimiento, pero consumimos el evento.
                }

                if (!isRotationLocked) return@setOnTouchListener processRotation(event)
            } else if (event.pointerCount == 2) {
                // DOS DEDOS -> MOVER
                lastMultiTouchTime = System.currentTimeMillis()
                if (!isTranslationLocked) return@setOnTouchListener processTwoFingerGestures(event)
            }

            return@setOnTouchListener true
        }
    }

    private fun tryClickHotspot(event: MotionEvent): Boolean {
        // 1. Crear Rayo desde el dedo
        val ray = sceneView.cameraNode.screenPointToRay(event.x, event.y)

        // 2. Revisar nuestra lista de hotspots registrados
        for ((node, accion) in misHotspots) {
            // Usamos la matemática pura (radio 0.2f = 20cm de margen)
            if (isRayHittingNode(ray, node, radius = 0.06f)) {
                // ¡BINGO! Ejecutamos la acción (el Dialog)
                accion.invoke()
                return true // Éxito
            }
        }
        return false // Fallo (no tocamos nada importante)
    }

    private fun processRotation(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true // Empezamos a escuchar el arrastre
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y

                // Rotación Horizontal (Y)
                val rotYAngle = deltaX * rotationSpeed
                val rotationY = Quaternion.fromAxisAngle(Float3(0.0f, 1.0f, 0.0f), rotYAngle)

                // Rotación Vertical (X) - Relativa a la cámara
                val cameraRot = sceneView.cameraNode.worldQuaternion
                val cameraRight = cameraRot * Float3(1.0f, 0.0f, 0.0f)
                val rotXAngle = deltaY * rotationSpeed
                val rotationX = Quaternion.fromAxisAngle(cameraRight, rotXAngle)

                // Aplicar al ancla
                val currentQuaternion = flightNode.quaternion
                //val finalRotation = rotationX * rotationY * currentQuaternion
                val finalRotation = rotationY * currentQuaternion
                flightNode.quaternion = finalRotation

                return true
            }
        }
        return false
    }

    private fun processTwoFingerGestures(event: MotionEvent): Boolean {
        // Calculamos el centro (X, Y) de los dos dedos
        val currentCenterX = (event.getX(0) + event.getX(1)) / 2
        val currentCenterY = (event.getY(0) + event.getY(1)) / 2
        // Distancia entre dedos (para Escala)
        val currentDist = getDistanceBetweenFingers(event)

        // --- Inicialización Automática ---
        // Si la distancia guardada es 0 (porque acabamos de empezar)
        // o si hubo un cambio brusco (reseteo), actualizamos y salimos.
        if (lastMultiTouchDistance == 0f) {
            lastTouchX = currentCenterX
            lastTouchY = currentCenterY
            lastMultiTouchDistance = currentDist
            return true
        }

        // --- INICIO DEL GESTO (Resetear valores) ---
        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            // Al poner el segundo dedo, reseteamos para evitar saltos
            lastTouchX = currentCenterX
            lastTouchY = currentCenterY
            lastMultiTouchDistance = currentDist
            return true
        }

        // --- DURANTE EL MOVIMIENTO ---
        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            // A) LÓGICA DE TRASLACIÓN (Mover)
            // ------------------------------------------------
            val deltaX = currentCenterX - lastTouchX
            val deltaY = currentCenterY - lastTouchY // En pantalla Y crece hacia abajo
            lastTouchX = currentCenterX
            lastTouchY = currentCenterY

            // --- MATEMÁTICA DE VECTORES ---
            val cameraRot = sceneView.cameraNode.worldQuaternion
            val rightVector = cameraRot * Float3(1.0f, 0.0f, 0.0f)
            val upVector = cameraRot * Float3(0.0f, 1.0f, 0.0f)
            // Calculamos cuánto movernos en cada dirección
            // Nota: deltaY negativo porque arrastrar arriba (negativo en pantalla) debe subir el avión
            val moveRight = rightVector * (deltaX * translationSpeed)
            val moveUp = upVector * (-deltaY * translationSpeed)

            // Aplicamos al avión
            val currentPos = flightNode.worldPosition
            flightNode.worldPosition = Float3(
                currentPos.x + moveRight.x + moveUp.x,
                currentPos.y + moveRight.y + moveUp.y,
                currentPos.z + moveRight.z + moveUp.z
            )

            // B) LÓGICA DE ESCALA (Zoom)
            // ------------------------------------------------
            // Evitamos dividir por 0 o saltos raros
            if (lastMultiTouchDistance > 10f && currentDist > 10f) {
                // Calculamos cuánto ha crecido/encogido
                // Ejemplo: Si antes era 100 y ahora 110, factor = 1.1 (Crecer 10%)
                val scaleFactor = currentDist / lastMultiTouchDistance

                // Aplicamos al tamaño actual
                var newScale = flightNode.scale.x * scaleFactor

                // LIMITES (Clamping): Ni muy pequeño ni muy grande
                newScale = newScale.coerceIn(minScale, maxScale)

                // Aplicamos la escala en los 3 ejes (X, Y, Z)
                flightNode.scale = Float3(newScale, newScale, newScale)

                // Guardamos la distancia actual para el siguiente frame
                lastMultiTouchDistance = currentDist
            }

            return true
        }
        return false
    }

    // --- CORRECCIÓN 2: Eliminados onPause y onResume ---
    // La librería actual gestiona esto sola, no necesitas llamarlos manualmente.

    // Solo mantenemos onDestroy para limpiar memoria si es necesario
    // (Si destroy() también te da error, puedes borrar este bloque entero)
    override fun onDestroy() {
        super.onDestroy()
        // Si esta línea te sigue dando error, bórrala también.
        // Las versiones más nuevas limpian solas al cerrarse la actividad.
        try {
            sceneView.destroy()
        } catch (e: Exception) {
            // Ignorar errores al cerrar
        }
    }


    private fun addHotspot(x: Float, y: Float, z: Float, scaleFactor: Float3, infoTitulo: String, infoMensaje: String) {
        // Creamos un nodo que contiene una Vista de Android
        val hotspotNode = ViewNode(sceneView.engine).apply {
            // Carga el XML del punto rojo
            loadView(
                this@ArActivity,
                lifecycle,
                R.layout.layout_hotspot
            )

            // ESCALA INVERSA
            val inverseScale = 1.0f / scaleFactor
            scale = Scale(inverseScale)

            // Lo colocamos en la posición relativa al avión
            position = Position(x, y, z)

            // Hacemos que mire siempre a la cámara (efecto Billboard)
            // Así el punto rojo siempre se ve redondo, no plano.
            onFrame = { _, node ->
                //val cameraPosition = sceneView.cameraNode.worldPosition
                //node.lookAt(cameraPosition)
                //node.rotation = node.rotation * Rotation(0f, 180f, 0f)

                //val cameraQuat = sceneView.cameraNode.worldQuaternion
                //node.worldQuaternion = cameraQuat
                //node.worldQuaternion = cameraQuat * Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), 180f)

                // 1. OBTENER POSICIONES GLOBALES
                // Usamos worldPosition para saber dónde están realmente en el mundo 3D
                val camPos = sceneView.cameraNode.worldPosition
                val nodePos = node.worldPosition

                // 2. CALCULAR VECTOR DIRECCIÓN (El vector que va desde el texto hacia ti)
                val direction = camPos - nodePos

                // 3. CÁLCULO MANUAL DE ÁNGULOS (Trigonometría)
                // Calculamos cuánto tiene que girar en Y (Yaw) y en X (Pitch) para mirarte.

                // Yaw: Arcotangente de la diferencia en X y Z
                // (Math.toDegrees convierte radianes a grados)
                val yaw = Math.toDegrees(atan2(direction.x.toDouble(), direction.z.toDouble())).toFloat()

                // Pitch: Arcotangente de la altura (Y) vs la distancia horizontal
                // Pitágoras para la distancia horizontal (hipotenusa en el suelo)
                val distHorizontal = sqrt((direction.x * direction.x + direction.z * direction.z).toDouble())
                val pitch = -Math.toDegrees(atan2(direction.y.toDouble(), distHorizontal)).toFloat()

                // 4. CONSTRUIR EL QUATERNION
                // Creamos la rotación combinando Pitch (X) y Yaw (Y). Roll (Z) es 0 para que no se incline de lado.
                val rotationTowardCamera = Quaternion.fromEuler(Float3(pitch, yaw, 0f))

                // 5. CORRECCIÓN DE ESPEJO (180 grados en Y)
                // Los ViewNodes (texto) suelen nacer mirando hacia "atrás". Esto los gira.
                val correction = Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), 180f)

                // 6. APLICAR AL MUNDO
                // Usamos worldQuaternion para sobrescribir cualquier rotación heredada del avión.
                node.worldQuaternion = rotationTowardCamera * correction
            }
        }

        // Guardamos este nodo y lo que queremos que haga en nuestra lista
        misHotspots[hotspotNode] = {
            // BLOQUEAMOS ROTACIÓN
            isRotationLocked = true
            isTranslationLocked = true

            // Esta es la acción que ejecutaremos manualmente
            val dialog = AlertDialog.Builder(this@ArActivity)
                .setTitle(infoTitulo)
                .setMessage(infoMensaje)
                .setPositiveButton("Cerrar", null) // El null es porque usamos el DismissListener abajo
                .create() // Usamos create() para tener la referencia

            // DETECTAR CUANDO SE CIERRA
            // Esto se ejecuta tanto si das a "Cerrar", como si tocas fuera, como si das a "Atrás".
            dialog.setOnDismissListener {
                isRotationLocked = false
                isTranslationLocked = false
            }

            // Mostrar
            dialog.show()
        }

        // Lo añadimos como HIJO del modelo visual (el avión)
        // Así, si el avión rota, el punto rota con él.
        visualNode.addChild(hotspotNode)
    }

    // Función auxiliar: Calcula si un Rayo pasa cerca de un Nodo (Matemática pura)
    private fun isRayHittingNode(ray: com.google.ar.sceneform.collision.Ray, node: io.github.sceneview.node.Node, radius: Float = 0.2f): Boolean {
        // 1. Posición del nodo y origen del rayo
        val nodePos = node.worldPosition
        val rayOrigin = ray.origin
        val rayDir = ray.direction

        // 2. Vector desde la cámara hasta el nodo
        val dx = nodePos.x - rayOrigin.x
        val dy = nodePos.y - rayOrigin.y
        val dz = nodePos.z - rayOrigin.z

        // 3. Proyección del vector sobre la dirección del rayo
        // (Producto escalar)
        val t = dx * rayDir.x + dy * rayDir.y + dz * rayDir.z

        // Si 't' es negativo, el objeto está detrás de la cámara
        if (t < 0) return false

        // 4. Punto más cercano en la línea del rayo al centro del nodo
        val closestX = rayOrigin.x + rayDir.x * t
        val closestY = rayOrigin.y + rayDir.y * t
        val closestZ = rayOrigin.z + rayDir.z * t

        // 5. Distancia al cuadrado desde ese punto cercano hasta el nodo real
        val distSq = (closestX - nodePos.x) * (closestX - nodePos.x) +
                (closestY - nodePos.y) * (closestY - nodePos.y) +
                (closestZ - nodePos.z) * (closestZ - nodePos.z)

        // 6. Si la distancia es menor que el radio (el tamaño del botón), ¡TOCADO!
        return distSq < (radius * radius)
    }

    // Calcula la distancia entre el dedo 1 y el dedo 2
    private fun getDistanceBetweenFingers(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }
}