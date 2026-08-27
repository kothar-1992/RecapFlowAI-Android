package com.recapflow.ai.media.render

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Produces a tiny, bounded seek used only to invalidate a paused ExoPlayer video frame.
 *
 * Realtime blur/logo shaders read mutable state once per rendered frame. While playback is paused,
 * changing a slider updates that state but the decoder may keep presenting the already-rendered
 * texture. A short seek forces one fresh frame through the existing Media3 effect graph without
 * rebuilding the graph and without creating any intermediate render file.
 */
object PausedPreviewRefreshPolicy {

    private const val DEFAULT_FRAME_RATE = 30.0
    private const val MIN_NUDGE_MS = 34L
    private const val MAX_NUDGE_MS = 84L

    fun nudgeMs(frameRate: Double): Long {
        val fps = frameRate.takeIf { it.isFinite() && it > 0.0 } ?: DEFAULT_FRAME_RATE
        // Two source frames is large enough to avoid landing on the same decoded frame on common
        // 24/25/30/50/60 fps sources while remaining visually negligible in the editor.
        return ((2_000.0 / fps).roundToLong()).coerceIn(MIN_NUDGE_MS, MAX_NUDGE_MS)
    }

    fun refreshTargetMs(
        anchorMs: Long,
        durationMs: Long,
        frameRate: Double,
        preferForward: Boolean,
    ): Long {
        val safeDuration = durationMs.coerceAtLeast(0L)
        val anchor = anchorMs.coerceIn(0L, safeDuration)
        val nudge = nudgeMs(frameRate)
        val forward = (anchor + nudge).coerceAtMost(safeDuration)
        val backward = (anchor - nudge).coerceAtLeast(0L)

        return when {
            preferForward && forward != anchor -> forward
            !preferForward && backward != anchor -> backward
            forward != anchor -> forward
            backward != anchor -> backward
            else -> anchor
        }
    }

    fun mayRestoreAnchor(
        anchorMs: Long,
        currentPositionMs: Long,
        frameRate: Double,
    ): Boolean {
        // Do not snap the user back if the timeline was intentionally scrubbed while a refresh
        // pulse was pending. The automatic pulse itself stays within one nudge from the anchor.
        val tolerance = nudgeMs(frameRate) + 8L
        return abs(currentPositionMs - anchorMs) <= tolerance
    }
}
