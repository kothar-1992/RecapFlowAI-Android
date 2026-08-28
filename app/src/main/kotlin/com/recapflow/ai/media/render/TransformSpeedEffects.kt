package com.recapflow.ai.media.render

import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Effects
import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformSettings

/** Paired audio/video effects created from one provider to keep export synchronized. */
@UnstableApi
data class TransformSpeedEffects(
    val audioProcessor: AudioProcessor?,
    val videoEffect: Effect,
)

@UnstableApi
object TransformSpeedEffectsFactory {
    fun forRender(
        settings: TransformSettings,
        hasAudio: Boolean,
    ): TransformSpeedEffects? {
        val compiled = SpeedCompiler.compile(settings) ?: return null
        if (!hasAudio) {
            return TransformSpeedEffects(
                audioProcessor = null,
                videoEffect = SpeedChangeEffect(compiled.multiplier),
            )
        }
        val pair = Effects.createExperimentalSpeedChangingEffect(
            ConstantSpeedProvider(compiled.multiplier),
        )
        return TransformSpeedEffects(
            audioProcessor = pair.first,
            videoEffect = pair.second,
        )
    }

    /**
     * CompositionPlayer only accepts speed-changing video effects created by
     * [Effects.createExperimentalSpeedChangingEffect]. The paired audio processor is retained only
     * when source audio is active; the video effect is always the experimental paired variant.
     */
    fun forCompositionPreview(
        settings: TransformSettings,
        hasAudio: Boolean,
    ): TransformSpeedEffects? {
        val compiled = SpeedCompiler.compile(settings) ?: return null
        val pair = Effects.createExperimentalSpeedChangingEffect(
            ConstantSpeedProvider(compiled.multiplier),
        )
        return TransformSpeedEffects(
            audioProcessor = pair.first.takeIf { hasAudio },
            videoEffect = pair.second,
        )
    }
}

@UnstableApi
private class ConstantSpeedProvider(
    private val speed: Float,
) : SpeedProvider {
    override fun getSpeed(timeUs: Long): Float = speed

    override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
}
