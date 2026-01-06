package com.sunwings.bestbikeday.data

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Simple heuristic that rates how ride-friendly a day is by using temperature comfort as the
 * baseline and modulating it with precipitation risk and wind impact. Output is 0-100 where 100 is
 * ideal conditions.
 */
object RideScoreCalculator {
    private const val OPTIMAL_TEMP_C = 26.0
    private const val MAX_REASONABLE_WIND_KPH = 45.0

    private const val CONDITION_MODIFIER_MIN = 0.4
    private const val CONDITION_MODIFIER_MAX = 1.0

    private const val RAIN_WEIGHT = 0.7
    private const val WIND_WEIGHT = 0.3

    private val TEMPERATURE_COMFORT_CURVE =
        listOf(
            ComfortAnchor(-10.0, 0.0),
            ComfortAnchor(0.0, 0.05),
            ComfortAnchor(5.0, 0.1),
            ComfortAnchor(10.0, 0.15),
            ComfortAnchor(15.0, 0.55),
            ComfortAnchor(20.0, 0.82),
            ComfortAnchor(22.0, 0.9),
            ComfortAnchor(25.0, 1.0),
            ComfortAnchor(27.0, 1.0),
            ComfortAnchor(30.0, 0.90),
            ComfortAnchor(32.0, 0.75),
            ComfortAnchor(35.0, 0.35),
            ComfortAnchor(40.0, 0.05)
        )
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

        val conditionsSuitability = combinedConditionSuitability(rainScore, windScore)
        val modifier = lerp(
            CONDITION_MODIFIER_MIN,
            CONDITION_MODIFIER_MAX,
            conditionsSuitability
        )
        val blended = tempScore * modifier

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

        return temperatureComfortScore(avgTemp)
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

    private fun combinedConditionSuitability(
        rainScore: Double,
        windScore: Double
    ): Double {
        val weighted = (rainScore * RAIN_WEIGHT) + (windScore * WIND_WEIGHT)
        return weighted.coerceIn(0.0, 1.0)
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

    // Gently escalates rain severity once the 25% threshold is crossed so 50% feels
    // almost certain while 90% nearly maxes out the penalty.
    private fun logScaledGrowth(normalizedProbability: Double): Double {
        if (normalizedProbability <= 0.0) return 0.0
        val numerator = ln(1 + RAIN_LOG_STEEPNESS * normalizedProbability)
        return (numerator / RAIN_LOG_DENOMINATOR).coerceIn(0.0, 1.0)
    }

    private fun temperatureComfortScore(tempC: Double): Double {
        val anchors = TEMPERATURE_COMFORT_CURVE
        val clampedTemp = tempC.coerceIn(anchors.first().tempC, anchors.last().tempC)
        val upperIndex = anchors.indexOfFirst { clampedTemp <= it.tempC }
        if (upperIndex == -1) return anchors.last().score
        if (upperIndex == 0) return anchors.first().score
        val lower = anchors[upperIndex - 1]
        val upper = anchors[upperIndex]
        if (upper.tempC == lower.tempC) return upper.score
        val fraction = (clampedTemp - lower.tempC) / (upper.tempC - lower.tempC)
        return lerp(lower.score, upper.score, fraction).coerceIn(0.0, 1.0)
    }

    private fun lerp(start: Double, end: Double, fraction: Double): Double {
        val clampedFraction = fraction.coerceIn(0.0, 1.0)
        return start + (end - start) * clampedFraction
    }
}

private data class ComfortAnchor(val tempC: Double, val score: Double)
