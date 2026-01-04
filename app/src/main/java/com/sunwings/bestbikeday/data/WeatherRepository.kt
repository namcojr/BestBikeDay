package com.sunwings.bestbikeday.data

import com.sunwings.bestbikeday.data.model.DailyForecast
import com.sunwings.bestbikeday.data.remote.WeatherApi
import com.sunwings.bestbikeday.data.remote.WeatherApiFactory

class WeatherRepository(
    private val api: WeatherApi = WeatherApiFactory.api
) {
    suspend fun getWeeklyForecast(latitude: Double, longitude: Double): List<DailyForecast> {
        val response = api.getDailyForecast(latitude = latitude, longitude = longitude)
        return response.toDailyForecasts()
    }
}
