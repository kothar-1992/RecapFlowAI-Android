package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveCutCompilerTest {
    @Test
    fun disabledSettingsAreANoOp() {
        assertNull(
            AdaptiveCutCompiler.compile(
                AdaptiveCutSettings(
                    enabled = false,
                    reviewedRanges = listOf(TrimRange(0L, 4_000L)),
                ),
                TrimRange(0L, 10_000L),
            ),
        )
    }

    @Test
    fun draftIsOrderedAndPreservesTheEnding() {
        val trim = TrimRange(2_000L, 22_000L)
        val ranges = AdaptiveCutDraftEngine.generate(trim, AdaptiveCutPreset.BALANCED)

        assertTrue(ranges.size > 1)
        assertEquals(trim.startMs, ranges.first().startMs)
        assertEquals(trim.endMs, ranges.last().endMs)
        assertTrue(AdaptiveCutCompiler.areRangesValid(ranges, trim))
        assertTrue(ranges.sumOf { it.durationMs } < trim.durationMs)
    }

    @Test
    fun approvedRangesCompileWithoutReordering() {
        val trim = TrimRange(0L, 12_000L)
        val reviewed = listOf(
            TrimRange(0L, 4_000L),
            TrimRange(5_000L, 9_000L),
            TrimRange(10_000L, 12_000L),
        )

        assertEquals(
            reviewed,
            AdaptiveCutCompiler.compile(
                AdaptiveCutSettings(enabled = true, reviewedRanges = reviewed),
                trim,
            ),
        )
    }

    @Test
    fun overlappingRangesAreRejected() {
        val trim = TrimRange(0L, 12_000L)
        val reviewed = listOf(
            TrimRange(0L, 5_000L),
            TrimRange(4_000L, 8_000L),
        )

        assertNull(
            AdaptiveCutCompiler.compile(
                AdaptiveCutSettings(enabled = true, reviewedRanges = reviewed),
                trim,
            ),
        )
    }
}
