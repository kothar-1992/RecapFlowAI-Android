package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange

/**
 * Maps Media3 sequence timestamps back to the clipped item's local timeline for time-gated
 * overlays. Media3 adds the presentation duration of preceding items before the GlEffect chain,
 * while RecapFlow stores blur/logo windows against source time and projects them to each clip.
 */
object CompositionOverlayTimelinePolicy {
    fun presentationDurationUs(settings: TransformSettings, range: TrimRange): Long =
        (SpeedCompiler.compile(settings)?.outputDurationMs(range.durationMs) ?: range.durationMs) *
            1_000L

    /** Offset passed to overlay effects after their absolute window is projected to clip-local time. */
    fun localEffectTimeOffsetUs(compositionOffsetUs: Long): Long = -compositionOffsetUs
}
