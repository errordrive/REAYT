package com.reayt.youtube.player

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.model.StreamFormat
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

object VideoPlaybackManager {

    private const val TAG = "VideoPlaybackManager"
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var progressJob: Job? = null
    private var resolveJob: Job? = null

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

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _selectedQuality = MutableStateFlow("Auto")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    private val _relatedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val relatedVideos: StateFlow<List<YouTubeVideo>> = _relatedVideos.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    fun getActivePlayer(): ExoPlayer? {
        return PlaybackService.instance?.player
    }

    init {
        startProgressTracking()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val p = getActivePlayer()
                if (p != null && p.isPlaying) {
                    _positionMs.value = p.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = p.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    fun updateIsPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updatePlaybackState(state: Int) {
        _isBuffering.value = (state == Player.STATE_BUFFERING)
        val p = getActivePlayer() ?: return
        _durationMs.value = p.duration.coerceAtLeast(0L)
        _positionMs.value = p.currentPosition.coerceAtLeast(0L)

        // Handle auto-advance when video ends and autoplay is on
        if (state == Player.STATE_ENDED && _isAutoplayEnabled.value) {
            val nextVideo = _relatedVideos.value.firstOrNull()
            if (nextVideo != null) {
                playVideo(nextVideo)
            }
        }
    }

    fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        Log.e(TAG, "Playback error: ${error.message}", error)
        _streamError.value = "Playback error: ${error.localizedMessage ?: "Unable to stream"}"
    }

    fun playVideo(video: YouTubeVideo) {
        _currentVideo.value = video
        _isPlayerExpanded.value = true
        _streamError.value = null
        _isLoadingStream.value = true
        _selectedQuality.value = "Auto"

        YouTubeRepository.recordHistory(video)

        resolveJob?.cancel()
        resolveJob = scope.launch(Dispatchers.IO) {
            // Load related videos
            launch {
                val related = YouTubeClient.fetchRelatedVideos(video.id, video.title)
                _relatedVideos.value = related
            }

            // Resolve streaming URLs
            val info = YouTubeClient.resolveVideoStream(video.id)
            _streamInfo.value = info

            if (info == null || (info.hlsUrl == null && info.directVideoUrl == null)) {
                _isLoadingStream.value = false
                _streamError.value = "Unable to fetch direct stream for this video."
                return@launch
            }

            val targetUrl = info.hlsUrl ?: info.directVideoUrl ?: ""
            val isHls = info.hlsUrl != null

            launch(Dispatchers.Main) {
                val p = getActivePlayer()
                if (p == null) {
                    _streamError.value = "Player engine initializing..."
                    _isLoadingStream.value = false
                    return@launch
                }

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(video.title)
                    .setArtist(video.channelTitle)
                    .setArtworkUri(Uri.parse(video.thumbnailUrl))
                    .build()

                val item = MediaItem.Builder()
                    .setUri(targetUrl)
                    .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                p.stop()
                p.clearMediaItems()
                p.setMediaItem(item)
                p.playbackParameters = PlaybackParameters(_playbackSpeed.value)
                p.prepare()
                p.playWhenReady = true
                p.play()

                _isLoadingStream.value = false
                _isPlaying.value = true
            }
        }
    }

    fun togglePlayPause() {
        val p = getActivePlayer() ?: return
        if (p.isPlaying) {
            p.pause()
            _isPlaying.value = false
        } else {
            p.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(positionMs: Long) {
        val p = getActivePlayer() ?: return
        p.seekTo(positionMs.coerceIn(0L, _durationMs.value))
        _positionMs.value = positionMs
    }

    fun seekForward10() {
        val p = getActivePlayer() ?: return
        val target = (p.currentPosition + 10_000L).coerceAtMost(p.duration)
        p.seekTo(target)
    }

    fun seekBackward10() {
        val p = getActivePlayer() ?: return
        val target = (p.currentPosition - 10_000L).coerceAtLeast(0L)
        p.seekTo(target)
    }

    fun setQuality(format: StreamFormat) {
        _selectedQuality.value = format.qualityLabel
        val p = getActivePlayer() ?: return
        val pos = p.currentPosition

        val item = MediaItem.Builder()
            .setUri(format.url)
            .setMimeType(if (format.mimeType.contains("mp4")) MimeTypes.VIDEO_MP4 else MimeTypes.VIDEO_WEBM)
            .build()

        p.setMediaItem(item, pos)
        p.prepare()
        p.play()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val p = getActivePlayer() ?: return
        p.playbackParameters = PlaybackParameters(speed)
    }

    fun toggleAutoplay() {
        _isAutoplayEnabled.value = !_isAutoplayEnabled.value
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun expandPlayer() {
        _isPlayerExpanded.value = true
    }

    fun collapsePlayer() {
        _isPlayerExpanded.value = false
        _isFullScreen.value = false
    }

    fun closePlayer() {
        val p = getActivePlayer()
        p?.stop()
        p?.clearMediaItems()
        _currentVideo.value = null
        _streamInfo.value = null
        _isPlaying.value = false
        _isPlayerExpanded.value = false
        _isFullScreen.value = false
    }
}
