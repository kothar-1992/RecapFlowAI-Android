package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.CompiledSpeed
import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.edit.VideoOverlaySettings

/**
 * One overlay fragment after absolute source time has been projected into the concatenated output.
 *
 * Adaptive Cuts can split one configured overlay window into multiple fragments. Each fragment
 * retains the matching interval in the overlay media itself so the renderer can clip rather than
 * loop or restart unexpectedly across removed source gaps.
 */
data class ProjectedVideoOverlaySegment(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val presentationStartMs: Long,
    val presentationEndMs: Long,
    val overlayMediaStartMs: Long,
    val overlayMediaEndMs: Long,
) {
    val presentationDurationMs: Long
        get() = (presentationEndMs - presentationStartMs).coerceAtLeast(0L)
}

/**
 * Phase 6G.1 timing contract for a muted picture-in-picture video overlay.
 *
 * The user window is stored in absolute source time. Trim / reviewed Adaptive Cuts select which
 * parts of that window survive. Speed is then applied to presentation time, while overlay-media
 * clipping remains expressed against the original overlay asset timeline. The integration layer
 * can apply the same playback speed to each overlay fragment so PIP motion stays synchronized with
 * the base sequence.
 */
object VideoOverlayTimelinePolicy {

    fun project(
        settings: VideoOverlaySettings,
        selectedRanges: List<TrimRange>,
        transform: TransformSettings,
    ): List<ProjectedVideoOverlaySegment> {
        val asset = settings.asset ?: return emptyList()
        if (!settings.enabled || !settings.isStructurallyValid() || selectedRanges.isEmpty()) {
            return emptyList()
        }

        // A short overlay ends naturally. A longer overlay is clipped by the configured window.
        val effectiveWindowEndMs = minOf(settings.endMs, settings.startMs + asset.durationMs)
        if (effectiveWindowEndMs <= settings.startMs) return emptyList()

        val speed = SpeedCompiler.compile(transform)
        var presentationCursorMs = 0L
        val result = mutableListOf<ProjectedVideoOverlaySegment>()

        selectedRanges.forEach { range ->
            val rangeDurationMs = range.durationMs.coerceAtLeast(0L)
            if (rangeDurationMs == 0L) return@forEach

            val intersectionStartMs = maxOf(range.startMs, settings.startMs)
            val intersectionEndMs = minOf(range.endMs, effectiveWindowEndMs)
            if (intersectionEndMs > intersectionStartMs) {
                val localStartMs = intersectionStartMs - range.startMs
                val localEndMs = intersectionEndMs - range.startMs
                result += ProjectedVideoOverlaySegment(
                    sourceStartMs = intersectionStartMs,
                    sourceEndMs = intersectionEndMs,
                    presentationStartMs = presentationCursorMs + outputDurationMs(speed, localStartMs),
                    presentationEndMs = presentationCursorMs + outputDurationMs(speed, localEndMs),
                    overlayMediaStartMs = intersectionStartMs - settings.startMs,
                    overlayMediaEndMs = intersectionEndMs - settings.startMs,
                )
            }

            presentationCursorMs += outputDurationMs(speed, rangeDurationMs)
        }

        return result
    }

    private fun outputDurationMs(speed: CompiledSpeed?, inputDurationMs: Long): Long =
        speed?.outputDurationMs(inputDurationMs) ?: inputDurationMs.coerceAtLeast(0L)
}
