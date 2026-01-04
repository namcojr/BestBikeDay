package com.sunwings.bestbikeday.data.model

import java.time.LocalDate

/**
 * Domain model representing basic conditions for a single day in the forecast.
 */
data class DailyForecast(
    val date: LocalDate,
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationChance: Int,
    val maxWindSpeedKph: Double,
    val weatherCode: Int,
    val conditionDescription: String,
    val rideScore: Int
)
