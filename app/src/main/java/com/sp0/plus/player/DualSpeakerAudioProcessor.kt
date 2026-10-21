package com.sp0.plus.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * AudioProcessor that allows simultaneous audio playback from both the main bottom loudspeaker
 * and the top ear speaker (earpiece) when Dual Speaker Mode is active.
 */
class DualSpeakerAudioProcessor : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            DualSpeakerManager.onAudioFormatChanged(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
            return inputAudioFormat
        }
        return AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (DualSpeakerManager.isEnabled) {
            val dup = inputBuffer.duplicate()
            DualSpeakerManager.writeAudio(dup)
        }

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    override fun onFlush() {
        super.onFlush()
        if (!DualSpeakerManager.isEnabled) {
            DualSpeakerManager.pause()
        }
    }

    override fun onReset() {
        super.onReset()
        DualSpeakerManager.stop()
    }
}
