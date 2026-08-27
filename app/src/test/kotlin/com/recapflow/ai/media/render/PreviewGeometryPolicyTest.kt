package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.TransformSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewGeometryPolicyTest {

    @Test
    fun lowResolutionSourceIsNotUpscaledForInteractivePreview() {
        assertEquals(540, PreviewGeometryPolicy.shortSidePixels(540, 960))
        assertEquals(
            540 to 960,
            PreviewGeometryPolicy.compile(
                settings = TransformSettings(
                    enabled = true,
                    aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                    scaleMode = ScaleMode.FIT,
                ),
                sourceWidth = 540,
                sourceHeight = 960,
            )?.let { it.targetWidth to it.targetHeight },
        )
    }

    @Test
    fun highResolutionSourceUsesBoundedPreviewGeometry() {
        assertEquals(720, PreviewGeometryPolicy.shortSidePixels(2160, 3840))
        assertEquals(
            1280 to 720,
            PreviewGeometryPolicy.compile(
                settings = TransformSettings(
                    enabled = true,
                    aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
                    scaleMode = ScaleMode.FILL,
                ),
                sourceWidth = 2160,
                sourceHeight = 3840,
            )?.let { it.targetWidth to it.targetHeight },
        )
    }

    @Test
    fun oddSourceShortSideIsRoundedDownForCodecFriendlyGeometry() {
        assertEquals(718, PreviewGeometryPolicy.shortSidePixels(719, 1280))
    }
}
