package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure timeline mapping used by CompositionPlayer preview.
 *
 * The editor stores semantic positions on the original source timeline, while CompositionPlayer
 * reports positions on the output timeline after Trim/Adaptive Cuts, Speed and optional clip
 * overlap. This policy is the single conversion boundary between those coordinate systems.
 */
object CompositionPreviewTimelinePolicy {

    fun presentationDurationMs(settings: TransformSettings, range: TrimRange): Long =
        SpeedCompiler.compile(settings)?.outputDurationMs(range.durationMs) ?: range.durationMs

    fun sourceToOutputMs(
        sourcePositionMs: Long,
        ranges: List<TrimRange>,
        settings: TransformSettings,
        introFreezeMs: Long = 0L,
    ): Long {
        if (ranges.isEmpty()) return 0L
        val speed = SpeedCompiler.compile(settings)?.multiplier ?: SpeedCompiler.NEUTRAL_SPEED
        val source = sourcePositionMs.coerceAtLeast(0L)
        var outputOffsetMs = introFreezeMs.coerceAtLeast(0L)

        ranges.forEach { range ->
            if (source in range.startMs..range.endMs) {
                val localSourceMs = (source - range.startMs).coerceIn(0L, range.durationMs)
                return outputOffsetMs + (localSourceMs / speed).roundToLong()
            }
            if (source < range.startMs) {
                return outputOffsetMs
            }
            outputOffsetMs += presentationDurationMs(settings, range)
        }
        return outputOffsetMs
    }

    fun outputToSourceMs(
        outputPositionMs: Long,
        ranges: List<TrimRange>,
        settings: TransformSettings,
        introFreezeMs: Long = 0L,
    ): Long {
        if (ranges.isEmpty()) return 0L
        val speed = SpeedCompiler.compile(settings)?.multiplier ?: SpeedCompiler.NEUTRAL_SPEED
        val freeze = introFreezeMs.coerceAtLeast(0L)
        var output = outputPositionMs.coerceAtLeast(0L)
        if (output <= freeze) return ranges.first().startMs
        output -= freeze

        ranges.forEach { range ->
            val durationMs = presentationDurationMs(settings, range)
            if (output <= durationMs) {
                val localSourceMs = (output * speed).roundToLong().coerceIn(0L, range.durationMs)
                return range.startMs + localSourceMs
            }
            output -= durationMs
        }
        return ranges.last().endMs
    }

    /** Shared mapping entry point for the reviewed Media3 plan. */
    fun sourceToOutputMs(
        sourcePositionMs: Long,
        plan: Media3CompositionPlan,
        editPlan: EditPlan,
    ): Long {
        if (plan.clipTransitions.isEmpty()) {
            return sourceToOutputMs(
                sourcePositionMs = sourcePositionMs,
                ranges = plan.selectedRanges,
                settings = editPlan.transform,
                introFreezeMs = plan.freeze?.durationMs ?: 0L,
            )
        }
        return sourceToOutputMs(
            sourcePositionMs = sourcePositionMs,
            topology = Media3CrossfadeTopologyCompiler.compile(plan, editPlan),
        )
    }

    /** Shared reverse mapping entry point for the reviewed Media3 plan. */
    fun outputToSourceMs(
        outputPositionMs: Long,
        plan: Media3CompositionPlan,
        editPlan: EditPlan,
    ): Long {
        if (plan.clipTransitions.isEmpty()) {
            return outputToSourceMs(
                outputPositionMs = outputPositionMs,
                ranges = plan.selectedRanges,
                settings = editPlan.transform,
                introFreezeMs = plan.freeze?.durationMs ?: 0L,
            )
        }
        return outputToSourceMs(
            outputPositionMs = outputPositionMs,
            topology = Media3CrossfadeTopologyCompiler.compile(plan, editPlan),
        )
    }

    /**
     * Maps one source position into its overlapping Crossfade slot. The mapping uses the slot's
     * reviewed presentation duration, so it remains consistent with Speed rounding in the topology.
     */
    fun sourceToOutputMs(
        sourcePositionMs: Long,
        topology: Media3CrossfadeTopology,
    ): Long {
        if (topology.slots.isEmpty()) return 0L
        val ranges = topology.slots.map(Media3CrossfadeClipSlot::sourceRange)
        val selectedSourceMs = nearestSelectedSourcePosition(sourcePositionMs, ranges)
        val slot = topology.slots.firstOrNull {
            selectedSourceMs in it.sourceRange.startMs..it.sourceRange.endMs
        } ?: return 0L
        val range = slot.sourceRange
        val localSourceMs = (selectedSourceMs - range.startMs).coerceIn(0L, range.durationMs)
        val localPresentationUs = if (range.durationMs <= 0L) {
            0L
        } else {
            (localSourceMs.toDouble() / range.durationMs.toDouble() * slot.presentationDurationUs)
                .roundToLong()
        }
        return usToMs(slot.presentationStartUs + localPresentationUs)
    }

