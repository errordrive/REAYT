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
  - Verifies `onResponseReceivedCommands` continuation token extraction for pagination.
  - Verifies search filter chip `continuationCommand` tokens are strictly ignored in favor of real pagination tokens.

---

## 9. Bug Fix: Restored Real Video Data Loading & Pagination
- **Root Cause 1: InnerTube `FEwhat_to_watch` Empty on Guest Sessions**:
  - YouTube InnerTube `/browse` with `browseId: "FEwhat_to_watch"` intentionally returns 0 videos (`feedNudgeRenderer` "Try searching to get started") for unauthenticated guest web sessions without account watch history.
  - *Fix*: In `YouTubeClient.fetchFeedPaged()` and `fetchBrowseFeed()`, if unauthenticated guest browse returns 0 videos, it automatically queries high-yield long-form video queries (`"viral videos full"`, category-tailored queries).
- **Root Cause 2: Filter Chip Token Collisions in `extractContinuationToken()`**:
  - Search and browse headers contain `chipCloudChipRenderer` elements whose navigation endpoints contain `continuationCommand` reload tokens.
  - The recursive parser was matching the chip filter reload token first rather than the item pagination token, corrupting subsequent pagination requests.
  - *Fix*: Prioritized `onResponseReceivedCommands` / `onResponseReceivedActions` `appendContinuationItemsAction.continuationItems` and `continuationItemRenderer`, while explicitly skipping `chipCloudRenderer`, `searchHeaderRenderer`, `header`, and `chips`.
- **Root Cause 3: Empty Feed Handling in `YouTubeRepository`**:
  - *Fix*: If initial feed loads 0 videos, `YouTubeRepository` immediately invokes `refillFromSeeds()` so the Home screen is never left empty or blank. Infinite scroll smoothly refills when tokens expire.
  - Verifies continuation token extraction.
- **`YouTubeRepositoryTest.kt`**:
  - Verifies ID deduplication logic during feed pagination.

---

## 10. Phase 7: Fullscreen Landscape, Downloads, Channels & UI Polish (Fixes 1–16)
- **FIX 1 — Fullscreen: Forced Landscape Rotation & Immersive Bars**:
  - Added `fullScreenEvents` `SharedFlow<Boolean>` in `PlayerStateHolder`.
  - In `MainActivity.onCreate`, collected `fullScreenEvents` to toggle `ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE` and hide system bars using `WindowInsetsControllerCompat`.
  - Configured `PlayerView.resizeMode` to `RESIZE_MODE_FILL` when fullscreen and `RESIZE_MODE_FIT` when restored.
- **FIX 2 — Bottom Nav Visibility**:
  - Wrapped `YouTubeRootPlayer` in a `Box` with `Modifier.padding(bottom = if (currentVideo != null && !isFullScreen) 56.dp else 0.dp)`.
  - Miniplayer docks flush above bottom nav, keeping bottom nav permanently visible and interactive.
- **FIX 3 — Non-HLS / Direct URL Quality Selector**:
  - Re-implemented `setQuality()` to filter progressive formats by height and bitrate, reloading the player with the chosen stream format while maintaining playback position.
- **FIX 4 — Race Condition Prevention (`resolveGeneration`)**:
  - Guarded asynchronous stream resolution with `AtomicLong(resolveGeneration)` to drop stale in-flight responses.
- **FIX 5 — Miniplayer Gesture Polish**:
  - Guarded upward expansion with `isExpandingFromMini` to eliminate per-frame animation restarts.
  - Implemented rubber-band spring recovery on cancelled horizontal dismissal.
- **FIX 6 — Native Download System**:
  - Added `WRITE_EXTERNAL_STORAGE` permission (API <= 28) in `AndroidManifest.xml`.
  - Added `downloadCurrentVideo()` and `downloadVideo()` via system `DownloadManager`.
  - Wired download actions into `YouTubeRootPlayer`, `YouTubeVideoCard`, and all screens.
- **FIX 7 — Real Share Intent**:
  - Replaced dummy share click with `Intent(Intent.ACTION_SEND)` passing the real video URL.
