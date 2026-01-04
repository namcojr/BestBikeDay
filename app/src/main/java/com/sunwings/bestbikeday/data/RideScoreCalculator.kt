package com.sunwings.bestbikeday.data

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Simple heuristic that rates how ride-friendly a day is by blending temperature comfort,
 * precipitation risk, and wind impact. Output is 0-100 where 100 is ideal conditions.
 */
object RideScoreCalculator {
    private const val OPTIMAL_TEMP_C = 22.0
    private const val TEMP_TOLERANCE_C = 15.0
    private const val MAX_REASONABLE_WIND_KPH = 45.0

    private const val TEMP_WEIGHT = 0.3
    private const val RAIN_WEIGHT = 0.55
    private const val WIND_WEIGHT = 0.15
    private const val RAIN_PROBABILITY_THRESHOLD = 25
    private const val RAIN_LOG_STEEPNESS = 400.0
    private val RAIN_LOG_DENOMINATOR = ln(1 + RAIN_LOG_STEEPNESS)

    fun evaluate(
        maxTempC: Double,
        minTempC: Double,
        precipitationChance: Int,
        maxWindSpeedKph: Double,
        weatherCode: Int
    ): Int {
        val tempScore = temperatureComponent(maxTempC, minTempC)
        val rainScore = rainComponent(precipitationChance)
        val windScore = windComponent(maxWindSpeedKph)

        val blended = (tempScore * TEMP_WEIGHT) +
            (rainScore * RAIN_WEIGHT) +
            (windScore * WIND_WEIGHT)

        val severityPenalty = weatherSeverityPenalty(
            weatherCode = weatherCode,
            precipitationChance = precipitationChance,
            windSpeedKph = maxWindSpeedKph
        )
        val adjusted = blended * (1 - severityPenalty)

        return (adjusted * 100).roundToInt().coerceIn(0, 100)
    }

    private fun temperatureComponent(maxTempC: Double, minTempC: Double): Double {
        val avgTemp = listOf(maxTempC, minTempC)
            .filterNot { it.isNaN() }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?: OPTIMAL_TEMP_C

        val delta = abs(avgTemp - OPTIMAL_TEMP_C)
        val normalized = 1 - (delta / TEMP_TOLERANCE_C)
        return normalized.coerceIn(0.0, 1.0)
    }

    private fun rainComponent(precipitationChance: Int): Double {
        val severity = rainSeverity(precipitationChance)
        return (1 - severity).coerceIn(0.0, 1.0)
    }

    private fun windComponent(maxWindSpeedKph: Double): Double {
        if (maxWindSpeedKph.isNaN()) return 0.8
        val clamped = maxWindSpeedKph.coerceIn(0.0, MAX_REASONABLE_WIND_KPH)
        val normalized = 1 - (clamped / MAX_REASONABLE_WIND_KPH)
        return normalized.coerceIn(0.0, 1.0).pow(1.4)
    }

    private fun weatherSeverityPenalty(
        weatherCode: Int,
        precipitationChance: Int,
        windSpeedKph: Double
    ): Double {
        val codePenalty = when (weatherCode) {
            0 -> 0.0
            1 -> 0.02
            2, 3 -> 0.05
            45, 48 -> 0.08
            in 51..55 -> 0.15
            56, 57 -> 0.2
            61, 62 -> 0.25
            63 -> 0.4
            65 -> 0.5
            66, 67 -> 0.45
            71, 73, 75 -> 0.25
            77 -> 0.2
            80 -> 0.45
            81 -> 0.55
            82 -> 0.65
            85, 86 -> 0.35
            95 -> 0.65
            96, 99 -> 0.75
            else -> 0.2
        }

        val rainPenalty = rainSeverity(precipitationChance) * 0.25

        val windPenalty = if (windSpeedKph.isNaN()) {
            0.0
        } else {
            (max(0.0, windSpeedKph - 20) / 60.0).coerceIn(0.0, 1.0) * 0.2
        }

        return (codePenalty + rainPenalty + windPenalty).coerceIn(0.0, 0.8)
    }

    private fun rainSeverity(precipitationChance: Int): Double {
        val clamped = precipitationChance.coerceIn(0, 100)
        if (clamped < RAIN_PROBABILITY_THRESHOLD) return 0.0
        val normalized = (clamped - RAIN_PROBABILITY_THRESHOLD).toDouble() /
            (100 - RAIN_PROBABILITY_THRESHOLD)
        return logScaledGrowth(normalized.coerceIn(0.0, 1.0))
    }

    // Gently escalates rain severity once the 30% threshold is crossed so 50% feels
    // almost certain while 90% nearly maxes out the penalty.
    private fun logScaledGrowth(normalizedProbability: Double): Double {
        if (normalizedProbability <= 0.0) return 0.0
        val numerator = ln(1 + RAIN_LOG_STEEPNESS * normalizedProbability)
        return (numerator / RAIN_LOG_DENOMINATOR).coerceIn(0.0, 1.0)
    }
}
