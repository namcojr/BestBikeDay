package com.sunwings.bestbikeday.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sunwings.bestbikeday.BuildConfig
import com.sunwings.bestbikeday.data.remote.model.DailyWeatherResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String = "auto",
        @Query("daily") daily: String = DAILY_PARAMS,
        @Query("forecast_days") days: Int = DEFAULT_FORECAST_DAYS
    ): DailyWeatherResponse

    companion object {
        private const val DEFAULT_FORECAST_DAYS = 7
        private const val DAILY_PARAMS = "temperature_2m_max,temperature_2m_min,precipitation_probability_mean,windspeed_10m_max,weathercode"
    }
}

object WeatherApiFactory {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: WeatherApi = retrofit.create(WeatherApi::class.java)
}
