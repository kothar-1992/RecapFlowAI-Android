package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals

class ReplacementAudioTimelineTest {
    @Test
    fun sourcePreviewIncludesFreezeAndSpeedAdjustedOffset() {
        assertEquals(
            4_000L,
            ReplacementAudioTimeline.sourcePositionMs(
                sourcePositionMs = 8_000L,
                trimRange = TrimRange(2_000L, 12_000L),
                speed = 2f,
                introFreezeMs = 1_000L,
            ),
        )
    }

    @Test
    fun sequencePreviewIncludesPriorRanges() {
        assertEquals(
            6_000L,
            ReplacementAudioTimeline.sequencePositionMs(
                ranges = listOf(TrimRange(0L, 4_000L), TrimRange(6_000L, 10_000L)),
                rangeIndex = 1,
                itemPositionMs = 2_000L,
                speed = 1f,
                introFreezeMs = 0L,
            ),
        )
    }

    @Test
    fun candidatePreviewMapsAbsoluteSourcePosition() {
        assertEquals(
            4_000L,
            ReplacementAudioTimeline.candidatePositionMs(
                ranges = listOf(TrimRange(1_000L, 3_000L), TrimRange(8_000L, 12_000L)),
                rangeIndex = 1,
                sourcePositionMs = 10_000L,
                speed = 1f,
                introFreezeMs = 0L,
            ),
        )
    }

    @Test
    fun shortReplacementLoopsAtOutputPosition() {
        assertEquals(1_250L, ReplacementAudioTimeline.loopPositionMs(9_250L, 4_000L))
    }
}
