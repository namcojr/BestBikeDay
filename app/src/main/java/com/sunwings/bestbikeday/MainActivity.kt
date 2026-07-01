package com.sunwings.bestbikeday

import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sunwings.bestbikeday.ui.theme.BestBikeDayTheme
import com.sunwings.bestbikeday.ui.weather.WeatherRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighestRefreshRate()
        setContent {
            BestBikeDayTheme {
                WeatherRoute()
            }
        }
    }

    /**
     * Ask the platform to render at the display's fastest refresh rate (e.g. 120Hz) so list
     * scrolling stays smooth. Some OEMs otherwise pin the window to 60Hz. We only switch between
     * modes that share the current resolution to avoid dropping display quality.
     */
    private fun enableHighestRefreshRate() {
        val display: Display =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION") windowManager.defaultDisplay
                }
                        ?: return

        val currentMode = display.mode ?: return
        val fastestMode =
                display.supportedModes
                        .filter {
                            it.physicalWidth == currentMode.physicalWidth &&
                                    it.physicalHeight == currentMode.physicalHeight
                        }
                        .maxByOrNull { it.refreshRate }
                        ?: return

        if (fastestMode.modeId != currentMode.modeId) {
            window.attributes =
                    window.attributes.apply { preferredDisplayModeId = fastestMode.modeId }
        }
    }
}