package com.example.diettracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
}