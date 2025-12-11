package com.example.museomovil

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// Importaciones de SceneView
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion


class ArActivity : AppCompatActivity() {

    lateinit var sceneView: ArSceneView
    // El nodo que controla la POSICIÓN y ROTACIÓN en el mundo (El Padre)
    lateinit var anchorNode: ArModelNode
    // El nodo que tiene el DIBUJO 3D (El Hijo)
    lateinit var visualNode: ModelNode

    // VARIABLES PARA EL TOQUE
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    // Velocidad de rotación (ajústala si va muy rápido o lento)
    private val rotationSpeed = 0.15f

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

        newModelNode("tinker.glb")
    }

    private fun newModelNode(name: String) {
        // 1. CONFIGURACIÓN DEL PADRE (ANCLA)
        anchorNode = ArModelNode(sceneView.engine, PlacementMode.PLANE_HORIZONTAL).apply {
            isPositionEditable = false
            isRotationEditable = false
            rotation = Rotation(-90f, 0f, 0f)
            scale = Position(0.01f, 0.01f, 0.01f)
            position = Position(0.0f, -0.5f, -1.0f)
        }

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
                    val centerX = box.center[0]
                    val centerY = box.center[1]
                    val centerZ = box.center[2]
                    position = Position(-centerX, -centerY, -centerZ)
                    Toast.makeText(this@ArActivity, "Listo. Toca el suelo.", Toast.LENGTH_SHORT).show()
                },
                onError = { }
            )
        }

        anchorNode.addChild(visualNode)
        sceneView.addChild(anchorNode)
        sceneView.selectedNode = anchorNode

        setupRotationGesture()
    }

    private fun setupRotationGesture() {
        sceneView.setOnTouchListener { _, event ->
            // Comprobamos si el ANCHOR NODE (Padre) está listo
            if (!::anchorNode.isInitialized) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y

                    val speed = 0.5f

                    // 1. Rotación Horizontal
                    val rotYAngle = -deltaX * speed
                    val rotationY = Quaternion.fromAxisAngle(Float3(0.0f, 1.0f, 0.0f), rotYAngle)

                    // 2. Rotación Vertical (Cámara)
                    val cameraRot = sceneView.cameraNode.worldQuaternion
                    val cameraRight = cameraRot * Float3(1.0f, 0.0f, 0.0f)
                    val rotXAngle = deltaY * speed
                    val rotationX = Quaternion.fromAxisAngle(cameraRight, rotXAngle)

                    // 3. APLICAR AL PADRE (anchorNode)
                    // Usamos el quaternion del padre
                    val currentQuaternion = anchorNode.quaternion
                    val finalRotation = rotationX * rotationY * currentQuaternion

                    anchorNode.quaternion = finalRotation

                    true
                }
                else -> false
            }
            true
        }
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
}