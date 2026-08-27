package com.recapflow.ai.media.render

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.recapflow.ai.media.edit.AudioCompiler
import java.nio.ByteBuffer

/**
 * Normalizes a Mix input to signed 16-bit stereo while applying its independent gain.
 *
 * Media3 requires concurrent Composition audio sequences to expose the same PCM channel
 * count. Mono is duplicated, stereo is preserved, and uncommon multi-channel inputs are
 * conservatively folded to centered stereo for this mobile Mix gate.
 */
@UnstableApi
class StereoPcmMixAudioProcessor(
    private val linearGain: Float,
) : BaseAudioProcessor() {

    private var inputChannelCount = 0

    init {
        require(linearGain in AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN) {
            "linearGain must be between 0 and 1"
        }
    }

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        if (
            inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount !in MIN_CHANNEL_COUNT..MAX_CHANNEL_COUNT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputChannelCount = inputAudioFormat.channelCount
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            OUTPUT_CHANNEL_COUNT,
            C.ENCODING_PCM_16BIT,
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerInputFrame = inputChannelCount * Short.SIZE_BYTES
        check(bytesPerInputFrame > 0 && inputBuffer.remaining() % bytesPerInputFrame == 0) {
            "Mix PCM input must contain complete audio frames"
        }
        val frameCount = inputBuffer.remaining() / bytesPerInputFrame
        val outputBuffer = replaceOutputBuffer(
            frameCount * OUTPUT_CHANNEL_COUNT * Short.SIZE_BYTES,
        )
        val samples = ShortArray(inputChannelCount)
        repeat(frameCount) {
            for (channel in samples.indices) samples[channel] = inputBuffer.short
            val (left, right) = mixFrameToStereo(samples, linearGain)
            outputBuffer.putShort(left)
            outputBuffer.putShort(right)
        }
        outputBuffer.flip()
    }

    companion object {
        private const val MIN_CHANNEL_COUNT = 1
        private const val MAX_CHANNEL_COUNT = 8
        private const val OUTPUT_CHANNEL_COUNT = 2

        internal fun mixFrameToStereo(
            samples: ShortArray,
            linearGain: Float,
        ): Pair<Short, Short> {
            require(samples.isNotEmpty())
            val left: Short
            val right: Short
            when (samples.size) {
                1 -> {
                    left = samples[0]
                    right = samples[0]
                }
                2 -> {
                    left = samples[0]
                    right = samples[1]
                }
                else -> {
                    val centered = (samples.sumOf { it.toInt() } / samples.size)
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                    left = centered
                    right = centered
                }
            }
            return PcmVolumeAudioProcessor.scalePcm16Sample(left, linearGain) to
                PcmVolumeAudioProcessor.scalePcm16Sample(right, linearGain)
        }
    }
}
