package com.recapflow.ai.media.edit

/** Validates and compiles user-reviewed keep ranges without changing their story order. */
object AdaptiveCutCompiler {
    const val MIN_RANGE_DURATION_MS = 1_000L
    const val MAX_REVIEWED_RANGES = 120

    fun compile(settings: AdaptiveCutSettings, trimRange: TrimRange): List<TrimRange>? {
        if (!settings.enabled) return null
        val ranges = settings.reviewedRanges
        if (ranges.isEmpty() || ranges.size > MAX_REVIEWED_RANGES) return null
        if (!areRangesValid(ranges, trimRange)) return null
        return ranges.toList()
    }

    fun areRangesValid(ranges: List<TrimRange>, trimRange: TrimRange): Boolean {
        var previousEndMs = trimRange.startMs
        return ranges.all { range ->
            val valid = range.startMs >= trimRange.startMs &&
                range.endMs <= trimRange.endMs &&
                range.durationMs >= MIN_RANGE_DURATION_MS &&
                range.startMs >= previousEndMs
            previousEndMs = range.endMs
            valid
        }
    }
}

/** Creates a transparent pacing draft. It is not scene or AI analysis. */
object AdaptiveCutDraftEngine {
    private const val ENDING_RESERVE_MS = 2_000L
    private const val TARGET_MAX_RANGES = 60

    fun generate(trimRange: TrimRange, preset: AdaptiveCutPreset): List<TrimRange> {
        if (trimRange.durationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS) return emptyList()

        val effectiveKeepMs = effectiveKeepWindow(trimRange.durationMs, preset)
        val ranges = mutableListOf<TrimRange>()
        var cursorMs = trimRange.startMs
        while (cursorMs < trimRange.endMs) {
            val keepEndMs = (cursorMs + effectiveKeepMs).coerceAtMost(trimRange.endMs)
            if (keepEndMs - cursorMs >= AdaptiveCutCompiler.MIN_RANGE_DURATION_MS) {
                ranges += TrimRange(cursorMs, keepEndMs)
            }
            cursorMs = keepEndMs + preset.skipWindowMs
        }

        preserveEnding(ranges, trimRange)
        return mergeTouchingRanges(ranges)
    }

    private fun effectiveKeepWindow(durationMs: Long, preset: AdaptiveCutPreset): Long {
        val baseCycleMs = preset.keepWindowMs + preset.skipWindowMs
        val estimatedRanges = (durationMs + baseCycleMs - 1L) / baseCycleMs
        if (estimatedRanges <= TARGET_MAX_RANGES) return preset.keepWindowMs

        val availableKeepMs = (durationMs - TARGET_MAX_RANGES * preset.skipWindowMs)
            .coerceAtLeast(TARGET_MAX_RANGES * AdaptiveCutCompiler.MIN_RANGE_DURATION_MS)
        return ((availableKeepMs + TARGET_MAX_RANGES - 1L) / TARGET_MAX_RANGES)
            .coerceAtLeast(preset.keepWindowMs)
    }

    private fun preserveEnding(ranges: MutableList<TrimRange>, trimRange: TrimRange) {
        if (ranges.isEmpty()) {
            ranges += trimRange
            return
        }
        val finalRange = ranges.last()
        if (finalRange.endMs == trimRange.endMs) return

        val endingStartMs = (trimRange.endMs - ENDING_RESERVE_MS)
            .coerceAtLeast(trimRange.startMs)
        if (endingStartMs <= finalRange.endMs) {
            ranges[ranges.lastIndex] = finalRange.copy(endMs = trimRange.endMs)
        } else {
            ranges += TrimRange(endingStartMs, trimRange.endMs)
        }
    }

    private fun mergeTouchingRanges(ranges: List<TrimRange>): List<TrimRange> {
        if (ranges.isEmpty()) return emptyList()
        val merged = mutableListOf(ranges.first())
        ranges.drop(1).forEach { range ->
            val previous = merged.last()
            if (range.startMs <= previous.endMs) {
                merged[merged.lastIndex] = previous.copy(endMs = maxOf(previous.endMs, range.endMs))
            } else {
                merged += range
            }
        }
        return merged
    }
}
