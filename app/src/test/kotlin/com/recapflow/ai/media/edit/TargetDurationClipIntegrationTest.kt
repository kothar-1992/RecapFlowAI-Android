package com.recapflow.ai.media.edit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TargetDurationClipIntegrationTest {
    private val source = TrimRange(0L, 180_000L)

    @Test
    fun generationProducesCanonicalTargetDurationSettings() {
        val result = assertNotNull(
            TargetDurationClipIntegration.generate(
                sourceRange = source,
                targetDurationMs = 60_000L,
            ),
        )

        assertTrue(result.adaptiveCuts.enabled)
        assertEquals(ClipPlanningMode.TARGET_DURATION, result.adaptiveCuts.mode)
        assertEquals(60_000L, result.adaptiveCuts.targetDurationMs)
        assertEquals(result.plan.ranges, result.adaptiveCuts.reviewedRanges)
        assertEquals(source.startMs, result.plan.ranges.first().startMs)
        assertEquals(source.endMs, result.plan.ranges.last().endMs)
        assertTrue(AdaptiveCutCompiler.areRangesValid(result.plan.ranges, source))
        assertTrue(abs(result.plan.estimatedFinalDurationMs - 60_000L) <= 250L)
    }

    @Test
    fun speedAndFreezeStillResolveToRequestedFinalDuration() {
        val transform = TransformSettings(
            enabled = true,
            speedEnabled = true,
            speed = 1.5f,
            freeze = FreezeSettings(enabled = true, durationMs = 2_000L),
        )

        val result = assertNotNull(
            TargetDurationClipIntegration.generate(
                sourceRange = source,
                targetDurationMs = 60_000L,
                transform = transform,
            ),
        )

        assertTrue(abs(result.plan.estimatedFinalDurationMs - 60_000L) <= 250L)
        assertTrue(result.plan.requiredSourceKeepDurationMs > 60_000L)
    }

    @Test
    fun existingCrossfadeIntentIsReboundByBoundaryIndex() {
        val initial = assertNotNull(
            TargetDurationClipPlanner.plan(
                sourceRange = source,
                targetDurationMs = 60_000L,
            ),
        )
        val oldRanges = initial.ranges
        assertTrue(oldRanges.size >= 3)

        val transitions = ClipTransitionSettings(
            enabled = true,
            boundaries = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = oldRanges[0].endMs,
                    rightSourceStartMs = oldRanges[1].startMs,
                    durationMs = 300L,
                ),
                ClipTransitionBoundary(
                    leftSourceEndMs = oldRanges[1].endMs,
                    rightSourceStartMs = oldRanges[2].startMs,
                    durationMs = 300L,
                ),
            ),
        )
        val transform = TransformSettings(
            enabled = true,
            speedEnabled = true,
            speed = 1.25f,
        )

        val result = assertNotNull(
            TargetDurationClipIntegration.generate(
                sourceRange = source,
                targetDurationMs = 60_000L,
                currentSelectedRanges = oldRanges,
                transform = transform,
                clipTransitions = transitions,
            ),
        )

        assertEquals(2, result.clipTransitions.boundaries.size)
        assertEquals(
            result.plan.ranges[0].endMs,
            result.clipTransitions.boundaries[0].leftSourceEndMs,
        )
        assertEquals(
            result.plan.ranges[1].startMs,
            result.clipTransitions.boundaries[0].rightSourceStartMs,
        )
        assertEquals(
            600L,
            ClipTransitionPolicy.plannedOverlapDurationMs(
                settings = result.clipTransitions,
                selectedRanges = result.plan.ranges,
                transform = transform,
            ),
        )
        assertTrue(abs(result.plan.estimatedFinalDurationMs - 60_000L) <= 250L)
    }

    @Test
    fun staleUnmatchedTransitionIdentityIsDroppedInsteadOfGuessed() {
        val settings = ClipTransitionSettings(
            enabled = true,
            boundaries = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 12_345L,
                    rightSourceStartMs = 13_456L,
                ),
            ),
        )
        val oldRanges = listOf(
            TrimRange(0L, 10_000L),
            TrimRange(20_000L, 30_000L),
        )
        val newRanges = listOf(
            TrimRange(0L, 8_000L),
            TrimRange(12_000L, 20_000L),
        )

        val rebound = TargetDurationClipIntegration.rebindTransitionsByIndex(
            settings = settings,
            oldRanges = oldRanges,
            newRanges = newRanges,
        )

        assertTrue(rebound.boundaries.isEmpty())
    }

    @Test
    fun integrationIsDeterministic() {
        val first = assertNotNull(
            TargetDurationClipIntegration.generate(source, 120_000L),
        )
        val second = assertNotNull(
            TargetDurationClipIntegration.generate(source, 120_000L),
        )

        assertEquals(first, second)
    }
}
