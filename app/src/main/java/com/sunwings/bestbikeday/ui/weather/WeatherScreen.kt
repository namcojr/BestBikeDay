package com.sunwings.bestbikeday.ui.weather

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunwings.bestbikeday.R
import com.sunwings.bestbikeday.data.model.DailyForecast
import com.sunwings.bestbikeday.data.model.RainRadarFrame
import com.sunwings.bestbikeday.location.awaitBestLocation
import com.sunwings.bestbikeday.location.fusedLocationProvider
import com.sunwings.bestbikeday.location.hasLocationPermission
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

@Composable
fun WeatherRoute(modifier: Modifier = Modifier, viewModel: WeatherViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedClient = remember { context.fusedLocationProvider() }
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var isResolvingLocation by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }
    val lastUpdatedState = rememberUpdatedState(uiState.lastUpdatedEpochMillis)
    val hasPermissionState = rememberUpdatedState(hasPermission)

    val permissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions.any { it.value }
                hasPermission = granted
                if (granted) {
                    refreshToken++
                }
            }

    DisposableEffect(lifecycleOwner) {
        var backgroundedAt: Long? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isAppInForeground = true
                    val lastUpdated = lastUpdatedState.value
                    val backgroundMoment = backgroundedAt
                    val now = System.currentTimeMillis()
                    if (lastUpdated != null &&
                                    backgroundMoment != null &&
                                    hasPermissionState.value &&
                                    now - backgroundMoment >= IDLE_REFRESH_THRESHOLD_MILLIS &&
                                    now - lastUpdated >= IDLE_REFRESH_THRESHOLD_MILLIS
                    ) {
                        refreshToken++
                    }
                    backgroundedAt = null
                }
                Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                    backgroundedAt = System.currentTimeMillis()
                }
                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission, refreshToken) {
        if (hasPermission) {
            isResolvingLocation = true
            val location = fusedClient.awaitBestLocation()
            isResolvingLocation = false
            if (location != null) {
                viewModel.loadForecast(location.latitude, location.longitude, forceRefresh = true)
            } else {
                viewModel.reportLocationIssue("Unable to determine your location. Try refreshing.")
            }
        }
    }

    LaunchedEffect(uiState.lastUpdatedEpochMillis, hasPermission, isAppInForeground) {
        val lastUpdated = uiState.lastUpdatedEpochMillis ?: return@LaunchedEffect
        if (!hasPermission || !isAppInForeground) return@LaunchedEffect
        val elapsed = System.currentTimeMillis() - lastUpdated
        val remaining = max(0L, IDLE_REFRESH_THRESHOLD_MILLIS - elapsed)
        if (remaining > 0L) {
            delay(remaining)
        }
        if (!isAppInForeground || !hasPermission) return@LaunchedEffect
        val latestTimestamp =
                viewModel.uiState.value.lastUpdatedEpochMillis ?: return@LaunchedEffect
        if (System.currentTimeMillis() - latestTimestamp >= IDLE_REFRESH_THRESHOLD_MILLIS) {
            refreshToken++
        }
    }

    WeatherScreen(
            uiState = uiState,
            permissionGranted = hasPermission,
            isRequestingLocation = isResolvingLocation,
            onRequestPermission = { permissionLauncher.launch(locationPermissions) },
            onRefresh = {
                if (hasPermission) {
                    refreshToken++
                } else {
                    permissionLauncher.launch(locationPermissions)
                }
            },
            modifier = modifier
    )
}

private val locationPermissions =
        arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

private const val IDLE_REFRESH_MINUTES = 30L
private val IDLE_REFRESH_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(IDLE_REFRESH_MINUTES)

