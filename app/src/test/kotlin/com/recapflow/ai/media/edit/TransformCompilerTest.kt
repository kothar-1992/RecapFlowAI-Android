package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransformCompilerTest {
    @Test
    fun twoKPortraitUsesExactQhdDimensions() {
        val result = TransformCompiler.compile(
            TransformSettings(
                enabled = true,
                aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
                scaleMode = ScaleMode.FIT,
            ),
            RenderPreset.QHD_2K,
        )

        assertEquals(1440, result?.targetWidth)
        assertEquals(2560, result?.targetHeight)
    }


    @Test
    fun disabledTransformIsOmittedEvenWhenSelectionsAreRemembered() {
        val settings = TransformSettings(
            enabled = false,
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            scaleMode = ScaleMode.FILL,
        )

        assertNull(TransformCompiler.compile(settings, RenderPreset.HD_720P))
    }

    @Test
    fun originalAspectIsAnExplicitNoOp() {
        val settings = TransformSettings(
            enabled = true,
            aspectRatio = AspectRatioPreset.ORIGINAL,
            scaleMode = ScaleMode.FILL,
        )

        assertNull(TransformCompiler.compile(settings, RenderPreset.FULL_HD_1080P))
    }

    @Test
    fun compilesPortraitTargetsForBothVerifiedPresets() {
        val settings = TransformSettings(
            enabled = true,
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            scaleMode = ScaleMode.FIT,
        )

        assertEquals(
            CompiledTransform(720, 1280, ScaleMode.FIT),
            TransformCompiler.compile(settings, RenderPreset.HD_720P),
        )
        assertEquals(
            CompiledTransform(1080, 1920, ScaleMode.FIT),
            TransformCompiler.compile(settings, RenderPreset.FULL_HD_1080P),
        )
    }

    @Test
    fun compilesLandscapeAndSquareFillTargets() {
        assertEquals(
            CompiledTransform(1280, 720, ScaleMode.FILL),
            TransformCompiler.compile(
                TransformSettings(
                    enabled = true,
                    aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
                    scaleMode = ScaleMode.FILL,
                ),
                RenderPreset.HD_720P,
            ),
        )
        assertEquals(
            CompiledTransform(1080, 1080, ScaleMode.FILL),
            TransformCompiler.compile(
                TransformSettings(
                    enabled = true,
                    aspectRatio = AspectRatioPreset.SQUARE_1_1,
                    scaleMode = ScaleMode.FILL,
                ),
                RenderPreset.FULL_HD_1080P,
            ),
        )
    }
}
