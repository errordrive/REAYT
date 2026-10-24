package com.reayt.youtube.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.reayt.youtube.data.NetworkModule
import com.reayt.youtube.player.VideoPlaybackManager

class PlaybackService : MediaSessionService() {

    var player: ExoPlayer? = null
        private set
    var mediaSession: MediaSession? = null
        private set
    var dataSourceFactory: DefaultDataSource.Factory? = null
        private set

    companion object {
        const val CHANNEL_ID = "youtube_playback"
        const val NOTIFICATION_ID = 2001

        @Volatile
        var instance: PlaybackService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(NetworkModule.apiClient)
            .setUserAgent(NetworkModule.UA_VISIONOS)

        val dsFactory = DefaultDataSource.Factory(this, okHttpDataSourceFactory)
        this.dataSourceFactory = dsFactory

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                VideoPlaybackManager.updateIsPlaying(isPlaying)
            }

            override fun onPlaybackStateChanged(state: Int) {
                VideoPlaybackManager.updatePlaybackState(state)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                VideoPlaybackManager.onPlayerError(error)
            }
        })

        this.player = exoPlayer
        this.mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    /**
     * Streams video either via full HLS manifest, merged video+audio sources, or direct muxed streams.
     */
    fun playStream(
        hlsUrl: String?,
        directVideoUrl: String?,
        audioUrl: String?,
        videoMimeType: String?,
        metadata: MediaMetadata,
        playbackSpeed: Float = 1.0f,
        initialPositionMs: Long = 0L
    ) {
        val p = player ?: return
        val dsFactory = dataSourceFactory ?: return

        p.stop()
        p.clearMediaItems()

        if (!hlsUrl.isNullOrBlank()) {
            val item = MediaItem.Builder()
                .setUri(hlsUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setMediaMetadata(metadata)
                .build()
            p.setMediaItem(item, initialPositionMs)
        } else if (!directVideoUrl.isNullOrBlank() && !audioUrl.isNullOrBlank()) {
            val videoItem = MediaItem.Builder().setUri(directVideoUrl).setMediaMetadata(metadata).build()
            val audioItem = MediaItem.fromUri(audioUrl)
            val videoSource = ProgressiveMediaSource.Factory(dsFactory).createMediaSource(videoItem)
            val audioSource = ProgressiveMediaSource.Factory(dsFactory).createMediaSource(audioItem)
            val mergedSource = MergingMediaSource(videoSource, audioSource)
            p.setMediaSource(mergedSource, initialPositionMs)
        } else if (!directVideoUrl.isNullOrBlank()) {
            val cleanMime = videoMimeType?.substringBefore(";")?.trim()
            val item = MediaItem.Builder()
                .setUri(directVideoUrl)
                .apply {
                    if (!cleanMime.isNullOrBlank()) {
                        setMimeType(cleanMime)
                    }
                }
                .setMediaMetadata(metadata)
                .build()
            p.setMediaItem(item, initialPositionMs)
        }

        p.playbackParameters = PlaybackParameters(playbackSpeed)
        p.prepare()
        p.playWhenReady = true
        p.play()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YouTube Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active playback for YouTube video and audio"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        instance = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
