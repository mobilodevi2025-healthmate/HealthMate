package com.mobil.healthmate.domain.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class StepSensorManager @Inject constructor(
    private val context: Context
) {
    // Sensör verisini Flow (Akış) olarak döndüren fonksiyon
    fun getStepCount(): Flow<Int> = callbackFlow {
        // Android Sensor Servisine erişim
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounterSensor == null) {
            // Cihazda adımsayar yoksa 0 döndür
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Sensörden gelen değer float'tır, int'e çeviriyoruz
                    val steps = it.values[0].toInt()
                    trySend(steps) // Yeni değeri yayına al
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Burasıyla işimiz yok
            }
        }

        // Dinlemeyi başlat
        sensorManager.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)

        // Flow kapandığında (ViewModel ölünce) dinlemeyi durdur (Pil koruması)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}