package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipTransitionPolicyTest {
    @Test
    fun disabledSettingsAreTrueNoOp() {
        val ranges = listOf(TrimRange(0L, 4_000L), TrimRange(5_000L, 9_000L))

        assertTrue(
            ClipTransitionPolicy.compile(
                settings = ClipTransitionSettings(
                    enabled = false,
                    boundaries = listOf(
                        ClipTransitionBoundary(
                            leftSourceEndMs = 4_000L,
                            rightSourceStartMs = 5_000L,
                        ),
                    ),
                ),
                selectedRanges = ranges,
                transform = TransformSettings(),
            ).isEmpty(),
        )
    }

    @Test
    fun crossfadeProjectsOntoAdjacentReviewedRanges() {
        val ranges = listOf(TrimRange(0L, 4_000L), TrimRange(5_000L, 9_000L))
        val settings = ClipTransitionSettings(
            enabled = true,
            boundaries = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 4_000L,
                    rightSourceStartMs = 5_000L,
                    durationMs = 300L,
                ),
            ),
        )

        val transition = ClipTransitionPolicy.compile(
            settings = settings,
            selectedRanges = ranges,
            transform = TransformSettings(),
        ).single()

        assertEquals(0, transition.boundaryIndex)
        assertEquals(3_700_000L, transition.presentationStartUs)
        assertEquals(300_000L, transition.presentationDurationUs)
        assertEquals(300L, transition.leftSourceOverlapMs)
        assertEquals(300L, transition.rightSourceOverlapMs)
        assertEquals(300L, ClipTransitionPolicy.plannedOverlapDurationMs(settings, ranges, TransformSettings()))
    }

    @Test
    fun speedChangesSourceOverlapButNotUserFacingTransitionDuration() {
        val ranges = listOf(TrimRange(0L, 4_000L), TrimRange(5_000L, 9_000L))
        val settings = ClipTransitionSettings(
            enabled = true,
            boundaries = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 4_000L,
                    rightSourceStartMs = 5_000L,
                    durationMs = 300L,
                ),
            ),
        )
        val transform = TransformSettings(
            enabled = true,
            speedEnabled = true,
            speed = 2f,
        )

        val transition = ClipTransitionPolicy.compile(settings, ranges, transform).single()

        assertEquals(1_700_000L, transition.presentationStartUs)
        assertEquals(300_000L, transition.presentationDurationUs)
        assertEquals(600L, transition.leftSourceOverlapMs)
        assertEquals(600L, transition.rightSourceOverlapMs)
    }

    @Test
    fun multipleTransitionsAccumulateOverlapOnPresentationTimeline() {
        val ranges = listOf(
            TrimRange(0L, 3_000L),
            TrimRange(4_000L, 7_000L),
            TrimRange(8_000L, 11_000L),
        )
        val settings = ClipTransitionSettings(
            enabled = true,
            boundaries = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 3_000L,
                    rightSourceStartMs = 4_000L,
                    durationMs = 200L,
                ),
                ClipTransitionBoundary(
                    leftSourceEndMs = 7_000L,
                    rightSourceStartMs = 8_000L,
                    durationMs = 400L,
                ),
            ),
        )

        val transitions = ClipTransitionPolicy.compile(settings, ranges, TransformSettings())

        assertEquals(2, transitions.size)
        assertEquals(2_800_000L, transitions[0].presentationStartUs)
        assertEquals(5_400_000L, transitions[1].presentationStartUs)
        assertEquals(600L, ClipTransitionPolicy.plannedOverlapDurationMs(settings, ranges, TransformSettings()))
    }

    @Test
    fun missingDuplicateAndTooLongBoundariesAreRejected() {
        val ranges = listOf(TrimRange(0L, 400L), TrimRange(1_000L, 1_400L))
        val duplicate = ClipTransitionBoundary(
            leftSourceEndMs = 400L,
            rightSourceStartMs = 1_000L,
            durationMs = 400L,
        )
        val settings = ClipTransitionSettings(
            enabled = true,
            boundaries = listOf(
                duplicate,
                duplicate,
                ClipTransitionBoundary(
                    leftSourceEndMs = 9_000L,
                    rightSourceStartMs = 10_000L,
                    durationMs = 100L,
                ),
            ),
        )

        val issues = ClipTransitionPolicy.validate(settings, ranges, TransformSettings())

        assertTrue(ClipTransitionIssue.DUPLICATE_BOUNDARY in issues)
        assertTrue(ClipTransitionIssue.BOUNDARY_NOT_FOUND in issues)
        assertTrue(ClipTransitionIssue.TRANSITION_LONGER_THAN_CLIP in issues)
        assertTrue(ClipTransitionPolicy.compile(settings, ranges, TransformSettings()).isEmpty())
    }
}
