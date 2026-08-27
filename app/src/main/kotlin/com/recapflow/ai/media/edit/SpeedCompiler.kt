package com.recapflow.ai.media.edit

import kotlin.math.roundToLong

/** Compiles the user-facing constant playback speed into a deterministic edit operation. */
object SpeedCompiler {
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 2f
    const val NEUTRAL_SPEED = 1f

    val supportedPresets = setOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    fun compile(settings: TransformSettings): CompiledSpeed? {
        if (!settings.enabled || !settings.speedEnabled) return null
        if (settings.speed !in MIN_SPEED..MAX_SPEED) return null
        if (settings.speed == NEUTRAL_SPEED) return null
        return CompiledSpeed(settings.speed)
    }
}

data class CompiledSpeed(
    val multiplier: Float,
) {
    fun outputDurationMs(inputDurationMs: Long): Long =
        (inputDurationMs.coerceAtLeast(0L) / multiplier).roundToLong()
}
