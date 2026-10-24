package com.sp0.plus.ui.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sp0.plus.data.NetworkModule
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.player.DualSpeakerAudioProcessor
import com.sp0.plus.player.DualSpeakerManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    var player: ExoPlayer? = null
        private set
    var mediaSession: MediaSession? = null
        private set

    var equalizer: Equalizer? = null
        private set
    var bassBoost: BassBoost? = null
        private set

    val dualSpeakerAudioProcessor = DualSpeakerAudioProcessor()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "sp0_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PREV    = "sp0.ACTION_PREV"
        const val ACTION_NEXT    = "sp0.ACTION_NEXT"
        const val ACTION_LIKE    = "sp0.ACTION_LIKE"
        const val ACTION_REPEAT  = "sp0.ACTION_REPEAT"

        val PRESET_BANDS = mapOf(
            "Flat (Lossless)"  to shortArrayOf(0, 0, 0, 0, 0),
            "Bass Boost"       to shortArrayOf(600, 400, 200, 0, 0),
            "Vocal Booster"    to shortArrayOf(-200, 0, 400, 400, 200),
            "Electronic / EDM" to shortArrayOf(400, 200, 0, 200, 500),
            "Rock & Metal"     to shortArrayOf(300, 100, -100, 200, 300),
            "Acoustic"         to shortArrayOf(200, 100, 0, 100, 200),
            "Custom"           to shortArrayOf(0, 0, 0, 0, 0)
        )

        @Volatile
        var instance: PlaybackService? = null
            private set
    }

    fun applyEqSettings(preset: String, bassOn: Boolean, customBands: ShortArray? = null) {
        try {
            val eq = equalizer ?: return
            val gains = if (preset == "Custom" && customBands != null && customBands.size >= 5) {
                customBands
            } else {
                PRESET_BANDS[preset] ?: shortArrayOf(0, 0, 0, 0, 0)
            }
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until minOf(5, numBands)) {
                try {
                    eq.setBandLevel(i.toShort(), gains[i])
                } catch (_: Throwable) {}
            }
            try {
                bassBoost?.setStrength(if (bassOn) 800.toShort() else 0.toShort())
            } catch (_: Throwable) {}
        } catch (e: Throwable) {
            android.util.Log.w("PlaybackService", "applyEqSettings error: ${e.message}")
        }
    }

    private fun initAudioEffects(audioSessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null

        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            applyEqSettings(
                SettingsManager.equalizerPreset.value,
                SettingsManager.bassBoostEnabled.value,
                SettingsManager.equalizerBands.value
            )
        } catch (e: Throwable) {
            android.util.Log.w("PlaybackService", "Failed to init Equalizer/BassBoost for session $audioSessionId: ${e.message}")
        }
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

        DualSpeakerManager.init(this)
        DualSpeakerManager.isVirtualStereoEnabled = SettingsManager.bothSpeakersEnabled.value

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(dualSpeakerAudioProcessor))
                    .build()
            }
        }

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
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

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    initAudioEffects(audioSessionId)
                }
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

        val commandButtons = listOf(
            CommandButton.Builder()
                .setDisplayName("Like")
                .setSessionCommand(SessionCommand(ACTION_LIKE, android.os.Bundle.EMPTY))
                .setIconResId(R.drawable.ic_add)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Repeat")
                .setSessionCommand(SessionCommand(ACTION_REPEAT, android.os.Bundle.EMPTY))
                .setIconResId(R.drawable.ic_repeat)
                .build()
        )
        mediaSession?.setCustomLayout(commandButtons)

        serviceScope.launch {
            combine(
                SettingsManager.equalizerPreset,
                SettingsManager.bassBoostEnabled,
                SettingsManager.equalizerBands
            ) { preset, bassOn, customBands ->
                Triple(preset, bassOn, customBands)
            }.collect { (preset, bassOn, customBands) ->
                applyEqSettings(preset, bassOn, customBands)
            }
        }

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

    fun playMediaItem(item: MediaItem, token: Long? = null) {
        val p = player ?: return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (token != null && com.sp0.plus.player.PlaybackManager.currentPlaybackToken != token) return@post
            try {
                if (item.localConfiguration?.uri != null) {
                    p.setMediaItem(item)
                    p.prepare()
                    p.play()
                } else {
                    android.util.Log.w("PlaybackService", "MediaItem URI is null; cancelling play")
                    com.sp0.plus.player.PlaybackManager.onStreamUnavailable("Unable to stream track (audio URI unavailable)")
                }
            } catch (e: Throwable) {
                android.util.Log.e("PlaybackService", "playMediaItem failed: ${e.message}", e)
                com.sp0.plus.player.PlaybackManager.onStreamUnavailable("Playback error: ${e.message}")
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
        serviceScope.cancel()
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_PREV, android.os.Bundle.EMPTY))
                .add(SessionCommand(ACTION_NEXT, android.os.Bundle.EMPTY))
                .add(SessionCommand(ACTION_LIKE, android.os.Bundle.EMPTY))
                .add(SessionCommand(ACTION_REPEAT, android.os.Bundle.EMPTY))
                .build()
            val availablePlayerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    com.sp0.plus.player.PlaybackManager.skipNext()
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    com.sp0.plus.player.PlaybackManager.skipPrevious()
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_PREV -> com.sp0.plus.player.PlaybackManager.skipPrevious()
                ACTION_NEXT -> com.sp0.plus.player.PlaybackManager.skipNext()
                ACTION_LIKE -> com.sp0.plus.player.PlaybackManager.toggleLike()
                ACTION_REPEAT -> com.sp0.plus.player.PlaybackManager.toggleRepeat()
            }
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
