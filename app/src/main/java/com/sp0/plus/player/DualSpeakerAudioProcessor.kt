package com.sp0.plus.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AudioProcessor that splits stereo PCM:
 * - Left channel is routed to the top Earpiece speaker.
 * - Right channel is routed to the bottom Loudspeaker.
 * This turns non-stereo single-speaker phones into true physical stereo devices.
 */
class DualSpeakerAudioProcessor : BaseAudioProcessor() {

    private var leftBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            DualSpeakerManager.onAudioFormatChanged(inputAudioFormat.sampleRate)
            return inputAudioFormat
        }
        return AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!DualSpeakerManager.isVirtualStereoEnabled) {
            // Normal passthrough
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Stereo split mode:
        // Left channel goes to Earpiece speaker (mono PCM 16-bit)
        // Right channel goes to main Loudspeaker (duplicated across stereo L/R so loudspeaker outputs pure Right)
        val numSamples = remaining / 4 // 4 bytes per stereo 16-bit frame (L: 2 bytes, R: 2 bytes)
        val leftBytesCount = numSamples * 2

        if (leftBuffer.capacity() < leftBytesCount) {
            leftBuffer = ByteBuffer.allocateDirect(leftBytesCount).order(ByteOrder.LITTLE_ENDIAN)
        }
        leftBuffer.clear()

        val outputBuffer = replaceOutputBuffer(remaining)

        while (inputBuffer.remaining() >= 4) {
            val left = inputBuffer.short
            val right = inputBuffer.short

            // Earpiece gets Left channel
            leftBuffer.putShort(left)

            // Main loudspeaker gets Right channel
            outputBuffer.putShort(right)
            outputBuffer.putShort(right)
        }

        leftBuffer.flip()
        outputBuffer.flip()

        DualSpeakerManager.writeLeftChannelAudio(leftBuffer)
    }

    override fun onFlush() {
        super.onFlush()
        if (!DualSpeakerManager.isVirtualStereoEnabled) {
            DualSpeakerManager.pause()
        }
    }

    override fun onReset() {
        super.onReset()
        DualSpeakerManager.stop()
        leftBuffer = AudioProcessor.EMPTY_BUFFER
    }
}
