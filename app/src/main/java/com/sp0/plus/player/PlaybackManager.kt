package com.sp0.plus.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.sp0.plus.data.AudioStreamResolver
import com.sp0.plus.data.LikesManager
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
import java.util.concurrent.atomic.AtomicLong

enum class RepeatMode {
    OFF, ONE, ALL
}

private enum class StreamSource {
    LOCAL_CACHE,
    YOUTUBE,
    SAAVN_320,
    SAAVN_160,
    OTHER
}

object PlaybackManager {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("PlaybackManager", "Uncaught playback error: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob() + exceptionHandler)
    private var progressJob: Job? = null
    private var streamJob: Job? = null
    private var retryJob: Job? = null
    private var crossfadeJob: Job? = null

    private val playbackToken = AtomicLong(0L)
    val currentPlaybackToken: Long get() = playbackToken.get()

    private var currentStreamSource = StreamSource.OTHER
    private var pendingFadeIn = false
    private var isCrossfading = false
    private var isSeeking = false

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()
    var mediaController: MediaController? = null
        private set

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

    private val _repeatMode = MutableStateFlow(
        try {
            RepeatMode.valueOf(SettingsManager.repeatModeSaved.value.ifBlank { "OFF" })
        } catch (_: Exception) {
            RepeatMode.OFF
        }
    )
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(SettingsManager.shuffleEnabled.value)
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

    private fun applyPlayerVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        PlaybackService.instance?.player?.volume = clamped
    }

