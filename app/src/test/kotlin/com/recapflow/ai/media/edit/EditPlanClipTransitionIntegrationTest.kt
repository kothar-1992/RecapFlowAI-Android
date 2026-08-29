package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditPlanClipTransitionIntegrationTest {
    @Test
    fun enabledCrossfadeReducesPlannedPresentationDuration() {
        val plan = basePlan(
            clipTransitions = ClipTransitionSettings(
                enabled = true,
                boundaries = listOf(
                    ClipTransitionBoundary(
                        leftSourceEndMs = 4_000L,
                        rightSourceStartMs = 5_000L,
                        durationMs = 300L,
                    ),
                ),
            ),
        )

        assertEquals(7_700L, plan.plannedDurationMs)
        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun speedKeepsCrossfadeDurationInPresentationTime() {
        val plan = basePlan(
            clipTransitions = ClipTransitionSettings(
                enabled = true,
                boundaries = listOf(
                    ClipTransitionBoundary(
                        leftSourceEndMs = 4_000L,
                        rightSourceStartMs = 5_000L,
                        durationMs = 300L,
                    ),
                ),
            ),
            transform = TransformSettings(
                enabled = true,
                speedEnabled = true,
                speed = 2f,
            ),
        )

        assertEquals(3_700L, plan.plannedDurationMs)
        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun disabledClipTransitionsPreserveExistingPlannedDuration() {
        val plan = basePlan(
            clipTransitions = ClipTransitionSettings(
                enabled = false,
                boundaries = listOf(
                    ClipTransitionBoundary(
                        leftSourceEndMs = 4_000L,
                        rightSourceStartMs = 5_000L,
                        durationMs = 300L,
                    ),
                ),
            ),
        )

        assertEquals(8_000L, plan.plannedDurationMs)
        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun validatorMapsSemanticBoundaryFailuresToEditPlanIssues() {
        val duplicateBoundary = ClipTransitionBoundary(
            leftSourceEndMs = 4_000L,
            rightSourceStartMs = 5_000L,
            durationMs = 300L,
        )
        val duplicatePlan = basePlan(
            clipTransitions = ClipTransitionSettings(
                enabled = true,
                boundaries = listOf(duplicateBoundary, duplicateBoundary),
            ),
        )
        assertTrue(
            EditPlanIssue.CLIP_TRANSITION_DUPLICATE_BOUNDARY in
                EditPlanValidator.validate(duplicatePlan),
        )

        val missingPlan = basePlan(
            clipTransitions = ClipTransitionSettings(
                enabled = true,
                boundaries = listOf(
                    ClipTransitionBoundary(
                        leftSourceEndMs = 8_500L,
                        rightSourceStartMs = 9_000L,
                        durationMs = 300L,
                    ),
                ),
            ),
        )
        assertTrue(
            EditPlanIssue.CLIP_TRANSITION_BOUNDARY_NOT_FOUND in
                EditPlanValidator.validate(missingPlan),
        )

        val invalidDurationPlan = basePlan(
            clipTransitions = ClipTransitionSettings(
                enabled = true,
                boundaries = listOf(
                    ClipTransitionBoundary(
                        leftSourceEndMs = 4_000L,
                        rightSourceStartMs = 5_000L,
                        durationMs = 100L,
                    ),
                ),
            ),
        )
        assertTrue(
            EditPlanIssue.CLIP_TRANSITION_DURATION_INVALID in
                EditPlanValidator.validate(invalidDurationPlan),
        )

        val tooLongPlan = EditPlan(
            sourcePath = "/tmp/source.mp4",
            sourceDurationMs = 3_000L,
            trimRange = TrimRange(0L, 3_000L),
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = listOf(
                    TrimRange(0L, 1_000L),
                    TrimRange(2_000L, 3_000L),
                ),
            ),
            clipTransitions = ClipTransitionSettings(
                enabled = true,
                boundaries = listOf(
                    ClipTransitionBoundary(
                        leftSourceEndMs = 1_000L,
                        rightSourceStartMs = 2_000L,
                        durationMs = 1_000L,
                    ),
                ),
            ),
            exportPreset = RenderPreset.HD_720P,
        )
        assertTrue(
            EditPlanIssue.CLIP_TRANSITION_TOO_LONG in
                EditPlanValidator.validate(tooLongPlan),
        )
    }

    private fun basePlan(
        clipTransitions: ClipTransitionSettings,
        transform: TransformSettings = TransformSettings(),
    ): EditPlan = EditPlan(
        sourcePath = "/tmp/source.mp4",
        sourceDurationMs = 10_000L,
        trimRange = TrimRange(0L, 10_000L),
        adaptiveCuts = AdaptiveCutSettings(
            enabled = true,
            reviewedRanges = listOf(
                TrimRange(0L, 4_000L),
                TrimRange(5_000L, 9_000L),
            ),
        ),
        clipTransitions = clipTransitions,
        transform = transform,
        exportPreset = RenderPreset.HD_720P,
    )
}
