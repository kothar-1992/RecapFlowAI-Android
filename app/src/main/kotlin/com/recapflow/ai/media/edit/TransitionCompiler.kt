package com.recapflow.ai.media.edit

import kotlin.math.min
import kotlin.math.roundToLong

/** Compiles one reversible fade-to/from-black operation for the selected source clip. */
object TransitionCompiler {
    const val DEFAULT_DURATION_MS = 1_000L
    val supportedDurationsMs = setOf(500L, 1_000L, 1_500L)

    fun compile(
        settings: TransformSettings,
        sourceDurationMs: Long,
    ): CompiledTransition? {
        val transition = settings.transition
        if (!settings.enabled || !transition.enabled || transition.mode == TransitionMode.OFF) {
            return null
        }
        if (transition.durationMs !in supportedDurationsMs || sourceDurationMs <= 0L) return null

        // The fade matrix runs before the export speed effect and ExoPlayer applies
        // preview speed at playback. Scale the source-time span so the user-facing
        // transition duration remains constant in output/wall-clock time.
        val speed = SpeedCompiler.compile(settings)?.multiplier ?: 1f
        val sourceFadeDurationUs = (transition.durationMs * speed * 1_000.0)
            .roundToLong()
            .coerceAtLeast(1L)
        return CompiledTransition(
            mode = transition.mode,
            sourceDurationUs = sourceDurationMs * 1_000L,
            sourceFadeDurationUs = sourceFadeDurationUs,
        )
    }
}

data class CompiledTransition(
    val mode: TransitionMode,
    val sourceDurationUs: Long,
    val sourceFadeDurationUs: Long,
) {
    fun gainAt(relativeTimeUs: Long): Float {
        val safeTimeUs = relativeTimeUs.coerceIn(0L, sourceDurationUs)
        val fadeInGain = (safeTimeUs.toDouble() / sourceFadeDurationUs.toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()
        val fadeOutGain = (
            (sourceDurationUs - safeTimeUs).toDouble() / sourceFadeDurationUs.toDouble()
        )
            .coerceIn(0.0, 1.0)
            .toFloat()
        return when (mode) {
            TransitionMode.OFF -> 1f
            TransitionMode.FADE_IN -> fadeInGain
            TransitionMode.FADE_OUT -> fadeOutGain
            TransitionMode.FADE_IN_OUT -> min(fadeInGain, fadeOutGain)
        }
    }
}
