package com.recapflow.ai.media.render

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chooses the final export frame rate from the probed source instead of forcing every export to 30.
 *
 * Common fractional rates are normalized to their nominal integer values (23.976 -> 24,
 * 29.97 -> 30, 59.94 -> 60). RecapFlow does not promote a normal-frame-rate source to 60 fps;
 * the policy only normalizes metadata and caps very high source rates at 60 fps.
 */
object ExportFrameRatePolicy {
    const val FALLBACK_FRAME_RATE = 30
    const val MAX_FRAME_RATE = 60
    const val HIGH_FRAME_RATE_THRESHOLD = 48

    private val commonFrameRates = intArrayOf(24, 25, 30, 48, 50, 60)

    fun forSource(sourceFrameRate: Double): Int {
        if (!sourceFrameRate.isFinite() || sourceFrameRate <= 0.0) {
            return FALLBACK_FRAME_RATE
        }
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
