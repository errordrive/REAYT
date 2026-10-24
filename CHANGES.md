# CHANGES.md - Comprehensive Upgrade & Refactoring Log

## Overview
Transformed the application from an initial prototype with static singletons, missing wrapper files, and hardcoded data into a production-grade Android video streaming client with a YouTube-identical drag-to-minimise mini-player, Room database persistence, Koin dependency injection, robust Media3/ExoPlayer integration, and resilient networking.

---

## 1. Ground Rules & Build Infrastructure
- **Wrapper & Version Catalog**:
  - Created `gradle/libs.versions.toml` version catalog defining stable dependencies for AGP (8.5.2), Kotlin (2.0.0), Compose BOM (2024.06.00), Media3 (1.3.1), OkHttp (4.12.0), Coil (2.7.0), Room (2.6.1), WorkManager (2.9.1), and Koin (3.5.6).
  - Restored `gradle/wrapper/gradle-wrapper.jar` for Gradle 8.7.
  - Updated root `build.gradle.kts` and `app/build.gradle.kts` to adopt version catalog aliases.
  - Set `compileSdk = 34` and `targetSdk = 34`.
  - Configured R8 minification and resource shrinking for release builds in `app/build.gradle.kts`.
  - Added keep rules to `app/proguard-rules.pro`.
- **Theming**:
  - Implemented both Dark and Light color palettes in `Color.kt` and `Theme.kt`, automatically responding to system dark/light mode (`isSystemInDarkTheme()`).

---

## 2. Phase 1: Crash and Stability Fixes
- **`app/src/main/AndroidManifest.xml`**:
  - Configured `configChanges` to include `orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize|uiMode|density|fontScale|layoutDirection|locale`.
  - Added intent filters for YouTube web deep links (`/watch`, `/shorts`, `/channel`, `/@`) without `autoVerify`.
  - Added `ACTION_SEND` intent filter for incoming text shares.
  - Configured `PlaybackService` with `foregroundServiceType="mediaPlayback"`.
- **`app/src/main/java/com/reayt/youtube/MainActivity.kt`**:
  - Removed `startForegroundService()` from `MainActivity`. MediaController binding starts `PlaybackService` lazily on demand.
  - Removed early `return` in `MainAppShell` before `remember` calls.
  - Adopted `rememberSaveable` for tab, search, and settings state to preserve state across PiP and configuration changes.
  - Deferred notification permission request away from initial launch.
- **`app/src/main/java/com/reayt/youtube/data/NetworkModule.kt`**:
  - Added DoH bootstrap disk cache (5MB).
  - Implemented fallback DNS: system DNS queried first, falling back to DoH (AdGuard) only if system resolution fails.
  - Added `Call.await()` coroutine extension binding OkHttp calls to coroutine cancellation.
- **`app/src/main/java/com/reayt/youtube/data/YouTubeClient.kt`**:
  - Replaced swallowed errors with sealed `ResultState<T>` (`Success`, `NetworkError`, `ParseError`, `Blocked`).
  - Implemented multi-client stream resolution with fallback: VisionOS -> Android VR -> refreshed visitor token retry.
  - Stored per-client user-agent and request headers inside `YouTubeStreamInfo`.
  - Formatted all time and query strings with `Locale.ROOT`.
- **`app/src/main/java/com/reayt/youtube/data/YouTubeRepository.kt`**:
  - Resolved duplicate-key `LazyColumn` crashes by canceling active jobs, tagging loads with atomic `generationId`, discarding stale generation emissions, and guarding list updates with `Mutex`.
  - Implemented thread-safe item deduplication by video ID.
  - Cached last feed on disk for instantaneous cold-start rendering.
- **`app/src/main/java/com/reayt/youtube/service/PlaybackService.kt`**:
  - Configured `setWakeMode(C.WAKE_MODE_NETWORK)`.
  - Handled `onTaskRemoved` to stop service when playback is not active.
  - Validated caller packages in `onGetSession`.
  - Integrated `ResolvingDataSource` to inject required headers and User-Agents for stream playback.

---

## 3. Phase 2: Persistence and Real Data
- **`app/src/main/java/com/reayt/youtube/data/db/AppDatabase.kt`**:
  - Built Room database (`AppDatabase`) with entities and DAOs for `watch_history` (with last played position), `liked_videos`, `watch_later`, `subscriptions`, `search_history`, `downloads`, and `app_settings`.
- **`app/src/main/java/com/reayt/youtube/data/UserDataRepository.kt`**:
  - Exposed reactive `Flow`s for user data with reactive `isLiked`, `isInWatchLater`, and `isSubscribed` states.
- **Fake Data Removal**:
  - Stripped all hardcoded sample data (fake subscriber counts, fake like counts, fake comments, verified badges on every channel, always-on notification dot).
  - Deleted `getSampleComments` and replaced with live continuation comments parser in `YouTubeClient.kt`.
- **`app/src/main/java/com/reayt/youtube/ui/components/YouTubeTopBar.kt`**:
  - Deleted Cast icon button (`onCastClick`) and Cast imports.
  - Removed hardcoded unread notification red dot.

---

## 4. Phase 3: Swipe-to-Minimise Mini-Player & Video Engine
- **`app/src/main/java/com/reayt/youtube/player/PlayerStateHolder.kt`**:
  - Replaced global `VideoPlaybackManager` singleton with process-wide, service-backed `PlayerStateHolder` injected via Koin.
  - Controlled by single `Animatable<Float>` `miniPlayerProgress` (0f = expanded, 1f = mini).
  - Clears `relatedVideos` immediately upon starting a new video.
  - Intercepts player errors (`BEHIND_LIVE_WINDOW` re-prepare; HTTP 403/410 re-resolves up to 3 times before displaying error overlay).
- **`app/src/main/java/com/reayt/youtube/ui/components/YouTubeRootPlayer.kt`**:
  - Hosts exactly **ONE** `PlayerView` at the application root, shared between expanded watch layout and mini-player dock.
  - Implemented vertical drag gesture on video surface to drag smoothly between expanded and mini modes.
  - Added velocity and threshold detection (progress > 0.35f or fling down animates to mini with spring; progress < 0.35f animates back to expanded).
  - Mini mode features: tap to expand, drag up to expand, horizontal swipe to dismiss, top red progress line.
  - Safely sets `playerView.player = null` in `onRelease`.
  - Intercepts back navigation: fullscreen -> normal, expanded -> mini, mini -> exit.

---

## 5. Phase 6 & 7: UI/UX Polish, Cleanup & Tests
- **Unused Resource Deletion**:
  - Removed extracted `exo_*` and `media3_*` duplicate drawables.
  - Removed `silent.mp3`, `automotive_app_desc.xml`, and `rapid_response_client_defaults.xml`.
  - Removed Spotify leftovers (`Theme.Sp0Plus` and `#1DB954` color).
- **`app/src/main/java/com/reayt/youtube/ui/screens/SearchScreen.kt`**:
  - Fixed TextField clipping by using `BasicTextField` with proper content and IME padding.
  - Added pointer input consumption to block touch click-through to content beneath the search overlay.
- **Unit Tests**:
  - Added `YouTubeClientParserTest.kt` verifying parser and result sealed class states.
  - Added `YouTubeRepositoryTest.kt` verifying list deduplication logic.

---

## Decisions Made
- **Dependency Injection**: Selected Koin 3.5.6 for lightweight, annotation-processor-free, Kotlin 2.0-native DI and seamless ViewModel injection in Jetpack Compose.
- **Single Authoritative Player Surface**: Consolidated all video rendering to `YouTubeRootPlayer` to eliminate video flickering, state desynchronization, and memory leaks.
