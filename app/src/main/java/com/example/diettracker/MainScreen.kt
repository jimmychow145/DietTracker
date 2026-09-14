package com.example.diettracker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * MainScreen activity for the Diet Tracker application.
 * Handles displaying user profile information, retrieving location & weather data,
 * and scheduling daily notification reminders.
 */
class MainScreen : AppCompatActivity() {

    // UI and Location components
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationText: TextView
    private lateinit var usernameText: TextView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 2001
        private const val NOTIFICATION_CHANNEL_ID = "diet_channel"
        private const val ALARM_PENDING_INTENT_REQUEST_CODE = 121
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge UI layout
        enableEdgeToEdge()

        // Set up notification system and daily alarms
        createNotificationChannel()
        scheduleNotification()

        setContentView(R.layout.activity_main_screen)

        // Adjust layout padding to accommodate system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request POST_NOTIFICATIONS permission for Android 13 (API 33) and above if not already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        // Display current Firebase user email
        val user = FirebaseAuth.getInstance().currentUser
        locationText = findViewById(R.id.Location)
        usernameText = findViewById(R.id.Username)
        usernameText.text = "Username: ${user?.email ?: "Unknown"}"

        // Initialize location provider client and fetch current location
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
    }

    /**
     * Creates a notification channel required on Android O (API 26) and higher
     * for sending daily diet tracker reminders.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Diet Tracker Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily diet reminders"
            }

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks location permissions and retrieves the user's last known location.
     * If permission is granted, initiates weather data lookup using latitude & longitude coordinates.
     */
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Fetch last known location asynchronously
        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                locationText.text = "Latitude: $lat\nLongitude: $lon"
                getWeather(lat, lon)
            } else {
                locationText.text = "Unable to get location"
            }
        }.addOnFailureListener { e ->
            locationText.text = "Location error: ${e.message}"
        }
    }

    /**
     * Callback for handling runtime permission request results (e.g., location permission).
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            locationText.text = "Location permission denied"
        }
    }

    /**
     * Fetches current weather details using Retrofit in a coroutine on lifecycleScope
     * and updates the UI with location name, temperature, condition, and humidity.
     */
    private fun getWeather(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val weather = RetrofitClient.weatherApi.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = RetrofitClient.WEATHER_API_KEY
                )

                locationText.text =
                    "Location: ${weather.name}\n" +
                            "Temperature: ${weather.main.temp}°F\n" +
                            "Feels like: ${weather.main.feelsLike}°F\n" +
                            "Condition: ${weather.weather.firstOrNull()?.description ?: "N/A"}\n" +
                            "Humidity: ${weather.main.humidity}%"
            } catch (e: Exception) {
                locationText.text = "Weather error: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Schedules a daily repeating alarm at 6:00 PM to trigger the notification broadcast receiver.
     */
    private fun scheduleNotification() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, Notification::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If 6 PM has already passed today, set for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Set daily repeating wakeup alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
