# Best Bike Day

Best Bike Day is an Android app that helps cyclists decide whether it is a good day to ride by combining a 7-day weather forecast, bike-friendly ride scoring, and a live rain radar. The experience is entirely Jetpack Compose-based, location-aware, and optimized for quick at-a-glance decisions before you head out.

## Highlights
- **Ride Readiness at a Glance** – Daily cards surface temperature, precipitation probability, wind, and a calculated ride score.
- **Live Rain Radar** – Uses RainViewer tiles on top of an OSMDroid map so you can see precipitation bands moving near you.
- **Location-Aware Forecasts** – Fused Location Provider fetches the most relevant Open-Meteo forecast for the rider’s current spot.
- **Material 3 + Edge-to-Edge UI** – Modern design system with Compose, adaptive gradients, and focus on readability outdoors.
- **Offline-Friendly Defaults** – Forecast data is cached in memory during a session and avoids redundant network calls when coordinates do not change.

## Tech Stack
- **Language:** Kotlin (JVM target 11)
- **UI:** Jetpack Compose, Material 3, edge-to-edge `ComponentActivity`
- **Architecture:** ViewModel + StateFlow + Compose state hoisting
- **Networking:** Retrofit + Kotlinx Serialization + OkHttp logging
- **Location:** Google Play Services Fused Location Provider
- **Maps & Radar:** OSMDroid `MapView` with RainViewer tile overlays
- **Weather Data:** [Open-Meteo API](https://open-meteo.com/)

## Getting Started
### Prerequisites
- Android Studio Koala (or newer) with bundled JDK 17
- Android SDK Platform 36, Build Tools 36, and Google Play Services packages
- An emulator or device running Android 8.0 (API 26) or newer

### Initial Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-org>/BestBikeDay.git
   cd BestBikeDay
   ```
2. Open the project in Android Studio and let Gradle sync, or run the sync manually:
   ```bash
   ./gradlew tasks
   ```

### Build & Run
- **Debug build (preferred while iterating):**
  ```bash
  ./gradlew :app:assembleDebug
  ```
- **Install on a connected device/emulator:**
  ```bash
  ./gradlew :app:installDebug
  ```
- Alternatively, use Android Studio’s *Run* action to deploy directly to the selected device.

### Testing
- **Unit tests:** `./gradlew test`
- **Instrumented Compose/UI tests:** `./gradlew connectedAndroidTest`
  (Requires a running emulator or device.)

## Project Structure
```
BestBikeDay/
├─ app/
│  ├─ src/main/java/com/sunwings/bestbikeday/
│  │  ├─ MainActivity.kt          # Entry point, sets WeatherRoute()
│  │  ├─ ui/weather/              # Compose UI, state, and OSMDroid radar host
│  │  ├─ data/                    # Repositories, remote APIs, models
│  │  └─ location/                # Location helpers & permissions
│  ├─ build.gradle.kts            # Module-level build & dependencies
├─ build.gradle.kts               # Shared Gradle config
├─ gradle/libs.versions.toml      # Version catalog
└─ settings.gradle.kts            # Project definition
```

## Permissions & Data Sources
- **Location:** The app requests `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` to personalize the forecast and radar location. Provide mock or manual coordinates if you prefer not to grant access.
- **Networking:** All weather data comes from Open-Meteo, and radar tiles come from RainViewer. Both services are free for non-commercial usage but review their terms if you plan to scale usage.

## Troubleshooting
- **Radar tiles don’t show immediately:** Ensure the device has network access; OSMDroid caches tiles per timestamp, so toggling the radar refresh will fetch the latest imagery.
- **Location failures:** Check that Google Play Services is installed and that the emulator has a set location. The app displays an inline error with a retry action when the fused provider cannot resolve a fix.
- **Gradle sync issues:** Verify that you’re using the matching Android Gradle Plugin version defined in `gradle/libs.versions.toml` and that the Google & Maven Central repositories are reachable.

## Contributing
1. Fork the repository and create a topic branch.
2. Make your changes with clear commit messages.
3. Run `./gradlew lint test` (or at least `test`) before pushing.
4. Submit a pull request describing the motivation and testing strategy.

## License
Choose the license that matches your distribution goals (MIT, Apache 2.0, etc.) and update this section before publishing.

## Credits
Built end-to-end with GitHub Copilot (GPT-5.1-Codex) supporting the implementation, alongside an original Best Bike Day concept.
Original idea by Creator Magic on YouTube https://www.youtube.com/watch?v=FbCA_qQSvYM. I just made my own, fine tuned the algorithm and added
a useful weather radar.