@Composable
private fun WeatherScreen(
        uiState: WeatherUiState,
        permissionGranted: Boolean,
        isRequestingLocation: Boolean,
        onRequestPermission: () -> Unit,
        onRefresh: () -> Unit,
        modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = isSystemInDarkTheme()

    val gradientBrush =
            remember(
                    colorScheme.surfaceVariant,
                    colorScheme.background,
                    colorScheme.surface,
                    isDarkMode
            ) {
                val lightColors =
                        listOf(
                                colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                colorScheme.background
                        )
                val darkColors = listOf(colorScheme.surface.copy(), colorScheme.surface.copy())
                Brush.verticalGradient(if (isDarkMode) darkColors else lightColors)
            }

    var showRadar by rememberSaveable { mutableStateOf(false) }
    val canShowRadar = uiState.userLocation != null

    LaunchedEffect(canShowRadar) {
        if (!canShowRadar && showRadar) {
            showRadar = false
        }
    }

    Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .padding(top = 64.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                            painter = painterResource(id = R.drawable.banner),
                            contentDescription = "Banner",
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.FillWidth
                    )
                }
            }
    ) { innerPadding ->
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(gradientBrush)
                                .padding(innerPadding)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            when {
                !permissionGranted -> PermissionRequestCard(onRequestPermission)
                isRequestingLocation -> LoadingState(text = "Finding your location...")
                uiState.isLoading && uiState.forecast.isEmpty() ->
                        LoadingState(text = "Fetching forecast...")
                uiState.errorMessage != null ->
                        ErrorState(message = uiState.errorMessage, onRetry = onRefresh)
                uiState.forecast.isEmpty() -> EmptyState(onRefresh = onRefresh)
                else ->
                        ForecastOrRadarSection(
                                forecast = uiState.forecast,
                                isRefreshing = uiState.isLoading,
                                onRefresh = onRefresh,
                                showingRadar = showRadar,
                                canShowRadar = canShowRadar,
                                onToggleRadar = {
                                    if (canShowRadar) {
                                        showRadar = !showRadar
                                    }
                                },
                                userLocation = uiState.userLocation,
                                rainFrame = uiState.rainFrame
                        )
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(onRequestPermission: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
    ) {
        Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Location access needed", style = MaterialTheme.typography.titleMedium)
            Text(
                    text =
                            "We use your approximate location to fetch the most accurate local weather.",
                    style = MaterialTheme.typography.bodyMedium
            )
            Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0),
                                    contentColor = Color.White
                            )
            ) { Text(text = "Grant permission") }
        }
    }
}

@Composable
private fun LoadingState(text: String) {
    Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                text = text,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                contentColor = Color.White
                        )
        ) { Text(text = "Try again") }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No forecast available yet.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(8.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                contentColor = Color.White
                        )
        ) { Text(text = "Refresh") }
    }
}

@Composable
private fun ForecastOrRadarSection(
        forecast: List<DailyForecast>,
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        showingRadar: Boolean,
        canShowRadar: Boolean,
        onToggleRadar: () -> Unit,
        userLocation: UserLocation?,
        rainFrame: RainRadarFrame?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderActions(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                showingRadar = showingRadar,
                canShowRadar = canShowRadar,
                onToggleRadar = onToggleRadar
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (showingRadar && canShowRadar) {
                RadarMapCard(
                        userLocation = userLocation,
                        rainFrame = rainFrame,
                        modifier = Modifier.fillMaxSize()
                )
            } else {
                ForecastCardsList(forecast = forecast, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun ForecastCardsList(forecast: List<DailyForecast>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(forecast) { day -> DailyForecastCard(day = day) }
    }
}

@Composable
private fun HeaderActions(
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        showingRadar: Boolean,
        canShowRadar: Boolean,
        onToggleRadar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                text = if (isRefreshing) "Updating forecast..." else "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                    onClick = onToggleRadar,
                    enabled = canShowRadar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0),
                                    contentColor = Color.White
                            )
            ) { Text(text = if (showingRadar) "Forecasts" else "Rain Radar") }
            Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0),
                                    contentColor = Color.White
                            )
            ) { Text(text = if (isRefreshing) "Refreshing" else "Refresh forecast") }
        }
    }
}

