package com.recapflow.ai.media.edit

import kotlin.math.roundToLong

/** Deterministic mapping from source/sequence preview clocks to rendered output time. */
object ReplacementAudioTimeline {
    fun sourcePositionMs(
        sourcePositionMs: Long,
        trimRange: TrimRange,
        speed: Float,
        introFreezeMs: Long,
    ): Long {
        val insideTrimMs = (sourcePositionMs - trimRange.startMs)
            .coerceIn(0L, trimRange.durationMs)
        return introFreezeMs.coerceAtLeast(0L) + scaleDuration(insideTrimMs, speed)
    }

    fun candidatePositionMs(
        ranges: List<TrimRange>,
        rangeIndex: Int,
        sourcePositionMs: Long,
        speed: Float,
        introFreezeMs: Long,
    ): Long {
        val boundedIndex = rangeIndex.coerceIn(0, (ranges.size - 1).coerceAtLeast(0))
        val selectedRange = ranges.getOrNull(boundedIndex) ?: return introFreezeMs.coerceAtLeast(0L)
        val priorDurationMs = ranges.take(boundedIndex)
            .sumOf { range -> scaleDuration(range.durationMs, speed) }
        val insideRangeMs = (sourcePositionMs - selectedRange.startMs)
            .coerceIn(0L, selectedRange.durationMs)
        return introFreezeMs.coerceAtLeast(0L) + priorDurationMs +
            scaleDuration(insideRangeMs, speed)
    }

    fun sequencePositionMs(
        ranges: List<TrimRange>,
        rangeIndex: Int,
        itemPositionMs: Long,
        speed: Float,
        introFreezeMs: Long,
    ): Long {
        val boundedIndex = rangeIndex.coerceIn(0, (ranges.size - 1).coerceAtLeast(0))
        val selectedRange = ranges.getOrNull(boundedIndex) ?: return introFreezeMs.coerceAtLeast(0L)
        val priorDurationMs = ranges.take(boundedIndex)
            .sumOf { range -> scaleDuration(range.durationMs, speed) }
        return introFreezeMs.coerceAtLeast(0L) + priorDurationMs +
            scaleDuration(itemPositionMs.coerceIn(0L, selectedRange.durationMs), speed)
    }

    fun loopPositionMs(outputPositionMs: Long, assetDurationMs: Long): Long {
        if (assetDurationMs <= 0L) return 0L
        return outputPositionMs.coerceAtLeast(0L) % assetDurationMs
    }

    private fun scaleDuration(durationMs: Long, speed: Float): Long =
        (durationMs.coerceAtLeast(0L) / speed.coerceAtLeast(MIN_SPEED)).roundToLong()

    private const val MIN_SPEED = 0.01f
}
