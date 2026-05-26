package com.sunwings.bestbikeday.data.remote.metno

import com.sunwings.bestbikeday.data.RideScoreCalculator
import com.sunwings.bestbikeday.data.WeatherCodeMapper
import com.sunwings.bestbikeday.data.model.DailyForecast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val DEFAULT_FORECAST_DAYS = 7
private const val METERS_PER_SECOND_TO_KPH = 3.6
private const val WET_PRECIPITATION_THRESHOLD_MM = 0.1

@Serializable
data class MetNoForecastResponse(
    val properties: MetNoProperties
)

@Serializable
data class MetNoProperties(
    val timeseries: List<MetNoTimeSeriesPoint>
)

@Serializable
data class MetNoTimeSeriesPoint(
    val time: String,
    val data: MetNoSeriesData
)

@Serializable
data class MetNoSeriesData(
    val instant: MetNoInstantData,
    @SerialName("next_1_hours") val nextOneHour: MetNoPeriodData? = null,
    @SerialName("next_6_hours") val nextSixHours: MetNoPeriodData? = null,
    @SerialName("next_12_hours") val nextTwelveHours: MetNoPeriodData? = null
)

@Serializable
data class MetNoInstantData(
    val details: MetNoInstantDetails
)

@Serializable
data class MetNoInstantDetails(
    @SerialName("air_temperature") val airTemperature: Double? = null,
    @SerialName("wind_speed") val windSpeedMetersPerSecond: Double? = null
)

@Serializable
data class MetNoPeriodData(
    val summary: MetNoSummary? = null,
    val details: MetNoPeriodDetails? = null
)

@Serializable
data class MetNoSummary(
    @SerialName("symbol_code") val symbolCode: String? = null
)

@Serializable
data class MetNoPeriodDetails(
    @SerialName("precipitation_amount") val precipitationAmountMm: Double? = null
)

fun MetNoForecastResponse.toDailyForecasts(
    zoneId: ZoneId = ZoneId.systemDefault(),
    days: Int = DEFAULT_FORECAST_DAYS
): List<DailyForecast> {
    val buckets = linkedMapOf<LocalDate, DailyBucket>()

    properties.timeseries.forEach { point ->
        val date = runCatching { Instant.parse(point.time) }
            .getOrNull()
            ?.atZone(zoneId)
            ?.toLocalDate()
            ?: return@forEach

        val bucket = buckets.getOrPut(date) { DailyBucket() }

        point.data.instant.details.airTemperature?.let { bucket.temperaturesC += it }
        point.data.instant.details.windSpeedMetersPerSecond
            ?.let { bucket.windSpeedsKph += it * METERS_PER_SECOND_TO_KPH }

        val periodData = point.data.nextOneHour ?: point.data.nextSixHours ?: point.data.nextTwelveHours

        periodData?.details?.precipitationAmountMm?.let { bucket.precipitationSamplesMm += it }
        periodData?.summary?.symbolCode?.let { bucket.symbolCodes += it }
    }

    val maxDays = days.coerceAtLeast(1)

    return buckets.entries
        .asSequence()
        .sortedBy { it.key }
        .take(maxDays)
        .map { (date, bucket) ->
            val symbol = bucket.primarySymbolCode()
            val mappedWeatherCode = MetNoSymbolMapper.toOpenMeteoCode(symbol)
            val precipitationChance = bucket.precipitationChancePercent()
            val maxTemp = bucket.temperaturesC.maxOrNull() ?: Double.NaN
            val minTemp = bucket.temperaturesC.minOrNull() ?: Double.NaN
            val maxWind = bucket.windSpeedsKph.maxOrNull() ?: Double.NaN
            val conditionDescription =
                MetNoSymbolMapper.describe(symbol)
                    ?: WeatherCodeMapper.describe(mappedWeatherCode)

            DailyForecast(
                date = date,
                maxTempC = maxTemp,
                minTempC = minTemp,
                precipitationChance = precipitationChance,
                maxWindSpeedKph = maxWind,
                weatherCode = mappedWeatherCode,
                conditionDescription = conditionDescription,
                rideScore = RideScoreCalculator.evaluate(
                    maxTempC = maxTemp,
                    minTempC = minTemp,
                    precipitationChance = precipitationChance,
                    maxWindSpeedKph = maxWind,
                    weatherCode = mappedWeatherCode
                )
            )
        }
        .toList()
}

