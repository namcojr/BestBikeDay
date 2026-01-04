package com.sunwings.bestbikeday.data

/**
 * Maps Open-Meteo weather codes to approachable descriptions.
 */
object WeatherCodeMapper {
    private val codeMap = mapOf(
        0 to "Clear sky",
        1 to "Mainly clear",
        2 to "Partly cloudy",
        3 to "Overcast",
        45 to "Foggy",
        48 to "Depositing rime fog",
        51 to "Light drizzle",
        53 to "Moderate drizzle",
        55 to "Dense drizzle",
        56 to "Freezing drizzle",
        57 to "Heavy freezing drizzle",
        61 to "Light rain",
        63 to "Moderate rain",
        65 to "Heavy rain",
        66 to "Freezing rain",
        67 to "Heavy freezing rain",
        71 to "Light snow",
        73 to "Moderate snow",
        75 to "Heavy snow",
        77 to "Snow grains",
        80 to "Rain showers",
        81 to "Heavy rain showers",
        82 to "Violent rain showers",
        85 to "Snow showers",
        86 to "Heavy snow showers",
        95 to "Thunderstorm",
        96 to "Thunderstorm with hail",
        99 to "Severe thunderstorm"
    )

    fun describe(code: Int): String = codeMap[code] ?: "Unknown conditions"
}
