package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutSettings
import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.AudioPolicy
import com.recapflow.ai.media.edit.AudioSettings
import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ColorCompiler
import com.recapflow.ai.media.edit.ColorSettings
import com.recapflow.ai.media.edit.CropCompiler
import com.recapflow.ai.media.edit.CropRectangle
import com.recapflow.ai.media.edit.CropSettings
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.EditPlanValidator
import com.recapflow.ai.media.edit.FreezeCompiler
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.ImageOverlayAsset
import com.recapflow.ai.media.edit.ImageOverlaySettings
import com.recapflow.ai.media.edit.MirrorCompiler
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.ReplacementAudioAsset
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.SourceSubtitleBlurSettings
import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TransitionCompiler
import com.recapflow.ai.media.edit.TransitionMode
import com.recapflow.ai.media.edit.TransitionSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.edit.ZoomCompiler
import com.recapflow.ai.media.edit.ZoomMode
import com.recapflow.ai.media.edit.ZoomSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 6F.2.6.2 regression contract.
 *
 * The editor must be able to keep Clips + Transform + Audio + Overlay + Export choices in one
 * immutable EditPlan and compile them together without requiring an intermediate render. These
 * tests intentionally exercise the pure planning/compiler layer so a future preview or render
 * change cannot silently drop a previously reviewed operation.
 */
class FullEditPlanCombinationRegressionTest {

    @Test
    fun allReviewedOperationsSurviveOneCombinedPlan() {
        val plan = combinedPlan(RenderPreset.FULL_HD_1080P)

        assertTrue(EditPlanValidator.validate(plan).isEmpty())

        val composition = Media3CompositionPlanCompiler.compile(mediaInfo(), plan)
        assertEquals(
            listOf(
                TrimRange(1_000L, 5_000L),
                TrimRange(7_000L, 11_000L),
                TrimRange(13_000L, 17_000L),
            ),
            composition.selectedRanges,
        )
        assertEquals(4, composition.videoItemCount) // freeze + three reviewed clips
        assertEquals(2, composition.sequenceCount) // video/source audio + looping added audio
        assertEquals(2_000L, assertNotNull(composition.freeze).durationMs)
        assertEquals(1_000L, composition.freeze?.sourceFrameTimeMs)
        assertFalse(composition.removeSourceAudio)
        assertTrue(composition.mixesSourceAudio)
        assertTrue(composition.forceSourceAudioTrack)
        assertTrue(composition.outputHasAudio)
        assertEquals(0.65f, composition.sourceLinearGain)
        assertEquals(0.35f, composition.replacementLinearGain)
        assertNotNull(composition.replacementAudio)
        assertEquals(10_000L, composition.plannedDurationMs)
        assertEquals(plan.plannedDurationMs, composition.plannedDurationMs)

        val geometry = assertNotNull(TransformCompiler.compile(plan.transform, plan.exportPreset))
        assertEquals(1080, geometry.targetWidth)
        assertEquals(1920, geometry.targetHeight)
        assertEquals(ScaleMode.FILL, geometry.scaleMode)
        assertNotNull(CropCompiler.compile(plan.transform))
        assertNotNull(MirrorCompiler.compile(plan.transform))
        assertNotNull(ColorCompiler.compile(plan.transform))
        assertNotNull(ZoomCompiler.compile(plan.transform))
        assertEquals(1.5f, assertNotNull(SpeedCompiler.compile(plan.transform)).multiplier)
        assertEquals(2_000L, assertNotNull(FreezeCompiler.compile(plan.transform)).durationMs)
        assertEquals(
            TransitionMode.FADE_IN_OUT,
            assertNotNull(TransitionCompiler.compile(plan.transform, 4_000L)).mode,
        )

        val audio = assertNotNull(AudioCompiler.compile(plan.audio))
        assertFalse(audio.removeAudio)
        assertTrue(audio.mixesSourceAudio)
        assertEquals(0.65f, audio.linearGain)
        assertEquals(0.35f, audio.replacementLinearGain)

        val blur = assertNotNull(OverlayCompiler.compile(plan.overlays))
        val image = assertNotNull(OverlayCompiler.compileImage(plan.overlays))
        assertEquals(1_000L, blur.startMs)
        assertEquals(17_000L, blur.endMs)
        assertEquals(3_000L, image.startMs)
        assertEquals(16_000L, image.endMs)
    }

