package com.sp0.plus.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.sp0.plus.data.AudioStreamResolver
import com.sp0.plus.data.MusicRepository
import com.sp0.plus.data.NetworkModule
import com.sp0.plus.data.PlayHistoryManager
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.data.YouTubeMusicClient
import com.sp0.plus.lyrics.LyricLine
import com.sp0.plus.lyrics.LyricsEngine
import com.sp0.plus.model.SongsModel
import com.sp0.plus.ui.notification.PlaybackService
import kotlinx.coroutines.CoroutineExceptionHandler
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

enum class RepeatMode {
    OFF, ONE, ALL
}

object PlaybackManager {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("PlaybackManager", "Uncaught playback error: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob() + exceptionHandler)
    private var progressJob: Job? = null
    private var streamJob: Job? = null
    private var retryJob: Job? = null

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()
    private var mediaController: MediaController? = null

    private val _currentTrack = MutableStateFlow<SongsModel?>(null)
    val currentTrack: StateFlow<SongsModel?> = _currentTrack.asStateFlow()

    private val _isPending = MutableStateFlow(false)
    val isPending: StateFlow<Boolean> = _isPending.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<SongsModel>>(emptyList())
    val queue: StateFlow<List<SongsModel>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _audioQualityBadge = MutableStateFlow("LOSSLESS • MASTER")
    val audioQualityBadge: StateFlow<String> = _audioQualityBadge.asStateFlow()

    private val _lyricsLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyricsLines: StateFlow<List<LyricLine>> = _lyricsLines.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(-1)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    private var sleepTimerJob: Job? = null
    private val _sleepTimerSeconds = MutableStateFlow(0L)
    val sleepTimerSeconds: StateFlow<Long> = _sleepTimerSeconds.asStateFlow()

    init {
        startProgressTracking()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = 0L
            return
        }

        val totalSecs = minutes * 60L
        _sleepTimerSeconds.value = totalSecs

        sleepTimerJob = scope.launch {
            var remaining = totalSecs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1L
                _sleepTimerSeconds.value = remaining
            }
            if (remaining <= 0) {
                mediaController?.pause()
                _isPlaying.value = false
                _sleepTimerSeconds.value = 0L
            }
        }
    }

    fun attachMediaController(controller: MediaController) {
        this.mediaController = controller

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateIsPlaying(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState(playbackState)
            }
        })
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
            _isPending.value = false
            _isBuffering.value = false
        }
    }

    fun updatePlaybackState(playbackState: Int) {
        _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
        if (playbackState == Player.STATE_READY) {
            _isBuffering.value = false
            _isPending.value = false
            val activePlayer = PlaybackService.instance?.player ?: mediaController
            val duration = activePlayer?.duration ?: 0L
            if (duration > 0) {
                _durationMs.value = duration
            }
        } else if (playbackState == Player.STATE_ENDED) {
            _isPending.value = false
            _isBuffering.value = false
            handleTrackEnded()
        }
    }

    fun updateDuration(duration: Long) {
        if (duration > 0) {
            _durationMs.value = duration
        }
    }

    fun handlePlayerError(error: androidx.media3.common.PlaybackException) {
        _isPending.value = false
        _isBuffering.value = false
        _isPlaying.value = false
        _streamError.value = "Playback error: ${error.message ?: error.errorCodeName}"
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val activePlayer = PlaybackService.instance?.player ?: mediaController
                if (activePlayer != null && activePlayer.isPlaying) {
                    val pos = activePlayer.currentPosition.coerceAtLeast(0L)
                    _positionMs.value = pos
                    val dur = activePlayer.duration
                    if (dur > 0) _durationMs.value = dur

                    updateActiveLyricIndex(pos)
                }
                delay(250L)
            }
        }
    }

    private fun updateActiveLyricIndex(pos: Long) {
        val lines = _lyricsLines.value
        val idx = LyricsEngine.getActiveLineIndex(lines, pos)
        if (idx != _activeLyricIndex.value) {
            _activeLyricIndex.value = idx
        }
    }

    fun playTrack(track: SongsModel, newQueue: List<SongsModel>? = null) {
        try {
            if (newQueue != null && newQueue.isNotEmpty()) {
                _queue.value = newQueue
                _currentIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            } else {
                val idx = _queue.value.indexOfFirst { it.id == track.id }
                if (idx >= 0) {
                    _currentIndex.value = idx
                } else {
                    _queue.value = _queue.value + track
                    _currentIndex.value = _queue.value.lastIndex
                }
            }

            _currentTrack.value = track
            _durationMs.value = if (track.durationMs > 0) track.durationMs else 180000L
            _positionMs.value = 0L
            _isLiked.value = track.isLiked
            _audioQualityBadge.value = track.audioFormat.takeIf { !it.contains("YOUTUBE", ignoreCase = true) && it.isNotBlank() } ?: "LOSSLESS • FLAC"
            _lyricsLines.value = emptyList()
            _activeLyricIndex.value = 0
            _streamError.value = null

            _isPending.value = true
            _isBuffering.value = true

            retryJob?.cancel()
            streamJob?.cancel()
            streamJob = scope.launch {
                try {
                    var streamUrl = track.url
                    var badge = _audioQualityBadge.value

                    if (DownloadManager.isDownloaded(track.youtubeVideoId)) {
                        val localPath = DownloadManager.getLocalPath(track.youtubeVideoId)
                        if (!localPath.isNullOrBlank()) {
                            streamUrl = localPath
                            badge = "OFFLINE • CACHED"
                        }
                    }

                    if (streamUrl.isBlank()) {
                        val resolved = AudioStreamResolver.resolveStream(
                            title = track.title,
                            singer = track.singer,
                            fallbackVideoId = track.youtubeVideoId
                        )
                        if (resolved != null) {
                            streamUrl = resolved.url
                            badge = resolved.audioBadge
                        }
                    }

                    _audioQualityBadge.value = badge

                    if (streamUrl.isBlank()) {
                        _isPending.value = false
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _streamError.value = "Couldn't load stream for \"${track.title}\". Please check your connection."
                        return@launch
                    }
                    _streamError.value = null

                    executePlayback(track, streamUrl)
                    PlayHistoryManager.recordPlay(track)

                    scope.launch(Dispatchers.IO) {
                        try {
                            val title = java.net.URLEncoder.encode(track.title, "UTF-8")
                            val artist = java.net.URLEncoder.encode(track.singer, "UTF-8")
                            val url = "https://lrclib.net/api/get?artist_name=$artist&track_name=$title"
                            val response = NetworkModule.apiClient.newCall(
                                okhttp3.Request.Builder().url(url).build()
                            ).execute()
                            val body = response.body?.string() ?: return@launch
                            val json = org.json.JSONObject(body)
                            val lrc = json.optString("syncedLyrics", "")
                            if (lrc.isNotBlank()) {
                                _lyricsLines.value = LyricsEngine.parse(lrc)
                            } else {
                                _lyricsLines.value = emptyList()
                            }
                        } catch (_: Exception) {
                            _lyricsLines.value = emptyList()
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("PlaybackManager", "Playback stream error: ${e.message}", e)
                    _isPending.value = false
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _streamError.value = "Playback error: ${e.message}"
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackManager", "playTrack failed: ${e.message}", e)
        }
    }

    private fun executePlayback(track: SongsModel, streamUrl: String) {
        try {
            val uri = android.net.Uri.parse(streamUrl)
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.singer)
                .setAlbumTitle(track.album)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(uri)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(uri)
                        .build()
                )
                .setMediaMetadata(mediaMetadata)
                .build()

            // 1. Direct PlaybackService instance execution (in-process, avoids IPC serialization)
            val srv = PlaybackService.instance
            if (srv?.player != null) {
                try {
                    srv.playMediaItem(mediaItem)
                    return
                } catch (e: Throwable) {
                    android.util.Log.e("PlaybackManager", "Direct playMediaItem failed", e)
                }
            }

            // 2. Controller execution fallback
            if (mediaController != null) {
                mediaController?.let { controller ->
                    try {
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.play()
                    } catch (e: Throwable) {
                        android.util.Log.e("PlaybackManager", "mediaController play failed", e)
                        _streamError.value = "Playback engine failed: ${e.message}"
                    }
                }
            } else if (srv == null) {
                // If neither service nor controller is connected yet, retry once after short delay
                retryJob?.cancel()
                retryJob = scope.launch {
                    delay(400L)
                    executePlayback(track, streamUrl)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackManager", "executePlayback unexpected error: ${e.message}", e)
            _isPending.value = false
            _isPlaying.value = false
            _isBuffering.value = false
            _streamError.value = "Playback error: ${e.message}"
        }
    }

    fun togglePlayPause() {
        val activePlayer = PlaybackService.instance?.player ?: mediaController
        if (activePlayer != null) {
            if (activePlayer.isPlaying) {
                activePlayer.pause()
                _isPlaying.value = false
            } else {
                val track = _currentTrack.value
                if (activePlayer.playbackState == Player.STATE_IDLE && track != null) {
                    playTrack(track)
                } else {
                    activePlayer.play()
                    _isPlaying.value = true
                }
            }
        } else {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun skipForward10() {
        val newPos = (_positionMs.value + 10_000L).coerceAtMost(_durationMs.value)
        seekTo(newPos)
    }

    fun skipBackward10() {
        val newPos = (_positionMs.value - 10_000L).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        var nextIdx: Int
        if (_isShuffle.value && q.size > 1) {
            val possibleIndices = q.indices.filter { it != _currentIndex.value }
            nextIdx = if (possibleIndices.isNotEmpty()) possibleIndices.random() else (_currentIndex.value + 1) % q.size
        } else {
            nextIdx = _currentIndex.value + 1
            if (nextIdx >= q.size) {
                nextIdx = 0
            }
        }
        playTrack(q[nextIdx])
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = q.lastIndex
        }
        playTrack(q[prevIdx])
    }

    fun seekTo(posMs: Long) {
        val clamped = posMs.coerceIn(0L, _durationMs.value)
        _positionMs.value = clamped
        updateActiveLyricIndex(clamped)
        PlaybackService.instance?.player?.seekTo(clamped) ?: mediaController?.seekTo(clamped)
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleLike() {
        val track = _currentTrack.value ?: return
        val newLiked = !_isLiked.value
        _isLiked.value = newLiked

        // Update in queue
        _queue.value = _queue.value.map {
            if (it.id == track.id) it.copy(isLiked = newLiked) else it
        }
        _currentTrack.value = track.copy(isLiked = newLiked)
    }

    private fun executeCrossfadeThenSkip() {
        if (SettingsManager.crossfadeEnabled.value) {
            val crossfadeDurationMs = SettingsManager.crossfadeDurationMs.value
            scope.launch {
                val steps = (crossfadeDurationMs / 50L).coerceAtLeast(1L)
                for (i in 0 until steps) {
                    val vol = 1f - (i.toFloat() / steps.toFloat())
                    PlaybackService.instance?.player?.volume = vol
                    delay(50L)
                }
                skipNext()
                val upSteps = (300L / 50L).coerceAtLeast(1L)
                for (i in 0 until upSteps) {
                    val vol = (i.toFloat() / upSteps.toFloat())
                    PlaybackService.instance?.player?.volume = vol
                    delay(50L)
                }
                PlaybackService.instance?.player?.volume = 1f
            }
        } else {
            skipNext()
        }
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                val activePlayer = PlaybackService.instance?.player ?: mediaController
                activePlayer?.play()
            }
            RepeatMode.ALL -> executeCrossfadeThenSkip()
            RepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.lastIndex) {
                    executeCrossfadeThenSkip()
                } else {
                    _isPlaying.value = false
                    seekTo(0L)
                }
            }
        }
    }
}