- **FIX 8 — Channel Screen & Navigation**:
  - Implemented `YouTubeClient.fetchChannelVideos()` fetching browseId channel uploads.
  - Created `ChannelScreen.kt` with horizontal slide transition.
  - Wired channel avatar and title taps from all screens and player.
- **FIX 9 — Collapsible Description in Watch Screen**:
  - Added collapsible description card with "Show more" / "Show less" toggle.
- **FIX 10 — Miniplayer Progress Bar Positioning**:
  - Repositioned miniplayer progress bar to `Alignment.BottomStart` with 2dp height.
- **FIX 11 — Real Comments Sheet**:
  - Extracted `commentsContinuationToken` in `YouTubeClient.fetchNextAndDetails()`.
  - Added comments button and `YouTubeCommentsSheet` invocation in watch screen.
- **FIX 12 — Watch Later Context Menu Wiring**:
  - Connected `onSaveToWatchLater` from `YouTubeVideoCard` across Home, Search, Subscriptions, and You.
- **FIX 13 — Parallel Channel Uploads Subscription Feed**:
  - Upgraded `SubscriptionsViewModel.loadSubscriptionFeed()` to fetch channel uploads in parallel using `async`/`awaitAll`.
- **FIX 14 — Autoplay Up-Next Video Waiting Loop**:
  - Autoplay waits up to 5 seconds for related videos to load before dispatching next video.
- **FIX 15 — Scope Leak Prevention**:
  - Added `scope.cancel()` in `PlayerStateHolder.release()`.
- **FIX 16 — Tightened MediaSession Security**:
  - Restricted `PlaybackService.onGetSession()` to self, system UI, and trusted controllers.
- **Additional Polish**:
  - Double-tap seek: Left third rewinds 10s, right third fast-forwards 10s.
  - Controls playback speed cycling: [0.5×, 0.75×, 1×, 1.25×, 1.5×, 2×].
  - Likes count label next to Like button.
  - Dedicated "Save" and "Download" pill buttons in watch screen action row.

---

## 9. Phase 4: Critical Miniplayer, Download & UI Bug Fixes
- **Miniplayer Blank Video Surface Fix**:
  - Root cause: `YouTubeRootPlayer` overlay previously drew an opaque `MaterialTheme.colorScheme.surface` background spanning the full width `0..screenWidth`, occluding the playing video surface on the left.
  - Fixed by padding the surface background start by `miniThumbWidthPx` (`~99dp`). The playing video thumbnail is completely unobstructed and renders continuously.
- **Bottom Navigation Bar Overlap & Touch Interception Fix**:
  - Root cause: Outer Box previously applied bottom padding unconditionally, and the translating Column had `fillMaxSize()` with background, extending down and covering the bottom navigation bar. Also, root gesture detectors intercepted full-screen touches.
  - Fixed by constraining the Translating Container height strictly to `56.dp` when in mini mode, applying mini drag/tap gestures ONLY to the 56dp mini bar, and passing `bottomNavHeightDp = 56`. The bottom nav bar and background feed remain 100% visible, touchable, and scrollable.
- **Immediate Auto-Expansion on New Video Selection**:
  - Root cause: Selecting a video while in mini mode or after dismissal did not expand the player because `playerCommands` had `replay = 0` and `miniPlayerProgress` stayed at 1f.
  - Fixed by setting `replay = 1` on `_playerCommands`, snapping `miniPlayerProgress` to `0f` immediately in `playVideo()`, and adding `LaunchedEffect(currentVideo?.id)` inside `YouTubeRootPlayer` to animate to expanded mode on every new video.
- **Raw JSON String Leakage Fix**:
  - Root cause: InnerTube `viewCount` object extraction previously read the JSONObject as a string, leaking `{"simpleText":"2.5M views"}`.
  - Implemented `cleanDisplayString()` in `YouTubeClient.kt` and applied it across `YouTubeRootPlayer`, `YouTubeVideoCard`, and models. Strips all JSON brackets, quotes, and colons.