    /**
     * Maps one Crossfade presentation position back to the visually dominant source clip. During
     * the overlap window lane 1's compositor alpha decides which simultaneously active clip owns the
     * editor's single source-time cursor; this avoids a discontinuity at the hard-cut boundary.
     */
    fun outputToSourceMs(
        outputPositionMs: Long,
        topology: Media3CrossfadeTopology,
    ): Long {
        if (topology.slots.isEmpty()) return 0L
        val presentationUs = (outputPositionMs.coerceAtLeast(0L) * 1_000L)
            .coerceAtMost(topology.totalDurationUs)
        if (presentationUs <= topology.freezeDurationUs) {
            return topology.slots.first().sourceRange.startMs
        }
        if (presentationUs >= topology.totalDurationUs) {
            return topology.slots.maxBy(Media3CrossfadeClipSlot::presentationEndUs)
                .sourceRange.endMs
        }

        val active = topology.slots.filter { slot ->
            presentationUs >= slot.presentationStartUs && presentationUs < slot.presentationEndUs
        }
        val slot = when (active.size) {
            0 -> topology.slots.minBy { candidate ->
                when {
                    presentationUs < candidate.presentationStartUs ->
                        candidate.presentationStartUs - presentationUs
                    presentationUs >= candidate.presentationEndUs ->
                        presentationUs - candidate.presentationEndUs
                    else -> 0L
                }
            }
            1 -> active.single()
            else -> {
                val lane1Alpha = topology.overlayLaneAlpha(presentationUs)
                val dominantLane = if (lane1Alpha >= 0.5f) 1 else 0
                active.firstOrNull { it.lane == dominantLane } ?: active.first()
            }
        }
        val localPresentationUs = (presentationUs - slot.presentationStartUs)
            .coerceIn(0L, slot.presentationDurationUs)
        val localSourceMs = if (slot.presentationDurationUs <= 0L) {
            0L
        } else {
            (localPresentationUs.toDouble() / slot.presentationDurationUs.toDouble() *
                slot.sourceRange.durationMs.toDouble()).roundToLong()
        }
        return slot.sourceRange.startMs +
            localSourceMs.coerceIn(0L, slot.sourceRange.durationMs)
    }

    /**
     * CompositionPlayer requires the experimental speed effect to be the first video effect. The
     * effects that follow it therefore observe presentation time after speed is applied. Convert a
     * range-local source-time overlay window and all source-anchored logo animation timing into that
     * presentation-time domain before constructing preview-only blur/logo effects.
     */
    fun projectOverlayWindowsToPresentationTime(
        overlays: OverlaySettings,
        settings: TransformSettings,
    ): OverlaySettings {
        val speed = SpeedCompiler.compile(settings)?.multiplier ?: return overlays
        fun scaled(timeMs: Long): Long = (timeMs.coerceAtLeast(0L) / speed).roundToLong()
        return overlays.copy(
            sourceSubtitleBlur = overlays.sourceSubtitleBlur.copy(
                startMs = scaled(overlays.sourceSubtitleBlur.startMs),
                endMs = scaled(overlays.sourceSubtitleBlur.endMs),
            ),
            image = overlays.image.copy(
                startMs = scaled(overlays.image.startMs),
                endMs = scaled(overlays.image.endMs),
                animation = overlays.image.animation.copy(
                    durationMs = scaled(overlays.image.animation.durationMs).coerceAtLeast(1L),
                    periodMs = scaled(overlays.image.animation.periodMs).coerceAtLeast(1L),
                    phaseOffsetMs = scaled(overlays.image.animation.phaseOffsetMs),
                ),
            ),
        )
    }

    /** Returns the nearest selected source boundary when a source position falls inside a cut gap. */
    fun nearestSelectedSourcePosition(sourcePositionMs: Long, ranges: List<TrimRange>): Long {
        if (ranges.isEmpty()) return sourcePositionMs.coerceAtLeast(0L)
        ranges.firstOrNull { sourcePositionMs in it.startMs..it.endMs }?.let {
            return sourcePositionMs.coerceIn(it.startMs, it.endMs)
        }
        return ranges
            .flatMap { listOf(it.startMs, it.endMs) }
            .minByOrNull { abs(it - sourcePositionMs) }
            ?: ranges.first().startMs
    }

    private fun usToMs(timeUs: Long): Long = (timeUs / 1_000.0).roundToLong()
}
