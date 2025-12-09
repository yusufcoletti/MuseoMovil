package com.example.museomovil

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import kotlin.math.max
import kotlin.math.min

class Vista3DActivity : AppCompatActivity() {

    private lateinit var gyroscope: Gyroscope3D
    private var modelNode: ModelNode? = null
    private lateinit var sceneView: SceneView

    private val MIN_ROTATION_Y = -45f
    private val MAX_ROTATION_Y = 45f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vista3_d)

        sceneView = findViewById(R.id.sceneView)

        loadModel("tinker.glb")

        gyroscope = Gyroscope3D(this) { rotationSpeed ->
            rotateModel(rotationSpeed)
        }
    }

    private fun loadModel(fileName: String) {
        modelNode = ModelNode(sceneView.engine).apply {
            loadModelGlbAsync(
                glbFileLocation = fileName,
                autoAnimate = true,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f),
                onError = { exception ->
                    Toast.makeText(this@Vista3DActivity, "Error al cargar modelo: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )

            rotation = Rotation(x = -90f, y = 0f, z = 0f)
        }
        sceneView.addChild(modelNode!!)
    }

    private fun rotateModel(speed: Float) {
        modelNode?.let { node ->
            val currentRotation = node.rotation
            var newY = currentRotation.y + speed
            newY = max(MIN_ROTATION_Y, min(newY, MAX_ROTATION_Y))
            node.rotation = Rotation(node.rotation.x, newY, node.rotation.z)
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope.start()
    }

    override fun onPause() {
        super.onPause()
        gyroscope.stop()
    }
}