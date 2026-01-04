package com.sunwings.bestbikeday.ui.weather

import com.sunwings.bestbikeday.data.model.DailyForecast
import com.sunwings.bestbikeday.data.model.RainRadarFrame

data class WeatherUiState(
    val isLoading: Boolean = false,
    val forecast: List<DailyForecast> = emptyList(),
    val errorMessage: String? = null,
    val userLocation: UserLocation? = null,
    val rainFrame: RainRadarFrame? = null
)

data class UserLocation(val latitude: Double, val longitude: Double)
