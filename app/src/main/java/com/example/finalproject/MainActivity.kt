package com.example.finalproject

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finalproject.ui.theme.FinalProjectTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var isPhoneInUse = true

    // Light level thresholds for notifications
    private val brightLightThreshold = 800f
    private val lowLightThreshold = 50f

    // State variables to hold sensor values and status
    private var lightLevel by mutableStateOf(0f)
    private var proximityStatus by mutableStateOf("Phone not in use")
    private var lightConditionMessage by mutableStateOf("")

    // Coroutine job
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Data Extraction: Initializing sensors
        initializeSensors()

        // Preprocessing: Setting up UI
        setContent {
            FinalProjectTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Sensor update texts at the top
                    Text("LightSense: Sensor Updates")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Light Level: $lightLevel")
                    Text("Proximity Status: $proximityStatus")
                    Spacer(modifier = Modifier.height(32.dp))

                    // Light condition message in the middle with larger font
                    if (lightConditionMessage.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = lightConditionMessage,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Registering sensors for data extraction
        registerSensors()
    }

    private fun registerSensors() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_LIGHT -> {
                    // Feature Extraction: Capturing and handling light sensor data
                    lightLevel = it.values[0]
                    handleLightSensor(lightLevel)
                }
                Sensor.TYPE_PROXIMITY -> {
                    // Feature Extraction: Capturing and handling proximity sensor data
                    isPhoneInUse = it.values[0] < (proximitySensor?.maximumRange ?: 10f)
                    proximityStatus = if (isPhoneInUse) "Phone in use" else "Phone not in use"
                }
            }
        }
    }

    private fun handleLightSensor(lightLevel: Float) {
        // Classification: Categorizing light levels
        if (isPhoneInUse) {
            when {
                lightLevel < lowLightThreshold -> {
                    lightConditionMessage = "Low Light Detected\nSwitch to night mode."
                    startMessageTimeout()
                }
                lightLevel > brightLightThreshold -> {
                    lightConditionMessage = "Bright Light Detected\nConsider lowering your screen brightness to save battery."
                    startMessageTimeout()
                }
                else -> {
                    lightConditionMessage = ""
                }
            }
        } else {
            // Clear the message if the phone is not in use
            lightConditionMessage = ""
        }
    }


    private fun startMessageTimeout() {
        // If there's already a job running, cancel it
        job?.cancel()

        // Launch a new coroutine to clear the message after 30 seconds
        job = CoroutineScope(Dispatchers.Main).launch {
            delay(30000)
            lightConditionMessage = ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        job?.cancel()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
