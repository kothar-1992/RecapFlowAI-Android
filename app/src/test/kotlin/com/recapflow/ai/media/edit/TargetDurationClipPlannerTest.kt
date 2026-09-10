package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetDurationClipPlannerTest {
    private val threeMinuteSource = TrimRange(0L, 180_000L)

    @Test
    fun threeMinutesToOneMinuteUsesDistributedOrderedRanges() {
        val plan = assertNotNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 60_000L,
            ),
        )

        assertTrue(plan.ranges.size > 2)
        assertEquals(threeMinuteSource.startMs, plan.ranges.first().startMs)
        assertEquals(threeMinuteSource.endMs, plan.ranges.last().endMs)
        assertEquals(60_000L, plan.requiredSourceKeepDurationMs)
        assertEquals(60_000L, plan.ranges.sumOf { it.durationMs })
        assertEquals(60_000L, plan.estimatedFinalDurationMs)
        assertTrue(AdaptiveCutCompiler.areRangesValid(plan.ranges, threeMinuteSource))
        assertTrue(plan.ranges.zipWithNext().all { (left, right) -> right.startMs > left.endMs })
    }

    @Test
    fun threeMinutesToTwoMinutesUsesDistributedOrderedRanges() {
        val plan = assertNotNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 120_000L,
            ),
        )

        assertTrue(plan.ranges.size > 2)
        assertEquals(120_000L, plan.ranges.sumOf { it.durationMs })
        assertEquals(120_000L, plan.estimatedFinalDurationMs)
        assertTrue(AdaptiveCutCompiler.areRangesValid(plan.ranges, threeMinuteSource))
    }

    @Test
    fun speedIsReconciledAgainstTheFinalTarget() {
        val transform = TransformSettings(
            enabled = true,
            speedEnabled = true,
            speed = 1.5f,
        )
        val plan = assertNotNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 60_000L,
                transform = transform,
            ),
        )

        assertEquals(90_000L, plan.requiredSourceKeepDurationMs)
        assertEquals(60_000L, plan.estimatedFinalDurationMs)
    }

    @Test
    fun introFreezeIsReservedInsideTheFinalTarget() {
        val transform = TransformSettings(
            enabled = true,
            freeze = FreezeSettings(
                enabled = true,
                durationMs = 2_000L,
            ),
        )
        val plan = assertNotNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 60_000L,
                transform = transform,
            ),
        )

        assertEquals(58_000L, plan.requiredSourceKeepDurationMs)
        assertEquals(60_000L, plan.estimatedFinalDurationMs)
    }

    @Test
    fun presentationOverlapBudgetIsReconciledInsideTheFinalTarget() {
        val plan = assertNotNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 60_000L,
                presentationOverlapBudgetMs = 300L,
            ),
        )

        assertEquals(60_300L, plan.requiredSourceKeepDurationMs)
        assertEquals(60_000L, plan.estimatedFinalDurationMs)
    }

    @Test
    fun repeatedPlanningIsDeterministic() {
        val first = TargetDurationClipPlanner.plan(
            sourceRange = threeMinuteSource,
            targetDurationMs = 60_000L,
        )
        val second = TargetDurationClipPlanner.plan(
            sourceRange = threeMinuteSource,
            targetDurationMs = 60_000L,
        )

        assertEquals(first, second)
    }

    @Test
    fun impossibleOrInvalidTargetsAreRejected() {
        assertNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 0L,
            ),
        )
        assertNull(
            TargetDurationClipPlanner.plan(
                sourceRange = threeMinuteSource,
                targetDurationMs = 100_000L,
                transform = TransformSettings(
                    enabled = true,
                    speedEnabled = true,
                    speed = 2f,
                ),
            ),
        )
    }

    @Test
    fun adaptiveCutSettingsKeepLegacyPresetModeByDefault() {
        val settings = AdaptiveCutSettings()

        assertEquals(ClipPlanningMode.PRESET_PACING, settings.mode)
        assertNull(settings.targetDurationMs)
    }
}
