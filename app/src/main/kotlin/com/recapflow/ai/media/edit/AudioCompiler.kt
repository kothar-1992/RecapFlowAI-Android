package com.recapflow.ai.media.edit

data class CompiledAudio(
    val removeAudio: Boolean,
    val linearGain: Float,
    val replacement: ReplacementAudioAsset? = null,
    val replacementLinearGain: Float = AudioCompiler.UNITY_LINEAR_GAIN,
    val mixesSourceAudio: Boolean = false,
)

object AudioCompiler {
    const val MIN_LINEAR_GAIN = 0f
    const val UNITY_LINEAR_GAIN = 1f
    const val MAX_LINEAR_GAIN = 1f
    const val DEFAULT_MIX_SOURCE_LINEAR_GAIN = 0.70f
    const val DEFAULT_MIX_LINEAR_GAIN = 0.30f

    fun compile(settings: AudioSettings): CompiledAudio? = when {
        !settings.enabled -> null
        settings.policy == AudioPolicy.MUTE -> CompiledAudio(
            removeAudio = true,
            linearGain = MIN_LINEAR_GAIN,
        )
        settings.policy == AudioPolicy.KEEP_ORIGINAL &&
            settings.volume !in MIN_LINEAR_GAIN..MAX_LINEAR_GAIN -> null
        settings.policy == AudioPolicy.KEEP_ORIGINAL &&
            settings.volume == UNITY_LINEAR_GAIN -> null
        settings.policy == AudioPolicy.KEEP_ORIGINAL -> CompiledAudio(
            removeAudio = false,
            linearGain = settings.volume,
        )
        settings.policy == AudioPolicy.REPLACE &&
            settings.volume in MIN_LINEAR_GAIN..MAX_LINEAR_GAIN -> CompiledAudio(
            removeAudio = true,
            linearGain = settings.volume,
            replacement = settings.replacement,
            replacementLinearGain = settings.volume,
        )
        settings.policy == AudioPolicy.MIX &&
            settings.volume in MIN_LINEAR_GAIN..MAX_LINEAR_GAIN &&
            settings.mixVolume in MIN_LINEAR_GAIN..MAX_LINEAR_GAIN -> CompiledAudio(
            removeAudio = false,
            linearGain = settings.volume,
            replacement = settings.replacement,
            replacementLinearGain = settings.mixVolume,
            mixesSourceAudio = true,
        )
        else -> null
    }
}
