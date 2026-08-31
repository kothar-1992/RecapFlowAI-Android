package com.recapflow.ai.media.edit

/** Pure validation/formatting policy for the Target Duration Clips mm:ss input. */
object TargetDurationInputPolicy {
    const val MAX_MINUTES = 999

    fun parseDurationMs(minutesText: String, secondsText: String): Long? {
        val minutes = minutesText.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
        val seconds = secondsText.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
        if (minutes !in 0..MAX_MINUTES) return null
        if (seconds !in 0..59) return null

        val totalMs = (minutes * 60L + seconds) * 1_000L
        return totalMs.takeIf { it >= TargetDurationClipPlanner.MIN_TARGET_DURATION_MS }
    }

    fun minutesPart(durationMs: Long): Int =
        (durationMs.coerceAtLeast(0L) / 60_000L).coerceAtMost(MAX_MINUTES.toLong()).toInt()

    fun secondsPart(durationMs: Long): Int =
        ((durationMs.coerceAtLeast(0L) / 1_000L) % 60L).toInt()

    /** Percentage of source time represented by the current kept-source plan. */
    fun sourceKeepPercent(sourceKeepRatio: Double): Int =
        (sourceKeepRatio.coerceIn(0.0, 1.0) * 100.0).toInt()
}
