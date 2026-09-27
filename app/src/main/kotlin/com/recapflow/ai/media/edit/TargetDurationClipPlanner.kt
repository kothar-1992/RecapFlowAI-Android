package com.recapflow.ai.media.edit

import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Deterministic source-range planner for the user-facing Target Duration Clips workflow.
 *
 * The planner works only with timeline semantics. It never reads media, never renders an
 * intermediate file, and never mutates the source. Callers provide the desired final presentation
 * duration plus any already-known presentation overlap budget (for example reviewed Crossfades).
 * Speed and Intro Freeze are reconciled here so the generated kept-source duration targets the
 * requested final output duration rather than the raw sum of selected clips.
 */
object TargetDurationClipPlanner {
    const val MIN_TARGET_DURATION_MS = 2_000L
    const val PREFERRED_CLIP_DURATION_MS = 4_000L
    const val MIN_GAP_DURATION_MS = 500L
    const val MAX_GENERATED_RANGES = 60
    const val DURATION_TOLERANCE_MS = 250L

    fun plan(
        sourceRange: TrimRange,
        targetDurationMs: Long,
        transform: TransformSettings = TransformSettings(),
        presentationOverlapBudgetMs: Long = 0L,
    ): TargetDurationClipPlan? {
        val sourceDurationMs = sourceRange.durationMs
        if (sourceRange.startMs < 0L) return null
        if (sourceDurationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS) return null
        if (targetDurationMs < MIN_TARGET_DURATION_MS) return null
        if (presentationOverlapBudgetMs < 0L) return null

        val freezeDurationMs = FreezeCompiler.compile(transform)?.durationMs ?: 0L
        val editablePresentationMs = targetDurationMs - freezeDurationMs + presentationOverlapBudgetMs
        if (editablePresentationMs <= 0L) return null

        val speedMultiplier = SpeedCompiler.compile(transform)?.multiplier ?: 1f
        val requiredSourceKeepDurationMs =
            (editablePresentationMs.toDouble() * speedMultiplier.toDouble()).roundToLong()

        if (requiredSourceKeepDurationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS) return null
        if (requiredSourceKeepDurationMs > sourceDurationMs) return null
        if (
            requiredSourceKeepDurationMs < sourceDurationMs &&
            requiredSourceKeepDurationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS * 2L
        ) {
            return null
        }

        val ranges = distributeRanges(
            sourceRange = sourceRange,
            keepDurationMs = requiredSourceKeepDurationMs,
        )
        if (!AdaptiveCutCompiler.areRangesValid(ranges, sourceRange)) return null

        val estimatedFinalDurationMs = estimateFinalDurationMs(
            ranges = ranges,
            transform = transform,
            presentationOverlapBudgetMs = presentationOverlapBudgetMs,
        )
        if (kotlin.math.abs(estimatedFinalDurationMs - targetDurationMs) > DURATION_TOLERANCE_MS) {
            return null
        }

        return TargetDurationClipPlan(
            ranges = ranges,
            requestedTargetDurationMs = targetDurationMs,
            requiredSourceKeepDurationMs = requiredSourceKeepDurationMs,
            estimatedFinalDurationMs = estimatedFinalDurationMs,
            sourceKeepRatio = requiredSourceKeepDurationMs.toDouble() / sourceDurationMs.toDouble(),
            presentationOverlapBudgetMs = presentationOverlapBudgetMs,
        )
    }

    fun estimateFinalDurationMs(
        ranges: List<TrimRange>,
        transform: TransformSettings = TransformSettings(),
        presentationOverlapBudgetMs: Long = 0L,
    ): Long {
        val selectedSourceDurationMs = ranges.sumOf { it.durationMs }.coerceAtLeast(0L)
        val presentationDurationMs = SpeedCompiler.compile(transform)
            ?.outputDurationMs(selectedSourceDurationMs)
            ?: selectedSourceDurationMs
        val freezeDurationMs = FreezeCompiler.compile(transform)?.durationMs ?: 0L
        return presentationDurationMs - presentationOverlapBudgetMs.coerceAtLeast(0L) + freezeDurationMs
    }

    /**
     * Longest final duration this source can produce at the given transform, or null when even the
     * minimum target is out of reach.
     *
     * plan() fails with no explanation when the requested target needs more source than exists,
     * which happens whenever the user speeds the video up: a 2x target needs twice as much kept
     * source to fill the same output duration. The UI uses this to name the real ceiling and offer
     * to lower the target, instead of reporting a bare "cannot be reconciled".
     */
    fun maximumAchievableDurationMs(
        sourceRange: TrimRange,
        transform: TransformSettings = TransformSettings(),
        presentationOverlapBudgetMs: Long = 0L,
    ): Long? {
        if (sourceRange.startMs < 0L) return null
        if (sourceRange.durationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS) return null
        val speedMultiplier = SpeedCompiler.compile(transform)?.multiplier ?: 1f
        val freezeDurationMs = FreezeCompiler.compile(transform)?.durationMs ?: 0L
        val editablePresentationMs =
            SpeedCompiler.compile(transform)
                ?.outputDurationMs(sourceRange.durationMs)
                ?.minus(freezeDurationMs)
                ?.plus(presentationOverlapBudgetMs.coerceAtLeast(0L))
            ?: (sourceRange.durationMs - freezeDurationMs + presentationOverlapBudgetMs.coerceAtLeast(0L))
        if (editablePresentationMs < MIN_TARGET_DURATION_MS) return null
        return editablePresentationMs
    }