- **Direct Download & Notification Progress Fix**:
  - Injected YouTube CDN required headers (`User-Agent`, `Origin`, `Referer`) into `DownloadManager.Request`.
  - Added direct MP4 format resolution (preventing broken HLS `.m3u8` download attempts).
  - Target directory set to `Environment.DIRECTORY_DOWNLOADS`.
  - Added Android 13+ (API 33+) runtime `POST_NOTIFICATIONS` permission prompt in `MainActivity`.
- **Dynamic, Region & Interest-Based Dynamic Home Feed**:
  - Configured InnerTube client contexts (`WEB`, `VISIONOS`, `ANDROID_VR`) with device `Locale.getDefault().language` (`hl`) and `Locale.getDefault().country` (`gl`).
  - Upgraded `YouTubeRepository.refillFromSeeds()` and `YouTubeClient.fetchBrowseFeed()` to derive seeds from user's actual watch history and subscriptions.
  - Falls back to dynamically rotating trending queries for the user's locale rather than static `"viral videos full"`.
- **Fullscreen Button Relocated to Player Bottom**:
  - Moved fullscreen button from the top player bar to the bottom right of the player controls row, adjacent to the seekbar and time elapsed/total indicators.
- **Horizontal Player Aspect Ratio Preservation**:
  - Enforced `AspectRatioFrameLayout.RESIZE_MODE_FIT` in `PlayerView` across all orientations, eliminating video stretching/distortion on landscape screens.
- **Complete Removal of Comments**:
  - Removed comments preview card, action buttons, and sheet triggers from `YouTubeRootPlayer` per user instruction.

---

## 10. Phase 5: Vanced Signature Features Implementation (Reverse Engineered from Vanced.apk)
- **Vanced APK Reverse Engineering**:
  - Inspected `/storage/emulated/0/ApkLab/REAYT/Vanced.apk` using `rea` (`decode-android-resources`) and multi-DEX structural analysis.
  - Extracted architectural blueprints for gesture controls (`io.awesome.gagtube.player.gesture`), playqueue repeat states (`io.awesome.gagtube.player.playqueue`), and watch view actions (`io.awesome.gagtube.player.ui`).
- **Swipe-to-Adjust Brightness & Volume Gestures**:
  - Vertical swipe on the left half of the video surface adjusts screen brightness smoothly via `Window.attributes.screenBrightness`.
  - Vertical swipe on the right half of the video surface adjusts system media volume via `AudioManager.setStreamVolume`.
  - Double-tap on the left/right thirds continues to perform 10s seek rewind and fast-forward.
- **Vanced Gesture HUD Overlay**:
  - Added a center HUD overlay displaying high-contrast icons (☀️ for brightness, 🔊 for volume), percentage text (e.g. `85%`), and a smooth animated progress indicator.
  - Automatically fades out 1.5 seconds after gesture release.
- **Watch Screen Vanced Action Bar**:
  - Added horizontal scrollable pill action bar below the video player with:
    - **Like**: Interactive button with view/like count toggle.
    - **Share**: Launches native Android share intent (`Intent.ACTION_SEND`) with video title and YouTube URL.
    - **Download**: Enqueues system `DownloadManager` download with valid CDN headers and notification.
    - **Repeat**: Toggles between Off, Repeat One, and Repeat All with dynamic icon and indicator badge.
    - **Popup (PiP)**: Triggers native Android Picture-in-Picture mode (`enterPictureInPictureMode`).
    - **Save**: Saves video to Watch Later in Room database.
- **Player Top Controls Bar**:
  - Integrated Repeat mode toggle button and PiP/Popup button into top player control row alongside Quality, Speed, Audio-Only, and Download.
- **Repeat Modes Engine**:
  - Supported `REPEAT_MODE_OFF`, `REPEAT_MODE_ONE`, and `REPEAT_MODE_ALL` in `PlayerStateHolder`.
  - Seamlessly syncs with ExoPlayer's `Player.Listener.onRepeatModeChanged`.
  - Immediately loops playback to position 0 when `STATE_ENDED` is triggered under `REPEAT_MODE_ONE`.
  - Added Repeat Mode selection row to `SettingsScreen`.
- **Autoplay Setting**:
  - Fully wired `isAutoplayEnabled` toggle in `SettingsScreen` to `PlayerStateHolder.toggleAutoplay()`.
