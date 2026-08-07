package com.sunwings.bestbikeday.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class RainRadarFrame(
    val host: String,
    val path: String,
    val timestamp: Long
)
