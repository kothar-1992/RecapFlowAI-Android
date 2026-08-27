package com.recapflow.ai.media.render

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.recapflow.ai.media.edit.AudioCompiler
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/** Applies the shared Phase 6D.2–6D.4 gain to one decoded PCM track. */
@UnstableApi
class PcmVolumeAudioProcessor(
    private val linearGain: Float,
) : BaseAudioProcessor() {

    init {
        require(linearGain in audioCompilerRange) {
            "linearGain must be between 0 and 1"
        }
    }

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        while (inputBuffer.remaining() >= Short.SIZE_BYTES) {
            outputBuffer.putShort(scalePcm16Sample(inputBuffer.short, linearGain))
        }
        outputBuffer.flip()
    }

    companion object {
        private val audioCompilerRange =
            AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN

        internal fun scalePcm16Sample(sample: Short, linearGain: Float): Short =
            (sample.toInt() * linearGain)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
    }
}
