package com.reayt.youtube.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.reayt.youtube.data.NetworkModule

@UnstableApi
class PlaybackService : MediaSessionService() {

    private val tag = "PlaybackService"
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "youtube_playback"
        const val NOTIFICATION_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Base OkHttp data source with dynamic header resolution
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(NetworkModule.apiClient)
            .setUserAgent(NetworkModule.UA_VISIONOS)

        val defaultDataSourceFactory = DefaultDataSource.Factory(this, okHttpDataSourceFactory)

        // ResolvingDataSource that applies client-specific headers based on stream URL
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(defaultDataSourceFactory) { dataSpec ->
            val url = dataSpec.uri.toString()
            if (url.contains("googlevideo.com") || url.contains("youtube.com")) {
                dataSpec.buildUpon()
                    .setHttpRequestHeaders(
                        mapOf(
                            "Origin" to "https://www.youtube.com",
                            "Referer" to "https://www.youtube.com/"
                        )
                    )
                    .build()
            } else {
                dataSpec
            }
        }

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        this.exoPlayer = player
        this.mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YouTube Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active background playback for YouTube streams"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Validate caller package matches self
        val callingPackage = controllerInfo.packageName
        if (callingPackage == packageName) {
            return mediaSession
        }
        // Allow system UI / Bluetooth controllers
        if (callingPackage == "com.android.systemui" || controllerInfo.connectionHints.isEmpty) {
            return mediaSession
        }
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            exoPlayer = null
        } catch (e: Exception) {
            Log.w(tag, "Service onDestroy warning: ${e.message}")
        }
        super.onDestroy()
    }
}
