package com.recapflow.ai.media.edit

data class AppliedSmartCut(val before: EditPlan, val after: EditPlan, val removedTransitionCount: Int) {
    /** Undo may restore this transaction only, never overwrite a subsequent edit. */
    fun undo(current: EditPlan): EditPlan? = before.takeIf { current == after }
}

/** Atomic plan adapter for the review screen. All existing render compilers remain authoritative. */
object SmartCutIntegration {
    fun apply(current: EditPlan, analyzedPlan: EditPlan, revision: String, draft: SmartCutDraft): AppliedSmartCut? {
        if (current != analyzedPlan) return null
        val original = AdaptiveCutCompiler.compile(current.adaptiveCuts, current.trimRange) ?: listOf(current.trimRange)
        if (!draft.matches(revision, original)) return null
        val kept = draft.keptRanges
        if (kept.isEmpty() || kept.size > AdaptiveCutCompiler.MAX_REVIEWED_RANGES ||
            !AdaptiveCutCompiler.areRangesValid(kept, current.trimRange) ||
            kept.any { range -> original.none { range.startMs >= it.startMs && range.endMs <= it.endMs } }) return null

        // Preserve transition identity by actual source endpoints, not the candidate list index.
        val boundaries = kept.zipWithNext().map { (left, right) -> left.endMs to right.startMs }.toSet()
        val transitions = current.clipTransitions.copy(boundaries = current.clipTransitions.boundaries.filter {
            it.leftSourceEndMs to it.rightSourceStartMs in boundaries
        })
        // Shortened neighboring clips can invalidate even an unchanged boundary. Require review
        // of transition settings instead of silently trimming the overlap or disabling the effect.
        if (ClipTransitionPolicy.validate(transitions, kept, current.transform).isNotEmpty()) return null
        val after = current.copy(
            profile = EditProfile.ADAPTIVE,
            adaptiveCuts = current.adaptiveCuts.copy(enabled = true, reviewedRanges = kept.toList(),
                mode = ClipPlanningMode.AI_SMART_CUTS, targetDurationMs = null),
            clipTransitions = transitions,
        )
        return AppliedSmartCut(current, after, current.clipTransitions.boundaries.size - transitions.boundaries.size)
    }
}