    @Test
    fun overlayWindowsProjectCorrectlyIntoEveryAdaptiveClip() {
        val plan = combinedPlan(RenderPreset.FULL_HD_1080P)
        val ranges = Media3CompositionPlanCompiler.compile(mediaInfo(), plan).selectedRanges

        val first = OverlayCompiler.projectToRange(plan.overlays, ranges[0])
        assertEquals(0L, first.sourceSubtitleBlur.startMs)
        assertEquals(4_000L, first.sourceSubtitleBlur.endMs)
        assertEquals(2_000L, first.image.startMs)
        assertEquals(4_000L, first.image.endMs)

        val middle = OverlayCompiler.projectToRange(plan.overlays, ranges[1])
        assertEquals(0L, middle.sourceSubtitleBlur.startMs)
        assertEquals(4_000L, middle.sourceSubtitleBlur.endMs)
        assertEquals(0L, middle.image.startMs)
        assertEquals(4_000L, middle.image.endMs)

        val last = OverlayCompiler.projectToRange(plan.overlays, ranges[2])
        assertEquals(0L, last.sourceSubtitleBlur.startMs)
        assertEquals(4_000L, last.sourceSubtitleBlur.endMs)
        assertEquals(0L, last.image.startMs)
        assertEquals(3_000L, last.image.endMs)
    }

    @Test
    fun masterSwitchesOffOmitRememberedChildOperationsWithoutDestroyingThem() {
        val enabled = combinedPlan(RenderPreset.FULL_HD_1080P)
        val disabled = enabled.copy(
            transform = enabled.transform.copy(enabled = false),
            audio = enabled.audio.copy(enabled = false),
            overlays = enabled.overlays.copy(enabled = false),
        )

        assertTrue(EditPlanValidator.validate(disabled).isEmpty())
        assertNull(TransformCompiler.compile(disabled.transform, disabled.exportPreset))
        assertNull(CropCompiler.compile(disabled.transform))
        assertNull(MirrorCompiler.compile(disabled.transform))
        assertNull(ColorCompiler.compile(disabled.transform))
        assertNull(ZoomCompiler.compile(disabled.transform))
        assertNull(SpeedCompiler.compile(disabled.transform))
        assertNull(FreezeCompiler.compile(disabled.transform))
        assertNull(TransitionCompiler.compile(disabled.transform, 4_000L))
        assertNull(AudioCompiler.compile(disabled.audio))
        assertNull(OverlayCompiler.compile(disabled.overlays))
        assertNull(OverlayCompiler.compileImage(disabled.overlays))

        // Remembered controls remain in the immutable settings and become active again when the
        // master switch is restored. No intermediate media file is needed to preserve them.
        assertTrue(disabled.transform.mirrorEnabled)
        assertTrue(disabled.transform.crop.enabled)
        assertTrue(disabled.audio.replacement != null)
        assertTrue(disabled.overlays.sourceSubtitleBlur.enabled)
        assertTrue(disabled.overlays.image.enabled)
    }

