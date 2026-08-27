package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DurationFitAdvisorTest {
    @Test
    fun trimPlanSuggestsAndBuildsExactNearestSecond() {
        val plan = plan(
            sourceDurationMs = 352_000L,
            trimRange = TrimRange(0L, 293_154L),
        )

        val assessment = DurationFitAdvisor.assess(plan)

        assertEquals(293_154L, assessment.plannedDurationMs)
        assertEquals(293_000L, assessment.suggestedDurationMs)
        assertEquals(-154L, assessment.adjustmentMs)
        val update = assertNotNull(assessment.update)
        assertEquals(293_000L, plan.copy(trimRange = update.trimRange).plannedDurationMs)
    }

    @Test
    fun adaptivePlanChangesOnlyTheFinalReviewedRange() {
        val ranges = listOf(
            TrimRange(0L, 4_000L),
            TrimRange(5_000L, 9_154L),
        )
        val plan = plan(
            sourceDurationMs = 12_000L,
            trimRange = TrimRange(0L, 12_000L),
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = ranges,
            ),
        )

        val update = assertNotNull(DurationFitAdvisor.assess(plan).update)

        assertEquals(plan.trimRange, update.trimRange)
        assertEquals(ranges.first(), update.reviewedRanges?.first())
        assertEquals(TrimRange(5_000L, 9_000L), update.reviewedRanges?.last())
    }

    @Test
    fun fullSourceFallsBackToClosestFeasibleLowerSecond() {
        val plan = plan(
            sourceDurationMs = 10_700L,
            trimRange = TrimRange(0L, 10_700L),
        )

        val assessment = DurationFitAdvisor.assess(plan)

        assertEquals(10_000L, assessment.suggestedDurationMs)
        assertEquals(-700L, assessment.adjustmentMs)
        assertTrue(assessment.canApply)
    }

    @Test
    fun speedAndFreezeStillResolveToAnExactWholeSecond() {
        val plan = plan(
            sourceDurationMs = 20_000L,
            trimRange = TrimRange(0L, 10_193L),
            transform = TransformSettings(
                enabled = true,
                speedEnabled = true,
                speed = 1.25f,
                freeze = FreezeSettings(enabled = true, durationMs = 2_000L),
            ),
        )

        val assessment = DurationFitAdvisor.assess(plan)
        val update = assertNotNull(assessment.update)
        val updatedPlan = plan.copy(trimRange = update.trimRange)

        assertEquals(10_000L, assessment.suggestedDurationMs)
        assertEquals(10_000L, updatedPlan.plannedDurationMs)
    }

    @Test
    fun alignedPlanHasNoMutation() {
        val assessment = DurationFitAdvisor.assess(
            plan(sourceDurationMs = 10_000L, trimRange = TrimRange(0L, 10_000L)),
        )

        assertTrue(assessment.isWholeSecondAligned)
        assertFalse(assessment.canApply)
        assertNull(assessment.update)
    }

    private fun plan(
        sourceDurationMs: Long,
        trimRange: TrimRange,
        adaptiveCuts: AdaptiveCutSettings = AdaptiveCutSettings(),
        transform: TransformSettings = TransformSettings(),
    ) = EditPlan(
        sourcePath = "/tmp/source.mp4",
        sourceDurationMs = sourceDurationMs,
        trimRange = trimRange,
        adaptiveCuts = adaptiveCuts,
        transform = transform,
        exportPreset = RenderPreset.FULL_HD_1080P,
    )
}
