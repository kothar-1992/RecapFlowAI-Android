package com.recapflow.ai.media.edit

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Resolves the reviewed EditPlan to the nearest whole-second duration that can be reached by
 * changing only the final selected source range. Nothing is changed until the UI applies the
 * returned update explicitly.
 */
object DurationFitAdvisor {
    const val WHOLE_SECOND_MS = 1_000L

    fun assess(plan: EditPlan): DurationFitAssessment {
        val plannedDurationMs = plan.plannedDurationMs.coerceAtLeast(0L)
        if (plannedDurationMs <= 0L) {
            return DurationFitAssessment(
                plannedDurationMs = plannedDurationMs,
                suggestedDurationMs = plannedDurationMs,
                adjustmentMs = 0L,
                update = null,
            )
        }

        val floorMs = (plannedDurationMs / WHOLE_SECOND_MS) * WHOLE_SECOND_MS
        val ceilMs = if (plannedDurationMs % WHOLE_SECOND_MS == 0L) {
            plannedDurationMs
        } else {
            floorMs + WHOLE_SECOND_MS
        }
        val roundedMs = ((plannedDurationMs + WHOLE_SECOND_MS / 2L) / WHOLE_SECOND_MS) *
            WHOLE_SECOND_MS

        if (plannedDurationMs == roundedMs) {
            return DurationFitAssessment(
                plannedDurationMs = plannedDurationMs,
                suggestedDurationMs = plannedDurationMs,
                adjustmentMs = 0L,
                update = null,
            )
        }

        val candidates = listOf(roundedMs, floorMs, ceilMs)
            .distinct()
            .filter { it >= EditPlanValidator.MIN_TRIM_DURATION_MS }
            .sortedBy { abs(it - plannedDurationMs) }

        candidates.forEach { targetDurationMs ->
            val update = buildUpdate(plan, targetDurationMs)
            if (update != null) {
                return DurationFitAssessment(
                    plannedDurationMs = plannedDurationMs,
                    suggestedDurationMs = targetDurationMs,
                    adjustmentMs = targetDurationMs - plannedDurationMs,
                    update = update,
                )
            }
        }

        val nearest = candidates.firstOrNull() ?: plannedDurationMs
        return DurationFitAssessment(
            plannedDurationMs = plannedDurationMs,
            suggestedDurationMs = nearest,
            adjustmentMs = nearest - plannedDurationMs,
            update = null,
        )
    }

    private fun buildUpdate(plan: EditPlan, targetDurationMs: Long): DurationFitUpdate? {
        val freezeDurationMs = FreezeCompiler.compile(plan.transform)?.durationMs ?: 0L
        val targetMovingDurationMs = targetDurationMs - freezeDurationMs
        if (targetMovingDurationMs < 0L) return null

        val speed = SpeedCompiler.compile(plan.transform)?.multiplier ?: 1f
        val selectedRanges = AdaptiveCutCompiler.compile(plan.adaptiveCuts, plan.trimRange)
        val selectedSourceDurationMs = selectedRanges?.sumOf { it.durationMs }
            ?: plan.trimRange.durationMs
        val approximateSourceDeltaMs =
            (targetMovingDurationMs * speed).roundToLong() - selectedSourceDurationMs

        // Speed rounding can move the result by a millisecond. Try a small deterministic window
        // and accept only an update whose compiled EditPlan lands on the exact whole second.
        val candidateDeltas = buildList {
            add(approximateSourceDeltaMs)
            for (offset in 1L..4L) {
                add(approximateSourceDeltaMs - offset)
                add(approximateSourceDeltaMs + offset)
            }
        }.distinct()

        candidateDeltas.forEach { sourceDeltaMs ->
            val update = if (selectedRanges != null) {
                adjustAdaptiveRanges(plan, selectedRanges, sourceDeltaMs)
            } else {
                adjustTrimRange(plan, sourceDeltaMs)
            } ?: return@forEach

            val updatedPlan = plan.copy(
                trimRange = update.trimRange,
                adaptiveCuts = plan.adaptiveCuts.copy(
                    reviewedRanges = update.reviewedRanges ?: plan.adaptiveCuts.reviewedRanges,
                ),
            )
            if (updatedPlan.plannedDurationMs == targetDurationMs) return update
        }
        return null
    }

    private fun adjustTrimRange(plan: EditPlan, sourceDeltaMs: Long): DurationFitUpdate? {
        val newEndMs = plan.trimRange.endMs + sourceDeltaMs
        if (newEndMs !in (plan.trimRange.startMs + EditPlanValidator.MIN_TRIM_DURATION_MS)..plan.sourceDurationMs) {
            return null
        }
        return DurationFitUpdate(
            trimRange = plan.trimRange.copy(endMs = newEndMs),
            reviewedRanges = null,
        )
    }

    private fun adjustAdaptiveRanges(
        plan: EditPlan,
        ranges: List<TrimRange>,
        sourceDeltaMs: Long,
    ): DurationFitUpdate? {
        val last = ranges.lastOrNull() ?: return null
        val newEndMs = last.endMs + sourceDeltaMs
        if (newEndMs !in (last.startMs + AdaptiveCutCompiler.MIN_RANGE_DURATION_MS)..plan.trimRange.endMs) {
            return null
        }
        val updated = ranges.toMutableList().apply {
            this[lastIndex] = last.copy(endMs = newEndMs)
        }
        if (!AdaptiveCutCompiler.areRangesValid(updated, plan.trimRange)) return null
        return DurationFitUpdate(
            trimRange = plan.trimRange,
            reviewedRanges = updated,
        )
    }
}

data class DurationFitAssessment(
    val plannedDurationMs: Long,
    val suggestedDurationMs: Long,
    val adjustmentMs: Long,
    val update: DurationFitUpdate?,
) {
    val isWholeSecondAligned: Boolean
        get() = plannedDurationMs > 0L && plannedDurationMs % DurationFitAdvisor.WHOLE_SECOND_MS == 0L

    val canApply: Boolean
        get() = update != null
}

data class DurationFitUpdate(
    val trimRange: TrimRange,
    val reviewedRanges: List<TrimRange>?,
)
