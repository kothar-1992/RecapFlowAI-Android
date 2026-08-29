package com.recapflow.ai.media.edit

import kotlin.math.roundToLong

/**
 * Semantic clip-boundary transition contract for Phase 6H.1.
 *
 * This model deliberately describes the edit rather than a Media3/FFmpeg implementation. A
 * boundary is identified by the adjacent source ranges that survive Trim / reviewed Adaptive Cuts.
 * Runtime preview/export compilers can therefore consume the same transition semantics later
 * without persisting backend-specific filter names.
 */
data class ClipTransitionSettings(
    val enabled: Boolean = false,
    val boundaries: List<ClipTransitionBoundary> = emptyList(),
)

data class ClipTransitionBoundary(
    val leftSourceEndMs: Long,
    val rightSourceStartMs: Long,
    val type: ClipTransitionType = ClipTransitionType.CROSSFADE,
    val durationMs: Long = ClipTransitionPolicy.DEFAULT_DURATION_MS,
    val easing: ClipTransitionEasing = ClipTransitionEasing.EASE_IN_OUT,
    val enabled: Boolean = true,
)

enum class ClipTransitionType {
    CROSSFADE,
}

enum class ClipTransitionEasing {
    LINEAR,
    EASE_IN_OUT,
}

data class CompiledClipTransition(
    val boundaryIndex: Int,
    val leftRange: TrimRange,
    val rightRange: TrimRange,
    val type: ClipTransitionType,
    val easing: ClipTransitionEasing,
    val presentationStartUs: Long,
    val presentationDurationUs: Long,
    val leftSourceOverlapMs: Long,
    val rightSourceOverlapMs: Long,
) {
    val presentationEndUs: Long
        get() = presentationStartUs + presentationDurationUs
}

enum class ClipTransitionIssue {
    DUPLICATE_BOUNDARY,
    BOUNDARY_NOT_FOUND,
    DURATION_OUT_OF_RANGE,
    TRANSITION_LONGER_THAN_CLIP,
}

/**
 * Projects semantic clip-boundary transitions onto the post-Speed presentation timeline.
 *
 * Crossfade duration is user-facing presentation time. When Speed is active, the source overlap
 * required from each participating range is scaled by the speed multiplier so a 300 ms crossfade
 * remains 300 ms on the output timeline at 0.5x, 1x, or 2x playback speed.
 */
object ClipTransitionPolicy {
    const val MIN_DURATION_MS = 150L
    const val MAX_DURATION_MS = 1_000L
    const val DEFAULT_DURATION_MS = 300L

    fun validate(
        settings: ClipTransitionSettings,
        selectedRanges: List<TrimRange>,
        transform: TransformSettings,
    ): Set<ClipTransitionIssue> {
        if (!settings.enabled) return emptySet()

        val active = settings.boundaries.filter { it.enabled }
        val issues = linkedSetOf<ClipTransitionIssue>()
        if (active.groupBy { it.leftSourceEndMs to it.rightSourceStartMs }.any { it.value.size > 1 }) {
            issues += ClipTransitionIssue.DUPLICATE_BOUNDARY
        }

        active.forEach { boundary ->
            val adjacentIndex = findAdjacentBoundaryIndex(boundary, selectedRanges)
            if (adjacentIndex == null) {
                issues += ClipTransitionIssue.BOUNDARY_NOT_FOUND
                return@forEach
            }
            if (boundary.durationMs !in MIN_DURATION_MS..MAX_DURATION_MS) {
                issues += ClipTransitionIssue.DURATION_OUT_OF_RANGE
                return@forEach
            }

            val leftDurationMs = presentationDurationMs(selectedRanges[adjacentIndex], transform)
            val rightDurationMs = presentationDurationMs(selectedRanges[adjacentIndex + 1], transform)
            if (boundary.durationMs >= leftDurationMs || boundary.durationMs >= rightDurationMs) {
                issues += ClipTransitionIssue.TRANSITION_LONGER_THAN_CLIP
            }
        }
        return issues
    }

    fun compile(
        settings: ClipTransitionSettings,
        selectedRanges: List<TrimRange>,
        transform: TransformSettings,
    ): List<CompiledClipTransition> {
        if (!settings.enabled || selectedRanges.size < 2) return emptyList()

        val activeByBoundary = settings.boundaries
            .asSequence()
            .filter { it.enabled }
            .groupBy { it.leftSourceEndMs to it.rightSourceStartMs }

        var currentOutputEndMs = presentationDurationMs(selectedRanges.first(), transform)
        val compiled = mutableListOf<CompiledClipTransition>()

        for (index in 0 until selectedRanges.lastIndex) {
            val left = selectedRanges[index]
            val right = selectedRanges[index + 1]
            val matches = activeByBoundary[left.endMs to right.startMs].orEmpty()
            val boundary = matches.singleOrNull()
            val rightDurationMs = presentationDurationMs(right, transform)

            if (
                boundary != null &&
                boundary.durationMs in MIN_DURATION_MS..MAX_DURATION_MS &&
                boundary.durationMs < presentationDurationMs(left, transform) &&
                boundary.durationMs < rightDurationMs
            ) {
                val durationMs = boundary.durationMs
                val sourceOverlapMs = sourceOverlapDurationMs(durationMs, transform)
                compiled += CompiledClipTransition(
                    boundaryIndex = index,
                    leftRange = left,
                    rightRange = right,
                    type = boundary.type,
                    easing = boundary.easing,
                    presentationStartUs = (currentOutputEndMs - durationMs) * 1_000L,
                    presentationDurationUs = durationMs * 1_000L,
                    leftSourceOverlapMs = sourceOverlapMs,
                    rightSourceOverlapMs = sourceOverlapMs,
                )
                currentOutputEndMs += rightDurationMs - durationMs
            } else {
                currentOutputEndMs += rightDurationMs
            }
        }

        return compiled
    }

    fun plannedOverlapDurationMs(
        settings: ClipTransitionSettings,
        selectedRanges: List<TrimRange>,
        transform: TransformSettings,
    ): Long = compile(settings, selectedRanges, transform).sumOf {
        it.presentationDurationUs / 1_000L
    }

    private fun findAdjacentBoundaryIndex(
        boundary: ClipTransitionBoundary,
        selectedRanges: List<TrimRange>,
    ): Int? = (0 until selectedRanges.lastIndex).firstOrNull { index ->
        selectedRanges[index].endMs == boundary.leftSourceEndMs &&
            selectedRanges[index + 1].startMs == boundary.rightSourceStartMs
    }

    private fun presentationDurationMs(range: TrimRange, transform: TransformSettings): Long =
        SpeedCompiler.compile(transform)?.outputDurationMs(range.durationMs) ?: range.durationMs

    private fun sourceOverlapDurationMs(
        presentationDurationMs: Long,
        transform: TransformSettings,
    ): Long {
        val speed = SpeedCompiler.compile(transform)?.multiplier ?: 1f
        return (presentationDurationMs.toDouble() * speed.toDouble())
            .roundToLong()
            .coerceAtLeast(1L)
    }
}
