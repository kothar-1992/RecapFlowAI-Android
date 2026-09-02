package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutSettings
import com.recapflow.ai.media.edit.ClipTransitionBoundary
import com.recapflow.ai.media.edit.ClipTransitionSettings
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.TrimRange
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Media3ClipTransitionRuntimePolicyTest {
    @Test
    fun hardCutPlanRemainsSupported() {
        val plan = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan())

        assertTrue(plan.clipTransitions.isEmpty())
        assertTrue(Media3ClipTransitionRuntimePolicy.isSupported(plan))
        Media3ClipTransitionRuntimePolicy.requireSupported(plan)
    }

    @Test
    fun reviewedCrossfadeIsCarriedIntoPlanAndRejectedExplicitlyByCurrentRuntime() {
        val editPlan = editPlan(
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

        val plan = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        assertFalse(plan.clipTransitions.isEmpty())
        assertFalse(Media3ClipTransitionRuntimePolicy.isSupported(plan))
        assertFailsWith<IllegalStateException> {
            Media3ClipTransitionRuntimePolicy.requireSupported(plan)
        }
    }

    private fun editPlan(
        clipTransitions: ClipTransitionSettings = ClipTransitionSettings(),
    ) = EditPlan(
        sourcePath = SOURCE_PATH,
        sourceDurationMs = 10_000L,
        adaptiveCuts = AdaptiveCutSettings(
            enabled = true,
            reviewedRanges = listOf(
                TrimRange(0L, 4_000L),
                TrimRange(5_000L, 9_000L),
            ),
        ),
        exportPreset = RenderPreset.FULL_HD_1080P,
        clipTransitions = clipTransitions,
    )

    private fun mediaInfo() = MediaInfo(
        sourceUri = "content://video/source",
        workingFilePath = SOURCE_PATH,
        displayName = "source.mp4",
        fileSizeBytes = 1_000_000L,
        durationMs = 10_000L,
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
