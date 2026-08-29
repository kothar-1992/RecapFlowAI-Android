package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.ClipTransitionEasing
import com.recapflow.ai.media.edit.ClipTransitionPolicy
import com.recapflow.ai.media.edit.CompiledClipTransition
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.TrimRange

/**
 * Backend-independent two-lane schedule for the experimental Phase 6H.1 Crossfade runtime.
 *
 * Media3 sequences do not overlap items inside one sequence. A clip-boundary Crossfade therefore
 * needs two alternating video lanes so the outgoing and incoming clips can coexist during the
 * transition window. This compiler owns only schedule semantics; it does not claim the current
 * Media3 runtime supports the resulting crossfade yet.
 */
data class Media3CrossfadeTopology(
    val slots: List<Media3CrossfadeClipSlot>,
    val freezeDurationUs: Long,
    val totalDurationUs: Long,
) {
    val laneCount: Int
        get() = if (slots.any { it.lane == 1 }) 2 else 1

    fun slotsForLane(lane: Int): List<Media3CrossfadeClipSlot> =
        slots.filter { it.lane == lane }

    /**
     * Visual alpha for compositor lane 1. Lane 0 remains the opaque background; alternating lanes
     * mean fading lane 1 in or out produces the same A/B blend at every Crossfade boundary.
     */
    fun overlayLaneAlpha(presentationTimeUs: Long): Float {
        val slot = slotsForLane(1).firstOrNull {
            presentationTimeUs >= it.presentationStartUs && presentationTimeUs < it.presentationEndUs
        } ?: return 0f

        slot.fadeIn?.let { fade ->
            if (presentationTimeUs < fade.endUs) {
                return ClipTransitionPolicy.easedProgress(
                    fade.easing,
                    fade.progressAt(presentationTimeUs),
                )
            }
        }
        slot.fadeOut?.let { fade ->
            if (presentationTimeUs >= fade.startUs) {
                return 1f - ClipTransitionPolicy.easedProgress(
                    fade.easing,
                    fade.progressAt(presentationTimeUs),
                )
            }
        }
        return 1f
    }
}

data class Media3CrossfadeClipSlot(
    val rangeIndex: Int,
    val lane: Int,
    val sourceRange: TrimRange,
    val presentationStartUs: Long,
    val presentationDurationUs: Long,
    val fadeIn: Media3CrossfadeEnvelope?,
    val fadeOut: Media3CrossfadeEnvelope?,
) {
    val presentationEndUs: Long
        get() = presentationStartUs + presentationDurationUs
}

data class Media3CrossfadeEnvelope(
    val startUs: Long,
    val durationUs: Long,
    val easing: ClipTransitionEasing,
) {
    val endUs: Long
        get() = startUs + durationUs

    fun progressAt(presentationTimeUs: Long): Float {
        if (durationUs <= 0L) return 1f
        return ((presentationTimeUs - startUs).toDouble() / durationUs.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
    }
}

object Media3CrossfadeTopologyCompiler {
    fun compile(plan: Media3CompositionPlan, editPlan: EditPlan): Media3CrossfadeTopology {
        require(plan.selectedRanges.isNotEmpty()) { "Crossfade topology requires at least one clip" }

        val transitionByBoundary = plan.clipTransitions.associateBy(CompiledClipTransition::boundaryIndex)
        require(transitionByBoundary.size == plan.clipTransitions.size) {
            "Crossfade topology contains duplicate compiled boundary indexes"
        }

        val freezeDurationUs = plan.freeze?.durationMs?.times(1_000L) ?: 0L
        val slots = mutableListOf<Media3CrossfadeClipSlot>()
        var currentLane = 0
        var currentStartUs = freezeDurationUs

        plan.selectedRanges.forEachIndexed { rangeIndex, range ->
            val incoming = transitionByBoundary[rangeIndex - 1]
            if (incoming != null) {
                require(incoming.boundaryIndex == rangeIndex - 1) {
                    "Incoming Crossfade boundary does not match clip index"
                }
                currentLane = 1 - currentLane
            }

            val presentationDurationUs = CompositionOverlayTimelinePolicy.presentationDurationUs(
                editPlan.transform,
                range,
            )
            val outgoing = transitionByBoundary[rangeIndex]
            val fadeIn = incoming?.let { transition ->
                Media3CrossfadeEnvelope(
                    startUs = currentStartUs,
                    durationUs = transition.presentationDurationUs,
                    easing = transition.easing,
                )
            }
            val fadeOut = outgoing?.let { transition ->
                Media3CrossfadeEnvelope(
                    startUs = currentStartUs + presentationDurationUs - transition.presentationDurationUs,
                    durationUs = transition.presentationDurationUs,
                    easing = transition.easing,
                )
            }

            require(fadeIn == null || fadeOut == null || fadeIn.endUs <= fadeOut.startUs) {
                "Adjacent Crossfades overlap inside clip index $rangeIndex"
            }

            slots += Media3CrossfadeClipSlot(
                rangeIndex = rangeIndex,
                lane = currentLane,
                sourceRange = range,
                presentationStartUs = currentStartUs,
                presentationDurationUs = presentationDurationUs,
                fadeIn = fadeIn,
                fadeOut = fadeOut,
            )

            currentStartUs += presentationDurationUs - (outgoing?.presentationDurationUs ?: 0L)
        }

        // A lane maps directly to one future EditedMediaItemSequence. Items on the same lane may
        // touch or have gaps, but they must never overlap each other.
        for (lane in 0..1) {
            val laneSlots = slots.filter { it.lane == lane }
            laneSlots.zipWithNext().forEach { (left, right) ->
                require(right.presentationStartUs >= left.presentationEndUs) {
                    "Crossfade lane $lane overlaps itself between clip ${left.rangeIndex} and ${right.rangeIndex}"
                }
            }
        }

        val totalDurationUs = slots.maxOf { it.presentationEndUs }
        require(totalDurationUs == plan.plannedDurationMs * 1_000L) {
            "Crossfade topology duration $totalDurationUs does not match reviewed plan ${plan.plannedDurationMs * 1_000L}"
        }

        return Media3CrossfadeTopology(
            slots = slots,
            freezeDurationUs = freezeDurationUs,
            totalDurationUs = totalDurationUs,
        )
    }
}
