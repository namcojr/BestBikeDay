package com.sunwings.bestbikeday.data.remote.model

import com.sunwings.bestbikeday.data.RideScoreCalculator
import com.sunwings.bestbikeday.data.WeatherCodeMapper
import com.sunwings.bestbikeday.data.model.DailyForecast
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyWeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerialName("daily") val dailyData: DailyData,
    @SerialName("daily_units") val dailyUnits: DailyUnits
) {
    fun toDailyForecasts(): List<DailyForecast> {
        val dates = dailyData.time
        val maxTemps = dailyData.maxTemperature
        val minTemps = dailyData.minTemperature
        val precipitation = dailyData.precipitationChance
        val weatherCodes = dailyData.weatherCode
        val windSpeeds = dailyData.maxWindSpeed

        return dates.indices.map { index ->
            val code = weatherCodes.getOrElse(index) { 0 }
            DailyForecast(
                date = LocalDate.parse(dates[index]),
                maxTempC = maxTemps.getOrElse(index) { Double.NaN },
                minTempC = minTemps.getOrElse(index) { Double.NaN },
                precipitationChance = precipitation.getOrElse(index) { 0 },
                maxWindSpeedKph = windSpeeds.getOrElse(index) { Double.NaN },
                weatherCode = code,
                conditionDescription = WeatherCodeMapper.describe(code),
                rideScore = RideScoreCalculator.evaluate(
                    maxTempC = maxTemps.getOrElse(index) { Double.NaN },
                    minTempC = minTemps.getOrElse(index) { Double.NaN },
                    precipitationChance = precipitation.getOrElse(index) { 0 },
                    maxWindSpeedKph = windSpeeds.getOrElse(index) { Double.NaN },
                    weatherCode = code
                )
            )
        }
    }
}

@Serializable
data class DailyUnits(
    val time: String,
    @SerialName("temperature_2m_max") val maxTemperature: String,
    @SerialName("temperature_2m_min") val minTemperature: String,
    @SerialName("precipitation_probability_mean") val precipitationChance: String,
    @SerialName("windspeed_10m_max") val maxWindSpeed: String,
    @SerialName("weathercode") val weatherCode: String
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("temperature_2m_max") val maxTemperature: List<Double>,
    @SerialName("temperature_2m_min") val minTemperature: List<Double>,
    @SerialName("precipitation_probability_mean") val precipitationChance: List<Int>,
    @SerialName("windspeed_10m_max") val maxWindSpeed: List<Double>,
    @SerialName("weathercode") val weatherCode: List<Int>
)
