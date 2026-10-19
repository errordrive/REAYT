package com.sp0.plus.ui.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sp0.plus.data.NetworkModule
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sp0.plus.MainActivity
import com.sp0.plus.R

class PlaybackService : MediaSessionService() {

    var player: ExoPlayer? = null
        private set
    var mediaSession: MediaSession? = null
        private set

    companion object {
        const val CHANNEL_ID = "sp0_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_REPEAT = "ACTION_REPEAT"
        const val ACTION_LIKE = "ACTION_LIKE"

        @Volatile
        var instance: PlaybackService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Route streaming through the shared OkHttp client (AdGuard DoH + per-client User-Agent).
        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(NetworkModule.playerClient)
        )

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                com.sp0.plus.player.PlaybackManager.updateIsPlaying(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                com.sp0.plus.player.PlaybackManager.updatePlaybackState(playbackState)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlaybackService", "Playback error: ${error.errorCodeName}", error)
                com.sp0.plus.player.PlaybackManager.handlePlayerError(error)
            }
        })

        this.player = exoPlayer

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        try {
            setMediaNotificationProvider(
                DefaultMediaNotificationProvider.Builder(this)
                    .setChannelId(CHANNEL_ID)
                    .build()
            )
        } catch (e: Throwable) {
            android.util.Log.w("PlaybackService", "Failed to set custom notification provider: ${e.message}")
        }
    }

    fun playMediaItem(item: MediaItem) {
        val p = player ?: return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                if (item.localConfiguration?.uri != null) {
                    p.setMediaItem(item)
                    p.prepare()
                    p.play()
                } else {
                    val ex = androidx.media3.common.PlaybackException(
                        "MediaItem URI is null",
                        null,
                        androidx.media3.common.PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK
                    )
                    com.sp0.plus.player.PlaybackManager.handlePlayerError(ex)
                }
            } catch (e: Throwable) {
                android.util.Log.e("PlaybackService", "playMediaItem failed: ${e.message}", e)
            }
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: Throwable) {
            android.util.Log.w("PlaybackService", "onUpdateNotification deferred: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sp0+ media playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<SessionResult> {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedItems = mediaItems.mapNotNull { item ->
                val uri = item.localConfiguration?.uri ?: item.requestMetadata.mediaUri
                if (uri != null) {
                    item.buildUpon()
                        .setUri(uri)
                        .setRequestMetadata(
                            item.requestMetadata.buildUpon().setMediaUri(uri).build()
                        )
                        .build()
                } else {
                    null
                }
            }.toMutableList()
            return Futures.immediateFuture(updatedItems)
        }
    }
}
