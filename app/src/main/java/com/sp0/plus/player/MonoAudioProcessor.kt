package com.sp0.plus.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * AudioProcessor that combines Left and Right audio channels into balanced Mono.
 * When enabled, (L + R) / 2 is output to both channels so users on single-speaker
 * phones or single earbuds hear 100% of stereo-separated instruments and vocals.
 */
class MonoAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isMonoEnabled: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            // Keep identical format and channel count (2-channel PCM) to avoid reconfiguring AudioTrack,
            // while duplicating the downmixed mono signal across both channels.
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (!isMonoEnabled) {
            // Passthrough unmodified stereo
            outputBuffer.put(inputBuffer)
        } else {
            // Downmix Left + Right to mono: (L + R) / 2
            while (inputBuffer.remaining() >= 4) {
                val left = inputBuffer.short
                val right = inputBuffer.short
                val mono = ((left.toInt() + right.toInt()) / 2).toShort()
                outputBuffer.putShort(mono)
                outputBuffer.putShort(mono)
            }
        }
        outputBuffer.flip()
    }
}
