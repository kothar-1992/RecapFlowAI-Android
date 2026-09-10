package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SmartCutPlannerTest {
    private val source = TrimRange(0, 20_000)
    private fun candidate(start: Long, end: Long, reason: SmartCutReason = SmartCutReason.PAUSE) =
        SmartCutCandidate("$start-$end", TrimRange(start, end), reason, 0.95, "Analyzer evidence")

    private fun draft(vararg candidates: SmartCutCandidate) = SmartCutPlanner.draft(
        "source-v1", source, listOf(source), candidates.toList(),
    )

    @Test fun trimsPauseButRetainsBreathingRoomInCanonicalKeepRanges() {
        val result = draft(candidate(4_000, 7_000))
        assertEquals(listOf(TrimRange(0, 4_150), TrimRange(6_850, 20_000)), result.keptRanges)
        assertEquals(2_700, result.removedDurationMs)
        assertEquals(result.keptRanges, AdaptiveCutCompiler.compile(
            AdaptiveCutSettings(enabled = true, reviewedRanges = result.keptRanges), source,
        ))
    }

    @Test fun longSpeechWithoutCompleteThoughtEvidenceIsKept() {
        val result = draft(candidate(2_000, 12_000, SmartCutReason.REDUNDANT_SPEECH))
        assertEquals(listOf(source), result.keptRanges)
        assertEquals(SmartCutRejection.INCOMPLETE_THOUGHT, result.decisions.single().rejection)
    }

    @Test fun completeRedundantSentenceCanBeRemoved() {
        val result = draft(candidate(2_000, 6_000, SmartCutReason.REDUNDANT_SPEECH).copy(completeThought = true))
        assertEquals(4_000, result.removedDurationMs)
    }

    @Test fun quietButImportantSceneIsProtected() {
        val result = SmartCutPlanner.draft("v1", source, listOf(source),
            listOf(candidate(3_000, 8_000, SmartCutReason.IDLE_SCENE)), listOf(TrimRange(5_000, 6_000)))
        assertEquals(SmartCutRejection.PROTECTED_CONTENT, result.decisions.single().rejection)
        assertEquals(listOf(source), result.keptRanges)
    }

    @Test fun manualExclusionsAreNeverResurrectedAndCrossGapProposalIsRejected() {
        val selected = listOf(TrimRange(0, 7_000), TrimRange(10_000, 20_000))
        val result = SmartCutPlanner.draft("v1", source, selected,
            listOf(candidate(5_000, 12_000), candidate(14_000, 17_000)))
        assertEquals(SmartCutRejection.OUTSIDE_SELECTION, result.decisions.first().rejection)
        assertEquals(listOf(TrimRange(0, 7_000), TrimRange(10_000, 14_150), TrimRange(16_850, 20_000)), result.keptRanges)
    }

    @Test fun rejectsStaleSourceAndChangedSelection() {
        val result = draft(candidate(4_000, 7_000))
        assertTrue(result.matches("source-v1", listOf(source)))
        assertFalse(result.matches("source-v2", listOf(source)))
        assertFalse(result.matches("source-v1", listOf(TrimRange(1_000, 20_000))))
    }

    @Test fun rejectsInvalidProviderRangesScoresAndEmptyEvidence() {
        val result = draft(candidate(-1, 3_000), candidate(4_000, 7_000).copy(confidence = Double.NaN),
            candidate(8_000, 11_000).copy(evidence = ""), candidate(15_000, 25_000))
        assertEquals(listOf(SmartCutRejection.INVALID_RANGE, SmartCutRejection.LOW_CONFIDENCE,
            SmartCutRejection.MISSING_EVIDENCE, SmartCutRejection.INVALID_RANGE), result.decisions.map { it.rejection })
        assertEquals(listOf(source), result.keptRanges)
    }

    @Test fun overlappingCandidatesAreNotDoubleCounted() {
        val result = draft(candidate(3_000, 6_000), candidate(4_000, 8_000))
        assertEquals(2_700, result.removedDurationMs)
        assertEquals(SmartCutRejection.OVERLAPPING_CUT, result.decisions.last().rejection)
    }

    @Test fun tinyRemainingFragmentsAreKeptByRejectingTheCut() {
        val result = draft(candidate(400, 3_000))
        assertEquals(SmartCutRejection.SHORT_REMAINDER, result.decisions.single().rejection)
        assertEquals(listOf(source), result.keptRanges)
    }

    @Test fun removalBudgetPreventsNearEmptyVideo() {
        val result = draft(candidate(1_000, 18_000, SmartCutReason.IDLE_SCENE))
        assertEquals(SmartCutRejection.REMOVAL_LIMIT, result.decisions.single().rejection)
    }

    @Test fun duplicateProposalIdsAreRejectedBeforeAnyDraft() {
        assertFailsWith<IllegalArgumentException> { draft(candidate(3_000, 6_000), candidate(3_000, 6_000)) }
    }

    @Test fun noEvidenceProducesUnchangedVideoNotPeriodicCuts() {
        assertEquals(listOf(source), draft().keptRanges)
    }
}
