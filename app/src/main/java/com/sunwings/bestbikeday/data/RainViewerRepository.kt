package com.sunwings.bestbikeday.data

import com.sunwings.bestbikeday.data.model.RainRadarFrame
import com.sunwings.bestbikeday.data.remote.rainviewer.RainViewerApi
import com.sunwings.bestbikeday.data.remote.rainviewer.RainViewerApiFactory

class RainViewerRepository(
    private val api: RainViewerApi = RainViewerApiFactory.api
) {
    suspend fun latestRadarFrame(): RainRadarFrame? {
        val response = api.getWeatherMaps()
        val candidate = response.radar?.nowcast?.firstOrNull()
            ?: response.radar?.past?.lastOrNull()
            ?: return null

        val host = candidate.overrideHost ?: response.host ?: return null

        return RainRadarFrame(
            host = host,
            path = candidate.path,
            timestamp = candidate.time
        )
    }
}