    /**
     * Why plan() returned null, in a form the UI can act on. Kept separate from plan() so the
     * planner stays a pure function with a single success path.
     */
    fun unreachableReason(
        sourceRange: TrimRange,
        targetDurationMs: Long,
        transform: TransformSettings = TransformSettings(),
        presentationOverlapBudgetMs: Long = 0L,
    ): TargetDurationUnreachableReason? {
        if (sourceRange.startMs < 0L) return TargetDurationUnreachableReason.INVALID_SOURCE
        if (sourceRange.durationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS) {
            return TargetDurationUnreachableReason.SOURCE_TOO_SHORT
        }
        if (targetDurationMs < MIN_TARGET_DURATION_MS) {
            return TargetDurationUnreachableReason.TARGET_TOO_SHORT
        }
        val freezeDurationMs = FreezeCompiler.compile(transform)?.durationMs ?: 0L
        val editablePresentationMs =
            targetDurationMs - freezeDurationMs + presentationOverlapBudgetMs
        if (editablePresentationMs <= 0L) return TargetDurationUnreachableReason.TARGET_TOO_SHORT
        val speedMultiplier = SpeedCompiler.compile(transform)?.multiplier ?: 1f
        val requiredSourceKeepDurationMs =
            (editablePresentationMs.toDouble() * speedMultiplier.toDouble()).roundToLong()
        if (requiredSourceKeepDurationMs > sourceRange.durationMs) {
            return TargetDurationUnreachableReason.SPEED_NEEDS_MORE_SOURCE
        }
        if (
            requiredSourceKeepDurationMs < sourceRange.durationMs &&
            requiredSourceKeepDurationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS * 2L
        ) {
            return TargetDurationUnreachableReason.TOO_FEW_CLIPS
        }
        return null
    }

    private fun distributeRanges(
        sourceRange: TrimRange,
        keepDurationMs: Long,
    ): List<TrimRange> {
        if (keepDurationMs >= sourceRange.durationMs) return listOf(sourceRange)

        val clipCount = chooseClipCount(
            sourceDurationMs = sourceRange.durationMs,
            keepDurationMs = keepDurationMs,
        )
        val totalGapDurationMs = sourceRange.durationMs - keepDurationMs
        val keepDurations = splitEvenly(keepDurationMs, clipCount)
        val gapDurations = splitEvenly(totalGapDurationMs, clipCount - 1)

        val ranges = ArrayList<TrimRange>(clipCount)
        var cursorMs = sourceRange.startMs
        for (index in 0 until clipCount) {
            val endMs = cursorMs + keepDurations[index]
            ranges += TrimRange(cursorMs, endMs)
            if (index < clipCount - 1) {
                cursorMs = endMs + gapDurations[index]
            }
        }
        return ranges
    }

    private fun chooseClipCount(sourceDurationMs: Long, keepDurationMs: Long): Int {
        if (keepDurationMs >= sourceDurationMs) return 1

        val maxByMinimumClip =
            (keepDurationMs / AdaptiveCutCompiler.MIN_RANGE_DURATION_MS).toInt().coerceAtLeast(1)
        if (maxByMinimumClip < 2) return 1

        val desired = ceil(
            keepDurationMs.toDouble() / PREFERRED_CLIP_DURATION_MS.toDouble(),
        ).toInt().coerceAtLeast(2)
        val totalGapDurationMs = sourceDurationMs - keepDurationMs
        val maxByPreferredGap =
            ((totalGapDurationMs / MIN_GAP_DURATION_MS) + 1L).toInt().coerceAtLeast(2)

        return minOf(
            desired,
            maxByMinimumClip,
            maxByPreferredGap,
            MAX_GENERATED_RANGES,
        ).coerceAtLeast(2)
    }

    private fun splitEvenly(totalMs: Long, parts: Int): LongArray {
        if (parts <= 0) return LongArray(0)
        val base = totalMs / parts
        val remainder = (totalMs % parts).toInt()
        return LongArray(parts) { index ->
            base + if (index < remainder) 1L else 0L
        }
    }
}

/** Why a requested target duration cannot be reconciled with the current source and transform. */
enum class TargetDurationUnreachableReason {
    INVALID_SOURCE,
    SOURCE_TOO_SHORT,
    TARGET_TOO_SHORT,
    /** The target needs more kept source than exists, which is what speeding the video up causes. */
    SPEED_NEEDS_MORE_SOURCE,
    /** The target maps to fewer than two minimum-length clips, so no usable cut exists. */
    TOO_FEW_CLIPS,
}

data class TargetDurationClipPlan(
    val ranges: List<TrimRange>,
    val requestedTargetDurationMs: Long,
    val requiredSourceKeepDurationMs: Long,
    val estimatedFinalDurationMs: Long,
    val sourceKeepRatio: Double,
    val presentationOverlapBudgetMs: Long,
) {
    val durationErrorMs: Long
        get() = estimatedFinalDurationMs - requestedTargetDurationMs
}
