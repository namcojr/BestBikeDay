package com.sunwings.bestbikeday.data

import com.sunwings.bestbikeday.data.model.DailyForecast
import com.sunwings.bestbikeday.data.remote.metno.MetNoApi
import com.sunwings.bestbikeday.data.remote.metno.MetNoApiFactory
import com.sunwings.bestbikeday.data.remote.metno.toDailyForecasts
import com.sunwings.bestbikeday.data.remote.WeatherApi
import com.sunwings.bestbikeday.data.remote.WeatherApiFactory
import kotlinx.coroutines.CancellationException

class WeatherRepository(
    private val primaryApi: WeatherApi = WeatherApiFactory.api,
    private val fallbackApi: MetNoApi = MetNoApiFactory.api
) {
    suspend fun getWeeklyForecast(latitude: Double, longitude: Double): List<DailyForecast> {
        val primaryForecast = try {
            primaryApi.getDailyForecast(latitude = latitude, longitude = longitude)
                .toDailyForecasts()
                .takeIf { it.isNotEmpty() }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            null
        }

        if (primaryForecast != null) {
            return primaryForecast
        }

        return fallbackApi.getForecast(latitude = latitude, longitude = longitude)
            .toDailyForecasts()
    }
}
