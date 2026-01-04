package com.sunwings.bestbikeday.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

fun Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

fun Context.fusedLocationProvider(): FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(this)

suspend fun FusedLocationProviderClient.awaitBestLocation(): Location? {
    val cached = runCatching { lastLocation.await() }.getOrNull()
    if (cached != null) return cached

    val cancellationSource = CancellationTokenSource()
    return try {
        getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationSource.token).await()
    } catch (error: SecurityException) {
        null
    } catch (error: Exception) {
        null
    } finally {
        cancellationSource.cancel()
    }
}