    @Test
    fun exportPresetChangesOnlyFinalGeometryAndQualityBudget() {
        val source = mediaInfo()
        val base = combinedPlan(RenderPreset.FULL_HD_1080P)
        val p720 = base.copy(exportPreset = RenderPreset.HD_720P)
        val p1080 = base.copy(exportPreset = RenderPreset.FULL_HD_1080P)
        val p2k = base.copy(exportPreset = RenderPreset.QHD_2K)

        assertEquals(10_000L, p720.plannedDurationMs)
        assertEquals(p720.plannedDurationMs, p1080.plannedDurationMs)
        assertEquals(p1080.plannedDurationMs, p2k.plannedDurationMs)

        assertEquals(720, assertNotNull(TransformCompiler.compile(p720.transform, p720.exportPreset)).targetWidth)
        assertEquals(1080, assertNotNull(TransformCompiler.compile(p1080.transform, p1080.exportPreset)).targetWidth)
        assertEquals(1440, assertNotNull(TransformCompiler.compile(p2k.transform, p2k.exportPreset)).targetWidth)

        assertEquals(7_500_000, RenderQualityPolicy.forSource(source, p720.exportPreset).requestedVideoBitrate)
        assertEquals(10_000_000, RenderQualityPolicy.forSource(source, p1080.exportPreset).requestedVideoBitrate)
        assertEquals(18_000_000, RenderQualityPolicy.forSource(source, p2k.exportPreset).requestedVideoBitrate)
    }

    private fun combinedPlan(preset: RenderPreset): EditPlan = EditPlan(
        sourcePath = SOURCE_PATH,
        sourceDurationMs = 18_000L,
        trimRange = TrimRange(1_000L, 17_000L),
        adaptiveCuts = AdaptiveCutSettings(
            enabled = true,
            reviewedRanges = listOf(
                TrimRange(1_000L, 5_000L),
                TrimRange(7_000L, 11_000L),
                TrimRange(13_000L, 17_000L),
            ),
        ),
        transform = TransformSettings(
            enabled = true,
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            scaleMode = ScaleMode.FILL,
            crop = CropSettings(
                enabled = true,
                rectangle = CropRectangle(0.08f, 0.06f, 0.92f, 0.94f),
            ),
            zoom = ZoomSettings(enabled = true, mode = ZoomMode.ALTERNATE),
            mirrorEnabled = true,
            color = ColorSettings(
                enabled = true,
                brightness = 8f,
                contrast = 12f,
                saturation = 15f,
                temperature = 6f,
            ),
            freeze = FreezeSettings(enabled = true, durationMs = 2_000L),
            speedEnabled = true,
            speed = 1.5f,
            transition = TransitionSettings(
                enabled = true,
                mode = TransitionMode.FADE_IN_OUT,
                durationMs = 500L,
            ),
        ),
        audio = AudioSettings(
            enabled = true,
            policy = AudioPolicy.MIX,
            volume = 0.65f,
            mixVolume = 0.35f,
            replacement = replacementAudio(),
        ),
        overlays = OverlaySettings(
            enabled = true,
            sourceSubtitleBlur = SourceSubtitleBlurSettings(
                enabled = true,
                rectangle = BlurRectangle(0.10f, 0.76f, 0.90f, 0.94f),
                strength = 18f,
                startMs = 1_000L,
                endMs = 17_000L,
            ),
            image = ImageOverlaySettings(
                enabled = true,
                asset = logoAsset(),
                centerX = 0.82f,
                centerY = 0.16f,
                widthFraction = 0.24f,
                opacity = 0.85f,
                startMs = 3_000L,
                endMs = 16_000L,
            ),
        ),
        exportPreset = preset,
    )

    private fun mediaInfo() = MediaInfo(
        sourceUri = "content://video/source",
        workingFilePath = SOURCE_PATH,
        displayName = "source.mp4",
        fileSizeBytes = 12_000_000L,
        durationMs = 18_000L,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frameRate = 30.0,
        videoCodec = "h264",
        audioCodec = "aac",
        audioSampleRate = 48_000,
        audioChannels = 2,
        bitrate = 18_000_000L,
        containerFormat = "mov,mp4",
    )

    private fun replacementAudio() = ReplacementAudioAsset(
        workingFilePath = "/private/background.m4a",
        displayName = "background.m4a",
        durationMs = 6_000L,
        fileSizeBytes = 512_000L,
    )

    private fun logoAsset() = ImageOverlayAsset(
        workingFilePath = "/private/logo.png",
        displayName = "logo.png",
        mimeType = "image/png",
        pixelWidth = 512,
        pixelHeight = 512,
        fileSizeBytes = 64_000L,
    )

    private companion object {
        const val SOURCE_PATH = "/private/source.mp4"
    }
}
