package com.sunwings.bestbikeday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sunwings.bestbikeday.ui.theme.BestBikeDayTheme
import com.sunwings.bestbikeday.ui.weather.WeatherRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BestBikeDayTheme {
                WeatherRoute()
            }
        }
    }
}