    private fun fadeIn(durationMs: Long) {
        if (durationMs <= 0L) {
            applyPlayerVolume(1f)
            return
        }
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            isCrossfading = true
            val steps = (durationMs / 40L).coerceIn(5L, 50L)
            val stepTime = durationMs / steps
            for (i in 0..steps) {
                val v = (i.toFloat() / steps.toFloat())
                applyPlayerVolume(v)
                delay(stepTime)
            }
            applyPlayerVolume(1f)
            isCrossfading = false
        }
    }

    private fun fadeOut(durationMs: Long, onComplete: (() -> Unit)? = null) {
        if (durationMs <= 0L) {
            applyPlayerVolume(0f)
            onComplete?.invoke()
            return
        }
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            isCrossfading = true
            val steps = (durationMs / 40L).coerceIn(5L, 50L)
            val stepTime = durationMs / steps
            for (i in steps downTo 0) {
                val v = (i.toFloat() / steps.toFloat())
                applyPlayerVolume(v)
                delay(stepTime)
            }
            applyPlayerVolume(0f)
            isCrossfading = false
            onComplete?.invoke()
        }
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
            _isPending.value = false
            _isBuffering.value = false
            if (pendingFadeIn) {
                pendingFadeIn = false
                val fadeMs = SettingsManager.crossfadeDurationMs.value.toLong().coerceIn(500L, 4000L)
                fadeIn(fadeMs)
            } else if (SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0) {
                fadeIn(600L)
            } else {
                applyPlayerVolume(1f)
            }
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
        val myToken = playbackToken.get()
        val track = _currentTrack.value

        val isSourceError = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ||
                (error.message?.contains("Source error", ignoreCase = true) == true)

        if (track != null && isSourceError) {
            when (currentStreamSource) {
                StreamSource.YOUTUBE -> {
                    android.util.Log.w("PlaybackManager", "YouTube direct stream failed with source error for '${track.title}'. Retrying with JioSaavn master stream...")
                    currentStreamSource = StreamSource.SAAVN_320
                    retryJob?.cancel()
                    retryJob = scope.launch {
                        val saavn = AudioStreamResolver.resolveSaavnStream(track.title, track.singer, prefer320 = true)
                        if (saavn != null && playbackToken.get() == myToken) {
                            _audioQualityBadge.value = saavn.audioBadge
                            executePlayback(track, saavn.url, myToken)
                            return@launch
                        }
                        val saavn160 = AudioStreamResolver.resolveSaavnStream(track.title, track.singer, prefer320 = false)
                        if (saavn160 != null && playbackToken.get() == myToken) {
                            currentStreamSource = StreamSource.SAAVN_160
                            _audioQualityBadge.value = saavn160.audioBadge
                            executePlayback(track, saavn160.url, myToken)
                            return@launch
                        }
                        _isPending.value = false
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _streamError.value = "Playback error: Source error (stream unavailable)"
                    }
                    return
                }
                StreamSource.SAAVN_320 -> {
                    android.util.Log.w("PlaybackManager", "Saavn 320k stream failed for '${track.title}'. Retrying with 160k stream...")
                    currentStreamSource = StreamSource.SAAVN_160
                    retryJob?.cancel()
                    retryJob = scope.launch {
                        val saavn160 = AudioStreamResolver.resolveSaavnStream(track.title, track.singer, prefer320 = false)
                        if (saavn160 != null && playbackToken.get() == myToken) {
                            _audioQualityBadge.value = saavn160.audioBadge
                            executePlayback(track, saavn160.url, myToken)
                            return@launch
                        }
                        _isPending.value = false
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _streamError.value = "Playback error: Source error (stream unavailable)"
                    }
                    return
                }
                else -> {}
            }
        }

        android.util.Log.e("PlaybackManager", "Player error: ${error.errorCodeName} - ${error.message}", error)
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
                    val dur = activePlayer.duration
                    if (!isSeeking) {
                        _positionMs.value = pos
                        if (dur > 0) _durationMs.value = dur
                        updateActiveLyricIndex(pos)

                        // Smooth crossfade fade-out towards end of track
                        val crossfadeEnabled = SettingsManager.crossfadeEnabled.value
                        val crossfadeMs = SettingsManager.crossfadeDurationMs.value.toLong()
                        if (crossfadeEnabled && crossfadeMs > 0 && dur > crossfadeMs * 2) {
                            val timeLeft = dur - pos
                            if (timeLeft in 1..crossfadeMs) {
                                val fraction = (timeLeft.toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
                                applyPlayerVolume(fraction)
                            } else if (timeLeft > crossfadeMs && !isCrossfading && !pendingFadeIn) {
                                applyPlayerVolume(1f)
                            }
                        }
                    }
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
            val myToken = playbackToken.incrementAndGet()

            retryJob?.cancel()
            streamJob?.cancel()
            crossfadeJob?.cancel()
            isCrossfading = false

            val shouldFadeIn = pendingFadeIn || (SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0)
            pendingFadeIn = shouldFadeIn

            PlaybackService.instance?.player?.let { p ->
                p.stop()
                p.clearMediaItems()
                p.volume = if (shouldFadeIn) 0f else 1f
            }
            mediaController?.let { c ->
                c.stop()
                c.clearMediaItems()
                c.volume = if (shouldFadeIn) 0f else 1f
            }

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

            val trackId = track.youtubeVideoId.ifBlank { track.id }
            _currentTrack.value = track
            _durationMs.value = if (track.durationMs > 0) track.durationMs else 180000L
            _positionMs.value = 0L
            _isLiked.value = LikesManager.isLiked(trackId)
            _audioQualityBadge.value = track.audioFormat.takeIf { !it.contains("YOUTUBE", ignoreCase = true) && it.isNotBlank() } ?: "LOSSLESS • FLAC"
            _lyricsLines.value = emptyList()
            _activeLyricIndex.value = 0
            _streamError.value = null

            _isPending.value = true
            _isBuffering.value = true

            streamJob = scope.launch {
                try {
                    if (playbackToken.get() != myToken) return@launch

                    var streamUrl = track.url
                    var badge = _audioQualityBadge.value
                    var streamSource = StreamSource.OTHER

                    if (DownloadManager.isDownloaded(track.youtubeVideoId)) {
                        val localPath = DownloadManager.getLocalPath(track.youtubeVideoId)
                        if (!localPath.isNullOrBlank()) {
                            streamUrl = if (localPath.startsWith("/")) "file://$localPath" else localPath
                            badge = "OFFLINE • CACHED"
                            streamSource = StreamSource.LOCAL_CACHE
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
                            streamSource = when {
                                streamUrl.contains("googlevideo.com") -> StreamSource.YOUTUBE
                                streamUrl.contains("saavncdn.com") -> if (streamUrl.contains("_320.mp4")) StreamSource.SAAVN_320 else StreamSource.SAAVN_160
                                else -> StreamSource.OTHER
                            }
                        }
                    }

                    if (playbackToken.get() != myToken) return@launch

                    _audioQualityBadge.value = badge
                    currentStreamSource = streamSource

                    if (streamUrl.isBlank()) {
                        _isPending.value = false
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _streamError.value = "Couldn't load stream for \"${track.title}\". Please check your connection."
                        return@launch
                    }
                    _streamError.value = null

                    executePlayback(track, streamUrl, myToken)
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
                    if (playbackToken.get() == myToken) {
                        android.util.Log.e("PlaybackManager", "Playback stream error: ${e.message}", e)
                        _isPending.value = false
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _streamError.value = "Playback error: ${e.message}"
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlaybackManager", "playTrack failed: ${e.message}", e)
        }
    }

    private fun executePlayback(track: SongsModel, streamUrl: String, token: Long) {
        try {
            if (playbackToken.get() != token) return

            val uri = if (streamUrl.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(streamUrl))
            } else {
                android.net.Uri.parse(streamUrl)
            }

            val mimeType = when {
                streamUrl.contains(".mp3", ignoreCase = true) || streamUrl.contains("audio/mpeg") -> MimeTypes.AUDIO_MPEG
                streamUrl.contains(".m4a", ignoreCase = true) || streamUrl.contains(".mp4", ignoreCase = true) || streamUrl.contains("audio/mp4") -> MimeTypes.AUDIO_MP4
                streamUrl.contains("googlevideo.com") -> MimeTypes.AUDIO_MP4
                else -> MimeTypes.AUDIO_MP4
            }

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.singer)
                .setAlbumTitle(track.album)
                .setArtworkUri(if (track.coverUri.isNotBlank()) android.net.Uri.parse(track.coverUri) else null)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(uri)
                .setMimeType(mimeType)
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
                    if (playbackToken.get() != token) return
                    srv.playMediaItem(mediaItem, token)
                    return
                } catch (e: Throwable) {
                    android.util.Log.e("PlaybackManager", "Direct playMediaItem failed", e)
                }
            }

            // 2. Controller execution fallback
            if (mediaController != null) {
                mediaController?.let { controller ->
                    try {
                        if (playbackToken.get() != token) return
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
                    if (playbackToken.get() == token) {
                        executePlayback(track, streamUrl, token)
                    }
                }
            }
        } catch (e: Throwable) {
            if (playbackToken.get() == token) {
                android.util.Log.e("PlaybackManager", "executePlayback unexpected error: ${e.message}", e)
                _isPending.value = false
                _isPlaying.value = false
                _isBuffering.value = false
                _streamError.value = "Playback error: ${e.message}"
            }
        }
    }

    fun togglePlayPause() {
        val activePlayer = PlaybackService.instance?.player ?: mediaController
        if (activePlayer != null) {
            if (activePlayer.isPlaying) {
                if (SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0) {
                    fadeOut(150L) {
                        activePlayer.pause()
                        _isPlaying.value = false
                        applyPlayerVolume(1f)
                    }
                } else {
                    activePlayer.pause()
                    _isPlaying.value = false
                }
            } else {
                val track = _currentTrack.value
                if (activePlayer.playbackState == Player.STATE_IDLE && track != null) {
                    playTrack(track)
                } else {
                    if (SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0) {
                        applyPlayerVolume(0f)
                        activePlayer.play()
                        _isPlaying.value = true
                        fadeIn(200L)
                    } else {
                        applyPlayerVolume(1f)
                        activePlayer.play()
                        _isPlaying.value = true
                    }
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
                if (_repeatMode.value == RepeatMode.ALL) {
                    nextIdx = 0
                } else {
                    return
                }
            }
        }
        val nextTrack = q[nextIdx]
        if (_isPlaying.value && SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0) {
            fadeOut(250L) {
                pendingFadeIn = true
                playTrack(nextTrack)
            }
        } else {
            playTrack(nextTrack)
        }
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
        val prevTrack = q[prevIdx]
        if (_isPlaying.value && SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0) {
            fadeOut(250L) {
                pendingFadeIn = true
                playTrack(prevTrack)
            }
        } else {
            playTrack(prevTrack)
        }
    }

    fun seekTo(posMs: Long) {
        isSeeking = true
        val clamped = posMs.coerceIn(0L, _durationMs.value)
        _positionMs.value = clamped
        updateActiveLyricIndex(clamped)
        PlaybackService.instance?.player?.seekTo(clamped) ?: mediaController?.seekTo(clamped)
        val crossfadeMs = SettingsManager.crossfadeDurationMs.value.toLong()
        if (_durationMs.value - clamped > crossfadeMs) {
            applyPlayerVolume(1f)
        }
        scope.launch {
            delay(300L)
            isSeeking = false
        }
    }

    fun toggleRepeat() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
        SettingsManager.setRepeatMode(newMode.name)
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        SettingsManager.setShuffleEnabled(newShuffle)
    }

    fun toggleLike() {
        val track = _currentTrack.value ?: return
        val trackId = track.youtubeVideoId.ifBlank { track.id }
        val newLiked = LikesManager.toggleLike(trackId)
        _isLiked.value = newLiked

        // Update in queue
        _queue.value = _queue.value.map {
            if (it.id == track.id) it.copy(isLiked = newLiked) else it
        }
        _currentTrack.value = track.copy(isLiked = newLiked)
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                val activePlayer = PlaybackService.instance?.player ?: mediaController
                if (SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0) {
                    applyPlayerVolume(0f)
                    activePlayer?.play()
                    fadeIn(SettingsManager.crossfadeDurationMs.value.toLong().coerceIn(500L, 3000L))
                } else {
                    applyPlayerVolume(1f)
                    activePlayer?.play()
                }
            }
            RepeatMode.ALL -> {
                pendingFadeIn = SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0
                skipNext()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.lastIndex) {
                    pendingFadeIn = SettingsManager.crossfadeEnabled.value && SettingsManager.crossfadeDurationMs.value > 0
                    skipNext()
                } else {
                    _isPlaying.value = false
                    applyPlayerVolume(1f)
                    seekTo(0L)
                }
            }
        }
    }
}
