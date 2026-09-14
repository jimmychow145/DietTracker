package com.example.diettracker

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val name: String,
    val main: MainWeather,
    val weather: List<Weather>,
)

data class MainWeather(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    val humidity: Int,
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String,
)
