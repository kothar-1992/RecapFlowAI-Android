package com.recapflow.ai.media.render

import kotlin.math.abs
import kotlin.math.roundToInt

/** Final-export FPS policy derived from source metadata; preview remains separately budgeted. */
object ExportFrameRatePolicy {
    const val FALLBACK_FRAME_RATE = 30
    const val MAX_FRAME_RATE = 60
    const val HIGH_FRAME_RATE_THRESHOLD = 48

    private val commonFrameRates = intArrayOf(24, 25, 30, 48, 50, 60)

    fun forSource(sourceFrameRate: Double): Int {
        if (!sourceFrameRate.isFinite() || sourceFrameRate <= 0.0) return FALLBACK_FRAME_RATE
        val capped = sourceFrameRate.coerceAtMost(MAX_FRAME_RATE.toDouble())
        val nearestCommon = commonFrameRates.minByOrNull { abs(capped - it) }
        if (nearestCommon != null && abs(capped - nearestCommon) <= COMMON_RATE_TOLERANCE) {
            return nearestCommon
        }
        return capped.roundToInt().coerceIn(1, MAX_FRAME_RATE)
    }

    fun isHighFrameRate(frameRate: Int): Boolean = frameRate >= HIGH_FRAME_RATE_THRESHOLD

    private const val COMMON_RATE_TOLERANCE = 0.75
}
