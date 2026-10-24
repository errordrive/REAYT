# CHANGES.md - Comprehensive Upgrade & Refactoring Log

## Overview
Transformed the application from an initial prototype with static singletons, missing wrapper files, and hardcoded data into a production-grade Android video streaming client with a YouTube-identical drag-to-minimise mini-player, Room database persistence, Koin dependency injection, robust Media3/ExoPlayer integration, infinite InnerTube browse feed, and resilient networking.

---

## 1. Ground Rules & Build Infrastructure
- **Wrapper & Version Catalog**:
  - Maintained `gradle/libs.versions.toml` version catalog defining stable dependencies for AGP (8.5.2), Kotlin (2.0.0), Compose BOM (2024.06.00), Media3 (1.3.1), OkHttp (4.12.0), Coil (2.7.0), Room (2.6.1), WorkManager (2.9.1), and Koin (3.5.6).
  - Maintained `gradle/wrapper/gradle-wrapper.jar` for Gradle 8.7.
  - Target and compile SDK configured to stable API 34.
  - Pure Compose `ImageVector` definitions in `AppIcons.kt` to eliminate bulky dependencies.
- **Theming**:
  - Implemented both Dark and Light color palettes in `Color.kt` and `Theme.kt`, automatically responding to system dark/light mode (`isSystemInDarkTheme()`).

---

## 2. Product Rules & Constraints Verification
- **NO Shorts and NO "+"/Create Button**:
  - Removed `<data android:pathPrefix="/shorts" />` deep-link from `AndroidManifest.xml`.
  - Removed `isShort` property from `YouTubeVideo` model.
  - Strict Shorts filter in `YouTubeClient.kt`: skips `reelShelfRenderer`, `reelItemRenderer`, `shortsLockupViewModel`, items with `reelWatchEndpoint`, `LOCKUP_CONTENT_TYPE_SHORTS`, and endpoints containing `/shorts/`. Never parses or shows a Shorts shelf.
  - Bottom bar strictly remains: Home, Subscriptions, You.
- **NO Cast UI**:
  - Removed all Cast buttons, Cast icons, and Cast imports.
- **NO Fake or Hardcoded Data**:
  - Stripped all fallback titles ("YouTube Video") and invented counts. If data is unavailable, elements are hidden.

---

## 3. Phase 1: Crash on Play & Scope Architecture Fixes
- **Root Cause Resolved**:
  - `PlayerStateHolder.playVideo()` previously invoked `miniPlayerProgress.animateTo(...)` inside a plain CoroutineScope without a Compose `MonotonicFrameClock`, throwing `IllegalStateException`.
  - Defined `sealed interface PlayerCommand { data object Expand; data object Minimise; data object Dismiss }`.
  - `PlayerStateHolder` now emits commands via `SharedFlow<PlayerCommand>`, collected by `LaunchedEffect` in `YouTubeRootPlayer` where the Compose frame clock is present.
  - Only `snapTo` is invoked outside composition.
- **Coroutine Safety**:
  - Installed `CoroutineExceptionHandler` on `PlayerStateHolder` and `YouTubeRepository` long-lived scopes.
  - Rethrows `CancellationException` in all catch blocks.
- **Pending Controller Connection Queue**:
  - If `playVideo()` is called before `MediaController` is bound, requests are queued in `pendingVideo` and immediately dispatched upon connection. Never silently drops playback.

---

## 4. Phase 2: Playback Correctness & MergingMediaSource
- **MergingMediaSource for Adaptive Streams**:
  - When HLS and progressive muxed streams are absent, `PlaybackService` inspects the stream URL and builds a synchronized `MergingMediaSource(true, videoSource, audioSource)`. Never plays video-only; never merges muxed with separate audio.
- **Per-Stream Client Headers & User-Agent**:
  - Encoded client name and user-agent into stream URI parameters (`__yt_client`, `__yt_ua`).
  - `ResolvingDataSource` extracts and applies stream-specific User-Agents (VisionOS vs Android VR vs Web) and required origin/referer headers per request.
- **MIME Type Sanitization**:
  - Stripped codec parameters from MIME type strings before passing to `MediaItem.Builder().setMimeType()`.
- **Quality Track Selection**:
  - Derived available quality list without duplicates ("Auto", "1080p", "720p", "480p", etc.) and applied via `TrackSelectionParameters.Builder().setMaxVideoSize(...)` without re-preparing the player.

---

## 5. Phase 3: Rewritten Swipe-to-Minimise Gesture & Player Surface
- **Stable Pointer Tracking**:
  - Moved `pointerInput` outside the translating `graphicsLayer` to eliminate relative coordinate distortion.
  - Smooth vertical interpolation: video surface smoothly scales from 16:9 full-width watch size down to exact 100x56dp docked thumbnail on the left in mini mode.
  - Mini bar: 56dp height docked above bottom nav, 100dp 16:9 real `PlayerView` surface on left, title and controls on right, 2dp red progress bar on top.
  - Gesture actions: tap to expand, drag up to expand, horizontal swipe to dismiss.
- **BackHandler Priority**:
  - Fullscreen exits fullscreen -> Search/Settings close -> expanded watch minimises -> mini mode disables BackHandler to allow normal tab back navigation without dismissing the video.

---

## 6. Phase 4: Unlimited YouTube-Style Infinite Feed
- **InnerTube `/browse` (`FEwhat_to_watch`)**:
  - Implemented `fetchBrowseFeed()` hitting InnerTube `/browse` with `browseId: "FEwhat_to_watch"` and WEB client context.
  - Continuation parser extracts pagination tokens from `appendContinuationItemsAction`, `richGridRenderer`, and `sectionListContinuation`.
  - Supports modern `lockupViewModel`, `videoRenderer`, and `compactVideoRenderer`.
- **Continuous Prefetching**:
  - Migrated `HomeScreen.kt` and `SearchScreen.kt` to `snapshotFlow` with `distinctUntilChanged()`, triggering pagination 7 items before the end of the list.
- **Infinite Refill Mechanism**:
  - When YouTube browse continuation tokens are exhausted or empty, `YouTubeRepository` seeds queries from local watch history, liked videos, and subscriptions, deduplicating IDs to provide infinite scrolling.

---

## 7. Phase 5: Real Metadata & Navigation Screens
- **Real Metadata Extraction**:
  - Real likes count extracted from `segmentedLikeDislikeButtonRenderer`.
  - Real comments token and continuation parsing.
- **Real `SettingsScreen.kt`**:
  - Created full Settings screen with Default Quality selection, Autoplay toggle, Audio-Only mode toggle, Watch History clearing with confirmation dialog, and Application info.
  - Wired into `MainActivity.kt` with horizontal slide transition.
- **Interactive You Screen**:
  - Wired "Liked videos", "Watch Later", and "Downloads" into interactive bottom sheets with direct video playback on tap.

---

## 8. Phase 6: Unit and Regression Tests
- **`PlayerRegressionTest.kt`**:
  - Verifies `PlayerCommand` dispatching outside composition without `MonotonicFrameClock`.
  - Verifies `YouTubeVideo` model without Shorts properties.
- **`YouTubeClientParserTest.kt`**:
  - Verifies rejection of `reelShelfRenderer`, `reelItemRenderer`, `shortsLockupViewModel`, and `/shorts/`.
  - Verifies `lockupViewModel` parsing and `compactVideoRenderer` parsing.
  - Verifies continuation token extraction.
- **`YouTubeRepositoryTest.kt`**:
  - Verifies ID deduplication logic during feed pagination.
