package com.sunwings.bestbikeday.ui.weather

import com.sunwings.bestbikeday.data.model.DailyForecast

data class WeatherUiState(
    val isLoading: Boolean = false,
    val forecast: List<DailyForecast> = emptyList(),
    val errorMessage: String? = null,
    val userLocation: UserLocation? = null
)

data class UserLocation(val latitude: Double, val longitude: Double)
