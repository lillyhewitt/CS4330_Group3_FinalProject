package com.example.finalproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.finalproject.ui.theme.FinalProjectTheme
import androidx.core.app.NotificationCompat

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var isPhoneInUse = true

    // light level thresholds for notifications
    private val brightLightThreshold = 100f
    private val lowLightThreshold = 10f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize sesnor manager and sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // register sensors for event listening
        registerSensors()

        // setup for UI
        setContent {
            FinalProjectTheme {
                Scaffold(
                    content = { padding ->
                        Column(modifier = Modifier.padding(padding)) {
                            Text("LightSense: Notifications Powered by Sensors")
                        }
                    }
                )
            }
        }

        createNotificationChannel()
    }

    // register light and proximity sensors
    private fun registerSensors() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // handle sensor events
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_LIGHT -> handleLightSensor(it.values[0])
                Sensor.TYPE_PROXIMITY -> handleProximitySensor(it.values[0])
            }
        }
    }

    // handle light sensor changes
    private fun handleLightSensor(lightLevel: Float) {
        Log.d("LightSensorHandler", "Handling light level: $lightLevel")
        when {
            lightLevel < LOW_LIGHT_THRESHOLD && isPhoneInUse -> showNotification(
                "Low Light Detected", "Switch to night mode."
            )
            lightLevel > BRIGHT_LIGHT_THRESHOLD && isPhoneInUse -> showNotification(
                "Bright Light Detected", "Consider lowering your screen brightness to save battery."
            )
        }
    }

    // handle proximity sensor changes (detect if the phone is in use)
    private fun handleProximitySensor(proximityValue: Float) {
        Log.d("ProximitySensorHandler", "Handling proximity value: $proximityValue")
        isPhoneInUse = proximityValue >= (proximitySensor?.maximumRange ?: 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this example
    }

    // show notification to user
    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "LightSense_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
        // notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // create notification channel for devices
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "LightSense_channel",
                "LightSense Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Channel for LightSense app notifications" }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // unregister sensor listeners when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
