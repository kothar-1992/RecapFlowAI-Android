package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditPlanValidatorTest {
    @Test
    fun fullSourcePlanIsValidAndOptionalEditsDefaultOff() {
        val plan = plan(TrimRange(0L, 10_000L))

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
        assertFalse(plan.adaptiveCuts.enabled)
        assertEquals(AdaptiveCutPreset.BALANCED, plan.adaptiveCuts.preset)
        assertTrue(plan.adaptiveCuts.reviewedRanges.isEmpty())
        assertFalse(plan.transform.enabled)
        assertEquals(AspectRatioPreset.ORIGINAL, plan.transform.aspectRatio)
        assertEquals(ScaleMode.FIT, plan.transform.scaleMode)
        assertFalse(plan.transform.crop.enabled)
        assertFalse(plan.transform.color.enabled)
        assertFalse(plan.transform.zoom.enabled)
        assertEquals(ZoomMode.IN, plan.transform.zoom.mode)
        assertFalse(plan.transform.speedEnabled)
        assertEquals(1f, plan.transform.speed)
        assertFalse(plan.transform.freeze.enabled)
        assertEquals(2_000L, plan.transform.freeze.durationMs)
        assertFalse(plan.transform.transition.enabled)
        assertEquals(TransitionMode.FADE_IN_OUT, plan.transform.transition.mode)
        assertEquals(1_000L, plan.transform.transition.durationMs)
        assertFalse(plan.audio.enabled)
        assertFalse(plan.overlays.enabled)
        assertFalse(plan.overlays.image.enabled)
        assertFalse(plan.subtitles.enabled)
    }

    @Test
    fun rejectsTrimShorterThanOneSecond() {
        val issues = EditPlanValidator.validate(plan(TrimRange(2_000L, 2_999L)))

        assertTrue(EditPlanIssue.TRIM_TOO_SHORT in issues)
    }

    @Test
    fun rejectsTrimOutsideSourceBounds() {
        val issues = EditPlanValidator.validate(plan(TrimRange(-1L, 10_001L)))

        assertTrue(EditPlanIssue.TRIM_START_BEFORE_SOURCE in issues)
        assertTrue(EditPlanIssue.TRIM_END_AFTER_SOURCE in issues)
    }

    @Test
    fun disabledTransformDoesNotValidateRememberedNestedValues() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = false,
                aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                scaleMode = ScaleMode.FILL,
                crop = CropSettings(
                    enabled = true,
                    rectangle = CropRectangle(left = 0.8f, right = 0.2f),
                ),
                speedEnabled = true,
                speed = 0f,
                freeze = FreezeSettings(enabled = true, durationMs = 1_500L),
                transition = TransitionSettings(
                    enabled = true,
                    mode = TransitionMode.FADE_IN,
                    durationMs = 750L,
                ),
                color = ColorSettings(enabled = true, brightness = 60f),
            ),
        )

        assertFalse(EditPlanIssue.SPEED_INVALID in EditPlanValidator.validate(plan))
        assertFalse(EditPlanIssue.FREEZE_DURATION_INVALID in EditPlanValidator.validate(plan))
        assertFalse(EditPlanIssue.TRANSITION_DURATION_INVALID in EditPlanValidator.validate(plan))
        assertFalse(EditPlanIssue.CROP_RECTANGLE_INVALID in EditPlanValidator.validate(plan))
        assertFalse(EditPlanIssue.COLOR_SETTINGS_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun enabledCropRejectsAnInvalidRememberedRectangle() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                crop = CropSettings(
                    enabled = true,
                    rectangle = CropRectangle(left = 0.6f, right = 0.65f),
                ),
            ),
        )

        assertTrue(EditPlanIssue.CROP_RECTANGLE_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun enabledColorRejectsValuesOutsideSupportedRanges() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                color = ColorSettings(enabled = true, temperature = -55f),
            ),
        )

        assertTrue(EditPlanIssue.COLOR_SETTINGS_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun enabledSpeedRejectsValuesOutsideSupportedRange() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(enabled = true, speedEnabled = true, speed = 2.5f),
        )

        assertTrue(EditPlanIssue.SPEED_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun plannedDurationIncludesEnabledSpeed() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(enabled = true, speedEnabled = true, speed = 0.5f),
        )

        assertEquals(20_000L, plan.plannedDurationMs)
    }

    @Test
    fun plannedDurationAddsFreezeAfterSpeedCalculation() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                speedEnabled = true,
                speed = 2f,
                freeze = FreezeSettings(enabled = true, durationMs = 3_000L),
            ),
        )

        assertEquals(8_000L, plan.plannedDurationMs)
    }

    @Test
    fun enabledFreezeRejectsUnsupportedDuration() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                freeze = FreezeSettings(enabled = true, durationMs = 1_500L),
            ),
        )

        assertTrue(EditPlanIssue.FREEZE_DURATION_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun enabledTransitionRejectsUnsupportedDuration() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                transition = TransitionSettings(
                    enabled = true,
                    mode = TransitionMode.FADE_IN,
                    durationMs = 750L,
                ),
            ),
        )

        assertTrue(EditPlanIssue.TRANSITION_DURATION_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun fadeInOutRejectsClipShorterThanBothFades() {
        val plan = plan(TrimRange(0L, 2_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                transition = TransitionSettings(
                    enabled = true,
                    mode = TransitionMode.FADE_IN_OUT,
                    durationMs = 1_500L,
                ),
            ),
        )

        assertTrue(EditPlanIssue.TRANSITION_TOO_LONG in EditPlanValidator.validate(plan))
    }

    @Test
    fun transitionDoesNotChangePlannedDuration() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            transform = TransformSettings(
                enabled = true,
                transition = TransitionSettings(
                    enabled = true,
                    mode = TransitionMode.FADE_IN_OUT,
                    durationMs = 1_000L,
                ),
            ),
        )

        assertEquals(10_000L, plan.plannedDurationMs)
    }

    @Test
    fun adaptivePlanRequiresReviewedRanges() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            adaptiveCuts = AdaptiveCutSettings(enabled = true),
        )

        assertTrue(EditPlanIssue.ADAPTIVE_RANGES_MISSING in EditPlanValidator.validate(plan))
    }

    @Test
    fun adaptivePlanAllowsPerClipFadeWhenEveryRangeIsLongEnough() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = listOf(TrimRange(0L, 4_000L), TrimRange(5_000L, 10_000L)),
            ),
            transform = TransformSettings(
                enabled = true,
                transition = TransitionSettings(enabled = true),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun adaptivePlanRejectsFadeWhenOneRangeIsTooShort() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = listOf(TrimRange(0L, 1_000L), TrimRange(2_000L, 6_000L)),
            ),
            transform = TransformSettings(
                enabled = true,
                transition = TransitionSettings(
                    enabled = true,
                    mode = TransitionMode.FADE_IN_OUT,
                    durationMs = 1_000L,
                ),
            ),
        )

        assertTrue(EditPlanIssue.TRANSITION_TOO_LONG in EditPlanValidator.validate(plan))
    }

    @Test
    fun adaptivePlannedDurationUsesOnlyReviewedRanges() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            adaptiveCuts = AdaptiveCutSettings(
                enabled = true,
                reviewedRanges = listOf(TrimRange(0L, 4_000L), TrimRange(5_000L, 9_000L)),
            ),
            transform = TransformSettings(enabled = true, speedEnabled = true, speed = 2f),
        )

        assertEquals(4_000L, plan.plannedDurationMs)
    }

    @Test
    fun muteAudioPolicyIsValid() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(enabled = true, policy = AudioPolicy.MUTE),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun enabledAudioRejectsVolumeAboveRealtimeParityRange() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.KEEP_ORIGINAL,
                volume = 1.05f,
            ),
        )

        assertTrue(EditPlanIssue.AUDIO_VOLUME_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun disabledAudioIgnoresRememberedVolume() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(enabled = false, volume = 2f),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun replaceRequiresASelectedAudioAsset() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(enabled = true, policy = AudioPolicy.REPLACE),
        )

        assertTrue(EditPlanIssue.EXTERNAL_AUDIO_MISSING in EditPlanValidator.validate(plan))
        assertFalse(EditPlanIssue.AUDIO_POLICY_UNSUPPORTED in EditPlanValidator.validate(plan))
    }

    @Test
    fun replaceWithValidAssetIsAccepted() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.REPLACE,
                replacement = ReplacementAudioAsset(
                    workingFilePath = "/private/music.m4a",
                    displayName = "music.m4a",
                    durationMs = 8_000L,
                    fileSizeBytes = 256_000L,
                ),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun replaceRejectsAnInvalidPreparedAsset() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.REPLACE,
                replacement = ReplacementAudioAsset(
                    workingFilePath = "",
                    displayName = "broken.m4a",
                    durationMs = 0L,
                    fileSizeBytes = 0L,
                ),
            ),
        )

        assertTrue(EditPlanIssue.REPLACEMENT_AUDIO_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun mixRequiresASelectedAudioAsset() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(enabled = true, policy = AudioPolicy.MIX),
        )

        val issues = EditPlanValidator.validate(plan)
        assertTrue(EditPlanIssue.EXTERNAL_AUDIO_MISSING in issues)
        assertFalse(EditPlanIssue.AUDIO_POLICY_UNSUPPORTED in issues)
    }

    @Test
    fun mixWithValidAssetAndIndependentGainsIsAccepted() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.MIX,
                volume = 0.7f,
                mixVolume = 0.3f,
                replacement = ReplacementAudioAsset(
                    workingFilePath = "/private/background.m4a",
                    displayName = "background.m4a",
                    durationMs = 8_000L,
                    fileSizeBytes = 256_000L,
                ),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun mixRejectsAddedVolumeOutsideRealtimeParityRange() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            audio = AudioSettings(
                enabled = true,
                policy = AudioPolicy.MIX,
                volume = 0.7f,
                mixVolume = 1.2f,
                replacement = ReplacementAudioAsset(
                    workingFilePath = "/private/background.m4a",
                    displayName = "background.m4a",
                    durationMs = 8_000L,
                    fileSizeBytes = 256_000L,
                ),
            ),
        )

        assertTrue(EditPlanIssue.MIX_AUDIO_VOLUME_INVALID in EditPlanValidator.validate(plan))
    }

    @Test
    fun enabledManualSourceBlurWithValidRectangleStrengthAndTimeIsAccepted() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = true,
                sourceSubtitleBlur = SourceSubtitleBlurSettings(
                    enabled = true,
                    rectangle = BlurRectangle(0.10f, 0.72f, 0.90f, 0.94f),
                    strength = 14f,
                    startMs = 1_000L,
                    endMs = 8_000L,
                ),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun enabledManualSourceBlurRejectsInvalidGeometryStrengthAndTime() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = true,
                sourceSubtitleBlur = SourceSubtitleBlurSettings(
                    enabled = true,
                    rectangle = BlurRectangle(0.98f, 0.80f, 1f, 0.82f),
                    strength = 40f,
                    startMs = 9_900L,
                    endMs = 10_000L,
                ),
            ),
        )

        val issues = EditPlanValidator.validate(plan)
        assertTrue(EditPlanIssue.SOURCE_BLUR_RECTANGLE_INVALID in issues)
        assertTrue(EditPlanIssue.SOURCE_BLUR_STRENGTH_INVALID in issues)
        assertTrue(EditPlanIssue.SOURCE_BLUR_TIME_RANGE_INVALID in issues)
    }

    @Test
    fun overlayMasterOffIgnoresRememberedInvalidSourceBlur() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = false,
                sourceSubtitleBlur = SourceSubtitleBlurSettings(
                    enabled = true,
                    rectangle = BlurRectangle(0.99f, 0.99f, 1f, 1f),
                    strength = 99f,
                    startMs = -1L,
                    endMs = 20_000L,
                ),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun enabledImageOverlayWithValidAssetGeometryAndTimeIsAccepted() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = true,
                image = validImageOverlay(),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    @Test
    fun imageOverlayOnRequiresUserToChooseAnImage() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = true,
                image = ImageOverlaySettings(enabled = true, startMs = 0L, endMs = 10_000L),
            ),
        )

        assertTrue(
            EditPlanIssue.IMAGE_OVERLAY_ASSET_INVALID in EditPlanValidator.validate(plan),
        )
        assertNull(OverlayCompiler.compileImage(plan.overlays))
    }

    @Test
    fun enabledImageOverlayRejectsInvalidAssetGeometryAndTime() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = true,
                image = validImageOverlay().copy(
                    asset = validImageOverlay().asset?.copy(mimeType = "image/gif"),
                    centerX = 1.2f,
                    widthFraction = 0.01f,
                    opacity = 0f,
                    startMs = 9_900L,
                    endMs = 10_000L,
                ),
            ),
        )

        val issues = EditPlanValidator.validate(plan)
        assertTrue(EditPlanIssue.IMAGE_OVERLAY_ASSET_INVALID in issues)
        assertTrue(EditPlanIssue.IMAGE_OVERLAY_GEOMETRY_INVALID in issues)
        assertTrue(EditPlanIssue.IMAGE_OVERLAY_TIME_RANGE_INVALID in issues)
    }

    @Test
    fun overlayMasterOffIgnoresRememberedInvalidImage() {
        val plan = plan(TrimRange(0L, 10_000L)).copy(
            overlays = OverlaySettings(
                enabled = false,
                image = validImageOverlay().copy(
                    centerX = -1f,
                    widthFraction = 2f,
                    startMs = -1L,
                    endMs = 20_000L,
                ),
            ),
        )

        assertTrue(EditPlanValidator.validate(plan).isEmpty())
    }

    private fun validImageOverlay() = ImageOverlaySettings(
        enabled = true,
        asset = ImageOverlayAsset(
            workingFilePath = "/private/logo.png",
            displayName = "logo.png",
            mimeType = "image/png",
            pixelWidth = 512,
            pixelHeight = 256,
            fileSizeBytes = 16_000L,
        ),
        centerX = 0.85f,
        centerY = 0.15f,
        widthFraction = 0.2f,
        opacity = 0.9f,
        startMs = 1_000L,
        endMs = 8_000L,
    )

    private fun plan(trimRange: TrimRange) = EditPlan(
        sourcePath = "/private/source.mp4",
        sourceDurationMs = 10_000L,
        trimRange = trimRange,
        exportPreset = RenderPreset.HD_720P,
    )
}
