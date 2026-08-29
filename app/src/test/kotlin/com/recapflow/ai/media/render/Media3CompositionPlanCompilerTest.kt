package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutSettings
import com.recapflow.ai.media.edit.AudioPolicy
import com.recapflow.ai.media.edit.AudioSettings
import com.recapflow.ai.media.edit.ClipTransitionBoundary
import com.recapflow.ai.media.edit.ClipTransitionSettings
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.ReplacementAudioAsset
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Media3CompositionPlanCompilerTest {
    @Test
    fun singleTrimUsesOneCompositionSequenceAndKeepsSourceAudio() {
        val editPlan = editPlan(trimRange = TrimRange(1_000L, 9_000L))

        val result = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        assertEquals(listOf(TrimRange(1_000L, 9_000L)), result.selectedRanges)
        assertNull(result.freeze)
        assertEquals(1, result.videoItemCount)
        assertEquals(1, result.videoSequenceCount)
        assertEquals(1, result.sequenceCount)
        assertFalse(result.removeSourceAudio)
        assertTrue(result.outputHasAudio)
        assertEquals(editPlan.plannedDurationMs, result.plannedDurationMs)
    }

    @Test
    fun reviewedCrossfadeReportsTwoVideoSequences() {
        val editPlan = editPlan(
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = listOf(
                    TrimRange(0L, 4_000L),
                    TrimRange(5_000L, 9_000L),
                ),
            ),
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

        val result = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        assertEquals(1, result.clipTransitions.size)
        assertEquals(2, result.videoSequenceCount)
        assertEquals(2, result.sequenceCount)
    }

    @Test
    fun adaptiveFreezeAndMixCompileOneAuthoritativeTopology() {
        val replacement = replacementAudio()
        val editPlan = editPlan(
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = listOf(
                    TrimRange(1_000L, 4_000L),
                    TrimRange(7_000L, 9_000L),
                ),
            ),
            transform = TransformSettings(
                enabled = true,
                // FreezeCompiler accepts the editor's supported 1/2/3-second choices.
                freeze = FreezeSettings(enabled = true, durationMs = 2_000L),
                speedEnabled = true,
                speed = 1.25f,
            ),
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.MIX,
                volume = 0.65f,
                mixVolume = 0.35f,
                replacement = replacement,
            ),
        )

        val result = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        assertEquals(2, result.selectedRanges.size)
        assertEquals(3, result.videoItemCount)
        assertEquals(1, result.videoSequenceCount)
        assertEquals(2, result.sequenceCount)
        assertEquals(1_000L, assertNotNull(result.freeze).sourceFrameTimeMs)
        assertEquals(2_000L, result.freeze?.durationMs)
        assertTrue(result.forceSourceAudioTrack)
        assertTrue(result.mixesSourceAudio)
        assertEquals(replacement, result.replacementAudio)
        assertEquals(editPlan.plannedDurationMs, result.plannedDurationMs)
    }

    @Test
    fun replaceRemovesSourceAudioAndAddsReplacementSequence() {
        val editPlan = editPlan(
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.REPLACE,
                volume = 0.8f,
                replacement = replacementAudio(),
            ),
        )

        val result = Media3CompositionPlanCompiler.compile(mediaInfo(), editPlan)

        assertTrue(result.removeSourceAudio)
        assertNotNull(result.replacementAudio)
        assertEquals(1, result.videoSequenceCount)
        assertEquals(2, result.sequenceCount)
        assertTrue(result.outputHasAudio)
        assertFalse(result.forceSourceAudioTrack)
    }

    @Test
    fun muteOnSilentSourceProducesVideoOnlyComposition() {
        val editPlan = editPlan(
            audio = AudioSettings(enabled = true, policy = AudioPolicy.MUTE),
        )

        val result = Media3CompositionPlanCompiler.compile(
            mediaInfo(audioCodec = null, audioChannels = 0),
            editPlan,
        )

        assertTrue(result.removeSourceAudio)
        assertFalse(result.outputHasAudio)
        assertEquals(1, result.videoSequenceCount)
        assertEquals(1, result.sequenceCount)
        assertNull(result.replacementAudio)
    }

    private fun editPlan(
        trimRange: TrimRange = TrimRange(0L, 10_000L),
        adaptiveCuts: AdaptiveCutSettings = AdaptiveCutSettings(),
        transform: TransformSettings = TransformSettings(),
        audio: AudioSettings = AudioSettings(),
        clipTransitions: ClipTransitionSettings = ClipTransitionSettings(),
    ) = EditPlan(
        sourcePath = SOURCE_PATH,
        sourceDurationMs = 10_000L,
        trimRange = trimRange,
        adaptiveCuts = adaptiveCuts,
        transform = transform,
        audio = audio,
        exportPreset = RenderPreset.FULL_HD_1080P,
        clipTransitions = clipTransitions,
    )

    private fun mediaInfo(
        audioCodec: String? = "aac",
        audioChannels: Int = 2,
    ) = MediaInfo(
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
        audioCodec = audioCodec,
        audioSampleRate = if (audioChannels == 0) 0 else 48_000,
        audioChannels = audioChannels,
        bitrate = 8_000_000L,
        containerFormat = "mov,mp4",
    )

    private fun replacementAudio() = ReplacementAudioAsset(
        workingFilePath = "/private/music.m4a",
        displayName = "music.m4a",
        durationMs = 4_000L,
        fileSizeBytes = 128_000L,
    )

    private companion object {
        const val SOURCE_PATH = "/private/source.mp4"
    }
}
