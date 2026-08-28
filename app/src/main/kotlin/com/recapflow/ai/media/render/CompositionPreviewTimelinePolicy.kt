package com.recapflow.ai.media.render

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
 * reports positions on the concatenated output timeline after Trim/Adaptive Cuts and Speed. This
 * policy is the single conversion boundary between those two coordinate systems.
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

    /**
     * CompositionPlayer requires the experimental speed effect to be the first video effect. The
     * effects that follow it therefore observe presentation time after speed is applied. Convert a
     * range-local source-time overlay window to that presentation-time domain before constructing
     * preview-only blur/logo effects.
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
}
