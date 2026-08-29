package com.recapflow.ai.media.render

/**
 * Explicit runtime boundary for Phase 6H.1 clip transitions.
 *
 * Media3 1.10 does not provide a supported native Crossfade contract. RecapFlow therefore keeps
 * reviewed Crossfade edits blocked unless the custom two-lane runtime spike is explicitly enabled.
 * This guarantees that a reviewed Crossfade can never be silently rendered as a hard cut.
 */
object Media3ClipTransitionRuntimePolicy {
    fun isSupported(
        plan: Media3CompositionPlan,
        runtimeSpikeEnabled: Boolean = false,
    ): Boolean = plan.clipTransitions.isEmpty() || runtimeSpikeEnabled

    fun requireSupported(
        plan: Media3CompositionPlan,
        runtimeSpikeEnabled: Boolean = false,
    ) {
        check(isSupported(plan, runtimeSpikeEnabled)) {
            "Clip Crossfade is reviewed in EditPlan but the experimental two-lane runtime is disabled. " +
                "Preserving the edit and refusing a silent hard-cut fallback."
        }
    }
}
