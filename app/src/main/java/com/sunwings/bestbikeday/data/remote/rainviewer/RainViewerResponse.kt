package com.sunwings.bestbikeday.data.remote.rainviewer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RainViewerResponse(
    val version: String? = null,
    val generated: Long? = null,
    val host: String? = null,
    val radar: RadarSection? = null
)

@Serializable
data class RadarSection(
    val past: List<RadarFrame> = emptyList(),
    val nowcast: List<RadarFrame> = emptyList()
)

@Serializable
data class RadarFrame(
    val time: Long,
    val path: String,
    @SerialName("host") val overrideHost: String? = null
)
