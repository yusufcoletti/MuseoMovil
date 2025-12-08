package com.example.museomovil

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class Gyroscope3D(context: Context, private val onRotationUpdate: (Float) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    private var lastTimestamp: Long = 0
    private var currentTiltAngleY: Float = 0f // Ángulo acumulado (inclinación)

    // Factores de velocidad para la rotación del modelo
    private val SLOW_SPEED = 0.5f
    private val FAST_SPEED = 2.0f

    fun start() {
        if (gyroscope != null) {
            // SENSOR_DELAY_GAME es adecuado para interacciones fluidas
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun resetAngle() {
        currentTiltAngleY = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (lastTimestamp == 0L) {
            lastTimestamp = event.timestamp
            return
        }

        // Calcular el tiempo transcurrido en segundos
        val dt = (event.timestamp - lastTimestamp) * NS2S
        lastTimestamp = event.timestamp

        // event.values[1] es la rotación alrededor del eje Y (girar pantalla izq/der)
        // Integramos la velocidad angular para obtener el ángulo de inclinación actual
        val axisY = event.values[1]
        currentTiltAngleY += axisY * dt * (180.0f / Math.PI.toFloat())

        // Lógica solicitada:
        // 10 grados -> lento
        // 15 grados -> rápido
        
        val absAngle = abs(currentTiltAngleY)
        var rotationStep = 0f

        // Determinar la dirección (signo)
        val direction = if (currentTiltAngleY > 0) 1 else -1

        if (absAngle >= 15) {
            // 15 grados o más: Rápido
            rotationStep = FAST_SPEED * direction
        } else if (absAngle >= 10) {
            // Entre 10 y 15 grados (o cerca de 10): Lento
            rotationStep = SLOW_SPEED * direction
        } else {
            // Menos de 10 grados: Zona muerta o sin movimiento
            rotationStep = 0f
        }

        // Enviar el paso de rotación al modelo 3D
        // Esto debería llamarse continuamente para animar el modelo
        if (rotationStep != 0f) {
            onRotationUpdate(rotationStep)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se requiere acción
    }

    companion object {
        private const val NS2S = 1.0f / 1000000000.0f
    }
}
