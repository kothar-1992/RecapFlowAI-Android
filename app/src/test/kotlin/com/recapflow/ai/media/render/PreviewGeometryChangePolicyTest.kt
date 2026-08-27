package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.ColorSettings
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.TransformSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreviewGeometryChangePolicyTest {

    @Test
    fun switchingPortraitToLandscapeRequiresSurfaceRebind() {
        val previous = TransformSettings(
            enabled = true,
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            scaleMode = ScaleMode.FIT,
        )
        val requested = previous.copy(aspectRatio = AspectRatioPreset.LANDSCAPE_16_9)
        assertTrue(PreviewGeometryChangePolicy.requiresSurfaceRebind(previous, requested))
    }

    @Test
    fun changingFitToFillRequiresSurfaceRebind() {
        val previous = TransformSettings(
            enabled = true,
            aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
            scaleMode = ScaleMode.FIT,
        )
        assertTrue(
            PreviewGeometryChangePolicy.requiresSurfaceRebind(
                previous,
                previous.copy(scaleMode = ScaleMode.FILL),
            ),
        )
    }

    @Test
    fun colorOnlyChangeStaysOnRetainedPlayer() {
        val previous = TransformSettings(
            enabled = true,
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            scaleMode = ScaleMode.FIT,
        )
        assertFalse(
            PreviewGeometryChangePolicy.requiresSurfaceRebind(
                previous,
                previous.copy(color = ColorSettings(enabled = true, brightness = 10f)),
            ),
        )
    }

    @Test
    fun noPresentationGeometryDoesNotRebindForTransformEnableAlone() {
        val previous = TransformSettings(enabled = false)
        val requested = TransformSettings(enabled = true, aspectRatio = AspectRatioPreset.ORIGINAL)
        assertFalse(PreviewGeometryChangePolicy.requiresSurfaceRebind(previous, requested))
    }
}
