package com.recapflow.ai.media.edit

import kotlin.math.PI
import kotlin.math.sin

/**
 * Compiles the user-facing Zoom switch and mode into deterministic scale values.
 * Off is a true no-op: no video effect is added to preview or export.
 */
object ZoomCompiler {

    fun compile(settings: TransformSettings): CompiledZoom? {
        val zoom = settings.zoom
        if (!settings.enabled || !zoom.enabled || zoom.mode == ZoomMode.OFF) return null

        return when (zoom.mode) {
            ZoomMode.OFF -> null
            ZoomMode.IN -> CompiledZoom.Static(IN_SCALE)
            ZoomMode.OUT -> CompiledZoom.Static(OUT_SCALE)
            ZoomMode.ALTERNATE -> CompiledZoom.Alternate(
                minimumScale = ALTERNATE_MIN_SCALE,
                maximumScale = ALTERNATE_MAX_SCALE,
                cycleDurationUs = ALTERNATE_CYCLE_DURATION_US,
            )
        }
    }

    const val IN_SCALE = 1.15f
    const val OUT_SCALE = 0.90f
    const val ALTERNATE_MIN_SCALE = 0.90f
    const val ALTERNATE_MAX_SCALE = 1.10f
    const val ALTERNATE_CYCLE_DURATION_US = 4_000_000L
}

sealed interface CompiledZoom {
    fun scaleAt(presentationTimeUs: Long): Float

    data class Static(val scale: Float) : CompiledZoom {
        override fun scaleAt(presentationTimeUs: Long): Float = scale
    }

    data class Alternate(
        val minimumScale: Float,
        val maximumScale: Float,
        val cycleDurationUs: Long,
    ) : CompiledZoom {
        init {
            require(minimumScale > 0f && maximumScale >= minimumScale)
            require(cycleDurationUs > 0L)
        }

        override fun scaleAt(presentationTimeUs: Long): Float {
            val safeTimeUs = presentationTimeUs.coerceAtLeast(0L)
            val phase = (safeTimeUs % cycleDurationUs).toDouble() / cycleDurationUs.toDouble()
            val center = (minimumScale + maximumScale) / 2f
            val amplitude = (maximumScale - minimumScale) / 2f
            return center + amplitude * sin(phase * 2.0 * PI).toFloat()
        }
    }
}
