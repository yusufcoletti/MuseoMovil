package com.example.museomovil

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode

class Vista3DActivity : AppCompatActivity() {

    private var modelNode: ModelNode? = null
    private lateinit var sceneView: SceneView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vista3_d)

        sceneView = findViewById(R.id.sceneView)

        // Obtener el archivo del modelo desde el Intent
        val modelFileName = intent.getStringExtra("MODEL_FILE") ?: "tinker.glb"
        val modelName = intent.getStringExtra("MODEL_NAME") ?: "Modelo 3D"

        loadModel(modelFileName)

        // CONFIGURACIÓN BOTÓN VOLVER (Para salir del modo 3D)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver3D)
        btnVolver.setOnClickListener {
            finish() // Cierra la pantalla y vuelve al Catálogo
        }
    }

    private fun loadModel(fileName: String) {
        // Si ya hay un nodo, lo eliminamos para evitar duplicados en el re-intento
        modelNode?.let { sceneView.removeChild(it) }

        modelNode = ModelNode(sceneView.engine).apply {
            loadModelGlbAsync(
                glbFileLocation = fileName,
                autoAnimate = false,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f),
                onError = { exception ->
                    // Si el error no es ya con el tinker, intentamos cargar el tinker por defecto
                    if (fileName != "tinker.glb") {
                        Toast.makeText(
                            this@Vista3DActivity,
                            "Modelo no encontrado. Cargando Tinker por defecto.",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadModel("tinker.glb")
                    } else {
                        Toast.makeText(
                            this@Vista3DActivity,
                            "Error crítico: No se pudo cargar ni el modelo por defecto.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
        sceneView.addChild(modelNode!!)
    }
}
