package com.recapflow.ai.media.render

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.recapflow.ai.media.edit.ClipTransitionPolicy
import java.nio.ByteBuffer

/**
 * Applies one clip slot's Crossfade envelope to decoded PCM after Speed has been applied.
 *
 * Each processor instance belongs to one EditedMediaItem, so its sample clock is item-local. The
 * envelope uses presentation time, matching the video compositor. This prevents overlapping audio
 * lanes from summing at unity gain through the transition window.
 */
@UnstableApi
class CrossfadePcmAudioProcessor(
    private val slot: Media3CrossfadeClipSlot,
) : BaseAudioProcessor() {

    private var sampleRate = 0
    private var channelCount = 0
    private var processedFrames = 0L

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        processedFrames = 0L
        return inputAudioFormat
    }

    @Suppress("DEPRECATION")
    override fun onFlush() {
        processedFrames = 0L
    }

    override fun onReset() {
        processedFrames = 0L
        sampleRate = 0
        channelCount = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        check(sampleRate > 0 && channelCount > 0) { "Crossfade audio processor is not configured" }
        val bytesPerFrame = channelCount * Short.SIZE_BYTES
        val wholeFrameBytes = inputBuffer.remaining() - (inputBuffer.remaining() % bytesPerFrame)
        val outputBuffer = replaceOutputBuffer(wholeFrameBytes)

        var remainingFrameBytes = wholeFrameBytes
        while (remainingFrameBytes >= bytesPerFrame) {
            val localPresentationTimeUs = processedFrames * MICROS_PER_SECOND / sampleRate
            val gain = gainAtLocalPresentationTimeUs(localPresentationTimeUs)
            repeat(channelCount) {
                outputBuffer.putShort(
                    PcmVolumeAudioProcessor.scalePcm16Sample(inputBuffer.short, gain),
                )
            }
            processedFrames += 1L
            remainingFrameBytes -= bytesPerFrame
        }
        outputBuffer.flip()
    }

    internal fun gainAtLocalPresentationTimeUs(localTimeUs: Long): Float {
        var gain = 1f
        slot.fadeIn?.let { envelope ->
            val localStartUs = envelope.startUs - slot.presentationStartUs
            val localEndUs = localStartUs + envelope.durationUs
            gain = when {
                localTimeUs <= localStartUs -> 0f
                localTimeUs >= localEndUs -> gain
                else -> ClipTransitionPolicy.easedProgress(
                    envelope.easing,
                    ((localTimeUs - localStartUs).toDouble() / envelope.durationUs.toDouble())
                        .toFloat()
                        .coerceIn(0f, 1f),
                )
            }
        }
        slot.fadeOut?.let { envelope ->
            val localStartUs = envelope.startUs - slot.presentationStartUs
            val localEndUs = localStartUs + envelope.durationUs
            val fadeOutGain = when {
                localTimeUs <= localStartUs -> 1f
                localTimeUs >= localEndUs -> 0f
                else -> 1f - ClipTransitionPolicy.easedProgress(
                    envelope.easing,
                    ((localTimeUs - localStartUs).toDouble() / envelope.durationUs.toDouble())
                        .toFloat()
                        .coerceIn(0f, 1f),
                )
            }
            gain *= fadeOutGain
        }
        return gain.coerceIn(0f, 1f)
    }

    private companion object {
        const val MICROS_PER_SECOND = 1_000_000L
    }
}
