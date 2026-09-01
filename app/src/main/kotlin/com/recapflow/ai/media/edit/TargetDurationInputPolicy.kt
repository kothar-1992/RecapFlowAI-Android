package com.recapflow.ai.media.edit

import kotlin.math.roundToInt

/** Pure validation/formatting policy for Target Duration Clips controls. */
object TargetDurationInputPolicy {
    const val MAX_MINUTES = 999
    private const val SECONDS_PER_MINUTE = 60L
    private const val MAX_SECONDS_PART = 59L

    val minimumTargetSeconds: Int
        get() = (TargetDurationClipPlanner.MIN_TARGET_DURATION_MS / 1_000L)
            .coerceAtLeast(1L)
            .toInt()

    fun parseDurationMs(minutesText: String, secondsText: String): Long? {
        val minutes = minutesText.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
        val seconds = secondsText.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
        if (minutes !in 0..MAX_MINUTES) return null
        if (seconds !in 0..59) return null

        val totalMs = (minutes * SECONDS_PER_MINUTE + seconds) * 1_000L
        return totalMs.takeIf { it >= TargetDurationClipPlanner.MIN_TARGET_DURATION_MS }
    }

    /** Largest whole-second target the slider can represent for this source. */
    fun sliderMaximumSeconds(sourceDurationMs: Long): Int {
        val sourceSeconds = sourceDurationMs.coerceAtLeast(0L) / 1_000L
        val uiMaximumSeconds = MAX_MINUTES * SECONDS_PER_MINUTE + MAX_SECONDS_PART
        return sourceSeconds.coerceAtMost(uiMaximumSeconds).toInt()
    }

    /**
     * Slider-first UX starts at the full source duration, rounded down to a whole second.
     * The user drags left to request a shorter recap; planning only happens on Generate.
     */
    fun defaultSliderTargetDurationMs(sourceDurationMs: Long): Long? {
        val seconds = sliderMaximumSeconds(sourceDurationMs)
        if (seconds < minimumTargetSeconds) return null
        return seconds * 1_000L
    }

    fun sliderValueToDurationMs(value: Float): Long? {
        val seconds = value.roundToInt()
        if (seconds < minimumTargetSeconds) return null
        return seconds * 1_000L
    }

    fun minutesPart(durationMs: Long): Int =
        (durationMs.coerceAtLeast(0L) / 60_000L).coerceAtMost(MAX_MINUTES.toLong()).toInt()

    fun secondsPart(durationMs: Long): Int =
        ((durationMs.coerceAtLeast(0L) / 1_000L) % 60L).toInt()

    /** Percentage of source time represented by the current kept-source plan. */
    fun sourceKeepPercent(sourceKeepRatio: Double): Int =
        (sourceKeepRatio.coerceIn(0.0, 1.0) * 100.0).toInt()

    /** Whole-percent target-output/source ratio for immediate slider feedback. */
    fun targetOutputPercent(targetDurationMs: Long, sourceDurationMs: Long): Int {
        if (sourceDurationMs <= 0L) return 0
        return ((targetDurationMs.toDouble() / sourceDurationMs.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }
}
