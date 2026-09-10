package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageOverlayAnimationValidationTest {

    @Test
    fun invalidLoopPeriodIsRejectedByCanonicalEditPlanValidation() {
        val issues = EditPlanValidator.validate(
            validPlan(
                ImageOverlayAnimationSettings(
                    preset = ImageOverlayAnimationPreset.PULSE,
                    loopEnabled = true,
                    durationMs = 1_000L,
                    periodMs = 500L,
                ),
            ),
        )

        assertTrue(EditPlanIssue.IMAGE_OVERLAY_ANIMATION_INVALID in issues)
    }

    @Test
    fun staticDefaultRemainsValidForBackwardCompatibility() {
        val issues = EditPlanValidator.validate(validPlan(ImageOverlayAnimationSettings()))

        assertFalse(EditPlanIssue.IMAGE_OVERLAY_ANIMATION_INVALID in issues)
    }

    private fun validPlan(animation: ImageOverlayAnimationSettings) = EditPlan(
        sourcePath = "/private/source.mp4",
        sourceDurationMs = 10_000L,
        overlays = OverlaySettings(
            enabled = true,
            image = ImageOverlaySettings(
                enabled = true,
                asset = ImageOverlayAsset(
                    workingFilePath = "/private/logo.png",
                    displayName = "logo.png",
                    mimeType = "image/png",
                    pixelWidth = 512,
                    pixelHeight = 256,
                    fileSizeBytes = 16_000L,
                ),
                startMs = 0L,
                endMs = 10_000L,
                animation = animation,
            ),
        ),
        exportPreset = RenderPreset.HD_720P,
    )
}
