package com.sunwings.bestbikeday.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunwings.bestbikeday.data.RainViewerRepository
import com.sunwings.bestbikeday.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository(),
    private val rainViewerRepository: RainViewerRepository = RainViewerRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState(isLoading = true))
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastCoordinates: Pair<Double, Double>? = null

    fun loadForecast(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        val targetCoordinates = latitude to longitude
        if (!forceRefresh && lastCoordinates == targetCoordinates && _uiState.value.forecast.isNotEmpty()) {
            return
        }

        lastCoordinates = targetCoordinates
        val newLocation = UserLocation(latitude, longitude)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    userLocation = newLocation
                )
            }
            val radarFrame = runCatching { rainViewerRepository.latestRadarFrame() }.getOrNull()
            runCatching {
                repository.getWeeklyForecast(latitude, longitude)
            }.onSuccess { dailyForecast ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        forecast = dailyForecast,
                        errorMessage = null,
                        userLocation = newLocation,
                        rainFrame = radarFrame ?: it.rainFrame
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to load forecast",
                        userLocation = newLocation,
                        rainFrame = radarFrame ?: it.rainFrame
                    )
                }
            }
        }
    }

    fun reportLocationIssue(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }
}
