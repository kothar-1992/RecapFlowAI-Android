package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutSettings
import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.AudioPolicy
import com.recapflow.ai.media.edit.AudioSettings
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
import com.recapflow.ai.media.edit.TransitionCompiler
import com.recapflow.ai.media.edit.TransitionMode
import com.recapflow.ai.media.edit.TransitionSettings
import com.recapflow.ai.media.edit.TransformCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.edit.ZoomCompiler
import com.recapflow.ai.media.edit.ZoomMode
import com.recapflow.ai.media.edit.ZoomSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FullEditPlanCombinationRegressionTest {
    @Test
    fun fullCombinationCompilesWithoutDroppingAnyEnabledFeature() {
        val editPlan = combinedPlan(RenderPreset.FULL_HD_1080P)

        assertTrue(EditPlanValidator.validate(editPlan).isEmpty())
        assertNotNull(AdaptiveCutCompiler.compile(editPlan.adaptiveCuts, editPlan.trimRange))
        assertNotNull(TransformCompiler.compile(editPlan.transform, editPlan.exportPreset))
        assertNotNull(CropCompiler.compile(editPlan.transform))
        assertNotNull(MirrorCompiler.compile(editPlan.transform))
        assertNotNull(ColorCompiler.compile(editPlan.transform))
        assertNotNull(ZoomCompiler.compile(editPlan.transform))
        assertNotNull(SpeedCompiler.compile(editPlan.transform))
        assertNotNull(FreezeCompiler.compile(editPlan.transform))
        assertNotNull(TransitionCompiler.compile(editPlan.transform, 4_000L))
        assertNotNull(AudioCompiler.compile(editPlan.audio))
        assertNotNull(OverlayCompiler.compile(editPlan.overlays))
        assertNotNull(OverlayCompiler.compileImage(editPlan.overlays))
    }

    @Test
    fun masterSwitchesDisableCompiledEffectsButPreserveRememberedControls() {
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

        // Phase 6F.2.8.1 keeps the same immutable EditPlan/timeline semantics while changing only
        // the final CBR quality budget selected for the 30fps source used by this regression test.
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
            speedEnabled = true,
            speed = 1.2f,
            freeze = FreezeSettings(enabled = true, durationMs = 2_000L),
            transition = TransitionSettings(
                enabled = true,
                mode = TransitionMode.CROSSFADE,
                durationMs = 500L,
            ),
        ),
        audio = AudioSettings(
            enabled = true,
            policy = AudioPolicy.MIX,
            volume = 0.7f,
            mixVolume = 0.3f,
            replacement = ReplacementAudioAsset(
                workingFilePath = "/private/music.m4a",
                displayName = "music.m4a",
                durationMs = 30_000L,
                fileSizeBytes = 1_000_000L,
            ),
        ),
        overlays = OverlaySettings(
            enabled = true,
            sourceSubtitleBlur = SourceSubtitleBlurSettings(
                enabled = true,
                topFraction = 0.78f,
                bottomFraction = 0.92f,
                blurRadius = 12f,
            ),
            image = ImageOverlaySettings(
                enabled = true,
                asset = ImageOverlayAsset(
                    workingFilePath = "/private/logo.png",
                    displayName = "logo.png",
                    width = 512,
                    height = 512,
                    fileSizeBytes = 50_000L,
                ),
            ),
        ),
        exportPreset = preset,
    )

    private fun mediaInfo() = MediaInfo(
        sourceUri = "content://video/source",
        workingFilePath = SOURCE_PATH,
        displayName = "source.mp4",
        fileSizeBytes = 20_000_000L,
        durationMs = 18_000L,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frameRate = 30.0,
        videoCodec = "h264",
        audioCodec = "aac",
        audioSampleRate = 48_000,
        audioChannels = 2,
        bitrate = 8_000_000L,
        containerFormat = "mp4",
    )

    private companion object {
        const val SOURCE_PATH = "/private/source.mp4"
    }
}