private data class DailyBucket(
    val temperaturesC: MutableList<Double> = mutableListOf(),
    val windSpeedsKph: MutableList<Double> = mutableListOf(),
    val precipitationSamplesMm: MutableList<Double> = mutableListOf(),
    val symbolCodes: MutableList<String> = mutableListOf()
) {
    fun primarySymbolCode(): String? = symbolCodes
        .groupingBy { MetNoSymbolMapper.normalize(it) ?: it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    fun precipitationChancePercent(): Int {
        if (precipitationSamplesMm.isEmpty()) return 0
        val wetPeriods = precipitationSamplesMm.count { it >= WET_PRECIPITATION_THRESHOLD_MM }
        return ((wetPeriods.toDouble() / precipitationSamplesMm.size.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }
}

private object MetNoSymbolMapper {
    private val codeMap = mapOf(
        "clearsky" to 0,
        "fair" to 1,
        "partlycloudy" to 2,
        "cloudy" to 3,
        "fog" to 45,
        "lightrain" to 61,
        "rain" to 63,
        "heavyrain" to 65,
        "lightsleet" to 66,
        "sleet" to 67,
        "heavysleet" to 67,
        "lightsnow" to 71,
        "snow" to 73,
        "heavysnow" to 75,
        "lightrainshowers" to 80,
        "rainshowers" to 81,
        "heavyrainshowers" to 82,
        "lightsleetshowers" to 80,
        "sleetshowers" to 81,
        "heavysleetshowers" to 82,
        "lightsnowshowers" to 85,
        "snowshowers" to 85,
        "heavysnowshowers" to 86,
        "thunderstorm" to 95,
        "lightrainandthunder" to 96,
        "rainandthunder" to 96,
        "heavyrainandthunder" to 99,
        "lightsleetandthunder" to 96,
        "sleetandthunder" to 96,
        "heavysleetandthunder" to 99,
        "lightsnowandthunder" to 96,
        "snowandthunder" to 99,
        "heavysnowandthunder" to 99,
        "lightrainshowersandthunder" to 96,
        "rainshowersandthunder" to 96,
        "heavyrainshowersandthunder" to 99,
        "lightsleetshowersandthunder" to 96,
        "sleetshowersandthunder" to 96,
        "heavysleetshowersandthunder" to 99,
        "lightsnowshowersandthunder" to 96,
        "snowshowersandthunder" to 99,
        "heavysnowshowersandthunder" to 99
    )

    private val descriptionMap = mapOf(
        "clearsky" to "Clear sky",
        "fair" to "Mostly clear",
        "partlycloudy" to "Partly cloudy",
        "cloudy" to "Overcast",
        "fog" to "Foggy",
        "lightrain" to "Light rain",
        "rain" to "Moderate rain",
        "heavyrain" to "Heavy rain",
        "lightsleet" to "Light sleet",
        "sleet" to "Sleet",
        "heavysleet" to "Heavy sleet",
        "lightsnow" to "Light snow",
        "snow" to "Snow",
        "heavysnow" to "Heavy snow",
        "lightrainshowers" to "Light rain showers",
        "rainshowers" to "Rain showers",
        "heavyrainshowers" to "Heavy rain showers",
        "lightsleetshowers" to "Light sleet showers",
        "sleetshowers" to "Sleet showers",
        "heavysleetshowers" to "Heavy sleet showers",
        "lightsnowshowers" to "Light snow showers",
        "snowshowers" to "Snow showers",
        "heavysnowshowers" to "Heavy snow showers",
        "thunderstorm" to "Thunderstorm",
        "lightrainandthunder" to "Rain and thunder",
        "rainandthunder" to "Rain and thunder",
        "heavyrainandthunder" to "Heavy rain and thunder",
        "lightsleetandthunder" to "Sleet and thunder",
        "sleetandthunder" to "Sleet and thunder",
        "heavysleetandthunder" to "Heavy sleet and thunder",
        "lightsnowandthunder" to "Snow and thunder",
        "snowandthunder" to "Snow and thunder",
        "heavysnowandthunder" to "Heavy snow and thunder",
        "lightrainshowersandthunder" to "Rain showers and thunder",
        "rainshowersandthunder" to "Rain showers and thunder",
        "heavyrainshowersandthunder" to "Heavy rain showers and thunder",
        "lightsleetshowersandthunder" to "Sleet showers and thunder",
        "sleetshowersandthunder" to "Sleet showers and thunder",
        "heavysleetshowersandthunder" to "Heavy sleet showers and thunder",
        "lightsnowshowersandthunder" to "Snow showers and thunder",
        "snowshowersandthunder" to "Snow showers and thunder",
        "heavysnowshowersandthunder" to "Heavy snow showers and thunder"
    )

    fun toOpenMeteoCode(symbolCode: String?): Int {
        val normalized = normalize(symbolCode) ?: return 0
        return codeMap[normalized] ?: 0
    }

    fun describe(symbolCode: String?): String? {
        val normalized = normalize(symbolCode) ?: return null
        return descriptionMap[normalized]
    }

    fun normalize(symbolCode: String?): String? {
        val raw = symbolCode?.trim()?.lowercase() ?: return null
        return raw
            .removeSuffix("_day")
            .removeSuffix("_night")
            .removeSuffix("_polartwilight")
    }
}