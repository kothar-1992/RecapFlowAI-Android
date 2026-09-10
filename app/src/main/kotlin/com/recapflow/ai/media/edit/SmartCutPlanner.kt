package com.recapflow.ai.media.edit

/** Evidence supplied by an analyzer; duration alone never establishes redundant speech. */
enum class SmartCutReason { PAUSE, FILLER, REDUNDANT_SPEECH, IDLE_SCENE }

data class SmartCutCandidate(
    val id: String,
    val range: TrimRange,
    val reason: SmartCutReason,
    val confidence: Double,
    val evidence: String,
    val completeThought: Boolean = false,
)

data class SmartCutPolicy(
    val minimumConfidence: Double = 0.85,
    val minimumPauseMs: Long = 1_200L,
    val retainedPauseEdgeMs: Long = 150L,
    val maximumRemovalFraction: Double = 0.6,
)

enum class SmartCutRejection {
    INVALID_RANGE, LOW_CONFIDENCE, MISSING_EVIDENCE, INCOMPLETE_THOUGHT,
    SHORT_PAUSE, PROTECTED_CONTENT, OUTSIDE_SELECTION, OVERLAPPING_CUT,
    SHORT_REMAINDER, REMOVAL_LIMIT, RANGE_LIMIT,
}

data class SmartCutDecision(
    val candidate: SmartCutCandidate,
    val removedRange: TrimRange? = null,
    val rejection: SmartCutRejection? = null,
)

data class SmartCutDraft(
    val sourceRevision: String,
    val originalRanges: List<TrimRange>,
    val keptRanges: List<TrimRange>,
    val decisions: List<SmartCutDecision>,
) {
    val removedDurationMs: Long
        get() = originalRanges.sumOf { it.durationMs } - keptRanges.sumOf { it.durationMs }

    /** A late network response must not overwrite another source or a newer manual edit. */
    fun matches(revision: String, selectedRanges: List<TrimRange>): Boolean =
        revision == sourceRevision && selectedRanges == originalRanges
}

/**
 * Converts analyzer removal proposals to chronological canonical keep ranges. It does not analyze
 * media, infer silence, change EditPlan, or render. Rejected proposals retain their source footage.
 * Source revision must identify both the media content and the analysis/edit generation.
 */
object SmartCutPlanner {
    const val MAX_CANDIDATES = 1_000

    fun draft(
        sourceRevision: String,
        sourceRange: TrimRange,
        selectedRanges: List<TrimRange>,
        candidates: List<SmartCutCandidate>,
        protectedRanges: List<TrimRange> = emptyList(),
        policy: SmartCutPolicy = SmartCutPolicy(),
    ): SmartCutDraft {
        require(sourceRevision.isNotBlank())
        require(sourceRange.startMs >= 0 && sourceRange.endMs > sourceRange.startMs)
        require(selectedRanges.isNotEmpty() && selectedRanges.size <= AdaptiveCutCompiler.MAX_REVIEWED_RANGES)
        require(AdaptiveCutCompiler.areRangesValid(selectedRanges, sourceRange))
        require(candidates.size <= MAX_CANDIDATES)
        require(candidates.all { it.id.isNotBlank() } && candidates.map { it.id }.distinct().size == candidates.size)
        require(policy.minimumConfidence.isFinite() && policy.minimumConfidence in 0.0..1.0)
        require(policy.maximumRemovalFraction.isFinite() && policy.maximumRemovalFraction in 0.0..1.0)
        require(policy.minimumPauseMs > 0 && policy.retainedPauseEdgeMs >= 0)
        require(protectedRanges.all { valid(it, sourceRange) })

        val original = selectedRanges.toList()
        var kept = original
        val totalMs = original.sumOf { it.durationMs }
        val accepted = mutableListOf<TrimRange>()
        val decisions = candidates.sortedWith(compareBy({ it.range.startMs }, { it.id })).map { candidate ->
            var cut = candidate.range
            var rejection = when {
                !valid(cut, sourceRange) -> SmartCutRejection.INVALID_RANGE
                !candidate.confidence.isFinite() || candidate.confidence !in policy.minimumConfidence..1.0 -> SmartCutRejection.LOW_CONFIDENCE
                candidate.evidence.isBlank() -> SmartCutRejection.MISSING_EVIDENCE
                candidate.reason == SmartCutReason.REDUNDANT_SPEECH && !candidate.completeThought -> SmartCutRejection.INCOMPLETE_THOUGHT
                else -> null
            }
            if (rejection == null && candidate.reason == SmartCutReason.PAUSE) {
                if (cut.durationMs < policy.minimumPauseMs || policy.retainedPauseEdgeMs >= cut.durationMs / 2) {
                    rejection = SmartCutRejection.SHORT_PAUSE
                } else {
                    cut = TrimRange(cut.startMs + policy.retainedPauseEdgeMs, cut.endMs - policy.retainedPauseEdgeMs)
                }
            }
            if (rejection == null) rejection = when {
                protectedRanges.any { overlaps(it, cut) } -> SmartCutRejection.PROTECTED_CONTENT
                original.none { contains(it, cut) } -> SmartCutRejection.OUTSIDE_SELECTION
                accepted.any { overlaps(it, cut) } -> SmartCutRejection.OVERLAPPING_CUT
                else -> null
            }
            if (rejection == null) {
                val next = kept.flatMap { range ->
                    if (!overlaps(range, cut)) listOf(range) else buildList {
                        if (range.startMs < cut.startMs) add(TrimRange(range.startMs, cut.startMs))
                        if (cut.endMs < range.endMs) add(TrimRange(cut.endMs, range.endMs))
                    }
                }
                rejection = when {
                    next.isEmpty() || next.any { it.durationMs < AdaptiveCutCompiler.MIN_RANGE_DURATION_MS } -> SmartCutRejection.SHORT_REMAINDER
                    next.size > AdaptiveCutCompiler.MAX_REVIEWED_RANGES -> SmartCutRejection.RANGE_LIMIT
                    (totalMs - next.sumOf { it.durationMs }).toDouble() / totalMs > policy.maximumRemovalFraction -> SmartCutRejection.REMOVAL_LIMIT
                    else -> null
                }
                if (rejection == null) {
                    kept = next
                    accepted += cut
                }
            }
            SmartCutDecision(candidate, if (rejection == null) cut else null, rejection)
        }
        check(AdaptiveCutCompiler.areRangesValid(kept, sourceRange))
        return SmartCutDraft(sourceRevision, original, kept.toList(), decisions)
    }

    private fun valid(range: TrimRange, source: TrimRange) =
        range.startMs >= source.startMs && range.endMs <= source.endMs && range.endMs > range.startMs

    private fun contains(outer: TrimRange, inner: TrimRange) =
        inner.startMs >= outer.startMs && inner.endMs <= outer.endMs

    private fun overlaps(a: TrimRange, b: TrimRange) = a.startMs < b.endMs && b.startMs < a.endMs
}
