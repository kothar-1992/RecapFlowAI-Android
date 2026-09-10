package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutSettings
import com.recapflow.ai.media.edit.ClipTransitionBoundary
import com.recapflow.ai.media.edit.ClipTransitionEasing
import com.recapflow.ai.media.edit.ClipTransitionSettings
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.EditPlanIssue
import com.recapflow.ai.media.edit.EditPlanValidator
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Media3CrossfadeTopologyTest {
    @Test
    fun singleCrossfadeAlternatesLanesAndMatchesReviewedDuration() {
        val editPlan = editPlan(
            ranges = listOf(
                TrimRange(0L, 4_000L),
                TrimRange(5_000L, 9_000L),
            ),
            transitions = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 4_000L,
                    rightSourceStartMs = 5_000L,
                    durationMs = 300L,
                ),
            ),
        )
        val plan = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        val topology = Media3CrossfadeTopologyCompiler.compile(plan, editPlan)

        assertEquals(2, topology.laneCount)
        assertEquals(0, topology.slots[0].lane)
        assertEquals(1, topology.slots[1].lane)
        assertEquals(0L, topology.slots[0].presentationStartUs)
        assertEquals(3_700_000L, topology.slots[1].presentationStartUs)
        assertEquals(300_000L, topology.slots[0].fadeOut?.durationUs)
        assertEquals(300_000L, topology.slots[1].fadeIn?.durationUs)
        assertEquals(7_700_000L, topology.totalDurationUs)
        assertEquals(editPlan.plannedDurationMs * 1_000L, topology.totalDurationUs)
    }

    @Test
    fun freezeAndSpeedShiftScheduleWithoutChangingPresentationCrossfadeDuration() {
        val editPlan = editPlan(
            ranges = listOf(
                TrimRange(0L, 4_000L),
                TrimRange(5_000L, 9_000L),
            ),
            transitions = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 4_000L,
                    rightSourceStartMs = 5_000L,
                    durationMs = 300L,
                ),
            ),
            transform = TransformSettings(
                enabled = true,
                freeze = FreezeSettings(enabled = true, durationMs = 1_000L),
                speedEnabled = true,
                speed = 2f,
            ),
        )
        val plan = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        val topology = Media3CrossfadeTopologyCompiler.compile(plan, editPlan)

        assertEquals(1_000_000L, topology.freezeDurationUs)
        assertEquals(1_000_000L, topology.slots[0].presentationStartUs)
        assertEquals(2_700_000L, topology.slots[1].presentationStartUs)
        assertEquals(300_000L, topology.slots[1].fadeIn?.durationUs)
        assertEquals(4_700_000L, topology.totalDurationUs)
    }

    @Test
    fun overlayLaneAlphaUsesSharedEasingDefinition() {
        val editPlan = editPlan(
            ranges = listOf(
                TrimRange(0L, 4_000L),
                TrimRange(5_000L, 9_000L),
            ),
            transitions = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 4_000L,
                    rightSourceStartMs = 5_000L,
                    durationMs = 300L,
                    easing = ClipTransitionEasing.EASE_IN_OUT,
                ),
            ),
        )
        val topology = Media3CrossfadeTopologyCompiler.compile(
            Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan),
            editPlan,
        )

        assertEquals(0f, topology.overlayLaneAlpha(3_700_000L))
        assertEquals(0.5f, topology.overlayLaneAlpha(3_850_000L), absoluteTolerance = 0.0001f)
        assertTrue(topology.overlayLaneAlpha(3_999_000L) > 0.99f)
    }

    @Test
    fun overlappingAdjacentCrossfadesAreRejectedBeforeRuntimeTopology() {
        val ranges = listOf(
            TrimRange(0L, 1_000L),
            TrimRange(2_000L, 3_000L),
            TrimRange(4_000L, 5_000L),
        )
        val editPlan = editPlan(
            ranges = ranges,
            transitions = listOf(
                ClipTransitionBoundary(
                    leftSourceEndMs = 1_000L,
                    rightSourceStartMs = 2_000L,
                    durationMs = 700L,
                ),
                ClipTransitionBoundary(
                    leftSourceEndMs = 3_000L,
                    rightSourceStartMs = 4_000L,
                    durationMs = 700L,
                ),
            ),
        )

        val issues = EditPlanValidator.validate(editPlan)
        assertTrue(EditPlanIssue.CLIP_TRANSITION_ADJACENT_OVERLAP in issues)

        val plan = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)
        assertFailsWith<IllegalArgumentException> {
            Media3CrossfadeTopologyCompiler.compile(plan, editPlan)
        }
    }

    private fun editPlan(
        ranges: List<TrimRange>,
        transitions: List<ClipTransitionBoundary>,
        transform: TransformSettings = TransformSettings(),
    ) = EditPlan(
        sourcePath = SOURCE_PATH,
        sourceDurationMs = 12_000L,
        adaptiveCuts = AdaptiveCutSettings(
            enabled = true,
            reviewedRanges = ranges,
        ),
        transform = transform,
        exportPreset = RenderPreset.FULL_HD_1080P,
        clipTransitions = ClipTransitionSettings(
            enabled = transitions.isNotEmpty(),
            boundaries = transitions,
        ),
    )

    private fun mediaInfo() = MediaInfo(
        sourceUri = "content://video/source",
        workingFilePath = SOURCE_PATH,
        displayName = "source.mp4",
        fileSizeBytes = 1_000_000L,
        durationMs = 12_000L,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frameRate = 30.0,
        videoCodec = "h264",
        audioCodec = "aac",
        audioSampleRate = 48_000,
        audioChannels = 2,
        bitrate = 8_000_000L,
        containerFormat = "mov,mp4",
    )

    private companion object {
        const val SOURCE_PATH = "/private/source.mp4"
    }
}
