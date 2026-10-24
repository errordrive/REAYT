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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Process-wide, service-backed playback state holder.
 * UI components observe this state holder and communicate solely via MediaController.
 */
class PlayerStateHolder(private val context: Context) {

    private val tag = "PlayerStateHolder"
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var controllerFuture: ListenableFuture<MediaController>? = null

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
                                val nextVideo = _relatedVideos.value.firstOrNull()
                                if (nextVideo != null && nextVideo.id != _currentVideo.value?.id) {
                                    playVideo(nextVideo)
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
        // Requirement: Clear relatedVideos at the start of playVideo
        _relatedVideos.value = emptyList()
        _currentVideo.value = video
        _streamError.value = null
        _isLoadingStream.value = true
        retryCount = 0

        // Auto-expand player when a new video is selected
        scope.launch {
            miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
        }

        resolveJob?.cancel()
        resolveJob = scope.launch {
            // Concurrently fetch video details / up-next and stream info
            val detailsDeferred = launch(Dispatchers.IO) {
                val detailsResult = YouTubeClient.fetchNextAndDetails(video.id)
                if (detailsResult is ResultState.Success) {
                    val (enrichedVideo, related) = detailsResult.data
                    withContext(Dispatchers.Main) {
                        _currentVideo.value = enrichedVideo
                        _relatedVideos.value = related
                    }
                }
            }

            resolveAndStartStream(video, startPositionMs)
            detailsDeferred.join()
        }
    }

    private suspend fun resolveAndStartStream(video: YouTubeVideo, startPositionMs: Long) {
        val streamResult = YouTubeClient.resolveVideoStream(video.id)
        withContext(Dispatchers.Main) {
            when (streamResult) {
                is ResultState.Success -> {
                    val info = streamResult.data
                    _streamInfo.value = info
                    _isLoadingStream.value = false
                    _streamError.value = null
                    loadMediaItemIntoController(video, info, startPositionMs)
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

    private fun loadMediaItemIntoController(
        video: YouTubeVideo,
        info: YouTubeStreamInfo,
        startPositionMs: Long
    ) {
        val controller = _mediaController.value ?: return

        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setArtist(video.channelTitle)
            .setArtworkUri(Uri.parse(video.thumbnailUrl))
            .build()

        val mediaUri = info.hlsUrl ?: info.directVideoUrl ?: return
        val mimeType = if (info.hlsUrl != null) MimeTypes.APPLICATION_M3U8 else info.videoMimeType

        val mediaItem = MediaItem.Builder()
            .setUri(mediaUri)
            .apply {
                if (!mimeType.isNullOrBlank()) {
                    setMimeType(mimeType)
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
            scope.launch {
                delay(800)
                resolveAndStartStream(current, pos)
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
        val controller = _mediaController.value ?: return
        // Use TrackSelectionParameters for HLS and adaptive formats
        val builder = controller.trackSelectionParameters.buildUpon()
        if (quality.contains("1080")) {
            builder.setMaxVideoSize(1920, 1080)
        } else if (quality.contains("720")) {
            builder.setMaxVideoSize(1280, 720)
        } else if (quality.contains("480")) {
            builder.setMaxVideoSize(854, 480)
        } else if (quality.contains("360")) {
            builder.setMaxVideoSize(640, 360)
        } else {
            builder.clearVideoSizeConstraints()
        }
        controller.trackSelectionParameters = builder.build()
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
        _isFullScreen.value = !_isFullScreen.value
    }

    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen
    }

    fun updateIsInPictureInPicture(inPiP: Boolean) {
        _isInPictureInPicture.value = inPiP
    }

    fun dismissPlayer() {
        _mediaController.value?.stop()
        _currentVideo.value = null
        _streamInfo.value = null
        _relatedVideos.value = emptyList()
        scope.launch {
            miniPlayerProgress.snapTo(1f)
        }
    }

    fun release() {
        progressTrackingJob?.cancel()
        resolveJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        _mediaController.value = null
    }
}
