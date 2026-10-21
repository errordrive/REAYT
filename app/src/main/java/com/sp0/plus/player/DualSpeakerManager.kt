package com.sp0.plus.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.nio.ByteBuffer

/**
 * Engine that enables physical stereo playback on non-stereo devices:
 * - Top Earpiece speaker = Left channel
 * - Bottom Loudspeaker = Right channel
 */
object DualSpeakerManager {
    private const val TAG = "DualSpeakerManager"

    @Volatile
    var isVirtualStereoEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                stop()
            }
        }

    private var audioManager: AudioManager? = null
    private var earpieceTrack: AudioTrack? = null
    private var currentSampleRate = 44100
    private var currentVolume = 1.0f

    fun init(context: Context) {
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    @Synchronized
    fun onAudioFormatChanged(sampleRate: Int) {
        if (sampleRate > 0 && currentSampleRate != sampleRate) {
            currentSampleRate = sampleRate
            if (isVirtualStereoEnabled) {
                recreateTrack()
            }
        }
    }

    @Synchronized
    fun writeLeftChannelAudio(leftBuffer: ByteBuffer) {
        if (!isVirtualStereoEnabled) return
        val am = audioManager ?: return

        // If headphones or Bluetooth are connected, don't output to earpiece
        if (am.isWiredHeadsetOn || am.isBluetoothA2dpOn || am.isBluetoothScoOn) {
            return
        }

        if (earpieceTrack == null || earpieceTrack?.state != AudioTrack.STATE_INITIALIZED) {
            recreateTrack()
        }

        val track = earpieceTrack ?: return
        try {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            val remaining = leftBuffer.remaining()
            if (remaining > 0) {
                track.write(leftBuffer, remaining, AudioTrack.WRITE_NON_BLOCKING)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "writeLeftChannelAudio error: ${e.message}")
        }
    }

    @Synchronized
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        currentVolume = clamped
        try {
            earpieceTrack?.setVolume(clamped)
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun pause() {
        try {
            earpieceTrack?.pause()
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun stop() {
        try {
            earpieceTrack?.stop()
            earpieceTrack?.release()
        } catch (_: Throwable) {}
        earpieceTrack = null
    }

    private fun recreateTrack() {
        stop()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val am = audioManager ?: return

        val earpieceDevice = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE } ?: return

        try {
            val minBuf = AudioTrack.getMinBufferSize(currentSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val bufferSize = (minBuf * 2).coerceAtLeast(4096)

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(currentSampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setPreferredDevice(earpieceDevice)
            track.setVolume(currentVolume)
            earpieceTrack = track
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to create earpiece AudioTrack: ${e.message}")
        }
    }
}
