package com.recapflow.ai.media.render

/**
 * Explicit runtime boundary for Phase 6H.1 clip transitions.
 *
 * Media3 1.10 Composition can carry multiple sequences and compositor settings, but the supported
 * Composition contract does not provide native video/audio crossfading. Until RecapFlowAI owns a
 * reviewed compositor/runtime path that is proven for both CompositionPlayer and Transformer, an
 * enabled semantic crossfade must fail explicitly instead of being silently rendered as a hard cut.
 */
object Media3ClipTransitionRuntimePolicy {
    fun isSupported(plan: Media3CompositionPlan): Boolean = plan.clipTransitions.isEmpty()

    fun requireSupported(plan: Media3CompositionPlan) {
        check(isSupported(plan)) {
            "Clip Crossfade is reviewed in EditPlan but is not enabled in the Media3 1.10 runtime yet. " +
                "Preserving the edit and refusing a silent hard-cut fallback."
        }
    }
}
