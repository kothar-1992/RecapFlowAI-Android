package com.recapflow.ai.media.edit

/**
 * Bridges the target-duration planner into the canonical reviewed-clips model.
 *
 * Replanning can move source boundaries. Existing Crossfade settings therefore cannot be reused by
 * source timestamp alone. This integration preserves transition intent by adjacent range index,
 * rebinds those settings onto the newly generated ranges, and feeds the resulting validated
 * presentation-overlap budget back into the planner so the requested final duration stays
 * authoritative when Speed, Intro Freeze and active Crossfades are present.
 */
object TargetDurationClipIntegration {
    private const val MAX_RECONCILIATION_PASSES = 3

    fun generate(
        sourceRange: TrimRange,
        targetDurationMs: Long,
        currentAdaptiveCuts: AdaptiveCutSettings = AdaptiveCutSettings(),
        currentSelectedRanges: List<TrimRange> = listOf(sourceRange),
        transform: TransformSettings = TransformSettings(),
        clipTransitions: ClipTransitionSettings = ClipTransitionSettings(),
    ): TargetDurationClipReconciliation? {
        var overlapBudgetMs = 0L
        var plan = TargetDurationClipPlanner.plan(
            sourceRange = sourceRange,
            targetDurationMs = targetDurationMs,
            transform = transform,
            presentationOverlapBudgetMs = overlapBudgetMs,
        ) ?: return null
        var reboundTransitions = rebindTransitionsByIndex(
            settings = clipTransitions,
            oldRanges = currentSelectedRanges,
            newRanges = plan.ranges,
        )

        repeat(MAX_RECONCILIATION_PASSES) {
            val compiledOverlapMs = ClipTransitionPolicy.plannedOverlapDurationMs(
                settings = reboundTransitions,
                selectedRanges = plan.ranges,
                transform = transform,
            )
            if (compiledOverlapMs == overlapBudgetMs) return@repeat

            overlapBudgetMs = compiledOverlapMs
            val previousRanges = plan.ranges
            plan = TargetDurationClipPlanner.plan(
                sourceRange = sourceRange,
                targetDurationMs = targetDurationMs,
                transform = transform,
                presentationOverlapBudgetMs = overlapBudgetMs,
            ) ?: return null
            reboundTransitions = rebindTransitionsByIndex(
                settings = reboundTransitions,
                oldRanges = previousRanges,
                newRanges = plan.ranges,
            )
        }

        val finalOverlapBudgetMs = ClipTransitionPolicy.plannedOverlapDurationMs(
            settings = reboundTransitions,
            selectedRanges = plan.ranges,
            transform = transform,
        )
        if (finalOverlapBudgetMs != plan.presentationOverlapBudgetMs) {
            val previousRanges = plan.ranges
            plan = TargetDurationClipPlanner.plan(
                sourceRange = sourceRange,
                targetDurationMs = targetDurationMs,
                transform = transform,
                presentationOverlapBudgetMs = finalOverlapBudgetMs,
            ) ?: return null
            reboundTransitions = rebindTransitionsByIndex(
                settings = reboundTransitions,
                oldRanges = previousRanges,
                newRanges = plan.ranges,
            )
        }

        val finalEstimatedDurationMs = TargetDurationClipPlanner.estimateFinalDurationMs(
            ranges = plan.ranges,
            transform = transform,
            presentationOverlapBudgetMs = ClipTransitionPolicy.plannedOverlapDurationMs(
                settings = reboundTransitions,
                selectedRanges = plan.ranges,
                transform = transform,
            ),
        )
        if (
            kotlin.math.abs(finalEstimatedDurationMs - targetDurationMs) >
            TargetDurationClipPlanner.DURATION_TOLERANCE_MS
        ) {
            return null
        }

        val adaptiveCuts = currentAdaptiveCuts.copy(
            enabled = true,
            reviewedRanges = plan.ranges,
            mode = ClipPlanningMode.TARGET_DURATION,
            targetDurationMs = targetDurationMs,
        )
        return TargetDurationClipReconciliation(
            adaptiveCuts = adaptiveCuts,
            clipTransitions = reboundTransitions,
            plan = plan.copy(estimatedFinalDurationMs = finalEstimatedDurationMs),
        )
    }

    fun rebindTransitionsByIndex(
        settings: ClipTransitionSettings,
        oldRanges: List<TrimRange>,
        newRanges: List<TrimRange>,
    ): ClipTransitionSettings {
        if (!settings.enabled || settings.boundaries.isEmpty()) return settings
        if (oldRanges.size < 2 || newRanges.size < 2) {
            return settings.copy(boundaries = emptyList())
        }

        val oldBoundaryByIndex = linkedMapOf<Int, ClipTransitionBoundary>()
        settings.boundaries.forEach { boundary ->
            val oldIndex = (0 until oldRanges.lastIndex).firstOrNull { index ->
                oldRanges[index].endMs == boundary.leftSourceEndMs &&
                    oldRanges[index + 1].startMs == boundary.rightSourceStartMs
            } ?: return@forEach
            if (oldIndex !in oldBoundaryByIndex) {
                oldBoundaryByIndex[oldIndex] = boundary
            }
        }

        val rebound = oldBoundaryByIndex.mapNotNull { (index, boundary) ->
            if (index >= newRanges.lastIndex) return@mapNotNull null
            boundary.copy(
                leftSourceEndMs = newRanges[index].endMs,
                rightSourceStartMs = newRanges[index + 1].startMs,
            )
        }
        return settings.copy(boundaries = rebound)
    }
}

data class TargetDurationClipReconciliation(
    val adaptiveCuts: AdaptiveCutSettings,
    val clipTransitions: ClipTransitionSettings,
    val plan: TargetDurationClipPlan,
)
