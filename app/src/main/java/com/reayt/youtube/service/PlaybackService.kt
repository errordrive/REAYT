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
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
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

        // ResolvingDataSource that applies client-specific headers based on stream URL & tags
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(defaultDataSourceFactory) { dataSpec ->
            val uri = dataSpec.uri
            val url = uri.toString()
            if (url.contains("googlevideo.com") || url.contains("youtube.com")) {
                val customUa = uri.getQueryParameter("__yt_ua") ?: when (uri.getQueryParameter("__yt_client")) {
                    "ANDROID_VR" -> NetworkModule.UA_ANDROID_VR
                    "WEB" -> NetworkModule.UA_WEB
                    else -> NetworkModule.UA_VISIONOS
                }
                dataSpec.buildUpon()
                    .setHttpRequestHeaders(
                        mapOf(
                            "Origin" to "https://www.youtube.com",
                            "Referer" to "https://www.youtube.com/",
                            "User-Agent" to customUa
                        )
                    )
                    .build()
            } else {
                dataSpec
            }
        }

        val baseMediaSourceFactory = DefaultMediaSourceFactory(resolvingDataSourceFactory)
        val customMediaSourceFactory = object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
                baseMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                return this
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
                baseMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                return this
            }

            override fun getSupportedTypes(): IntArray {
                return baseMediaSourceFactory.supportedTypes
            }

            override fun createMediaSource(mediaItem: androidx.media3.common.MediaItem): MediaSource {
                val uri = mediaItem.localConfiguration?.uri
                val audioUrl = uri?.getQueryParameter("__yt_audio_url")
                if (!audioUrl.isNullOrBlank()) {
                    val cleanVideoUri = uri.buildUpon().clearQuery()
                        .apply {
                            for (name in uri.queryParameterNames) {
                                if (name != "__yt_audio_url") {
                                    appendQueryParameter(name, uri.getQueryParameter(name))
                                }
                            }
                        }
                        .build()
                    val videoItem = mediaItem.buildUpon().setUri(cleanVideoUri).build()
                    val videoSource = baseMediaSourceFactory.createMediaSource(videoItem)

                    val audioUri = android.net.Uri.parse(audioUrl)
                    val audioItem = androidx.media3.common.MediaItem.Builder().setUri(audioUri).build()
                    val audioSource = baseMediaSourceFactory.createMediaSource(audioItem)

                    return MergingMediaSource(true, videoSource, audioSource)
                }
                return baseMediaSourceFactory.createMediaSource(mediaItem)
            }
        }

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(customMediaSourceFactory)
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
