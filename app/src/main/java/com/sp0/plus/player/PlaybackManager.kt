package com.sp0.plus.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.sp0.plus.data.MusicRepository
import com.sp0.plus.data.YouTubeMusicClient
import com.sp0.plus.lyrics.LyricLine
import com.sp0.plus.lyrics.LyricsEngine
import com.sp0.plus.model.SongsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var streamJob: Job? = null

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()
    private var mediaController: MediaController? = null

    private val _currentTrack = MutableStateFlow<SongsModel?>(MusicRepository.sampleTracks.firstOrNull())
    val currentTrack: StateFlow<SongsModel?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(MusicRepository.sampleTracks.firstOrNull()?.durationMs ?: 0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow(MusicRepository.sampleTracks)
    val queue: StateFlow<List<SongsModel>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isLiked = MutableStateFlow(MusicRepository.sampleTracks.firstOrNull()?.isLiked ?: false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _audioQualityBadge = MutableStateFlow(MusicRepository.sampleTracks.firstOrNull()?.audioFormat ?: "LOSSLESS • FLAC")
    val audioQualityBadge: StateFlow<String> = _audioQualityBadge.asStateFlow()

    private val _lyricsLines = MutableStateFlow<List<LyricLine>>(
        LyricsEngine.parse(MusicRepository.sampleTracks.firstOrNull()?.lyricsLrc ?: "")
    )
    val lyricsLines: StateFlow<List<LyricLine>> = _lyricsLines.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(0)
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
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val duration = controller.duration
                if (duration > 0) {
                    _durationMs.value = duration
                }
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val pos = controller.currentPosition.coerceAtLeast(0L)
                        _positionMs.value = pos
                        val dur = controller.duration
                        if (dur > 0) _durationMs.value = dur

                        updateActiveLyricIndex(pos)
                    }
                } ?: run {
                    if (_isPlaying.value) {
                        val nextPos = (_positionMs.value + 500L).coerceAtMost(_durationMs.value)
                        _positionMs.value = nextPos
                        updateActiveLyricIndex(nextPos)
                        if (nextPos >= _durationMs.value && _durationMs.value > 0) {
                            handleTrackEnded()
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
        _durationMs.value = track.durationMs
        _positionMs.value = 0L
        _isLiked.value = track.isLiked
        _audioQualityBadge.value = track.audioFormat
        _lyricsLines.value = LyricsEngine.parse(track.lyricsLrc)
        _activeLyricIndex.value = 0

        _isPlaying.value = true
        streamJob?.cancel()
        streamJob = scope.launch {
            var streamUrl = track.url
            if (streamUrl.isBlank() && track.youtubeVideoId.isNotBlank()) {
                _isBuffering.value = true
                streamUrl = YouTubeMusicClient.resolveStreamUrl(track.youtubeVideoId).orEmpty()
                _isBuffering.value = false
            }
            if (streamUrl.isBlank()) {
                // Could not resolve a stream (expired visitor id, blocked client, offline...).
                _isPlaying.value = false
                _streamError.value = "Couldn't load \"${track.title}\". Try a new Visitor ID in Settings."
                return@launch
            }
            _streamError.value = null

            mediaController?.let { controller ->
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.singer)
                    .setAlbumTitle(track.album)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id.toString())
                    .setUri(streamUrl)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
                _isPlaying.value = false
            } else {
                val track = _currentTrack.value
                if (controller.playbackState == Player.STATE_IDLE && track != null) {
                    playTrack(track)
                } else {
                    controller.play()
                    _isPlaying.value = true
                }
            }
        } ?: run {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        var nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            nextIdx = 0
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
        mediaController?.seekTo(clamped)
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

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                mediaController?.play()
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.lastIndex) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                    seekTo(0L)
                }
            }
        }
    }
}
