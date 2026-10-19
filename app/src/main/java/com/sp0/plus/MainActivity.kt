package com.sp0.plus

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sp0.plus.model.Track
import com.sp0.plus.ui.notification.PlaybackService
import com.sp0.plus.ui.screens.PlayerScreen
import com.sp0.plus.ui.theme.Sp0PlusTheme

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Sp0PlusTheme {
                var isPlaying by remember { mutableStateOf(false) }
                var currentTrack by remember {
                    mutableStateOf<Track?>(
                        Track(
                            id = "demo_1",
                            title = "Sample Audio Track",
                            artist = "Sp0+ Player",
                            durationMs = 210000L
                        )
                    )
                }
                var progressMs by remember { mutableStateOf(0L) }

                PlayerScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    progressMs = progressMs,
                    durationMs = currentTrack?.durationMs ?: 0L,
                    onPlayPauseToggle = {
                        mediaController?.let { controller ->
                            if (controller.isPlaying) {
                                controller.pause()
                                isPlaying = false
                            } else {
                                controller.play()
                                isPlaying = true
                            }
                        } ?: run {
                            isPlaying = !isPlaying
                        }
                    },
                    onSkipNext = {
                        mediaController?.seekToNextMediaItem()
                    },
                    onSkipPrevious = {
                        mediaController?.seekToPreviousMediaItem()
                    },
                    onSeek = { fraction ->
                        val duration = currentTrack?.durationMs ?: 0L
                        val targetMs = (fraction * duration).toLong()
                        progressMs = targetMs
                        mediaController?.seekTo(targetMs)
                    }
                )
            }
        }

        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // handled reactively
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
