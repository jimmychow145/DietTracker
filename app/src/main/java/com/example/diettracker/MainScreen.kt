package com.example.diettracker
import android.Manifest
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import java.util.Date

class MainScreen : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationText: TextView
    private lateinit var usernameText: TextView

    private val LOCATION_PERMISSION_REQUEST = 1001
    private val weatherApi = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        scheduleNotification()
        setContentView(R.layout.activity_main_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                2001
            )
        }
        // Initialize the TextView and Button from the layout
        val user = FirebaseAuth.getInstance().currentUser
        locationText = findViewById(R.id.Location)
        usernameText = findViewById(R.id.Username)
        usernameText.text = "Username:" + user?.email
        // Initialize the location provider client
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            "diet_channel",
            "Diet Tracker Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        channel.description = "Daily diet reminders"

        val manager =
            getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.createNotificationChannel(channel)
    }

    private fun getCurrentLocation() {
        // Check if the location permission is granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is not granted, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        // Fetch the last known location
        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // If location is available, extract latitude and longitude
                val lat = location.latitude
                val lon = location.longitude

                // Display location in the TextView
                locationText.text = "Latitude: $lat\nLongitude: $lon"
                getWeather(lat, lon)

            } else {
                // If location is null, display an error message
                locationText.text = "Unable to get location"
            }
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if the permission was granted
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is granted, fetch the location
            getCurrentLocation()
        } else {
            // If permission is denied, update the TextView with an error message
            locationText.text = "Location permission denied"
        }
    }

    private fun getWeather(lat: Double, lon: Double) {

        lifecycleScope.launch {
            try {

                val weather = weatherApi.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = "bc9d58ed501fc4fd725c26f48017734f"
                )

                locationText.text =
                    "Location: ${weather.name}\n" +
                            "Temperature: ${weather.main.temp}°F\n" +
                            "Feels like: ${weather.main.feels_like}°F\n" +
                            "Condition: ${weather.weather[0].description}\n" +
                            "Humidity: ${weather.main.humidity}%"

            } catch (e: Exception) {
                locationText.text = "Weather error: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")

    private fun showAlert(time: Long, title: String, message: String) {
        // Format the time for display
        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        // Create and show an alert dialog with notification details
        AlertDialog.Builder(this)
            .setTitle("Notification Scheduled")
            .setMessage(
                "Title: $title\nMessage: $message\nAt: ${dateFormat.format(date)} ${
                    timeFormat.format(
                        date
                    )
                }"
            )
            .setPositiveButton("Okay") { _, _ -> }
            .show()
    }

    private fun scheduleNotification() {

        val alarmManager =
            getSystemService(ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, Notification::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            121,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If 6 PM already passed today,
            // start tomorrow.
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }


    fun checkNotificationPermissions(context: Context): Boolean {
        // Check if notification permissions are granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val isEnabled = notificationManager.areNotificationsEnabled()

            if (!isEnabled) {
                // Open the app notification settings if notifications are not enabled
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                context.startActivity(intent)

                return false
            }
        } else {
            val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

            if (!areEnabled) {
                // Open the app notification settings if notifications are not enabled
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                context.startActivity(intent)

                return false
            }
        }

        // Permissions are granted
        return true
    }
}