package com.example.finalproject

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.example.finalproject.ui.theme.FinalProjectTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var isPhoneInUse = true

    // Light level thresholds for notifications
    private val brightLightThreshold = 100f
    private val lowLightThreshold = 10f

    // flag to control amount of notifications
    private var canNotify = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize sensor manager and sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Register sensors for event listening
        registerSensors()

        // Setup for UI
        setContent {
            FinalProjectTheme {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                ) {
                    // main content
                    Text("LightSense: Notifications Powered by Sensors")
                }
            }
        }

        createNotificationChannel()
    }

    // Register light and proximity sensors
    private fun registerSensors() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Handle sensor events
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_LIGHT -> handleLightSensor(it.values[0])
                Sensor.TYPE_PROXIMITY -> handleProximitySensor(it.values[0])
            }
        }
    }

    // Handle light sensor changes
    private fun handleLightSensor(lightLevel: Float) {
        Log.d("SensorData", "Light level: $lightLevel")
        if (canNotify) {
            when {
                lightLevel < lowLightThreshold && isPhoneInUse -> {
                    showNotification("Low Light Detected", "Switch to night mode.")
                    startCooldown()
                }
                lightLevel > brightLightThreshold && isPhoneInUse -> {
                    showNotification("Bright Light Detected", "Consider lowering your screen brightness to save battery.")
                    startCooldown()
                }
            }
        }
    }

    // Handle proximity sensor changes (detect if the phone is in use)
    private fun handleProximitySensor(proximityValue: Float) {
        Log.d("ProximitySensorHandler", "Handling proximity value: $proximityValue")
        isPhoneInUse = proximityValue >= (proximitySensor?.maximumRange ?: 1f)
    }

    private fun startCooldown() {
        canNotify = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000) // 5-second cooldown
            canNotify = true
        }
    }

    // Show notification to user
    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "LightSense_channel")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification) // Use a unique ID
    }

    // Create notification channel for devices
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "LightSense_channel",
                "LightSense Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Channel for LightSense app notifications" }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Unregister sensor listeners when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // not needed, but implemented from abstract
    }
}