@Composable
private fun RadarMapCard(
        userLocation: UserLocation?,
        rainFrame: RainRadarFrame?,
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxSize(), contentAlignment = Alignment.Center) {
            if (userLocation != null && rainFrame != null) {
                RadarMapView(
                        location = userLocation,
                        rainFrame = rainFrame,
                        colorScheme = DEFAULT_RAIN_COLOR_SCHEME,
                        modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                            text =
                                    if (userLocation == null) {
                                        "Need your location to show the radar"
                                    } else {
                                        "Radar data unavailable right now"
                                    },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarMapView(
        location: UserLocation,
        rainFrame: RainRadarFrame,
        colorScheme: Int,
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView =
            remember(context) {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setTilesScaledToDpi(true)
                    // RainViewer tiles cap at zoom level 10 per their latest policy.
                    maxZoomLevel = 10.0
                }
            }
    val radarOverlay =
            remember(mapView, rainFrame, colorScheme) {
                createRainViewerOverlay(mapView, rainFrame, colorScheme)
            }
    val userMarker =
            remember(mapView) {
                Marker(mapView).apply {
                    title = "You are here"
                    icon =
                            AppCompatResources.getDrawable(
                                    context,
                                    org.osmdroid.library.R.drawable.marker_default
                            )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            }
    val lastCameraLocation = remember { mutableStateOf<GeoPoint?>(null) }
    val activeRadarOverlay = remember { mutableStateOf<TilesOverlay?>(null) }

    LaunchedEffect(context) {
        val appContext = context.applicationContext
        Configuration.getInstance().userAgentValue = appContext.packageName
    }

    LaunchedEffect(rainFrame.timestamp, colorScheme) {
        mapView.invalidate()
        mapView.postInvalidateOnAnimation()
        mapView.post {
            mapView.invalidate()
            mapView.postInvalidateOnAnimation()
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
            modifier = modifier,
            factory = { mapView },
            update = { view: MapView ->
                val newPoint = GeoPoint(location.latitude, location.longitude)
                val previousOverlay = activeRadarOverlay.value
                if (previousOverlay != null && previousOverlay !== radarOverlay) {
                    view.overlays.remove(previousOverlay)
                }
                if (!view.overlays.contains(radarOverlay)) {
                    view.overlays.add(radarOverlay)
                }
                activeRadarOverlay.value = radarOverlay
                if (!view.overlays.contains(userMarker)) {
                    view.overlays.add(userMarker)
                }
                userMarker.position = newPoint

                val previous = lastCameraLocation.value
                if (previous == null) {
                    view.controller.setZoom(9.0)
                    view.controller.setCenter(newPoint)
                } else if (previous.distanceToAsDouble(newPoint) > 50) {
                    view.controller.animateTo(newPoint)
                }
                lastCameraLocation.value = newPoint
                view.invalidate()
                view.postInvalidateOnAnimation()
                view.post {
                    view.invalidate()
                    view.postInvalidateOnAnimation()
                }
            }
    )
}

private fun createRainViewerOverlay(
        mapView: MapView,
        frame: RainRadarFrame,
        colorScheme: Int
): TilesOverlay {
    val appContext = mapView.context.applicationContext
    val normalizedHost = frame.host.trimEnd('/')
    val normalizedPath = frame.path.trimStart('/')
    val tileSourceName = "RAIN_VIEWER_${frame.timestamp}_$colorScheme"
    val tileSource =
            object :
                    OnlineTileSourceBase(
                            tileSourceName,
                            3,
                            10,
                            RAIN_TILE_SIZE,
                            ".png",
                            arrayOf(normalizedHost)
                    ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = MapTileIndex.getZoom(pMapTileIndex)
                    val x = MapTileIndex.getX(pMapTileIndex)
                    val y = MapTileIndex.getY(pMapTileIndex)
                    return "$normalizedHost/$normalizedPath/$RAIN_TILE_SIZE/$zoom/$x/$y/$colorScheme/$DEFAULT_RAIN_OPTIONS.png"
                }
            }
    val tileProvider =
            MapTileProviderBasic(appContext, tileSource).apply { setUseDataConnection(true) }
    val redrawHandler =
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    mapView.postInvalidateOnAnimation()
                }
            }
    tileProvider.tileRequestCompleteHandlers.add(redrawHandler)
    return TilesOverlay(tileProvider, appContext).apply {
        setUseDataConnection(true)
        loadingBackgroundColor = AndroidColor.TRANSPARENT
        loadingLineColor = AndroidColor.TRANSPARENT
    }
}

private const val RAIN_TILE_SIZE = 256
private const val DEFAULT_RAIN_COLOR_SCHEME = 5 // "Ice" palette from RainViewer docs
private const val DEFAULT_RAIN_OPTIONS = "1_0"

@Composable
private fun DailyForecastCard(day: DailyForecast) {
    val containerColor = rideScoreContainerColor(day.rideScore)
    val borderColor = rideScoreStrokeColor(day.rideScore).copy(alpha = 0.5f)

    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = containerColor,
                            contentColor = MaterialTheme.colorScheme.onSurface
                    ),
            border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(start = 20.dp, top = 20.dp, end = 10.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = formatDayLabel(day.date), style = MaterialTheme.typography.titleMedium)
                Text(
                        text = day.conditionDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ForecastDetailRow(
                        icon = Icons.Rounded.DeviceThermostat,
                        label = "Temperature",
                        value = formatTemperatureRange(day.maxTempC, day.minTempC)
                )
                ForecastDetailRow(
                        icon = Icons.Rounded.InvertColors,
                        label = "Rain Chance",
                        value = formatPrecipitation(day.precipitationChance)
                )
                ForecastDetailRow(
                        icon = Icons.Rounded.Air,
                        label = "Wind",
                        value = formatWindDetailed(day.maxWindSpeedKph)
                )
            }
            Box(
                    modifier =
                            Modifier.size(110.dp)
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
            ) { RideScoreBadge(score = day.rideScore, modifier = Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun RideScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val clamped = score.coerceIn(0, 100)
    val accentColor = rideScoreStrokeColor(score)
    val trackColor = Color.Transparent
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    Box(modifier = modifier.padding(6.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
                progress = { clamped / 100f },
                strokeWidth = 8.dp,
                modifier = Modifier.fillMaxSize(),
                color = accentColor,
                trackColor = trackColor
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                    text = "$clamped%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
            )
            Text(
                    text = scoreDescriptor(clamped),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

private fun formatDayLabel(date: LocalDate): String {
    return if (date == LocalDate.now()) {
        "Today"
    } else {
        date.format(dayFormatter)
    }
}

@Composable
private fun ForecastDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = "$label: $value", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTemperatureRange(maxTemp: Double, minTemp: Double): String {
    val maxValue = if (maxTemp.isNaN()) "--" else "${maxTemp.roundToInt()}°"
    val minValue = if (minTemp.isNaN()) "--" else "${minTemp.roundToInt()}°"
    return "$maxValue/$minValue"
}

private fun formatPrecipitation(chance: Int): String = "${chance.coerceIn(0, 100)}%"

private fun formatWindDetailed(value: Double): String {
    if (value.isNaN()) return "-- km/h"
    return String.format(Locale.getDefault(), "%.1f km/h", value)
}

private fun scoreDescriptor(score: Int): String =
        when {
            score >= 85 -> "Perfect"
            score >= 70 -> "Great"
            score >= 55 -> "Good"
            score >= 40 -> "Fair"
            else -> "Poor"
        }

@Composable
private fun rideScoreContainerColor(score: Int): Color {
    val isDark = isSystemInDarkTheme()
    val fraction = score.toFraction()
    val hue = 120f * fraction
    val saturation = 0.75f
    val lightness =
            if (isDark) {
                lerpFloat(0.22f, 0.4f, fraction)
            } else {
                lerpFloat(0.6f, 0.9f, fraction)
            }
    return Color.hsl(hue = hue, saturation = saturation, lightness = lightness)
}

@Composable
private fun rideScoreStrokeColor(score: Int): Color {
    val isDark = isSystemInDarkTheme()
    val fraction = score.toFraction()
    val hue = 120f * fraction
    val saturation = 0.9f
    val lightness =
            if (isDark) {
                lerpFloat(0.4f, 0.65f, fraction)
            } else {
                lerpFloat(0.45f, 0.6f, fraction)
            }
    return Color.hsl(hue = hue, saturation = saturation, lightness = lightness)
}

private fun Int.toFraction(): Float = this.coerceIn(0, 100) / 100f

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
