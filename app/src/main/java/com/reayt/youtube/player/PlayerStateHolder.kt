package com.reayt.youtube.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.model.ResultState
import com.reayt.youtube.model.YouTubeStreamInfo
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.service.PlaybackService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

sealed interface PlayerCommand {
    data object Expand : PlayerCommand
    data object Minimise : PlayerCommand
    data object Dismiss : PlayerCommand
}

/**
 * Process-wide, service-backed playback state holder.
 * UI components observe this state holder and communicate solely via MediaController.
 */
class PlayerStateHolder(private val context: Context) {

    private val tag = "PlayerStateHolder"
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(tag, "Uncaught coroutine exception: ${throwable.message}", throwable)
        if (throwable !is CancellationException) {
            _streamError.value = throwable.localizedMessage ?: "An unexpected error occurred"
            _isLoadingStream.value = false
        }
    }
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob() + exceptionHandler)
    private val resolveGeneration = AtomicLong(0)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingVideo: YouTubeVideo? = null
    private var pendingStartPositionMs: Long = 0L

    private val _playerCommands = MutableSharedFlow<PlayerCommand>(replay = 1, extraBufferCapacity = 64)
    val playerCommands: SharedFlow<PlayerCommand> = _playerCommands.asSharedFlow()

    private val _fullScreenEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    val fullScreenEvents: SharedFlow<Boolean> = _fullScreenEvents.asSharedFlow()

    private val _mediaController = MutableStateFlow<Player?>(null)
    val mediaController: StateFlow<Player?> = _mediaController.asStateFlow()

    private val _currentVideo = MutableStateFlow<YouTubeVideo?>(null)
    val currentVideo: StateFlow<YouTubeVideo?> = _currentVideo.asStateFlow()

    private val _streamInfo = MutableStateFlow<YouTubeStreamInfo?>(null)
    val streamInfo: StateFlow<YouTubeStreamInfo?> = _streamInfo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isLoadingStream = MutableStateFlow(false)
    val isLoadingStream: StateFlow<Boolean> = _isLoadingStream.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _selectedQuality = MutableStateFlow("Auto")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    val availableQualities: StateFlow<List<String>> = _streamInfo.map { info ->
        if (info == null) listOf("Auto")
        else {
            val heights = info.formats.filter { it.isVideo && it.height > 0 }
                .map { "${it.height}p" }
                .distinct()
                .sortedByDescending { it.removeSuffix("p").toIntOrNull() ?: 0 }
            if (heights.isEmpty()) listOf("Auto") else listOf("Auto") + heights
        }
    }.stateIn(scope, SharingStarted.Eagerly, listOf("Auto"))

    private val _isAudioOnly = MutableStateFlow(false)
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    private val _relatedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val relatedVideos: StateFlow<List<YouTubeVideo>> = _relatedVideos.asStateFlow()

    private val _isInPictureInPicture = MutableStateFlow(false)
    val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture.asStateFlow()

    // 0f = fully expanded watch screen, 1f = mini player dock
    val miniPlayerProgress = Animatable(0f)

    private var progressTrackingJob: Job? = null
    private var resolveJob: Job? = null
    private var retryCount = 0

    init {
        bindMediaController()
        startTracking()
    }

    fun bindMediaController() {
        if (_mediaController.value != null) return
        try {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future

            future.addListener({
                try {
                    val controller = future.get()
                    _mediaController.value = controller

                    pendingVideo?.let { queuedVideo ->
                        val pos = pendingStartPositionMs
                        pendingVideo = null
                        playVideo(queuedVideo, pos)
                    }

                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                            _durationMs.value = controller.duration.coerceAtLeast(0L)
                            _positionMs.value = controller.currentPosition.coerceAtLeast(0L)
                            _bufferedPositionMs.value = controller.bufferedPosition.coerceAtLeast(0L)

                            if (playbackState == Player.STATE_ENDED && _isAutoplayEnabled.value) {
                                scope.launch {
                                    // Wait up to 5 seconds for related videos to load
                                    var waited = 0
                                    while (_relatedVideos.value.isEmpty() && waited < 10) {
                                        delay(500)
                                        waited++
                                    }
                                    val nextVideo = _relatedVideos.value.firstOrNull()
                                    if (nextVideo != null && nextVideo.id != _currentVideo.value?.id) {
                                        playVideo(nextVideo)
                                    }
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            handlePlaybackError(error)
                        }
                    })
                } catch (e: Exception) {
                    Log.w(tag, "MediaController connection error: ${e.message}")
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.w(tag, "Failed to build SessionToken: ${e.message}")
        }
    }

    private fun startTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                val controller = _mediaController.value
                if (controller != null && controller.isPlaying) {
                    _positionMs.value = controller.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = controller.duration.coerceAtLeast(0L)
                    _bufferedPositionMs.value = controller.bufferedPosition.coerceAtLeast(0L)
                }
                delay(400)
            }
        }
    }

    /**
     * Initiates playback for a selected video.
     */
    fun playVideo(video: YouTubeVideo, startPositionMs: Long = 0L) {
        val gen = resolveGeneration.incrementAndGet()
        // Requirement: Clear relatedVideos at the start of playVideo
        _relatedVideos.value = emptyList()
        _currentVideo.value = video
        _streamError.value = null
        _isLoadingStream.value = true
        retryCount = 0

        // Auto-expand player immediately to main video player
        scope.launch {
            miniPlayerProgress.snapTo(0f)
        }
        _playerCommands.tryEmit(PlayerCommand.Expand)

        if (_mediaController.value == null) {
            pendingVideo = video
            pendingStartPositionMs = startPositionMs
            bindMediaController()
        }

        resolveJob?.cancel()
        resolveJob = scope.launch {
            try {
                // Concurrently fetch video details / up-next and stream info
                val detailsDeferred = launch(Dispatchers.IO) {
                    try {
                        val detailsResult = YouTubeClient.fetchNextAndDetails(video.id)
                        if (detailsResult is ResultState.Success) {
                            val (enrichedVideo, related) = detailsResult.data
                            withContext(Dispatchers.Main) {
                                if (gen == resolveGeneration.get()) {
                                    _currentVideo.value = enrichedVideo
                                    _relatedVideos.value = related
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(tag, "Failed fetching details: ${e.message}")
                    }
                }

                resolveAndStartStream(video, startPositionMs, gen)
                detailsDeferred.join()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isLoadingStream.value = false
                _streamError.value = e.localizedMessage ?: "Failed loading stream"
            }
        }
    }

    private suspend fun resolveAndStartStream(video: YouTubeVideo, startPositionMs: Long, gen: Long) {
        val streamResult = YouTubeClient.resolveVideoStream(video.id)
        withContext(Dispatchers.Main) {
            if (gen != resolveGeneration.get()) return@withContext
            when (streamResult) {
                is ResultState.Success -> {
                    val info = streamResult.data
                    _streamInfo.value = info
                    _isLoadingStream.value = false
                    _streamError.value = null
                    loadMediaItemIntoController(video, info, startPositionMs, gen)
                }
                is ResultState.NetworkError -> {
                    _isLoadingStream.value = false
                    _streamError.value = streamResult.message
                }
                is ResultState.ParseError -> {
                    _isLoadingStream.value = false
                    _streamError.value = streamResult.message
                }
                is ResultState.Blocked -> {
                    _isLoadingStream.value = false
                    _streamError.value = streamResult.message
                }
            }
        }
    }

    private fun cleanMimeType(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        return mimeType.substringBefore(";").trim()
    }

    private fun loadMediaItemIntoController(
        video: YouTubeVideo,
        info: YouTubeStreamInfo,
        startPositionMs: Long,
        gen: Long
    ) {
        if (_currentVideo.value?.id != video.id || gen != resolveGeneration.get()) return

        val controller = _mediaController.value
        if (controller == null) {
            pendingVideo = video
            pendingStartPositionMs = startPositionMs
            return
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setArtist(video.channelTitle)
            .setArtworkUri(Uri.parse(video.thumbnailUrl))
            .build()

        val rawUri = info.hlsUrl ?: info.directVideoUrl ?: return
        val rawMime = if (info.hlsUrl != null) MimeTypes.APPLICATION_M3U8 else info.videoMimeType
        val cleanMime = cleanMimeType(rawMime)

        // Build stream URI with embedded tags for ResolvingDataSource and separate audio merging
        val uriBuilder = Uri.parse(rawUri).buildUpon()
            .appendQueryParameter("__yt_client", info.clientName)
        if (info.userAgent.isNotBlank()) {
            uriBuilder.appendQueryParameter("__yt_ua", info.userAgent)
        }
        if (info.hlsUrl == null && info.directVideoUrl != null && !info.audioUrl.isNullOrBlank()) {
            uriBuilder.appendQueryParameter("__yt_audio_url", info.audioUrl)
        }
        val mediaUri = uriBuilder.build()

        val mediaItem = MediaItem.Builder()
            .setUri(mediaUri)
            .apply {
                if (!cleanMime.isNullOrBlank()) {
                    setMimeType(cleanMime)
                }
            }
            .setMediaMetadata(metadata)
            .build()

        controller.stop()
        controller.clearMediaItems()
        controller.setMediaItem(mediaItem, startPositionMs)
        controller.playbackParameters = PlaybackParameters(_playbackSpeed.value)
        controller.prepare()
        controller.play()
    }

    fun handlePlaybackError(error: PlaybackException) {
        Log.e(tag, "Playback error: ${error.errorCodeName} - ${error.message}")
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            val controller = _mediaController.value ?: return
            controller.seekToDefaultPosition()
            controller.prepare()
            return
        }

        if (retryCount < 3) {
            retryCount++
            val current = _currentVideo.value ?: return
            val pos = _positionMs.value
            _isLoadingStream.value = true
            val currentGen = resolveGeneration.get()
            scope.launch {
                delay(800)
                resolveAndStartStream(current, pos, currentGen)
            }
        } else {
            _isLoadingStream.value = false
            _streamError.value = "Playback error: ${error.localizedMessage ?: "Source unavailable"}. Tap Retry to reload."
        }
    }

    fun retryPlayback() {
        val current = _currentVideo.value ?: return
        playVideo(current, _positionMs.value)
    }

    fun playPause() {
        val controller = _mediaController.value ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = _mediaController.value ?: return
        controller.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs
    }

    fun seekRelative(offsetMs: Long) {
        val controller = _mediaController.value ?: return
        val target = (controller.currentPosition + offsetMs).coerceIn(0L, controller.duration.coerceAtLeast(0L))
        controller.seekTo(target)
        _positionMs.value = target
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        _mediaController.value?.playbackParameters = PlaybackParameters(speed)
    }

    fun setQuality(quality: String) {
        _selectedQuality.value = quality
        val info = _streamInfo.value ?: return
        val controller = _mediaController.value ?: return
        val video = _currentVideo.value ?: return
        val savedPos = _positionMs.value

        if (info.hlsUrl != null) {
            // HLS: use track selection
            val builder = controller.trackSelectionParameters.buildUpon()
            when {
                quality.contains("1080") -> builder.setMaxVideoSize(1920, 1080)
                quality.contains("720")  -> builder.setMaxVideoSize(1280, 720)
                quality.contains("480")  -> builder.setMaxVideoSize(854, 480)
                quality.contains("360")  -> builder.setMaxVideoSize(640, 360)
                else -> builder.clearVideoSizeConstraints()
            }
            controller.trackSelectionParameters = builder.build()
        } else {
            // Direct URL: pick specific format and reload
            val targetHeight = quality.removeSuffix("p").toIntOrNull()
            val videoFormat = if (targetHeight != null) {
                info.formats.filter { it.isVideo && it.height == targetHeight }
                    .maxByOrNull { it.bitrate }
            } else {
                info.formats.filter { it.isVideo }
                    .maxByOrNull { it.height }
            }
            val audioFormat = info.formats.filter { !it.isVideo }
                .maxByOrNull { it.bitrate }

            if (videoFormat != null) {
                val patchedInfo = info.copy(
                    directVideoUrl = videoFormat.url,
                    audioUrl = audioFormat?.url ?: info.audioUrl,
                    hlsUrl = null
                )
                _streamInfo.value = patchedInfo
                _isLoadingStream.value = true
                loadMediaItemIntoController(video, patchedInfo, savedPos, resolveGeneration.get())
            }
        }
    }

    fun toggleAudioOnly() {
        val current = _isAudioOnly.value
        _isAudioOnly.value = !current
        val controller = _mediaController.value ?: return
        val builder = controller.trackSelectionParameters.buildUpon()
        builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, !current)
        controller.trackSelectionParameters = builder.build()
    }

    fun toggleFullScreen() {
        val target = !_isFullScreen.value
        _isFullScreen.value = target
        _fullScreenEvents.tryEmit(target)
    }

    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen
        _fullScreenEvents.tryEmit(fullScreen)
    }

    fun updateIsInPictureInPicture(inPiP: Boolean) {
        _isInPictureInPicture.value = inPiP
    }

    fun dismissPlayer() {
        _mediaController.value?.stop()
        _currentVideo.value = null
        _streamInfo.value = null
        _relatedVideos.value = emptyList()
        _playerCommands.tryEmit(PlayerCommand.Dismiss)
        scope.launch {
            miniPlayerProgress.snapTo(1f)
        }
    }

    fun release() {
        progressTrackingJob?.cancel()
        resolveJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        _mediaController.value = null
        scope.cancel()
    }
}
