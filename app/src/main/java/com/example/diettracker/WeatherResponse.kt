package com.example.diettracker

data class WeatherResponse(
    val name: String,
    val main: MainWeather,
    val weather: List<Weather>
)

data class MainWeather(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)