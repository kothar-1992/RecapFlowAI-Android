package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.ImageOverlayAnimationPreset
import com.recapflow.ai.media.edit.ImageOverlayAnimationSettings
import com.recapflow.ai.media.edit.ImageOverlayAnimationVisualPolicy
import com.recapflow.ai.media.edit.ImageOverlayAsset
import com.recapflow.ai.media.edit.ImageOverlaySettings
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.TransformSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageOverlayAnimationPreviewExportParityTest {

    @Test
    fun twoTimesCompositionPreviewResolvesSamePulseVisualAsSourceTimeExport() {
        val overlays = OverlaySettings(
            enabled = true,
            image = ImageOverlaySettings(
                enabled = true,
                asset = imageAsset(),
                startMs = 2_000L,
                endMs = 10_000L,
                animation = ImageOverlayAnimationSettings(
                    preset = ImageOverlayAnimationPreset.PULSE,
                    loopEnabled = true,
                    durationMs = 800L,
                    periodMs = 2_400L,
                ),
            ),
        )
        val exportImage = requireNotNull(OverlayCompiler.compileImage(overlays))
        val exportVisual = ImageOverlayAnimationVisualPolicy.resolve(
            exportImage.animation,
            exportImage.animationPhaseAt(5_000L),
        )

        val previewOverlays = CompositionPreviewTimelinePolicy.projectOverlayWindowsToPresentationTime(
            overlays = overlays,
            settings = TransformSettings(enabled = true, speedEnabled = true, speed = 2f),
        )
        val previewImage = requireNotNull(OverlayCompiler.compileImage(previewOverlays))
        val previewVisual = ImageOverlayAnimationVisualPolicy.resolve(
            previewImage.animation,
            previewImage.animationPhaseAt(2_500L),
        )

        assertEquals(exportImage.animationPhaseAt(5_000L), previewImage.animationPhaseAt(2_500L))
        assertEquals(exportVisual, previewVisual)
    }

    private fun imageAsset() = ImageOverlayAsset(
        workingFilePath = "/private/logo.png",
        displayName = "logo.png",
        mimeType = "image/png",
        pixelWidth = 512,
        pixelHeight = 256,
        fileSizeBytes = 16_000L,
    )
}
